# LDP Test Suite

Test Suite for Linked Data Platform (LDP).

## Background 

* [Linked Data Platform 1.0](http://www.w3.org/TR/ldp/) (W3C Last Call Working Draft 11 March 2014)
* [Linked Data Platform Use Cases and Requirements](http://www.w3.org/TR/ldp-ucr/) (W3C Working Group Note 13 March 2014)
* [LDP Best Practices and Guidelines](https://dvcs.w3.org/hg/ldpwg/raw-file/default/ldp-bp/ldp-bp.html) (W3C Editor's Draft 17 April 2014)
* [Linked Data Platform 1.0 Test Cases](https://dvcs.w3.org/hg/ldpwg/raw-file/default/Test%20Cases/LDP%20Test%20Cases.html) (W3C Working Group Note 17 April 2014)

## Status

This Test Suite is still _work-in-progress_, so __should not be used__ yet.

## Usage

### Standalone

You can run the test suite against an arbitrary server:

    mvn package
    cd target
    java -jar ldp-testsuite-1.0.0-SNAPSHOT-shaded.jar --server http://ldp.example.org

For instance, if you want to test [Apache Marmotta](http://marmotta.apache.org), once you
[have it installed](http://marmotta.apache.org/installation.html#source) the command to use
would be:

    java -jar ldp-testsuite-1.0.0-SNAPSHOT-shaded.jar --server http://localhost:8080/ldp

### Maven

If you are using [Maven](http://maven.apache.org) for building your project, you can use the test suite by 
[attaching the test JAR](http://maven.apache.org/guides/mini/guide-attached-tests.html#Using_the_attached_test_JAR):

    <dependency>
        <groupId>org.w3</groupId>
        <artifactId>ldp-testsuite</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <type>test-jar</type>
        <scope>test</scope>
    </dependency>

@@TODO@@: code example from Marmotta, as soon as we have this api available

## Contributors

* Steve Speicher (IBM)
* Samuel Padgett (IBM)
* Sergio Fernández (ASF)

## Licenses

The Test Suite software is avaible under the terms of the [W3C Software Notice and License](http://www.w3.org/Consortium/Legal/2002/copyright-software-20021231).
To contribute to the Test Suite software, @@TODO@@.

The Test Suite data is available under both [W3C Test Suite License](http://www.w3.org/Consortium/Legal/2008/04-testsuite-license) and 
[W3C 3-clause BSD License](http://www.w3.org/Consortium/Legal/2008/03-bsd-license). To contribute to the Test Suite data, see the 
[policies and contribution forms](http://www.w3.org/2004/10/27-testcases).

