package swiss.opendata.piveau.pipe.module.filter;

import io.piveau.pipe.PipeContext;
import io.piveau.pipe.connector.PipeConnector;
import io.piveau.rdf.Piveau;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Launcher;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.vocabulary.DCAT;
import org.apache.jena.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainVerticle extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(MainVerticle.class);


    /**
     * The main Vert.x function aka entry point to the application
     */
    @Override
    public void start(Promise<Void> startPromise) throws Exception {

        Future<PipeConnector> pipeConnector = PipeConnector.create(vertx);
        pipeConnector.onSuccess(connector -> {
            connector.handlePipe(this::handlePipe);
        });

        startPromise.complete();
        logger.info("Piveau Consus Filter started successfully.");
    }

    public static boolean isValidIdentifierValue(RDFNode node, String organizationID) {
        if (!node.isLiteral()) {
            return false;
        }
        String identifier = node.asLiteral().getString();
        return identifier.matches("^[A-Za-z0-9_-]+@" + organizationID + "$");
    }

    private static String missingIdentifier = "The property 'dct:identifier' on dcat:Dataset is missing. The dataset cannot be imported without a valid dataset identifier. Please add the missing identifier to proceed with the import.";
    private static String multipleIdentifiers = "The property 'dct:identifier' on dcat:Dataset has multiple values. The dataset cannot be imported with multiple identifiers. Please ensure there is only one identifier to proceed with the import.";
    private static String invalidIdentifier = "The value of 'dct:identifier' on dcat:Dataset is invalid. Please provide a correct identifier to allow the dataset to be imported.";

    public static void checkIdentifier(Model model, Resource dataset, String organizationID, Consumer<String> writeError) {
        Property dctIdentifier = model.createProperty(DCTERMS.IDENTIFIER.stringValue());

        List<RDFNode> nodes = model.listObjectsOfProperty(dataset, dctIdentifier).toList();
        
        if (nodes.isEmpty()) {
            writeError.accept(missingIdentifier);
        }

        if (nodes.size() > 1) {
            writeError.accept(multipleIdentifiers);
        }

        // The identifier is expected in the following structure: [Source-Dataset-ID]@[Source-Organisation-ID] 
        // where [Source-Organisation-ID] is the slug of the organization on opendata.swiss. 
        // [Source-Dataset-ID] must be unique within the datasets of the organization. 
        // A recommended way to choose this is to use the ID in the source system of the publisher. 
        // It can consist of the following characters: A-Za-z, 0-9 and - and _

        for (RDFNode node : nodes) {
            if (!isValidIdentifierValue(node, organizationID)) {
                writeError.accept(invalidIdentifier);
            }        
        }  
    }

    public static boolean isValidConformsToValue(RDFNode node) {
        // what is the correctness criteria for dct:conformsTo?
        // should have rdf:type dct:Standard?

        if (!node.isURIResource()) {
            return false;
        }
       
        return true;
    }

    private static String invalidConformsToInDataset = "The property 'dct:conformsTo' on dcat:Dataset contains an invalid value. Since the value is incorrect, the dataset cannot be imported. Please correct it to proceed with the import.";
    private static String invalidConformsToInDistribution = "The property 'dct:conformsTo' on dcat:Distribution contains an invalid value. Since the value is incorrect, the dataset cannot be imported. Please correct it to proceed with the import.";
    
    public static void checkConformsTo(Model model, Resource dataset, Consumer<String> writeError) {
        Property dctConformsTo = model.createProperty(DCTERMS.CONFORMS_TO.stringValue());

        List<RDFNode> nodes = model.listObjectsOfProperty(dataset, dctConformsTo).toList();
        for (RDFNode node : nodes) {
            if (!isValidConformsToValue(node)) {
                writeError.accept(invalidConformsToInDataset);
            }
        }
        
        List<RDFNode> distributions = model.listObjectsOfProperty(dataset, DCAT.distribution).toList();
        for (RDFNode distribution : distributions) {
            List<RDFNode> distNodes = model.listObjectsOfProperty(distribution.asResource(), dctConformsTo).toList();
            for (RDFNode node : distNodes) {
                if (!isValidConformsToValue(node)) {
                    writeError.accept(invalidConformsToInDistribution);
                }
            }
        }
    }

    private static List<String> validLicenseURIs = Arrays.asList(
        "http://dcat-ap.ch/vocabulary/licenses/terms_open",
        "http://dcat-ap.ch/vocabulary/licenses/terms_by",
        "http://dcat-ap.ch/vocabulary/licenses/terms_ask",
        "http://dcat-ap.ch/vocabulary/licenses/terms_by_ask"
    );

    private static Map<String, String> deprecatedLicenceMap = Map.of(
        "NonCommercialAllowed-CommercialAllowed-ReferenceNotRequired", "http://dcat-ap.ch/vocabulary/licenses/terms_open",
        "AttributionRequired-CommercialAllowed-ReferenceNotRequired", "http://dcat-ap.ch/vocabulary/licenses/terms_by",
        "AskForPermission-CommercialAllowed-ReferenceNotRequired", "http://dcat-ap.ch/vocabulary/licenses/terms_ask",
        "AttributionRequired-AskForPermission-ReferenceNotRequired", "http://dcat-ap.ch/vocabulary/licenses/terms_by_ask"
    );

    private static String missingLicense = "The property 'dct:license' is missing for one or more distributions. Each distribution must provide a valid license, and all distributions must use the same Terms of Use. The dataset cannot be imported. Please add the missing license to proceed with the import.";
    private static String invalidLicense = "One or more distributions contain an invalid or non‑conformant 'dct:license' value. Each distribution must provide a valid license, and all distributions must use the same Terms of Use. The dataset cannot be imported until all licenses are corrected.";
    private static String inconsistentLicenses = "The distributions of this dataset do not use the same Terms of Use. All distributions must have identical 'dct:license' values. The dataset cannot be imported until the licenses are aligned.";

    public static void checkLicense(Model model, Resource dataset, Consumer<String> writeError) {
        Property dctLicense = model.createProperty(DCTERMS.LICENSE.stringValue());

        Set<String> validLicenses = new HashSet<String>();
        boolean invalidLicenseFound = false;
        boolean missingLicenseFound = false;

        List<RDFNode> distributions = model.listObjectsOfProperty(dataset, DCAT.distribution).toList();
        for (RDFNode distribution : distributions) {
            List<RDFNode> licenseNodes = model.listObjectsOfProperty(distribution.asResource(), dctLicense).toList();
            
            if (licenseNodes.isEmpty()) {
                missingLicenseFound = true;
                continue;
            }

            for (RDFNode node : licenseNodes) {
                if (node.isResource() && validLicenseURIs.contains(node.asResource().getURI())) {
                    validLicenses.add(node.asResource().getURI());
                } else if (node.isLiteral() && deprecatedLicenceMap.containsKey(node.asLiteral().getString())) {
                    String updatedLicense = deprecatedLicenceMap.get(node.asLiteral().getString());
                    logger.warn("The license '{}' is deprecated. Please update it to <{}>.", node.asLiteral().getString(), updatedLicense);
                    validLicenses.add(updatedLicense); 
                    model.removeAll(distribution.asResource(), dctLicense, node);
                    model.add(distribution.asResource(), dctLicense, model.createResource(updatedLicense));
                } else {
                    invalidLicenseFound = true;
                }
            }
        }
        if(missingLicenseFound) {
            writeError.accept(missingLicense);
        }
        if(invalidLicenseFound) {
            writeError.accept(invalidLicense);
        }
        if(validLicenses.size() > 1) {
            writeError.accept(inconsistentLicenses);
        }
    }

    class ErrorHandler implements Consumer<String> {        
        private JsonObject config;
        private List<String> errors = new ArrayList<>();

        // may need additional arguments for sending notifications (e.g. email client, API client, etc.)
        public ErrorHandler(JsonObject config) {
            super();
            this.config = config;
        }

        @Override
        public void accept(String error) {
            errors.add(error);
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public void notifyErrors() {
            if (!hasErrors()) return;
            
            StringBuilder sb = new StringBuilder();
            if(config.containsKey("catalogue")){
                sb.append("Catalogue: ").append(config.getString("catalogue")).append("\n");
            }
            if (config.containsKey("datasetURI")){
                sb.append("Dataset: ").append(config.getString("datasetURI")).append("\n");
            }
            for (String error : errors) {
                sb.append("- ").append(error).append("\n");
            }
            String message = sb.toString();

            logger.warn(message);
            if (config.containsKey("mailto")) {
                logger.trace("TODO: Notify data publisher at {}", config.getString("mailto"));
            }   
        }
    }

    private void handlePipe(PipeContext pipeContext) {
        if (pipeContext.log().isTraceEnabled()) {
            pipeContext.log().trace(pipeContext.getPipeManager().prettyPrint());
        }

        if (Lang.NTRIPLES.getHeaderString().equals(pipeContext.getMimeType())) {
            JsonObject config = pipeContext.getConfig();
            Model model = Piveau.toModel(
                pipeContext.getStringData().getBytes(),
                Lang.NTRIPLES
            );
            List<Resource> datasets = model.listResourcesWithProperty(RDF.type, DCAT.Dataset).toList();  
            if (datasets.size() != 1) {
                logger.warn("Expected exactly one dcat:Dataset, but found {}. Skipping this pipe execution.", datasets.size());
                return;
            }
            Resource dataset = datasets.get(0);
            config.put("datasetURI", dataset.getURI());

            String organizationID = config.getString("org_id");

            ErrorHandler errorHandler = new ErrorHandler(config);
            checkIdentifier(model, dataset, organizationID, errorHandler);
            checkConformsTo(model, dataset, errorHandler);
            checkLicense(model, dataset, errorHandler);

            if (errorHandler.hasErrors()) {
                errorHandler.notifyErrors();
            } else {
                logger.info("passing {}", dataset.getURI());
                pipeContext.pass();
            }
        } else {
            pipeContext.pass();
        }
    }

    /**
     * This is an optional function which is handy if you want to start your app form an IDE.
     *
     * @param args
     */
    public static void main(String[] args) {
        String[] params = Arrays.copyOf(args, args.length + 1);
        params[params.length - 1] = MainVerticle.class.getName();
        Launcher.executeCommand("run", params);
    }
}
