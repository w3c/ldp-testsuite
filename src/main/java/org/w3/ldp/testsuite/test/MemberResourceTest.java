package org.w3.ldp.testsuite.test;

import com.hp.hpl.jena.rdf.model.Model;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.w3.ldp.testsuite.mapper.RdfObjectMapper;

import static org.hamcrest.Matchers.notNullValue;
import static org.w3.ldp.testsuite.matcher.HttpStatusSuccessMatcher.isSuccessful;

/**
 * Tests that run on an LDP-RS that is not a container.
 */
public class MemberResourceTest extends RdfSourceTest {

    private String container;
    private String memberResource;

    @Parameters({"memberResource", "directContainer", "indirectContainer", "basicContainer", "post"})
    public MemberResourceTest(@Optional String memberResource, @Optional String directContainer, 
    		@Optional String indirectContainer, @Optional String basicContainer, @Optional String post) {
        // If resource is defined, use that. Otherwise, fall back to creating one from one of the containers.
        if (memberResource != null) {
            this.memberResource = memberResource;
        } else if (directContainer != null) {
            this.container = directContainer;
        } else if (indirectContainer != null) {
            this.container = indirectContainer;
        } else if (basicContainer != null) {
            this.container = basicContainer;
        } else {
            throw new SkipException("No memberResource or container parameters defined in testng.xml");
        }
        
        this.post = post;

        if (this.memberResource == null) {
            Model model = postContent();

            Response postResponse =
                    RestAssured.given().contentType(TEXT_TURTLE).body(model, new RdfObjectMapper())
                            .expect().statusCode(HttpStatus.SC_CREATED).header(LOCATION, notNullValue())
                            .when().post(this.container);

            this.memberResource = postResponse.getHeader(LOCATION);
        }
    }

    protected String getResourceUri() {
        return memberResource;
    }

    @AfterClass
    public void deleteTestResource() {
        // If container isn't null, we created the resource ourselves. To clean up, delete the resource.
        if (container != null) {
            RestAssured.expect().statusCode(isSuccessful()).when().delete(memberResource);
        }
    }

}
