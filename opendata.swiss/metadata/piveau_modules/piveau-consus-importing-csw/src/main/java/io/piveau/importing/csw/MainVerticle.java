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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

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

    private static final String CATALOGUE_INFO_FIELD_NAME = "catalogue";

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

        Integer startPosition = 1;
        Integer totalRecords = -1; // Sentinel value: not yet known
        Integer recordsFetched = 0;

        HttpClient client = HttpClient.newHttpClient();

        do {
            String requestUrl = cswUrl + "?service=CSW&version=2.0.2&request=GetRecords&elementsetname=full&resultType=results";

            requestUrl += "&typeNames=" + typeNames;
            requestUrl += "&startPosition=" + startPosition;

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
                        Namespace dcNamespace = Namespace.getNamespace("dc", "http://purl.org/dc/elements/1.1/");

                        // This assumes the records are under <csw:GetRecordsResponse>/<csw:SearchResults>
                        Element records = rootElement.getChild("SearchResults", cswNamespace);

                        if (totalRecords == -1) { // This only runs on the first iteration
                            totalRecords = records.getAttributeValue("numberOfRecordsMatched") != null ?
                                        Integer.parseInt(records.getAttributeValue("numberOfRecordsMatched")) : 0;
                            System.out.println("Total records to import: " + totalRecords);

                            if (totalRecords == 0) {
                                System.out.println("No records found to import.");
                                break;
                            }
                        }

                        System.out.println("Found " + totalRecords + " records to forward.");


                        List<Element> recordsList = records.getChildren("Record", cswNamespace);
                        Integer recordsOnThisPage = recordsList.size();

                        recordsFetched += recordsOnThisPage;
                        startPosition += recordsOnThisPage;

                        int counter = 0;
                        for (Element record : recordsList) {
                            ObjectNode dataInfo = new ObjectMapper().createObjectNode()
                                .put("total", totalRecords)
                                .put("current", startPosition + counter)
                                .put("identifier", record.getChildText("identifier", dcNamespace))
                                .put(CATALOGUE_INFO_FIELD_NAME, pipeContext.getConfig().getString(CATALOGUE_INFO_FIELD_NAME));

                            String xmlString = new XMLOutputter().outputString(record);
                            JSONObject jsonObject = XML.toJSONObject(xmlString);
                            String jsonString = jsonObject.toString(4);

                            pipeContext.setResult(jsonString, "application/json", dataInfo).forward();
                            pipeContext.log().info("Dataset imported: {}", dataInfo);

                            counter++;

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

        } while (totalRecords > 0 && recordsFetched < totalRecords);


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
