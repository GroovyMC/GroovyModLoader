/*
 * Copyright (c) Matyrobbrt
 * SPDX-License-Identifier: MIT
 */

package testmod

import com.matyrobbrt.gml.BaseGMod
import com.matyrobbrt.gml.GMod
import com.matyrobbrt.gml.bus.EventBusSubscriber
import com.matyrobbrt.gml.bus.type.ModBus
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent

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
    }

    @SubscribeEvent
    static void yes(final FMLCommonSetupEvent e) {
        log.warn('HI FROM COMMON SETUP!')
        //System.exit(0)
    }
}
