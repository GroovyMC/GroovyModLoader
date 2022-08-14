/*
 * MIT License
 *
 * Copyright (c) 2022 matyrobbrt
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
