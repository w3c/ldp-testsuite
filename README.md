# LDP Test Suite

Test Suite for Linked Data Platform (LDP).

## Background 

* [Linked Data Platform 1.0](http://www.w3.org/TR/ldp/) (W3C Last Call Working Draft 11 March 2014)
* [Linked Data Platform Use Cases and Requirements](http://www.w3.org/TR/ldp-ucr/) (W3C Working Group Note 13 March 2014)
* [LDP Best Practices and Guidelines](https://dvcs.w3.org/hg/ldpwg/raw-file/default/ldp-bp/ldp-bp.html) (W3C Editor's Draft 17 April 2014)
* [Linked Data Platform 1.0 Test Cases](https://dvcs.w3.org/hg/ldpwg/raw-file/default/Test%20Cases/LDP%20Test%20Cases.html) (W3C Working Group Note 17 April 2014)

## Usage

### Standalone

You can run the test suite against an arbitrary server:

    mvn package
    java -jar target/ldp-testsuite-1.0.0-SNAPSHOT-shaded.jar

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

@@TODO@@: code example from Marmotta

## Contributors

* Steve Speicher (IBM)
* Sergio Fernández (ASF)

## Licenses

The Test Suite's source code is avaible as open source software under the terms of the 
[W3C Software Notice and License](http://www.w3.org/Consortium/Legal/2002/copyright-software-20021231).

The [Test Cases](https://dvcs.w3.org/hg/ldpwg/raw-file/default/Test%20Cases/LDP%20Test%20Cases.html) 
have [Copyright](http://www.w3.org/Consortium/Legal/ipr-notice#Copyright) © 2014 [W3C](http://www.w3.org/)® 
([MIT](http://www.csail.mit.edu), [ERCIM](http://www.ercim.eu), [Keio](http://www.keio.ac.jp), 
[Beihang](http://ev.buaa.edu.cn)), All Rights Reserved. W3C [liability](http://www.w3.org/Consortium/Legal/ipr-notice#Legal_Disclaimer), 
[trademark](http://www.w3.org/Consortium/Legal/ipr-notice#W3C_Trademarks) and 
[document use](http://www.w3.org/Consortium/Legal/copyright-documents) rules apply.

