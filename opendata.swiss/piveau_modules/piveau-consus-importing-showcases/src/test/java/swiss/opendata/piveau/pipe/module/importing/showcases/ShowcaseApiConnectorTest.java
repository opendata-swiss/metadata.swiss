package swiss.opendata.piveau.pipe.module.importing.showcases;

import io.piveau.pipe.PipeContext;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(VertxExtension.class)
public class ShowcaseApiConnectorTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    PipeContext pipeContext;

    HttpServer server;

    @BeforeEach
    public void setUp(Vertx vertx, VertxTestContext testContext) {
        MockitoAnnotations.openMocks(this);

        vertx.createHttpServer().requestHandler(req -> {
            try {
                java.io.InputStream in = getClass().getResourceAsStream("/showcase-2.jsonld");
                String content = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                req.response().putHeader("content-type", "application/ld+json").end(content);
            } catch (Exception e) {
                req.response().setStatusCode(500).end();
            }
        }).listen(0).onComplete(testContext.succeeding(httpServer -> {
            this.server = httpServer;
            int port = httpServer.actualPort();
            when(pipeContext.getConfig()).thenReturn(new JsonObject()
                    .put("catalogue", "test-catalogue")
                    .put("address", "http://127.0.0.1:" + port + "/showcase-2.jsonld"));
            testContext.completeNow();
        }));
    }

    @AfterEach
    public void tearDown(Vertx vertx, VertxTestContext testContext) {
        if (server != null) {
            server.close().onComplete(testContext.succeeding(v -> testContext.completeNow()));
        } else {
            testContext.completeNow();
        }
    }

    @Test
    public void testFetchAll(Vertx vertx, VertxTestContext testContext) {
        ShowcaseApiConnector connector = ShowcaseApiConnector.create(vertx, pipeContext);

        Promise<Void> promise = Promise.promise();
        promise.future().onComplete(testContext.succeeding(v -> {
            testContext.verify(() -> {
                List<String> identifiers = connector.getIdentifiers();
                assertNotNull(identifiers);
                assertFalse(identifiers.isEmpty());

                assertTrue(identifiers.contains("mietpreisentwicklung-in-bern"));
                assertTrue(identifiers.contains("weg-der-vielfalt"));

                // Verify that setResult was called
                verify(pipeContext, atLeastOnce()).setResult(anyString(), anyString(), any(com.fasterxml.jackson.databind.node.ObjectNode.class));

                testContext.completeNow();
            });
        }));

        connector.fetchAll(promise);
    }
}
