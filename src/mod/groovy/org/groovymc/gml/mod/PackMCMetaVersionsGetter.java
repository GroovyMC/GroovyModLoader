/*
 * Copyright (c) Matyrobbrt
 * SPDX-License-Identifier: MIT
 */

package org.groovymc.gml.mod;

import groovy.transform.CompileStatic;
import net.minecraft.SharedConstants;
import net.minecraft.WorldVersion;
import net.minecraft.server.packs.PackType;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.groovymc.gml.GModContainer;

import java.lang.reflect.Method;

@CompileStatic
public final class PackMCMetaVersionsGetter {
    private static final int[] VERSIONS;

    static {
        try {
            final WorldVersion currentVersion = (WorldVersion) ObfuscationReflectionHelper.findMethod(SharedConstants.class, "m_183709" + '_').invoke(null);
            final Method getVersion = ObfuscationReflectionHelper.findMethod(WorldVersion.class, "m_264084" + '_', PackType.class);
            VERSIONS = new int[] {
                    (int) getVersion.invoke(currentVersion, PackType.CLIENT_RESOURCES),
                    (int) getVersion.invoke(currentVersion, PackType.SERVER_DATA)
            };
        } catch (Exception ex) {
            throw new RuntimeException("BARF!", ex);
        }
    }

    /**
     * Called by GModContainer to get the pack.mcmeta versions from the currently running game for GroovyScripts.
     *
     * @see GModContainer
     */
    public static int[] get() {
        return VERSIONS;
    }
}
