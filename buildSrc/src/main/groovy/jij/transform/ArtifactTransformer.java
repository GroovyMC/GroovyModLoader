package jij.transform;

import jij.JiJDependency;

import java.io.IOException;

public interface ArtifactTransformer {
    byte[] transform(JiJDependency dependency) throws IOException;
    default boolean shouldTransform(JiJDependency dependency) throws IOException {
        return true;
    }

    String hash();
}
