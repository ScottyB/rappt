package io.rappt.operations.operationtypes;

import org.xml.sax.SAXException;
import io.rappt.compiler.Compiler;
import io.rappt.runnableinterface.rabbitmq.jsonobjects.RequestObject;
import io.rappt.runnableinterface.rabbitmq.jsonobjects.ResponseObject;

import java.io.IOException;

public abstract class Operation {
    public abstract String operationName();
    public abstract ResponseObject performOperation(RequestObject requestData)  throws IOException, Compiler.IntermediateException, Compiler.StringTemplateException, SAXException;
}
