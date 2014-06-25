package org.w3.ldp.testsuite.mapper;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFWriter;
import com.jayway.restassured.mapper.ObjectMapper;
import com.jayway.restassured.mapper.ObjectMapperDeserializationContext;
import com.jayway.restassured.mapper.ObjectMapperSerializationContext;

public class RdfObjectMapper implements ObjectMapper {

    private String baseURI;

    public RdfObjectMapper() {
        this.baseURI = "";
    }

    public RdfObjectMapper(String baseURI) {
        this.baseURI = baseURI;
    }

    private String getLang(String mediaType) {
        if ("text/turtle".equals(mediaType)) {
            return "TURTLE";
        } else if ("application/rdf+xml".equals(mediaType)) {
            return "RDF/XML";
        }

        throw new IllegalArgumentException("Unsupported media type: " + mediaType);
    }

    @Override
    public Object deserialize(ObjectMapperDeserializationContext context) {
        InputStream input = context.getDataToDeserialize().asInputStream();
        Model m = ModelFactory.createDefaultModel();
        m.read(input, baseURI, getLang(context.getContentType()));
        return m;
    }

    @Override
    public Object serialize(ObjectMapperSerializationContext context) {
        Model model = context.getObjectToSerializeAs(Model.class);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        String lang = getLang(context.getContentType());
        RDFWriter rdfWriter = model.getWriter(lang);
        rdfWriter.setProperty("relativeURIs", "same-document");
        rdfWriter.setProperty("allowBadURIs", "true");
        rdfWriter.write(model, out, baseURI);

        return out.toByteArray();
    }
}
