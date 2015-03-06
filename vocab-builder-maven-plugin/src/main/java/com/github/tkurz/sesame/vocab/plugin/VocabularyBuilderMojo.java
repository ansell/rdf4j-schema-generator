package com.github.tkurz.sesame.vocab.plugin;

import com.github.tkurz.sesame.vocab.GenerationException;
import com.github.tkurz.sesame.vocab.VocabBuilder;
import com.google.common.base.CaseFormat;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.utils.DateUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;
import org.openrdf.model.util.GraphUtilException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParserRegistry;
import org.openrdf.rio.Rio;
import org.slf4j.impl.StaticLoggerBinder;
import org.sonatype.plexus.build.incremental.BuildContext;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.*;

/**
 * Maven Plugin to generate Sesame Vocabulary Classes.
 *
 * @author Jakob Frank (jakob@apache.org)
 */
@Mojo(name = "generate",
        defaultPhase = LifecyclePhase.GENERATE_SOURCES,
        requiresDependencyResolution = ResolutionScope.COMPILE,
        requiresProject = true)
public class VocabularyBuilderMojo extends AbstractMojo {

    @Parameter(property = "output", defaultValue = "${project.build.directory}/generated-sources/sesame-vocabs")
    private File outputDirectory;

    @Parameter(property = "bundleOutput", defaultValue = "${project.build.directory}/generated-resources/sesame-vocabs")
    private File bundleOutputDirectory;

    @Parameter(property = "remoteCacheDir", defaultValue = "${project.build.directory}/vocab-builder-maven-plugin.cache")
    private File remoteCacheDir;

    @Parameter
    private List<Vocabulary> vocabularies;

    @Parameter(property = "url")
    private URL url;
    @Parameter(property = "file")
    private File file;
    @Parameter(property = "name")
    private String name;
    @Parameter(property = "className")
    private String className;

    @Parameter(property = "package")
    private String packageName;

    @Parameter(alias = "format")
    private String mimeType;

    @Parameter(property = "preferredLanguage")
    private String preferredLanguage;

    @Parameter(property = "createResourceBundles", defaultValue = "false")
    private boolean createResourceBundles;

    @Parameter(property = "createStringConstants", defaultValue = "false")
    private boolean createStringConstants;
    @Parameter(property = "stringConstantPrefix")
    private String stringConstantPrefix;
    
    @Parameter(property = "constantCase")
    private CaseFormat constantCase;

    @Parameter(property = "project", required = true, readonly = true)
    private MavenProject project;

    @Component
    private BuildContext buildContext;

    @Component
    private MavenSession mavenSession;

