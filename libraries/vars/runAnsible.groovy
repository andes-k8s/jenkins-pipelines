#!/usr/bin/groovy
def call(body) {
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST 
  body.delegate = config
  body()

  if (!config.playbook && !config.playbookFromParams) {
    error "playbook or playbookFromParams are required"
  }
  if (!config.hosts && !config.hostsFile && !config.hostsFromParams) {
    error "hosts or hostFile or hostsFromParams are required"
  }
  if (!config.userPublicKey && !config.userPublicKeyFromParams) {
    error "userPublicKey or userPublicKeyFromParams are required"
  }

  def playbookFileName = getPlaybookFileName(config.playbook, config.playbookFromParams)
  def hostsFileName = getHostsFileName(config.hosts, config.hostFile, config.hostsFromParams)
  def userPublicKey = getFromValueOrParams(config.userPublicKey, config.userPublicKeyFromParams)
  def pubKeyFileName = "id_rsa"
  convertValueToFile(userPublicKey, pubKeyFileName)
  sh "docker run --rm -it -v .:/ansible/playbooks philm/ansible_playbook -v ./${pubKeyFileName}:/root/id_rsa ${playbookFileName} -i ${hostsFileName} "


}

def getFromValueOrParams(value, paramName) {
  if (value) 
    return value 
  return params[paramName]
}

def getPlaybookFileName(playbook, playbookFromParams) {
  if (!playbook && !playbookFromParams) {
    error "playbook or playbookFromParams are required"
  }
  def fileName = "playbook.yml"
  if (playbook) {
    convertValueToFile(playbook, fileName)
  } else {
    fileName = params[playbookFromParams]
  }
  return fileName
}

def convertValueToFile(value, fileName) {
  File file = new File(fileName)
  file.write value 
}

def getHostsFileName(hosts, hostsFile, hostsFromParams) {
  if (!hosts && !hostFile && !hostsFromParams) {
    error "hosts or hostFile or hostsFromParams are required"
  }
  def fileName = "inventory"
  if (hosts) {
    convertValueToFile(hosts, fileName)
  } else {
    if (hostFile) {
      fileName = hostFile
    } else {
      fileName = params[hostsFromParams] 
    }
  }
}

