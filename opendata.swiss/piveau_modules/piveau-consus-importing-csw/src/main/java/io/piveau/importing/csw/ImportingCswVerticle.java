// File path: src/main/java/io/piveau/importing/csw/ImportingCswVerticle.java
package io.piveau.importing.csw;

import io.piveau.pipe.PipeContext;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.core.Promise;
import io.vertx.core.Future;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.core.eventbus.Message;
import org.apache.jena.riot.Lang;
import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;


public class ImportingCswVerticle extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(ImportingCswVerticle.class);

    public static final String ADDRESS = "io.piveau.pipe.importing.csw.queue";

    @Override
    public void start(Promise<Void> startPromise) {
        vertx.eventBus().consumer(ADDRESS, this::handlePipe);
        logger.info("Custom CSW Importer started successfully.");
        startPromise.complete();
    }

    private Future<Integer> importData(PipeContext pipeContext, String address, String catalogue) {
        
        XmlSource xmlSource = new XmlSource(address);
        Iterator<Element> iterator = xmlSource.getRecordsStream()
            .flatMap(records -> records.stream())
            .iterator();

        Promise<Integer> promise = Promise.promise();
        AtomicInteger index = new AtomicInteger(1);
        // consider using vert.x ConfigRetriever
        String delayEnv = System.getenv("CSW_IMPORT_DELAY");
        long delay = delayEnv != null ? Long.parseLong(delayEnv) : 100;
        logger.info("Starting data import with a delay of {} ms between records", delay);
        vertx.setPeriodic(delay, id -> { // wait between each record to avoid overwhelming the system
            if (iterator.hasNext()) {
                Element record = iterator.next();
                String result = new XMLOutputter().outputString(record);
                String dataMimeType = Lang.RDFXML.getHeaderString();
                ObjectNode dataInfo = new ObjectMapper().createObjectNode()
                    .put("content", "dcatResource")
                    .put("total", xmlSource.getTotalRecords())
                    .put("current", index.getAndIncrement())
                    .put("identifier", getIdentifier(record))
                    .put("catalogue", catalogue);
                pipeContext.setResult(result, dataMimeType, dataInfo).forward();
                pipeContext.log().info("Dataset imported: {}", dataInfo);
            } else {
                vertx.cancelTimer(id); // stop when done
                int importedRecords = index.get() - 1;
                if (xmlSource.getTotalRecords() == 0) {
                    promise.fail("No records found to import from " + address);
                } else if (importedRecords != xmlSource.getTotalRecords()) {
                    promise.fail("Expected to import " + xmlSource.getTotalRecords() + " records, but imported " + importedRecords);
                } else {
                    promise.complete(importedRecords);
                }
            }
        });

        return promise.future();
    }

    private String getIdentifier(Element record) {  
        Element dataset = record.getChild("Dataset", XmlSource.dcatNamespace);
        if (dataset == null) {
            logger.warn("Dataset element not found in record, returning empty string");
            return "";
        }
        String identifier = dataset.getChildText("identifier", XmlSource.dctNamespace);
        if (identifier == null) {
            logger.warn("Identifier not found in record, returning empty string");
            return "";
        }
        return identifier;
    }

    private void handlePipe(Message<PipeContext> message) {
        PipeContext pipeContext = message.body();
        // Get the URL from the pipe's configuration.
        JsonObject config = pipeContext.getConfig();
        String cswUrl = config.getString("address");
        String catalogue = config.getString("catalogue");

        logger.info("Starting data import for catalogue: {} from CSW URL: {}", catalogue, cswUrl);

        if (cswUrl == null) {
            pipeContext.setFailure("CSW URL is missing in the pipe configuration.");
            return;
        }

        Future<Integer> importDataFuture = importData(pipeContext, cswUrl, catalogue);
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
