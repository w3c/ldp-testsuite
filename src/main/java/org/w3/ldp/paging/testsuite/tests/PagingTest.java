package org.w3.ldp.paging.testsuite.tests;

import static com.jayway.restassured.config.LogConfig.logConfig;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;

import org.apache.commons.io.output.WriterOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.w3.ldp.testsuite.annotations.SpecTest;
import org.w3.ldp.testsuite.annotations.SpecTest.METHOD;
import org.w3.ldp.testsuite.annotations.SpecTest.STATUS;
import org.w3.ldp.testsuite.test.LdpTest;

import com.google.common.collect.ImmutableMap;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.specification.RequestSpecification;

public class PagingTest extends LdpTest{
	
	public static final String PAGING = "PAGING";
	public static final String SPEC_URI = "https://dvcs.w3.org/hg/ldpwg/raw-file/default/ldp-paging.html";

	@Test(
			groups = {MUST, PAGING},
			description = "LDP Paging clients MUST advertise their ability "
				+ "to support LDP Paging on all retrieval requests that normally "
				+ "result in a response containing a representation.")
	@SpecTest(
			specRefUri = SPEC_URI + "#ldpp-client-advertise",
			testMethod = METHOD.NOT_IMPLEMENTED,
			approval = STATUS.WG_PENDING)
	public void testClientAdvertise() {
		// TODO: Impl testClientAdvetrise
	}
	
	@Test(
			groups = {MUST, PAGING},
			description = "LDP Paging clients MUST be capable of at least " 
				+ "one of forward traversal and/or backward traversal.")
	@SpecTest(
			specRefUri = SPEC_URI + "#ldpp-client-traversal",
			testMethod = METHOD.NOT_IMPLEMENTED,
			approval = STATUS.WG_PENDING)
	public void testPagingTraversal() {
		// TODO: Impl testPagingTraversal
	}
	
	@Test(
			groups = {MUST, PAGING},
			description = "LDP Paging clients MUST NOT assume that any "
				+ "in-sequence page resource's paging links will remain "
				+ "unchanged when the in-sequence page resource is retrieved "
				+ "more than once. Such an assumption would conflict with a "
				+ "server's ability to add pages to a sequence as the paged "
				+ "resource changes, for example.")
	@SpecTest(
			specRefUri = SPEC_URI + "#ldpr-pagingGET-sequences-change",
			testMethod = METHOD.NOT_IMPLEMENTED,
			approval = STATUS.WG_PENDING)
	public void testSequenceChange() {
		// TODO: Impl testSequenceChange
	}
	
	@Test(
			groups = {MUST, PAGING},
			description = " LDP Paging clients MUST NOT assume that any in-sequence " 
				+ "page resource's paging links will always be accessible. ")
	@SpecTest(
			specRefUri = SPEC_URI + "#dfn-ldp-paging-client",
			testMethod = METHOD.NOT_IMPLEMENTED,
			approval = STATUS.WG_PENDING)
	public void testPagingAccess() {
		// TODO: Impl testPagingAccess
	}
	
	@Test(
			groups = {SHOULD, PAGING},
			description = "LDP Paging clients SHOULD NOT present paged resources "
				+ "as coherent or complete, or make assumptions to that effect.")
	@SpecTest(
			specRefUri = SPEC_URI + "#ldpp-client-paging-incomplete",
			testMethod = METHOD.NOT_IMPLEMENTED,
			approval = STATUS.WG_PENDING)
	public void testResourcePresentation() {
		// TODO: Impl testResourcePresentation
	}
	
	@Test(
			groups = {SHOULD, PAGING},
			description = "LDP Paging servers SHOULD allow clients to retrieve large LDP-RSs in pages.")
	@SpecTest(
			specRefUri = SPEC_URI + "#ldpr-page-large",
			testMethod = METHOD.NOT_IMPLEMENTED,
			approval = STATUS.WG_PENDING)
	public void testLargePage() {
		// TODO: Impl testLargePage
	}
	
	@Test(
			groups = {MAY, PAGING},
			description = "LDP Paging servers MAY treat any resource (LDP-RS or not) as a paged resource.")
	@SpecTest(
			specRefUri = SPEC_URI + "#dfn-ldp-paging-server",
			testMethod = METHOD.NOT_IMPLEMENTED,
			approval = STATUS.WG_PENDING)
	public void testResourcePaging() {
		// TODO: Impl testResourcePaging
	}
	
