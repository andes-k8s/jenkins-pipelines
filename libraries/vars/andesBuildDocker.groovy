def call(body) {
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST 
  body()

  def repoUrl= null 
  def branch = "master" 
  def imageName = null 
  def pushToDockerRegistry = false 
  def dockerFileFolder = "."
  def dockerTags = ["${branch}-latest"]
  echo "PASO ----1"
  if (config != null) {
    repoUrl = config.repoUrl ? config.repoUrl : ""
    branch = config.branch ? config.branch : "master"
    imageName = config.imageName ? config.imageName : ""
    pushToDockerRegistry = config.pushToDockerRegistry ? config.pushToDockerRegistry : false
    dockerFileFolder = config.dockerFileFolder ? config.dockerFileFolder : "."
    dockerTags = config.dockerTags ? config.dockerTags : ["${branch}"]
    if (pushToDockerRegistry && config.registryCredential == null) 
      error "registryCredential is needed"
    echo "Clonning ${repoUrl} branch: ${branch}"
    sh "git clone ${repoUrl}"
    sh "ls -lah"
    sh "git checkout ${branch}"
    echo "PASO ----2"
    def clonnedAppFolder = getNameFromRepoUrl(repoUrl)
    echo "PASO ----3"
    sh "ls -lah"
    sh "cd ${clonnedAppFolder}"
    sh "ls -lah"
    def BRANCH = GIT_BRANCH.replaceAll("origin/", "")
    def HASH = checkoutResponse.GIT_COMMIT
    dockerTags.push("${branch}-${HASH}")
    echo "Building docker ${imageName} from folder ${dockerFileFolder}"
    docker.withRegistry('', config.registryCredential ) {
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
