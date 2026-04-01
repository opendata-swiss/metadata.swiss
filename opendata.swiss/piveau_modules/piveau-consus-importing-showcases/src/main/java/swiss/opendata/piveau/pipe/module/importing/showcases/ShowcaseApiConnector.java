package swiss.opendata.piveau.pipe.module.importing.showcases;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.piveau.pipe.PipeContext;
import io.piveau.rdf.Piveau;
import io.piveau.rdf.RDFMediaTypes;
import io.piveau.utils.JenaUtils;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.vocabulary.RDF;

import java.util.ArrayList;
import java.util.List;

public class ShowcaseApiConnector {

    private final PipeContext pipeContext;
    private final WebClient webClient;

    private final String catalogue;
    private final String address;

    private final List<String> identifiers = new ArrayList<>();


    public static ShowcaseApiConnector create(Vertx vertx, PipeContext pipeContext) {
        return new ShowcaseApiConnector(vertx, pipeContext);
    }

    private ShowcaseApiConnector(Vertx vertx, PipeContext pipeContext) {
        this.pipeContext = pipeContext;
        this.webClient = WebClient.create(vertx);

        JsonObject config = pipeContext.getConfig();

        this.catalogue = config.getString("catalogue");
        this.address = config.getString("address");
    }

    public List<String> getIdentifiers() {
        return identifiers;
    }

    public void fetchAll(Promise<Void> promise) {
        webClient.getAbs(address).send().onSuccess(response -> {
            try {
                Model model = Piveau.toModel(response.bodyAsString(), Lang.JSONLD);

                ShowcaseExtractor extractor = new ShowcaseExtractor();

                Resource showcaseClass = model.createResource(PayloadPatcher.SHOWCASE_TMP_URI);

                List<Resource> showcases = model.listSubjectsWithProperty(RDF.type, showcaseClass).toList();
                long totalCount = showcases.size();

                showcases.forEach(showcase -> {
                    Model extracted = extractor.extract(showcase, model);
                    String identifier = JenaUtils.findIdentifier(showcase);
                    identifiers.add(identifier);

                    // patch the RDF payload
                    PayloadPatcher.apply(extracted, pipeContext.log());

                    ObjectNode dataInfo = new ObjectMapper().createObjectNode()
                            .put("total", totalCount)
                            .put("counter", identifiers.size())
                            .put("identifier", identifier)
                            .put("content", "resource")
                            .put("resourceType", "showcase")
                            .put("catalogue", catalogue);

                    pipeContext.setResult(JenaUtils.write(extracted, RDFMediaTypes.NTRIPLES), RDFMediaTypes.NTRIPLES, dataInfo)
                            .forward();
                    pipeContext.log().info("Data imported: {}", dataInfo);

                    extracted.close();
                });

                model.close();
                promise.complete();
            } catch (Exception e) {
                pipeContext.log().error("Error processing showcases", e);
                promise.fail(e);
            }
        }).onFailure(err -> {
            pipeContext.log().error("Failed to fetch showcases from " + address, err);
            promise.fail(err);
        });
    }
}