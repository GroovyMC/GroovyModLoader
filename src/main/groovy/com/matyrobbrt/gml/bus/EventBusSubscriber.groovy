/*
 * Copyright (c) Matyrobbrt
 * SPDX-License-Identifier: MIT
 */

package com.matyrobbrt.gml.bus

import com.matyrobbrt.gml.bus.type.BusType
import com.matyrobbrt.gml.bus.type.ForgeBus
import com.matyrobbrt.gml.util.Environment
import net.minecraftforge.api.distmarker.Dist

import java.lang.annotation.Documented
import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * Annotate a class which will be subscribed to an Event Bus at mod construction time.
 * Defaults to subscribing to the {@link ForgeBus forge bus}
 * on both sides.
 *
 * @see BusType
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@interface EventBusSubscriber {
    /**
     * Specify the bus to listen to.
     *
     * @return the bus you wish to listen to
     */
    Class<? extends BusType> value() default ForgeBus

    /**
     * Optional value, only necessary if this annotation is not on in a package of your mod. <br>
     * Needed to prevent early classloading of classes not owned by your mod.
     *
     * @return the modid to register listeners as
     */
    String modId() default ''

    /**
     * Specify targets to load this event subscriber on. Can be used to avoid loading Client specific events
     * on a dedicated server, for example.
     *
     * @return an array of Dist to load this event subscriber on
     */
    Dist[] dist() default [Dist.CLIENT, Dist.DEDICATED_SERVER]

    /**
     * Specify the environments this listener will be registered in.
     * @return an array of environments to load this event subscriber on
     */
    Environment[] environment() default [Environment.DEV, Environment.PRODUCTION]

    /**
     * If this subscriber should register a new instance of the class, instead of registering the class.
     */
    boolean createInstance() default false
}