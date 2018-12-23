package io.rappt.settings;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Settings specific to individual Apps
 */
public class AppConfig {
    private static final Logger LOGGER = Logger.getLogger(AppConfig.class.getName());

    // unique identifier for app
    public final String projectId;

    // name of AML file
    public final String amlFile;
    // name of Swagger file
    public final String swaggerFile;
    // name of zip file to create
    public final String zipFile;

    public final String projectName;
    public final String packageName;

    public final boolean generateApiDoc;
    public final boolean generateApiServer;
    public final boolean generateZip;

    public static class Builder {

        private String projectId = "";
        private String amlFile = "";
        private String swaggerFile = "";
        private String zipFile = "";
        private String projectName = "";
        private String packageName = "";
        private boolean generateApiDoc = false;
        private boolean generateApiServer = false;
        private boolean generateZip = false;

        public Builder setProjectId(String projectId) {
            if (projectId == null) {
                LOGGER.log(Level.SEVERE, "projectId was null");
                return this;
            }
            this.projectId = projectId;
            return this;
        }

        public Builder setAmlFile(String amlFile) {
            if (amlFile == null) {
                LOGGER.log(Level.SEVERE, "amlFile was null");
                return this;
            }
            this.amlFile = amlFile;
            return this;
        }

        public Builder setSwaggerFile(String swaggerFile) {
            if (swaggerFile == null) {
                LOGGER.log(Level.SEVERE, "swaggerFile was null");
                return this;
            }
            this.swaggerFile = swaggerFile;
            return this;
        }

        public Builder setZipFile(String zipFile) {
            if (zipFile == null) {
                LOGGER.log(Level.SEVERE, "zipFile was null");
                return this;
            }
            this.zipFile = zipFile;
            return this;
        }

        public Builder setProjectName(String projectName) {
            if (projectName == null) {
                LOGGER.log(Level.SEVERE, "projectName was null");
                return this;
            }
            this.projectName = projectName;
            return this;
        }

        public Builder setPackageName(String packageName) {
            if (packageName == null) {
                LOGGER.log(Level.SEVERE, "packageName was null");
                return this;
            }
            this.packageName = packageName;
            return this;
        }

        public Builder setGenerateApiDoc(boolean generateApiDoc) {
            this.generateApiDoc = generateApiDoc;
            return this;
        }

        public Builder setGenerateApiServer(boolean generateApiServer) {
            this.generateApiServer = generateApiServer;
            return this;
        }

        public Builder setGenerateZip(boolean generateZip) {
            this.generateZip = generateZip;
            return this;
        }

        public AppConfig build() {
            return new AppConfig(this);
        }
    }

    public boolean hasSwagger() {
        return !swaggerFile.isEmpty();
    }

    public boolean hasProjectId() {
        return !projectId.isEmpty();
    }

    private AppConfig(Builder builder) {
        assert builder.amlFile != null && builder.projectName != null && builder.packageName != null && !builder.packageName.isEmpty() && !builder.projectName.isEmpty();

        this.projectId = builder.projectId;
        this.amlFile = builder.amlFile;
        this.swaggerFile = builder.swaggerFile;
        this.zipFile = builder.zipFile;
        this.projectName = builder.projectName;
        this.packageName = builder.packageName;
        this.generateApiDoc = builder.generateApiDoc;
        this.generateApiServer = builder.generateApiServer;
        this.generateZip = builder.generateZip;
    }
}
