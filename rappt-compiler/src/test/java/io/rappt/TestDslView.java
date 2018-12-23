package io.rappt;

import io.rappt.compiler.Compiler;
import org.junit.Before;
import org.junit.Test;
import io.rappt.model.AppModel;
import io.rappt.settings.AppConfig;
import io.rappt.settings.CompilerConfig;
import io.rappt.view.AMLStringRenderer;
import org.xml.sax.SAXException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

import static org.junit.Assert.*;

public class TestDslView {
    private static final Path exampleDir = Paths.get("examples/");
    private static final String packageName = "com.rappt";
    private static final String[] examples = {
            "HelloScott",
            "Navigation",
            "Tabbar",
            "Drawer",
            "Map",
            "UiElements",
            "List1",
            "List2"
    };
    private Path tempDir;

    @Before
    public void setUp() throws IOException {
        this.tempDir = TestDirSingleton.getTempDir();
    }

    @Test
    public void testAmlCycle() throws Compiler.StringTemplateException, Compiler.IntermediateException, SAXException, IOException, CompilerConfig.SettingsException {
        // Test that compiler can read the AML it generates
        for (String example : examples) {
            Path amlFilePath = exampleDir.resolve(example + ".aml");
            String projectName = "Aml" + example; // Avoid conflicting with other examples

            // AML1 -> AppModel1
            Compiler rappt1 = new Compiler(
                    new CompilerConfig.Builder()
                            .setPathToScriptDir(exampleDir.toString())
                            .setPathToStoreGeneratedApps(tempDir.toString())
                            .build(),
                    new AppConfig.Builder()
                            .setAmlFile(amlFilePath.toString())
                            .setProjectName(projectName)
                            .setPackageName(packageName)
                            .setProjectId("1")
                            .build());
            // build appmodel from AMLView
            rappt1.amlToModel();

            // AppModel1 -> AML2
            AppModel appModel1 = rappt1.getAppModel().get();
            String aml = appModel1.getAml();
            assertFalse("Example: " + example + " generated empty AML", aml.isEmpty());
            Path aml2FilePath = tempDir.resolve("aml2" + example + ".aml");
            amlToFile(aml, aml2FilePath.toFile());

            // AML2 -> AppModel2 -> Compile
            Compiler rappt2 = new Compiler(
                    new CompilerConfig.Builder()
                            .setPathToScriptDir(exampleDir.toString())
                            .setPathToStoreGeneratedApps(tempDir.toString())
                            .build(),
                    new AppConfig.Builder()
                            .setAmlFile(aml2FilePath.toString())
                            .setProjectName(projectName)
                            .setPackageName(packageName)
                            .setProjectId("2")
                            .build());
            rappt2.amlToModel();
            assertFalse("Example: " + example + " Cannot generate app after AML read/write cycle", rappt2.generate());
        }
    }

    public static void amlToFile(String aml, File file) throws IOException {
        BufferedWriter output = new BufferedWriter(new FileWriter(file));
        output.write(aml);
        output.close();
    }

    @Test
    public void TestAMLEscapeStringRenderer() {
        // The model retains the backslashes when reading AML
        String test = "Hello \\\"World\\\"";
        String expected = "\"Hello \\\"World\\\"\"";
        String actual = (new AMLStringRenderer()).toString(test, "amlString", Locale.getDefault());
        assertEquals(expected, actual);
        String test2 = "HELLO WORLD";
        String expected2 = "hello world";
        String actual2 = (new AMLStringRenderer()).toString(test2, "lowercase", Locale.getDefault());
        assertEquals(expected2, actual2);
    }
}
