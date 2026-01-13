package swiss.opendata.piveau.pipe.module.patching;

import io.piveau.pipe.PipeLogger;
import io.vertx.core.json.JsonArray;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.DCAT;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PayloadPatcher {

    // fallback logger gets used during unit-tests
    private static final Logger fallbackLog = LoggerFactory.getLogger(PayloadPatcher.class);

    public static void apply(Model model, JsonArray actions, PipeLogger pipeLog) {
        if (actions.contains("remove-dataset-cloak")) {
            for (StmtIterator it = model.listStatements(null, RDF.type, DCAT.Dataset); it.hasNext(); ) {
                Statement stmt = it.next();
                log_info(pipeLog, "Removing triple " + stmt);
                it.remove();
            }
        }

        if (actions.contains("fix-showcase-typing")) {
            final Resource tempType = model.createResource("http://localhost:3000/Showcase");
            final Resource targetType = model.createResource("https://example.org/Showcase");

            Statement fixedStmt = null;
            for (StmtIterator it = model.listStatements(null, RDF.type, tempType); it.hasNext(); ) {
                final Statement stmt = it.next();
                log_info(pipeLog, "Removing triple " + stmt);
                it.remove();

                // only one single resource to fix: assuming here we get at most one single showcase per pipe message
                fixedStmt = model.createStatement(stmt.getSubject(), RDF.type, targetType);
            }

            if (fixedStmt != null) {
                log_info(pipeLog, "Adding triple " + fixedStmt);
                model.add(fixedStmt);
            }
        }
    }

    private static void log_info(PipeLogger pipeLog, String message) {
        if (pipeLog != null) {
            pipeLog.info(message);
        } else {
            fallbackLog.info(message);
        }
    }

}
