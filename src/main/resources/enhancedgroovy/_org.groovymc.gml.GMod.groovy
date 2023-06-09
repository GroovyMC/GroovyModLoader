/*
 * Copyright (c) Matyrobbrt
 * SPDX-License-Identifier: MIT
 */

//file:noinspection GrPackage
import com.matyrobbrt.enhancedgroovy.dsl.ClassTransformer

((ClassTransformer) this.transformer).tap {
    addField name: 'modBus',
             type: 'org.groovymc.gml.bus.GModEventBus',
             modifiers: ['private', 'final']

    addField name: 'forgeBus',
             type: 'net.minecraftforge.eventbus.api.IEventBus',
             modifiers: ['private', 'final']

    addMethod name: 'getModBus',
              returnType: 'org.groovymc.gml.bus.GModEventBus',
              modifiers: ['private', 'final']

    addMethod name: 'getForgeBus',
              returnType: 'net.minecraftforge.eventbus.api.IEventBus',
              modifiers: ['private', 'final']
}
