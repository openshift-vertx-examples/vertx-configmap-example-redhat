# Introduction

This project exposes a configurable HTTP endpoint exposing a _greeting_ service. The service is available at this 
address: _http://hostname:port/api/greeting_ and returns a JSON response containing the _greeting_ message:

```json
{
  "content":"Hello, World from Kubernetes ConfigMap !"
}
 ```
 
The  message is configured using an external configuration provided by an OpenShift _configmap_.

# Prerequisites

To get started with this quickstart you'll need the following prerequisites:

Name | Description | Version
--- | --- | ---
[java][1] | Java JDK | 8
[maven][2] | Apache Maven | 3.3.x 
[oc][3] | OpenShift Client | v1.4.x
[git][4] | Git version management | 2.x 

[1]: http://www.oracle.com/technetwork/java/javase/downloads/
[2]: https://maven.apache.org/download.cgi?Preferred=ftp://mirror.reverse.net/pub/apache/
[3]: https://docs.openshift.com/enterprise/3.2/cli_reference/get_started_cli.html
[4]: https://git-scm.com/book/en/v2/Getting-Started-Installing-Git

In order to build and deploy this project on OpenShift, you need either:

* a local OpenShift instance such as Minishift,
* account on an OpenShift Online (OSO) instance, such as https://console.dev-preview-int.openshift.com/ instance.

# Deployment instructions

To build and deploy this quickstart you can:

1. deploy it to OpenShift using Apache Maven,
2. deploy it to OpenShift using a pipeline.
 
You need to be logged in to your OpenShift instance.
 
**If you are using Minishift**

1. Login to your OpenShift instance using:

```bash
oc login https://192.168.64.12:8443 -u developer -p developer
```

2. Open your browser to https://192.168.64.12:8443/console/, and log in using _developer/developer_.

3. Check that you have a project. If `oc project` returns an error, create a project with:

```bash
oc new-project myproject
```

**If your are using OpenShift Online**
  
1. Go to [OpenShift Online](https://console.dev-preview-int.openshift.com/console/command-line) to get the token used 
by the `oc` client for authentication and project access.
2. On the oc client, execute the following command to replace $MYTOKEN with the one from the Web Console:
     
```bash
oc login https://api.dev-preview-int.openshift.com --token=$MYTOKEN
```

3. Check that you have a project. If `oc project` returns an error, create a project with:
   
```bash
oc new-project myproject
```

**Permissions**

To be able to run this quickstart you need to add some permission to the project:

```bash
oc policy add-role-to-user view -n $(oc project -q) -z default
```

## Deploy the application to OpenShift using Maven

To deploy the application using Maven, launch:

```bash
mvn fabric8:deploy -Popenshift
```

This command builds and deploys the application to the OpenShift instance on which you are logged in. The 
configuration map (containing the application configuration) is also created and uploaded.

Once deployed, you can access the application using the _application URL_. Retrieve it using:

```bash
$ oc get route vertx-http-configmap -o jsonpath={$.spec.host}
vertx-http-myproject.192.168.64.12.nip.io                                                                                                                              
```

Then, open your browser to the displayed url: http://vertx-http-configmap-myproject.192.168.64.12.nip.io.                                                                         

Alternatively, you can invoke the _greeting_ service directly using curl or httpie:
    
```bash
curl http://vertx-http-myproject.192.168.64.12.nip.io/api/greeting
curl http://vertx-http-myproject.192.168.64.12.nip.io/api/greeting?name=Bruno
http http://vertx-http-myproject.192.168.64.12.nip.io/api/greeting
http http://vertx-http-myproject.192.168.64.12.nip.io/api/greeting name==Charles
```

If you get a `503` response, it means that the application is not ready yet.

Once the application is running, you can reconfigure it:

1. Open the src/main/fabric8/configmap.yml file
2. Change the log level and message; save the file
3. Execute `oc apply -f src/main/fabric8/configmap.yml`

After a few seconds, the application is reconfigured.

## Deploy the application to OpenShift using a pipeline

When deployed with a _pipeline_ the application is built from the sources (from a git repository) by a continuous 
integration server (Jenkins) running in OpenShift.

To trigger this built:

1. Apply the OpenShift template:

```bash
oc new-app -f src/openshift/openshift-pipeline-template.yml
```

2. Trigger the pipeline build:

```bash
oc start-build vertx-http-configmap
```

With the sequence of command, you have deployed a Jenkins instance in your OpenShift project, define the build 
pipeline of the application and trigger a first build of the application.

Once the build is complete, you can access the application using the _application URL_. Retrieve this url using:

```bash
oc get route vertx-http -o jsonpath={$.spec.host}
```

Then, open your browser to the displayed url. For instance, http://vertx-http-myproject.192.168.64.12.nip.io.           
                                                              
Alternatively, you can invoke the _greeting_ service directly using curl or httpie:
    
```bash
curl http://vertx-http-myproject.192.168.64.12.nip.io/api/greeting
curl http://vertx-http-myproject.192.168.64.12.nip.io/api/greeting?name=Bruno
http http://vertx-http-myproject.192.168.64.12.nip.io/api/greeting
http http://vertx-http-myproject.192.168.64.12.nip.io/api/greeting name==Charles
```

If you get a `503` response, it means that the application is not ready yet.

Once the application is running, you can reconfigure it:

1. Open the src/main/fabric8/configmap.yml file
2. Change the log level and message; save the file
3. Execute `oc apply -f src/main/fabric8/configmap.yml`


# Running integration tests

The quickstart also contains a set of integration tests. You need to be connected to an OpenShift instance (Openshift 
Online or Minishift) to run them. You also need to select an existing project.

You must be logged in (`oc login ...`) and be in a project to run the integration tests. You must allow the 
application to read the config map:

```
oc policy add-role-to-user view -n $(oc project -q) -z default
```

Then, execute the integration tests with:

```
mvn clean verify  -Popenshift -Popenshift-it
```

# Implementation

Vert.x reads the _config map_ "directly" (not using files or environment variables). The Vert.x `ConfigRetriever` is 
configured to load the content of the config map:

```java
ConfigStoreOptions appStore = new ConfigStoreOptions();
appStore.setType("configmap")               // store type - configmap
    .setFormat("yaml")                      // format - yaml
    .setConfig(new JsonObject()
        .put("name", "vertx-http-configmap")  // name of the config map
        .put("key", "conf"));                  // key inside the config map
```

The config map contains under the `conf` key, the list of the key/value pairs defined 
using Yaml as Data format for our application as you can see hereafter :

```yml
apiVersion: v1
kind: ConfigMap
metadata:
  name: vertx-http-configmap
data:
  conf: |-
    message : "Hello, %s from Kubernetes ConfigMap !"
    level : INFO
```