package swiss.opendata.piveau.pipe.module.importing.showcases;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.piveau.pipe.PipeContext;
import io.piveau.pipe.connector.PipeConnector;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Launcher;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;
import java.util.List;

public class MainVerticle extends AbstractVerticle {

    static final String ENV_SHOWCASE_API_ENDPOINT = "SHOWCASE_API_ENDPOINT";

    // falling back to a default address (configurable via SHOWCASE_API_ENDPOINT) in case no address is set in the pipe segment configuration
    private String showcaseApiAddressDefault;


    /**
     * The main Vert.x function aka entry point to the application
     */
    @Override
    public void start(Promise<Void> startPromise) throws Exception {

        ConfigStoreOptions envStoreOptions = new ConfigStoreOptions()
                .setType("env")
                .setConfig(new JsonObject().put("keys", new JsonArray()
                        .add(ENV_SHOWCASE_API_ENDPOINT)));

        ConfigRetriever.create(vertx, new ConfigRetrieverOptions().addStore(envStoreOptions))
                .getConfig()
                .onSuccess(config -> {
                    this.showcaseApiAddressDefault = config.getString(ENV_SHOWCASE_API_ENDPOINT);
                })
                .<Void>mapEmpty()
                .onComplete(startPromise);

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

        ShowcaseApiConnector connector = ShowcaseApiConnector.create(vertx, pipeContext, showcaseApiAddressDefault);

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
