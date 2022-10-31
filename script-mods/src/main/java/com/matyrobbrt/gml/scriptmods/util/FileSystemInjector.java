/*
 * Copyright (c) Matyrobbrt
 * SPDX-License-Identifier: MIT
 */

package com.matyrobbrt.gml.scriptmods.util;

import sun.misc.Unsafe;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class FileSystemInjector {
    private static final Unsafe UNSAFE;

    static {
        try {
            final Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            UNSAFE = (Unsafe)theUnsafe.get(null);
        } catch (Exception exception) {
            throw new RuntimeException("hmmmmm");
        }
    }

    private static final StaticFieldHandle<Object> LOCK = fieldGetter(FileSystemProvider.class, "lock");
    private static final StaticFieldHandle<List<FileSystemProvider>> INSTALLED_PROVIDERS = fieldGetter(FileSystemProvider.class, "installedProviders");

    private FileSystemInjector() {}

    public static void injectFileSystem(final FileSystemProvider provider) {
        injectFileSystem(provider, Integer.MAX_VALUE);
    }

    public static void injectFileSystem(final FileSystemProvider provider, final int tries) {
        if (exists(provider)) {
            return;
        }

        int counter = tries;
        while (counter-- > 0) {
            synchronized (LOCK.get()) {
                final List<FileSystemProvider> providers = INSTALLED_PROVIDERS.get();

                final List<FileSystemProvider> newProviders = new ArrayList<>(providers);
                newProviders.add(provider);

                final List<FileSystemProvider> toSet = Collections.unmodifiableList(newProviders);
                if (INSTALLED_PROVIDERS.set(toSet) == providers) {
                    // Make sure that the variable has been altered correctly, just in case some other thread is not
                    // using the lock correctly or some other JVM shenanigans are going on
                    if (exists(provider)) {
                        return;
                    }
                }
            }
        }

        throw new UnableToInjectFileSystemException(provider);
    }

    private static boolean exists(final FileSystemProvider provider) {
        return FileSystemProvider.installedProviders().stream().anyMatch(it -> Objects.equals(it, provider));
    }

    @SuppressWarnings({"unchecked", "SameParameterValue"})
    private static <T> StaticFieldHandle<T> fieldGetter(Class<?> fieldClazz, String fieldName) {
        try {
            final var field = fieldClazz.getDeclaredField(fieldName);
            final var base = UNSAFE.staticFieldBase(field);
            final var offset = UNSAFE.staticFieldOffset(field);
            return new StaticFieldHandle<>() {
                @Override
                public T get() {
                    return (T) UNSAFE.getObject(base, offset);
                }

                @Override
                public T set(T value) {
                    return (T) UNSAFE.getAndSetObject(base, offset, value);
                }
            };
        } catch (Exception exception) {
            throw new RuntimeException("Something went wrong while reflecting: ", exception);
        }
    }

    private interface StaticFieldHandle<T> {
        T get();
        @Nullable
        T set(T value);
    }

    private static final class UnableToInjectFileSystemException extends RuntimeException {
        private UnableToInjectFileSystemException(FileSystemProvider provider) {
            super("Unable to inject file system: " + provider);
        }
    }
}
