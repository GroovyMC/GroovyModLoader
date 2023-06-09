/*
 * Copyright (c) Matyrobbrt
 * SPDX-License-Identifier: MIT
 */

package org.groovymc.gml.extensions

import groovy.transform.CompileStatic
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.FirstParam
import groovy.transform.stc.FromAbstractTypeMethods
import groovy.transform.stc.SecondParam
import net.minecraftforge.eventbus.api.Event
import net.minecraftforge.eventbus.api.EventPriority
import net.minecraftforge.eventbus.api.IEventBus

@CompileStatic
class EventBusExtensions {
    /**
     * Add a consumer listener with the specified {@link EventPriority} and potentially cancelled events.<br>
     * Useful for {@literal @}CompileStatic - for dynamic scenarios consider {@link #addListener(IEventBus, Class, Closure)}.
     *
     * @param closure A closure or borrowed method with the first parameter being event type you want to listen to
     * @param priority {@link EventPriority} for this listener
     * @param receiveCancelled Indicates whether this listener should receive cancelled events
     */
    static <T extends Event> void addListener(final IEventBus self,
                                              final EventPriority priority = EventPriority.NORMAL,
                                              final boolean receiveCancelled = false,
                                              @ClosureParams(value = FromAbstractTypeMethods, options = 'net.minecraftforge.eventbus.api.Event') final Closure<?> closure) {
        if (closure.parameterTypes.size() !== 1 || closure.parameterTypes[0] === Object)
            throw new IllegalArgumentException('Closure must have one explicitly typed parameter. For example: modBus.addListener { FMLCommonSetupEvent event -> ... }')

        final Class<T> eventClass = closure.parameterTypes[0] as Class<T>
        self.<T>addListener(priority, receiveCancelled, eventClass, (T event) -> closure.call(event))
    }

    /**
     * Add a consumer listener with the specified {@link EventPriority} and potentially cancelled events.<br>
     *
     * @param eventClass unused parameter needed in order to properly specify the event type
     * @param priority {@link EventPriority} for this listener. Defaults to {@link EventPriority#NORMAL}
     * @param receiveCancelled indicate if this listener should receive events that have been {@link net.minecraftforge.eventbus.api.Cancelable} cancelled. Defaults to {@code false}.
     * @param closure callback to invoke when a matching event is received.
     * @param <T>  the {@link Event} subclass to listen for
     */
    @SuppressWarnings('unused')
    static <T extends Event> void addListener(IEventBus self, Class<T> eventClass,
                                              @ClosureParams(value = SecondParam.FirstGenericType.class) Closure closure,
                                              EventPriority priority = EventPriority.NORMAL, boolean receiveCancelled = false) {
        //noinspection UnnecessaryQualifiedReference
        self.<T> addListener(priority, receiveCancelled, eventClass, (T ev) -> closure.call(ev))
    }
}
