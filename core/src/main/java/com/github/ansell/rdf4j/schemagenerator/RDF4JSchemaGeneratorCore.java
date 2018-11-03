package com.github.ansell.rdf4j.schemagenerator;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import org.eclipse.rdf4j.common.io.MavenUtil;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.ParseErrorLogger;
import org.eclipse.rdf4j.rio.helpers.XMLParserSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.ansell.rdf4j.schemagenerator.internal.SchemaRecordImpl;
import com.google.common.base.CaseFormat;
import com.google.common.collect.Sets;

import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.BeansWrapperBuilder;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModel;

/**
 * The core implementation of the Schema Generation.
 *
 * @author Thomas Kurz (tkurz@apache.org)
 * @author Jakob Frank (jakob@apache.org)
 * @author Peter Ansell p_ansell@yahoo.com
 */
public class RDF4JSchemaGeneratorCore {

    private static final Logger log = LoggerFactory.getLogger(RDF4JSchemaGeneratorCore.class);

    private static final IRI[] COMMENT_PROPERTIES = new IRI[] { RDFS.COMMENT, DCTERMS.DESCRIPTION,
            SKOS.DEFINITION, DC.DESCRIPTION };
    private static final IRI[] LABEL_PROPERTIES = new IRI[] { RDFS.LABEL, DCTERMS.TITLE, DC.TITLE,
            SKOS.PREF_LABEL, SKOS.ALT_LABEL };
    private String templatePath = "/com/github/ansell/rdf4j/schemagenerator/javaStaticClassRDF4J.ftl";
    private String name = null;
    private String prefix = null;
    private String packageName = null;
    private String indent = "\t";
    private String language = null;
    private final Model model;
    private CaseFormat caseFormat;
    private CaseFormat stringCaseFormat;
    private CaseFormat localNameStringCaseFormat;
    private String stringPropertyPrefix;
    private String stringPropertySuffix;
    private String localNameStringPropertyPrefix;
    private String localNameStringPropertySuffix;
    private String metaInfServicesInterface;
    private final Set<String> createdFields = new HashSet<>();
    private static Set<String> reservedWords = Sets.newHashSet("abstract", "assert", "boolean",
            "break", "byte", "case", "catch", "char", "class", "const", "default", "do", "double",
            "else", "enum", "extends", "false", "final", "finally", "float", "for", "goto", "if",
            "implements", "import", "instanceof", "int", "interface", "long", "native", "new",
            "null", "package", "private", "protected", "public", "return", "short", "static",
            "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient",
            "true", "try", "var", "void", "volatile", "while", "continue", "PREFIX", "NAMESPACE");

    /**
     * Create a new RDF4JSchemaGeneratorCore, reading the schema definition from
     * the provided file
     *
     * @param filename
     *            the input file to read the schema from
     * @param format
     *            the format of the schema file, may be {@code null}
     * @throws java.io.IOException
     *             if the file could not be read
     * @throws RDFParseException
     *             if the format of the schema could not be detected or is
     *             unknown.
     */
    public RDF4JSchemaGeneratorCore(final String filename, final String format)
            throws IOException, RDFParseException {
        this(filename, format != null ? Rio.getParserFormatForMIMEType(format).orElse(null) : null);
    }

    public RDF4JSchemaGeneratorCore(final String filename, final RDFFormat format)
            throws IOException, RDFParseException {
        final Path file = Paths.get(filename);
        if (!Files.exists(file)) {
            throw new FileNotFoundException(filename);
        }

        final RDFFormat rdfFormat = Optional.ofNullable(format).orElseGet(() -> {
            final RDFFormat result = Rio.getParserFormatForFileName(filename).orElse(null);
            log.trace("detected input format from filename {}: {}", filename, result);
            return result;
        });

        try (final InputStream inputStream = Files.newInputStream(file)) {
            log.trace("Loading input file: {}", file);
            final ParserConfig settings = new ParserConfig()
                    .set(XMLParserSettings.DISALLOW_DOCTYPE_DECL, false);
            model = Rio.parse(inputStream, "", rdfFormat, settings,
                    SimpleValueFactory.getInstance(), new ParseErrorLogger());
        }

        // import
        final Set<Resource> owlOntologies = model.filter(null, RDF.TYPE, OWL.ONTOLOGY).subjects();
        if (!owlOntologies.isEmpty()) {
            setPrefix(owlOntologies.iterator().next().stringValue());
        }
    }

