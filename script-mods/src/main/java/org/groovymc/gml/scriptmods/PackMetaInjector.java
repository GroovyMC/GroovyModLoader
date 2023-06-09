/*
 * Copyright (c) Matyrobbrt
 * SPDX-License-Identifier: MIT
 */

package org.groovymc.gml.scriptmods;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import cpw.mods.modlauncher.api.LamdbaExceptionUtils;
import net.minecraftforge.forgespi.language.IModInfo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

public class PackMetaInjector implements Consumer<IModInfo> {
    private static final Gson GSON = new Gson();
    private final int rpFormat, dpFormat;

    public PackMetaInjector(int rpFormat, int dpFormat) {
        this.rpFormat = rpFormat;
        this.dpFormat = dpFormat;
    }

    @Override
    public void accept(IModInfo info) {
        final Path packDotMcmetaPath = info.getOwningFile().getFile().findResource("pack.mcmeta");
        if (Files.notExists(packDotMcmetaPath)) {
            final JsonObject pack = new JsonObject();
            {
                final JsonObject pack1 = new JsonObject();
                pack1.addProperty("description", (info.getDisplayName() == null || info.getDisplayName().isBlank() ? info.getModId() : info.getDisplayName()) + " script resources");
                pack1.addProperty("pack_format", rpFormat);
                pack1.addProperty("forge:resource_pack_format", rpFormat);
                pack1.addProperty("forge:data_pack_format", dpFormat);
                pack.add("pack", pack1);
            }
            final String json = GSON.toJson(pack);
            LamdbaExceptionUtils.uncheck(() -> Files.writeString(packDotMcmetaPath, json));
        }
    }
}
