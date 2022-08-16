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

//file:noinspection GrMethodMayBeStatic
package com.matyrobbrt.gml.transform.gmods

import com.matyrobbrt.gml.transform.api.GModTransformer
import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotatedNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.codehaus.groovy.transform.TransformWithPriority

@CompileStatic
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class GModASTTransformer extends AbstractASTTransformation implements TransformWithPriority {
    private static final List<GModTransformer> TRANSFORMERS = []
    static {
        TRANSFORMERS.addAll(ServiceLoader.load(GModTransformer, GModASTTransformer.class.classLoader))
    }

    static void registerTransformer(GModTransformer transformer) {
        TRANSFORMERS.push(transformer)
    }

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        init(nodes, source)
        final annotation = nodes[0] as AnnotationNode
        final node = nodes[1] as AnnotatedNode
        if (!(node instanceof ClassNode)) throw new IllegalArgumentException('@GMod annotation can only be applied to classes!')
        final cNode = node as ClassNode
        TRANSFORMERS.each { it.transform(cNode, annotation, source) }
    }

    @Override
    int priority() {
        return -10
    }
}
