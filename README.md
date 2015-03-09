# Sesame Vocabulary Builder

Sesame Vocabulary Builder provides a command line tool and maven plugin that allows to create constants for RDF primitives for a given namespace from RDF ontology files.

## How To

1. Download the latest version [here](https://github.com/tkurz/sesame-vocab-builder/releases).
2. Run jar from command line (Java 7 required): `java -jar vocab-builder-cli-{VERSION}-exe.jar <input-file> [<output-file>]`
3. Additional information can be configured using command-line parameters

## Command Line Options

```
  <input-file>                          the input file to read from
  [<output-file>]                       the output file to write, StdOut if
                                        omitted
  -b,--languageBundles                  generate L10N LanguageBundles
  -c,--constantCase <prefConstantCase>  case to use for URI constants
  -f,--format <input-format>            mime-type of the input file (will try to
                                        guess if absent)
  -h,--help                             print this help
  -l,--language <prefLang>              preferred language for vocabulary labels
  -n,--name <ns>                        the name of the namespace (will try to
                                        guess from the input file if absent)
  -p,--package <package>                package declaration (will use default
                                        (empty) package if absent
  -s,--spaces <indent>                  use spaces for indentation (tabs if
                                        missing, 4 spaces if no number given)
  -S,--stringConstantSuffix <suffix>    suffix to create string constants (e.g.
                                        _STRING
  -u,--uri <prefix>                     the prefix for the vocabulary (if not
                                        available in the input file)
```

## Run from Git

1. Clone from https://github.com/tkurz/sesame-vocab-builder.git
2. Run `./sesame-vocab-builder  <input-file> <output-file>`
3. Additional information can be configured using command-line parameters

## Maven Plugin

```xml
<build>
    <plugins>
        <plugin>
            <groupId>com.github.tkurz.sesame</groupId>
            <artifactId>vocab-builder-maven-plugin</artifactId>
            <version>1.3</version>
            <executions>
                <execution>
                    <id>generate-vocabularies</id>
                    <phase>generate-sources</phase>
                    <goals>
                        <goal>generate</goal>
                    </goals>
                </execution>
            </executions>
            <configuration>
                <output>${project.build.directory}/generated-sources/sesame-vocabs</output>
                <packageName>com.example.sesame.vocabularies</packageName>
                <mimeType>text/turtle</mimeType>
                <preferredLanguage>en</preferredLanguage>
                <createResourceBundles>true</createResourceBundles>
                <constantCase>UPPER_UNDERSCORE</constantCase>
                <createStringConstants>true</createStringConstants>
                <stringConstantPrefix>_</stringConstantPrefix>
                <stringConstantSuffix>_STRING</stringConstantSuffix>
                <vocabularies>
                    <vocabulary>
                        <className>LDP</className>
                        <prefix>http://www.w3.org/ns/ldp#</prefix>
                        <file>sesame-vocab-builder-core/src/test/resources/ldp.ttl</file>
                    </vocabulary>
                    <vocabulary>
                        <className>RDF</className>
                        <url>http://www.w3.org/1999/02/22-rdf-syntax-ns</url>
                    </vocabulary>
                </vocabularies>
            </configuration>
        </plugin>
    </plugins>
</build>
```
