/*
 * Copyright (c) Matyrobbrt
 * SPDX-License-Identifier: MIT
 */

package org.groovymc.gml.internal

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import net.minecraftforge.fml.loading.FMLEnvironment
import net.minecraftforge.forgespi.language.IModInfo
import net.minecraftforge.forgespi.language.IModLanguageProvider
import net.minecraftforge.forgespi.language.ModFileScanData
import org.groovymc.gml.mappings.MappingMetaClassCreationHandle
import org.groovymc.gml.mappings.MappingsProvider

@Canonical
@PackageScope
@CompileStatic
final class GMLLangLoader implements IModLanguageProvider.IModLanguageLoader {
    final String className, modId
    @Override
    <T> T loadMod(IModInfo info, ModFileScanData modFileScanResults, ModuleLayer layer) {
        final threadLoader = Thread.currentThread().contextClassLoader
        if (FMLEnvironment.production) {
            // Only load runtime mappings in production
            var mappingsOrThrow = MappingsProvider.INSTANCE.mappingsProvider.get()
            if (mappingsOrThrow.right().isPresent()) {
                throw mappingsOrThrow.right().get()
            }
            MappingMetaClassCreationHandle.applyCreationHandle(mappingsOrThrow.left().orElse(null), threadLoader)
        }
        ModExtensionLoader.setup(threadLoader)
        final gContainer = Class.forName('org.groovymc.gml.GModContainer', true, threadLoader)
        final ctor = gContainer.getDeclaredConstructor(IModInfo, String, ModFileScanData, ModuleLayer)
        return ctor.newInstance(info, className, modFileScanResults, layer) as T
    }
}