    public void generate(OutputStream outputStream) throws GenerationException, IOException {
        String cName = getName();
        if (StringUtils.isBlank(cName)) {
            throw new GenerationException("could not detect name, please set explicitly");
        }
        // noinspection ConstantConditions
        cName = WordUtils.capitalize(cName.replaceAll("\\W+", " ")).replaceAll("\\s+", "");

        generate(cName, new PrintWriter(outputStream));
    }

    public void generate(Path output) throws IOException, GenerationException {
        final String className = output.getFileName().toString().replaceFirst("\\.java$", "");
        try (PrintWriter out = new PrintWriter(
                Files.newBufferedWriter(output, StandardCharsets.UTF_8))) {
            generate(className, out);
        }
    }

    public void generate(String className, PrintWriter out)
            throws IOException, GenerationException {
        log.trace("classname: {}", className);
        if (StringUtils.isBlank(name)) {
            name = className;
        }
        if (StringUtils.isBlank(prefix)) {
            throw new GenerationException("could not detect prefix, please set explicitly");
        } else {
            log.debug("prefix: {}", prefix);
        }

        final Pattern pattern = Pattern.compile(Pattern.quote(getPrefix()) + "(.+)");
        final ConcurrentMap<String, IRI> splitUris = new ConcurrentHashMap<>();
        for (final Resource nextSubject : model.subjects()) {
            if (nextSubject instanceof IRI) {
                final Matcher matcher = pattern.matcher(nextSubject.stringValue());
                if (matcher.find()) {
                    final String k = matcher.group(1);
                    final IRI putIfAbsent = splitUris.putIfAbsent(k, (IRI) nextSubject);
                    if (putIfAbsent != null) {
                        log.warn("Conflicting keys found: uri={} key={} existing={}",
                                nextSubject.stringValue(), k, putIfAbsent);
                    }
                }
            }
        }

        final IRI pfx = SimpleValueFactory.getInstance().createIRI(prefix);
        final Literal oTitle = getFirstExistingObjectLiteral(model, pfx, getPreferredLanguage(),
                LABEL_PROPERTIES);
        final Literal oDescr = getFirstExistingObjectLiteral(model, pfx, getPreferredLanguage(),
                COMMENT_PROPERTIES);
        final Set<Value> oSeeAlso = model.filter(pfx, RDFS.SEEALSO, null).objects();

        final List<String> keys = new ArrayList<>(splitUris.keySet());
        Collections.sort(keys, String.CASE_INSENSITIVE_ORDER);

        final List<SchemaRecord> stringConstants = new ArrayList<>();

        // string constant values
        if (stringCaseFormat != null || StringUtils.isNotBlank(stringPropertyPrefix)
                || (StringUtils.isNotBlank(stringPropertySuffix))) {
            // add the possibility to add a string property with the namespace
            // for usage in
            for (final String key : keys) {
                final IRI nextIRI = splitUris.get(key);
                final Literal comment = getFirstExistingObjectLiteral(model, nextIRI,
                        getPreferredLanguage(), COMMENT_PROPERTIES);
                final Literal label = getFirstExistingObjectLiteral(model, nextIRI,
                        getPreferredLanguage(), LABEL_PROPERTIES);

                final String nextKey = cleanKey(String.format("%s%s%s",
                        StringUtils.defaultString(getStringPropertyPrefix()),
                        doCaseFormatting(key, getStringConstantCase()),
                        StringUtils.defaultString(getStringPropertySuffix())));
                checkField(className, nextKey);
                stringConstants.add(new SchemaRecordImpl(nextIRI, nextKey, key, label, comment));
            }
        }

        final List<SchemaRecord> localNameStringConstants = new ArrayList<>();

        // string constant values
        if (localNameStringCaseFormat != null
                || StringUtils.isNotBlank(localNameStringPropertyPrefix)
                || (StringUtils.isNotBlank(localNameStringPropertySuffix))) {
            // add the possibility to add a string property with the namespace
            // for usage in
            for (final String key : keys) {
                final IRI nextIRI = splitUris.get(key);
                final Literal comment = getFirstExistingObjectLiteral(model, nextIRI,
                        getPreferredLanguage(), COMMENT_PROPERTIES);
                final Literal label = getFirstExistingObjectLiteral(model, nextIRI,
                        getPreferredLanguage(), LABEL_PROPERTIES);
                final String localNameKey;
                try {
                    localNameKey = nextIRI.getLocalName();
                } catch (final Exception e) {
                    log.error("Could not get localName for: {}", key);
                    continue;
                }

                final String nextKey = cleanKey(String.format("%s%s%s",
                        StringUtils.defaultString(getLocalNameStringPropertyPrefix()),
                        doCaseFormatting(localNameKey, getLocalNameStringConstantCase()),
                        StringUtils.defaultString(getLocalNameStringPropertySuffix())));
                checkField(className, nextKey);
                localNameStringConstants
                        .add(new SchemaRecordImpl(nextIRI, nextKey, key, label, comment));
            }
        }

        final List<SchemaRecord> iriConstants = new ArrayList<>();

        // and now the resources
        for (final String key : keys) {
            final IRI nextIRI = splitUris.get(key);
            final Literal comment = getFirstExistingObjectLiteral(model, nextIRI,
                    getPreferredLanguage(), COMMENT_PROPERTIES);
            final Literal label = getFirstExistingObjectLiteral(model, nextIRI,
                    getPreferredLanguage(), LABEL_PROPERTIES);

            final String nextKey = cleanKey(doCaseFormatting(key, getConstantCase()));
            checkField(className, nextKey);
            iriConstants.add(new SchemaRecordImpl(nextIRI, nextKey, key, label, comment));
        }

        try {
            // Generate using Freemarker
            final Configuration cfg = new Configuration(Configuration.VERSION_2_3_25);

            cfg.setClassForTemplateLoading(RDF4JSchemaGeneratorCore.class, "/");
            cfg.setDefaultEncoding("UTF-8");

            final Template template = cfg.getTemplate(getTemplatePath());

            final Map<String, Object> templateData = new HashMap<>();
            templateData.put("indent", getIndent());
            templateData.put("packageName", packageName);
            templateData.put("title", oTitle);
            templateData.put("description", oDescr);
            templateData.put("seeAlsoUrls", oSeeAlso);
            templateData.put("className", className);
            templateData.put("prefix", prefix);
            templateData.put("name", name);
            templateData.put("stringConstants", stringConstants);
            templateData.put("localNameStringConstants", localNameStringConstants);
            templateData.put("iriConstants", iriConstants);

            final BeansWrapperBuilder builder = new BeansWrapperBuilder(
                    Configuration.VERSION_2_3_25);
            final BeansWrapper wrapper = builder.build();
            final TemplateHashModel staticModels = wrapper.getStaticModels();
            templateData.put("StringUtils",
                    staticModels.get("org.apache.commons.lang3.StringUtils"));
            templateData.put("WordUtils",
                    staticModels.get("org.apache.commons.lang3.text.WordUtils"));

            template.process(templateData, out);
        } catch (final TemplateException e) {
            throw new GenerationException(e);
        } finally {
            out.flush();
        }
    }

