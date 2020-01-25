<h1 align="center"><img src="img/Jengu2.png" alt="Jengu Logo" width="450" /></h1>
<p align="center">A lightweight Jenkins Shared Library test framework.</p>

---

<p align="center"><img alt="GitHub release (latest SemVer)" src="https://img.shields.io/github/v/release/agarthetiger/jengu?sort=semver"></p>

Jengu is a test framework to test Jenkins Shared Library code. It was born from necessity while working with Jenkins at scale. See the repo [wiki](https://github.com/agarthetiger/jengu/wiki) for the background to why this project exists. 

## Overview

The purpose of this library is to enable writing automated tests to assert the correct behaviour of Jenkins Shared Libraries. Jengu enables the execution of tests and reporting of the results, it is your job to write good tests for your shared library.

The entry point is the `libraryTestRunner()` method. This looks by default for files in the workspace using the file glob "`tests/*Tests.groovy`". These files will be loaded and any methods annotated with `@Test` from `org.junit.Test` will be executed. The results of the tests will be output into a series of xml files, one xml file per test file, under `output/test-results/` in the workspace. The test results are published using the JUnit plugin (see dependency).

## Getting started
### Usage

Import this repo as a shared library in a Jenkinsfile in your Jenkins shared library repository. 

```groovy
library identifier: "jengu@v1.0.0",
    retriever: modernSCM([
        $class: 'GitSCMSource',
        remote: 'https://github.com/agarthetiger/jengu.git'
    ])
```

Importing this library makes the libraryTestRunner() method available to call from your Jenkinsfile. 

In your Jenkinsfile you then need to import your shared library.

```groovy
def libraryVersion = (!env.BRANCH_NAME || env.BRANCH_NAME.startsWith('PR')) ?
    'master' : env.BRANCH_NAME
library identifier: "jengu-demo@${libraryVersion}",
    retriever: modernSCM([
        $class: 'GitSCMSource',
        remote: 'https://github.com/agarthetiger/jengu-demo.git'
    ])
```

As we want to run the tests on feature branches the first line gets the branch name from the Jenkins Environment. 

Next you need to also clone your shared library into the workspace. Below is an example using a Declarative Jenkinsfile, this library also works with Scripted Jenkinsfiles. 

```groovy
pipeline {
    agent any
    options {
        ansiColor('xterm')
        timestamps()
    }
    stages {
        stage('Run Tests'){
            steps {
                libraryTestRunner()
            }
        }
    }
}
```

Note that the test classes in `/tests` will be loaded and executed based on the code checkout to the local workspace, however the methods under test will be executed based on the refspec of the imported library. This can be problematic when trying to test pull requests as it is not possible to checkout a (GitHub Enterprise) PR as the refspec for a shared library (according to CloudBees Enterprise support). 

### Example setup

See the [jengu-demo](https://github.com/agarthetiger/jengu-demo) project for an example of how to incorporate this framework into your shared library project. 

## Dependencies

### White-listed methods

If you're choosing or having to use this library as a Shared Library running in the Groovy sandbox, you will need to get all the following methods whitelisted. 

```
staticMethod org.apache.commons.lang.exception.ExceptionUtils getStackTrace java.lang.Throwable
method java.lang.Class getMethods
method java.lang.reflect.AnnotatedElement getAnnotation java.lang.Class
method java.lang.reflect.Member getName
staticMethod groovy.time.TimeCategory minus java.util.Date java.util.Date
method groovy.time.BaseDuration getMillis
staticMethod groovy.xml.DOMBuilder newInstance
method groovy.xml.DOMBuilder createDocument
method org.w3c.dom.Document createElement java.lang.String
method org.w3c.dom.Node appendChild org.w3c.dom.Node
method org.w3c.dom.Element setAttribute java.lang.String java.lang.String
method org.w3c.dom.Document getDocumentElement
method org.w3c.dom.Document createTextNode java.lang.String
```
:exclamation: Note that `method java.lang.Class getMethods` will be identified in the Jenkins Script Approvals admin page as a signature which may have introduced a security vulnerability. 

The following method is required by the code in the jengu-demo but is not required for the Jengu library itself. 

* method org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper getRawBuild

Personally I'd recommend enforcing a PR process for this code and loading it as a global shared library, which is not subject to the Groovy sandbox. Despite first appearances this is a more secure deployment because code calling whitelisted methods can change at any point once the method is whitelisted into something unintentionally destructive or intentionally malicious.

### Jenkins Plugins required

* Script Security
* Pipeline: Utility Steps
* AnsiColor
* Timestamps
* JUnit

## Releases

This project uses [SemVer](https://semver.org/) for versioning. See the [releases](https://github.com/agarthetiger/jengu/releases) in this repo for available release versions.

## Authors

[Andrew Garner](https://www.linkedin.com/in/buildthethingright/)

## License

[MIT](LICENSE)
