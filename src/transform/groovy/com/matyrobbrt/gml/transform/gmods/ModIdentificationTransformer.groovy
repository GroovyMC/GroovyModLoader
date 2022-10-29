/*
 * Copyright (c) Matyrobbrt
 * SPDX-License-Identifier: MIT
 */

package com.matyrobbrt.gml.transform.gmods

import com.matyrobbrt.gml.internal.__InternalGModMarker
import com.matyrobbrt.gml.transform.api.ModRegistry
import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.codehaus.groovy.transform.TransformWithPriority

@CompileStatic
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class ModIdentificationTransformer extends AbstractASTTransformation implements TransformWithPriority {
    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        init(nodes, source)

        final annotation = nodes[0] as AnnotationNode
        if (!(nodes[1] instanceof ClassNode)) throw new IllegalArgumentException('@GMod annotation can only be applied to classes!')
        final node = nodes[1] as ClassNode
        // noinspection UnnecessaryQualifiedReference
        ModRegistry.register(node.packageName, new ModRegistry.ModData(node.name, getMemberStringValue(annotation, 'value')))

        //noinspection GrDeprecatedAPIUsage
        final ann = new AnnotationNode(ClassHelper.make(__InternalGModMarker))
        annotation.members.each(ann.&addMember)
        node.addAnnotation(ann)
    }

    @Override
    int priority() {
        return 50
    }
}
