/*
 * Copyright (c) Matyrobbrt
 * SPDX-License-Identifier: MIT
 */

package org.groovymc.gml.transform.api

import groovy.transform.CompileStatic
import org.jetbrains.annotations.ApiStatus

import javax.annotation.Nullable

@CompileStatic
class ModRegistry {
    private static final Map<String, ModData> REGISTRY = [:]

    /**
     * Gets the mod data for a package, or {@code null} if one isn't found.
     * @param packageName the package to lookup mod data for
     * @return the mod data for the package, or {@code null} if one isn't found
     */
    @Nullable
    static ModData getData(String packageName) {
        ModData found = REGISTRY[packageName]
        if (found !== null) return found
        final split = packageName.split('\\.').toList()
        for (int i = split.size() - 1; i >= 0; i--) {
            found = REGISTRY[split.subList(0, i).join('.')]
            if (found !== null) return found
        }
        return found
    }

    @ApiStatus.Internal
    static void register(String packageName, ModData modData) {
        REGISTRY[packageName] = modData
    }

    @CompileStatic
    static record ModData(String className, String modId) {}
}
