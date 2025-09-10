// File path: src/main/java/io/piveau/importing/csw/MainVerticle.java
package io.piveau.importing.csw;

import io.piveau.pipe.connector.PipeConnector;
import io.piveau.pipe.PipeContext;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.core.Future;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import io.vertx.core.Launcher; // <-- Add this import
import java.util.Arrays;      // <-- Add this import

public class MainVerticle extends AbstractVerticle {

    private PipeConnector pipeConnector;
    private HttpClient client;

    @Override
    public void start() {
        // Initialize the PipeConnector, which connects this module to the Piveau pipeline.
        PipeConnector.create(vertx)
            .onSuccess(connector -> {
                this.pipeConnector = connector;
                // Handle a pipe instance when it is received.
                connector.handlePipe(this::handlePipe);
                System.out.println("Custom CSW Importer started successfully.");
            })
            .onFailure(Throwable::printStackTrace);

        // Initialize a standard Vert.x HTTP client for fetching data.
        HttpClientOptions options = new HttpClientOptions().setSsl(true).setTrustAll(true);
        client = vertx.createHttpClient(options);
    }

    private void handlePipe(PipeContext pipeContext) {
        // Get the URL from the pipe's configuration.
        JsonObject config = pipeContext.getConfig();
        String cswUrl = config.getString("url");
        String cqlFilter = config.getJsonObject("config", new JsonObject()).getString("cql", null);

        if (cswUrl == null) {
            pipeContext.setFailure("CSW URL is missing in the pipe configuration.");
            return;
        }

        // Construct the CSW GetRecords request URL.
        String requestUrl = cswUrl + "?service=CSW&version=2.0.2&request=GetRecords&elementsetname=full";
        if (cqlFilter != null) {
            // Note: CSW uses a filter parameter, which might need proper encoding.
            // This is a simplified example.
            requestUrl += "&constraintlanguage=CQL_TEXT&constraint=" + cqlFilter;
        }

        // Fetch the raw XML content from the CSW endpoint.
        client.request(HttpMethod.GET, requestUrl)
            .compose(request -> {
                request.setFollowRedirects(true);
                return request.send();
            })
            .compose(response -> {
                if (response.statusCode() == 200) {
                    return response.body();
                } else {
                    return Future.failedFuture("Failed to fetch data: " + response.statusMessage());
                }
            })
            .onSuccess(buffer -> {
                // Parse the XML response.
                String xmlContent = buffer.toString();
                try {
                    SAXBuilder saxBuilder = new SAXBuilder();
                    Document document = saxBuilder.build(new StringReader(xmlContent));
                    Element rootElement = document.getRootElement();

                    // Define the necessary XML namespaces for ISO 19139 and CSW.
                    Namespace cswNamespace = Namespace.getNamespace("csw", "http://www.opengis.net/cat/csw/2.0.2");
                    Namespace gmdNamespace = Namespace.getNamespace("gmd", "http://www.isotc211.org/2005/gmd");

                    // The core logic: find all <csw:Record> elements.
                    List<Element> records = rootElement.getChild("SearchResults", cswNamespace)
                                                         .getChildren("Record", cswNamespace);

                    if (records.isEmpty()) {
                        System.out.println("No records found to import.");
                        return;
                    }

                    // For each record, pass it as a separate payload to the next segment (the transformer).
                    for (Element record : records) {
                        // Extract the raw XML of the record and send it to the next pipe segment.
                        // The transformer will then convert this XML to RDF.
                        String xmlString = new XMLOutputter().outputString(record);
                        pipeContext.setResult(xmlString).forward();
                    }

                    // Complete the pipe after all records have been forwarded.

                } catch (JDOMException | IOException e) {
                    pipeContext.setFailure("Failed to parse XML response: " + e.getMessage());
                }
            })
            .onFailure(cause -> pipeContext.setFailure(cause.getMessage()));
    }


    // Setter for the HTTP client to facilitate testing with a mock client.
    public void setClient(HttpClient client) {
        this.client = client;
    }

    public static void main(String[] args) {
        String[] params = Arrays.copyOf(args, args.length + 1);
        params[params.length - 1] = MainVerticle.class.getName();
        Launcher.executeCommand("run", params);
    }
}
