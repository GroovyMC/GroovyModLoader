/*
 * Copyright (c) Matyrobbrt
 * SPDX-License-Identifier: MIT
 */

package com.matyrobbrt.gml.internal.locator

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import net.minecraftforge.forgespi.language.IConfigurable
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ImportCustomizer

import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path

@CompileStatic
class ModsDotGroovyCompiler {
    @Lazy
    private static GroovyShell shell = {
        final jarPath = Path.of(ModsDotGroovyCompiler.class.getResource('/META-INF/jarjar/mdg-dsl.jar').toURI())
        final fs = FileSystems.newFileSystem(jarPath)

        final parentLoader = new ClassLoader(ModsDotGroovyCompiler.classLoader) {
            @Override
            protected URL findResource(String name) {
                final path = fs.getPath(name)
                return Files.exists(path) ? path.toUri().toURL() : null
            }

            @Override
            protected Enumeration<URL> findResources(String name) throws IOException {
                final res = findResource(name)
                return res === null ? Collections.<URL>emptyEnumeration() : Collections.enumeration([findResource(name)])
            }

            @Override
            protected Class<?> findClass(final String name) throws ClassNotFoundException {
                final path = fs.getPath(name.replace('.', '/') + '.class')
                if (Files.exists(path)) {
                    try (final is = Files.newInputStream(path)) {
                        final bytes = is.readAllBytes()
                        return defineClass(name, bytes, 0, bytes.length)
                    } catch (IOException e) {
                        throw new ClassNotFoundException(name, e)
                    }
                }
                throw new ClassNotFoundException(name)
            }
        }
        final shell = new GroovyShell(parentLoader, new Binding(), createConfiguration())
        shell.evaluate("ModsDotGroovy.setPlatform('forge')")
        return shell
    }()

    private static CompilerConfiguration createConfiguration() {
        return new CompilerConfiguration()
            .addCompilationCustomizers(new ImportCustomizer()
                .addStaticImport('ModsDotGroovy', 'make'))
    }

    @CompileDynamic
    static IConfigurable compileMDG(String script) {
        fromMap(shell.evaluate(script).data as Map)
    }

    static IConfigurable fromMap(Map map) {
        new IConfigurable() {
            @Override
            <T> Optional<T> getConfigElement(String... key) {
                return Optional.ofNullable((T)map[key.join('.')])
            }

            @Override
            List getConfigList(String... key) {
                return getConfigElement(key)
                    .filter { it instanceof List }
                    .map {
                        ((List) it).stream()
                            .filter { el -> el instanceof Map }
                            .map { el -> fromMap((Map) el)}
                            .toList()
                    }
                    .orElse(List.of())
            }
        }
    }
}
