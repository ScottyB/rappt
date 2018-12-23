package io.rappt.runnableinterface.rabbitmq;

import com.google.gson.*;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import io.rappt.compiler.Compiler;
import io.rappt.operations.Operations;
import io.rappt.runnableinterface.rabbitmq.jsonobjects.RabbitMQSettingsObject;
import io.rappt.runnableinterface.rabbitmq.jsonobjects.RequestData;
import io.rappt.runnableinterface.rabbitmq.jsonobjects.RequestObject;
import io.rappt.runnableinterface.rabbitmq.jsonobjects.ResponseObject;
import joptsimple.OptionSet;
import io.rappt.settings.CompilerConfig;
import org.xml.sax.SAXException;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class RabbitMQ {
    private static final Logger LOGGER = Logger.getLogger(RabbitMQ.class.getName());
    private static boolean CLOSE_REQUESTED = false;
    // TODO: Validate if this is doing what it was designed for, create a seperate REGEX for project name
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("^[a-zA-Z_\\$][\\w\\$]*(?:\\.[a-zA-Z_\\$][\\w\\$]*)*$");
    public static final Gson SETTINGS_GSON = new Gson();

    // TODO: Dependency Injection rather than static field
    private static RabbitMQSettingsObject SETTINGS;
    private static CompilerConfig COMPILER_CONFIG = CompilerConfig.getDefault();

    public static CompilerConfig getCompilerConfig () {
        return COMPILER_CONFIG;
    }

    // Intended for test purposes only
    public static void setCompilerConfig (CompilerConfig config) {
        COMPILER_CONFIG = config;
    }

    public static RabbitMQSettingsObject getRabbitMQSettingsObject() {
        return SETTINGS;
    }

    // Extracts compiler config from Rabbit settings
    public static CompilerConfig readCompilerConfig(RabbitMQSettingsObject rabbitMQSettingsObject) {
        try {
            return new CompilerConfig.Builder()
                    .setPathToScriptDir(rabbitMQSettingsObject.pathToScriptDirectory)
                    .setPathToStoreGeneratedApps(rabbitMQSettingsObject.pathToStoreGeneratedApps)
                    .setPathToStoreZips(rabbitMQSettingsObject.pathToStoreZips)
                    .build();
        } catch (CompilerConfig.SettingsException e) {
            LOGGER.severe(e.toString());
            LOGGER.severe("Invalid Compiler Settings, using defaults");
            return CompilerConfig.getDefault();
        }
    }

    public static void main(File settingsFile) {
        LOGGER.info("Compiler started with RabbitMQ...");

        try {
            byte[] encoded = Files.readAllBytes(settingsFile.toPath());
            String settingsString = new String(encoded, Charset.defaultCharset());
            SETTINGS = SETTINGS_GSON.fromJson(settingsString, RabbitMQSettingsObject.class);
        } catch (IOException e) {
            LOGGER.severe(e.toString());
            LOGGER.severe("Could not load settings JSON");
            return;
        }

        COMPILER_CONFIG = readCompilerConfig(SETTINGS);

        final Thread mainThread = Thread.currentThread();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                CLOSE_REQUESTED = true;
                LOGGER.info("Close Requested, Shutting Down...");
                mainThread.interrupt();
            }
        });

        run();
    }

    public static void runInCLI(OptionSet options) {
        LOGGER.info("Compiler started with RabbitMQ in CLI mode," +
                    "Daemon will terminate after processing command line arguments.");
        LOGGER.info("Inspecting passed arguments...");

        boolean failed = false;

        // Checking Operation
        if (!options.has("o") && !options.hasArgument("o")) {
            failed = true;
        }
        // Checking AML File
        if (!options.has("a") && !options.hasArgument("a")) {
            failed = true;
        }

        if (failed) {
            LOGGER.severe("Arguments 'a' or 'o' are missing or have missing arguments!");
            return;
        }

        SETTINGS = new RabbitMQSettingsObject();

        if (options.has("d") && options.hasArgument("d")) {
            SETTINGS.pathToStoreGeneratedApps = options.valueOf("d").toString();
        } else {
            SETTINGS.pathToStoreGeneratedApps = "project-output";
        }

        SETTINGS.pathToStoreZips = "zips";

        SETTINGS.pathToScriptDirectory = "aml";

        COMPILER_CONFIG = readCompilerConfig(SETTINGS);

        LOGGER.info("Building request object...");

        RequestObject request = new RequestObject();
        request.id = "123";
        request.operation = (String)options.valueOf("o");
        request.data = new RequestData();

        if (options.has("n") && options.hasArgument("n")) {
            request.data.projectName = (String)options.valueOf("n");
        } else {
            request.data.projectName = "tango";
        }

        if (options.has("p") && options.hasArgument("p")) {
            request.data.packageName = (String)options.valueOf("p");
        } else {
            request.data.packageName = "twist";
        }

        String amlCode;

        try {
            amlCode = new String(Files.readAllBytes(((File)options.valueOf("a")).toPath()));
        } catch (IOException e) {
            LOGGER.severe("Could not read input aml file, using blank string");
            amlCode = "";
        }

        request.data.setView(amlCode);

        try {
            String result = processRequest(request);

            LOGGER.info("Success! Data was processed");
            System.out.println(result);
        } catch (Exception e) {
            LOGGER.severe(e.toString());
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public static void run(){
        try {
            ConnectionFactory factory = new ConnectionFactory();

            factory.setHost(SETTINGS.host);
            factory.setPort(SETTINGS.port);
            if (SETTINGS.username != null && !SETTINGS.password.isEmpty()){
                factory.setUsername(SETTINGS.username);
            }
            if (SETTINGS.password != null && !SETTINGS.password.isEmpty()) {
                factory.setPassword(SETTINGS.password);
            }

            Connection connection = factory.newConnection();

            //Outbound Channels
            Channel outboundChannel = connection.createChannel();
            outboundChannel.exchangeDeclare(SETTINGS.outboundExchangeName, "fanout");

            //Inbound Channels / Consumers
            Channel inboundChannel = connection.createChannel();
            inboundChannel.exchangeDeclare(SETTINGS.inboundExchangeName, "fanout");
            inboundChannel.queueDeclare(SETTINGS.inboundQueueName, false, false, false, null);
            inboundChannel.queueBind(SETTINGS.inboundQueueName, SETTINGS.inboundExchangeName, "");

            QueueingConsumer inboundConsumer = new QueueingConsumer(outboundChannel);
            inboundChannel.basicConsume(SETTINGS.inboundQueueName, false, inboundConsumer);

            while (!CLOSE_REQUESTED) {
                QueueingConsumer.Delivery delivery = inboundConsumer.nextDelivery();
                String message = new String(delivery.getBody());

                Optional<String> result = processMessage(message);
                if (result.isPresent()) {
                    outboundChannel.basicPublish(SETTINGS.outboundExchangeName, "", null, result.get().getBytes());
                }

                //Acknowledge the delivery was processed successfully
                inboundChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        } catch (InterruptedException e) {
            //This exception will occur (most likely) because a exit was requested
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    public static Optional<String> processMessage(String message) {
        RequestObject requestObject;
        try {
            requestObject = Operations.jsonToRequestObject(message);
        } catch (JsonParseException e) {
            LOGGER.log(Level.SEVERE, "Bad JSON Syntax", e);
            // We can't reply without a message to reply to
            return Optional.empty();
        }
        if (!requestObject.getIsValid()) {
            LOGGER.log(Level.SEVERE, "Missing/Invalid JSON fields in requestObject");
            // We can't reply without a message to reply to
            return Optional.empty();
        }
        if (!requestObject.data.getIsValid()) {
            LOGGER.log(Level.SEVERE, "Missing/Invalid JSON fields in requestObject data");
            return Optional.of(getErrorObject(requestObject, "Missing/Invalid JSON fields in request"));
        }

        try {
            return Optional.of(processRequest(requestObject));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            return Optional.of(getErrorObject(requestObject, "Internal Error"));
        }
    }

    private static String processRequest(RequestObject requestObject) throws IOException, Compiler.IntermediateException, Compiler.StringTemplateException, SAXException {
        return Operations.performOperation(requestObject.operation, requestObject);
    }

    private static String getErrorObject(RequestObject requestObject, String errorMessage) {
        ResponseObject responseObject = requestObject.replyTo();
        responseObject.data.errors.add(errorMessage);
        return Operations.responseObjectToJson(responseObject);
    }
}
