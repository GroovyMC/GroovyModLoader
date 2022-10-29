/*
 * Copyright (c) Matyrobbrt
 * SPDX-License-Identifier: MIT
 */

package com.matyrobbrt.gml.mappings

import groovy.transform.CompileStatic

@CompileStatic
class OfficialParser implements Closeable {

    final Reader reader

    // obf class name to map of official -> obf names
    final Map<String, Map<String, List<String>>> methods = new HashMap<>()
    final Map<String, Map<String, String>> fields = new HashMap<>()
    // official -> obf
    final Map<String, String> classes = new HashMap<>()
    Map<String, List<String>> workingMethods
    Map<String, String> workingFields

    OfficialParser(Reader reader) {
        this.reader = reader
        reader.readLine() // Drop header
    }

    @Override
    void close() throws IOException {
        reader.close()
    }

    private void parseLine(String line) {
        var found = line.split(' ').findAll {it.length()!=0}
        if (found.size() == 0) return // Filter out any empty lines
        if (found[-1].endsWith(':')) {
            String obf = found[-1].substring(0,found[-1].length()-1)
            String official = found[0]
            workingFields = new HashMap<>()
            fields.put(obf, workingFields)
            workingMethods = new HashMap<>()
            methods.put(obf, workingMethods)
            classes.put(official,obf)
        } else {
            String[] parts = found[1].split(/\(/)
            String official = parts[0]
            String obf = found[-1]
            if (parts.size()>1) {
                String oldSignature = parts[1].replace(')','')
                String[] sigParts = oldSignature.split(',')
                String signature = "("
                for (final def part in sigParts) {
                    signature += transformSigPart(part)
                }
                obf += signature + ')'
            }
            if (found[0].contains(':')) {
                //method
                if (!workingMethods.containsKey(official)) workingMethods.put(official, new ArrayList<String>())
                var methodList = workingMethods.get(official)
                methodList.add(obf)
            } else {
                workingFields.put(official, obf)
            }
        }
    }

    void parse() {
        for (String line : reader.readLines()) {
            parseLine(line)
        }
        for (Map<String, List<String>> map : methods.values()) {
            for (List<String> list : map.values()) {
                for (int i = 0; i<list.size(); i++) {
                    var value = list.get(i)
                    value = value.replaceAll(/L(.*?);/) { String all, String officialName ->
                        String obfName = classes.get(officialName.replace('/','.'))
                        if (obfName === null) return all
                        return 'L'+obfName+';'
                    }
                    list.set(i, value)
                }
            }
        }
    }

    private static String transformSigPart(String part) {
        if (part.isEmpty()) return ''
        if (part.endsWith('[]')) return transformSigPart(part.replace('[]',''))+'['
        return switch (part) {
            case 'int' -> 'I'
            case 'float' -> 'F'
            case 'byte' -> 'B'
            case 'boolean' -> 'Z'
            case 'long' -> 'J'
            case 'short' -> 'S'
            case 'double' -> 'D'
            case 'char' -> 'C'
            default -> 'L'+part.replace('.','/')+';'
        }
    }
}
