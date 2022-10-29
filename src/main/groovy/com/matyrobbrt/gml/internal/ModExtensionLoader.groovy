/*
 * Copyright (c) Matyrobbrt
 * SPDX-License-Identifier: MIT
 */

package com.matyrobbrt.gml.internal

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.fml.ModList
import net.minecraftforge.fml.loading.FMLEnvironment
import org.codehaus.groovy.reflection.CachedClass
import org.codehaus.groovy.runtime.m12n.ExtensionModule
import org.codehaus.groovy.runtime.m12n.ExtensionModuleScanner
import org.codehaus.groovy.runtime.metaclass.MetaClassRegistryImpl
import org.objectweb.asm.*

import java.nio.file.Files
import java.util.concurrent.atomic.AtomicBoolean

@Slf4j
@CompileStatic
class ModExtensionLoader {

    private static boolean setup
    static void setup(ClassLoader loader) {
        if (setup) return
        setup = true

        if (GroovySystem.metaClassRegistry instanceof MetaClassRegistryImpl) {
            final registry = GroovySystem.metaClassRegistry as MetaClassRegistryImpl
            final Map<CachedClass, List<MetaMethod>> methods = [:]
            final scanner = new ExtensionModuleScanner({ ExtensionModule module ->
                final metaMethods = module.metaMethods
                if (!registry.moduleRegistry.hasModule(module.getName())) {
                    registry.moduleRegistry.addModule(module)
                    for (final metaMethod : metaMethods) {
                        if (metaMethod.isStatic()) {
                            registry.staticMethods.add(metaMethod)
                        } else {
                            registry.instanceMethods.add(metaMethod)
                        }
                    }
                }
                for (final metaMethod : metaMethods) {
                    final cachedClass = metaMethod.getDeclaringClass()
                    List<MetaMethod> clazzMethods = methods.computeIfAbsent(cachedClass, {[]})
                    clazzMethods.add(metaMethod)
                }
            }, loader)
            ModList.get().modFiles.each {
                final path = it.file.findResource(ExtensionModuleScanner.MODULE_META_INF_FILE)
                if (Files.exists(path)) {
                    final properties = new Properties()
                    properties.load(path.newReader())
                    if (properties.extensionClasses != null)
                        properties.extensionClasses = (properties.extensionClasses as String).split(',')
                                .findAll { isOnDist(it.trim(), loader) }.join(',')
                    if (properties.staticExtensionClasses != null)
                        properties.staticExtensionClasses = (properties.staticExtensionClasses as String).split(',')
                                .findAll { isOnDist(it.trim(), loader) }.join(',')
                    scanner.scanExtensionModuleFromProperties(properties)
                    registry.registerExtensionModuleFromProperties(properties, loader, methods)
                }
            }
            methods.each { key, value ->
                try {
                    key.addNewMopMethods(value)
                } catch (Exception ignored) {}
            }
        }
    }

    private static final String ONLYIN = Type.getDescriptor(OnlyIn.class)

    private static boolean isOnDist(String className, ClassLoader loader) {
        AtomicBoolean isOnDist = new AtomicBoolean(true)
        final is = loader.getResourceAsStream(className.replace('.', '/') + ".class")
        if (is === null) return true
        new ClassReader(is.readAllBytes()).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                super.visit(version, access, name, signature, superName, interfaces)
            }

            @Override
            AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                if (desc == ONLYIN || desc.contains('Environment')) {
                    return new AnnotationVisitor(Opcodes.ASM9) {
                        @Override
                        void visitEnum(String name, String descriptor, String value) {
                            if (name == 'value') {
                                final boolean rightDist = ModExtensionLoader.isOnDist(value)
                                isOnDist.set(rightDist)
                                if (!rightDist) {
                                    log.info("Skipping extension class {}: it requires dist {}, but we're running on {}", className, value, FMLEnvironment.dist)
                                }
                            }
                        }
                    }
                }
                return super.visitAnnotation(desc, visible)
            }
        }, ClassReader.SKIP_CODE)
        is.close()

        return isOnDist.get()
    }

    private static boolean isOnDist(String name) {
        name = name.toLowerCase(Locale.ROOT)
        if (FMLEnvironment.dist.name().toLowerCase(Locale.ROOT) == name) return true
        if (name == 'server' && FMLEnvironment.dist == Dist.DEDICATED_SERVER) return true
        return false
    }
}
