package swiss.opendata.piveau.pipe.module.importing.showcases;

import io.piveau.pipe.PipeContext;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(VertxExtension.class)
@Disabled
public class ShowcaseApiConnectorLocalRunTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    PipeContext pipeContext;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        when(pipeContext.getConfig()).thenReturn(new JsonObject()
                .put("catalogue", "test-catalogue")
                .put("address", "https://piveau.int.ods.zazukoians.org/api/showcases"));
    }

    @Test
    public void testFetchAllLive(Vertx vertx, VertxTestContext testContext) {
        ShowcaseApiConnector connector = ShowcaseApiConnector.create(vertx, pipeContext);

        Promise<Void> promise = Promise.promise();
        promise.future().onComplete(testContext.succeeding(v -> {
            testContext.verify(() -> {
                List<String> identifiers = connector.getIdentifiers();

                assertNotNull(identifiers, "Identifiers should not be null");
                assertFalse(identifiers.isEmpty(), "Live endpoint should return at least one showcase");

                System.out.println("Successfully processed " + identifiers.size() + " showcases from public live endpoint.");

                // Verify that setResult was called
                verify(pipeContext, atLeastOnce()).setResult(anyString(), anyString(), any(com.fasterxml.jackson.databind.node.ObjectNode.class));

                testContext.completeNow();
            });
        }));

        connector.fetchAll(promise);
    }
}
