package com.github.ansell.rdf4j.schemagenerator;

import com.google.common.collect.Sets;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.eclipse.rdf4j.common.io.MavenUtil;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.*;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CaseFormat;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The core implementation of the Schema Generation.
 *
 * @author Thomas Kurz (tkurz@apache.org)
 * @author Jakob Frank (jakob@apache.org)
 * @author Peter Ansell p_ansell@yahoo.com
 */
public class RDF4JSchemaGeneratorCore {

    private static final Logger log = LoggerFactory.getLogger(RDF4JSchemaGeneratorCore.class);

    private static final IRI[] COMMENT_PROPERTIES = new IRI[]{RDFS.COMMENT, DCTERMS.DESCRIPTION, SKOS.DEFINITION, DC.DESCRIPTION};
    private static final IRI[] LABEL_PROPERTIES = new IRI[]{RDFS.LABEL, DCTERMS.TITLE, DC.TITLE, SKOS.PREF_LABEL, SKOS.ALT_LABEL};
    private String name = null;
    private String prefix = null;
    private String packageName = null;
    private String indent = "\t";
    private String language = null;
    private final Model model;
    private CaseFormat caseFormat = null;
    private CaseFormat stringCaseFormat = null;
    private CaseFormat localNameStringCaseFormat = null;
    private String stringPropertyPrefix;
    private String stringPropertySuffix;
    private String localNameStringPropertyPrefix;
    private String localNameStringPropertySuffix;
    private Set<String> createdFields = new HashSet<>();
    private static Set<String> reservedWords = Sets.newHashSet("abstract","assert","boolean","break","byte","case","catch","char","class","const","default","do","double","else","enum","extends","false","final","finally","float","for","goto","if","implements","import","instanceof","int","interface","long","native","new","null","package","private","protected","public","return","short","static","strictfp","super","switch","synchronized","this","throw","throws","transient","true","try","void","volatile","while","continue","PREFIX","NAMESPACE");

    /**
     * Create a new RDF4JSchemaGeneratorCore, reading the schema definition from the provided file
     *
     * @param filename the input file to read the schema from
     * @param format   the format of the schema file, may be {@code null}
     * @throws java.io.IOException if the file could not be read
     * @throws RDFParseException   if the format of the schema could not be detected or is unknown.
     */
    public RDF4JSchemaGeneratorCore(String filename, String format) throws IOException, RDFParseException {
        this(filename, format != null ? Rio.getParserFormatForMIMEType(format).orElse(null) : null);
    }

    public RDF4JSchemaGeneratorCore(String filename, RDFFormat format) throws IOException, RDFParseException {
        Path file = Paths.get(filename);
        if (!Files.exists(file)) throw new FileNotFoundException(filename);

        if (format == null) {
            format = Rio.getParserFormatForFileName(filename).orElse(null);
            log.trace("detected input format from filename {}: {}", filename, format);
        }

        try (final InputStream inputStream = Files.newInputStream(file)) {
            log.trace("Loading input file");
            model = Rio.parse(inputStream, "", format);
        }

        //import
        Set<Resource> owlOntologies = model.filter(null, RDF.TYPE, OWL.ONTOLOGY).subjects();
        if (!owlOntologies.isEmpty()) {
            setPrefix(owlOntologies.iterator().next().stringValue());
        }
    }

    public void generate(OutputStream outputStream) throws GenerationException, IOException {
        String cName = getName();
        if (StringUtils.isBlank(cName)) {
            throw new GenerationException("could not detect name, please set explicitly");
        }
        //noinspection ConstantConditions
        cName = WordUtils.capitalize(cName.replaceAll("\\W+", " ")).replaceAll("\\s+", "");

        generate(cName, new PrintWriter(outputStream));
    }

