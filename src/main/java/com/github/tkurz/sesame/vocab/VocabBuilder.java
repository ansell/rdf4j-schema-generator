package com.github.tkurz.sesame.vocab;

import org.openrdf.model.Literal;
import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.util.GraphUtil;
import org.openrdf.model.util.GraphUtilException;
import org.openrdf.model.vocabulary.DCTERMS;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.model.vocabulary.SKOS;
import org.openrdf.query.*;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.Rio;
import org.openrdf.sail.memory.MemoryStore;

import java.io.*;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ...
 * <p/>
 * Author: Thomas Kurz (tkurz@apache.org)
 */
public class VocabBuilder {

    File file;
    String name;
    RDFFormat rdfFormat;
    String prefix = "http://example.org/ontology#";
    String outputFolder = "/tmp";
    String packageName = "org.apache.marmotta.commons.vocabulary";
	private Model model;

    public VocabBuilder(String filename, RDFFormat format) throws IOException, RepositoryException, RDFParseException, MalformedQueryException, QueryEvaluationException {
        this.file = new File(filename);
        if(!this.file.exists()) throw new FileNotFoundException();
        this.rdfFormat = format;

        model = Rio.parse(new FileInputStream(file), "", rdfFormat);

        //import
        Set<Resource> owlOntologies = model.filter(null, RDF.TYPE, OWL.ONTOLOGY).subjects();
        if(!owlOntologies.isEmpty()) {
        	prefix = owlOntologies.iterator().next().stringValue();
        }
        
        name = this.file.getName();

        if(name.contains(".")) name = name.substring(0,name.lastIndexOf("."));
        name = Character.toUpperCase(name.charAt(0)) + name.substring(1);

    }

    public void run() throws IOException, GraphUtilException {

        Pattern pattern = Pattern.compile(Pattern.quote(prefix)+"(.+)");
        HashMap<String,URI> splitUris = new HashMap<String, URI>();
        for(Resource nextSubject : model.subjects()) {
        	if(nextSubject instanceof URI) {
	            Matcher matcher = pattern.matcher(nextSubject.stringValue());
	            if(matcher.find()) {
	                String k = cleanKey(matcher.group(1));
	                splitUris.put(k, (URI)nextSubject);
	            }
        	}
        }

        //print
        try(final FileWriter w = new FileWriter(outputFolder+"/"+name+".java");
        		final PrintWriter out = new PrintWriter(w);)
        {
	        out.printf("package %s;",packageName);
	        out.printf("\n\nimport org.openrdf.model.URI;\n" +
	                "import org.openrdf.model.ValueFactory;\n" +
	                "import org.openrdf.model.impl.ValueFactoryImpl;\n\n");
	        out.printf("/** \n * Namespace %s\n */\npublic class %s {\n\n",name,name);
	        out.printf("\tpublic static final String NAMESPACE = \"%s\";\n\n",prefix);
	        out.printf("\tpublic static final String PREFIX = \"%s\";\n\n",name.toLowerCase());
	
	        TreeSet<String> keys = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
	        keys.addAll(splitUris.keySet());
	        
	        for(String key : keys) {
	        	Literal comment = GraphUtil.getOptionalObjectLiteral(model, splitUris.get(key), RDFS.COMMENT);
	        	if(comment == null) {
	        		comment = GraphUtil.getOptionalObjectLiteral(model, splitUris.get(key), DCTERMS.DESCRIPTION);
	        	}
	        	if(comment == null) {
	        		comment = GraphUtil.getOptionalObjectLiteral(model, splitUris.get(key), SKOS.DEFINITION);
	        	}
	            if(comment != null) {
	                out.printf("\t/**\n\t * %s \n\t */\n", comment.getLabel());
	            } else {
	                out.printf("\t/**\n\t * %s \n\t */\n", key);
	            }
	            out.printf("\tpublic static final URI %s;\n\n",key);
	        }
	
	        out.printf("\n\tstatic{\n\t\tValueFactory factory = ValueFactoryImpl.getInstance();");
	        for(String key : keys) {
	
	            out.printf("\n\t\t%s = factory.createURI(%s, \"%s\");",key,name+".NAMESPACE",key);
	        }
	
	        out.println("\n\t}\n}");
	
	        //end
	        out.flush();
        }
    }

    private String cleanKey(String s) {
        s = s.replaceAll("#","");
        s = s.replaceAll("\\.","_");
        s = s.replaceAll("-","_");
        return s;
    }

}
