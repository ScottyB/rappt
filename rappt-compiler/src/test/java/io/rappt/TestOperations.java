package io.rappt;

import com.google.gson.*;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import io.rappt.runnableinterface.rabbitmq.RabbitMQ;
import io.rappt.settings.CompilerConfig;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertTrue;

public class TestOperations {

    private static final Path exampleDir = Paths.get("examples/rabbitmq-operations");
    // expect a valid result
    private static final String[] validJsonExamples = {
            "dsl_helloscott",
            "json_helloscott",
            "gen_dsl_helloscott",
    };
    // expect no response
    private static final String[] rejectedJsonExamples = {
            "operation_malformed",
            "operation_missing_id",
    };
    // expect error message
    private static final String[] invalidJsonExamples = {
            "operation_nonexistant",
            "json_missing",
            "dsl_missing",
            "type_wrapper_missing"
    };
    private Path tempDir;

    @Before
    public void setUp() throws IOException {
        this.tempDir = TestDirSingleton.getTempDir();
    }

    @Test
    public void testJsonRobustness() throws IOException, CompilerConfig.SettingsException {
        CompilerConfig testConfig = new CompilerConfig.Builder()
                .setPathToScriptDir(tempDir.toString())
                .setPathToStoreGeneratedApps(tempDir.toString())
                .build();
        RabbitMQ.setCompilerConfig(testConfig);

        for (String example : rejectedJsonExamples) {
            // The message handling code should catch any exceptions and disregard the message
            assertTrue("JSON example: " + example + " not expected to reply ", !checkExampleErrors(example).isPresent());
        }

        for (String example : invalidJsonExamples) {
            // The message handling code should catch any exceptions and place them in the error message
            assertTrue("JSON example: " + example + " should reply ", checkExampleErrors(example).isPresent());
            assertTrue("JSON example: " + example + " should cause error message", checkExampleErrors(example).get().size() > 0);
        }

        for (String example : validJsonExamples) {
            assertTrue("JSON example: " + example + " should reply ", checkExampleErrors(example).isPresent());
            assertTrue("JSON example: " + example + " should not contain error message", checkExampleErrors(example).get().size() == 0);
        }
    }

    private Optional<List<String>> checkExampleErrors (String example) throws IOException {
        Path message = exampleDir.resolve(example + ".json");
        Optional<String> result = RabbitMQ.processMessage(FileUtils.readFileToString(message.toFile()));

        if (result.isPresent()) {
            List<String> errors = new ArrayList<>();
            JsonObject json = new JsonParser().parse(result.get()).getAsJsonObject();
            for (JsonElement e : json.get("data").getAsJsonObject().get("errors").getAsJsonArray()) {
                errors.add(e.getAsString());
            }
            return Optional.of(errors);
        } else {
            return Optional.empty();
        }
    }
}