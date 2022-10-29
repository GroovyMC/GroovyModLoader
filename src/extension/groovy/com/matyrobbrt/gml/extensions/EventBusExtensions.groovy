/*
 * Copyright (c) Matyrobbrt
 * SPDX-License-Identifier: MIT
 */

package com.matyrobbrt.gml.extensions

import groovy.transform.CompileStatic
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.FirstParam
import groovy.transform.stc.SecondParam
import net.minecraftforge.eventbus.api.Event
import net.minecraftforge.eventbus.api.EventPriority
import net.minecraftforge.eventbus.api.IEventBus

@CompileStatic
class EventBusExtensions {
    @Deprecated(forRemoval = true, since = "1.1.1")
    static <T extends Event> void addListener(IEventBus self,
                                              @ClosureParams(value = FirstParam.FirstGenericType) Closure closure,
                                              EventPriority priority = EventPriority.NORMAL, boolean receiveCancelled = false) {
        self.<T> addListener(priority, receiveCancelled, ev -> closure.call(ev))
    }

    /**
     * Add a consumer listener with the specified {@link EventPriority} and potentially cancelled events.
     *
     * @param eventClass unused parameter needed in order to properly specify the event type
     * @param priority {@link EventPriority} for this listener. Defaults to {@link EventPriority#NORMAL}
     * @param receiveCancelled indicate if this listener should receive events that have been {@link net.minecraftforge.eventbus.api.Cancelable} cancelled. Defaults to {@code false}.
     * @param closure callback to invoke when a matching event is received.
     * @param <T>  the {@link Event} subclass to listen for
     * @deprecated this doesn't actually encode the right generic type
     */
    @SuppressWarnings('unused')
    @Deprecated(forRemoval = true, since = "1.1.3")
    static <T extends Event> void addListener(IEventBus self, Class<T> eventClass,
                                              @ClosureParams(value = SecondParam.FirstGenericType.class) Closure closure,
                                              EventPriority priority = EventPriority.NORMAL, boolean receiveCancelled = false) {
        //noinspection UnnecessaryQualifiedReference
        self.<T> addListener(priority, receiveCancelled, (T ev) -> closure.call(ev))
    }
}
