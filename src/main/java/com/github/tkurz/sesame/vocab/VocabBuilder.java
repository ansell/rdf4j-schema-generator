package com.github.tkurz.sesame.vocab;

import org.openrdf.query.*;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.sail.memory.MemoryStore;

import java.io.*;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ...
 * <p/>
 * Author: Thomas Kurz (tkurz@apache.org)
 */
public class VocabBuilder {

    private Repository repository;

    File file;
    String name;
    RDFFormat rdfFormat;
    String prefix = "http://example.org/ontology#";
    String outputFolder = "/tmp";
    String packageName = "org.apache.marmotta.commons.vocabulary";

    public VocabBuilder(String filename, String format) throws IOException, RepositoryException, RDFParseException, MalformedQueryException, QueryEvaluationException {
        this.file = new File(filename);
        if(!this.file.exists()) throw new FileNotFoundException();
        this.rdfFormat = RDFFormat.forMIMEType(format);

        //parse
        repository = new SailRepository(new MemoryStore());
        repository.initialize();
        RepositoryConnection connection = repository.getConnection();

        //import
        connection.add(file,null,rdfFormat);
        TupleQuery tupleQuery = connection.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT ?ont {?ont a <http://www.w3.org/2002/07/owl#Ontology>}");

        TupleQueryResult results = tupleQuery.evaluate();
        if(results.hasNext()) {
            prefix = results.next().getBinding("ont").getValue().stringValue();
        }

        name = this.file.getName();

        if(name.contains(".")) name = name.substring(name.lastIndexOf(".")+1);
        name = Character.toUpperCase(name.charAt(0)) + name.substring(1);

        connection.close();
    }

    public void run() throws RepositoryException, IOException, RDFParseException, MalformedQueryException, QueryEvaluationException {

        RepositoryConnection connection = repository.getConnection();

        TupleQuery tupleQuery = connection.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT ?a ?b ?c {?a ?b ?c.}");

        TupleQueryResult results = tupleQuery.evaluate();

        Pattern pattern = Pattern.compile(Pattern.quote(prefix)+"(.+)");

        HashMap<String,String> vals = new HashMap<String,String>();
        while(results.hasNext()) {
            BindingSet bs = results.next();
            Matcher matcher = pattern.matcher(bs.getValue("a").stringValue());
            if(matcher.find()) {
                String k = cleanKey(matcher.group(1));
                vals.put(k, matcher.group(0));
            }
        }

        //print
        FileWriter w = new FileWriter(outputFolder+"/"+name+".java");
        PrintWriter out = new PrintWriter(w);
        out.printf("package %s;",packageName);
        out.printf("\n\nimport org.openrdf.model.URI;\n" +
                "import org.openrdf.model.ValueFactory;\n" +
                "import org.openrdf.model.impl.ValueFactoryImpl;\n\n");
        out.printf("/** \n * Namespace %s\n */\npublic class %s {\n\n",name,name);
        out.printf("\tpublic static final String NAMESPACE = \"%s\";\n\n",prefix);
        out.printf("\tpublic static final String PREFIX = \"%s\";\n\n",name.toLowerCase());

        TreeSet<String> keys = new TreeSet(vals.keySet());

        for(String key : keys) {
            tupleQuery = connection.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT ?a WHERE {{<"+vals.get(key)+"> <http://www.w3.org/2000/01/rdf-schema#comment> ?a} UNION {<"+vals.get(key)+"> <http://purl.org/dc/terms/description> ?a} UNION {<"+vals.get(key)+"> <http://www.w3.org/2004/02/skos/core#definition> ?a}}");
            results = tupleQuery.evaluate();
            if(results.hasNext()) {
                out.printf("\t/**\n\t * %s \n\t */\n", results.next().getValue("a").stringValue());
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
        out.close();
        w.close();
        connection.close();
        repository.shutDown();
    }

    private String cleanKey(String s) {
        s = s.replaceAll("#","");
        s = s.replaceAll("\\.","_");
        s = s.replaceAll("-","_");
        return s;
    }

}
