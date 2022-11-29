#!/usr/bin/groovy
def call(body) {
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST 
  body.delegate = config
  body()

  // params: repoUrl, cypressImage, apiTag, appTag 
  // Steps 
  // clone 
  // npm ci 
  // compose up 
  // run test 
  def testsStages = [:]
  for(def i=1; i<4; i++) {
    def currentValue = i
    testsStages[i] = {
      echo "running in parallel ${currentValue}"
      sh "docker ps -a"
    }
  }
  parallel testsStages

}
