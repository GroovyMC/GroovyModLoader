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

package com.matyrobbrt.gml.mod

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.matyrobbrt.gml.GMod
import com.matyrobbrt.gml.bus.EventBusSubscriber
import com.matyrobbrt.gml.bus.type.ModBus
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import net.minecraft.SharedConstants
import net.minecraft.server.packs.PackType
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.ModList
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent

import java.nio.file.Files

@GMod('gml')
@CompileStatic
@Slf4j(category = 'gml')
final class GMLMod {
    GMLMod() {
        log.info('Initialised GML mod. Version: {}', getClass().getPackage().implementationVersion)
    }

    @CompileStatic
    @EventBusSubscriber(modId = 'gml', value = ModBus)
    static class PackMcMetaSetup {
        private static final Gson GSON = new Gson()
        @SubscribeEvent
        static void onCommonSetup(final FMLConstructModEvent event) {
            ModList.get().modFiles.each { // TODO - still seems to be too late?
                if (it.file.fileName.endsWith('.groovy')) {
                    // This is a groovy script, inject the pack.mcmeta
                    final mcmetaPath = it.file.findResource('pack.mcmeta')
                    if (Files.notExists(mcmetaPath)) {
                        final pack = new JsonObject()
                        pack.addProperty("description", it.mods[0].displayName + ' Resources')
                        pack.addProperty("pack_format", PackType.CLIENT_RESOURCES.getVersion(SharedConstants.getCurrentVersion()))
                        pack.addProperty("forge:resource_pack_format", PackType.CLIENT_RESOURCES.getVersion(SharedConstants.getCurrentVersion()))
                        pack.addProperty("forge:data_pack_format", PackType.SERVER_DATA.getVersion(SharedConstants.getCurrentVersion()))
                        Files.writeString(mcmetaPath, GSON.toJson(pack))
                    }
                }
            }
        }
    }
}
