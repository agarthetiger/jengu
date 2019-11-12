#!/usr/bin/env groovy

def debug (String message = '') {
    if (env.DEBUG) {
        logMessage(message, 'DEBUG', '34;1m')
    }
}

def info (String message = '') {
    logMessage(message, 'INFO', '34;1m')
}

def warn (String message = '') {
    logMessage(message, 'WARN', '33;1m')
}

def error (String message = '') {
    logMessage(message, 'ERROR', '31;1m')
}

def exception (Exception e = null) {
    logMessage(e?.toString(), 'ERROR', '31;1m')
}

private def logMessage(String message, String type, String colour) {
    def now = new Date()
    def nowFormat = now.format("yyyyMMdd-HH:mm:ss.SSS", TimeZone.getTimeZone('UTC'))
    Closure log = { m, t, c ->
        echo "\033[${c}[${t}] ${nowFormat} ${m}\033[0m"
    }
    env.TERM ? log(message, type, colour) : ansiColor('xterm') { log(message, type, colour) }
}
