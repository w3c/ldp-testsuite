package org.w3.ldp.testsuite.reporter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;

import org.w3.ldp.testsuite.vocab.LDP;

import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.jena.JenaJSONLD;
import com.github.jsonldjava.utils.JsonUtils;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public abstract class AbstractEarlReporter {

	protected BufferedWriter writerTurtle;
	protected BufferedWriter writerJson;
	protected Model model;
	protected static final String TURTLE = "TURTLE";
	protected static final String JSON_LD = "JSON-LD";
	protected static final String OUTPUT_DIR = "report";

	static {
		JenaJSONLD.init();
	}

	protected abstract String getFilename();

	protected void createWriter(String directory) throws IOException {
		File dir = new File(directory);
		dir.mkdirs();
		writerTurtle = new BufferedWriter(new FileWriter(new File(dir, getFilename() + ".ttl")));
		writerJson = new BufferedWriter(new FileWriter(new File(dir, getFilename() + ".jsonld")));
	}

	protected void write() {
		model.write(writerTurtle, TURTLE);

		StringWriter sw = new StringWriter();
		model.write(sw, JSON_LD);
		try {

			Object jsonObject = JsonUtils.fromString(sw.toString());

			HashMap<String, String> context = new HashMap<String, String>();
			// Customise context
			context.put("dcterms", "http://purl.org/dc/terms/");
			context.put("earl", "http://www.w3.org/ns/earl#");
			context.put("foaf", "http://xmlns.com/foaf/0.1/");
			context.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");

			// Create an instance of JsonLdOptions with the standard JSON-LD
			// options (will just be default for now)
			JsonLdOptions options = new JsonLdOptions();
			Object compact = JsonLdProcessor.compact(jsonObject, context,
					options);

			writerJson.write(JsonUtils.toPrettyString(compact));
		} catch (IOException | JsonLdError e) {
			e.printStackTrace();
		}

	}

	protected void endWriter() throws IOException {
		writerTurtle.flush();
		writerTurtle.close();

		writerJson.flush();
		writerTurtle.close();
	}

	protected void createModel() {
		model = ModelFactory.createDefaultModel();
		writePrefixes(model);
	}

	public void writePrefixes(Model model) {
		model.setNsPrefix("doap", "http://usefulinc.com/ns/doap#");
		model.setNsPrefix("foaf", "http://xmlns.com/foaf/0.1/");
		model.setNsPrefix("earl", "http://www.w3.org/ns/earl#");
		model.setNsPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		model.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
		model.setNsPrefix("mf",
				"http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#");
		model.setNsPrefix("rdft", "http://www.w3.org/ns/rdftest#");
		model.setNsPrefix("dcterms", "http://purl.org/dc/terms/");
		model.setNsPrefix("td", "http://www.w3.org/2006/03/test-description#");
		model.setNsPrefix(LDP.LDPT_PREFIX, LDP.LDPT_NAMESPACE);
	}

	public String createTestCaseName(String className, String methodName) {

		className = className.substring(className.lastIndexOf(".") + 1);

		if (className.endsWith("Test")) {
			className = className.substring(0, className.length()-4);
		}
		if (methodName.startsWith("test")) {
			methodName = methodName.substring(4, methodName.length());
		}
		return className + "-" + methodName;
	}

	public String createTestCaseURL(String className, String methodName) {
		return LDP.LDPT_NAMESPACE + createTestCaseName(className, methodName);
	}
}