    private void checkField(String className, String fieldName) throws GenerationException {
        log.debug("checkField: {} {}", className, fieldName);
        if (!createdFields.add(fieldName)) {
            throw new GenerationException(
                    String.format("field %s.%s is defined twice", className, fieldName));
        }
    }

    public void generateResourceBundle(String baseName, Path bundleDir)
            throws GenerationException, IOException {
        final Map<String, Properties> bundles = generateResourceBundle(baseName);

        for (final String bKey : bundles.keySet()) {
            final Properties bundle = bundles.get(bKey);

            final Path file = bundleDir.resolve(bKey + ".properties");
            try (Writer w = Files.newBufferedWriter(file, Charset.forName("utf8"))) {
                bundle.store(w, String.format("ResourceBundle (%s) for %s, generated by %s v%s",
                        bKey, baseName, "com.github.ansell.rdf4j-schema-generator:schema-generator",
                        MavenUtil.loadVersion("com.github.ansell.rdf4j-schema-generator",
                                "schema-generator", "0.0.0-DEVELOP")));
            } catch (final IOException e) {
                log.error("Could not write Bundle {} to {}: {}", bKey, file, e);
                throw e;
            }
        }

    }

    public Map<String, Properties> generateResourceBundle(String baseName)
            throws GenerationException {
        final Pattern pattern = Pattern.compile(Pattern.quote(getPrefix()) + "(.+)");
        final Map<String, IRI> splitUris = new HashMap<>();
        for (final Resource nextSubject : model.subjects()) {
            if (nextSubject instanceof IRI) {
                final Matcher matcher = pattern.matcher(nextSubject.stringValue());
                if (matcher.find()) {
                    final String k = matcher.group(1);
                    splitUris.put(k, (IRI) nextSubject);
                }
            }
        }

        final List<String> keys = new ArrayList<>();
        keys.addAll(splitUris.keySet());
        Collections.sort(keys, String.CASE_INSENSITIVE_ORDER);

        final Map<String, Properties> bundles = new HashMap<>();
        // Default we have for sure
        bundles.put(baseName, new Properties());
        for (final String key : keys) {
            final IRI resource = splitUris.get(key);
            final String nextKey = cleanKey(doCaseFormatting(key, getConstantCase()));

            for (final IRI p : LABEL_PROPERTIES) {
                for (final Value v : model.filter(resource, p, null).objects()) {
                    if (v instanceof Literal) {
                        final Literal lit = (Literal) v;
                        final Optional<String> lang = lit.getLanguage();
                        final String nextPropertySuffix = ".label";

                        final Properties bundle = getBundleForLangTag(baseName, bundles, lang);
                        addPropertyToBundle(nextKey, lit, nextPropertySuffix, bundle);
                    }
                }
            }

            for (final IRI p : COMMENT_PROPERTIES) {
                for (final Value v : model.filter(resource, p, null).objects()) {
                    if (v instanceof Literal) {
                        final Literal lit = (Literal) v;
                        final Optional<String> lang = lit.getLanguage();
                        final String nextPropertySuffix = ".comment";

                        final Properties bundle = getBundleForLangTag(baseName, bundles, lang);
                        addPropertyToBundle(nextKey, lit, nextPropertySuffix, bundle);
                    }
                }
            }
        }

        if (getPreferredLanguage() != null) {
            log.debug("completing default Bundle with preferred language {}",
                    getPreferredLanguage());
            final Properties defaultBundle = bundles.get(baseName);
            final Properties prefBundle = bundles.get(baseName + "_" + getPreferredLanguage());
            if (prefBundle != null) {
                for (final Entry<Object, Object> key : prefBundle.entrySet()) {
                    final String nextKey = (String) key.getKey();
                    if (!defaultBundle.containsKey(nextKey)) {
                        log.trace("copying {} from {} to default Bundle", nextKey,
                                getPreferredLanguage());
                        defaultBundle.setProperty(nextKey, (String) key.getValue());
                    }
                }
            } else {
                log.warn("No Bundle data found for preferred language {}", getPreferredLanguage());
            }
        }
        return bundles;
    }

