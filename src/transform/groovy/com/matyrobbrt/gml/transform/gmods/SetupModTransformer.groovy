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

package com.matyrobbrt.gml.transform.gmods

import com.matyrobbrt.gml.GMLModLoadingContext
import com.matyrobbrt.gml.transform.api.GModTransformer
import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.expr.ConstructorCallExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.tools.GeneralUtils
import org.codehaus.groovy.control.SourceUnit

@CompileStatic
 final class SetupModTransformer implements GModTransformer {

    @Override
    void transform(ClassNode classNode, AnnotationNode annotationNode, SourceUnit source) {
        classNode.declaredConstructors.each {
            final code = it.code as BlockStatement
            final callsAnotherCtor = code.statements.any {
                if (it instanceof ExpressionStatement) {
                    final expr = it.expression
                    if (expr instanceof ConstructorCallExpression) {
                        return expr.isThisCall() || expr.isSpecialCall()
                    }
                }
                return false
            }
            // We ONLY want to insert the setupMod in constructors that do not call others
            if (!callsAnotherCtor) {
                it.setCode(GeneralUtils.block(
                        code.variableScope,
                        new ExpressionStatement(
                                GeneralUtils.callX(
                                        GeneralUtils.callX(GeneralUtils.callX(ClassHelper.make(GMLModLoadingContext), 'get'), 'getContainer'),
                                        'setupMod',
                                        GeneralUtils.varX('this')
                                )
                        ),
                        code
                ))
            }
        }
    }
}
