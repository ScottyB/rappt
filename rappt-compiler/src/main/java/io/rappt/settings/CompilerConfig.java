package io.rappt.settings;

import java.io.File;
import java.nio.file.InvalidPathException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configuration of input and output directories.
 * These remain constant regardless of the App being built.
 */
public class CompilerConfig {
    private static final Logger LOGGER = Logger.getLogger(CompilerConfig.class.getName());

    // pathToScriptDir holds AML files, Swagger files, and Images
    public final String pathToScriptDir;
    public final String pathToStoreZips;
    public final String pathToStoreGeneratedApps;
    public final boolean imagesDisabled = false;
    public final boolean absoluteImagePathsDisabled = true;

    public static class Builder {

        private String pathToScriptDir = "";
        private String pathToStoreZips = "";
        private String pathToStoreGeneratedApps = "";

        public Builder() {}

        public Builder setPathToScriptDir(String pathToScriptDir) {
            if (pathToScriptDir == null) {
                LOGGER.log(Level.SEVERE, "pathToScriptDir was null");
                return this;
            }
            this.pathToScriptDir = pathToScriptDir;
            return this;
        }

        public Builder setPathToStoreZips(String pathToStoreZips) {
            if (pathToStoreZips == null) {
                LOGGER.log(Level.SEVERE, "pathToStoreZips was null");
                return this;
            }
            this.pathToStoreZips = pathToStoreZips;
            return this;
        }

        public Builder setPathToStoreGeneratedApps(String pathToStoreGeneratedApps) {
            if (pathToStoreGeneratedApps == null) {
                LOGGER.log(Level.SEVERE, "pathToStoreGeneratedApps was null");
                return this;
            }
            this.pathToStoreGeneratedApps = pathToStoreGeneratedApps;
            return this;
        }

        public CompilerConfig build() throws SettingsException {
            try {
                return this.buildUnvalidated();
            } catch (InvalidPathException e) {
                throw new SettingsException(e);
            }
        }

        private CompilerConfig buildUnvalidated() {
            updateToAbsolutePaths();
            return new CompilerConfig(this);
        }

        private void updateToAbsolutePaths() {
            pathToStoreZips = new File(pathToStoreZips).toPath().toAbsolutePath().toString();
            pathToScriptDir = new File(pathToScriptDir).toPath().toAbsolutePath().toString();
            pathToStoreGeneratedApps = new File(pathToStoreGeneratedApps).toPath().toAbsolutePath().toString();
        }
    }

    public static class SettingsException extends Exception {
        public SettingsException(String message) {
            super(message);
        }

        public SettingsException(String message, Throwable cause) {
            super(message, cause);
        }

        public SettingsException(Throwable cause) {
            super(cause);
        }
    }

    private CompilerConfig(Builder builder) {
        this.pathToScriptDir = builder.pathToScriptDir;
        this.pathToStoreZips = builder.pathToStoreZips;
        this.pathToStoreGeneratedApps = builder.pathToStoreGeneratedApps;
    }

    public static CompilerConfig getDefault() {
        // Default config is assumed to be safe
        return new CompilerConfig.Builder().buildUnvalidated();
    }
}
