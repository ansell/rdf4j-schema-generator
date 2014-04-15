# Sesame Vocab Builder

Sesame Vocab Builder provides a command line tool that allows to create constants for RDF primitives for a given namespace
out of RDF ontology files.

## How To

1. Download the latest version [here](https://github.com/tkurz/sesame-vocab-builder/releases/tag/sesame-vocab-builder-1.0).
2. Run jar from command line (Java 7 required): `java -jar sesame-vocab-builder-1.1.jar <input-file> <output-file>`
3. Additional information can be configured using command-line parameters

## Command Line Options

```
  -f,--format <input-format>  mime-type of the input file (will try to guess if
                              absent)
  -h,--help                   pint this help
  -n,--name <ns>              the name of the namespace (will try to guess from
                              the input file if absent)
  -p,--package <package>      package declaration (will use default (empty)
                              package if absent
  -u,--uri <prefix>           the prefix for the vocabulary (if not available in
                              the input file)
  -l,--language <language>    preferred language for vocabulary labels
  -c,--constantCase           case to use for URI constants
  -b,--languageBundles        generate L10N LanguageBundles
```

## Run from Git

1. Clone from https://github.com/tkurz/sesame-vocab-builder.git
2. Run ./sesame-vocab-builder
3. Put in the required information (mimetype, url-prefix, classname, package and directory).

## Maven Plugin

```
<build>
    <plugins>
        <plugin>
            <groupId>com.github.tkurz</groupId>
            <artifactId>sesame-vocabbuilder-maven-plugin</artifactId>
            <version>1.1</version>
            <executions>
                <execution>
                    <id>generate-vocabularies</id>
                    <phase>generate-sources</phase>
                    <goals>
                        <goal>generate</goal>
                    </goals>
                    <configuration>
                        <output>${project.build.directory}/generated-sources/sesame-vocabs</output>
                        <packageName>com.example.sesame.vocabularies</packageName>
                        <mimeType>text/turtle</mimeType>
                        <preferredLanguage>en</preferredLanguage>
                        <constantCase>UPPER_UNDERSCORE</constantCase>
                        <vocabularies>
                            <vocabulary>
                                <className>LDP</className>
                                <file>sesame-vocab-builder-core/src/test/resources/ldp.ttl</file>
                            </vocabulary>
                            <vocabulary>
                                <className>RDF</className>
                                <url>http://www.w3.org/1999/02/22-rdf-syntax-ns</url>
                            </vocabulary>
                        </vocabularies>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```
