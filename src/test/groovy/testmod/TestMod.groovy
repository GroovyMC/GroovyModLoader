/*
 * Copyright (c) Matyrobbrt
 * SPDX-License-Identifier: MIT
 */

package testmod

import org.groovymc.gml.BaseGMod
import org.groovymc.gml.GMod
import org.groovymc.gml.bus.EventBusSubscriber
import org.groovymc.gml.bus.type.ModBus
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent

@Slf4j
@CompileStatic
@GMod('gmltestmod')
@EventBusSubscriber(ModBus)
class TestMod implements BaseGMod {
    TestMod() {
        this('hi')
    }

    TestMod(String shush) {
        this(shush, 15)
    }

    TestMod(String shush, int val) {
        println shush
        println val + 12

        modBus.addListener { FMLConstructModEvent event ->
            log.warn('HI FROM FML CONSTRUCT MOD EVENT!')
        }

        modBus.onClientSetup {
            log.warn("HI FROM ${it.class.simpleName}!")
        }
    }

    @SubscribeEvent
    static void yes(final FMLCommonSetupEvent e) {
        log.warn('HI FROM COMMON SETUP!')
        //System.exit(0)
    }
}
