package io.rappt.swagger;

import com.wordnik.swagger.codegen.ClientOptInput;
import com.wordnik.swagger.codegen.ClientOpts;
import com.wordnik.swagger.codegen.CodegenConfig;
import com.wordnik.swagger.codegen.DefaultGenerator;
import com.wordnik.swagger.codegen.languages.*;
import com.wordnik.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;

import java.io.File;
import java.nio.file.Path;

public class SwaggerGenerator extends DefaultGenerator {

    public static void generateSwaggerDoc (Path swaggerFilePath, Path outputDir) {
        try {
            SwaggerGenerator.callSwagger(swaggerFilePath, outputDir.resolve("api" + File.separator + "doc"), new StaticHtmlGenerator());
        } catch (SwaggerException e) {
            e.printStackTrace();
        }
    }

    public static void generateSwaggerServer (Path swaggerFilePath, Path outputDir) {
        try {
            SwaggerGenerator.callSwagger(swaggerFilePath, outputDir.resolve("api"), new NodeJSServerCodegen());
        } catch (SwaggerException e) {
            e.printStackTrace();
        }
    }

    private static void callSwagger(Path swaggerFilePath, Path outputDir, CodegenConfig generator) throws SwaggerException {
        // Adapted from com.wordnik.swagger.codegen.Codegen.main() with command line parsing code removed

        // TODO: Validate Swagger YAML file
        // Invalid Swagger can cause generator to produce missing/bad code

        // TODO: Security Code Review to determine whether use of $ref in Swagger spec to include external files could lead to unauthorised file access

        ClientOptInput clientOptInput = new ClientOptInput();
        ClientOpts clientOpts = new ClientOpts();
        clientOptInput.setConfig(generator);
        clientOptInput.getConfig().setOutputDir(outputDir.toString());

        // read method will catch and print any exceptions
        Swagger swagger = new SwaggerParser().read(swaggerFilePath.toString());
        if (swagger == null) {
            // TODO: Find way to capture original cause of error.
            throw new SwaggerException("Could not parse Swagger YAML");
        }

        clientOptInput.opts(clientOpts).swagger(swagger);

        // generate method will catch and print any exceptions
        // TODO: Find way to flag error if there are internal generation errors
        new SwaggerGenerator().opts(clientOptInput).generate();
    }
}