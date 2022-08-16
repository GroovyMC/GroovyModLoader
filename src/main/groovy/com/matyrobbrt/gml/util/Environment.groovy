package com.matyrobbrt.gml.util

import groovy.transform.CompileStatic
import net.minecraftforge.fml.loading.FMLLoader

import java.util.function.BooleanSupplier

/**
 * Represents the environment of the current Minecraft instance.
 */
@CompileStatic
enum Environment implements BooleanSupplier {
    DEV,
    PRODUCTION

    static Environment current() {
        return FMLLoader.isProduction() ? PRODUCTION : DEV
    }

    @Override
    boolean getAsBoolean() {
        return current() == this
    }
}