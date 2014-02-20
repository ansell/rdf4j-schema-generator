package com.github.tkurz.sesame.vocab;

import org.apache.commons.cli.*;
import org.openrdf.model.util.GraphUtilException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.Rio;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * ...
 * <p/>
 * @author  Thomas Kurz (tkurz@apache.org)
 * @author  Jakob Frank (jakob@apache.org)
 */
public class Main {

    public static void main(String [] args) {
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
                    throw new ParseException("Missing output-dir");
                case 2:
                    input = cliArgs[0];
                    output = cliArgs[1];
                    break;
                default:
                    throw new ParseException("too many arguments");
            }

            RDFFormat format = Rio.getParserFormatForMIMEType(cli.getOptionValue('f', null));
            final VocabBuilder builder = new VocabBuilder(input, format);

            if (cli.hasOption('p')) {
                builder.setPackageName(cli.getOptionValue('p'));
            }
            if (cli.hasOption('n')) {
                builder.setName(cli.getOptionValue('n'));
            }
            if (cli.hasOption('u')) {
                builder.setPrefix(cli.getOptionValue('u'));
            }

            if (output != null) {
                System.out.printf("Starting generation%n");
                builder.setOutputFolder(output);
                builder.run();
                System.out.printf("Generation finished, result available in '%s'%n", output);
            }

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
        hf.printWrapped(w, 80, "usage: Main [options...] <input-file> [<output-file>]");
        hf.printWrapped(w, 80, "  <input-file>                the input file to read from");
        hf.printWrapped(w, 80, "  [<output-file>]             the output file to write, StdOut if omitted");
        hf.printOptions(w, 80, getCliOpts(), 2, 2);
        w.flush();
        w.close();
    }

    @SuppressWarnings("AccessStaticViaInstance")
    private static Options getCliOpts() {
        Options o = new Options();

        o.addOption(OptionBuilder
                .withLongOpt("format")
                .hasArg()
                .withArgName("input-format")
                .withDescription("mime-type of the input file (will try to guess if absent)")
                .isRequired(false)
                .create('f'));

        o.addOption(OptionBuilder
                .withLongOpt("package")
                .hasArg()
                .withArgName("package")
                .withDescription("package declaration (will use default (empty) package if absent")
                .isRequired(false)
                .create('p'));

        o.addOption(OptionBuilder
                .withLongOpt("name")
                .hasArg()
                .withArgName("ns")
                .withDescription("the name of the namespace (will try to guess from the input file if absent)")
                .create('n'));

        o.addOption(OptionBuilder
                .withLongOpt("uri")
                .hasArg()
                .withArgName("prefix")
                .withDescription("the prefix for the vocabulary (if not available in the input file)")
                .isRequired(false)
                .create('u'));

        o.addOption(OptionBuilder
                .withLongOpt("help")
                .withDescription("pint this help")
                .isRequired(false)
                .hasArg(false)
                .create('h'));

        return o;
    }

}
