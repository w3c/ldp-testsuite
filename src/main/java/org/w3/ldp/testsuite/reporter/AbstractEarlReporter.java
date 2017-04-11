package org.w3.ldp.testsuite.reporter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

import org.w3.ldp.testsuite.vocab.LDP;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

public abstract class AbstractEarlReporter {

	protected BufferedWriter writerTurtle;
	protected BufferedWriter writerJson;
	protected Model model;
	protected static final String TURTLE = "TURTLE";
	protected static final String JSON_LD = "JSON-LD";
	protected static final HashMap<String, String> prefixes = new HashMap<String, String>();

	protected String outputDirectory;

	public void setOutputDirectory(String outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	static {
		prefixes.put("doap", "http://usefulinc.com/ns/doap#");
		prefixes.put("foaf", "http://xmlns.com/foaf/0.1/");
		prefixes.put("earl", "http://www.w3.org/ns/earl#");
		prefixes.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		prefixes.put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
		prefixes.put("mf", "http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#");
		prefixes.put("rdft", "http://www.w3.org/ns/rdftest#");
		prefixes.put("dcterms", "http://purl.org/dc/terms/");
		prefixes.put("td", "http://www.w3.org/2006/03/test-description#");
		prefixes.put(LDP.LDPT_PREFIX, LDP.LDPT_NAMESPACE);
	}

	protected abstract String getFilename();

	protected void createWriter(String directory, String title) throws IOException {
		File dir = new File(directory);
		dir.mkdirs();
		System.out.println("Writing EARL results:");
		String fileName = getFilename() + title + ".ttl";
		File file = new File(dir, fileName);
		writerTurtle = new BufferedWriter(new FileWriter(file));
		System.out.println("\t"+file.getAbsolutePath());
		fileName = getFilename() + title + ".jsonld";
		file = new File(dir, fileName);
		writerJson = new BufferedWriter(new FileWriter(file));
		System.out.println("\t"+file.getAbsolutePath());
	}

	protected void write() {
		model.write(writerTurtle, TURTLE);
		model.write(writerJson, JSON_LD);
	}

	protected void endWriter() throws IOException {
		writerTurtle.flush();
		writerTurtle.close();
		writerJson.flush();
		writerJson.close();
	}

	protected void createModel() {
		model = ModelFactory.createDefaultModel();
		writePrefixes(model);
	}

	public void writePrefixes(Model model) {
		for (Entry<String, String> prefix : prefixes.entrySet()) {
			model.setNsPrefix(prefix.getKey(), prefix.getValue());
		}
	}

	public static String createTestCaseName(String className, String methodName) {

		className = className.substring(className.lastIndexOf(".") + 1);

		if (className.endsWith("Test")) {
			className = className.substring(0, className.length()-4);
		}
		if (methodName.startsWith("test")) {
			methodName = methodName.substring(4, methodName.length());
		}
		return className + "-" + methodName;
	}

	public static String createTestCaseURL(String className, String methodName) {
		return LDP.LDPT_NAMESPACE + createTestCaseName(className, methodName);
	}
}
