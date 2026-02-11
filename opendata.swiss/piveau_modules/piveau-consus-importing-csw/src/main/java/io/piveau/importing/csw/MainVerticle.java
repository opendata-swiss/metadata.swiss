// File path: src/main/java/io/piveau/importing/csw/MainVerticle.java
package io.piveau.importing.csw;

import io.piveau.pipe.connector.PipeConnector;
import io.piveau.pipe.PipeContext;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;

import org.jdom2.output.XMLOutputter;
import org.json.JSONObject;
import org.json.XML;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.concurrent.atomic.AtomicInteger;

import io.vertx.core.Launcher;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainVerticle extends AbstractVerticle {

    // private static final String CATALOGUE_INFO_FIELD_NAME = "catalogue";

    // private PipeConnector pipeConnector;
    private static final Logger logger = LoggerFactory.getLogger(MainVerticle.class);

    @Override
    public void start() {
        // Initialize the PipeConnector, which connects this module to the Piveau
        // pipeline.
        PipeConnector.create(vertx)
                .onSuccess(connector -> {
                    // this.pipeConnector = connector;
                    // Handle a pipe instance when it is received.
                    connector.handlePipe(this::handlePipe);
                    logger.info("Custom CSW Importer started successfully.");
                })
                .onFailure(Throwable::printStackTrace);

    }

    private void importData(PipeContext pipeContext, String address, String typeNames, String catalogue) {
        AtomicInteger index = new AtomicInteger(1);
        XmlSource xmlSource = new XmlSource(address, typeNames);
        xmlSource.getRecordsStream()
            .flatMap(records -> records.stream())
            .forEach(record -> {
                ObjectNode dataInfo = new ObjectMapper().createObjectNode()
                        .put("total", xmlSource.getTotalRecords())
                        .put("current", index.getAndIncrement())
                        .put("identifier", record.getChildText("identifier", XmlSource.dcNamespace))
                        .put("catalogue", catalogue);

                String xmlString = new XMLOutputter().outputString(record);
                JSONObject jsonObject = XML.toJSONObject(xmlString);
                String jsonString = jsonObject.toString(4);

                pipeContext.setResult(jsonString, "application/json", dataInfo).forward();
                pipeContext.log().info("Dataset imported: {}", dataInfo);
            });
        if (xmlSource.getTotalRecords() == 0) {
            throw new RuntimeException("No records found to import from " + address);
        } 
        if (index.get() - 1 != xmlSource.getTotalRecords()) {
            throw new RuntimeException("Expected to import " + xmlSource.getTotalRecords() + " records, but imported " + (index.get() - 1));
        }
    }

    private void handlePipe(PipeContext pipeContext) {
        // Get the URL from the pipe's configuration.
        JsonObject config = pipeContext.getConfig();
        String cswUrl = config.getString("address");
        String catalogue = config.getString("catalogue");
        String typeNames = config.getJsonObject("config", new JsonObject()).getString("typeNames", "dcat");

        if (cswUrl == null) {
            pipeContext.setFailure("CSW URL is missing in the pipe configuration.");
            return;
        }

        try {
            importData(pipeContext, cswUrl, typeNames, catalogue);
            pipeContext.setRunFinished();
            logger.info("Data import completed successfully for catalogue: " + catalogue);

        } catch (Exception e) {
            logger.error("An error occurred during data import: " + e.getMessage(), e);
            pipeContext.setFailure("An error occurred during data import: " + e.getMessage());
        }
    }



    public static void main(String[] args) {
        String[] params = Arrays.copyOf(args, args.length + 1);
        params[params.length - 1] = MainVerticle.class.getName();
        Launcher.executeCommand("run", params);
    }
}
