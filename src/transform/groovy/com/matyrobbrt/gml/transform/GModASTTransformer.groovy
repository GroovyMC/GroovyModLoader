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
package com.matyrobbrt.gml.transform

import com.matyrobbrt.gml.GMLModLoadingContext
import com.matyrobbrt.gml.bus.GModEventBus
import com.matyrobbrt.gml.transform.api.GModTransformer
import com.matyrobbrt.gml.transform.api.ModRegistry
import groovy.transform.CompileStatic
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.eventbus.api.IEventBus
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.expr.ConstructorCallExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.tools.GeneralUtils
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.codehaus.groovy.transform.TransformWithPriority
import org.objectweb.asm.Opcodes

@CompileStatic
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class GModASTTransformer extends AbstractASTTransformation implements TransformWithPriority {

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        init(nodes, source)
        final annotation = nodes[0] as AnnotationNode
        final node = nodes[1] as AnnotatedNode
        if (!(node instanceof ClassNode)) throw new IllegalArgumentException('@GMod annotation can only be applied to classes!')
        final cNode = node as ClassNode
        // noinspection UnnecessaryQualifiedReference
        ModRegistry.register(cNode.packageName, new ModRegistry.ModData(cNode.name, getMemberStringValue(annotation, 'value')))
        ServiceLoader.load(GModTransformer, getClass().classLoader).each { it.transform(cNode, annotation, source) }
    }

    @Override
    int priority() {
        return 10
    }

    @CompileStatic
    static final class BusTransformer implements GModTransformer {
        @Override
        void transform(ClassNode classNode, AnnotationNode annotationNode, SourceUnit source) {
            classNode.addProperty(
                    'modBus', Opcodes.ACC_PUBLIC, ClassHelper.make(GModEventBus),
                    null, null, null
            )
            classNode.addProperty(
                    'forgeBus', Opcodes.ACC_PUBLIC, ClassHelper.make(IEventBus),
                    GeneralUtils.propX(GeneralUtils.classX(MinecraftForge), 'EVENT_BUS'), null, null
            )
        }
    }

    @CompileStatic
    static final class CtorFix implements GModTransformer {

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
}
