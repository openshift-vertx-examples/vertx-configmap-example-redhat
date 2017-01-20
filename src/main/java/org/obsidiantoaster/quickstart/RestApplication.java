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

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.example.util.Runner;
import io.vertx.ext.configuration.ConfigurationRetriever;
import io.vertx.ext.configuration.ConfigurationRetrieverOptions;
import io.vertx.ext.configuration.ConfigurationStoreOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
/**
 *
 */
public class RestApplication extends AbstractVerticle {

    private static final Logger LOG = LoggerFactory.getLogger(RestApplication.class);
    public static final String DEFAULT_TEMPLATE = "Hello, %s!";

    // Convenience method so you can run it in your IDE
    public static void main(String[] args) {
        Runner.runExample(RestApplication.class);
    }

    private ConfigurationRetriever conf;
    private String template;
    private long counter;


    @Override
    public void start() {
        setUpConfiguration();

        Router router = Router.router(vertx);
        router.get("/greeting").handler(this::greeting);

        // Create the HTTP server and pass the "accept" method to the request handler.
        vertx
                .createHttpServer()
                .requestHandler(router::accept)
                .listen(
                        // Retrieve the port from the configuration,
                        // default to 8080.
                        config().getInteger("http.port", 8080));

        conf.getConfiguration(ar -> {
            if (ar.succeeded()) {
                template = ar.result().getString("template", DEFAULT_TEMPLATE);
                LOG.info("ConfigMap -> tempate : " + template);
            } else {
                template = DEFAULT_TEMPLATE;
                ar.cause().printStackTrace();
            }
        });

        conf.listen((configuration -> {
            LOG.info("Configuration change: " + configuration.toJson().encodePrettily());

            JsonObject newConfiguration = configuration.getNewConfiguration();
            template = newConfiguration.getString("template", DEFAULT_TEMPLATE);
            LOG.info("Template has changed: " + template);
        }));

    }

    private void greeting(RoutingContext context) {
        String name = context.request().getParam("name");
        if (name == null) {
            name = "World";
        }
        context.response()
                .putHeader(CONTENT_TYPE, "application/json; charset=utf-8")
                .end(Json.encode(new Greeting(++counter, String.format(template, name))));
    }

    private void setUpConfiguration() {
        ConfigurationStoreOptions appStore = new ConfigurationStoreOptions();
        appStore.setType("configmap")
                .setConfig(new JsonObject()
                        .put("namespace", "vertx-demo")
                        .put("name", "app-config")
                        .put("key", "app.json"));

        conf = ConfigurationRetriever.create(vertx, new ConfigurationRetrieverOptions()
                .addStore(appStore));
    }
}