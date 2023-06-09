/*
 * Copyright (c) Matyrobbrt
 * SPDX-License-Identifier: MIT
 */

package org.groovymc.gml.internal

import groovy.transform.CompileStatic
import org.codehaus.groovy.transform.GroovyASTTransformationClass
import org.jetbrains.annotations.ApiStatus

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

@CompileStatic
@ApiStatus.Internal
@Deprecated(since = "Internal! Do not use!")
@Retention(RetentionPolicy.SOURCE)
@GroovyASTTransformationClass(value = 'org.groovymc.gml.transform.gmods.GModASTTransformer')
@interface __InternalGModMarker {
    String value()
}