package io.openshift.booster;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectReference;
import io.fabric8.kubernetes.api.model.ObjectReferenceBuilder;
import io.fabric8.openshift.api.model.Role;
import io.fabric8.openshift.api.model.RoleBinding;
import io.fabric8.openshift.client.*;
import org.arquillian.cube.openshift.impl.enricher.RouteURL;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.restassured.RestAssured.get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

/**
 * Check the behavior of the application when running in OpenShift.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(Arquillian.class)
public class OpenShiftIT {

    private final String applicationName = "configmap-vertx";

    @RouteURL(applicationName)
    private URL route;

    @ArquillianResource
    private OpenShiftClient oc;

    @Before
    public void setup() {
        RestAssured.baseURI = route.toString();
//
//        System.out.println("\nTrying to add a new role binding...");
//
//        if (oc.roleBindings().inNamespace(oc.getNamespace()).withName("view").get() == null) {
//            RoleBinding rb = oc.roleBindings()
//                .inNamespace(oc.getNamespace())
//                .createNew()
//                .withNewMetadata()
//                .withName("view")
//                .endMetadata()
//                .withNewRoleRef()
//                .withName("view")
//                .endRoleRef()
//                .done();
//
//            addSubjectToRoleBinding(rb, "ServiceAccount", "default", oc.getNamespace());
//            addUserNameToRoleBinding(rb, String.format("system:serviceaccount:%s:default", oc.getNamespace()));
//            oc.roleBindings().replace(rb);
//            System.out.println("\nBinding is:\n" + rb.toString());
//        } else {
//            RoleBinding binding = oc.roleBindings()
//                .withName("view")
//                .get();
//            addSubjectToRoleBinding(binding, "ServiceAccount", "default", oc.getNamespace());
//            addUserNameToRoleBinding(binding, String.format("system:serviceaccount:%s:default", oc.getNamespace()));
//            oc.roleBindings().replace(binding);
//            System.out.println("\nBinding replaced:\n" + binding.toString());
//        }
    }

    @Test
    public void testAThatWeAreReady() throws Exception {
        await().atMost(5, TimeUnit.MINUTES).catchUncaughtExceptions().until(() -> {
            Response response = get();
            return response.getStatusCode() < 500;
        });
    }

    @Test
    public void testBThatWeServeAsExpected() throws MalformedURLException {
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

        await().atMost(5, TimeUnit.MINUTES).catchUncaughtExceptions().until(() ->
            get("/api/greeting").then().statusCode(500)
        );
    }

    private void addSubjectToRoleBinding(RoleBinding roleBinding, String entityKind, String entityName, String namespace) {
        ObjectReference subject = new ObjectReferenceBuilder().withKind(entityKind).withName(entityName).withNamespace(namespace).build();

        if(roleBinding.getSubjects().stream().noneMatch(x -> x.getName().equals(subject.getName()) && x.getKind().equals(subject.getKind()))) {
            roleBinding.getSubjects().add(subject);
        }
    }

    private void addUserNameToRoleBinding(RoleBinding roleBinding, String userName) {
        if( roleBinding.getUserNames() == null) {
            roleBinding.setUserNames(new ArrayList<>());
        }
        if( !roleBinding.getUserNames().contains(userName)) {
            roleBinding.getUserNames().add(userName);
        }
    }
}
