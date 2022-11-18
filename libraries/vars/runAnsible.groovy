#!/usr/bin/groovy
def call(body) {
  def ansibleImage = "andesnqn/ansible-runner:master"
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST 
  body.delegate = config
  body()

  if (!config.playbook && !config.playbookFile && !config.playbookFromParams) {
    error "playbook or playbookFile or playbookFromParams are required"
  }
  if (!config.hosts && !config.hostsFile && !config.hostsFromParams) {
    error "hosts or hostFile or hostsFromParams are required"
  }
  if (!config.userPublicKey && !config.userPublicKeyFromParams) {
    error "userPublicKey or userPublicKeyFromParams are required"
  }


  def playbookFileName = createFileFrom(config.playbook, config.playbookFile, config.playbookFromParams, "playbook.yml", params)
  def hostsFileName = createFileFrom(config.hosts, config.hostFile, config.hostsFromParams, "inventory.ini", params)
  def userPublicKey = getFromValueOrParams(config.userPublicKey, config.userPublicKeyFromParams)
  def pubKeyFileName = "id_rsa"
  sh "rm -f id_rsa"
  convertValueToFile(userPublicKey, pubKeyFileName)
  sh "ls -lah"
  sh "docker run --rm -v \$(pwd)/${playbookFileName}:/ansible/${playbookFileName} -v \$(pwd)/${hostsFileName}:/app/${hostsFileName} -v \$(pwd)/${pubKeyFileName}:/root/.ssh/id_rsa --entrypoint /usr/bin/ls ${ansibleImage}"
  sh "docker run --rm -v \$(pwd)/${playbookFileName}:/ansible/${playbookFileName} -v \$(pwd)/${hostsFileName}:/app/${hostsFileName} -v \$(pwd)/${pubKeyFileName}:/root/.ssh/id_rsa --entrypoint /usr/bin/pwd ${ansibleImage}"
  sh "docker run --rm -v \$(pwd)/${playbookFileName}:/ansible/${playbookFileName} -v \$(pwd)/${hostsFileName}:/app/${hostsFileName} -v \$(pwd)/${pubKeyFileName}:/root/.ssh/id_rsa ${ansibleImage} -i ${hostsFileName} /app/${playbookFileName}"


}

def getFromValueOrParams(value, paramName) {
  if (value) 
    return value 
  return params[paramName]
}

def convertValueToFile(content, fileName) {
  sh "echo '${content}' > ${fileName}"
}

def createFileFrom(value, valueFile, valueFromParams, outputFileName, params) {
  if (!value && !valueFile && !valueFromParams) {
    error "value or valueFile or valueFromParams are required"
  }
  def fileName = outputFileName 
  if (value) {
    convertValueToFile(value, fileName)
  } else {
    if (valueFile) {
      fileName = valueFile
    } else {
      convertValueToFile(params[valueFromParams], outputFileName)
    }
  }
  return fileName
}

