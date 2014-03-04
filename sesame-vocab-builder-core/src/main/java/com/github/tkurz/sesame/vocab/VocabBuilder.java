package com.github.tkurz.sesame.vocab;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.openrdf.model.*;
import org.openrdf.model.impl.URIImpl;
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
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ...
 * <p/>
 * @author Thomas Kurz (tkurz@apache.org)
 * @author Jakob Frank (jakob@apache.org)
 */
public class VocabBuilder {

    private String name = null;
    private String prefix = null;
    private String packageName = null;
    private String indent = "\t";
    private final Model model;

    /**
     * Create a new VocabularyBuilder, reading the vocab definition from the provided file
     * @param filename the input file to read the vocab from
     * @param format the format of the vocab file, may be {@code null}
     * @throws java.io.IOException if the file could not be read
     * @throws RDFParseException if the format of the vocab could not be detected or is unknown.
     */
    public VocabBuilder(String filename, String format) throws IOException, RDFParseException {
        this(filename, format!=null?Rio.getParserFormatForMIMEType(format):null);
    }

    public VocabBuilder(String filename, RDFFormat format) throws IOException, RDFParseException {
        Path file = Paths.get(filename);
        if(!Files.exists(file)) throw new FileNotFoundException(filename);

        if (format == null) {
            format = Rio.getParserFormatForFileName(filename);
        }

        try(final InputStream inputStream = Files.newInputStream(file)) {
            model = Rio.parse(inputStream, "", format);
        }

        //import
        Set<Resource> owlOntologies = model.filter(null, RDF.TYPE, OWL.ONTOLOGY).subjects();
        if(!owlOntologies.isEmpty()) {
            setPrefix(owlOntologies.iterator().next().stringValue());
        }

        setName(file.getFileName().toString());
        if(getName().contains(".")) setName(getName().substring(0,getName().lastIndexOf(".")));
        setName(Character.toUpperCase(getName().charAt(0)) + getName().substring(1));
    }

    public void generate(OutputStream outputStream) throws GenerationException, IOException, GraphUtilException {
        String cName = getName();
        if (StringUtils.isBlank(cName)) {
            throw new GenerationException("could not detect name, please set explicitly");
        }
        cName = WordUtils.capitalize(cName.replaceAll("\\W+", " ")).replaceAll("\\s+", "");

        generate(cName, new PrintWriter(outputStream));
    }

