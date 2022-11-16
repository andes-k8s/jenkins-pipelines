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
  if (config != null) {
    repoUrl = config.repoUrl ? config.repoUrl : ""
    branch = config.branch ? config.branch : "master"
    pushToDockerRegistry = config.pushToDockerRegistry ? config.pushToDockerRegistry : false
    dockerFileFolder = config.dockerFileFolder ? config.dockerFileFolder : "."
    dockerTags = config.dockerTags ? config.dockerTags : ["${branch}"]
    print(config.registryCredential)
    if (env.IMAGE_NAME == null) 
      error "IMAGE_NAME environment variable is required"
    if (pushToDockerRegistry && config.registryCredential == null) 
      error "registryCredential is needed"
    echo "Clonning ${repoUrl} branch: ${branch}"
    def checkoutResponse = checkout([
        $class: 'GitSCM',
        branches: [[name:  branch ]],
        userRemoteConfigs: [[ url: repoUrl]]
    ])
    def BRANCH = GIT_BRANCH.replaceAll("origin/", "")
    def HASH = checkoutResponse.GIT_COMMIT
    dockerTags.push("${branch}-${HASH}")
    echo "Building docker ${env.IMAGE_NAME} from folder ${dockerFileFolder}"
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
