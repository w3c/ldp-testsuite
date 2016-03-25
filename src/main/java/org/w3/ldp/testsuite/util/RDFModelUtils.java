package org.w3.ldp.testsuite.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

public class RDFModelUtils {
	
	public static Model cloneModel(Model model) {
		Model result = ModelFactory.createDefaultModel();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		model.write(out);
		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		result.read(in, null);
		return result;
	}

}
