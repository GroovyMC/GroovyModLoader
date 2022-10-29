/*
 * Copyright (c) Matyrobbrt
 * SPDX-License-Identifier: MIT
 */

package com.matyrobbrt.gml.mappings

import com.google.gson.annotations.Expose
import groovy.transform.CompileStatic

@CompileStatic
class VersionMetaFile {
    @Expose
    DownloadsMeta downloads

    @CompileStatic
    static class DownloadsMeta {
        @Expose
        MappingsMeta client_mappings
        @Expose
        MappingsMeta server_mappings
    }

    @CompileStatic
    static class MappingsMeta {
        @Expose
        String sha1
        @Expose
        String url
    }
}
