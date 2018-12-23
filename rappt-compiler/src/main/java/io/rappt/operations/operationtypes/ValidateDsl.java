package io.rappt.operations.operationtypes;

import io.rappt.compiler.Compiler;
import io.rappt.operations.Operations;
import io.rappt.runnableinterface.rabbitmq.jsonobjects.RequestObject;
import io.rappt.runnableinterface.rabbitmq.jsonobjects.ResponseObject;
import io.rappt.settings.AppConfig;
import io.rappt.model.AppModel;
import io.rappt.runnableinterface.rabbitmq.RabbitMQ;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.logging.Logger;

public class ValidateDsl extends Operation {
    private static final Logger LOGGER = Logger.getLogger(ValidateDsl.class.getName());

    public String operationName(){
        return "/validate/dsl";
    }

    public ResponseObject performOperation(RequestObject requestData) throws IOException, Compiler.IntermediateException, Compiler.StringTemplateException, SAXException {
        ResponseObject result = requestData.replyTo();

        if (requestData.data.getViewMissing()) {
            LOGGER.warning("dsl not provided. Aborting compilation.");
            result.data.errors.add("Internal error: dsl not provided in JSON request");
            return result;
        }

        String pathToAML = Operations.writeAMLToFile(requestData);

        AppConfig.Builder appBuilder = new AppConfig.Builder()
                .setAmlFile(pathToAML)
                .setProjectName(requestData.data.projectName)
                .setProjectId(requestData.id)
                .setPackageName(requestData.data.packageName);
        AppConfig appConfig = appBuilder.build();

        Compiler compiler = new Compiler(
                RabbitMQ.getCompilerConfig(), appConfig);

        if (compiler.amlToModel())
        {
            // appModel should exist if compiler.amlToModel() was successful.
            AppModel appModel = compiler.getAppModel().get();
            result.data.model = appModel;
        } else {
            compiler.getErrors().forEach(result.data.errors::add);
        }

        return result;
    }
}
