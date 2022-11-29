#!/usr/bin/groovy
def call(body) {
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST 
  body.delegate = config
  body()

  def repoUrl = null 
  def branch = "master"
  def cypressImage = "cypress/browsers:node14.19.0-chrome100-ff99-edge" 
  def dockerApiTag = "master"
  def dockerAppTag = "master"
  if (config != null) {
    repoUrl = config.repoUrl ? config.repoUrl : ""
    branch = config.branchName ? config.branchName : (config.branchFromParam ? params[config.branchFromParam] : branch) 
    cypressImage = config.cypressImage ? config.cypressImage : cypressImage
    dockerApiTag = config.dockerApiTag ? config.dockerApiTag : dockerApiTag
    dockerAppTag = config.dockerAppTag ? config.dockerAppTag : dockerAppTag
    // params: repoUrl, cypressImage, dockerApiTag, dockerAppTag 
    // Steps 
    // clone 
    // npm ci 
    // compose up 
    // run test 
    def checkoutResponse = checkout([
        $class: 'GitSCM',
        branches: [[name:  branch ]],
        userRemoteConfigs: [[ url: repoUrl]]
    ])
    sh "ls -lah"
    def testsStages = [:]
    for(def i=1; i<5; i++) {
      def currentValue = i
      testsStages[i] = {
        node {
          sh "API=$dockerApiTag APP=$dockerAppTag docker-compose -p andes-${currentValue}-${BUILD_NUMBER} -f docker/docker-compose.yml up -d"
          sh 'sleep 10'
          // run tests 

          echo "running in parallel ${currentValue}"
          sh "docker ps -a"
          sh "docker-compose -f docker/docker-compose.yml down"
        }
      }
    }
    parallel testsStages
  }
}
