# Quickstart - Vert.x - Kubernetes Config Map

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
  creationTimestamp: 2016-09-14T12:13:08Z
  name: app-config
  namespace: vertx-demo
  resourceVersion: "6232"
  selfLink: /api/v1/namespaces/vertx-demo/configmaps/app-config
  uid: 990dd236-7a74-11e6-8d26-868f517b6834
```


# Instructions

* Launch minishift

```
minishift start --deploy-registry=true --deploy-router=true --memory=4048 --vm-driver="xhyve"
eval $(minishift docker-env)
```
   
* Log on to openshift
```    
oc login $(minishift ip):8443 -u admin -p admin -n default
```    
# Create a new project

```    
oc new-project vertx-demo
oc policy add-role-to-user view openshift-dev -n vertx-demo
oc policy add-role-to-group view system:serviceaccounts -n vertx-demo
```

# Create the ConfigMap

```
oc create configmap app-config --from-file=src/main/resources/app.json
```

# To consult the configMap (optional)

```
oc get configmap/app-config -o yaml
```

* Build and deploy the project
   
```
mvn -Popenshift   
```

# Consult the Service deployed 

First, we must retrieve the IP Address of the service exposed by the OpenShift Router to our host machine

```
export service=$(minishift service simple-vertx-configmap -n vertx-demo --url=true)
```

Next, we can use curl to perform a greeting from the REST service

```
curl $service/greeting

#example output
{"id":2,"content":"From config ==> Hello World"}% 
```

# Get the log of the Container 

```
bin/oc-log.sh simple-config-map
```

# Update the template

The Vertx Configuration Service provides a listener which can be informed if a config parameter of the ConfigMap has changed.
The listener checks every 5s if such a modification occurred. 

```java
conf.listen((configuration -> {
    LOG.info("Configuration change: " + configuration.toJson().encodePrettily());

    JsonObject newConfiguration = configuration.getNewConfiguration();
    template = newConfiguration.getString("template", DEFAULT_TEMPLATE);
    LOG.info("Template has changed: " + template);
}));
```

To test this feature, you will edit first the configMap and change the message template from the value `From config ==> Hello %s!` to `How are you %s?`. 

```
oc edit configmap/app-config
```

Next, we will check the log of the pod to verify that the modification has been propagated to the listener of Vertx.

```
bin/oc-log.sh simple-config-map
... 
"newConfiguration" : {
    "template" : "How are you %s?"
  },
  "previousConfiguration" : {
    "template" : "From config ==> Hello %s!"
  }

2017-01-20 13:36:28 INFO  RestApplication:80 - Template has changed: How are you %s?  
```

If the log reports that the template is now `How are you %s?`, then we can call the service again.

```
curl $service/greeting

#example output:
{"id":3,"content":"How are you World?"}%
```

# Troubleshooting

## Delete Replication controller, service, ConfigMap

```
oc delete configmap/app-config

oc delete service simple-vertx-configmap
oc delete rc simple-config-map

oc edit configmap/app-config
```

