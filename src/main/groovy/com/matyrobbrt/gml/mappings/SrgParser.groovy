/*
 * Copyright (c) Matyrobbrt
 * SPDX-License-Identifier: MIT
 */

package com.matyrobbrt.gml.mappings

import groovy.transform.CompileStatic
import org.codehaus.groovy.util.ListHashMap

@CompileStatic
class SrgParser implements Closeable {

    final Reader reader

    // obf class name to map of obf -> srg names
    // initial capacity is 9000 because on 1.19.2, max size ends up being 6859. Adding 25% to account for the Map's
    // default load factor and rounding *up* to the nearest 250. Re-adjust this every MC release for better performance.
    final Map<String, Map<String, String>> methods = new LinkedHashMap<>(8750)
    final Map<String, Map<String, String>> fields = new LinkedHashMap<>(8750)

    private Map<String, String> workingMethods
    private Map<String, String> workingFields

    SrgParser(Reader reader) {
        this.reader = reader
        reader.readLine() // Drop header
    }

    @Override
    void close() throws IOException {
        reader.close()
    }

    private void parseLine(final String line) {
        final found = line.split(' ', 4)
                .collectMany { it.split('\t', 3).toList() }
                .findAll { !it.isEmpty() }
        if (found.size() <= 1) return // Filter out "static" lines

        final String obf = found[0]
        if (!line.startsWith('\t')) {
            workingFields = new ListHashMap<String, String>(4)
            fields.put(obf, workingFields)
            workingMethods = new LinkedHashMap<String, String>()
            methods.put(obf, workingMethods)
        } else if (!line.startsWith('\t\t')) {
            final String srg = found[-2]
            if (found.size() === 4) {
                workingMethods.put(obf + found[-3].split(/\)/, 1)[0] + ')', srg)
            } else if (found.size() === 3) {
                workingFields.put(obf, srg)
            }
        }
    }

    void parse() {
        for (final String line in reader.readLines()) {
            parseLine(line)
        }
    }
}
