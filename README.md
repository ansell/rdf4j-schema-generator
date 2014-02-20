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
```

## Run from Git

1. Clone from https://github.com/tkurz/sesame-vocab-builder.git
2. Run ./sesame-vocab-builder
3. Put in the required information (mimetype, url-prefix, classname, package and directory).

