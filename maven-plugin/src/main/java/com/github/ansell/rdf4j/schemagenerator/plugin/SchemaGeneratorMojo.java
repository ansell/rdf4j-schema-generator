package com.github.ansell.rdf4j.schemagenerator.plugin;

import com.github.ansell.rdf4j.schemagenerator.GenerationException;
import com.github.ansell.rdf4j.schemagenerator.RDF4JSchemaGeneratorCore;
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
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParserRegistry;
import org.eclipse.rdf4j.rio.Rio;
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
 * Maven Plugin to generate RDF4J Vocabulary Classes.
 *
 * @author Jakob Frank (jakob@apache.org)
 * @author Peter Ansell p_ansell@yahoo.com
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES, requiresDependencyResolution = ResolutionScope.COMPILE, requiresProject = true)
public class SchemaGeneratorMojo extends AbstractMojo {

	@Parameter(property = "output", defaultValue = "${project.build.directory}/generated-sources/rdf4j-schemas")
	private File outputDirectory;

	@Parameter(property = "resourceOutput", defaultValue = "${project.build.directory}/generated-resources/rdf4j-schemas")
	private File resourceOutputDirectory;

	@Parameter(property = "remoteCacheDir", defaultValue = "${project.build.directory}/schema-generator-maven-plugin.cache")
	private File remoteCacheDir;

	@Parameter(property = "schemas")
	private List<SchemaConfig> schemas;

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

	@Parameter(property = "templateClassPathLocation", defaultValue = "/com/github/ansell/rdf4j/schemagenerator/javaStaticClassRDF4J.ftl")
	private String templatePath = "/com/github/ansell/rdf4j/schemagenerator/javaStaticClassRDF4J.ftl";

	@Parameter(property = "createResourceBundles", defaultValue = "true")
	private boolean createResourceBundles = true;

	@Parameter(property = "createMetaInfServices", defaultValue = "false")
	private boolean createMetaInfServices = false;
	@Parameter(property = "metaInfServicesInterface", defaultValue = "com.github.ansell.rdf4j.schemagenerator.Schema")
	private String metaInfServicesInterface = "com.github.ansell.rdf4j.schemagenerator.Schema";

	@Parameter(property = "createStringConstants", defaultValue = "true")
	private boolean createStringConstants = true;
	@Parameter(property = "stringConstantPrefix", defaultValue = "")
	private String stringConstantPrefix = "";
	@Parameter(property = "stringConstantSuffix", defaultValue = "_STRING")
	private String stringConstantSuffix = "_STRING";

	@Parameter(property = "createLocalNameStringConstants", defaultValue = "true")
	private boolean createLocalNameStringConstants = true;
	@Parameter(property = "localNameStringConstantPrefix", defaultValue = "")
	private String localNameStringConstantPrefix = "";
	@Parameter(property = "localNameStringConstantSuffix", defaultValue = "_LOCALNAME")
	private String localNameStringConstantSuffix = "_LOCALNAME";

	@Parameter(property = "constantCase", defaultValue = "UPPER_UNDERSCORE")
	private CaseFormat constantCase = CaseFormat.UPPER_UNDERSCORE;

	@Parameter(property = "stringConstantCase", defaultValue = "UPPER_UNDERSCORE")
	private CaseFormat stringConstantCase = CaseFormat.UPPER_UNDERSCORE;

	@Parameter(property = "localNameStringConstantCase", defaultValue = "UPPER_UNDERSCORE")
	private CaseFormat localNameStringConstantCase = CaseFormat.UPPER_UNDERSCORE;

	@Parameter(property = "project", required = true, readonly = true)
	private MavenProject project;

	@Component
	private BuildContext buildContext;

	@Parameter(defaultValue = "${session}", readonly = true)
	private MavenSession mavenSession;

	@Parameter(defaultValue = "${plugin}", readonly = true)
	private PluginDescriptor pluginDescriptor;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		StaticLoggerBinder.getSingleton().setLog(getLog());

