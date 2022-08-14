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

    private shouldWrap(Class clazz) {
        if (clazz == null)
            return false
        if (mappings.mappable.contains(clazz.name))
            return true
        if (shouldWrap(clazz.superclass))
            return true
        for (Class aClass : clazz.interfaces) {
            if (shouldWrap(aClass))
                return true
        }
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