	@Test(
			groups = {MAY, PAGING},
			description = "LDP Paging servers MAY vary their treatment of any resource "
				+ "(LDP-RS or not) as a paged resource over time. In other words, given "
				+ "two attempts to retrieve the same resource at different points in time, "
				+ "the server can choose to return a representation of the first page at "
				+ "one time and of the entire resource at a different time. Clients distinguish "
				+ "between these cases based on the status code and response headers.")
	@SpecTest(
			specRefUri = SPEC_URI + "#ldpr-split-any-time",
			testMethod = METHOD.NOT_IMPLEMENTED,
			approval = STATUS.WG_PENDING)
	public void testSplitPaging() {
		// TODO: Impl testSplitPaging
	}
	
	@Test(
			groups = {SHOULD, PAGING},
			description = "LDP Paging servers SHOULD respect all of a client's LDP-Paging-defined "
				+ "hints, for example the largest page size the client is interested in processing, "
				+ "to influence the amount of data returned in representations.")
	@SpecTest(
			specRefUri = SPEC_URI + "#ldpp-prefer",
			testMethod = METHOD.NOT_IMPLEMENTED,
			approval = STATUS.WG_PENDING)
	public void testPreferredPaging() {
		// TODO: Impl testPreferredPaging
	}
	
	@Test(
			groups = {MAY, PAGING},
			description = "LDP Paging servers MAY ignore a page size of zero, or unrecognized "
				+ "units, and process the request as if no maximum desired size was specified; "
				+ "in the latter case the server can select whatever page size it deems appropriate, "
				+ "or choose not to page the resource at all.")
	@SpecTest(
			specRefUri = SPEC_URI + "#ldpp-prefer-unrecognized",
			testMethod = METHOD.NOT_IMPLEMENTED,
			approval = STATUS.WG_PENDING)
	public void testPreferUnrecognized() {
		// TODO: Impl testPreferUnrecognized
	}
	
	@Test(
			groups = {SHOULD, PAGING},
			description = "LDP Paging servers SHOULD respond with HTTP status code 2NN "
				+ "Contents of Related to successful GET requests with any paged resource "
				+ "as the Request-URI when the request indicates the client's support for "
				+ "that status code [2NN], although any appropriate code such as "
				+ "303 See Other MAY be used.")
	@SpecTest(
			specRefUri = SPEC_URI + "#ldpr-status-code",
			testMethod = METHOD.NOT_IMPLEMENTED,
			approval = STATUS.WG_PENDING)
	public void testPagingGet() {
		// TODO: Impl testPagingGet
	}
	
	@Test(
			groups = {MUST, PAGING},
			description = "LDP Paging servers MUST ensure that all state present in the "
				+ "paged resource throughout a client's entire traversal operation is "
				+ "represented in at least one in-sequence page resource. In other words, "
				+ "whatever subset of the paged resource that is not added, updated, or "
				+ "removed during the client's traversal of its pages has to be present "
				+ "in one of the pages. ")
	@SpecTest(
			specRefUri = SPEC_URI + "#ldpr-guarantee-show-unchanged",
			testMethod = METHOD.NOT_IMPLEMENTED,
			approval = STATUS.WG_PENDING)
	public void testShowUnchanged() {
		// TODO: Impl testShowUnchanged
	}
	
	@Test(
			groups = {MUST, PAGING},
			description = "LDP Paging servers MUST enable a client to detect any change "
					+ "to the paged resource that occurs while the client is retrieving "
					+ "pages by including a HTTP Link header on all successful HTTP GET responses. ")
	@SpecTest(
			specRefUri = SPEC_URI + "#ldpr-notify-changes",
			testMethod = METHOD.NOT_IMPLEMENTED,
			approval = STATUS.WG_PENDING)
	public void testDetectChange() {
		// TODO: Impl testDetectChange
		// "Covers only part of the specification requirement.
		// testPagingHeaders covers the rest."
	}
	
