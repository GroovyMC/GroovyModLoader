/*
 * Copyright (c) Matyrobbrt
 * SPDX-License-Identifier: MIT
 */

package org.groovymc.gml.util;

import sun.misc.Unsafe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;

@SuppressWarnings("unchecked")
public final class Reflections {
    private static final Unsafe UNSAFE;

    private static final MethodHandles.Lookup HANDLE;

    static {
        try {
            final Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            UNSAFE = (Unsafe)theUnsafe.get(null);

            HANDLE = getStaticField(MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP"));
        } catch (Exception exception) {
            throw new RuntimeException("hmmmmm");
        }
    }

    public static <T> T getStaticField(Field field) {
        return (T) UNSAFE.getObject(UNSAFE.staticFieldBase(field), UNSAFE.staticFieldOffset(field));
    }

    public static <T> MethodCaller<T> methodSpecial(Class<?> clazz, String name, MethodType methodType) throws Exception {
        final var handle = HANDLE.findSpecial(clazz, name, methodType, clazz);
        return caller(handle);
    }

    public static <T> MethodCaller<T> constructor(Class<?> clazz, MethodType methodType) throws Exception {
        final var handle = HANDLE.findConstructor(clazz, methodType);
        return caller(handle);
    }

    private static <T> MethodCaller<T> caller(MethodHandle handle) {
        return args -> {
            if (args.length == 0) return (T) handle.invoke();
            return (T) handle.invokeWithArguments(args);
        };
    }

    public interface MethodCaller<T> {
        T call(Object... args) throws Throwable;
    }
}
