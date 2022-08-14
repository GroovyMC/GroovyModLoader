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
class LoadedMappings {
    // moj class name (with dots) to map of moj -> srg names
    final Map<String, Map<String, List<String>>> methods
    final Map<String, Map<String, String>> fields
    final Set<String> mappable

    LoadedMappings(Map<String, Map<String, List<String>>> methods, Map<String, Map<String, String>> fields) {
        this.methods = methods
        this.fields = fields

        List<String> emptyRemovalQueue = []
        methods.each (className,methodMap) -> {
            List<String> noKnownMappingsRemovalQueue = []
            methodMap.forEach(official,srg) -> {
                List<String> unnecessaryRemovalQueue = []
                srg.forEach (a)->{
                    if (official==a) unnecessaryRemovalQueue.add(a)
                }
                unnecessaryRemovalQueue.each {srg.remove it}
                if (srg.isEmpty()) noKnownMappingsRemovalQueue.add(official)
            }
            noKnownMappingsRemovalQueue.each {methodMap.remove it}

            if (methodMap.isEmpty()) {
                emptyRemovalQueue.add(className)
            }
        }
        emptyRemovalQueue.each {methods.remove it}

        emptyRemovalQueue.clear()
        fields.forEach (className,fieldMap) -> {
            List<String> unnecessaryRemovalQueue = []
            fieldMap.forEach (official,srg) -> {
                if (official==srg) unnecessaryRemovalQueue.add(official)
                return
            }
            unnecessaryRemovalQueue.each {fieldMap.remove it}

            if (fieldMap.isEmpty()) {
                emptyRemovalQueue.add(className)
            }
        }
        emptyRemovalQueue.each {fields.remove it}

        this.mappable = methods.keySet() + fields.keySet()
    }
}
