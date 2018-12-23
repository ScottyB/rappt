package io.rappt;

import com.google.gson.JsonObject;
import io.rappt.compiler.Compiler;
import io.rappt.settings.AppConfig;
import org.junit.Before;
import org.junit.Test;
import io.rappt.model.AppModel;
import io.rappt.settings.CompilerConfig;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestCompiler {

    private static final Path exampleDir = Paths.get("examples/");
    private static final Path generateDir = Paths.get("genApps/");
    private static final String packageName = "com.rappt";
    private static final String[] examples = {
            "Api",
            "Drawer",
            "HelloScott",
            "List1",
            "List2",
            "Map",
            "Navigation",
            "Tabbar",
            "UiElements"
    };
    private static final String[] commonInvalidNames = {
            "",
            ".",
            "..",
            "../HelloWorld",
            "Hello/World",
            "Hello\\World",
            "Hello-World",
            "\'HelloWorld\'",
            "\"HelloWorld\"",
            "Hello\nWorld",
            "2com",
            "com.",
            ".com.thing",
            "com.2thing"
    };
    // The project name is used as part of the filename, and must therefore be carefully validated.
    // Dash '-' is used to separate the projectName from the projectId, so should be avoided.
    // The project name must not conflict with other directories, such as the 'api' folder.
    // The generated zip may be extracted to a case-insensitive file-system.
    private static final String[] invalidProjectNames = {
            "api",
            //"Api", // TODO: Check whether 'Api' and 'api' directories conflict under Windows or Mac OS
    };
    private static final String[] validProjectNames = {
            "HelloWorld",
            "HelloWorld2",
            "myapi",
            "api2"
    };
    private static final String[] invalidPackageNames = {
            "Capitalised.thing"
    };
    private static final String[] validPackageNames = {
            "com.example",
            "com",
            "org.my.longer.example",
            "com.project.version2"
    };
    private Path tempDir;

    @Before
    public void setUp() throws IOException {
        this.tempDir = TestDirSingleton.getTempDir();
    }

    @Test
    public void testBuildingExamples() throws Compiler.StringTemplateException, Compiler.IntermediateException, SAXException, IOException, CompilerConfig.SettingsException {
        for (String example : examples) {
            Path amlFilePath = exampleDir.resolve(example + ".aml");
            String projectName = example;
            Compiler rappt = new Compiler(
                    new CompilerConfig.Builder()
                            .setPathToScriptDir(exampleDir.toString())
                            .setPathToStoreGeneratedApps(generateDir.toString())
                            .build(),
                    new AppConfig.Builder()
                            .setAmlFile(amlFilePath.toString())
                            .setProjectName(example)
                            .setPackageName(packageName)
                            .build());
            assertFalse("Example: " + example + " is broken", rappt.generate());
        }
    }

    @Test
    public void testJsonModel() throws Compiler.StringTemplateException, Compiler.IntermediateException, SAXException, IOException, CompilerConfig.SettingsException {
        // Test that compiler can read the JSON model it generates
        for (String example : examples) {
            Path amlFilePath = exampleDir.resolve(example + ".aml");
            String projectName = "Json" + example; // Avoid conflicting with other examples
            Compiler rappt = new Compiler(
                    new CompilerConfig.Builder()
                            .setPathToScriptDir(exampleDir.toString())
                            .setPathToStoreGeneratedApps(tempDir.toString())
                            .build(),
                    new AppConfig.Builder()
                            .setAmlFile(amlFilePath.toString())
                            .setProjectName(projectName)
                            .setPackageName(packageName)
                            .build());
            // build appmodel from AMLView
            rappt.amlToModel();
            JsonObject json = rappt.getAppModel().get().toJson();
            AppModel updatedModel = AppModel.fromJson(json);
            rappt.setAppModel(updatedModel);
            assertFalse("Example: " + example + " JSON read/write is broken", rappt.generate());
        }
    }

    @Test
    public void testProjectNameValidation() {
        for (String projectName : commonInvalidNames) {
            assertFalse("ProjectName: " + projectName + " should be invalid", Compiler.validateProjectName(projectName));
        }

        for (String projectName : invalidProjectNames) {
            assertFalse("ProjectName: " + projectName + " should be invalid", Compiler.validateProjectName(projectName));
        }

        for (String projectName : validProjectNames) {
            assertTrue("ProjectName: " + projectName + " should be valid", Compiler.validateProjectName(projectName));
        }
    }

    @Test public void testPackageNameValidation () {
        for (String packageName : commonInvalidNames) {
            assertFalse("PackageName: " + packageName + " should be invalid", Compiler.validatePackageName(packageName));
        }

        for (String packageName : invalidPackageNames) {
            assertFalse("PackageName: " + packageName + " should be invalid", Compiler.validatePackageName(packageName));
        }

        for (String packageName : validPackageNames) {
            assertTrue("PackageName: " + packageName + " should be valid", Compiler.validatePackageName(packageName));
        }
    }
}