package io.openshift.booster;

import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.config.ConfigRetriever;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.handler.StaticHandler;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import rx.Single;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

/**
 *
 */
public class HttpApplication extends AbstractVerticle {

    private ConfigRetriever conf;
    private String message;

    private static final Logger LOGGER = LogManager.getLogger(HttpApplication.class);
    private JsonObject config;

    @Override
    public void start() {
        setUpConfiguration();

        Router router = Router.router(vertx);
        router.get("/api/greeting").handler(this::greeting);
        router.get("/health").handler(rc -> rc.response().end("OK"));
        router.get("/").handler(StaticHandler.create());

        retrieveMessageTemplateFromConfiguration()
            .doOnSuccess(msg -> message = msg)
            .flatMap(s -> vertx
                .createHttpServer()
                .requestHandler(router::accept)
                .rxListen(
                    // Retrieve the port from the configuration,
                    // default to 8080.
                    config().getInteger("http.port", 8080))
            ).subscribe();


        conf.configStream().toObservable()
            .subscribe(json -> {
                LOGGER.info("New configuration retrieved: {}",
                    json.getString("message"));
                message = json.getString("message");
                String level = json.getString("level", "INFO");
                LOGGER.info("New log level: {}", level);
                setLogLevel(level);
            });

    }

    private void setLogLevel(String level) {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
        loggerConfig.setLevel(Level.getLevel(level));
        ctx.updateLoggers();
    }

    private void greeting(RoutingContext rc) {
        if (message == null) {
            rc.response().setStatusCode(500)
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(new JsonObject().put("content", "no config map").encode());
            return;
        }
        String name = rc.request().getParam("name");
        if (name == null) {
            name = "World";
        }

        LOGGER.debug("Replying to request, parameter={}", name);
        JsonObject response = new JsonObject()
            .put("content", String.format(message, name));

        rc.response()
            .putHeader("content-type", "application/json; charset=utf-8")
            .end(response.encodePrettily());
    }

    private Single<String> retrieveMessageTemplateFromConfiguration() {
        return conf.rxGetConfig()
            .map(json -> json.getString("message"))
            .onErrorReturn(t -> null);
    }

    private void setUpConfiguration() {
        ConfigStoreOptions appStore = new ConfigStoreOptions();
        appStore.setType("configmap")
            .setFormat("yaml")
            .setConfig(new JsonObject()
                .put("name", "app-config")
                .put("key", "app-config.yml"));

        conf = ConfigRetriever.create(vertx, new ConfigRetrieverOptions()
            .addStore(appStore));
    }
}