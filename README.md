# RDF4J Schema Generator

[![Build Status](https://travis-ci.org/ansell/rdf4j-schema-generator.svg?branch=master)](https://travis-ci.org/ansell/rdf4j-schema-generator) [![Coverage Status](https://coveralls.io/repos/ansell/rdf4j-schema-generator/badge.svg?branch=master)](https://coveralls.io/r/ansell/rdf4j-schema-generator?branch=master)

RDF4J Schema Generator provides a command line tool and maven plugin that allows to create constants for RDF primitives for a given namespace from RDF ontology files.

## How To

1. Clone from https://github.com/ansell/rdf4j-schema-generator.git
1. Run `./rdf4j-schema-generator <input-file> <output-file>`
1. Additional information can be configured using command-line parameters

## Command Line Options

```
  <input-file>                            the input file to read from
  [<output-file>]                         the output file to write, StdOut if
                                          omitted
  -b,--languageBundles                    generate L10N LanguageBundles
  -c,--constantCase <constantCase>        case to use for URI constants,
                                          possible values: LOWER_UNDERSCORE,
                                          LOWER_CAMEL, UPPER_CAMEL,
                                          UPPER_UNDERSCORE
  -C,--stringConstantCase <constantCase>  case to use for String constants, see
                                          constantCase
  -f,--format <input-format>              mime-type of the input file (will try
                                          to guess if absent)
  -h,--help                               print this help
  -l,--language <prefLang>                preferred language for vocabulary
                                          labels
  -n,--name <ns>                          the name of the namespace (will try to
                                          guess from the input file if absent)
  -P,--stringConstantPrefix <prefix>      prefix to create string constants
                                          (e.g. _)
  -p,--package <package>                  package declaration (will use default
                                          (empty) package if absent)
  -s,--spaces <indent>                    use spaces for indentation (tabs if
                                          missing, 4 spaces if no number given)
  -S,--stringConstantSuffix <suffix>      suffix to create string constants
                                          (e.g. _STRING)
  -u,--uri <prefix>                       the prefix for the vocabulary (if not
                                          available in the input file)
```

## Run from Git

## Maven Plugin

```xml
<build>
    <plugins>
        <plugin>
            <groupId>com.github.ansell.rdf4j-schema-generator</groupId>
            <artifactId>schema-generator-maven-plugin</artifactId>
            <version>0.1</version>
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
                <output>${project.build.directory}/generated-sources/rdf4j-schemas</output>
                <packageName>com.example.rdf4j.schemas</packageName>
                <mimeType>text/turtle</mimeType>
                <preferredLanguage>en</preferredLanguage>
                <createResourceBundles>true</createResourceBundles>
                <constantCase>UPPER_UNDERSCORE</constantCase>
                <createStringConstants>true</createStringConstants>
                <stringConstantCase>UPPER_UNDERSCORE</stringConstantCase>
                <stringConstantPrefix>_</stringConstantPrefix>
                <stringConstantSuffix>_STRING</stringConstantSuffix>
                <vocabularies>
                    <vocabulary>
                        <className>LDP</className>
                        <prefix>http://www.w3.org/ns/ldp#</prefix>
                        <file>core/src/test/resources/ldp.ttl</file>
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

# Changelog

## 2017-02-23
* Rebranded to rdf4j-schema-generator
* Converted to use RDF4J
* Version dropped to 0.1 for active development
