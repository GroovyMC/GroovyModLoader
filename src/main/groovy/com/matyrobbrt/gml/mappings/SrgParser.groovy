/*
 * Copyright (c) Matyrobbrt
 * SPDX-License-Identifier: MIT
 */

package com.matyrobbrt.gml.mappings

import groovy.transform.CompileStatic

@CompileStatic
class SrgParser implements Closeable {

    final Reader reader

    // obf class name to map of obf -> srg names
    final Map<String, Map<String, String>> methods = new HashMap<>()
    final Map<String, Map<String, String>> fields = new HashMap<>()
    Map<String, String> workingMethods
    Map<String, String> workingFields

    SrgParser(Reader reader) {
        this.reader = reader
        reader.readLine() // Drop header
    }

    @Override
    void close() throws IOException {
        reader.close()
    }

    private void parseLine(String line) {
        var found = line.split(' ').<String, String> collectMany { ((String) it).split('\t').toList() }.findAll { ((String) it).length() != 0 }
        if (found.size() <= 1) return // Filter out "static" lines
        String obf = found[0]
        String srg = found[-2]
        if (!line.startsWith("\t")) {
            workingFields = new HashMap<>()
            fields.put(obf, workingFields)
            workingMethods = new HashMap<>()
            methods.put(obf, workingMethods)
        } else if (!line.startsWith("\t\t")) {
            if (found.size() == 4) {
                obf = obf + found[-3].split(/\)/)[0] + ')'
                workingMethods.put(obf, srg)
            } else if (found.size() == 3) {
                workingFields.put(obf, srg)
            }
        }
    }

    void parse() {
        for (String line : reader.readLines()) {
            parseLine(line)
        }
    }
}
