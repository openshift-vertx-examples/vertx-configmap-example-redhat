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
package org.obsidiantoaster.quickstart;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

/**
 *
 */
public class RestApplication extends AbstractVerticle {

    public static final String DEFAULT_TEMPLATE = "Hello, %s!";

    private ConfigRetriever conf;
    private String message;
    private long counter;

    @Override
    public void start() {
        setUpConfiguration();

        Router router = Router.router(vertx);
        router.get("/greeting").handler(this::greeting);

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
            System.out.println("New configuration retrieved: " + change.getNewConfiguration().getString("message"));
            message = change.getNewConfiguration().getString("message", DEFAULT_TEMPLATE);
        });
    }

    private void greeting(RoutingContext context) {
        String name = context.request().getParam("name");
        if (name == null) {
            name = "World";
        }
        context.response()
            .putHeader(CONTENT_TYPE, "application/json; charset=utf-8")
            .end(Json.encode(new Greeting(++counter, String.format(message, name))));
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
            .setConfig(new JsonObject()
                .put("name", "vertx-rest-configmap")
                .put("key", "app.json"));

        conf = ConfigRetriever.create(vertx, new ConfigRetrieverOptions()
            .addStore(appStore));
    }
}