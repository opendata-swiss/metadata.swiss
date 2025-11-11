package swiss.opendata.piveau.pipe.module.patching;

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
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.vocabulary.DCAT;
import org.apache.jena.vocabulary.RDF;

import java.util.Arrays;
import java.util.Iterator;

public class MainVerticle extends AbstractVerticle {

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
    }

    private void handlePipe(PipeContext pipeContext) {
        if (pipeContext.log().isTraceEnabled()) {
            pipeContext.log().trace(pipeContext.getPipeManager().prettyPrint());
        }

        if (Lang.NTRIPLES.getHeaderString().equals(pipeContext.getMimeType())) {
            final JsonArray actions = pipeContext.getConfig().getJsonArray("actions");
            final Model model = Piveau.toModel(
                    pipeContext.getStringData().getBytes(),
                    Lang.NTRIPLES
            );
            final JsonObject outboundDataInfo = new JsonObject().mergeIn(pipeContext.getDataInfo());

            if (actions.contains("remove-dataset-cloak")) {
                for (StmtIterator it = model.listStatements((Resource) null, RDF.type, DCAT.Dataset); it.hasNext(); ) {
                    Statement stmt = it.next();
                    pipeContext.log().info("Removing triple " + stmt);
                    it.remove();
                }
            }

            if (actions.contains("fix-showcase-typing")) {
                final Resource tempType = model.createResource("http://localhost:3000/Showcase");
                final Resource targetType = model.createResource("https://example.org/Showcase");

                Statement fixedStmt = null;
                for (StmtIterator it = model.listStatements((Resource) null, RDF.type, tempType); it.hasNext(); ) {
                    final Statement stmt = it.next();
                    pipeContext.log().info("Removing triple " + stmt);
                    it.remove();

                    // only one single resource to fix: assuming here we get at most one single showcase per pipe message
                    fixedStmt = model.createStatement(stmt.getSubject(), RDF.type, targetType);
                }

                if (fixedStmt != null) {
                    pipeContext.log().info("Adding triple " + fixedStmt);
                    model.add(fixedStmt);
                }
            }

            String resourceType = null;
            for (Iterator<Object> it = actions.iterator(); it.hasNext(); ) {
                final String action = it.next().toString();
                if (action.startsWith("signal-resource-")) {
                    resourceType = action.substring("signal-resource-".length());
                    break;  // only consider the first "signal-resource-" match
                }
            }
            if (resourceType != null && ! resourceType.isBlank()) {
                pipeContext.log().info("Signaling resourceType: {}", resourceType);
                outboundDataInfo
                        .put("content", "resource")
                        .put("resourceType", resourceType);
            }

            pipeContext.log().debug("Outbound dataInfo: {}", outboundDataInfo.toString());

            pipeContext.setResult(
                            Piveau.presentAs(model, Lang.NTRIPLES),
                            Lang.NTRIPLES.getHeaderString(),
                            outboundDataInfo)
                    .forward();

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
