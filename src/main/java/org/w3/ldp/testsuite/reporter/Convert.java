package org.w3.ldp.testsuite.reporter;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import com.github.jsonldjava.jena.JenaJSONLD;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class Convert {

	public static void main(String[] args) {
		String fileName = args[0];
		
		JenaJSONLD.init();
		
		Model in = ModelFactory.createDefaultModel();
		try {
			in.read(new FileInputStream(fileName), null, "TURTLE");
			fileName = fileName.substring(0, fileName.length()-3)+"jsonld";
			System.out.println("Writing to file: "+fileName);
			in.write(new FileOutputStream(fileName), "JSON-LD");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		
	}

}