	@Test(
			groups = {SHOULD, PAGING},
			description = "LDP Paging servers SHOULD include the same header on all 4xx "
					+ "status code responses. The link's context URI identifies the "
					+ "in-sequence page resource being retrieved, target URI identifies "
					+ "the paged resource, link relation type is canonical [REL-CANONICAL], "
					+ "and link extension parameters include the parameter name etag and a "
					+ "corresponding parameter value identical to the ETag [RFC7232] of "
					+ "the paged resource.")
	@SpecTest(
			specRefUri = SPEC_URI + "#ldpr-notify-changes",
			testMethod = METHOD.NOT_IMPLEMENTED,
			approval = STATUS.WG_PENDING)
	public void testPagingHeaders() {
		// TODO: Impl testPagingHeaders
		// "Covers only part of the specification requirement. testDetectChange covers the rest."
	}
	
	@Test(
			groups = {MAY, PAGING},
			description = "LDP Paging servers MAY add or remove in-sequence page resources "
					+ "to a paged resource's sequence over time, but SHOULD only add pages "
					+ "to the end of a sequence.")
	@SpecTest(
			specRefUri = SPEC_URI + "#ldpr-pagingGET-sequences-change",
			testMethod = METHOD.NOT_IMPLEMENTED,
			approval = STATUS.WG_PENDING)
	public void testInSequencePaging() {
		// TODO: Impl testInSequencePaging
		// "Covers only part of the specification requirement.
		// testPageAddEnd covers the rest."
	}
	
	@Test(
			groups = {SHOULD, PAGING},
			description = "LDP Paging servers SHOULD only add pages "
					+ "to the end of a sequence.")
	@SpecTest(
			specRefUri = SPEC_URI + "#ldpr-pagingGET-sequences-change",
			testMethod = METHOD.NOT_IMPLEMENTED,
			approval = STATUS.WG_PENDING)
	public void testPageAddEnd() {
		// TODO: Impl testPageAddEnd
		// "Covers only part of the specification requirement.
		// testInSequencePaging covers the rest."
	}
	
	@Test(
			groups = {MAY, PAGING},
			description = "LDP Paging servers MAY provide a first page link when "
					+ "responding to requests with any in-sequence page resource "
					+ "as the Request-URI.")
	@SpecTest(
			specRefUri = SPEC_URI + "#ldpr-pagingGET-first-allowed-onpages",
			testMethod = METHOD.NOT_IMPLEMENTED,
			approval = STATUS.WG_PENDING)
	public void testAllowFirstLink() {
		// TODO: Impl testAllowFirstLink
	}
	
	@Test(
			groups = {MAY, PAGING},
			description = "LDP Paging servers MAY provide a last page link in responses "
					+ "to GET requests with any in-sequence page resource as the Request-URI.")
	@SpecTest(
			specRefUri = SPEC_URI + "#ldpr-pagingGET-last-allowed-onpages",
			testMethod = METHOD.NOT_IMPLEMENTED,
			approval = STATUS.WG_PENDING)
	public void testAllowLastLink() {
		// TODO: Impl testAllowLastLink
	}
	
	@Test(
			groups = {MUST, PAGING},
			description = "LDP Paging servers MUST provide a next page link in responses to "
					+ "GET requests with any in-sequence page resource other than the final "
					+ "page as the Request-URI. This is the mechanism by which clients can "
					+ "discover the URL of the next page.")
	@SpecTest(
			specRefUri = SPEC_URI + "#dfn-in-sequence-page-resource",
			testMethod = METHOD.NOT_IMPLEMENTED,
			approval = STATUS.WG_PENDING)
	public void testSequenced() {
		// TODO: Impl testSequenced
	}
	
	@Test(
			groups = {MUST, PAGING},
			description = "LDP Paging servers MUST NOT provide a next page link in responses "
					+ "to GET requests with the final in-sequence page resource as the "
					+ "Request-URI. This is the mechanism by which clients can discover the end "
					+ "of the page sequence as currently known by the server.")
	@SpecTest(
			specRefUri = SPEC_URI + "#ldpr-pagingGET-lastnext-prohibited",
			testMethod = METHOD.NOT_IMPLEMENTED,
			approval = STATUS.WG_PENDING)
	public void testRestrictFinalLink() {
		// TODO: Impl testRestrictFinalLink
	}
	
