package io.piveau.importing.csw;

import io.piveau.pipe.PipeContext;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(VertxExtension.class)
class MainVerticleTest {

    private MainVerticle verticle;

    @BeforeEach
    void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
        verticle = new MainVerticle();
        vertx.deployVerticle(verticle, testContext.succeeding(id -> testContext.completeNow()));
    }

    @Test
    @DisplayName("Test that the verticle correctly parses a CSW response and forwards two records")
    void testHandlePipe(Vertx vertx, VertxTestContext testContext) throws IOException {
        // --- 1. MOCK the PipeContext ---
        PipeContext mockPipeContext = mock(PipeContext.class);
        JsonObject config = new JsonObject().put("url", "http://localhost:8080/csw");
        when(mockPipeContext.getConfig()).thenReturn(config);
        when(mockPipeContext.setResult(anyString())).thenReturn(mockPipeContext);

        // --- 2. MOCK the HTTP Client and its Response Chain ---
        String xmlContent = new String(Files.readAllBytes(Paths.get("src/test/resources/csw-response.xml")));

        // Create mocks for each step of the asynchronous HTTP call
        HttpClient mockClient = mock(HttpClient.class);
        HttpClientRequest mockRequest = mock(HttpClientRequest.class);
        HttpClientResponse mockResponse = mock(HttpClientResponse.class);

        // Configure the mock's behavior:
        // When request() is called, return a successful Future containing our mockRequest.
        when(mockClient.request(any(RequestOptions.class))).thenReturn(Future.succeededFuture(mockRequest));

        // When send() is called on the mockRequest, return a successful Future with our mockResponse.
        when(mockRequest.send()).thenReturn(Future.succeededFuture(mockResponse));

        // When body() is called on the mockResponse, return a successful Future with our XML content.
        when(mockResponse.body()).thenReturn(Future.succeededFuture(Buffer.buffer(xmlContent)));

        // When statusCode() is called, return 200 OK.
        when(mockResponse.statusCode()).thenReturn(200);


        // --- 3. INJECT the Mock Client and EXECUTE ---
        // Use the setClient method you created to inject the mock.
        verticle.setClient(mockClient);

        // Execute the method we want to test.
        verticle.handlePipe(mockPipeContext);

        // --- 4. VERIFY the results ---
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        // Verify that setResult and forward were called twice, and capture the arguments.
        verify(mockPipeContext, timeout(1000).times(2)).setResult(captor.capture());
        verify(mockPipe-context, timeout(1000).times(2)).forward();

        String firstRecord = captor.getAllValues().get(0);
        String secondRecord = captor.getAllValues().get(1);

        assertTrue(firstRecord.contains("First Test Record"));
        assertTrue(secondRecord.contains("Second Test Record"));

        testContext.completeNow();
    }
}
