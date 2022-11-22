/*
 * Copyright (c) Matyrobbrt
 * SPDX-License-Identifier: MIT
 */

package com.matyrobbrt.gml.internal.locator

import com.matyrobbrt.gml.util.Reflections
import com.matyrobbrt.gml.util.Reflections.MethodCaller
import cpw.mods.modlauncher.api.IModuleLayerManager
import groovy.transform.CompileStatic
import net.minecraftforge.fml.loading.FMLLoader
import net.minecraftforge.fml.loading.moddiscovery.InvalidModFileException
import net.minecraftforge.fml.loading.moddiscovery.ModFile
import net.minecraftforge.fml.loading.moddiscovery.ModValidator
import net.minecraftforge.fml.unsafe.UnsafeHacks
import net.minecraftforge.forgespi.language.IConfigurable
import net.minecraftforge.forgespi.locating.IModFile
import net.minecraftforge.forgespi.locating.IModLocator
import org.apache.commons.lang3.function.TriFunction
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.lang.invoke.MethodType
import java.lang.reflect.Field
import java.nio.file.FileSystem
import java.nio.file.Files

@CompileStatic
class ModLocatorInjector {
    private static boolean wasInjected
    private static final Logger log = LoggerFactory.getLogger(ModLocatorInjector.class)
    private static final Field modValidatorField = FMLLoader.class.getDeclaredField('modValidator')
    private static final Field moduleLayerManagerField = FMLLoader.class.getDeclaredField('moduleLayerManager')
    private static final Field candidateModsField = ModValidator.class.getDeclaredField('candidateMods')
    private static final Field brokenFilesField = ModValidator.class.getDeclaredField('brokenFiles')
    private static final MethodCaller<List<ModFile>> validateFiles = Reflections.methodSpecial(ModValidator, 'validateFiles', MethodType.methodType(List, List))

    private static final TriFunction<FileSystem, IModFile, String, IConfigurable> infoParser = ((FileSystem fs, IModFile modFile, String modId) -> {
        final String SCRIPTS_DIR = 'scripts'

        // first let's see if there's a dedicated mods.groovy file associated with this script group
        final modsDotGroovyPath = fs.getPath(SCRIPTS_DIR, 'mods.groovy')
        final IConfigurable configurable
        if (Files.exists(modsDotGroovyPath)) {
            // there is! let's load it in and parse it
            configurable = ModsDotGroovyCompiler.compileMDG(modId, Files.readString(modsDotGroovyPath))
        } else {
            // couldn't find one, so let's set a reasonable fallback
            configurable = ModsDotGroovyCompiler.getDefaultConfig(modId)
        }

        return configurable
    })

    // Modlocators aren't JiJable, so let's use some hacks to load script mods
    @SuppressWarnings('UnnecessaryQualifiedReference')
    static void inject() {
        if (wasInjected) return; wasInjected = true

        final module = Reflections.<IModuleLayerManager>getStaticField(moduleLayerManagerField).getLayer(IModuleLayerManager.Layer.PLUGIN).orElseThrow().findModule('com.matyrobbrt.gml.scriptmods')

        log.info('Injecting ScriptModLocator candidates...')
        final IModLocator locator = Reflections.<IModLocator>constructor(
                Class.forName(module.orElseThrow(), 'com.matyrobbrt.gml.scriptmods.ScriptModLocator'),
                MethodType.fromMethodDescriptorString('(Lorg/apache/commons/lang3/function/TriFunction;)V', module.orElseThrow().classLoader)
        ).call(infoParser)
        final ModValidator validator = Reflections.getStaticField(modValidatorField)
        final List<ModFile> candidateMods = UnsafeHacks.getField(candidateModsField, validator)
        final List<IModFile> brokenFiles = UnsafeHacks.getField(brokenFilesField, validator)

        final oldCandidateSize = candidateMods.size()
        final oldBrokenSize = brokenFiles.size()

        final scanResult = locator.scanMods()
        final foundGoodCandidates = new ArrayList<>(scanResult.stream().map {it.file()}.filter { it instanceof ModFile }.map { (ModFile) it }.toList())

        brokenFiles.addAll(scanResult.stream().map {it.ex()}.filter {it !== null }.filter { it instanceof InvalidModFileException }.map { ((InvalidModFileException) it).brokenFile.file }.toList())
        brokenFiles.addAll(validateFiles.call(validator, foundGoodCandidates))
        foundGoodCandidates.each {
            // Add our candidates first as it seems like the list is reversed or something?
            candidateMods.add(0, it)
        }
        log.info('Injected ScriptModLocator mod candidates. Found {} valid mod candidates and {} broken mod files.', candidateMods.size() - oldCandidateSize, oldBrokenSize - brokenFiles.size())
    }
}
