package life.genny.bridge;


import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.Future;
import life.genny.channel.Routers;
import life.genny.cluster.Cluster;
import life.genny.security.SecureResources;

public class ServiceVerticle extends AbstractVerticle {

  @Override
  public void start() {
    System.out.println("Setting up routes");
    final Future<Void> startFuture = Future.future();
    Cluster.joinCluster().compose(res -> {
      final Future<Void> fut = Future.future();
      SecureResources.setKeycloakJsonMap().compose(p -> {
    	Routers.routers(vertx);
        BridgeRouters.routers(vertx);
        Routers.activate(vertx);
        System.out.println("Bridge now ready");
        fut.complete();
      }, fut);
      EBCHandlers.registerHandlers();
      startFuture.complete();
    }, startFuture);
    
  }
}
