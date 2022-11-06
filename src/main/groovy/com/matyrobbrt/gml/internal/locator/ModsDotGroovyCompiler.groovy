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
        final shell = new GroovyShell(parentLoader, new Binding(), new CompilerConfiguration())
        shell.evaluate("ModsDotGroovy.setPlatform('forge')")
        return shell
    }()

    static IConfigurable compileMDG(final String modId, final String script) {
        final Map parsedMap = shell.evaluate(script)['data'] as Map

        // add groovyscript-specific defaults manually for now
        // todo: remove this once a "Runtime ModsDotGroovy" PR is submitted and used here

        // if the mods.groovy is missing modLoader and loaderVersion, assume it'll work on this loader
        parsedMap.putIfAbsent('modLoader', 'gml')
        parsedMap.putIfAbsent('loaderVersion', '[1,)')

        (((List) parsedMap.get('mods'))[0] as Map).putIfAbsent('credits', 'Powered by GroovyScript')

        // add the groovyscript = true property if absent
        parsedMap.putIfAbsent('properties', [:])
        ((Map) parsedMap.get('properties')).putIfAbsent('groovyscript', true)

        return fromMap(parsedMap)
    }

    static IConfigurable fromMap(final Map map) {
        new IConfigurable() {
            @Override
            <T> Optional<T> getConfigElement(String... key) {
                return Optional.ofNullable((T) map[key.join('.')])
            }

            @Override
            List getConfigList(String... key) {
                final element = getConfigElement(key).orElse(List.of())
                if (element instanceof Map) {
                    return List.of(fromMap(element as Map))
                } else if (element instanceof List) {
                    return getConfigElement(key)
                            .map {
                                ((List) it).stream()
                                        .map { el -> el instanceof Map ? fromMap((Map) el) : el }
                                        .toList()
                            }
                            .orElse(List.of())
                } else {
                    return List.of(element)
                }
            }
        }
    }
}
