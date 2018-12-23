package io.rappt.operations.operationtypes;

import io.rappt.compiler.Compiler;
import io.rappt.model.AppModel;
import io.rappt.runnableinterface.rabbitmq.jsonobjects.RequestObject;
import io.rappt.runnableinterface.rabbitmq.jsonobjects.ResponseObject;
import io.rappt.settings.AppConfig;
import org.apache.commons.io.FileUtils;
import io.rappt.runnableinterface.rabbitmq.RabbitMQ;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.logging.Logger;

public class GenerateZip extends Operation {
    private static final Logger LOGGER = Logger.getLogger(GenerateZip.class.getName());
    
    public String operationName(){
        return "/generate/zip";
    }

    public ResponseObject performOperation(RequestObject requestData) throws IOException, Compiler.IntermediateException, Compiler.StringTemplateException, SAXException {
        ResponseObject result = requestData.replyTo();

        AppModel updatedModel = requestData.data.model;
        if (updatedModel == null) {
            LOGGER.warning("appModel is null. Aborting compilation.");
            result.data.errors.add("Internal error: appModel not provided in JSON request");
            // We can't compile without a valid AppModel
            return result;
        }

        AppConfig.Builder appBuilder = new AppConfig.Builder()
                .setProjectName(requestData.data.projectName)
                .setProjectId(requestData.id)
                .setPackageName(requestData.data.packageName);
        AppConfig appConfig = appBuilder.build();

        Compiler compiler = new Compiler(
                RabbitMQ.getCompilerConfig(), appConfig);

        // use JSON AppModel
        compiler.setAppModel(updatedModel);

        if (!compiler.generate())
        {
            String zipLocation = compiler.generateZip(
                    new File(RabbitMQ.getCompilerConfig().pathToStoreZips).toPath(),
                    requestData.id,
                    true);

            byte[] zip = FileUtils.readFileToByteArray(new File(zipLocation));
            result.data.view = new String(Base64.getEncoder().encode(zip));

            compiler.cleanCurrentProjectZip();
            compiler.cleanCurrentProjectFiles();
        } else {
            compiler.getErrors().forEach(result.data.errors::add);
        }
        
        return result;
    }
}
