/*
 * Copyright (c) Matyrobbrt
 * SPDX-License-Identifier: MIT
 */

package com.matyrobbrt.gml.mappings

import groovy.transform.CompileStatic
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap

@CompileStatic
class MappingMetaClassCreationHandle extends MetaClassRegistry.MetaClassCreationHandle {

    private static final String GROOVY_SYSTEM = 'groovy.lang.GroovySystem'

    final LoadedMappings mappings

    private static boolean hasWrapped = false

    MappingMetaClassCreationHandle(LoadedMappings mappings) {
        this.mappings = mappings
    }

    @Override
    protected MetaClass createNormalMetaClass(Class theClass, MetaClassRegistry registry) {
        MetaClass delegate = super.createNormalMetaClass(theClass, registry)
        MetaClass wrapped = wrapMetaClass(delegate)
        return wrapped === null ? delegate : wrapped
    }

    private MappingMetaClass wrapMetaClass(MetaClass delegated) {
        if (shouldWrap(delegated.theClass)) {
            // Check if the class is in the remapping key set
            return new MappingMetaClass(delegated, mappings)
        }
        return null
    }

    private boolean shouldWrap(Class clazz) {
        if (clazz === null)
            return false
        if (mappings.getMappable().contains(clazz.getName()))
            return true
        if (shouldWrap(clazz.getSuperclass()))
            return true
        for (Class aClass : clazz.getInterfaces()) {
            if (shouldWrap(aClass))
                return true
        }
        return false
    }

    static synchronized applyCreationHandle(LoadedMappings mappings, ClassLoader loader) {
        if (!hasWrapped) {
            Class groovySystem = Class.forName(GROOVY_SYSTEM, true, loader)
            MetaClassRegistry registry = groovySystem.getMethod('getMetaClassRegistry').invoke(null) as MetaClassRegistry

            if (mappings === null) throw new IllegalArgumentException('Found uninitialized runtime mappings!')
            hasWrapped = true
            var instance = new MappingMetaClassCreationHandle(mappings)
            registry.metaClassCreationHandle = instance
            synchronized (MetaClassRegistry) {
                Map<Class, MetaClass> queue = new Object2ObjectArrayMap<>()
                for (def it : registry.iterator()) {
                    if (it instanceof MetaClass) {
                        MetaClass wrapped = instance.wrapMetaClass(it)
                        if (wrapped !== null)
                            queue[it.theClass] = wrapped
                    }
                }
                queue.forEach { clazz, metaClazz ->
                    registry.setMetaClass(clazz, metaClazz)
                }
            }
        }
    }
}
