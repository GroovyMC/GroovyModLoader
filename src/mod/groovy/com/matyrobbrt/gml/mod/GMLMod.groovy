/*
 * Copyright (c) Matyrobbrt
 * SPDX-License-Identifier: MIT
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
import java.nio.file.Path

@GMod('gml')
@CompileStatic
@Slf4j(category = 'gml')
final class GMLMod {
    GMLMod() {
        log.info('Initialised GML mod. Version: {}', getClass().getPackage().implementationVersion)
    }
}
