/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.openshift.booster;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

/**
 *
 */
public class HttpApplication extends AbstractVerticle {

    public static final String DEFAULT_TEMPLATE = "Hello, %s!";

    private ConfigRetriever conf;
    private String message;

    private static final Logger LOGGER = LogManager.getLogger(HttpApplication.class);

    @Override
    public void start() {
        setUpConfiguration();

        Router router = Router.router(vertx);
        router.get("/api/greeting").handler(this::greeting);
        router.get("/").handler(StaticHandler.create());

        retrieveMessageTemplateFromConfiguration()
            .setHandler(ar -> {
                // Once retrieved, store it and start the HTTP server.
                message = ar.result();
                vertx
                    .createHttpServer()
                    .requestHandler(router::accept)
                    .listen(
                        // Retrieve the port from the configuration,
                        // default to 8080.
                        config().getInteger("http.port", 8080));

            });

        conf.listen(change -> {
            LOGGER.info("New configuration retrieved: {} message",
                change.getNewConfiguration().getString("message"));
            message = change.getNewConfiguration().getString("message", DEFAULT_TEMPLATE);
            String level = change.getNewConfiguration().getString("level", "INFO");
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
        String name = rc.request().getParam("name");
        if (name == null) {
            name = "World";
        }

        LOGGER.debug("Replying to request, parameter={}", name);
        JsonObject response = new JsonObject()
            .put("content", String.format(message, name));

        rc.response()
            .putHeader(CONTENT_TYPE, "application/json; charset=utf-8")
            .end(response.encodePrettily());
    }

    private Future<String> retrieveMessageTemplateFromConfiguration() {
        Future<String> future = Future.future();
        conf.getConfig(ar ->
            future.handle(ar
                .map(json -> json.getString("message", DEFAULT_TEMPLATE))
                .otherwise(t -> DEFAULT_TEMPLATE)));
        return future;
    }

    private void setUpConfiguration() {
        ConfigStoreOptions appStore = new ConfigStoreOptions();
        appStore.setType("configmap")
            .setFormat("yaml")
            .setConfig(new JsonObject()
                .put("name", "vertx-http-configmap")
                .put("key", "conf"));

        conf = ConfigRetriever.create(vertx, new ConfigRetrieverOptions()
            .addStore(appStore));
    }
}