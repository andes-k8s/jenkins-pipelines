def call(Map params) {
  def repoUrl= null 
  def branch = "master" 
  def imageName = null 
  def pushToDockerRegistry = false 
  def dockerFile = "Dockerfile"
  def dockerTags = ["${branch}-latest"]
  if (params != null) {
    repoUrl = params.repoUrl ? params.repoUrl : ""
    branch = params.branch ? params.branch : "master"
    imageName = params.imageName ? params.imageName : ""
    pushToDockerRegistry = params.pushToDockerRegistry ? params.pushToDockerRegistry : false
    dockerFile = params.dockerFile ? params.dockerFile : "Dockerfile"
    dockerTags = params.dockerTags ? params.dockerTags : ["${branch}"]
    if (pushToDockerRegistry && "${env.DOCKER_REGISTRY}" == 'null') 
      error "DOCKER_REGISTRY is needed"
    def checkoutResponse = checkout([
        $class: 'GitSCM',
        branches: [[name:  branch ]],
        userRemoteConfigs: [[ url: repoUrl]]
    ])
    def BRANCH = GIT_BRANCH.replaceAll("origin/", "")
    def HASH = checkoutResponse.GIT_COMMIT
    tags.push("${imageName}:${branch}-${HASH}")
    docker.withRegistry('', params.registryCredential ) {
      def apiImage = docker.build("${API_IMAGE_NAME}:${BRANCH}-${HASH}", "./api")
      if (pushToDockerRegistry) {
        for(tag in dockerTags) {
          apiImage.push(tag) 
        }
      }
    }


}
