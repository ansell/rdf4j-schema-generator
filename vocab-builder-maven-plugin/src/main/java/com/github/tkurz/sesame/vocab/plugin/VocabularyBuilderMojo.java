package com.github.tkurz.sesame.vocab.plugin;

import com.github.tkurz.sesame.vocab.GenerationException;
import com.github.tkurz.sesame.vocab.VocabBuilder;
import org.apache.maven.model.Resource;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.openrdf.model.util.GraphUtilException;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Properties;

/**
 *
 */
@Mojo(name = "generate",
        defaultPhase = LifecyclePhase.GENERATE_SOURCES,
        requiresDependencyResolution = ResolutionScope.COMPILE,
        requiresProject = true)
public class VocabularyBuilderMojo extends AbstractVocabularyBuilderMojo {

    private boolean outputDirAdded = false;

    protected void executeBuilder(String displayName, Vocabulary vocab, VocabBuilder builder, String className) throws IOException, GraphUtilException, GenerationException {
        Path target = outputDirectory.toPath();
        if (builder.getPackageName() != null) {
            target = target.resolve(builder.getPackageName().replaceAll("\\.", "/"));
            Files.createDirectories(target);
        }

        final Path vFile = target.resolve(className + ".java");
        try (final PrintWriter out = new PrintWriter(
                new OutputStreamWriter(
                        buildContext.newFileOutputStream(vFile.toFile()), StandardCharsets.UTF_8)
        )) {
            if (builder.getPackageName() != null) {
                getLog().info(String.format("    Generating vocabulary class: %s.%s", builder.getPackageName(), className));
            } else {
                getLog().info(String.format("    Generating vocabulary class: %s", className));
            }
            builder.generate(className, out);
        }

        if (project != null && !outputDirAdded) {
            getLog().debug(String.format("Adding %s as additional compile source", outputDirectory.getPath()));
            project.addCompileSourceRoot(outputDirectory.getPath());
            outputDirAdded = true;
        }

        getLog().info(String.format("Generated %s", displayName));
    }

}
