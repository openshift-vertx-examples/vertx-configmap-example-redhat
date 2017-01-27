# Instructions

Quickstart where the Vert.x container retrieves the Application parameters using a Kubernetes ConfigMap. 
For this quickstart, we will instantiate a Vertx HTTP Server where the port number has been defined using a Kubernetes configMap using this name `app-config`. 

```java
 ConfigurationStoreOptions appStore = new ConfigurationStoreOptions();
 appStore.setType("configmap")
         .setConfig(new JsonObject()
                 .put("namespace", "vertx-demo")
                 .put("name", "app-config")); / Name of the ConfigMap to be fetched 

```

The config map contains under the `app.json` key, the list of the key/value pairs defined 
using JSon as Dataformat for our application as you can see hereafter :

```yml
apiVersion: v1
data:
  app.json: |-
    {
        "template":"From config ==> Hello %s!"
    }
kind: ConfigMap
metadata:
  name: app-config
```

# Prerequisites

To get started with these quickstarts you'll need the following prerequisites:

Name | Description | Version
--- | --- | ---
[java][1] | Java JDK | 8
[maven][2] | Apache Maven | 3.2.x 
[oc][3] | OpenShift Client | v3.3.x
[git][4] | Git version management | 2.x 

[1]: http://www.oracle.com/technetwork/java/javase/downloads/
[2]: https://maven.apache.org/download.cgi?Preferred=ftp://mirror.reverse.net/pub/apache/
[3]: https://docs.openshift.com/enterprise/3.2/cli_reference/get_started_cli.html
[4]: https://git-scm.com/book/en/v2/Getting-Started-Installing-Git

In order to build and deploy this project, you must have an account on an OpenShift Online (OSO): https://console.dev-preview-int.openshift.com/ instance.


# Build the Project

1. Execute the following apache maven command:

```bash
mvn clean package
```

# OpenShift Online

1. Go to [OpenShift Online](https://console.dev-preview-int.openshift.com/console/command-line) to get the token used by the oc client for authentication and project access. 

1. On the oc client, execute the following command to replace MYTOKEN with the one from the Web Console:

    ```
    oc login https://api.dev-preview-int.openshift.com --token=MYTOKEN
    ```
1. To allow the Vert.x application running as a pod to access the Kubernetes Api to retrieve the Config Map associated to the application name of the project `vertx-rest-configmap`, 
   the view role must be assigned to the default service account in the current project:

    ```
    oc policy add-role-to-user view system:serviceaccount:$(oc project -q):default -n $(oc project -q)
    ```    
1. Use the Fabric8 Maven Plugin to launch the S2I process on the OpenShift Online machine & start the pod.

    ```
    mvn clean package fabric8:deploy -Popenshift  -DskipTests
    ```
    
1. Get the route url.

    ```
    oc get route/vertx-configmap-rest
    NAME                  HOST/PORT               PATH    SERVICE                PORT      TERMINATION  
    vertx-configmap-rest  <HOST_PORT_ADDRESS>             vertx-configmap-rest   8080
    ```

1. Use the Host or Port address to access the REST endpoint.
    ```
    http http://<HOST_PORT_ADDRESS>/greeting
    http http://<HOST_PORT_ADDRESS>/greeting name==Bruno

    or 

    curl http://<HOST_PORT_ADDRESS>/greeting
    curl http://<HOST_PORT_ADDRESS>/greeting name==Bruno
    ```
1. Validate that you get the message `Hello, World from Kubernetes ConfigMap !` as call's response from the REST endpoint