package org.groovymc.gml.mod;

import net.minecraft.SharedConstants;
import net.minecraft.WorldVersion;
import net.minecraft.server.packs.PackType;
import org.groovymc.gml.GModContainer;

public final class PackMCMetaVersionsGetter {
    private static final int[] VERSIONS;

    static {
        try {
            final WorldVersion currentVersion = SharedConstants.getCurrentVersion();
            VERSIONS = new int[] {
                    currentVersion.getPackVersion(PackType.CLIENT_RESOURCES),
                    currentVersion.getPackVersion(PackType.SERVER_DATA)
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