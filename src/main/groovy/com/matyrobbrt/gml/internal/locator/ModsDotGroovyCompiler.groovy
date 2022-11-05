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

@CompileStatic
@Newify(pattern = '[A-z][A-Za-z0-9_]*')
class ModsDotGroovyCompiler {
    @Lazy
    private static GroovyShell shell = {
        final shell = GroovyShell(Binding(), createConfiguration())
        ModsDotGroovyCompiler.class.getResource('/META-INF/jarjar/mdg-dsl.jar')?.tap { shell.classLoader.addURL(it) }
        shell.evaluate("ModsDotGroovy.setPlatform('forge')")
        return shell
    }()

    private static CompilerConfiguration createConfiguration() {
        return CompilerConfiguration()
            .addCompilationCustomizers(ImportCustomizer()
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