    public void generate(Path output) throws IOException, GraphUtilException, GenerationException {
        final String className = output.getFileName().toString().replaceFirst("\\.java$", "");
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(output, Charset.forName("utf8")))) {
            generate(className, out);
        }
    }

    /**
     *
     */
    public void generate(String className, PrintWriter out) throws IOException, GraphUtilException, GenerationException {
        if (StringUtils.isBlank(name)) {
            name = className;
        }
        if (StringUtils.isBlank(prefix)) {
            throw new GenerationException("could not detect prefix, please set explicitly");
        }

        Pattern pattern = Pattern.compile(Pattern.quote(getPrefix())+"(.+)");
        HashMap<String,URI> splitUris = new HashMap<>();
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

        //package is optional
        if (StringUtils.isNotBlank(packageName)) {
            out.printf("package %s;%n%n",getPackageName());
        }
        //imports
        out.println("import org.openrdf.model.URI;");
        out.println("import org.openrdf.model.ValueFactory;");
        out.println("import org.openrdf.model.impl.ValueFactoryImpl;");
        out.println();

        final URI pfx = new URIImpl(prefix);
        Literal oTitle = getFirstExistingObjectLiteral(model, pfx, RDFS.LABEL, DCTERMS.TITLE, DC.TITLE);
        Literal oDescr = getFirstExistingObjectLiteral(model, pfx, RDFS.COMMENT, DCTERMS.DESCRIPTION, DC.DESCRIPTION);
        Set<Value> oSeeAlso = model.filter(pfx, RDFS.SEEALSO, null).objects();

        //class JavaDoc
        out.println("/**");
        if (oTitle != null) {
            out.printf(" * %s.%n", WordUtils.wrap(oTitle.getLabel().replaceAll("\\s+", " "), 70, "\n * ", false));
            out.println(" * <p>");
        }
        if (oDescr != null) {
            out.printf(" * %s.%n", WordUtils.wrap(oDescr.getLabel().replaceAll("\\s+", " "), 70, "\n * ", false));
            out.println(" * <p>");
        }
        out.printf(" * Namespace %s.%n", name);
        out.printf(" * Prefix: {@code <%s>}%n", prefix);
        if (!oSeeAlso.isEmpty()) {
            out.println(" *");
            for(Value s: oSeeAlso) {
                if (s instanceof URI) {
                    out.printf(" * @see <a href=\"%s\">%s</a>%n", s.stringValue(), s.stringValue());
                }
            }
        }
        out.println(" */");
        //class Definition
        out.printf("public class %s {%n", className);
        out.println();

        //constants
        out.printf(getIndent(1) + "/** {@code %s} **/%n", prefix);
        out.printf(getIndent(1) + "public static final String NAMESPACE = \"%s\";%n",prefix);
        out.println();
        out.printf(getIndent(1) + "/** {@code %s} **/%n", name.toLowerCase());
        out.printf(getIndent(1) + "public static final String PREFIX = \"%s\";%n",name.toLowerCase());
        out.println();

        //and now the resources
        TreeSet<String> keys = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        keys.addAll(splitUris.keySet());

        for(String key : keys) {
            Literal comment = getFirstExistingObjectLiteral(model, splitUris.get(key), RDFS.COMMENT, DCTERMS.DESCRIPTION, SKOS.DEFINITION, DC.DESCRIPTION);
            Literal label = getFirstExistingObjectLiteral(model, splitUris.get(key), RDFS.LABEL, DCTERMS.TITLE, DC.TITLE);

            out.println(getIndent(1) + "/**");
            if (label != null) {
                out.printf(getIndent(1) + " * %s%n", label.getLabel());
                out.println(getIndent(1) + " * <p>");
            }
            out.printf(getIndent(1) + " * {@code %s}.%n", splitUris.get(key).stringValue());
            if (comment != null) {
                out.println(getIndent(1) + " * <p>");
                out.printf(getIndent(1) + " * %s%n", WordUtils.wrap(comment.getLabel().replaceAll("\\s+", " "), 70, "\n\t * ", false));
            }
            out.println(getIndent(1) + " *");
            out.printf(getIndent(1) + " * @see <a href=\"%s\">%s</a>%n", splitUris.get(key), key);
            out.println(getIndent(1) + " */");
            out.printf(getIndent(1) + "public static final URI %s;%n", key);
            out.println();
        }

        //static init
        out.println(getIndent(1) + "static {");
        out.printf(getIndent(2) + "ValueFactory factory = ValueFactoryImpl.getInstance();%n");
        out.println();
        for(String key : keys) {
            out.printf(getIndent(2) + "%s = factory.createURI(%s, \"%s\");%n",key,className+".NAMESPACE",key);
        }
        out.println(getIndent(1) + "}");
        out.println();

        //private contructor to avoid instances
        out.printf(getIndent(1) + "private %s() {%n", className);
        out.println(getIndent(2) + "//static access only");
        out.println(getIndent(1) + "}");
        out.println();

        //class end
        out.println("}");
    }

    private String getIndent(int level) {
        return StringUtils.repeat(getIndent(), level);
    }

    private Literal getFirstExistingObjectLiteral(Model model, Resource subject, URI... predicates) throws GraphUtilException {
        for (URI predicate: predicates) {
            Literal literal = GraphUtil.getOptionalObjectLiteral(model, subject, predicate);
            if (literal != null) {
                return literal;
            }
        }
        return null;
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

    public void setIndent(String indent) {
        this.indent = indent;
    }

    public String getIndent() {
        return indent;
    }
}
