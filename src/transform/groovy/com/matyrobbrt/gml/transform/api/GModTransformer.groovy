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

package com.matyrobbrt.gml.transform.api

import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.control.SourceUnit

/**
 * A transformer for {@linkplain com.matyrobbrt.gml.GMod GMods}. <br>
 * Global transformers can be registered with a service file or with {@linkplain com.matyrobbrt.gml.transform.gmods.GModASTTransformer#registerGlobalTransformer(com.matyrobbrt.gml.transform.api.GModTransformer)},
 * and mod-specific transformers with {@linkplain com.matyrobbrt.gml.transform.gmods.GModASTTransformer#registerTransformer(java.lang.String, com.matyrobbrt.gml.transform.api.GModTransformer)}.
 */
@CompileStatic
interface GModTransformer {
    /**
     * Transforms the GMod class.
     * @param classNode the class to transform
     * @param annotationNode the GMod annotation
     * @param source the source unit
     */
    void transform(ClassNode classNode, AnnotationNode annotationNode, SourceUnit source)
}