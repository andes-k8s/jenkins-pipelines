def call(Map params) {
  def repoUrl= null 
  def branch = "master" 
  def imageName = null 
  def pushToDockerRegistry = false 
  def dockerFileFolder = "."
  def dockerTags = ["${branch}-latest"]
  if (params != null) {
    repoUrl = params.repoUrl ? params.repoUrl : ""
    branch = params.branch ? params.branch : "master"
    imageName = params.imageName ? params.imageName : ""
    pushToDockerRegistry = params.pushToDockerRegistry ? params.pushToDockerRegistry : false
    dockerFileFolder = params.dockerFileFolder ? params.dockerFileFolder : "."
    dockerTags = params.dockerTags ? params.dockerTags : ["${branch}"]
    if (pushToDockerRegistry && params.registryCredential == null) 
      error "registryCredential is needed"
    echo "Clonning ${repoUrl} branch: ${branch}"
    def checkoutResponse = checkout([
        $class: 'GitSCM',
        branches: [[name:  branch ]],
        userRemoteConfigs: [[ url: repoUrl]]
    ])
    def clonnedAppFolder = getNameFromRepoUrl(repoUrl)
    sh "cd ${clonnedAppFolder}"
    sh "ls -lah"
    def BRANCH = GIT_BRANCH.replaceAll("origin/", "")
    def HASH = checkoutResponse.GIT_COMMIT
    dockerTags.push("${branch}-${HASH}")
    echo "Building docker ${imageName} from folder ${dockerFileFolder}"
    docker.withRegistry('', params.registryCredential ) {
      def apiImage = docker.build("${imageName}:${branch}-${HASH}", dockerFileFolder)
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
