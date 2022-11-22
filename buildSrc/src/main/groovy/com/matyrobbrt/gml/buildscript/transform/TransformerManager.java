package com.matyrobbrt.gml.buildscript.transform;

import com.matyrobbrt.gml.buildscript.data.JiJDependency;
import com.matyrobbrt.gml.buildscript.util.HashFunction;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;
import java.util.stream.Collectors;

public interface TransformerManager {
    String getName(JiJDependency dependency) throws IOException;

    static TransformerManager forFile(File file) {
        return new ForFile(file.toPath());
    }

    final class ForFile implements TransformerManager {
        private final Path file;
        public ForFile(Path file) {
            this.file = file;
        }

        private HashMap<TransformerData, String> names;

        @Override
        public String getName(JiJDependency dependency) throws IOException {
            final TransformerData data = new TransformerData(
                    dependency.transformers().stream()
                            .map(ArtifactTransformer::hash)
                            .collect(Collectors.toCollection(HashSet::new)),
                    dependency.group(), dependency.artifact(), dependency.version(),
                    HashFunction.MD5.hash(dependency.file())
            );

            if (names == null) read();

            final String oldName = names.get(data); if (oldName != null) return oldName;

            final String newName = appendExtension(UUID.randomUUID().toString(), dependency.file().getName());
            names.put(data, newName); save();

            return newName;
        }

        private String appendExtension(String stringIn, String fileName) {
            final int dotIndexOf = fileName.lastIndexOf('.');
            if (dotIndexOf == -1) return stringIn;
            return stringIn + "." + fileName.substring(dotIndexOf + 1);
        }

        private void save() throws IOException {
            if (!Files.exists(file)) {
                Files.createDirectories(file.getParent());
            }
            try (final var out = new ObjectOutputStream(Files.newOutputStream(file))) {
                out.writeObject(names);
            }
        }

        @SuppressWarnings("unchecked")
        private void read() throws IOException {
            if (!Files.exists(file)) {
                names = new HashMap<>();
            } else {
                try (final var is = new ObjectInputStream(Files.newInputStream(file))) {
                    names = (HashMap<TransformerData, String>) is.readObject();
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        private record TransformerData(HashSet<String> transformerHashes, String group, String artifact, String version, String fileHash) implements Serializable {}
    }
}
