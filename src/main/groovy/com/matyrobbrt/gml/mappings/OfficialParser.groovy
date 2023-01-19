/*
 * Copyright (c) Matyrobbrt
 * SPDX-License-Identifier: MIT
 */

package com.matyrobbrt.gml.mappings

import groovy.transform.CompileStatic
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import org.codehaus.groovy.util.ListHashMap

import java.util.regex.Pattern

@CompileStatic
class OfficialParser implements Closeable {

    final Reader reader

    // obf class name to map of official -> obf names
    final Map<String, Map<String, List<String>>> methods = new LinkedHashMap<>(8750)
    final Map<String, Map<String, String>> fields = new LinkedHashMap<>(8750)
    // official -> obf
    final Map<String, String> classes = new LinkedHashMap<>(8750)

    private Map<String, List<String>> workingMethods
    private Map<String, String> workingFields

    OfficialParser(Reader reader) {
        this.reader = reader
        reader.readLine() // Drop header
    }

    @Override
    void close() throws IOException {
        reader.close()
    }

    private void parseLine(String line) {
        var found = line.split(' ', 8).findAll { !it.isEmpty() }
        if (found[-1].endsWith(':')) {
            String obf = found[-1].substring(0, found[-1].length() - 1)
            String official = found[0]
            // most of the time, workingFields.size() <= 4, so use a tiny backing array in those scenarios
            // and copy over to a HashMap when it grows beyond that
            workingFields = new ListHashMap<String, String>(4)
            fields.put(obf, workingFields)
            workingMethods = new LinkedHashMap<String, List<String>>()
            methods.put(obf, workingMethods)
            classes.put(official,obf)
        } else {
            final String[] parts = found[1].split(/\(/, 2)
            final String official = parts[0]
            String obf = found[-1]
            if (parts.size() > 1) {
                final String oldSignature = parts[1].substring(0, parts[1].length() - 1)
                final String[] sigParts = oldSignature.split(',')
                String signature = '('
                for (final String part in sigParts) {
                    signature += transformSigPart(part)
                }
                signature += ')'
                obf += signature
            }
            if (found[0].contains(':')) {
                //method
                if (!workingMethods.containsKey(official)) workingMethods.put(official, new ObjectArrayList<String>())
                var methodList = workingMethods.get(official)
                methodList.add(obf)
            } else {
                workingFields.put(official, obf)
            }
        }
    }

    void parse() {
        for (final String line : reader.readLines()) {
            parseLine(line)
        }
        final pattern = Pattern.compile(/L(.*?);/)
        for (final Map<String, List<String>> map in methods.values()) {
            for (final List<String> list in map.values()) {
                for (int i = 0; i < list.size(); i++) {
                    final value = list.get(i).replaceAll(pattern) { String all, String officialName ->
                        final String obfName = classes.get(officialName.replace('/','.'))
                        if (obfName === null) return all
                        else return "L${obfName};"
                    }
                    list.set(i, value)
                }
            }
        }
    }

    private static String transformSigPart(final String part) {
        if (part.isEmpty()) return part
        if (part.endsWith('[]')) return transformSigPart(part.substring(0, part.length() - 2)) + '['
        return switch (part) {
            case 'int' -> 'I'
            case 'float' -> 'F'
            case 'byte' -> 'B'
            case 'boolean' -> 'Z'
            case 'long' -> 'J'
            case 'short' -> 'S'
            case 'double' -> 'D'
            case 'char' -> 'C'
            default -> "L${part.replace('.','/')};"
        }
    }
}
