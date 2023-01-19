/*
 * Copyright (c) Matyrobbrt
 * SPDX-License-Identifier: MIT
 */

package com.matyrobbrt.gml.mappings

import com.google.gson.annotations.Expose
import groovy.transform.CompileStatic
import groovy.transform.stc.POJO

@POJO
@CompileStatic
class VersionMetaFile {
    @Expose
    DownloadsMeta downloads

    static class DownloadsMeta {
        @Expose
        MappingsMeta client_mappings
        @Expose
        MappingsMeta server_mappings
    }

    static class MappingsMeta {
        @Expose
        String sha1
        @Expose
        String url
    }
}
