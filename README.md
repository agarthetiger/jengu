# Jengu
## A lightweight Jenkins Shared Library test framework

Jengu is a test framework to test Jenkins Shared Library code. It was born from necessity while working with Jenkins at scale.

### Background
Company X had a handful of global product teams, each product team had hundreds of developers with several hundred micro-services and [microliths](https://dzone.com/articles/are-you-building-microservices-or-microliths), each in separate git repositories.  Multi-branch jobs were used to build and test on feature branches as well as on develop, hotfix and release branches, and there were thousands of Jenkins jobs.  Most projects used a common toolset including Gradle and Cucumber, and broadly followed the same pattern for CI/CD, which was build, unit test, and publish to artifact repository. Following a git-flow branching strategy and depending on whether the target infrastructure was dynamic, there would be subsequent groups of deployments and further tests executed, ultimately leading to a production deployment. A handful of people were responsible for building CI/CD Pipelines for all the products, and efficient reuse of Pipeline code was essential.

### Problems
As the shared library grew in popularity and adoption across the consuming teams a few problems became apparent, which were not a surprise. Changes to the shared library code would often break builds for the library consumers because all the testing was manual. Coming from nearly two decades of sofware development I searched for sustainable ways to answer the question "How can I run automated tests for this?". For several reasons we decided against [Jenkins Pipeline Unit](https://github.com/jenkinsci/JenkinsPipelineUnit). One reason in particular was that changes like method renames were slipping through code review, which this framework would not have caught.

Unsatisfied with anything I found, I wrote code which ran a series of test cases against the shared library code. This worked, but required knowledge to troubleshoot any errors from the job console log, as the code would sometimes print error messages to the console log when testing an error scenario. Being familiar with Java and JUnit I wrote code to use the standard JUnit annotations and build JUnit-compatible XML reports which could be consumed by Jenkins to more usefully display the results of a test run.

### Solution, or part of it

Jengu is a hybrid, with aspects of unit and integration tests in one. It requires the tests are executed on a Jenkins instance and will depend on certain plugins being installed and configuration made, such as white-listed methods. This is acceptable and even desirable in the case of multiple Jenkins masters. Each Jenkins master was execute the tests and a pass on any given Jenkins master should mean the shared library code will execute as expected on that master. New Jenkins masters were being created on a regular basis. Once the shared library tests were completing and passing on a new Jenkins master, this hybrid unit and integration test framework meant that where the test coverage was good we could be confident that consumers would be able to run the shared library pipelines successfully.

### Enhancements
Where the shared library contained complete pipelines, we used dummy projects which we could build, deploy, publish, branch, release and hotfix whenever we needed to. We had a wrapper job which would kick off each branch of a multi-branch job for several dummy projects, each with a different configuration, and the job would report the roll-up status of all the other jobs triggered. We hadn't automated any commits, bumping the semantic version or creating new branches, all of which would have increased the coverage and functional testing of this particular shared library.


## Dependencies
JENKINS_SSH_KEY - Jenkins CredentialId to check-out this library with.
White-listed methods -

### Jenkins Plugins required
Script Security
Pipeline: Utility Steps
AnsiColor
Timestamps


## Getting started

### Usage

### Writing tests

### Writing documentation



## Releases

This project uses [SemVer](https://semver.org/) for versioning. See the [releases](https://github.com/agarthetiger/jengu/releases) in this repo for available release versions.

## Authors

[Andrew Garner](https://www.linkedin.com/in/buildthethingright/)

## License

[MIT](LICENSE)