    @Component
    private PluginDescriptor pluginDescriptor;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        StaticLoggerBinder.getSingleton().setLog(getLog());
        try {
            final Path output = outputDirectory.toPath();
            final Path bundleOutput = bundleOutputDirectory.toPath();

            if (vocabularies == null) {
                vocabularies = new ArrayList<>();
            }

            if (url != null) {
                vocabularies.add(0, Vocabulary.create(url, name, className));
            } else if (file != null) {
                vocabularies.add(0, Vocabulary.create(file, name, className));
            }


            Files.createDirectories(output);
            Files.createDirectories(bundleOutput);


            final Log log = getLog();
            log.info(String.format("Generating %d vocabularies", vocabularies.size()));

            for (Vocabulary vocab : vocabularies) {
                final String displayName = vocab.getName() != null ? vocab.getName() : vocab.getClassName();
                if (displayName == null) {
                    log.error("Incomplete Configuration: Vocabulary without className or name");
                    throw new MojoExecutionException("Incomplete Configuration: Vocabulary without className or name");
                }
                try {
                    String language = preferredLanguage;
                    if (vocab.getPreferredLanguage() != null) {
                        language = vocab.getPreferredLanguage();
                    }

                    String mime = vocab.getMimeType();

                    if (mime == null) {
                        if (vocab.getUrl() != null) {
                            RDFFormat guess = Rio.getParserFormatForFileName(vocab.getUrl().toString());
                            if (guess != null) {
                                mime = guess.getDefaultMIMEType();
                            }
                        } else if (vocab.getFile() != null) {
                            RDFFormat guess = Rio.getParserFormatForFileName(vocab.getFile().toString());
                            if (guess != null) {
                                mime = guess.getDefaultMIMEType();
                            }
                        }
                    }

                    if (mime == null) {
                        mime = mimeType;
                    }

                    final VocabBuilder builder;
                    if (vocab.getUrl() != null) {
                        if (mavenSession.isOffline()) {
                            log.info(String.format("Offline-Mode: Skipping generation of %s from %s", displayName, vocab.getUrl()));
                            continue;
                        } else {
                            try {

                                File cache = fetchVocab(vocab.getUrl(), displayName, vocab);
                                if (cache != null) {
                                    builder = new VocabBuilder(cache.getAbsolutePath(), mime);
                                } else {
                                    log.info(String.format("Skipping %s, vocabulary is did not change", displayName));
                                    continue;
                                }
                            } catch (IOException e) {
                                final String msg = String.format("Error fetching remote vocabulary %s: %s", displayName, e.getMessage());
                                log.error(msg);
                                throw new MojoFailureException(msg, e);
                            }
                        }
                    } else if (vocab.getFile() != null) {
                        // Incremental builds can skip this file if the following returns true
                        if (!buildContext.hasDelta(vocab.getFile())) {
                            log.debug(String.format("Skipping %s, vocabulary is did not change", displayName));
                            continue;
                        }
                        log.info(String.format("Generating %s vocabulary", displayName));
                        buildContext.removeMessages(vocab.getFile());

                        builder = new VocabBuilder(vocab.getFile().getAbsolutePath(), mime);
                    } else {
                        final String msg = String.format("Incomplete Configuration for %s: Vocabulary without URL or FILE param!", displayName);
                        log.error(msg);
                        throw new MojoExecutionException(msg);
                    }

                    log.debug(String.format("    Setting default preferred language: %s", language));
                    builder.setPreferredLanguage(language);

                    if (vocab.getPackageName() != null) {
                        log.debug(String.format("    Setting package: %s", vocab.getPackageName()));
                        builder.setPackageName(vocab.getPackageName());
                    } else if (packageName != null) {
                        log.debug(String.format("    Setting default package: %s", packageName));
                        builder.setPackageName(packageName);
                    } else {
                        log.warn(String.format("%s is using discouraged default package", displayName));
                    }

                    if (vocab.getConstantCase() != null) {
                        log.debug(String.format("    Setting constant case: %s", vocab.getConstantCase()));
                        builder.setConstantCase(vocab.getConstantCase());
                    } else {
                        log.debug(String.format("    Setting default constant case: %s", constantCase));
                        builder.setConstantCase(constantCase);
                    }

                    if (vocab.getPrefix() != null) {
                        builder.setPrefix(vocab.getPrefix());
                    }
                    
                    builder.setName(vocab.getName());

                    String fName;
                    if (vocab.getClassName() != null) {
                        fName = vocab.getClassName() + ".java";
                    } else if (vocab.getName() != null) {
                        fName = StringUtils.capitalize(vocab.getName()) + ".java";
                    } else {
                        throw new MojoExecutionException("Incomplete Configuration: Vocabulary without className or name");
                    }

                    Path target = output;
                    if (builder.getPackageName() != null) {
                        target = target.resolve(builder.getPackageName().replaceAll("\\.", "/"));
                        Files.createDirectories(target);
                    }
                    // when string constant generation set, specify a prefix
                    if ( createStringConstants ) {
                    	// when prefix set, the builder will generate string constants in addition to the URI's
                    	// when no string constant prefix set, use a single underscore by default
                    	builder.setStringPropertyPrefix((stringConstantPrefix==null? "_": stringConstantPrefix));
                    }
                    else {
                    	// be sure to not generate String constants
                    	builder.setStringPropertyPrefix(null);
                    }
                    final Path vFile = target.resolve(fName);
                    final String className = vFile.getFileName().toString().replaceFirst("\\.java$", "");
                    try (final PrintWriter out = new PrintWriter(
                            new OutputStreamWriter(
                                    buildContext.newFileOutputStream(vFile.toFile()), StandardCharsets.UTF_8)
                    )) {
                        if (builder.getPackageName() != null) {
                            log.info(String.format("    Generating vocabulary class: %s.%s", builder.getPackageName(), className));
                        } else {
                            log.info(String.format("    Generating vocabulary class: %s", className));
                        }
                        builder.generate(className, out);
                    }
                    if (vocab.isCreateResourceBundlesSet() && vocab.isCreateResourceBundles() || createResourceBundles) {
                        Path bundleTarget = bundleOutput;
                        if (builder.getPackageName() != null) {
                            bundleTarget = bundleTarget.resolve(builder.getPackageName().replaceAll("\\.", "/"));
                            Files.createDirectories(bundleTarget);
                        }
                        final HashMap<String, Properties> bundles = builder.generateResourceBundle(className);
                        for (String bKey : bundles.keySet()) {
                            try (final Writer out = new OutputStreamWriter(
                                    buildContext.newFileOutputStream(bundleTarget.resolve(bKey + ".properties").toFile()), StandardCharsets.UTF_8)) {
                                log.info(String.format("    Generating ResourceBundle: %s", bKey));
                                bundles.get(bKey).store(out, String.format("Generated by %s:%s v%s (%s)",
                                        pluginDescriptor.getGroupId(), pluginDescriptor.getArtifactId(), pluginDescriptor.getVersion(), pluginDescriptor.getName()));
                            }
                        }

                        Resource rsc = new Resource();
                        rsc.setDirectory(bundleOutput.toAbsolutePath().toString());
                        rsc.setFiltering(false);
                        log.debug(String.format("Adding %s as additional resource folder", rsc));
                        project.addResource(rsc);
                    }
                    log.info(String.format("Generated %s", displayName));

                } catch (RDFParseException e) {
                    throw new MojoFailureException(String.format("Could not parse vocabulary %s: %s", displayName, e.getMessage()));
                } catch (GraphUtilException e) {
                    throw new MojoExecutionException("Internal Mojo Error", e);
                } catch (GenerationException e) {
                    throw new MojoFailureException(String.format("Could not generate vocabulary %s: %s", displayName, e.getMessage()));
                } catch (URISyntaxException e) {
                    throw new MojoFailureException(String.format("Invalid URL for vocabulary %s: %s", displayName, vocab.getUrl()));
                }
            }
            if (project != null) {
                log.debug(String.format("Adding %s as additional compile source", output.toString()));
                project.addCompileSourceRoot(output.toString());
            }
            log.info("Vocabulary generation complete");
        } catch (IOException e) {
            throw new MojoExecutionException("Could not write Vocabularies", e);
        }
    }

    private File fetchVocab(URL url, final String displayName, final Vocabulary vocab) throws URISyntaxException, IOException {
        final HttpClientBuilder clientBuilder = HttpClientBuilder.create()
                .setUserAgent(
                        String.format("%s:%s/%s (%s) %s:%s/%s (%s)",
                                pluginDescriptor.getGroupId(), pluginDescriptor.getArtifactId(), pluginDescriptor.getVersion(), pluginDescriptor.getName(),
                                project.getGroupId(), project.getArtifactId(), project.getVersion(), project.getName())
                );

        final Path cache = remoteCacheDir.toPath();
        Files.createDirectories(cache);

        try (CloseableHttpClient client = clientBuilder.build()) {
            final HttpUriRequest request = RequestBuilder.get()
                    .setUri(url.toURI())
                    .setHeader(HttpHeaders.ACCEPT, getAcceptHeaderValue())
                    .build();

            return client.execute(request, new ResponseHandler<File>() {
                @Override
                public File handleResponse(HttpResponse response) throws IOException {
                    final Log log = getLog();
                    // Check the mime-type
                    String mime = mimeType;
                    if (vocab.getMimeType() != null) {
                        mime = vocab.getMimeType();
                    }
                    if (mime == null) {
                        mime = getHeaderValue(response, HttpHeaders.CONTENT_TYPE);
                        log.debug("Using mime-type from response-header: " + mime);
                    }

                    final RDFFormat format = Rio.getParserFormatForMIMEType(mime);
                    final String fName;
                    if (format == null) {
                        fName = displayName + ".cache";
                        log.debug(String.format("Unknown format, cache will be %s", fName));
                    } else {
                        fName = displayName + "." + format.getDefaultFileExtension();
                        log.debug(String.format("%s format, cache will be %s", format.getName(), fName));
                    }

                    Path cacheFile = cache.resolve(fName);
                    if (Files.exists(cacheFile)) {
                        log.debug(String.format("Cache-File %s found, checking if up-to-date", cacheFile));
                        // Check if the cache is up-to-date
                        final FileTime fileTime = Files.getLastModifiedTime(cache);
                        final Date remoteDate = DateUtils.parseDate(getHeaderValue(response, HttpHeaders.LAST_MODIFIED));

                        if (remoteDate != null && remoteDate.getTime() < fileTime.toMillis()) {
                            // The remote file was changed before the cache, so no action required
                            log.debug(String.format("%tF %<tT is after %tF %<tT, no action required", new Date(fileTime.toMillis()), remoteDate));
                            return null;
                        } else {
                            log.debug(String.format("remote file is newer - need to rebuild vocabulary"));
                        }
                    } else {
                        log.debug(String.format("No Cache-File %s, need to fetch", cacheFile));
                    }

                    final File cf = cacheFile.toFile();
                    FileUtils.copyInputStreamToFile(response.getEntity().getContent(), cf);
                    log.info(String.format("Fetched vocabulary definition for %s from %s", displayName, request.getURI()));
                    return cf;
                }

                private String getHeaderValue(HttpResponse response, String header) {
                    final Header h = response.getFirstHeader(header);
                    if (h != null) {
                        return h.getValue();
                    } else {
                        return null;
                    }
                }
            });
        }
    }

    private String getAcceptHeaderValue() {
        final Set<RDFFormat> rdfFormats = RDFParserRegistry.getInstance().getKeys();
        final Iterator<String> acceptParams = RDFFormat.getAcceptParams(rdfFormats, false, RDFFormat.TURTLE).iterator();
        if (acceptParams.hasNext()) {
            final StringBuilder sb = new StringBuilder();
            while (acceptParams.hasNext()) {
                sb.append(acceptParams.next());
                if (acceptParams.hasNext()) {
                    sb.append(", ");
                }
            }
            return sb.toString();
        } else {
            return null;
        }
    }

}
