package io.rappt.runnableinterface.rabbitmq.jsonobjects;

public class RabbitMQSettingsObject {

    public String inboundExchangeName = "compiler-bound";
    public String inboundQueueName = "compiler-bound";
    public String outboundExchangeName = "user-bound";
    public String host = "localhost";
    public int port = 5672;
    public String username;
    public String password;
    public String pathToScriptDirectory = "";
    public String pathToStoreZips = "";
    public String pathToStoreGeneratedApps = "";
}