    private void addPropertyToBundle(String nextKey, final Literal lit, String nextPropertySuffix,
            final Properties bundle) {
        if (!bundle.containsKey(nextKey + nextPropertySuffix)) {
            bundle.put(nextKey + nextPropertySuffix, lit.getLabel().replaceAll("\\s+", " "));
        }
    }

    private Properties getBundleForLangTag(String baseName, Map<String, Properties> bundles,
            final Optional<String> lang) {
        final Properties bundle;
        if (!lang.isPresent()) {
            bundle = bundles.get(baseName);
        } else if (bundles.containsKey(baseName + "_" + lang.get())) {
            bundle = bundles.get(baseName + "_" + lang.get());
        } else {
            bundle = new Properties();
            bundles.put(baseName + "_" + lang.get(), bundle);
        }
        return bundle;
    }

    private String getIndent(int level) {
        return StringUtils.repeat(getIndent(), level);
    }

    private Literal getFirstExistingObjectLiteral(Model model, Resource subject, String lang,
            IRI... predicates) {
        for (final IRI predicate : predicates) {
            final Literal literal = getOptionalObjectLiteral(model, subject, predicate, lang);
            if (literal != null) {
                return literal;
            }
        }
        return null;
    }

    private Literal getOptionalObjectLiteral(Model model, Resource subject, IRI predicate,
            String lang) {
        final Set<Value> objects = model.filter(subject, predicate, null).objects();

        Literal result = null;

        for (final Value nextValue : objects) {
            if (nextValue instanceof Literal) {
                final Literal literal = (Literal) nextValue;
                final Optional<String> nextLang = literal.getLanguage();
                if (result == null
                        || (lang != null && nextLang.isPresent() && lang.equals(nextLang.get()))) {
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

        if (reservedWords.contains(s)) {
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

    public String getTemplatePath() {
        return templatePath;
    }

    public void setTemplatePath(String templatePath) {
        this.templatePath = templatePath;
    }

    public String getMetaInfServicesInterface() {
        return metaInfServicesInterface;
    }

    public void setMetaInfServicesInterface(String metaInfServicesInterface) {
        this.metaInfServicesInterface = metaInfServicesInterface;
    }
}
