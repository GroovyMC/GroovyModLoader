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

package com.matyrobbrt.gml.bus

import com.matyrobbrt.gml.bus.type.BusType
import com.matyrobbrt.gml.bus.type.ForgeBus
import com.matyrobbrt.gml.util.Environment
import net.minecraftforge.api.distmarker.Dist

/**
 * Annotate a class which will be subscribed to an Event Bus at mod construction time.
 * Defaults to subscribing to the {@link ForgeBus forge bus}
 * on both sides.
 *
 * @see BusType
 */
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