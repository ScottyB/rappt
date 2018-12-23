package io.rappt;

import java.io.IOException;
import java.nio.file.Path;

import static java.nio.file.Files.createTempDirectory;

public class TestDirSingleton {
    // Reuse the same temp directory for all tests in order to make the results easier to find
    private static Path tmpDir = null;

    public static synchronized Path getTempDir() throws IOException {
        if (tmpDir == null) {
            tmpDir = createTempDirectory("testApps"); // Within "/tmp" on Linux
        }
        return tmpDir;
    }
}
