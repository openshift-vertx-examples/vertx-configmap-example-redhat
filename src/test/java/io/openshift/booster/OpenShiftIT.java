package io.openshift.booster;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.openshift.client.OpenShiftClient;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.arquillian.cube.openshift.impl.enricher.AwaitRoute;
import org.arquillian.cube.openshift.impl.enricher.RouteURL;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.core.IsEqual.equalTo;

/**
 * Check the behavior of the application when running in OpenShift.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(Arquillian.class)
public class OpenShiftIT {

    @RouteURL("${app.name}")
    @AwaitRoute
    private URL route;

    @ArquillianResource
    private OpenShiftClient oc;

    @Before
    public void setup() {
        RestAssured.baseURI = route.toString();
    }

    @Test
    public void testAThatWeAreReady() {
        await().atMost(5, TimeUnit.MINUTES).catchUncaughtExceptions().until(() -> {
            Response response = get();
            return response.getStatusCode() < 500;
        });
    }

    @Test
    public void testBThatWeServeAsExpected() {
        get("/api/greeting").then().body("content", equalTo("Hello, World from a ConfigMap !"));
        get("/api/greeting?name=vert.x").then().body("content", equalTo("Hello, vert.x from a ConfigMap !"));
    }

    @Test
    public void testCThatWeCanReloadTheConfiguration() {
        ConfigMap map = oc.configMaps().withName("app-config").get();
        assertThat(map).isNotNull();

        oc.configMaps().withName("app-config").edit()
            .addToData("app-config.yml", "message : \"Bonjour, %s from a ConfigMap !\"")
            .done();

        await().atMost(5, TimeUnit.MINUTES).catchUncaughtExceptions().until(() -> {
            Response response = get("/api/greeting");
            return response.getStatusCode() < 500 && response.asString().contains("Bonjour");
        });

        get("/api/greeting?name=vert.x").then().body("content", equalTo("Bonjour, vert.x from a ConfigMap !"));
    }

    @Test
    public void testDThatWeServeErrorWithoutConfigMap() {
        get("/api/greeting").then().statusCode(200);
        oc.configMaps().withName("app-config").delete();

        await().atMost(5, TimeUnit.MINUTES).catchUncaughtExceptions().untilAsserted(() ->
            get("/api/greeting").then().statusCode(500)
        );
    }
}
