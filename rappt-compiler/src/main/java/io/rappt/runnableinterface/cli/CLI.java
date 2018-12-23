package io.rappt.runnableinterface.cli;

import io.rappt.compiler.Compiler;
import io.rappt.settings.AppConfig;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import io.rappt.runnableinterface.rabbitmq.RabbitMQ;
import io.rappt.settings.CompilerConfig;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.regex.Pattern;


public class CLI {
    private static final Logger LOGGER = Logger.getLogger(CLI.class.getName());

    private static void setupLogger() {
        try {
            Date now = new Date();
            SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy_MM_dd__hh_mm_ss");

            SimpleFormatter simpleFormatter = new SimpleFormatter();
            FileHandler fileHandler = new FileHandler("log_" + dateFormatter.format(now) + ".log");
            fileHandler.setFormatter(simpleFormatter);

            Logger.getLogger("").addHandler(fileHandler);

            LOGGER.info("Log initialised");
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String... args) {
        OptionParser parser = new OptionParser();

        // For RabbitMQ Daemon
        parser.accepts("r", "Run in Daemon mode. When supplied with other arguments, " +
                "it will run supplied arguments through Daemon and return " +
                "through console as test")
                .withOptionalArg().ofType(File.class)
                .describedAs("RabbitMQ settings file").defaultsTo(new File("rabbitmq-settings.json"));
        parser.accepts("o", "Operation to run, when Daemon provided with other arguments")
                .withRequiredArg();

        OptionSpec<File> file = parser.accepts("a", "AML file used to generate app")
                .requiredUnless("r")
                .requiredIf("o")
                .withRequiredArg().ofType(File.class).describedAs("AML file");
        OptionSpec<File> swagger = parser.accepts("s", "Swagger 2.0 YAML file used to specify REST API")
                .withRequiredArg().ofType(File.class)
                .describedAs("YAML file");
        OptionSpec<File> directory = parser.accepts("d", "Output directory")
                .withOptionalArg().ofType(File.class)
                .describedAs("Output directory").defaultsTo(new File(System.getProperty("user.dir")));
        parser.accepts("v", "Verbose output to print internal models");
        parser.accepts("z", "Name of zip directory to create")
                .withRequiredArg();
        parser.accepts("p", "Package name for new project")
                .requiredIf("a")
                .requiredIf("o")
                .withRequiredArg();
        parser.accepts("n", "New app project name")
                .requiredIf("a")
                .withRequiredArg();
        parser.accepts("h", "Generate HTML doc for REST API");
        parser.accepts("b", "Generate backend server stub");

        Pattern p = Pattern.compile("^[a-zA-Z_\\$][\\w\\$]*(?:\\.[a-zA-Z_\\$][\\w\\$]*)*$");

        setupLogger();

        CompilerConfig.Builder compilerConfigBuilder = new CompilerConfig.Builder();
        AppConfig.Builder appConfigBuilder = new AppConfig.Builder();

        try {
            OptionSet options = parser.parse(args);

            if (options.has("r")) {
                // Run Daemon Mode

                if (options.specs().size() > 1) {
                    // Has at least one other argument? Run as CLI test
                    RabbitMQ.runInCLI(options);
                } else {
                    // Has no other arguments, run as proper Daemon
                    RabbitMQ.main((File) options.valueOf("r"));
                }
            } else {
                Path amlFilePath = options.valueOf(file).toPath();
                String packageName = (String) options.valueOf("p");
                String projectName = (String) options.valueOf("n");
                Optional<Path> swaggerFilePath = Optional.empty();
                if (options.has(swagger)) {
                    swaggerFilePath = Optional.of(options.valueOf(swagger).toPath());
                }
                if (!Files.exists(amlFilePath)) {
                    System.out.println("Cannot find file: " + amlFilePath);
                } else if (!amlFilePath.toString().toLowerCase().endsWith(".aml")) {
                    System.out.println("AML file must have the .aml extension");
                } else if (amlFilePath.toString().contains(" ")) {
                    System.out.println("AML file cannot contain spaces");
                }
                // Swagger file does not need to be provided, but it must be valid if provided
                else if (swaggerFilePath.filter(s -> !Files.exists(s)).isPresent()) {
                    System.out.println("Cannot find Swagger file: " + swaggerFilePath.get());
                } else if (swaggerFilePath.filter(s -> !s.toString().toLowerCase().endsWith(".yaml")).isPresent()) {
                    System.out.println("Swagger file must have the .yaml extension");
                } else if (swaggerFilePath.filter(s -> s.toString().contains(" ")).isPresent()) {
                    System.out.println("Swagger file cannot contain spaces");
                }
                else if (!p.matcher(packageName).matches()) {
                    System.out.println("Invalid package name: " + packageName);
                } else {
                    String scriptDirectory = "";
                    if (amlFilePath.getParent() != null) {
                        scriptDirectory = amlFilePath.getParent().toAbsolutePath().toString();
                    } else {
                        scriptDirectory = Paths.get("").toAbsolutePath().toString();
                    }
                    appConfigBuilder.setAmlFile(amlFilePath.toString());
                    // Builder does not require a swagger file (Swagger file will be provided only if Optional is not empty)
                    swaggerFilePath.map(s -> appConfigBuilder.setSwaggerFile(s.toString()));
                    compilerConfigBuilder.setPathToScriptDir(scriptDirectory);
                    appConfigBuilder.setPackageName(packageName);
                    appConfigBuilder.setProjectName(projectName);
                    String outDir = "";
                    boolean hasZip = false;
                    boolean genDoc = options.has("h");
                    boolean genServer = options.has("b");
                    String outputDirectory = options.valueOf(directory).toString();
                    compilerConfigBuilder.setPathToStoreGeneratedApps(outputDirectory);
                    if (hasZip = options.has("z")) {
                        String zip = (String) options.valueOf("z");
                        appConfigBuilder.setZipFile(zip);
                        compilerConfigBuilder.setPathToStoreZips(outputDirectory);
                    }
                    appConfigBuilder.setGenerateZip(hasZip);
                    appConfigBuilder.setGenerateApiDoc(genDoc);
                    appConfigBuilder.setGenerateApiServer(genServer);
                    CompilerConfig compilerConfig = compilerConfigBuilder.build();
                    AppConfig appConfig = appConfigBuilder.build();
                    Compiler rappt = new Compiler(compilerConfig, appConfig);
                    if (!rappt.generate()) {
                        outDir = rappt.generateZip();
                    } else {
                        rappt.getErrors().forEach(System.out::println);
                    }
                    System.out.println(outDir);
                }
            }
        }catch(Exception e){
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            System.err.println(e.getMessage());
            try {
                parser.printHelpOn(System.out);
            } catch (IOException io) {
                io.printStackTrace();
            }
        }
    }
}
