// File path: src/main/java/io/piveau/importing/csw/MainVerticle.java
package io.piveau.importing.csw;

import io.piveau.pipe.connector.PipeConnector;
import io.piveau.pipe.PipeContext;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import io.vertx.core.Launcher;
import java.util.Arrays;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;




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

    }

    private void handlePipe(PipeContext pipeContext) {
        // Get the URL from the pipe's configuration.
        JsonObject config = pipeContext.getConfig();
        String cswUrl = config.getString("address");
        String cqlFilter = config.getJsonObject("config", new JsonObject()).getString("cql", null);
        String typeNames = config.getJsonObject("config", new JsonObject()).getString("typeNames", "dcat");

        if (cswUrl == null) {
            pipeContext.setFailure("CSW URL is missing in the pipe configuration.");
            return;
        }

        String requestUrl = cswUrl + "?service=CSW&version=2.0.2&request=GetRecords&elementsetname=full&resultType=results";
        requestUrl += "&typeNames=" + typeNames;

        if (cqlFilter != null) {
            // Note: CSW uses a filter parameter, which might need proper encoding.
            // This is a simplified example.
            requestUrl += "&constraintlanguage=CQL_TEXT&constraint=" + cqlFilter;
        }

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(requestUrl))
                .build();

        System.out.println(requestUrl);
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("Successfully fetched XML:");
                System.out.println("-------------------------");

                try {

                    String xmlContent = response.body();
                    SAXBuilder saxBuilder = new SAXBuilder();
                    Document document = saxBuilder.build(new StringReader(xmlContent));
                    Element rootElement = document.getRootElement();

                    Namespace cswNamespace = Namespace.getNamespace("csw", "http://www.opengis.net/cat/csw/2.0.2");

                    // This assumes the records are under <csw:GetRecordsResponse>/<csw:SearchResults>/<csw:Record>
                    List<Element> records = rootElement.getChild("SearchResults", cswNamespace)
                                                         .getChildren("Record", cswNamespace);

                    if (records.isEmpty()) {
                        System.out.println("No records found to import.");
                        return;
                    }

                    System.out.println("Found " + records.size() + " records to forward.");

                    for (Element record : records) {
                        String xmlString = new XMLOutputter().outputString(record);
                        JSONObject jsonObject = XML.toJSONObject(xmlString);
                        String jsonString = jsonObject.toString(4);

                        pipeContext.setResult(jsonString).forward();
                    }

                } catch (JDOMException | IOException e) {
                    pipeContext.setFailure("Failed to parse XML response: " + e.getMessage());
                } catch (JSONException e) {
                    pipeContext.setFailure("Failed to convert XML to JSON: " + e.getMessage());
                }


            } else {
                System.err.println("Error: Received status code " + response.statusCode());
                System.err.println("Response Body:");
                System.err.println(response.body());
            }

        } catch (Exception e) {
            System.err.println("An error occurred during the HTTP request:");
            e.printStackTrace();
        }

        //client.request(HttpMethod.GET, requestUrl)
        //    .compose(request -> {
        //        request.setFollowRedirects(false);
        //        return request.send();
        //    })
        //    .compose(response -> {
        //        if (response.statusCode() == 200) {
        //            return response.body();
        //        } else {
        //            return Future.failedFuture("Failed to fetch data: " + response.statusMessage());
        //        }
        //    })
        //    .onSuccess(buffer -> {
        //        // Parse the XML response.
        //        String xmlContent = buffer.toString();
        //        try {
        //            SAXBuilder saxBuilder = new SAXBuilder();
        //            Document document = saxBuilder.build(new StringReader(xmlContent));
        //            Element rootElement = document.getRootElement();
//
        //            // Define the necessary XML namespaces for ISO 19139 and CSW.
        //            Namespace cswNamespace = Namespace.getNamespace("csw", "http://www.opengis.net/cat/csw/2.0.2");
        //            Namespace gmdNamespace = Namespace.getNamespace("gmd", "http://www.isotc211.org/2005/gmd");
//
        //            // The core logic: find all <csw:Record> elements.
        //            List<Element> records = rootElement.getChild("SearchResults", cswNamespace)
        //                                                 .getChildren("Record", cswNamespace);
//
        //            if (records.isEmpty()) {
        //                System.out.println("No records found to import.");
        //                return;
        //            }
//
        //            // For each record, pass it as a separate payload to the next segment (the transformer).
        //            for (Element record : records) {
        //                // Extract the raw XML of the record and send it to the next pipe segment.
        //                // The transformer will then convert this XML to RDF.
        //                String xmlString = new XMLOutputter().outputString(record);
        //                pipeContext.setResult(xmlString).forward();
        //            }
//
        //            // Complete the pipe after all records have been forwarded.
//
        //        } catch (JDOMException | IOException e) {
        //            pipeContext.setFailure("Failed to parse XML response: " + e.getMessage());
        //        }
        //    })
        //    .onFailure(cause -> pipeContext.setFailure(cause.getMessage()));
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
