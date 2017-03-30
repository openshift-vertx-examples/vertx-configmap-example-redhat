package io.openshift.booster;

import com.jayway.restassured.response.Response;
import io.fabric8.kubernetes.api.model.ConfigMap;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.net.MalformedURLException;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.restassured.RestAssured.get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

/**
 * Check the behavior of the application when running in OpenShift.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class OpenShiftIT {

    private static OpenShiftTestAssistant assistant = new OpenShiftTestAssistant();

    @BeforeClass
    public static void prepare() throws Exception {
        assistant.deployApplication();
    }

    @AfterClass
    public static void cleanup() {
        assistant.cleanup();
    }

    @Test
    public void testAThatWeAreReady() throws Exception {
        assistant.awaitApplicationReadinessOrFail();
        await().atMost(5, TimeUnit.MINUTES).catchUncaughtExceptions().until(() -> {
            Response response = get();
            return response.getStatusCode() < 500;
        });
    }

    @Test
    public void testBThatWeServeAsExpected() throws MalformedURLException {
        get("/api/greeting").then().body("content", equalTo("Hello, World from Kubernetes ConfigMap !"));
        get("/api/greeting?name=vert.x").then().body("content", equalTo("Hello, vert.x from Kubernetes ConfigMap !"));
    }

    @Test
    public void testCThatWeCanReloadTheConfiguration() {
        ConfigMap map = assistant.client().configMaps().withName("vertx-http-configmap").get();
        assertThat(map).isNotNull();

        assistant.client().configMaps().withName("vertx-http-configmap").edit()
            .addToData("conf", "message : \"Bonjour, %s from Kubernetes ConfigMap !\"")
            .done();

        await().atMost(5, TimeUnit.MINUTES).catchUncaughtExceptions().until(() -> {
            Response response = get("/api/greeting");
            return response.getStatusCode() < 500 && response.asString().contains("Bonjour");
        });
    }

}
