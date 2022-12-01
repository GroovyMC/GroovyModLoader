/*
 * Copyright (c) Matyrobbrt
 * SPDX-License-Identifier: MIT
 */

package com.matyrobbrt.gml.mod;

import com.mojang.bridge.game.GameVersion;
import groovy.transform.CompileStatic;
import net.minecraft.SharedConstants;
import net.minecraft.WorldVersion;
import net.minecraft.server.packs.PackType;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Method;

@CompileStatic
public final class PackMCMetaVersionsGetter {
    private static final int[] VERSIONS;

    static {
        try {
            final WorldVersion currentVersion = (WorldVersion)ObfuscationReflectionHelper.findMethod(SharedConstants.class, "m_183709" + '_').invoke(null);
            final Method getVersion = ObfuscationReflectionHelper.findMethod(PackType.class, "m_143756" + '_', GameVersion.class);
            VERSIONS = new int[] {
                    (int) getVersion.invoke(PackType.CLIENT_RESOURCES, currentVersion),
                    (int) getVersion.invoke(PackType.SERVER_DATA, currentVersion)
            };
        } catch (Exception ex) {
            throw new RuntimeException("BARF!", ex);
        }
    }

    /**
     * Called by GModContainer to get the pack.mcmeta versions from the currently running game for GroovyScripts.
     * @see com.matyrobbrt.gml.GModContainer
     */
    public static int[] get() {
        return VERSIONS;
    }
}
