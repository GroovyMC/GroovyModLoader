/*
 * Copyright (c) Matyrobbrt
 * SPDX-License-Identifier: MIT
 */

package org.groovymc.gml.internal.scripts

import groovy.transform.CompileStatic
import org.codehaus.groovy.control.customizers.ImportCustomizer
import org.jetbrains.annotations.Nullable

@CompileStatic
@FunctionalInterface
interface ImportType<T> {
    ImportType<PackageImport> PACKAGE = { PackageImport imp, ImportCustomizer c -> c.addStarImports(imp.packageNames) }
    ImportType<ClassImport> CLASS = { ClassImport imp, ImportCustomizer c ->
        if (imp.alias !== null) c.addImport(imp.alias, imp.className)
        else c.addImports(imp.className)
    }
    ImportType<StaticImport> STATIC = { StaticImport imp, ImportCustomizer c ->
        if (imp.alias !== null) c.addStaticImport(imp.alias, imp.className, imp.memberName)
        else c.addStaticImport(imp.className, imp.memberName)
    }

    void add(T imp, ImportCustomizer customizer)
}

@CompileStatic
abstract class GImport {
    abstract ImportType getType()

    void add(ImportCustomizer customizer) {
        type.add(this, customizer)
    }
}

@CompileStatic
final class PackageImport extends GImport {
    public final String[] packageNames
    PackageImport(String[] packageNames) {
        this.packageNames = packageNames
    }

    @Override
    ImportType getType() {
        return ImportType.PACKAGE
    }
}

@CompileStatic
final class ClassImport extends GImport {
    public final String className
    public final @Nullable String alias

    ClassImport(String className, String alias) {
        this.className = className
        this.alias = alias
    }

    @Override
    ImportType getType() {
        return ImportType.CLASS
    }
}

@CompileStatic
final class StaticImport extends GImport {
    public final String className
    public final String memberName
    public final @Nullable String alias

    StaticImport(String className, String memberName, String alias) {
        this.className = className
        this.memberName = memberName
        this.alias = alias
    }

    @Override
    ImportType getType() {
        return ImportType.STATIC
    }
}