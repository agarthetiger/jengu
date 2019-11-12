#!/usr/bin/env groovy
import groovy.time.TimeCategory
import groovy.xml.*
import org.w3c.dom.*

def call(String testFileGlob = 'tests/*Tests.groovy') {
    try {
        runTests(testFileGlob)
    }
    catch (Exception e) {
        jobUtils.fail e
    }
    finally {
        def testResultsXml = 'output/**/*.xml'
        junit testResultsXml
    }
}

private runTests(String testFileGlob) {
    def testFiles = findFiles(glob: testFileGlob)
    if (!testFiles) {
        jobUtils.fail "No test files found using file glob ${testFileGlob}"
    }
    log.debug "Found test files: ${testFiles.toString()}"

    for (int i = 0; i < testFiles.size(); i++) {
        List testCaseResults = []
        def testFile = testFiles[i]
        def testSuite = load(testFile.path)
        List testCases = getAnnotatedMethods(testSuite, org.junit.Test)
        log.debug "Test cases in ${testFile.path}: ${testCases.toString()}"
        def suiteStartTime = new Date()

        for (int j = 0; j < testCases.size(); j++) {
            def testCase = testCases[j]
            log.debug "Executing ${testCase}() from ${testFile.path}"
            def startTime = new Date()
            def additionalAttributes = [:]
            try {
                testSuite."$testCase"()
            }
            catch (AssertionError ae) {
                log.debug "Caught AssertionError thrown by ${testCase}() from ${testFile.path}"
                log.debug "AssertionError message is ${ae.getMessage()}"
                additionalAttributes = [result: 'failure', resultMessage: ae.getMessage()]
            }
            catch (Exception e) {
                log.debug "Caught Exception thrown by ${testCase}() from ${testFile.path}"
                log.error "Exception message is ${e.getMessage()}"
                additionalAttributes = [result: 'error', resultMessage: e.getMessage(), resultStackTrace: e.getStackTrace()]
            }
            finally {
                def testCaseResult = [classname: testFile.name.tokenize('/.')[0],
                                     testcase: testCase,
                                     duration: getDurationInSeconds(startTime)]
                testCaseResults.add(testCaseResult << additionalAttributes)
            }
        }

        Map testSuiteResults = generateTestSuite(testFile, getDurationInSeconds(suiteStartTime), testCases, testCaseResults)
        def reportFilename = "output/test-results/${testFile.name.replaceAll('\\.', '-')}.xml"
        def reportXmlString = generateJUnitXml(testSuiteResults)
        writeFile file: reportFilename, text: reportXmlString
        log.debug "Created file ${reportFilename} with xml of '${reportXmlString}'"
    }
}

private Map generateTestSuite(def testFile, def testDuration, def testCases, List testCaseResults) {
    log.debug "Executing generateTestSuite"

    Map testSuite
    List testCaseList = []

    try {
        for (int i = 0; i < testCaseResults.size(); i++) {
            Map testCase = testCaseResults[i]
            log.debug "Generating structure for test case ${testCase.toString()}"
            Map testCaseErrorDetails = [:]
            if (testCase.containsKey('result')) {
                testCaseErrorDetails << [result: testCase.result]
                testCaseErrorDetails << (testCase.containsKey('resultMessage') ? [resultMessage: testCase.resultMessage] : [:])
                testCaseErrorDetails << (testCase.containsKey('resultStackTrace') ? [resultStackTrace: testCase.resultStackTrace] : [:])
            }

            testCaseList.add([
                classname: testCase.classname,
                name: testCase.testcase,
                duration: testCase.duration,
            ] << testCaseErrorDetails)
        }

        log.debug "Generating structure for test suite ${testFile.toString()}"
        testSuite = [
            name: testFile.name,
            errors: testCaseResults.findAll{ it.result == 'error'}?.size(),
            skipped: testCaseResults.findAll{ it.result == 'skipped'}?.size(),
            failures: testCaseResults.findAll{ it.result == 'failure'}?.size(),
            tests: testCaseResults.size(),
            duration: testDuration,
            testCases: testCaseList
        ]

        log.debug "generateTestSuiteList generated the following structure:"
        log.debug testSuite.toString()
    }
    catch (Exception e) {
        log.exception e
    }

    return testSuite
}

/******************************************************************************
Expected input for generateJUnitXml
Map (testsuite) [
    name: '<my_class_name>'
    errors: '<number of tests which errored>'
    skipped: '<number of skipped tests>'
    failures: '<number of failed tests>'
    tests: '<total number of tests>'
    duration: '<time in millis of test suite execution>'
    timestamp:
    testCases: [ List of testCaseResults ]
]

Each testCaseResult in testCases List
List (testcase) [
    classname: testFiles[i].name.tokenize('/.')[0], // Optional, if not present use the test suite classname
    name: '<test case/method name>',
    duration: '<time in millis of test case execution>'
    result: 'failure', // Options are failure, skipped, error. Success is indicated by no 'result' in list.
    resultMessage: '' // Optional
    resultStackTrace: '' // Optional
]
******************************************************************************/
private String generateJUnitXml(def testSuite) {

    log.debug "Testsuite is ${testSuite.toString()}"

    Document doc = DOMBuilder.newInstance().createDocument()
    doc.appendChild(doc.createElement('testsuites'))

    Element ts = doc.createElement('testsuite')
    ts.setAttribute('name', testSuite.name)
    ts.setAttribute('errors', testSuite.errors?.toString())
    ts.setAttribute('skipped', testSuite.skipped?.toString())
    ts.setAttribute('tests', testSuite.testCases?.size()?.toString())
    ts.setAttribute('failures', testSuite.failures?.toString())
    ts.setAttribute('time', testSuite.duration?.toString())
    ts.setAttribute('timestamp', new Date().format("yyyy-MM-dd HH:mm:ss"))
    doc.documentElement.appendChild(ts)

    testSuite.testCases.each() { testCase ->
        log.debug "Test case is ${testCase.toString()}"
        Element tc = doc.createElement('testcase')
        tc.setAttribute('classname', testCase.classname)
        tc.setAttribute('name', testCase.name)
        tc.setAttribute('time', testCase.duration)
        if (testCase.result == 'failure' || testCase.result == 'error') {
            Element err = doc.createElement(testCase.result)
            err.setAttribute('message', testCase.resultMessage)
            if (testCase.resultStackTrace) {
                Text stackTrace = doc.createTextNode(testCase.resultStackTrace)
                err.appendChild(stackTrace)
            }
        }
        ts.appendChild(tc)
    }

    log.debug "generateJUnitXmlString returning ${doc.documentElement as String}"
    return doc.documentElement as String
}

private String getDurationInSeconds(Date startTime) {
    def durationInMillis = TimeCategory.minus(new Date(), startTime).millis
    return (durationInMillis/1000).toString()
}

@NonCPS
private List<String> getAnnotatedMethods(loadedClass, annotation) {
    def methodList = loadedClass?.class?.methods?.findAll { it.getAnnotation (annotation) }
    List<String> methodNames = methodList?.collect { it.getName() }
    return methodNames
}