    public void generate(Path output) throws IOException, GenerationException {
        final String className = output.getFileName().toString().replaceFirst("\\.java$", "");
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(output, StandardCharsets.UTF_8))) {
            generate(className, out);
        }
    }

    /**
     *
     */
    public void generate(String className, PrintWriter out) throws IOException, GenerationException {
        log.trace("classname: {}", className);
        if (StringUtils.isBlank(name)) {
            name = className;
        }
        if (StringUtils.isBlank(prefix)) {
            throw new GenerationException("could not detect prefix, please set explicitly");
        } else {
            log.debug("prefix: {}", prefix);
        }

        Pattern pattern = Pattern.compile(Pattern.quote(getPrefix()) + "(.+)");
        ConcurrentMap<String, IRI> splitUris = new ConcurrentHashMap<>();
        for (Resource nextSubject : model.subjects()) {
            if (nextSubject instanceof IRI) {
                Matcher matcher = pattern.matcher(nextSubject.stringValue());
                if (matcher.find()) {
                    String k = matcher.group(1);
                    IRI putIfAbsent = splitUris.putIfAbsent(k, (IRI) nextSubject);
                    if (putIfAbsent != null) {
                        log.warn("Conflicting keys found: uri={} key={} existing={}",
                                nextSubject.stringValue(), k, putIfAbsent);
                    }
                }
            }
        }

        //print

        //package is optional
        if (StringUtils.isNotBlank(packageName)) {
            out.printf("package %s;%n%n", getPackageName());
        }
        //imports
        out.println("import org.eclipse.rdf4j.model.IRI;");
        out.println("import org.eclipse.rdf4j.model.ValueFactory;");
        out.println("import org.eclipse.rdf4j.model.impl.SimpleValueFactory;");
        out.println();

        final IRI pfx = SimpleValueFactory.getInstance().createIRI(prefix);
        Literal oTitle = getFirstExistingObjectLiteral(model, pfx, getPreferredLanguage(), LABEL_PROPERTIES);
        Literal oDescr = getFirstExistingObjectLiteral(model, pfx, getPreferredLanguage(), COMMENT_PROPERTIES);
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
            for (Value s : oSeeAlso) {
                if (s instanceof IRI) {
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
        out.printf(getIndent(1) + "public static final String NAMESPACE = \"%s\";%n", prefix);
        out.println();
        out.printf(getIndent(1) + "/** {@code %s} **/%n", name.toLowerCase());
        out.printf(getIndent(1) + "public static final String PREFIX = \"%s\";%n", name.toLowerCase());
        out.println();

        List<String> keys = new ArrayList<>();
        keys.addAll(splitUris.keySet());
        Collections.sort(keys, String.CASE_INSENSITIVE_ORDER);

        //string constant values
        if (stringCaseFormat != null || StringUtils.isNotBlank(stringPropertyPrefix) || (StringUtils.isNotBlank(stringPropertySuffix))) {
            // add the possibility to add a string property with the namespace for usage in
            for (String key : keys) {
                final Literal comment = getFirstExistingObjectLiteral(model, splitUris.get(key), getPreferredLanguage(), COMMENT_PROPERTIES);
                final Literal label = getFirstExistingObjectLiteral(model, splitUris.get(key), getPreferredLanguage(), LABEL_PROPERTIES);

                out.println(getIndent(1) + "/**");
                if (label != null) {
                    out.printf(getIndent(1) + " * %s%n", label.getLabel());
                    out.println(getIndent(1) + " * <p>");
                }
                out.printf(getIndent(1) + " * {@code %s}.%n", splitUris.get(key).stringValue());
                if (comment != null) {
                    out.println(getIndent(1) + " * <p>");
                    out.printf(getIndent(1) + " * %s%n", WordUtils.wrap(comment.getLabel().replaceAll("\\s+", " "), 70, "\n" + getIndent(1) + " * ", false));
                }
                out.println(getIndent(1) + " *");
                out.printf(getIndent(1) + " * @see <a href=\"%s\">%s</a>%n", splitUris.get(key), key);
                out.println(getIndent(1) + " */");

                final String nextKey = cleanKey(String.format("%s%s%s", StringUtils.defaultString(getStringPropertyPrefix()),
                        doCaseFormatting(key, getStringConstantCase()),
                        StringUtils.defaultString(getStringPropertySuffix())));
                checkField(className, nextKey);
                out.printf(getIndent(1) + "public static final String %s = %s.NAMESPACE + \"%s\";%n",
                         nextKey, className, key);
                out.println();
            }
        }

        //and now the resources
        for (String key : keys) {
            Literal comment = getFirstExistingObjectLiteral(model, splitUris.get(key), getPreferredLanguage(), COMMENT_PROPERTIES);
            Literal label = getFirstExistingObjectLiteral(model, splitUris.get(key), getPreferredLanguage(), LABEL_PROPERTIES);

            out.println(getIndent(1) + "/**");
            if (label != null) {
                out.printf(getIndent(1) + " * %s%n", label.getLabel());
                out.println(getIndent(1) + " * <p>");
            }
            out.printf(getIndent(1) + " * {@code %s}.%n", splitUris.get(key).stringValue());
            if (comment != null) {
                out.println(getIndent(1) + " * <p>");
                out.printf(getIndent(1) + " * %s%n", WordUtils.wrap(comment.getLabel().replaceAll("\\s+", " "), 70, "\n" + getIndent(1) + " * ", false));
            }
            out.println(getIndent(1) + " *");
            out.printf(getIndent(1) + " * @see <a href=\"%s\">%s</a>%n", splitUris.get(key), key);
            out.println(getIndent(1) + " */");

            String nextKey = cleanKey(doCaseFormatting(key, getConstantCase()));
            checkField(className, nextKey);
            out.printf(getIndent(1) + "public static final IRI %s;%n", nextKey);
            out.println();
        }

        //static init
        out.println(getIndent(1) + "static {");
        out.printf(getIndent(2) + "ValueFactory factory = SimpleValueFactory.getInstance();%n");
        out.println();
        for (String key : keys) {
            String nextKey = cleanKey(doCaseFormatting(key, getConstantCase()));
            out.printf(getIndent(2) + "%s = factory.createIRI(%s.NAMESPACE, \"%s\");%n", nextKey, className, key);
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
        out.flush();
    }

    private void checkField(String className, String fieldName) throws GenerationException {
        if (!createdFields.add(fieldName)) {
            throw new GenerationException(String.format("field %s.%s is defined twice", className, fieldName));
        }
    }

    public void generateResourceBundle(String baseName, Path bundleDir) throws GenerationException, IOException {
        HashMap<String, Properties> bundles = generateResourceBundle(baseName);

        for (String bKey : bundles.keySet()) {
            final Properties bundle = bundles.get(bKey);

            final Path file = bundleDir.resolve(bKey + ".properties");
            try (Writer w = Files.newBufferedWriter(file, Charset.forName("utf8"))) {
                bundle.store(w, String.format("ResourceBundle (%s) for %s, generated by %s v%s",
                        bKey, baseName,
                        "com.github.ansell.rdf4j-schema-generator:schema-generator",
                        MavenUtil.loadVersion("com.github.ansell.rdf4j-schema-generator", "schema-generator", "0.0.0-DEVELOP")));
            } catch (IOException e) {
                log.error("Could not write Bundle {} to {}: {}", bKey, file, e);
                throw e;
            }
        }

    }

    public HashMap<String, Properties> generateResourceBundle(String baseName) throws GenerationException {
        Pattern pattern = Pattern.compile(Pattern.quote(getPrefix()) + "(.+)");
        HashMap<String, IRI> splitUris = new HashMap<>();
        for (Resource nextSubject : model.subjects()) {
            if (nextSubject instanceof IRI) {
                Matcher matcher = pattern.matcher(nextSubject.stringValue());
                if (matcher.find()) {
                    String k = matcher.group(1);
                    splitUris.put(k, (IRI) nextSubject);
                }
            }
        }

        List<String> keys = new ArrayList<>();
        keys.addAll(splitUris.keySet());
        Collections.sort(keys, String.CASE_INSENSITIVE_ORDER);

        HashMap<String, Properties> bundles = new HashMap<>();
        // Default we have for sure
        bundles.put(baseName, new Properties());
        for (String key : keys) {
            final IRI resource = splitUris.get(key);
            String nextKey = cleanKey(doCaseFormatting(key, getConstantCase()));

            for (IRI p : LABEL_PROPERTIES) {
                for (Value v : model.filter(resource, p, null).objects()) {
                    if (v instanceof Literal) {
                        final Literal lit = (Literal) v;
                        final Optional<String> lang = lit.getLanguage();
                        final Properties bundle;
                        if (!lang.isPresent()) {
                            bundle = bundles.get(baseName);
                        } else if (bundles.containsKey(baseName + "_" + lang.get())) {
                            bundle = bundles.get(baseName + "_" + lang.get());
                        } else {
                            bundle = new Properties();
                            bundles.put(baseName + "_" + lang.get(), bundle);
                        }

                        if (!bundle.containsKey(nextKey + ".label")) {
                            bundle.put(nextKey + ".label", lit.getLabel().replaceAll("\\s+", " "));
                        }
                    }
                }
            }

            for (IRI p : COMMENT_PROPERTIES) {
                for (Value v : model.filter(resource, p, null).objects()) {
                    if (v instanceof Literal) {
                        final Literal lit = (Literal) v;
                        final Optional<String> lang = lit.getLanguage();
                        final Properties bundle;
                        if (!lang.isPresent()) {
                            bundle = bundles.get(baseName);
                        } else if (bundles.containsKey(baseName + "_" + lang.get())) {
                            bundle = bundles.get(baseName + "_" + lang.get());
                        } else {
                            bundle = new Properties();
                            bundles.put(baseName + "_" + lang.get(), bundle);
                        }

                        if (!bundle.containsKey(nextKey + ".comment")) {
                            bundle.put(nextKey + ".comment", lit.getLabel().replaceAll("\\s+", " "));
                        }
                    }
                }
            }
        }

        if (getPreferredLanguage() != null) {
            log.debug("completing default Bundle with preferred language {}", getPreferredLanguage());
            final Properties defaultBundle = bundles.get(baseName);
            final Properties prefBundle = bundles.get(baseName + "_" + getPreferredLanguage());
            if (prefBundle != null) {
                for (Entry<Object, Object> key : prefBundle.entrySet()) {
                    String nextKey = (String)key.getKey();
                    if (!defaultBundle.containsKey(nextKey)) {
                        log.trace("copying {} from {} to default Bundle", nextKey, getPreferredLanguage());
                        defaultBundle.setProperty(nextKey, (String) key.getValue());
                    }
                }
            } else {
                log.warn("No Bundle data found for preferred language {}", getPreferredLanguage());
            }
        }
        return bundles;
    }

    private String getIndent(int level) {
        return StringUtils.repeat(getIndent(), level);
    }

    private Literal getFirstExistingObjectLiteral(Model model, Resource subject, String lang, IRI... predicates) {
        for (IRI predicate : predicates) {
            Literal literal = getOptionalObjectLiteral(model, subject, predicate, lang);
            if (literal != null) {
                return literal;
            }
        }
        return null;
    }

    private Literal getOptionalObjectLiteral(Model model, Resource subject,
                                             IRI predicate, String lang) {
        Set<Value> objects = model.filter(subject, predicate, null).objects();

        Literal result = null;

        for (Value nextValue : objects) {
            if (nextValue instanceof Literal) {
                final Literal literal = (Literal) nextValue;
                Optional<String> nextLang = literal.getLanguage();
                if (result == null || (lang != null && nextLang.isPresent() && lang.equals(nextLang.get()))) {
                    result = literal;
                }
            }
        }
        return result;
    }

    private String cleanKey(String s) {
        s = s.replaceAll("#", "");
        s = s.replaceAll("\\.", "_");
        s = s.replaceAll("-", "_");
        s = s.replaceAll("/", "_");

        if(reservedWords.contains(s)) {
            s = "_" + s;
        }

        return s;
    }

    private String doCaseFormatting(String key, CaseFormat targetFormat) {
        if (targetFormat == null) {
            return key;
        } else {
            CaseFormat originalFormat = CaseFormat.LOWER_CAMEL;
            if (Character.isUpperCase(key.charAt(0)) && key.contains("_")) {
                originalFormat = CaseFormat.UPPER_UNDERSCORE;
            } else if (Character.isUpperCase(key.charAt(0))) {
                originalFormat = CaseFormat.UPPER_CAMEL;
            } else if (key.contains("_")) {
                originalFormat = CaseFormat.LOWER_UNDERSCORE;
            } else if (key.contains("-")) {
                originalFormat = CaseFormat.LOWER_HYPHEN;
            }
            return originalFormat.to(targetFormat, key);
        }
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

    public void setPreferredLanguage(String language) {
        this.language = language;
    }

    public String getPreferredLanguage() {
        return language;
    }

    public void setConstantCase(CaseFormat caseFormat) {
        this.caseFormat = caseFormat;
    }

    public CaseFormat getConstantCase() {
        return caseFormat;
    }

    public CaseFormat getStringConstantCase() {
        return stringCaseFormat;
    }

    public void setStringConstantCase(CaseFormat stringCaseFormat) {
        this.stringCaseFormat = stringCaseFormat;
    }

    public CaseFormat getLocalNameStringConstantCase() {
        return localNameStringCaseFormat;
    }

    public void setLocalNameStringConstantCase(CaseFormat localNameStringCaseFormat) {
        this.localNameStringCaseFormat = localNameStringCaseFormat;
    }

    public String getStringPropertyPrefix() {
        return stringPropertyPrefix;
    }

    public void setStringPropertyPrefix(String stringPropertyPrefix) {
        this.stringPropertyPrefix = stringPropertyPrefix;
    }

    public String getStringPropertySuffix() {
		return stringPropertySuffix;
	}

	public void setStringPropertySuffix(String stringPropertySuffix) {
		this.stringPropertySuffix = stringPropertySuffix;
	}

    public String getLocalNameStringPropertyPrefix() {
        return localNameStringPropertyPrefix;
    }

    public void setLocalNameStringPropertyPrefix(String localNameStringPropertyPrefix) {
        this.localNameStringPropertyPrefix = localNameStringPropertyPrefix;
    }

    public String getLocalNameStringPropertySuffix() {
		return localNameStringPropertySuffix;
	}

	public void setLocalNameStringPropertySuffix(String localNameStringPropertySuffix) {
		this.localNameStringPropertySuffix = localNameStringPropertySuffix;
	}
}
