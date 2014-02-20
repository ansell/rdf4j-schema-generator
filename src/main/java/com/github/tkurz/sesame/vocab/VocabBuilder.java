package com.github.tkurz.sesame.vocab;

import org.openrdf.model.Literal;
import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.util.GraphUtil;
import org.openrdf.model.util.GraphUtilException;
import org.openrdf.model.vocabulary.DC;
import org.openrdf.model.vocabulary.DCTERMS;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.model.vocabulary.SKOS;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.Rio;

import java.io.*;
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

    private File file;
    private String name;
    private RDFFormat rdfFormat;
    private String prefix = "http://example.org/ontology#";
    private String outputFolder = "/tmp";
    private String packageName = "org.apache.marmotta.commons.vocabulary";
	private Model model;

    public VocabBuilder(String filename, RDFFormat format) throws IOException, RDFParseException {
        this.file = new File(filename);
        if(!this.file.exists()) throw new FileNotFoundException();
        this.rdfFormat = format;

        model = Rio.parse(new FileInputStream(file), "", rdfFormat);

        //import
        Set<Resource> owlOntologies = model.filter(null, RDF.TYPE, OWL.ONTOLOGY).subjects();
        if(!owlOntologies.isEmpty()) {
        	setPrefix(owlOntologies.iterator().next().stringValue());
        }
        
        if(this.getName() == null) {
        	setName(this.file.getName());
        }

        if(getName().contains(".")) setName(getName().substring(0,getName().lastIndexOf(".")));
        setName(Character.toUpperCase(getName().charAt(0)) + getName().substring(1));

    }

    /**
     * 
     */
    public void run() throws IOException, GraphUtilException {

        Pattern pattern = Pattern.compile(Pattern.quote(getPrefix())+"(.+)");
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
        try(final FileWriter w = new FileWriter(getOutputFolder()+"/"+getName()+".java");
        		final PrintWriter out = new PrintWriter(w);)
        {
	        out.printf("package %s;",getPackageName());
	        out.printf("\n\nimport org.openrdf.model.URI;\n" +
	                "import org.openrdf.model.ValueFactory;\n" +
	                "import org.openrdf.model.impl.ValueFactoryImpl;\n\n");
	        out.printf("/** \n * Namespace %s\n */\npublic class %s {\n\n",getName(),getName());
	        out.printf("\tpublic static final String NAMESPACE = \"%s\";\n\n",getPrefix());
	        out.printf("\tpublic static final String PREFIX = \"%s\";\n\n",getName().toLowerCase());
	
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
	        	if(comment == null) {
	        		comment = GraphUtil.getOptionalObjectLiteral(model, splitUris.get(key), DC.DESCRIPTION);
	        	}
	        	if(comment == null) {
	        		comment = GraphUtil.getOptionalObjectLiteral(model, splitUris.get(key), RDFS.LABEL);
	        	}
	        	if(comment == null) {
	        		comment = GraphUtil.getOptionalObjectLiteral(model, splitUris.get(key), DCTERMS.TITLE);
	        	}
	        	if(comment == null) {
	        		comment = GraphUtil.getOptionalObjectLiteral(model, splitUris.get(key), DC.TITLE);
	        	}
	            if(comment != null) {
	                out.printf("\t/**\n\t * %s \n\t * @see <a href=\"%s\">%s</a>\n\t */\n", comment.getLabel(), splitUris.get(key).stringValue(), comment.getLabel());
	            } else {
	                out.printf("\t/**\n\t * %s \n\t * @see <a href=\"%s\">%s</a>\n\t */\n", key, splitUris.get(key).stringValue(), key);
	            }
	            out.printf("\tpublic static final URI %s;\n\n",key);
	        }
	
	        out.printf("\n\tstatic{\n\t\tValueFactory factory = ValueFactoryImpl.getInstance();");
	        for(String key : keys) {
	
	            out.printf("\n\t\t%s = factory.createURI(%s, \"%s\");",key,getName()+".NAMESPACE",key);
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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getOutputFolder() {
		return outputFolder;
	}

	public void setOutputFolder(String outputFolder) {
		this.outputFolder = outputFolder;
	}

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

}