	@Test(
			groups = {MAY, PAGING},
			description = "LDP Paging servers MAY provide a previous page link in responses to "
					+ "GET requests with any in-sequence page resource other than the first page "
					+ "as the Request-URI. This is one mechanism by which clients can discover "
					+ "the URL of the previous page.")
	@SpecTest(
			specRefUri = SPEC_URI + "#ldpr-pagingGET-prev-allowed",
			testMethod = METHOD.NOT_IMPLEMENTED,
			approval = STATUS.WG_PENDING)
	public void testGetPreviousLink() {
		// TODO: Impl testGetPreviousLink
	}
	
	@Test(
			groups = {MUST, PAGING},
			description = "LDP Paging servers MUST NOT provide a previous page link in responses to "
					+ "GET requests with the first in-sequence page resource as the Request-URI. "
					+ "This is one mechanism by which clients can discover the beginning of the page "
					+ "sequence as currently known by the server.")
	@SpecTest(
			specRefUri = SPEC_URI + "#ldpr-pagingGET-firstprev-prohibited",
			testMethod = METHOD.NOT_IMPLEMENTED,
			approval = STATUS.WG_PENDING)
	public void testRestrictFirstLink() {
		// TODO: Impl testRestrictFirstLink
	}
	
	@Test(
			groups = {MUST, PAGING},
			description = "LDP Paging servers MUST provide an HTTP Link header whose target URI "
					+ "is http://www.w3.org/ns/ldp#Page, and whose link relation type is type "
					+ "[RFC5988] in responses to GET requests with any in-sequence page resource "
					+ "as the Request-URI. This is one mechanism by which clients know that the "
					+ "resource is one of a sequence of pages.")
	@SpecTest(
			specRefUri = SPEC_URI + "#ldpr-pagingGET-page-type-reqd",
			testMethod = METHOD.NOT_IMPLEMENTED,
			approval = STATUS.WG_PENDING)
	public void testGetPageReq() {
		// TODO: Impl testGetPageReq
	}
	
	@Test(
			groups = {MUST, PAGING},
			description = "LDP Paging servers MUST NOT initiate paging unless the client has "
					+ "indicated it understands paging. The only standard means defined by LDP "
					+ "paging for a client to signal a server that the client understands paging "
					+ "is via the client preference defined for this purpose; other "
					+ "implementation-specific means could also be used.")
	@SpecTest(
			specRefUri = SPEC_URI + "#ldpr-pagingGET-only-paging-clients",
			testMethod = METHOD.NOT_IMPLEMENTED,
			approval = STATUS.WG_PENDING)
	public void testGetOnlyPagingClients() {
		// TODO: Impl testGetOnlyPagingClients
	}
	
	@Test(
			groups = {MUST, PAGING},
			description = "LDP Paging servers MUST 	ensure that the membership triple and "
					+ "containment triple for each member are part of the same in-sequence "
					+ "page resource, whenever both triples are present in the page "
					+ "sequence for a paged LDPC.")
	@SpecTest(
			specRefUri = SPEC_URI + "#ldpc-onsamepage",
			testMethod = METHOD.NOT_IMPLEMENTED,
			approval = STATUS.WG_PENDING)
	public void testMembershipSequence() {
		// TODO: Impl testMembershipSequence
	}
	
	@Test(
			groups = {MUST, PAGING},
			description = "LDP Paging servers MAY communicate the order it uses to allocate LDPC members to "
					+ "in-sequence page resources as part of the pages' representations; "
					+ "LDP Paging does not specify ordering for pages of LDPRs in other cases.")
	@SpecTest(
			specRefUri = SPEC_URI + "#ldpc-sortcriteriaobj",
			testMethod = METHOD.NOT_IMPLEMENTED,
			approval = STATUS.WG_PENDING)
	public void testSortMembers() {
		// TODO: Impl testSortMembers
		// "Covers only part of the specification requirement.
		// testPageAddEnd covers the rest."
	}
	
	@Test(
			groups = {MUST, PAGING},
			description = "If the server communicates this (ordering), it MUST specify the order using "
					+ "a triple whose subject is the page URI, whose predicate is ldp:containerSortCriteria, "
					+ "and whose object is a rdf:List of ldp:containerSortCriterion resources. "
					+ "The resulting order MUST be as defined by SPARQL SELECT’s ORDER BY clause "
					+ "[sparql11-query]. Sorting criteria MUST be the same for all pages of a representation; "
					+ "if the criteria were allowed to vary, the ordering among members of a container "
					+ "across pages would be undefined.")
	@SpecTest(
			specRefUri = SPEC_URI + "#ldpc-sortcriteriaobj",
			testMethod = METHOD.NOT_IMPLEMENTED,
			approval = STATUS.WG_PENDING)
	public void testSortTriples() {
		// TODO: Impl testSortTriples
		// "Covers only part of the specification requirement.
		// testSortMembers covers the rest."
	}
	
