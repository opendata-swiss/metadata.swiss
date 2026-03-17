package swiss.opendata.piveau.pipe.module.filter;

import io.piveau.pipe.PipeContext;
import io.piveau.pipe.connector.PipeConnector;
import io.piveau.rdf.Piveau;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Launcher;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.sparql.function.library.e;
import org.apache.jena.vocabulary.DCAT;
import org.apache.jena.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

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

    private void handlePipe(PipeContext pipeContext) {
        if (pipeContext.log().isTraceEnabled()) {
            pipeContext.log().trace(pipeContext.getPipeManager().prettyPrint());
        }

        if (Lang.NTRIPLES.getHeaderString().equals(pipeContext.getMimeType())) {
            final Model model = Piveau.toModel(
                pipeContext.getStringData().getBytes(),
                Lang.NTRIPLES
            );
            final List<Resource> datasets = model.listResourcesWithProperty(RDF.type, DCAT.Dataset).toList();  
            if (datasets.size() != 1) {
                logger.info("datasets size: " + datasets.size() + ", skipping.");
                return;
            }
            Resource dataset = datasets.get(0);

            // this is just an example of a filter
            List<RDFNode> landingPages = model.listObjectsOfProperty(dataset, DCAT.landingPage).toList();
            if (landingPages.isEmpty()) {
                logger.info("Dataset " + dataset.getURI() + " has no landing page, skipping.");
                return;
            };
            
            logger.info("passing data for dataset: " + dataset.getURI());
            pipeContext.pass();
        
        } else {
            logger.warn("Unsupported MIME type: " + pipeContext.getMimeType());
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
