/*
 * Copyright (c) Matyrobbrt
 * SPDX-License-Identifier: MIT
 */

package com.matyrobbrt.gml.bus

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType
import net.minecraftforge.eventbus.api.Event
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent
import net.minecraftforge.fml.loading.FMLEnvironment

@Canonical
@CompileStatic
final class GModEventBus implements IEventBus {
    @Delegate
    final IEventBus delegate

    void onCommonSetup(@ClosureParams(value = SimpleType, options = 'net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent')
                       final Closure<?> closure) {
        addListener { FMLCommonSetupEvent event -> closure.call(event) }
    }

    void onClientSetup(@ClosureParams(value = SimpleType, options = 'net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent')
                       final Closure<?> closure) {
        if (FMLEnvironment.dist.isClient())
            addListener { FMLClientSetupEvent event -> closure.call(event) }
    }

    void onDedicatedServerSetup(@ClosureParams(value = SimpleType, options = 'net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent')
                                final Closure<?> closure) {
        if (FMLEnvironment.dist.isDedicatedServer())
            addListener { FMLDedicatedServerSetupEvent event -> closure.call(event) }
    }

    void onLoadComplete(@ClosureParams(value = SimpleType, options = 'net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent')
                        final Closure<?> closure) {
        addListener { FMLLoadCompleteEvent event -> closure.call(event) }
    }

    def methodMissing(String name, def args) {
        args = args as Object[]
        if (name.startsWith('on') && name.length() > 2) {
            final String event = name.substring(2)
            final GString eventClass = "net.minecraftforge.fml.event.lifecycle.FML${event}Event"
            try {
                final Class<?> clazz = Class.forName(eventClass)
                final Closure<?> closure = args[0] as Closure<?>
                addListener(clazz as Class<? extends Event>, closure)
            } catch (ClassNotFoundException e) {
                throw new Exception("Cannot find event class \"$eventClass\"", e)
            }
        }
        throw new MissingMethodException(name, this.class, args)
    }
}
