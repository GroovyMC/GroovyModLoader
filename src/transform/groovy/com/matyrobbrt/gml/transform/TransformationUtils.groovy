package com.matyrobbrt.gml.transform

import groovy.transform.CompileStatic
import groovy.transform.Generated
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassHelper

@CompileStatic
class TransformationUtils {
    public static final AnnotationNode GENERATED_ANNOTATION = new AnnotationNode(ClassHelper.make(Generated))
}
