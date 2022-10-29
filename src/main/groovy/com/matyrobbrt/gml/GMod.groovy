/*
 * Copyright (c) Matyrobbrt
 * SPDX-License-Identifier: MIT
 */

package com.matyrobbrt.gml

import groovy.transform.CompileStatic
import org.codehaus.groovy.transform.GroovyASTTransformationClass

import java.lang.annotation.Documented
import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * This annotation defines a GML mod. <br>
 * Any class found with this annotation applied will be loaded as a Mod. The instance that is loaded will represent the mod to other Mods in the system. <br>
 * It will be sent various subclasses of {@link net.minecraftforge.fml.event.lifecycle.ModLifecycleEvent} at pre-defined times during the loading of the game.
 */
@Documented
@CompileStatic
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@GroovyASTTransformationClass(value = 'com.matyrobbrt.gml.transform.gmods.ModIdentificationTransformer')
@interface GMod {
    /**
     * The unique mod identifier for this mod.
     * <b>Required to be lowercased in the english locale for compatibility. Will be truncated to 64 characters long.</b>
     *
     * This will be used to identify your mod for third parties (other mods), it will be used to identify your mod for registries such as block and item registries.
     * By default, you will have a resource domain that matches the modid. All these uses require that constraints are imposed on the format of the modid.
     */
    String value()
}
