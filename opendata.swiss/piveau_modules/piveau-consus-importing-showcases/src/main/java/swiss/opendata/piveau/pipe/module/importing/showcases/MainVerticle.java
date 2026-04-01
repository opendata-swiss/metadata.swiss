package swiss.opendata.piveau.pipe.module.importing.showcases;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.piveau.pipe.PipeContext;
import io.piveau.pipe.connector.PipeConnector;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Launcher;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;
import java.util.List;

public class MainVerticle extends AbstractVerticle {

    /**
     * The main Vert.x function aka entry point to the application
     */
    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        PipeConnector.create(vertx)
                .onSuccess(connector -> connector.handlePipe(this::handlePipe))
                .<Void>mapEmpty()
                .onComplete(startPromise);
    }

    private void handlePipe(PipeContext pipeContext) {
        if (pipeContext.log().isTraceEnabled()) {
            pipeContext.log().trace(pipeContext.getPipeManager().prettyPrint());
        }

        JsonObject config = pipeContext.getConfig();

        pipeContext.log().info("Import started.");

        ShowcaseApiConnector connector = ShowcaseApiConnector.create(vertx, pipeContext);

        Future.future(connector::fetchAll).onSuccess(v -> {
            List<String> identifiers = connector.getIdentifiers();
            ObjectNode info = new ObjectMapper().createObjectNode()
                    .put("content", "identifierList")
                    .put("catalogue", config.getString("catalogue"));

            pipeContext.setResult(new JsonArray(identifiers).encodePrettily(), "application/json", info)
                    .forward();

            pipeContext.log().info("Import showcases finished (count: {})", identifiers.size());

            pipeContext.succeed();
        }).onFailure(cause -> pipeContext.log().error("Import showcases failed", cause.getCause()));
    }

    /**
     * This is an optional function which is handy if you want to start your app
     * form an IDE.
     *
     * @param args
     */
    public static void main(String[] args) {
        String[] params = Arrays.copyOf(args, args.length + 1);
        params[params.length - 1] = MainVerticle.class.getName();
        Launcher.executeCommand("run", params);
    }

}
