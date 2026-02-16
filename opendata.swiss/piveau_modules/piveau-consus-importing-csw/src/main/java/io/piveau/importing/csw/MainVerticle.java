package io.piveau.importing.csw;

import io.piveau.pipe.connector.PipeConnector;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Launcher;
import io.vertx.core.Promise;
import io.vertx.core.ThreadingModel;

import java.util.Arrays;

public class MainVerticle extends AbstractVerticle {

    @Override
    public void start(Promise<Void> startPromise) {
        DeploymentOptions options = new DeploymentOptions();
        // options.setWorker(true);
        options.setThreadingModel(ThreadingModel.WORKER);
        vertx.deployVerticle(ImportingCswVerticle.class, options)
                .compose(id -> PipeConnector.create(vertx))
                .onSuccess(connector -> {
                    connector.publishTo(ImportingCswVerticle.ADDRESS, false);
                    startPromise.complete();
                })
                .onFailure(startPromise::fail);
    }

    public static void main(String[] args) {
        String[] params = Arrays.copyOf(args, args.length + 1);
        params[params.length - 1] = MainVerticle.class.getName();
        Launcher.executeCommand("run", params);
    }

}
