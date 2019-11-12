#!/usr/bin/env groovy

def success (String message = '') {
    if (message) {
        log.info message
    }
    currentBuild.result = "SUCCESS"
}

def fail (String message) {
    log.error message
    currentBuild.result = "FAILURE"
    error "[ERROR] Job failed, see console log for details."
}

def fail (Exception e) {
    log.exception e
    currentBuild.result = "FAILURE"
    error "[ERROR] Job failed, see console log for details."
}

def fail (Exception e, String message) {
    log.error message
    fail e
}

def setBuildDescription(String message, String colour = null) {
    currentBuild.description = colour ? "<span style='color:$colour'>" + message + "</span>" : message
}
