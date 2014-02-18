# Sesame Vocab Builder

Sesame Vocab Builder provides a command line tool that allows to create constants for RDF primitives for a given namespace
out of RDF ontology files.

## How To

1. Download the latest version [here](releases/tag/sesame-vocab-builder-1.0).
2. Run jar from command line (Java 7 required), you can specify the filepath as parameter.
3. Put in the required information (mimetype, url-prefix, classname, package and directory).

## Example:

```
$ java -jar sesame-vocab-builder-1.0.jar ldp.ttl

*** RDF Namespace Constants Constructor ***
filepath : myfile.ttl
insert file mimetype [text/turtle] :

insert url-prefix [http://www.w3.org/ns/ldp#] :

insert class name [Ldp] :
LDP
insert package name [org.apache.marmotta.commons.vocabulary] :

insert output folder [/tmp] :

*** file created: '/tmp/LDP.java' ***
```
