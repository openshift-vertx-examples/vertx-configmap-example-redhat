#!/bin/bash

echo "Delete service & replication controller"
oc delete service simple-vertx-configmap
oc delete rc simple-config-map
oc delete project vertx-demo

echo "Create project"
oc project vertx-demo
oc policy add-role-to-user view openshift-dev -n vertx-demo
oc policy add-role-to-group view system:serviceaccounts -n vertx-demo

echo "Create the configMap"
oc delete configmap/app-config
oc create configmap app-config --from-file=src/main/resources/app.json

echo "Remove old docker image"
docker rmi -f vertx-demo/simple-config-map:1.0.0-SNAPSHOT

echo "Create the docker image, kubernetes/openshift config file and deploy the pod"
mvn -Popenshift

sleep 10s

echo "Call REST endpoint"
export service=$(minishift service simple-vertx-configmap -n vertx-demo --url=true)
http $service/products
