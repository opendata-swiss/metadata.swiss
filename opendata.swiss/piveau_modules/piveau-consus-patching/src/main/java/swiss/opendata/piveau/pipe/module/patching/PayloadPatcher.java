package swiss.opendata.piveau.pipe.module.patching;

import io.piveau.pipe.PipeLogger;
import io.vertx.core.json.JsonArray;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.DCAT;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class PayloadPatcher {

    // fallback logger gets used during unit-tests
    private static final Logger fallbackLog = LoggerFactory.getLogger(PayloadPatcher.class);

    public static final String SHOWCASE_TMP_URI = "http://localhost:3000/Showcase";
    public static final String SHOWCASE_TARGET_URI = "https://example.org/Showcase";

    public static void apply(Model model, JsonArray actions, PipeLogger pipeLog) {
        if (actions.contains("remove-dataset-cloak")) {

            final Patch patch = new Patch();
            for (StmtIterator it = model.listStatements(null, RDF.type, DCAT.Dataset); it.hasNext(); ) {
                Statement stmt = it.next();
                patch.obsoleteStatements.add(stmt);
            }
            patch.applyOn(model, pipeLog);
        }

        if (actions.contains("fix-showcase-typing")) {
            final Resource tempType = model.createResource(SHOWCASE_TMP_URI);
            final Resource targetType = model.createResource(SHOWCASE_TARGET_URI);

            final Patch patch = new Patch();
            for (StmtIterator it = model.listStatements(null, RDF.type, tempType); it.hasNext(); ) {
                final Statement stmt = it.next();

                patch.obsoleteStatements.add(stmt);
                patch.newStatements.add(model.createStatement(stmt.getSubject(), RDF.type, targetType));
            }
            patch.applyOn(model, pipeLog);
        }

        if (actions.contains("unwrap-references")) {

            final Patch patch = new Patch();

            final Property propReferences = DCTerms.references;

            for (StmtIterator it = model.listStatements(null, propReferences, (RDFNode) null); it.hasNext(); ) {
                final Statement stmt = it.next();
                if (stmt.getObject().isAnon()) {
                    populateUnwrapReferencesPatch(patch, model, stmt, pipeLog);
                }
            }

            patch.applyOn(model, pipeLog);
        }
    }

    private static void populateUnwrapReferencesPatch(Patch patch, Model model, Statement stmt, PipeLogger pipeLog) {
        final Resource referenceSource = stmt.getSubject();
        final Resource bNode = stmt.getObject().asResource();

        for (NodeIterator targetIdIterator = model.listObjectsOfProperty(bNode, DCTerms.identifier); targetIdIterator.hasNext(); ) {
            final RDFNode targetId = targetIdIterator.next();

            if (targetId.isLiteral()) {
                String targetIdString = targetId.asLiteral().getLexicalForm();

                try {
                    new URI(targetIdString); // assert targetIdString is a valid URI
                    final Resource referenceTarget = model.createResource(targetIdString);

                    patch.newStatements.add(model.createStatement(referenceSource, DCTerms.references, referenceTarget));

                    patch.obsoleteStatements.add(stmt);
                    for (StmtIterator bNodeStmtIterator = model.listStatements(bNode, null, (RDFNode) null); bNodeStmtIterator.hasNext(); ) {
                        patch.obsoleteStatements.add(bNodeStmtIterator.next());
                    }

                } catch (URISyntaxException e) {
                    log_warn(pipeLog, "Reference left unwrapped, not a valid URI: " + targetIdString);
                }
            }
        }
    }

    record Patch(List<Statement> obsoleteStatements, List<Statement> newStatements) {

        Patch() {
            this(new ArrayList<>(), new ArrayList<>());
        }

        void applyOn(Model model, PipeLogger pipeLog) {
            for (Statement stmt : obsoleteStatements) {
                log_info(pipeLog, "Removing triple " + stmt);
                model.remove(stmt);
            }

            for (Statement stmt : newStatements) {
                log_info(pipeLog, "Adding triple " + stmt);
                model.add(stmt);
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

    private static void log_warn(PipeLogger pipeLog, String message) {
        if (pipeLog != null) {
            pipeLog.warn(message);
        } else {
            fallbackLog.warn(message);
        }
    }

}
