node("maven") {
  checkout scm
  stage("Prepare") {
    sh "oc policy add-role-to-user view -z default"
  }
  stage("Build") {
    sh "mvn fabric8:deploy -Popenshift -DskipTests"
  }
  stage("Deploy")
}
