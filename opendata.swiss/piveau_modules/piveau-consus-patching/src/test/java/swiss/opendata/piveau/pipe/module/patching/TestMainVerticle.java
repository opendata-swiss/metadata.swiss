package swiss.opendata.piveau.pipe.module.patching;

import io.piveau.rdf.Piveau;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

import java.util.ArrayList;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class TestMainVerticle {

  @BeforeEach
  void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
    vertx.deployVerticle(new MainVerticle(), testContext.succeeding(id -> testContext.completeNow()));
  }

  @Test
  void verticle_deployed(Vertx vertx, VertxTestContext testContext) throws Throwable {
    testContext.completeNow();
  }

  @Test
  void testRemoveDatasetCloak() { 
    ArrayList<String> loggerOutput = new ArrayList<String>();
    String data = "<http://example.org/dataset/1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/ns/dcat#Dataset> .";
    Model model = Piveau.toModel(data.getBytes(), Lang.NTRIPLES);
    
    MainVerticle.removeDatasetCloak(loggerOutput::add, model);

    assert(loggerOutput.size() == 1);
    assert(loggerOutput.get(0).startsWith("Removing triple "));
    assert(model.isEmpty()); // Should be empty after removal
  }

  @Test
  void testFixShowcaseTyping() { 
    ArrayList<String> loggerOutput = new ArrayList<String>();
    String data = "<http://example.org/showcase/1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://localhost:3000/Showcase> .";
    Model model = Piveau.toModel(data.getBytes(), Lang.NTRIPLES);
    
    MainVerticle.fixShowcaseTyping(loggerOutput::add, model);

    assert(loggerOutput.size() == 2);
    assert(loggerOutput.get(0).startsWith("Removing triple "));
    assert(loggerOutput.get(1).startsWith("Adding triple "));
    assert(model.size() == 1); // Should contain exactly one triple after fixing
    assert(model.contains(model.createResource("http://example.org/showcase/1"), 
                          model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"), 
                          model.createResource("https://example.org/Showcase"))); // Should contain the fixed triple
  }

  @Test
  void testRemoveUnlicensedDistribution() {
    ArrayList<String> loggerOutput = new ArrayList<String>();
    String data = """
      <http://example.org/dataset/1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/ns/dcat#Dataset> .
      <http://example.org/dataset/1> <http://www.w3.org/ns/dcat#distribution> <http://example.org/distribution/1> .
      <http://example.org/dataset/1> <http://www.w3.org/ns/dcat#distribution> <http://example.org/distribution/2> .
      <http://example.org/distribution/1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/ns/dcat#Distribution> .
      <http://example.org/distribution/1> <http://purl.org/dc/terms/license> <http://example.org/license/1> .
      <http://example.org/distribution/2> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/ns/dcat#Distribution> .
      <http://example.org/distribution/2> <http://www.w3.org/ns/dcat#byteSize> "12345"^^<http://www.w3.org/2001/XMLSchema#integer>  .
    """;
    Model model = Piveau.toModel(data.getBytes(), Lang.NTRIPLES); 
    MainVerticle.removeUnlicensedDistributions(loggerOutput::add, model);
    assert(loggerOutput.size() == 1);
    assert(loggerOutput.get(0).startsWith("Removing triples for "));

    assert(model.size() == 4);
    Resource distribution2 = model.createResource("http://example.org/distribution/2");
    assert(!model.contains(distribution2, (Property) null, (RDFNode) null)); 
    assert(!model.contains(null, null, distribution2));
  }
}
