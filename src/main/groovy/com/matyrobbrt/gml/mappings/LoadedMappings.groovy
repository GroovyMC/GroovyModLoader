/*
 * Copyright (c) Matyrobbrt
 * SPDX-License-Identifier: MIT
 */

package com.matyrobbrt.gml.mappings

import groovy.transform.CompileStatic

@CompileStatic
class LoadedMappings {
    // moj class name (with dots) to map of moj -> srg names
    final Map<String, Map<String, List<String>>> methods
    final Map<String, Map<String, String>> fields
    final Set<String> mappable

    LoadedMappings(Map<String, Map<String, List<String>>> methods, Map<String, Map<String, String>> fields) {
        this.methods = methods
        this.fields = fields

        this.mappable = methods.keySet() + fields.keySet()
    }
}
