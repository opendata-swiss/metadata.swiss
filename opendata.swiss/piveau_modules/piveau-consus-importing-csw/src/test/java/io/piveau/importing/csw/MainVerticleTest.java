// package io.piveau.importing.csw;

// import io.piveau.pipe.PipeContext;
// import io.vertx.core.Vertx;
// import io.vertx.core.json.JsonObject;
// import io.vertx.junit5.VertxExtension;
// import io.vertx.junit5.VertxTestContext;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.ArgumentCaptor;

// import java.io.IOException;
// import java.net.http.HttpClient;
// import java.net.http.HttpRequest;
// import java.net.http.HttpResponse;
// import java.nio.file.Files;
// import java.nio.file.Paths;

// import static org.junit.jupiter.api.Assertions.assertTrue;
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.Mockito.*;

// @ExtendWith(VertxExtension.class)
// class MainVerticleTest {

//     private MainVerticle verticle;

//     // This setup method will now compile correctly. It deploys the verticle before each test.
//     @BeforeEach
//     void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
//         verticle = new MainVerticle();
//         vertx.deployVerticle(verticle, testContext.succeeding(id -> testContext.completeNow()));
//     }

//     @Test
//     @DisplayName("Test that the verticle correctly parses a CSW response and forwards two records")
//     void testHandlePipe(Vertx vertx, VertxTestContext testContext) throws IOException, InterruptedException {
//         // --- 1. ARRANGE (Setup Mocks and Data) ---

//         // Mock the PipeContext and its logger
//         PipeContext mockPipeContext = mock(PipeContext.class);
//         Log mockLogger = mock(Log.class); // Mock the logger object
//         JsonObject config = new JsonObject()
//                 .put("address", "http://mock-csw-server.com/csw")
//                 .put("catalogue", "test-catalogue");

//         // Configure mock behavior
//         when(mockPipeContext.getConfig()).thenReturn(config);
//         when(mockPipeContext.log()).thenReturn(mockLogger); // Ensure .log() returns the mock logger
//         when(mockPipeContext.setResult(anyString(), anyString(), any(JsonObject.class))).thenReturn(mockPipeContext);

//         // Read the mock XML response from a file
//         String xmlContent = new String(Files.readAllBytes(Paths.get("src/test/resources/csw-response.xml")));

//         // Mock the Java 11+ HttpClient and its response
//         HttpClient mockClient = mock(HttpClient.class);
//         @SuppressWarnings("unchecked")
//         HttpResponse<String> mockResponse = mock(HttpResponse.class);

//         // Configure the mock client to return our mock response
//         when(mockClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);
//         when(mockResponse.statusCode()).thenReturn(200);
//         when(mockResponse.body()).thenReturn(xmlContent);


//         // --- 2. ACT (Execute the method under test) ---

//         // Inject the mock client into our verticle instance
//         verticle.setClient(mockClient);

//         // Directly invoke the private handlePipe method for testing purposes
//         // Note: For this to work, you may need to change `handlePipe` from `private` to package-private (no modifier).
//         verticle.handlePipe(mockPipeContext);


//         // --- 3. ASSERT (Verify the results) ---

//         // Use an ArgumentCaptor to capture the JSON string sent to setResult
//         ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

//         // Verify that forward() was called exactly twice
//         verify(mockPipeContext, timeout(1000).times(2)).forward();
//         verify(mockPipe_context, timeout(1000).times(2)).setResult(captor.capture(), anyString(), any(JsonObject.class));

//         // Get the captured values
//         String firstRecordJson = captor.getAllValues().get(0);
//         String secondRecordJson = captor.getAllValues().get(1);

//         // Assert that the captured JSON contains the correct data
//         assertTrue(firstRecordJson.contains("First Test Record"));
//         assertTrue(secondRecordJson.contains("Second Test Record"));

//         // Signal that the test has completed successfully
//         testContext.completeNow();
//     }
// }
