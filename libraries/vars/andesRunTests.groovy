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
    def testsStages = [:]
    for(def i=1; i<5; i++) {
      def currentValue = i
      testsStages[i] = {
        container("docker") {
          def checkoutResponse = checkout([
              $class: 'GitSCM',
              branches: [[name:  branch ]],
              userRemoteConfigs: [[ url: repoUrl]]
          ])
          sh "ls -lah"
          sh "ls -lah docker"
          sh "API=${dockerApiTag} APP=${dockerAppTag} docker-compose -p andes-${BUILD_NUMBER}-${currentValue} -f docker/docker-compose.yml up -d"
          sh 'sleep 10'
          // run tests 

          echo "running in parallel ${currentValue}"
          sh "docker ps -a"
          sh 'MONGO_URI="mongodb://db:27017/andes" node scripts/seeder.js'
          sh 'npx cypress run --browser chrome --tag $tag ${filefilter} --env MONGO_URI="mongodb://db:27017/andes",API_SERVER="http://nginx"${filefilter} --config baseUrl=http://nginx,video=${video},numTestsKeptInMemory=1,trashAssetsBeforeRuns=false --record --key d6b64714-ccf9-4fc9-959c-5923d23f2a06 ${cypressparams} --parallel || true'
          sh "docker-compose -f docker/docker-compose.yml down"
        }
      }
    }
    parallel testsStages
  }
}
