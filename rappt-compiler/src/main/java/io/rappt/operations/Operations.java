package io.rappt.operations;

import com.google.gson.*;
import io.rappt.compiler.Compiler;
import io.rappt.operations.operationtypes.GenerateDsl;
import io.rappt.operations.operationtypes.Operation;
import io.rappt.runnableinterface.rabbitmq.jsonobjects.RequestObject;
import io.rappt.runnableinterface.rabbitmq.jsonobjects.ResponseObject;
import io.rappt.operations.operationtypes.GenerateZip;
import io.rappt.operations.operationtypes.ValidateDsl;
import io.rappt.runnableinterface.rabbitmq.RabbitMQ;
import io.rappt.settings.CompilerConfig;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Logger;

public class Operations {
    private static final Logger LOGGER = Logger.getLogger(Operations.class.getName());
    private static boolean INITIALISED = false;

    private static HashMap<String, Operation> operations = new HashMap<>();

    public static void addOperationToTable(Operation operation) {
        operations.put(operation.operationName(), operation);
    }

    public static Operation retrieveOperation(String operationName) {
        return operations.get(operationName);
    }

    private static void populateTasks() {
        if (!INITIALISED) {
            // Add Compiler Tasks Here
            addOperationToTable(new GenerateZip());
            addOperationToTable(new ValidateDsl());
            addOperationToTable(new GenerateDsl());

            INITIALISED = true;
        }
    }

    public static String performOperation(String operationName, RequestObject requestData) throws IOException, Compiler.IntermediateException, Compiler.StringTemplateException, SAXException {
        populateTasks();

        Operation operation = retrieveOperation(operationName);
        if (operation != null){
            return responseObjectToJson(operation.performOperation(requestData));
        }

        ResponseObject error = requestData.replyTo();
        error.data.errors.add("Internal error: Requested operation not found.");

        return responseObjectToJson(error);
    }


    public static RequestObject jsonToRequestObject(String jsonString) throws JsonParseException {
        JsonObject json = new JsonParser().parse(jsonString).getAsJsonObject();
        return RequestObject.fromJson(json);
    }

    public static String responseObjectToJson(ResponseObject responseObject) throws JsonParseException {
        return responseObject.toJson().toString();
    }

    public static String writeAMLToFile(RequestObject requestData) throws IOException {
        CompilerConfig compilerConfig = RabbitMQ.getCompilerConfig();

        File file = new File(compilerConfig.pathToScriptDir + "/"
            + requestData.data.packageName + "-"
            + requestData.data.projectName + "-"
            + requestData.id + ".aml");

        try {
            // Make the directories if they don't exist already
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }

            if (!file.exists()) {
                file.createNewFile();
            }

            FileOutputStream fop = new FileOutputStream(file);

            byte[] contentInBytes = requestData.data.getView().getBytes();

            fop.write(contentInBytes);
            fop.flush();
            fop.close();

        } catch (IOException e) {
            throw new IOException("Failed to write AML to file, file name: " + file.getName()
                    + ", Exception: " + e.toString());
        }

        return file.getPath();
    }
}
