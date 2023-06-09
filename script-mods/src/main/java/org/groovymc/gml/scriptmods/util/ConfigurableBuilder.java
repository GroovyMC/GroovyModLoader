/*
 * Copyright (c) Matyrobbrt
 * SPDX-License-Identifier: MIT
 */

package org.groovymc.gml.scriptmods.util;

import net.minecraftforge.forgespi.language.IConfigurable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ConfigurableBuilder {
    private final Map<String, Object> values = new HashMap<>();

    public ConfigurableBuilder add(String path, Object value) {
        this.values.put(path, value);
        return this;
    }

    public ConfigurableBuilder addList(String path, ConfigurableBuilder... builders) {
        return add(path, Arrays.stream(builders).map(ConfigurableBuilder::build).toList());
    }

    public IConfigurable build() {
        return new IConfigurable() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> Optional<T> getConfigElement(String... key) {
                return Optional.ofNullable((T) values.get(String.join(".", key)));
            }

            @Override
            @SuppressWarnings({"rawtypes", "unchecked"})
            public List getConfigList(String... key) {
                return this.<List>getConfigElement(key).orElse(List.of());
            }
        };
    }
}
