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

package com.matyrobbrt.gml

import groovy.transform.CompileStatic
import org.codehaus.groovy.transform.GroovyASTTransformationClass

/**
 * This annotation defines a GML mod. <br>
 * Any class found with this annotation applied will be loaded as a Mod. The instance that is loaded will represent the mod to other Mods in the system. It will be sent various subclasses of ModLifecycleEvent at pre-defined times during the loading of the game.
 */
@CompileStatic
@GroovyASTTransformationClass(value = 'com.matyrobbrt.gml.transform.GModASTTransformer')
@interface GMod {
    String value()
}
