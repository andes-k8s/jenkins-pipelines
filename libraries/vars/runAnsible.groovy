#!/usr/bin/groovy
def call(body) {
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST 
  body.delegate = config
  body()

  println("----------------------------")
  print config
  println("----------------------------")

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
  def hostsFileName = createFileFrom(config.hosts, config.hostFile, config.hostsFromParams, "inventory", params)
  def userPublicKey = getFromValueOrParams(config.userPublicKey, config.userPublicKeyFromParams)
  def pubKeyFileName = "id_rsa"
  convertValueToFile(userPublicKey, pubKeyFileName)
  sh "ls -lah"
  sh "docker run --rm -v $PWD:/ansible/playbooks philm/ansible_playbook -v $PWD/${pubKeyFileName}:/root/.ssh/id_rsa ${playbookFileName} -i ${hostsFileName} "


}

def getFromValueOrParams(value, paramName) {
  if (value) 
    return value 
  return params[paramName]
}

def convertValueToFile(content, fileName) {
  sh "echo '${content}' > ${fileName}"
  println("-------------------- ${fileName}")
  sh "cat ${fileName}"
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

