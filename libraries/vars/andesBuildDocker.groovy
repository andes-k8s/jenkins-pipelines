#!/usr/bin/groovy
def call(body) {
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST 
  body.delegate = config
  body()

  def repoUrl= null 
  def branch = null
  def pushToDockerRegistry = false 
  def dockerFileFolder = null
  def dockerTags = []
  def registryCredential = null
  def imageName = null
  def gitMergeWithMaster = null
  def dockerBuildArgs = []
  if (config != null) {
    repoUrl = config.repoUrl ? config.repoUrl : ""
    registryCredential = config.registryCredential ? config.registryCredential : ""
    branch = config.branchName ? config.branchName : (config.branchFromParam ? params[config.branchFromParam] : "master") 
    pushToDockerRegistry = config.pushToDockerRegistry ? config.pushToDockerRegistry : false
    dockerFileFolder = config.dockerFileFolder ? config.dockerFileFolder : "."
    dockerTags = config.dockerTags ? config.dockerTags : ["${branch}"]
    imageName = config.dockerImageFromParam ? params[config.dockerImageFromParam] : env.IMAGE_NAME
    dockerBuildArgs = config.dockerBuildArgs ? dockerBuildArgs : []
    gitMergeWithMaster = config.gitMergeWithMaster ? config.gitMergeWithMaster : false

    if (imageName == null) 
      error "IMAGE_NAME environment variable is required or dockerImageFromEnv parameter"
    if (pushToDockerRegistry && registryCredential == null) 
      error "registryCredential is needed"
    def checkoutResponse = checkout([
        $class: 'GitSCM',
        branches: [[name:  branch ]],
        userRemoteConfigs: [[ url: repoUrl]]
    ])
    def BRANCH = GIT_BRANCH.replaceAll("origin/", "")
    if (gitMergeWithMaster) {
       sh "git merge origin/master --no-edit"
    }
    def HASH = checkoutResponse.GIT_COMMIT
    // Check if this hash is already built 
    def imageAlreadyExists = sh(script: "docker pull -q ${imageName}:${branch}-${HASH}", returnStatus: true) 
    if (imageAlreadyExists == 0) {
      echo "Image already in dockerhub"
      return
    }
    def buildArgsString = ""
    dockerBuildArgs.each{arg -> buildArgsString += "--build-arg ${arg} "}

    dockerTags.push("${branch}-${HASH}")
    docker.withRegistry('', registryCredential ) {
      def apiImage = docker.build("${imageName}:${branch}-${HASH}", "${buildArgsString} ${dockerFileFolder}")
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
