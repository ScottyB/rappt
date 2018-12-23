package io.rappt.android;

// Class that results in the creation of a file
// Subclasses should contain fields that get passed to String Templates
public class FileTemplate extends Template {

    @STIgnore
    public String outputPath = "";

    public FileTemplate(String templateName, String outputPath) {
        super(templateName);
        this.outputPath = outputPath;
    }

    static public class SimpleFileTemplate extends FileTemplate {

        public String templateVariable;

        public SimpleFileTemplate(String templateName, String outputPath, String templateVariable) {
            super(templateName, outputPath);
            this.templateVariable = templateVariable;
        }
    }
}
