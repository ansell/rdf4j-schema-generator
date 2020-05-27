package com.github.ansell.rdf4j.schemagenerator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParserRegistry;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;

import com.google.common.base.CaseFormat;

/**
 * The Command Line Interface for the RDF4J Schema Generator.
 *
 * @author Thomas Kurz (tkurz@apache.org)
 * @author Jakob Frank (jakob@apache.org)
 * @author Peter Ansell p_ansell@yahoo.com
 */
public class RDF4JSchemaGenerator {

    public static void main(String[] args) {
        Path tempFile = null;
        try {
            final CommandLineParser parser = new PosixParser();
            final CommandLine cli = parser.parse(getCliOpts(), args);

            if (cli.hasOption('h')) {
                printHelp();
                return;
            }

            // two args must be left over: <input-inputFile> <output-inputFile>
            final String[] cliArgs = cli.getArgs();
            final String input, output;
            switch (cliArgs.length) {
            case 0:
                throw new ParseException("Missing input-file");
            case 1:
                input = cliArgs[0];
                output = null;
                break;
            case 2:
                input = cliArgs[0];
                output = cliArgs[1];
                break;
            default:
                throw new ParseException("too many arguments");
            }

            Optional<RDFFormat> format = Rio
                    .getParserFormatForMIMEType(cli.getOptionValue('f', null));

            final RDF4JSchemaGeneratorCore builder;
            if (input.startsWith("http://")) {
                final URL url = new URL(input);

                if (!format.isPresent()) {
                    // try to guess format if they didn't specify it
                    format = Rio.getParserFormatForFileName(url.getFile());
                }

                tempFile = Files.createTempFile("schema-generator", "."
                        + (format.isPresent() ? format.get().getDefaultFileExtension() : "cache"));

                try {
                    fetchSchema(url, tempFile);
                } catch (final URISyntaxException e) {
                    throw new ParseException("Invalid input URL: " + e.getMessage());
                }

                // Default to Turtle if we didn't guess the format or have it
                // specified
                builder = new RDF4JSchemaGeneratorCore(tempFile.toString(),
                        format.orElse(RDFFormat.TURTLE));
            } else {
                // Default to Turtle if we didn't have the format specified
                builder = new RDF4JSchemaGeneratorCore(input, format.orElse(RDFFormat.TURTLE));
            }
            if (cli.hasOption('p')) {
                builder.setPackageName(cli.getOptionValue('p'));
            }
            if (cli.hasOption('n')) {
                builder.setName(cli.getOptionValue('n'));
            }
            if (cli.hasOption('u')) {
                builder.setPrefix(cli.getOptionValue('u'));
            }
            if (cli.hasOption('l')) {
                builder.setPreferredLanguage(cli.getOptionValue('l'));
            }
            if (cli.hasOption('S')) {
                builder.setStringPropertySuffix(cli.getOptionValue('S'));
            } else {
                builder.setStringPropertySuffix(null);
            }
            if (cli.hasOption('P')) {
                builder.setStringPropertyPrefix(cli.getOptionValue('P'));
            } else {
                builder.setStringPropertyPrefix(null);
            }
            if (cli.hasOption('c')) {
                try {
                    final CaseFormat caseFormat = CaseFormat.valueOf(cli.getOptionValue('c'));
                    if (caseFormat == null) {
                        throw new ParseException("Did not recognise constantCase: Must be one of "
                                + Arrays.asList(CaseFormat.values()));
                    }
                    builder.setConstantCase(caseFormat);
                } catch (final IllegalArgumentException e) {
                    throw new ParseException("Did not recognise constantCase: Must be one of "
                            + Arrays.asList(CaseFormat.values()));
                }
            }
            if (cli.hasOption('C')) {
                try {
                    final CaseFormat caseFormat = CaseFormat.valueOf(cli.getOptionValue('C'));
                    if (caseFormat == null) {
                        throw new ParseException("Did not recognise constantCase: Must be one of "
                                + Arrays.asList(CaseFormat.values()));
                    }
                    builder.setStringConstantCase(caseFormat);
                } catch (final IllegalArgumentException e) {
                    throw new ParseException("Did not recognise constantCase: Must be one of "
                            + Arrays.asList(CaseFormat.values()));
                }
            }
            if (cli.hasOption('s')) {
                try {
                    builder.setIndent(StringUtils.repeat(' ',
                            Integer.parseInt(cli.getOptionValue('s', "4"))));
                } catch (final NumberFormatException e) {
                    throw new ParseException("indent must be numeric");
                }
            } else {
                builder.setIndent("\t");
            }

            if (output != null) {
                System.err.printf("Starting generation%n");
                final Path outFile = Paths.get(output);
                if (outFile.getParent() != null) {
                    if (!Files.exists(outFile.getParent())) {
                        Files.createDirectories(outFile.getParent());
                    } else if (!Files.isDirectory(outFile.getParent())) {
                        throw new IOException(
                                String.format("%s is not a directory", outFile.getParent()));
                    }
                }
                builder.generate(outFile);
                if (cli.hasOption('b')) {
                    System.err.printf("Generate ResourceBundles%n");
                    builder.generateResourceBundle(
                            outFile.getFileName().toString().replaceAll("\\.[^.]+$", ""),
                            outFile.toAbsolutePath().getParent());
                }
                System.err.printf("Generation finished, result available in '%s'%n", output);
            } else {
                builder.generate(System.out);
            }

        } catch (final UnsupportedRDFormatException e) {
            System.err.printf("%s%nTry setting the format explicitly%n", e.getMessage());
        } catch (final ParseException e) {
            printHelp(e.getMessage());
        } catch (final RDFParseException e) {
            System.err.println("Could not parse input file: " + e.getMessage());
        } catch (final FileNotFoundException e) {
            System.err.println("Could not read input-file: " + e.getMessage());
        } catch (final IOException e) {
            System.err.println("Error during file-access: " + e.getMessage());
        } catch (final GenerationException e) {
            System.err.println(e.getMessage());
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (final IOException e) {
                    System.err.println(
                            "Error while deleting temp-file " + tempFile + ": " + e.getMessage());
                }
            }
        }
    }

    private static void printHelp() {
        printHelp(null);
    }

    private static void printHelp(String error) {
        final HelpFormatter hf = new HelpFormatter();
        final PrintWriter w = new PrintWriter(System.out);
        if (error != null) {
            hf.printWrapped(w, 80, error);
            w.println();
        }
        hf.printWrapped(w, 80, 12,
                "usage: RDF4JSchemaGenerator [options...] <input-file> [<output-file>]");
        hf.printWrapped(w, 80, 42,
                "  <input-file>                            the input file to read from");
        hf.printWrapped(w, 80, 42,
                "  [<output-file>]                         the output file to write, StdOut if omitted");
        hf.printOptions(w, 80, getCliOpts(), 2, 2);
        w.flush();
        w.close();
    }

    @SuppressWarnings({ "AccessStaticViaInstance", "static-access" })
    private static Options getCliOpts() {
        final Options o = new Options();

        OptionBuilder.withLongOpt("format");
        OptionBuilder.withDescription("mime-type of the input file (will try to guess if absent)");
        OptionBuilder.hasArgs(1);
        OptionBuilder.withArgName("input-format");
        OptionBuilder.isRequired(false);
        o.addOption(OptionBuilder.create('f'));

        OptionBuilder.withLongOpt("package");
        OptionBuilder.withDescription(
                "package declaration (will use default (empty) package if absent)");
        OptionBuilder.hasArgs(1);
        OptionBuilder.withArgName("package");
        OptionBuilder.isRequired(false);
        o.addOption(OptionBuilder.create('p'));

        OptionBuilder.withLongOpt("name");
        OptionBuilder.withDescription(
                "the name of the namespace (will try to guess from the input file if absent)");
        OptionBuilder.hasArgs(1);
        OptionBuilder.withArgName("ns");
        OptionBuilder.isRequired(false);
        o.addOption(OptionBuilder.create('n'));

        OptionBuilder.withLongOpt("uri");
        OptionBuilder
                .withDescription("the prefix for the schema (if not available in the input file)");
        OptionBuilder.hasArgs(1);
        OptionBuilder.withArgName("prefix");
        OptionBuilder.isRequired(false);
        o.addOption(OptionBuilder.create('u'));

        OptionBuilder.withLongOpt("spaces");
        OptionBuilder.withDescription(
                "use spaces for indentation (tabs if missing, 4 spaces if no number given)");
        OptionBuilder.hasOptionalArgs(1);
        OptionBuilder.withArgName("indent");
        OptionBuilder.isRequired(false);
        o.addOption(OptionBuilder.create('s'));

        OptionBuilder.withLongOpt("languageBundles");
        OptionBuilder.withDescription("generate L10N LanguageBundles");
        OptionBuilder.hasArg(false);
        OptionBuilder.isRequired(false);
        o.addOption(OptionBuilder.create('b'));

        OptionBuilder.withLongOpt("language");
        OptionBuilder.withDescription("preferred language for schema labels");
        OptionBuilder.hasArgs(1);
        OptionBuilder.withArgName("prefLang");
        OptionBuilder.isRequired(false);
        o.addOption(OptionBuilder.create('l'));

        OptionBuilder.withLongOpt("constantCase");
        OptionBuilder.withDescription(
                "case to use for URI constants, possible values: LOWER_UNDERSCORE, LOWER_CAMEL, UPPER_CAMEL, UPPER_UNDERSCORE");
        OptionBuilder.hasArgs(1);
        OptionBuilder.withArgName("constantCase");
        OptionBuilder.isRequired(false);
        o.addOption(OptionBuilder.create('c'));

        OptionBuilder.withLongOpt("stringConstantCase");
        OptionBuilder.withDescription("case to use for String constants, see constantCase");
        OptionBuilder.hasArgs(1);
        OptionBuilder.withArgName("constantCase");
        OptionBuilder.isRequired(false);
        o.addOption(OptionBuilder.create('C'));

        OptionBuilder.withLongOpt("stringConstantSuffix");
        OptionBuilder.withDescription("suffix to create string constants (e.g. _STRING)");
        OptionBuilder.hasArgs(1);
        OptionBuilder.withArgName("suffix");
        o.addOption(OptionBuilder.create('S'));

        OptionBuilder.withLongOpt("stringConstantPrefix");
        OptionBuilder.withDescription("prefix to create string constants (e.g. _)");
        OptionBuilder.hasArgs(1);
        OptionBuilder.withArgName("prefix");
        o.addOption(OptionBuilder.create('P'));

        OptionBuilder.withLongOpt("help");
        OptionBuilder.withDescription("print this help");
        OptionBuilder.isRequired(false);
        OptionBuilder.hasArg(false);
        o.addOption(OptionBuilder.create('h'));

        return o;
    }

    private static File fetchSchema(URL url, final Path tempFile)
            throws URISyntaxException, IOException {
        System.err.printf("Fetching remote schema <%s>%n", url);
        final Properties buildProperties = getBuildProperties();
        final HttpClientBuilder clientBuilder = HttpClientBuilder.create()
                .setUserAgent(String.format("%s:%s/%s (%s)",
                        buildProperties.getProperty("groupId", "unknown"),
                        buildProperties.getProperty("artifactId", "unknown"),
                        buildProperties.getProperty("version", "unknown"),
                        buildProperties.getProperty("name", "unknown")));

        try (CloseableHttpClient client = clientBuilder.build()) {
            final HttpUriRequest request = RequestBuilder.get().setUri(url.toURI())
                    .setHeader(HttpHeaders.ACCEPT, getAcceptHeaderValue()).build();

            return client.execute(request, new ResponseHandler<File>() {
                @Override
                public File handleResponse(HttpResponse response) throws IOException {
                    final File cf = tempFile.toFile();
                    FileUtils.copyInputStreamToFile(response.getEntity().getContent(), cf);
                    return cf;
                }
            });
        }
    }

    private static Properties getBuildProperties() {
        final Properties p = new Properties();
        try {
            p.load(RDF4JSchemaGenerator.class.getResourceAsStream("/build.properties"));
        } catch (final IOException e) {
            // ignore
        }
        return p;
    }

    private static String getAcceptHeaderValue() {
        final Set<RDFFormat> rdfFormats = RDFParserRegistry.getInstance().getKeys();
        final Iterator<String> acceptParams = RDFFormat
                .getAcceptParams(rdfFormats, false, RDFFormat.TURTLE).iterator();
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
