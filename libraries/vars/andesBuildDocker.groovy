#!/usr/bin/groovy
def call(body) {
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST 
  body.delegate = config
  body()

  def repoUrl= null 
  def branch = "master" 
  def pushToDockerRegistry = false 
  def dockerFileFolder = "."
  def dockerTags = ["${branch}-latest"]
  def registryCredential = ""
  if (config != null) {
    repoUrl = config.repoUrl ? config.repoUrl : ""
    registryCredential = config.registryCredential ? config.registryCredential : ""
    branch = config.branch ? config.branch : "master"
    pushToDockerRegistry = config.pushToDockerRegistry ? config.pushToDockerRegistry : false
    dockerFileFolder = config.dockerFileFolder ? config.dockerFileFolder : "."
    dockerTags = config.dockerTags ? config.dockerTags : ["${branch}"]
    if (env.IMAGE_NAME == null) 
      error "IMAGE_NAME environment variable is required"
    if (pushToDockerRegistry && registryCredential == null) 
      error "registryCredential is needed"
    def checkoutResponse = checkout([
        $class: 'GitSCM',
        branches: [[name:  branch ]],
        userRemoteConfigs: [[ url: repoUrl]]
    ])
    def BRANCH = GIT_BRANCH.replaceAll("origin/", "")
    def HASH = checkoutResponse.GIT_COMMIT
    // Check if this hash is already built 
    def imageAlreadyExists = sh(script: "docker pull -q ${env.IMAGE_NAME}:${HASH}", returnStatus: true) 
    echo "-----------------------------------------------"
    echo imageAlreadyExists
    echo "-----------------------------------------------"

    dockerTags.push("${branch}-${HASH}")
    docker.withRegistry('', registryCredential ) {
      def apiImage = docker.build("${env.IMAGE_NAME}:${branch}-${HASH}", dockerFileFolder)
      if (pushToDockerRegistry) {
        for(tag in dockerTags) {
          apiImage.push(tag) 
        }
      }
    }
  }
}

def getNameFromRepoUrl(repoUrl) {
  def parts = repoUrl.split("/")
  def lastWithExtension = parts[parts.size()-1]
  def name = lastWithExtension.replaceAll(".git", "")
  return name
}
