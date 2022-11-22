package com.matyrobbrt.gml.buildscript.transform

import com.matyrobbrt.gml.buildscript.data.JiJDependency
import com.matyrobbrt.gml.buildscript.util.HashFunction
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor

import java.util.jar.JarFile
import java.util.jar.JarInputStream
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import java.util.zip.ZipEntry

@CompileStatic
@MapConstructor
class JiJDependencyFixerTransformer implements ArtifactTransformer {
    String modType
    String modulePrefix

    @Override
    byte[] transform(JiJDependency dependency, byte[] input) {
        final bytes = new ByteArrayOutputStream()
        try (final depOs = new JarOutputStream(bytes)
            final inputOs = new JarInputStream(new ByteArrayInputStream(input))) {

            final manifest = new Manifest(inputOs.manifest)
            manifest.mainAttributes.putValue('FMLModType', modType)
            // TODO do we actually want to replace the module name?
            manifest.mainAttributes.putValue('Automatic-Module-Name', (modulePrefix + '.' + dependency.group() + '.' + dependency.artifact()).replace('-', '.'))
            depOs.putNextEntry(new ZipEntry(JarFile.MANIFEST_NAME))
            manifest.write(depOs)
            depOs.closeEntry()

            ZipEntry entry
            while ((entry = inputOs.nextEntry) !== null) {
                if (entry.name.endsWith('module-info.class')) continue
                if (entry.name == 'META-INF/mods.toml') { // Skip mods
                    return input
                }
                depOs.putNextEntry(makeNewEntry(entry))
                depOs.write(inputOs.readAllBytes())
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
    String hash() {
        return HashFunction.SHA1.hash('jijdepfix:' + modType + ';' + modulePrefix)
    }
}
