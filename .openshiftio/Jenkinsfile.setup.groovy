
def setupEnvironmentPre(env) {
  sh "if ! oc get -n ${env} configmap app-config -o yaml | grep app-config.yml; then oc create -n ${env} configmap app-config --from-file=app-config.yml; fi"
}

def setupEnvironmentPost(env) {
}

return this