	@Test(
			groups = {MUST, PAGING},
			description = "LDPC page representations ordered using ldp:containerSortCriteria MUST contain, "
					+ "in every ldp:containerSortCriterion list entry, a triple whose subject is the sort "
					+ "criterion identifier, whose predicate is ldp:containerSortPredicate and whose object "
					+ "is the predicate whose value is used to order members between pages "
					+ "(the page-ordering values).")
	@SpecTest(
			specRefUri = SPEC_URI + "#ldpc-sortliteraltype",
			testMethod = METHOD.NOT_IMPLEMENTED,
			approval = STATUS.WG_PENDING)
	public void testSortCriteria() {
		// TODO: Impl testSortCriteria
	}
	
	@Test(
			groups = {MUST, PAGING},
			description = "LDPC page representations ordered using ldp:containerSortCriteria MUST contain, "
					+ "in every ldp:containerSortCriterion list entry, a triple whose subject is the sort "
					+ "criterion identifier, whose predicate is ldp:containerSortOrder and whose object "
					+ "describes the order used. LDP defines two values, ldp:Ascending and ldp:Descending, "
					+ "for use as the object of this triple. Other values can be used, but LDP assigns no "
					+ "meaning to them and interoperability will be limited.")
	@SpecTest(
			specRefUri = SPEC_URI + "#ldpc-sortorder",
			testMethod = METHOD.NOT_IMPLEMENTED,
			approval = STATUS.WG_PENDING)
	public void testSortOrder() {
		// TODO: Impl testSortOrder
	}
	
	@Test(
			groups = {MAY, PAGING},
			description = "LDPC page representations ordered using ldp:containerSortCriteria MAY contain, "
					+ "in any ldp:containerSortCriterion list entry, a triple whose subject is the sort "
					+ "criterion identifier, whose predicate is ldp:containerSortCollation and whose object "
					+ "identifies the collation used.")
	@SpecTest(
			specRefUri = SPEC_URI + "#ldpc-sortcollation",
			testMethod = METHOD.NOT_IMPLEMENTED,
			approval = STATUS.WG_PENDING)
	public void testSortCollation() {
		// TODO: Impl testSortCollation
		// "Covers only part of the specification requirement.
		// testRestrictCollation covers the rest."
	}
	
	@Test(
			groups = {MUST, PAGING},
			description = "The ldp:containerSortCollation triple MUST be omitted for comparisons involving "
					+ "page-ordering values for which [sparql11-query] does not use collations.")
	@SpecTest(
			specRefUri = SPEC_URI + "#ldpc-sortcollation",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_PENDING)
	public void testRestrictCollation() {
		// TODO: Impl testRestrictCollation
		// "Covers only part of the specification
		// requirement. testSortCollation covers the rest."
	}
	
	protected Map<String,String> auth;

	@Parameters("auth")
	public PagingTest(@Optional String auth) throws IOException {
		if (StringUtils.isNotBlank(auth) && auth.contains(":")) {
			String[] split = auth.split(":");
			if (split.length == 2 && StringUtils.isNotBlank(split[0]) && StringUtils.isNotBlank(split[1])) {
				this.auth = ImmutableMap.of("username", split[0], "password", split[1]);
			}
		} else {
			this.auth = null;
		}
	}
	
	@Override
	protected RequestSpecification buildBaseRequestSpecification() {
		RequestSpecification spec = RestAssured.given();
		if (auth != null) {
			spec.auth().preemptive().basic(auth.get("username"), auth.get("password"));
		}

		if (httpLog != null) {
			spec.config(RestAssured
					.config()
					.logConfig(logConfig()
							.enableLoggingOfRequestAndResponseIfValidationFails()
							.defaultStream(new PrintStream(new WriterOutputStream(httpLog)))
							.enablePrettyPrinting(true)));
		}
		return spec;
	}
	
}
