/*
 * Copyright (c) Matyrobbrt
 * SPDX-License-Identifier: MIT
 */

package org.groovymc.gml.internal

import org.groovymc.gml.mappings.MappingMetaClassCreationHandle
import org.groovymc.gml.mappings.MappingsProvider
import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import net.minecraftforge.fml.loading.FMLEnvironment
import net.minecraftforge.forgespi.language.IModInfo
import net.minecraftforge.forgespi.language.IModLanguageProvider
import net.minecraftforge.forgespi.language.ModFileScanData

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
            MappingMetaClassCreationHandle.applyCreationHandle(MappingsProvider.INSTANCE.mappingsProvider.get(), threadLoader)
        }
        ModExtensionLoader.setup(threadLoader)
        final gContainer = Class.forName('org.groovymc.gml.GModContainer', true, threadLoader)
        final ctor = gContainer.getDeclaredConstructor(IModInfo, String, ModFileScanData, ModuleLayer)
        return ctor.newInstance(info, className, modFileScanResults, layer) as T
    }
}
