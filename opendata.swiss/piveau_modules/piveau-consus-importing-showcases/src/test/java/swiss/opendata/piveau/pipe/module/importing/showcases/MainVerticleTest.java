package swiss.opendata.piveau.pipe.module.importing.showcases;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class MainVerticleTest {

    @Test
    public void testMainVerticleDeployment(Vertx vertx, VertxTestContext testContext) {
        vertx.deployVerticle(MainVerticle.class.getName())
                .onComplete(testContext.succeeding(id -> testContext.completeNow()));
    }
}
