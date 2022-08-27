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

package com.matyrobbrt.gml

import com.matyrobbrt.gml.bus.EventBusSubscriber
import com.matyrobbrt.gml.bus.GModEventBus
import com.matyrobbrt.gml.bus.type.ForgeBus
import com.matyrobbrt.gml.bus.type.ModBus
import com.matyrobbrt.gml.util.Environment
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.eventbus.EventBusErrorMessage
import net.minecraftforge.eventbus.api.BusBuilder
import net.minecraftforge.eventbus.api.Event
import net.minecraftforge.fml.Bindings
import net.minecraftforge.fml.ModContainer
import net.minecraftforge.fml.ModLoadingException
import net.minecraftforge.fml.ModLoadingStage
import net.minecraftforge.fml.config.IConfigEvent
import net.minecraftforge.fml.event.IModBusEvent
import net.minecraftforge.fml.loading.FMLEnvironment
import net.minecraftforge.fml.loading.moddiscovery.ModAnnotation
import net.minecraftforge.forgespi.language.IModInfo
import net.minecraftforge.forgespi.language.ModFileScanData
import org.objectweb.asm.Type

import java.lang.reflect.Constructor
import java.util.function.Consumer

@Slf4j
@CompileStatic
final class GModContainer extends ModContainer {
    private static final Type FORGE_EBS = Type.getType(ForgeBus)
    private static final Type MOD_EBS = Type.getType(ModBus)
    private static final Type EBS = Type.getType(EventBusSubscriber)

    private final Class modClass
    private Object mod
    private final GModEventBus modBus
    private final ModFileScanData scanData

    GModContainer(final IModInfo info, final String className, final ModFileScanData scanData, final ModuleLayer layer) {
        super(info)
        this.scanData = scanData

        activityMap[ModLoadingStage.CONSTRUCT] = this.&constructMod
        this.configHandler = Optional.of(this::postConfigEvent as Consumer<IConfigEvent>)
        this.contextExtension = { new GMLModLoadingContext(this) }

        modBus = new GModEventBus(BusBuilder.builder()
                    .setExceptionHandler { bus, event, listeners, i, cause -> log.error('Failed to process mod event: {}', new EventBusErrorMessage(event, i, listeners, cause)) }
                    .setTrackPhases(false)
                    .markerType(IModBusEvent)
                    .build())

        final module = layer.findModule(info.owningFile.moduleName()).orElseThrow()
        modClass = Class.forName(module, className)
        log.debug('Loaded GMod class {} on loader {} and module {}', className, modClass.classLoader, module)
    }

    private void constructMod() {
        try {
            log.debug('Loading mod class {} for {}', modClass.name, this.modId)
            def modCtor = resolveCtor()
            this.mod = modCtor.parameterTypes.length > 0 ? modCtor.newInstance(this) : modCtor.newInstance()
            log.debug('Successfully loaded mod {}', this.modId)
            injectEBS()
        } catch (final Throwable t) {
            log.error('Failed to create mod from class {} for modid {}', modClass.name, modId, t)
            throw new ModLoadingException(this.modInfo, ModLoadingStage.CONSTRUCT, 'fml.modloading.failedtoloadmod', t, this.modClass)
        }
    }

    private boolean setup = false
    @Deprecated(forRemoval = true, since = "1.1.1")
    @SuppressWarnings('unused')
    void setupMod(Object mod) {
        if (!setup) {
            log.debug('Mod {} was compiled with a previous version of GML, calling deprecated `setupMod` method, which will be removed in the future.', modId)
            injectBus(mod)
            setup = true
        }
    }

    @CompileDynamic
    @SuppressWarnings('GrUnresolvedAccess')
    private void injectBus(Object mod) {
        log.debug('Injecting bus into mod {}, with class {}', modId, modClass)
        mod.modBus = getModBus()
        log.debug('Successfully injected bus into mod {}, with class {}', modId, modClass)
    }

    private void injectEBS() {
        log.debug('Registering EventBusSubscribers for mod {}', modId)

        scanData.annotations.findAll { it.annotationType() == EBS }
            .each {
                final modId = it.annotationData()['modId'] as String
                final boolean isInMod = { ModFileScanData.AnnotationData data ->
                    if (modId !== null && !modId.isEmpty()) {
                        return modId == this.getModId()
                    }
                    return data.clazz().internalName.startsWith(modClass.packageName.replace('.' as char, '/' as char))
                }.call(it)

                if (!isInMod) return
                final bus = it.annotationData()['value'] as Type ?: FORGE_EBS
                final dists = enumValues(it, 'dist', Dist)
                final envs = enumValues(it, 'environment', Environment)

                if (FMLEnvironment.dist in dists && Environment.current() in envs) {
                    log.info('Auto-Subscribing EBS class {} to bus {}', it.clazz().className, bus)
                    final eventBus = (switch (bus) {
                        case null, FORGE_EBS -> Bindings.getForgeBus().get()
                        case MOD_EBS -> getModBus()
                        default -> throw new IllegalArgumentException("Unknown bus: $bus")
                    })
                    final cls = Class.forName(it.clazz().className, true, modClass.classLoader)
                    if (it.annotationData()['createInstance'] === true) {
                        eventBus.register(cls.getDeclaredConstructor().newInstance())
                    } else {
                        eventBus.register(cls)
                    }
                }
            }

        log.debug('Registered EventBusSubscribers for mod {}', modId)
    }

    private static <T extends Enum<T>> Set<T> enumValues(final ModFileScanData.AnnotationData data, final String name, final Class<T> clazz) {
        final List<ModAnnotation.EnumHolder> declaredHolders = data.annotationData()[name] as List<ModAnnotation.EnumHolder>
        final List<ModAnnotation.EnumHolder> holders = declaredHolders ?: makeDefaultHolders(clazz)
        holders.collect { Enum.valueOf(clazz, it.value) }.toSet()
    }

    private static <T extends Enum<T>> List<ModAnnotation.EnumHolder> makeDefaultHolders(Class<T> enumClass) {
        List.of(enumClass.getEnumConstants()).collect { new ModAnnotation.EnumHolder(null, it.name()) }
    }

    private Constructor resolveCtor() {
        try {
            return modClass.getDeclaredConstructor(GModContainer)
        } catch (NoSuchMethodException ignored) {
            return modClass.getDeclaredConstructor()
        }
    }

    GModEventBus getModBus() {
        modBus
    }

    @Override
    boolean matches(Object mod) {
        return mod.is(this.mod)
    }

    @Override
    Object getMod() {
        return mod
    }

    @Override
    protected <T extends Event & IModBusEvent> void acceptEvent(T e) {
        try {
            modBus.post(e)
        } catch (Throwable t) {
            log.error('Caught exception in mod \'{}\' during event dispatch for {}', modId, e, t)
            throw new ModLoadingException(this.modInfo, this.modLoadingStage, 'fml.modloading.errorduringevent', t)
        }
    }

    private void postConfigEvent(final IConfigEvent event) {
        modBus.post(event.self())
    }
}
