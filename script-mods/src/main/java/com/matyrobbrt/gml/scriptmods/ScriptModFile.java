package com.matyrobbrt.gml.scriptmods;

import cpw.mods.modlauncher.api.LamdbaExceptionUtils;
import groovy.lang.GroovyCodeSource;
import groovyjarjarasm.asm.ClassWriter;
import net.minecraftforge.fml.loading.LogMarkers;
import net.minecraftforge.fml.loading.moddiscovery.ModClassVisitor;
import net.minecraftforge.fml.loading.moddiscovery.ModFile;
import net.minecraftforge.forgespi.language.ModFileScanData;
import net.minecraftforge.forgespi.locating.IModProvider;
import net.minecraftforge.forgespi.locating.ModFileFactory;
import org.codehaus.groovy.control.BytecodeProcessor;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.Phases;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.StringGroovyMethods;
import org.objectweb.asm.ClassReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.stream.Stream;

public class ScriptModFile extends ModFile {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptModFile.class);

    private boolean wasCompiled;
    private final FileSystem fs;
    private final String rootPackage;
    private final String modId;
    public ScriptModFile(ScriptJar scriptJar, IModProvider provider, ModFileFactory.ModFileInfoParser parser, String rootPackage, String modId) {
        super(scriptJar, provider, parser, "MOD");
        fs = scriptJar.fileSystem();
        this.rootPackage = rootPackage;
        this.modId = modId;
    }

    @SuppressWarnings("unused") // It is reflection-called
    public void compile(ModFileScanData scanData) {
        if (wasCompiled) return; wasCompiled = true;
        LOGGER.info("Compiling script mod {}", modId);

        try (final Stream<Path> stream = Files.walk(fs.getPath("scripts"))
                .filter(it -> it.toString().endsWith(".groovy"))) {
            // First compile classes
            final var itr = stream.iterator();
            while (itr.hasNext()) compileClass(itr.next());

            // TODO - And now throw in the pack.mcmeta otherwise Forge will scream
        } catch (IOException exception) {
            LOGGER.error("Encountered exception compiling class: ", exception);
        }

        scanFile(path -> {
            try (InputStream in = Files.newInputStream(path)){
                ModClassVisitor mcv = new ModClassVisitor();
                ClassReader cr = new ClassReader(in);
                cr.accept(mcv, 0);
                mcv.buildData(scanData.getClasses(), scanData.getAnnotations());
            } catch (IOException | IllegalArgumentException e) {
                // We can ignore this
            }
        });
    }

    private void compileClass(Path path) throws IOException {
        final String className = StringGroovyMethods.capitalize(path.getFileName().toString().replace(".groovy", ""));
        final GroovyCodeSource source = new GroovyCodeSource(Files.readString(path), className, "/gml/groovy/script");
        CompilationUnit unit = createCompilationUnit(source.getCodeSource());
        SourceUnit su = unit.addSource(source.getName(), source.getScriptText());

        CompilationUnit.ClassgenCallback collector = createCollector(unit, className.equals("Main"));
        unit.setClassgenCallback(collector);
        unit.compile(Phases.PARSING);
        su.getAST().setPackageName(rootPackage);
        unit.compile(Phases.CLASS_GENERATION);
        Files.delete(path);
    }

    protected CompilationUnit.ClassgenCallback createCollector(CompilationUnit unit, boolean mainClass) {
        return (classVisitor, classNode) -> {
            if (mainClass) {
                final var annotation = classVisitor.visitAnnotation("Lcom/matyrobbrt/gml/GMod;", true);
                annotation.visit("value", modId);
                annotation.visitEnd();
            }
            byte[] data = ((ClassWriter) classVisitor).toByteArray();
            BytecodeProcessor bytecodePostprocessor = unit.getConfiguration().getBytecodePostprocessor();
            if (bytecodePostprocessor!=null) {
                data = bytecodePostprocessor.processBytecode(classNode.getName(), data);
            }
            Path path = fs.getPath(classNode.getPackageName().replace('.', '/') + '/' + classNode.getName() + ".class");
            final byte[] finalData = data;
            LamdbaExceptionUtils.uncheck(() -> Files.write(path, finalData));
        };
    }

    protected CompilationUnit createCompilationUnit(CodeSource source) {
        return new CompilationUnit(new CompilerConfiguration()
                .addCompilationCustomizers(new ImportCustomizer()
                        .addStarImports("com.matyrobbrt.gml", "com.matyrobbrt.gml.bus", "com.matyrobbrt.gml.bus.type", "net.minecraftforge.eventbus.api")));
    }
}
