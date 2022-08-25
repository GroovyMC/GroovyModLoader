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

package com.matyrobbrt.gml.mod

import com.matyrobbrt.gml.BaseGMod
import com.matyrobbrt.gml.GMod
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent

@GMod('gml')
@CompileStatic
@Slf4j(category = 'gml')
final class GMLMod implements BaseGMod {
    GMLMod() {
        log.info('Initialised GML mod. Version: {}', getClass().getPackage().implementationVersion)
        modBus.addListener(FMLLoadCompleteEvent) {
            log.debug('GML finished loading!')
        }
    }
}
