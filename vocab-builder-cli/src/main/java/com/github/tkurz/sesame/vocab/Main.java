package com.github.tkurz.sesame.vocab;

import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.openrdf.model.util.GraphUtilException;
import org.openrdf.rio.*;

import com.google.common.base.CaseFormat;

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
import java.util.Properties;
import java.util.Set;

/**
 * ...
 * <p/>
 *
 * @author Thomas Kurz (tkurz@apache.org)
 * @author Jakob Frank (jakob@apache.org)
 */
public class Main {

    public static void main(String[] args) {
        Path tempFile = null;
        try {
            CommandLineParser parser = new PosixParser();
            CommandLine cli = parser.parse(getCliOpts(), args);

            if (cli.hasOption('h')) {
                printHelp();
                return;
            }

            // two args must be left over: <input-inputFile> <output-inputFile>
            String[] cliArgs = cli.getArgs();
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

            RDFFormat format = Rio.getParserFormatForMIMEType(cli.getOptionValue('f', null));

            final VocabBuilder builder;
            if (input.startsWith("http://")) {
                URL url = new URL(input);

                //try to guess format
                format = RDFFormat.forFileName(url.getFile());

                tempFile = Files.createTempFile("vocab-builder", "." + (format != null ? format.getDefaultFileExtension() : "cache"));

                try {
                    fetchVocab(url, tempFile);
                } catch (URISyntaxException e) {
                    throw new ParseException("Invalid input URL: " + e.getMessage());
                }

                builder = new VocabBuilder(tempFile.toString(), format);
            } else
                builder = new VocabBuilder(input, format);

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
            if (cli.hasOption('c')) {
                try {
                    CaseFormat caseFormat = CaseFormat.valueOf(cli.getOptionValue('c'));
                    if (caseFormat == null) {
                        throw new ParseException("Did not recognise constantCase: Must be one of " + Arrays.asList(CaseFormat.values()));
                    }
                    builder.setConstantCase(caseFormat);
                } catch (IllegalArgumentException e) {
                    throw new ParseException("Did not recognise constantCase: Must be one of " + Arrays.asList(CaseFormat.values()));
                }
            }
            if (cli.hasOption('s')) {
                try {
                    builder.setIndent(StringUtils.repeat(' ', Integer.parseInt(cli.getOptionValue('s', "4"))));
                } catch (NumberFormatException e) {
                    throw new ParseException("indent must be numeric");
                }
            } else {
                builder.setIndent("\t");
            }

            if (output != null) {
                System.out.printf("Starting generation%n");
                Path outFile = Paths.get(output);
                if (outFile.getParent() != null) {
                    if (!Files.exists(outFile.getParent())) {
                        Files.createDirectories(outFile.getParent());
                    } else if (!Files.isDirectory(outFile.getParent())) {
                        throw new IOException(String.format("%s is not a directory", outFile.getParent()));
                    }
                }
                builder.generate(outFile);
                if (cli.hasOption('b')) {
                    System.out.printf("Generate ResourceBundles%n");
                    builder.generateResourceBundle(outFile.getFileName().toString().replaceAll("\\.[^.]+$", ""), outFile.toAbsolutePath().getParent());
                }
                System.out.printf("Generation finished, result available in '%s'%n", output);
            } else {
                builder.generate(System.out);
            }

        } catch (UnsupportedRDFormatException e) {
            System.err.printf("%s%nTry setting the format explicitly%n", e.getMessage());
        } catch (ParseException e) {
            printHelp(e.getMessage());
        } catch (RDFParseException e) {
            System.err.println("Could not parse input file: " + e.getMessage());
        } catch (FileNotFoundException e) {
            System.err.println("Could not read input-file: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error during file-access: " + e.getMessage());
        } catch (GraphUtilException e) {
            e.printStackTrace();
        } catch (GenerationException e) {
            System.err.println(e.getMessage());
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    System.err.println("Error while deleting temp-file " + tempFile + ": "+ e.getMessage());
                }
            }
        }
    }

    private static void printHelp() {
        printHelp(null);
    }

    private static void printHelp(String error) {
        HelpFormatter hf = new HelpFormatter();
        PrintWriter w = new PrintWriter(System.out);
        if (error != null) {
            hf.printWrapped(w, 80, error);
            w.println();
        }
        hf.printWrapped(w, 80, 12, "usage: Main [options...] <input-file> [<output-file>]");
        hf.printWrapped(w, 80, 40, "  <input-file>                          the input file to read from");
        hf.printWrapped(w, 80, 40, "  [<output-file>]                       the output file to write, StdOut if omitted");
        hf.printOptions(w, 80, getCliOpts(), 2, 2);
        w.flush();
        w.close();
    }

    @SuppressWarnings({"AccessStaticViaInstance", "static-access"})
    private static Options getCliOpts() {
        Options o = new Options();

        o.addOption(OptionBuilder
                .withLongOpt("format")
                .withDescription("mime-type of the input file (will try to guess if absent)")
                .hasArgs(1)
                .withArgName("input-format")
                .isRequired(false)
                .create('f'));

        o.addOption(OptionBuilder
                .withLongOpt("package")
                .withDescription("package declaration (will use default (empty) package if absent")
                .hasArgs(1)
                .withArgName("package")
                .isRequired(false)
                .create('p'));

        o.addOption(OptionBuilder
                .withLongOpt("name")
                .withDescription("the name of the namespace (will try to guess from the input file if absent)")
                .hasArgs(1)
                .withArgName("ns")
                .isRequired(false)
                .create('n'));

        o.addOption(OptionBuilder
                .withLongOpt("uri")
                .withDescription("the prefix for the vocabulary (if not available in the input file)")
                .hasArgs(1)
                .withArgName("prefix")
                .isRequired(false)
                .create('u'));

        o.addOption(OptionBuilder
                .withLongOpt("spaces")
                .withDescription("use spaces for indentation (tabs if missing, 4 spaces if no number given)")
                .hasOptionalArgs(1)
                .withArgName("indent")
                .isRequired(false)
                .create('s'));

        o.addOption(OptionBuilder
                .withLongOpt("languageBundles")
                .withDescription("generate L10N LanguageBundles")
                .hasArg(false)
                .isRequired(false)
                .create('b'));

        o.addOption(OptionBuilder
                .withLongOpt("language")
                .withDescription("preferred language for vocabulary labels")
                .hasArgs(1)
                .withArgName("prefLang")
                .isRequired(false)
                .create('l'));

        o.addOption(OptionBuilder
                .withLongOpt("constantCase")
                .withDescription("case to use for URI constants")
                .hasArgs(1)
                .withArgName("prefConstantCase")
                .isRequired(false)
                .create('c'));

        o.addOption(OptionBuilder
                .withLongOpt("stringConstantSuffix")
                .withDescription("suffix to create string constants (e.g. _STRING")
                .hasArgs(1)
                .withArgName("suffix")
                .create('S'));

        o.addOption(OptionBuilder
                .withLongOpt("help")
                .withDescription("print this help")
                .isRequired(false)
                .hasArg(false)
                .create('h'));

        return o;
    }

    private static File fetchVocab(URL url, final Path tempFile) throws URISyntaxException, IOException {
        System.out.printf("Fetching remote vocabulary <%s>%n", url);
        final Properties buildProperties = getBuildProperties();
        final HttpClientBuilder clientBuilder = HttpClientBuilder.create()
                .setUserAgent(
                        String.format("%s:%s/%s (%s)",
                                buildProperties.getProperty("groupId", "unknown"),
                                buildProperties.getProperty("artifactId", "unknown"),
                                buildProperties.getProperty("version", "unknown"),
                                buildProperties.getProperty("name", "unknown"))
                );

        try (CloseableHttpClient client = clientBuilder.build()) {
            final HttpUriRequest request = RequestBuilder.get()
                    .setUri(url.toURI())
                    .setHeader(HttpHeaders.ACCEPT, getAcceptHeaderValue())
                    .build();

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
        Properties p = new Properties();
        try {
            p.load(Main.class.getResourceAsStream("/build.properties"));
        } catch (IOException e) {
            // ignore
        }
        return p;
    }

    private static String getAcceptHeaderValue() {
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
