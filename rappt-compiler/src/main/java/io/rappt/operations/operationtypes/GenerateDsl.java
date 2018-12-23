package io.rappt.operations.operationtypes;

import io.rappt.compiler.Compiler;
import io.rappt.runnableinterface.rabbitmq.jsonobjects.ResponseObject;
import io.rappt.model.AppModel;
import io.rappt.runnableinterface.rabbitmq.jsonobjects.RequestObject;

import java.util.logging.Level;
import java.util.logging.Logger;

public class GenerateDsl extends Operation {
    private static final Logger LOGGER = Logger.getLogger(GenerateDsl.class.getName());

    public String operationName(){
        return "/generate/dsl";
    }

    public ResponseObject performOperation(RequestObject requestData) {
        ResponseObject result = requestData.replyTo();

        AppModel appModel = requestData.data.model;
        if (appModel == null) {
            LOGGER.warning("appModel is null. Aborting compilation.");
            result.data.errors.add("Internal error: appModel not provided in JSON request");
            // We can't generate dsl without a valid AppModel
            return result;
        }

        try {
            String aml = appModel.getAml();
            result.data.view = aml;
        } catch (Compiler.StringTemplateException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            result.data.errors.add("Internal Error: Could not generate AML");
        }

        return result;
    }
}