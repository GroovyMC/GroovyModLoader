/*
 * MIT License
 *
 * Copyright (c) 2022 matyrobbrt
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.matyrobbrt.gml.internal

import com.matyrobbrt.gml.mappings.MappingMetaClassCreationHandle
import com.matyrobbrt.gml.mappings.MappingsProvider
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
        final gContainer = Class.forName('com.matyrobbrt.gml.GModContainer', true, threadLoader)
        final ctor = gContainer.getDeclaredConstructor(IModInfo, String, ModFileScanData, ModuleLayer)
        return ctor.newInstance(info, className, modFileScanResults, layer) as T
    }
}
