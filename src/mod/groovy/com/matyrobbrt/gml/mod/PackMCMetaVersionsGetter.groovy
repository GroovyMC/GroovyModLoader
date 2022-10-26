package com.matyrobbrt.gml.mod

import groovy.transform.CompileStatic
import net.minecraft.SharedConstants
import net.minecraft.server.packs.PackType

@CompileStatic
final class PackMCMetaVersionsGetter {
    /**
     * Called by GModContainer to get the pack.mcmeta versions from the currently running game for GroovyScripts.
     * @see com.matyrobbrt.gml.GModContainer
     */
    static int[] get() {
        return new int[] {
                PackType.CLIENT_RESOURCES.getVersion(SharedConstants.currentVersion),
                PackType.SERVER_DATA.getVersion(SharedConstants.currentVersion)
        }
    }
}
