// File path: src/main/java/io/piveau/importing/csw/ImportingCswVerticle.java
package io.piveau.importing.csw;

import io.piveau.pipe.PipeContext;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.core.Promise;
import io.vertx.core.Future;

import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;
import org.json.JSONObject;
import org.json.XML;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.concurrent.atomic.AtomicInteger;

import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.eventbus.Message;


public class ImportingCswVerticle extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(ImportingCswVerticle.class);

    public static final String ADDRESS = "io.piveau.pipe.importing.csw.queue";

    @Override
    public void start(Promise<Void> startPromise) {
        vertx.eventBus().consumer(ADDRESS, this::handlePipe);
        logger.info("Custom CSW Importer started successfully.");
        startPromise.complete();
    }

    private Future<Integer> importData(PipeContext pipeContext, String address, String typeNames, String catalogue) {
        
        XmlSource xmlSource = new XmlSource(address, typeNames);
        Iterator<JSONObject> iterator = xmlSource.getRecordsStream()
            .flatMap(records -> records.stream())
            .map(record -> convertToJSON(record))
            .iterator();

        Promise<Integer> promise = Promise.promise();
        AtomicInteger index = new AtomicInteger(1);
        vertx.setPeriodic(300, id -> { // wait between each record to avoid overwhelming the system
            if (iterator.hasNext()) {
                JSONObject jsonObject = iterator.next();
                ObjectNode dataInfo = new ObjectMapper().createObjectNode()
                    .put("total", xmlSource.getTotalRecords())
                    .put("current", index.getAndIncrement())
                    .put("identifier", getIdentifier(jsonObject))
                    .put("catalogue", catalogue);
                String jsonString = jsonObject.toString(4);
                pipeContext.setResult(jsonString, "application/json", dataInfo).forward();
                pipeContext.log().info("Dataset imported: {}", dataInfo);
            } else {
                vertx.cancelTimer(id); // stop when done

                if (xmlSource.getTotalRecords() == 0) {
                    promise.fail("No records found to import from " + address);
                } 
                int importedRecords = index.get() - 1;
                if (importedRecords != xmlSource.getTotalRecords()) {
                    promise.fail("Expected to import " + xmlSource.getTotalRecords() + " records, but imported " + importedRecords);
                }
                promise.complete(importedRecords);
            }
        });

        return promise.future();
    }

    private String getIdentifier(JSONObject jsonObject) {
       return jsonObject.getJSONObject("csw:Record").getString("dc:identifier");
    }

    private JSONObject convertToJSON(Element record) {
        String xmlString = new XMLOutputter().outputString(record);
        return XML.toJSONObject(xmlString);
    }

    private void handlePipe(Message<PipeContext> message) {
        PipeContext pipeContext = message.body();
        // Get the URL from the pipe's configuration.
        JsonObject config = pipeContext.getConfig();
        String cswUrl = config.getString("address");
        String catalogue = config.getString("catalogue");
        String typeNames = config.getJsonObject("config", new JsonObject()).getString("typeNames", "dcat");

        logger.info("Starting {} data import for catalogue: {} from CSW URL: {}", typeNames, catalogue, cswUrl);

        if (cswUrl == null) {
            pipeContext.setFailure("CSW URL is missing in the pipe configuration.");
            return;
        }

        Future<Integer> importDataFuture = importData(pipeContext, cswUrl, typeNames, catalogue);
        importDataFuture.onFailure(e -> {
            logger.error("Data import failed for catalogue: {} with error: {}", catalogue, e.getMessage(), e);
            pipeContext.setFailure("Data import failed: " + e.getMessage()).forward();
            
        });
        importDataFuture.onSuccess(result -> {
            logger.info("Data import of {} records completed successfully for catalogue: {}", result, catalogue);
            pipeContext.setRunFinished();
        });
    }
}
