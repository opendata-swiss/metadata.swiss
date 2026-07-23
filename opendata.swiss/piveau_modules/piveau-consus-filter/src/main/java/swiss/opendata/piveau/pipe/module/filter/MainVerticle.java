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
import org.apache.jena.riot.RiotException;
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
        return true;
        // String identifier = node.asLiteral().getString();
        // return identifier.matches("^[A-Za-z0-9_-]+@" + organizationID + "$");
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
        "NonCommercialAllowed-CommercialAllowed-ReferenceRequired", "http://dcat-ap.ch/vocabulary/licenses/terms_by",
        "NonCommercialAllowed-CommercialWithPermission-ReferenceNotRequired", "http://dcat-ap.ch/vocabulary/licenses/terms_ask",
        "NonCommercialAllowed-CommercialWithPermission-ReferenceRequired", "http://dcat-ap.ch/vocabulary/licenses/terms_by_ask"
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
                    logger.warn("Invalid license: {}", node);
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
            logger.warn("multiple licences found: {}", String.join(", ", validLicenses));
            writeError.accept(inconsistentLicenses);
        }
    }

    public static void checkSpatial(Model model, Resource dataset) {
        Property dctSpatial = model.createProperty(DCTERMS.SPATIAL.stringValue());

        List<RDFNode> nodes = model.listObjectsOfProperty(dataset, dctSpatial).toList();
        for (RDFNode node : nodes) {
            if (node.isLiteral()) {
                model.removeAll(dataset, dctSpatial, node);
                logger.warn("Invalid spatial value: {}.", node);
            }
        }
    }

    class ErrorHandler implements Consumer<String> {        
        private JsonObject config;
        private JsonObject dataInfo;
        private List<String> errors = new ArrayList<>();

        // may need additional arguments for sending notifications (e.g. email client, API client, etc.)
        public ErrorHandler(JsonObject config, JsonObject dataInfo) {
            super();
            this.config = config;
            this.dataInfo = dataInfo;
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
                sb.append("Catalogue: ").append(config.getString("catalogue")).append("\t");
            }
            if (config.containsKey("org_id")){
                sb.append("Organization: ").append(config.getString("org_id")).append("\t");
            }
            if (dataInfo.containsKey("identifier")){
                sb.append("Identifier: ").append(dataInfo.getString("identifier")).append("\t");
            }
            for (String error : errors) {
                sb.append("- ").append(error).append("\t");
            }
            String message = sb.toString();

            logger.error(message);
            if (config.containsKey("mailto")) {
                logger.trace("TODO: Notify data publisher at {}", config.getString("mailto"));
            }   
        }
    }

    private Model getModel(PipeContext pipeContext) {
        try {
            if (Lang.NTRIPLES.getHeaderString().equals(pipeContext.getMimeType())) {
                return Piveau.toModel(pipeContext.getStringData().getBytes(), Lang.NTRIPLES);
            }
            if (Lang.RDFXML.getHeaderString().equals(pipeContext.getMimeType())) {
                return Piveau.toModel(pipeContext.getStringData().getBytes(), Lang.RDFXML);
            }
        } catch (RiotException e) {
            logger.error("Failed to parse RDF data from pipe context {}", pipeContext.getDataInfo(), e);
        }

        return null;
    }

    private void handlePipe(PipeContext pipeContext) {
        if (pipeContext.log().isTraceEnabled()) {
            pipeContext.log().trace(pipeContext.getPipeManager().prettyPrint());
        }
        Model model = getModel(pipeContext);
        if (model == null) {
            pipeContext.pass();
            return;
        }
        
        List<Resource> datasets = model.listResourcesWithProperty(RDF.type, DCAT.Dataset).toList();
        Set<Resource> dcatResources = new HashSet<>(datasets); // the same resource could be both a Dataset and a DatasetSeries, so we use a Set to avoid duplicates
        dcatResources.addAll(model.listResourcesWithProperty(RDF.type, model.createResource(DCAT.NAMESPACE + "DatasetSeries")).toList());
        dcatResources.addAll(model.listResourcesWithProperty(RDF.type, DCAT.DataService).toList());
       
        if (dcatResources.size() != 1) {
            logger.warn("Expected exactly one dcat resource (Dataset, DatasetSeries or DataService), but found {}. Skipping this pipe execution.", dcatResources.size());
            return;
        }
        Resource resource = dcatResources.iterator().next();

        JsonObject dataInfo = pipeContext.getDataInfo();

        if(datasets.contains(resource)) {
            // if resource is a Dataset, we perform checks (and possibly make little changes to it)
            JsonObject config = pipeContext.getConfig();
            
            ErrorHandler errorHandler = new ErrorHandler(config, dataInfo);
            checkIdentifier(model, resource, config.getString("org_id"), errorHandler);
            checkConformsTo(model, resource, errorHandler);
            checkLicense(model, resource, errorHandler);
            checkSpatial(model, resource);

            if (errorHandler.hasErrors()) {
                errorHandler.notifyErrors();
            } else {
                logger.info("dataset {}", dataInfo);
                pipeContext.setResult(Piveau.presentAs(model, Lang.NTRIPLES), Lang.NTRIPLES.getHeaderString(), dataInfo).forward();
            }
        } else {
            logger.info("resource {}", dataInfo);
            // we propagate the resource as is, without any checks or modifications
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
