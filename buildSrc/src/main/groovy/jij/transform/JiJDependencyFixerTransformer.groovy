package jij.transform

import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import jij.hash.HashFunction
import jij.JiJDependency

import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

@CompileStatic
@MapConstructor
class JiJDependencyFixerTransformer implements ArtifactTransformer {
    String modType
    String modulePrefix

    @Override
    byte[] transform(JiJDependency dependency) {
        final bytes = new ByteArrayOutputStream()
        try (final depOs = new JarOutputStream(bytes)) {
            final asZip = new JarFile(dependency.file())

            final manifest = new Manifest(asZip.manifest)
            manifest.mainAttributes.putValue('FMLModType', modType)
            // TODO do we actually want to replace the module name?
            manifest.mainAttributes.putValue('Automatic-Module-Name', (modulePrefix + '.' + dependency.group() + '.' + dependency.artifact()).replace('-', '.'))
            depOs.putNextEntry(new ZipEntry(JarFile.MANIFEST_NAME))
            manifest.write(depOs)
            depOs.closeEntry()

            asZip.entries().asIterator().each {
                if (it.name == 'META-INF/MANIFEST.MF' || it.name.endsWith('module-info.class')) return
                depOs.putNextEntry(makeNewEntry(it))
                depOs.write(asZip.getInputStream(it).readAllBytes())
                depOs.closeEntry()
            }
        }
        return bytes.toByteArray()
    }

    static ZipEntry makeNewEntry(ZipEntry oldEntry) {
        ZipEntry newEntry = new ZipEntry(oldEntry.name)
        if (oldEntry.getComment() !== null) newEntry.setComment(oldEntry.getComment())
        return newEntry
    }

    @Override
    boolean shouldTransform(JiJDependency dependency) throws IOException {
        final zipFile = new ZipFile(dependency.file())
        // Only transform non-mods
        boolean shouldTransform = zipFile.getEntry('META-INF/mods.toml') === null
        zipFile.close()
        return shouldTransform
    }

    @Override
    String hash() {
        return HashFunction.SHA1.hash('jijdepfix:' + modType + ';' + modulePrefix)
    }
}
