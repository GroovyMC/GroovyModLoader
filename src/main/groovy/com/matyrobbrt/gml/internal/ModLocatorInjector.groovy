package com.matyrobbrt.gml.internal

import cpw.mods.modlauncher.ModuleLayerHandler
import cpw.mods.modlauncher.api.IModuleLayerManager
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import groovy.transform.PackageScopeTarget
import net.minecraftforge.fml.loading.FMLLoader
import net.minecraftforge.fml.loading.LogMarkers
import net.minecraftforge.fml.loading.moddiscovery.InvalidModFileException
import net.minecraftforge.fml.loading.moddiscovery.ModFile
import net.minecraftforge.fml.loading.moddiscovery.ModValidator
import net.minecraftforge.fml.unsafe.UnsafeHacks
import net.minecraftforge.forgespi.locating.IModFile
import net.minecraftforge.forgespi.locating.IModLocator
import org.jetbrains.annotations.NotNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import sun.misc.Unsafe

import java.lang.reflect.Field

@CompileStatic
@PackageScope([PackageScopeTarget.CLASS])
class ModLocatorInjector {
    private static boolean wasInjected
    private static final Logger log = LoggerFactory.getLogger(ModLocatorInjector.class)
    private static final Field modValidatorField = FMLLoader.class.getDeclaredField('modValidator')
    private static final Field moduleLayerManagerField = FMLLoader.class.getDeclaredField('moduleLayerManager')
    private static final Field candidateModsField = ModValidator.class.getDeclaredField('candidateMods')
    private static final Field brokenFilesField = ModValidator.class.getDeclaredField('brokenFiles')


    @SuppressWarnings('UnnecessaryQualifiedReference')
    static void inject() {
        if (wasInjected) return; wasInjected = true

        final module = ModLocatorInjector.<IModuleLayerManager>getStaticField(moduleLayerManagerField).getLayer(IModuleLayerManager.Layer.PLUGIN).orElseThrow().findModule('com.matyrobbrt.gml.scriptmods')

        log.info('Injecting ScriptModLocator candidates...')
        final IModLocator locator = (IModLocator)Class.forName(module.orElseThrow(), 'com.matyrobbrt.gml.scriptmods.ScriptModLocator').getDeclaredConstructor().newInstance()
        final ModValidator validator = getStaticField(modValidatorField)
        final List<ModFile> candidateMods = UnsafeHacks.getField(candidateModsField, validator)
        final List<IModFile> brokenFiles = UnsafeHacks.getField(brokenFilesField, validator)

        final oldCandidateSize = candidateMods.size()
        final oldBrokenSize = brokenFiles.size()

        final scanResult = locator.scanMods()
        final foundGoodCandidates = new ArrayList<>(scanResult.stream().map {it.file()}.filter { it instanceof ModFile }.map { (ModFile) it }.toList())

        brokenFiles.addAll(scanResult.stream().map {it.ex()}.filter {it !== null }.filter { it instanceof InvalidModFileException }.map { ((InvalidModFileException) it).brokenFile.file }.toList())
        brokenFiles.addAll(validateFiles(foundGoodCandidates))
        foundGoodCandidates.each {
            // Add our candidates first as it seems like the list is reversed or something?
            candidateMods.add(0, it)
        }
        log.info('Injected ScriptModLocator mod candidates. Found {} valid mod candidates and {} broken mod files.', candidateMods.size() - oldCandidateSize, oldBrokenSize - brokenFiles.size())
    }

    @NotNull // TODO - maybe use reflection to call ModValidator#validateFiles?
    private static List<ModFile> validateFiles(final List<ModFile> mods) {
        final List<ModFile> brokenFiles = new ArrayList<>()
        for (Iterator<ModFile> iterator = mods.iterator(); iterator.hasNext();)
        {
            ModFile modFile = iterator.next();
            if (!modFile.getProvider().isValid(modFile) || !modFile.identifyMods()) {
                log.warn(LogMarkers.SCAN, "File {} has been ignored - it is invalid", modFile.getFilePath())
                iterator.remove()
                brokenFiles.add(modFile)
            }
        }
        return brokenFiles
    }

    private static <T> T getStaticField(Field field) {
        return (T) UNSAFE.getObject(UNSAFE.staticFieldBase(field), UNSAFE.staticFieldOffset(field))
    }

    private static final Unsafe UNSAFE = {
        try
        {
            final Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe")
            theUnsafe.setAccessible(true)
            return (Unsafe)theUnsafe.get(null)
        }
        catch (IllegalAccessException | NoSuchFieldException e)
        {
            throw new RuntimeException("BARF!", e)
        }
    }()
}