		try {
			final Log log = getLog();
			log.info(String.format("Generating %d schemas", schemas.size()));

			if (schemas == null) {
				schemas = new ArrayList<>();
			}

			if (url != null) {
				schemas.add(0, SchemaConfig.create(url, name, className));
			} else if (file != null) {
				schemas.add(0, SchemaConfig.create(file, name, className));
			}

			final Path output = outputDirectory.toPath();
			final Path resourceOutput = resourceOutputDirectory.toPath();
			Files.createDirectories(output);
			Files.createDirectories(resourceOutput);

			Path metaInfServicesFolderTarget = resourceOutput.resolve("META-INF").resolve("services");
			Files.createDirectories(metaInfServicesFolderTarget);
			Path metaInfServicesTarget = metaInfServicesFolderTarget.resolve(metaInfServicesInterface);
			try (final Writer metaInfServicesOut = new OutputStreamWriter(
					buildContext.newFileOutputStream(metaInfServicesTarget.toFile()), StandardCharsets.UTF_8)) {

				for (SchemaConfig nextSchema : schemas) {
					final String displayName = nextSchema.getName() != null ? nextSchema.getName()
							: nextSchema.getClassName();
					if (displayName == null) {
						log.error("Incomplete Configuration: Schema without className or name");
						throw new MojoExecutionException("Incomplete Configuration: Schema without className or name");
					}
					try {
						String language = preferredLanguage;
						if (nextSchema.getPreferredLanguage() != null) {
							language = nextSchema.getPreferredLanguage();
						}

						String mime = nextSchema.getMimeType();

						if (mime == null) {
							if (nextSchema.getUrl() != null) {
								Optional<RDFFormat> guess = Rio
										.getParserFormatForFileName(nextSchema.getUrl().toString());
								if (guess.isPresent()) {
									mime = guess.get().getDefaultMIMEType();
								}
							}
						}

						if (mime == null) {
							if (nextSchema.getFile() != null) {
								Optional<RDFFormat> guess = Rio
										.getParserFormatForFileName(nextSchema.getFile().toString());
								if (guess.isPresent()) {
									mime = guess.get().getDefaultMIMEType();
								}
							}
						}

						if (mime == null) {
							mime = mimeType;
						}

						final RDF4JSchemaGeneratorCore builder;
						if (nextSchema.getUrl() != null) {
							if (mavenSession.isOffline()) {
								log.info(String.format("Offline-Mode: Skipping generation of %s from %s", displayName,
										nextSchema.getUrl()));
								continue;
							} else {
								try {

									File cache = fetchSchema(nextSchema.getUrl(), displayName, nextSchema);
									if (cache != null) {
										builder = new RDF4JSchemaGeneratorCore(cache.getAbsolutePath(), mime);
									} else {
										log.info(String.format("Skipping %s, schema is did not change", displayName));
										continue;
									}
								} catch (IOException e) {
									final String msg = String.format("Error fetching remote schema %s: %s", displayName,
											e.getMessage());
									log.error(msg);
									throw new MojoFailureException(msg, e);
								}
							}
						} else if (nextSchema.getFile() != null) {
							// Incremental builds can skip this file if the
							// following returns true
							if (!buildContext.hasDelta(nextSchema.getFile())) {
								log.debug(String.format("Skipping %s, schema is did not change", displayName));
								continue;
							}
							log.info(String.format("Generating %s schema", displayName));
							buildContext.removeMessages(nextSchema.getFile());

							builder = new RDF4JSchemaGeneratorCore(nextSchema.getFile().getAbsolutePath(), mime);
						} else {
							final String msg = String.format(
									"Incomplete Configuration for %s: Schema without URL or FILE param!", displayName);
							log.error(msg);
							throw new MojoExecutionException(msg);
						}

						log.debug(String.format("    Setting default preferred language: %s", language));
						builder.setPreferredLanguage(language);

						if (nextSchema.getPackageName() != null) {
							log.debug(String.format("    Setting package: %s", nextSchema.getPackageName()));
							builder.setPackageName(nextSchema.getPackageName());
						} else if (packageName != null) {
							log.debug(String.format("    Setting default package: %s", packageName));
							builder.setPackageName(packageName);
						} else {
							log.warn(String.format("%s is using discouraged default package", displayName));
						}

						if (nextSchema.getConstantCase() != null) {
							log.debug(String.format("    Setting constant case: %s", nextSchema.getConstantCase()));
							builder.setConstantCase(nextSchema.getConstantCase());
						} else {
							log.debug(String.format("    Setting default constant case: %s", constantCase));
							builder.setConstantCase(constantCase);
						}

						if (nextSchema.getPrefix() != null) {
							builder.setPrefix(nextSchema.getPrefix());
						}

						builder.setName(nextSchema.getName());

						String fName;
						if (nextSchema.getClassName() != null) {
							fName = nextSchema.getClassName() + ".java";
						} else if (nextSchema.getName() != null) {
							fName = StringUtils.capitalize(nextSchema.getName()) + ".java";
						} else {
							throw new MojoExecutionException(
									"Incomplete Configuration: Schema without className or name");
						}

						Path target = output;
						if (builder.getPackageName() != null) {
							target = target.resolve(builder.getPackageName().replaceAll("\\.", "/"));
							Files.createDirectories(target);
						}
						// when string constant generation set, specify prefix
						// and suffix
						if (createStringConstants) {
							// when prefix set, the builder will generate string
							// constants in addition to the URI's
							// when no string constant prefix set, use a single
							// underscore by default
							builder.setStringPropertyPrefix(stringConstantPrefix);
							builder.setStringPropertySuffix(stringConstantSuffix);
							builder.setStringConstantCase(stringConstantCase);
						} else {
							// be sure to not generate String constants
							builder.setStringPropertyPrefix(null);
							builder.setStringPropertySuffix(null);
							builder.setStringConstantCase(null);
						}
						// when string constant generation set, specify prefix
						// and suffix
						if (createLocalNameStringConstants) {
							// when prefix set, the builder will generate string
							// constants for the local names
							builder.setLocalNameStringPropertyPrefix(localNameStringConstantPrefix);
							builder.setLocalNameStringPropertySuffix(localNameStringConstantSuffix);
							builder.setLocalNameStringConstantCase(localNameStringConstantCase);
						} else {
							// be sure to not generate String constants
							builder.setLocalNameStringPropertyPrefix(null);
							builder.setLocalNameStringPropertySuffix(null);
							builder.setLocalNameStringConstantCase(null);
						}
						final Path vFile = target.resolve(fName);
						final String className = vFile.getFileName().toString().replaceFirst("\\.java$", "");
						try (final PrintWriter out = new PrintWriter(new OutputStreamWriter(
								buildContext.newFileOutputStream(vFile.toFile()), StandardCharsets.UTF_8))) {
							if (builder.getPackageName() != null) {
								log.info(String.format("    Generating schema class: %s.%s", builder.getPackageName(),
										className));
							} else {
								log.info(String.format("    Generating schema class: %s", className));
							}
							builder.generate(className, out);
						}
						if (nextSchema.isCreateResourceBundlesSet() && nextSchema.isCreateResourceBundles()
								|| createResourceBundles) {
							Path bundleTarget = resourceOutput;
							if (builder.getPackageName() != null) {
								bundleTarget = bundleTarget.resolve(builder.getPackageName().replaceAll("\\.", "/"));
								Files.createDirectories(bundleTarget);
							}
							final Map<String, Properties> bundles = builder.generateResourceBundle(className);
							for (String bKey : bundles.keySet()) {
								try (final Writer out = new OutputStreamWriter(
										buildContext.newFileOutputStream(
												bundleTarget.resolve(bKey + ".properties").toFile()),
										StandardCharsets.UTF_8)) {
									log.info(String.format("    Generating ResourceBundle: %s", bKey));
									bundles.get(bKey).store(out,
											String.format("Generated by %s:%s v%s (%s)", pluginDescriptor.getGroupId(),
													pluginDescriptor.getArtifactId(), pluginDescriptor.getVersion(),
													pluginDescriptor.getName()));
								}
							}
						}
						if (createMetaInfServices) {
							log.info(String.format("    Generating META-INF/services/%s: %s", metaInfServicesInterface,
									nextSchema.getClassName()));
							if (builder.getPackageName() != null) {
								metaInfServicesOut.write(builder.getPackageName());
								metaInfServicesOut.write('.');
							}
							metaInfServicesOut.write(nextSchema.getClassName());
							metaInfServicesOut.write('\n');
						}

						Resource rsc = new Resource();
						rsc.setDirectory(resourceOutput.toAbsolutePath().toString());
						rsc.setFiltering(false);
						log.debug(String.format("Adding %s as additional resource folder", rsc));
						project.addResource(rsc);

						log.info(String.format("Generated %s", displayName));

					} catch (RDFParseException e) {
						throw new MojoFailureException(
								String.format("Could not parse schema %s: %s", displayName, e.getMessage()));
					} catch (GenerationException e) {
						throw new MojoFailureException(
								String.format("Could not generate schema %s: %s", displayName, e.getMessage()));
					} catch (URISyntaxException e) {
						throw new MojoFailureException(
								String.format("Invalid URL for schema %s: %s", displayName, nextSchema.getUrl()));
					}
				}
				if (project != null) {
					log.debug(String.format("Adding %s as additional compile source", output.toString()));
					project.addCompileSourceRoot(output.toString());
				}
				log.info("Schema generation complete");
			}
		} catch (IOException e) {
			throw new MojoExecutionException("Could not write Schemas", e);
		}
	}

	private File fetchSchema(URL url, final String displayName, final SchemaConfig nextSchema)
			throws URISyntaxException, IOException {
		final HttpClientBuilder clientBuilder = HttpClientBuilder.create()
				.setUserAgent(String.format("%s:%s/%s (%s) %s:%s/%s (%s)", pluginDescriptor.getGroupId(),
						pluginDescriptor.getArtifactId(), pluginDescriptor.getVersion(), pluginDescriptor.getName(),
						project.getGroupId(), project.getArtifactId(), project.getVersion(), project.getName()));

		final Path cache = remoteCacheDir.toPath();
		Files.createDirectories(cache);

		try (CloseableHttpClient client = clientBuilder.build()) {
			final HttpUriRequest request = RequestBuilder.get().setUri(url.toURI())
					.setHeader(HttpHeaders.ACCEPT, getAcceptHeaderValue()).build();

			return client.execute(request, new ResponseHandler<File>() {
				@Override
				public File handleResponse(HttpResponse response) throws IOException {
					final Log log = getLog();
					// Check the mime-type
					String mime = mimeType;
					if (nextSchema.getMimeType() != null) {
						mime = nextSchema.getMimeType();
					}
					if (mime == null) {
						mime = getHeaderValue(response, HttpHeaders.CONTENT_TYPE);
						log.debug("Using mime-type from response-header: " + mime);
					}

					final Optional<RDFFormat> format = Rio.getParserFormatForMIMEType(mime);
					final String fName;
					if (!format.isPresent()) {
						fName = displayName + ".cache";
						log.debug(String.format("Unknown format, cache will be %s", fName));
					} else {
						fName = displayName + "." + format.get().getDefaultFileExtension();
						log.debug(String.format("%s format, cache will be %s", format.get().getName(), fName));
					}

					Path cacheFile = cache.resolve(fName);
					if (Files.exists(cacheFile)) {
						log.debug(String.format("Cache-File %s found, checking if up-to-date", cacheFile));
						// Check if the cache is up-to-date
						final FileTime fileTime = Files.getLastModifiedTime(cache);
						final Date remoteDate = DateUtils
								.parseDate(getHeaderValue(response, HttpHeaders.LAST_MODIFIED));

						if (remoteDate != null && remoteDate.getTime() < fileTime.toMillis()) {
							// The remote file was changed before the cache, so
							// no action required
							log.debug(String.format("%tF %<tT is after %tF %<tT, no action required",
									new Date(fileTime.toMillis()), remoteDate));
							return null;
						} else {
							log.debug(String.format("remote file is newer - need to rebuild schema"));
						}
					} else {
						log.debug(String.format("No Cache-File %s, need to fetch", cacheFile));
					}

					final File cf = cacheFile.toFile();
					FileUtils.copyInputStreamToFile(response.getEntity().getContent(), cf);
					log.info(String.format("Fetched schema definition for %s from %s", displayName, request.getURI()));
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
