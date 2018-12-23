package io.rappt.compiler;

import com.github.abrarsyed.jastyle.ASFormatter;
import io.rappt.antlr.*;
import io.rappt.swagger.SwaggerGenerator;
import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.apache.commons.io.FileUtils;
import io.rappt.AMLLexer;
import io.rappt.AMLParser;
import io.rappt.android.AndroidModel;
import io.rappt.android.AndroidScreen;
import io.rappt.android.FileTemplate;
import io.rappt.android.STIgnore;
import io.rappt.antlr.*;
import io.rappt.layout.Layout;
import io.rappt.model.AppModel;
import io.rappt.settings.AppConfig;
import io.rappt.settings.CompilerConfig;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;
import org.xml.sax.SAXException;

import java.io.*;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Compiler
 * Compiles AML code into an Android Gradle Project
 */
public class Compiler {
    // TODO: Validate if this is doing what it was designed for
    private static final Pattern PROJECT_NAME_PATTERN =
        Pattern.compile("^[a-zA-Z][a-zA-Z_$0-9]*$");
    private static final Pattern PACKAGE_NAME_PATTERN =
        Pattern.compile("^[a-z][a-z_$0-9]*(\\.[a-z][a-z_$0-9]*)*$");

    static final char DELIMITERS = '$';
    private final Path amlFilePath;
    private final Optional<Path> swaggerFilePath;
    private final String projectName;
    private final Optional<String> projectId;
    private final String packageName;
    private final Logger logger = Logger.getLogger(Compiler.class.getName());
    private List<String> errors = new ArrayList<>();
    private Optional<AppModel> appModel = Optional.empty();
    private AndroidModel androidModel;
    private IntermediateModel intermediateModel;
    private Path projectRootDir = null;
    private CompilerConfig compilerConfig;
    private AppConfig appConfig;
    private String zipFileLocation = null;

    public void writeModel(String model, String fileName, boolean printModels, String message) {
        if (printModels) {
            try {
                File file = new File("./" + fileName);
                BufferedWriter output = new BufferedWriter(new FileWriter(file));
                output.write(model);
                logger.info("\n" + message + " " + fileName);
                output.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void generateCode(Queue<FileTemplate> psm) throws StringTemplateException {
        STGroup group = new STGroupFile("templates/all.stg", DELIMITERS, DELIMITERS);
        group.delimiterStartChar = group.delimiterStopChar = DELIMITERS;
        for (FileTemplate t : psm) {
            ST st = group.getInstanceOf(t.templateName);
            if (st == null) {
                String error = "Error with template: " + t.templateName;
                StringTemplateException e = new StringTemplateException(error);
                logger.log(Level.SEVERE, error, e);
                throw e;
            }
            Field[] classFields = t.getClass().getFields();
            if (!t.getClass().getSimpleName().equals("Template")) {
                for (Field f : classFields) {
                    STIgnore[] annotation = f.getAnnotationsByType(STIgnore.class);
                    if (annotation.length == 0) {
                        f.setAccessible(true);
                        try {
                            st.add(f.getName(), f.get(t));
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            writeTemplate(t.outputPath, st.render());
        }
    }

    private void writeTemplate(String outputFile, String templateString) {
        try {
            FileWriter fw = new FileWriter(outputFile);
            if (outputFile.endsWith(".java")) {
                ASFormatter formatter = new ASFormatter();
                formatter.format(new StringReader(templateString), fw);
            } else {
                fw.write(templateString);
            }
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public IntermediateModel parseAppModel(final AppModel appModel) {

        // ORDER MATTERS!!!
        IntermediateTranslator intermediateTranslator = new IntermediateTranslator();
        IntermediateModel interModel = new IntermediateModel(appModel.projectName);
        interModel = intermediateTranslator.buildApiAndDataModel(appModel, interModel);
        interModel = intermediateTranslator.buildScreens(appModel, interModel);
        interModel = intermediateTranslator.buildMainNavigation(appModel, interModel);
        interModel = intermediateTranslator.processWholeModel(appModel, interModel);
        interModel = intermediateTranslator.buildNotifications(appModel, interModel);
        interModel = intermediateTranslator.buildSharedPreferences(appModel, interModel);
        interModel = intermediateTranslator.processCallInstructions(appModel, interModel);
        interModel = intermediateTranslator.buildDynamicLists(appModel, interModel);
        interModel = intermediateTranslator.buildModel(appModel, interModel);

        interModel = intermediateTranslator.buildDataModel(appModel, interModel);
        interModel = intermediateTranslator.buildUiFieldStrings(appModel, interModel);
        interModel = intermediateTranslator.buildMiscellaneous(appModel, interModel);
        return interModel;
    }

    public AndroidModel parseIntermediateModel(final IntermediateModel interModel, final AppModel appModel, Path targetDirectory) throws IntermediateException {
        AndroidTranslator translator = new AndroidTranslator(this.compilerConfig);
        AndroidModel androidModel = new AndroidModel(targetDirectory, appModel.androidSdk, interModel, appModel.projectName, appModel.packageName);
        // IMPORTANT: Order of the following methods matters
        try {
            androidModel = translator.configureModel(appModel, interModel, androidModel);
            androidModel = translator.buildFunctionality(appModel, interModel, androidModel);
            androidModel = translator.buildScreens(appModel, interModel, androidModel);
            androidModel = translator.addScreenFunctionality(appModel, interModel, androidModel);
        } catch (Exception e) {
            throw new IntermediateException("Error with Intermediate Translator", e);
        }
        return androidModel;
    }

    public void generateApp() throws StringTemplateException, IOException, SAXException {
        // Generate code!!!
        androidModel.project.buildProjectStructure();
        androidModel.copyFiles(compilerConfig);
        androidModel.copyProjectFiles("templates/asIs", Paths.get(androidModel.project.projectDir));
        generateCode(androidModel.getTemplates());
        androidModel.copyDrawables();
        runLayoutEngine(androidModel);
        // Build Data Model if required
        DataModelBuilder.writeJsonFiles(intermediateModel, androidModel, true);
    }

    private void runLayoutEngine(AndroidModel androidModel) throws IOException, SAXException {
        for (AndroidScreen screen : androidModel.androidScreens) {
            Collection<AndroidScreen.ViewController> views = new ArrayList(screen.elements);
            views.add(screen.viewController);
            for (AndroidScreen.ViewController vC : views) {
                if (vC.view.hasLayout()) {
                    Path inputFile = Paths.get(vC.view.outputPath);
                    Layout layout = new Layout(vC.view.layout, inputFile);
                    layout.parse(inputFile);
                }
            }
        }
    }


    public AMLParseModel parse(String amlFile) throws IOException {
        AMLLexer lex = new AMLLexer(new ANTLRFileStream(amlFile));
        AMLParser parser = new AMLParser(new CommonTokenStream(lex));
        AMLParseModel model = new AMLParseModel();
        parser.removeErrorListeners();
        parser.addErrorListener(model);
        model.tree = parser.parse();
        return model;
    }

    public AppModel parseAMLParseModel(AMLParser.ParseContext tree, String projectName, String packageName) {
        AppModel appModel = new AppModel(projectName, packageName);
        AMLIdVisitor idVisitor = new AMLIdVisitor(appModel);
        idVisitor.visit(tree);

        // Check ID Model
        AMLIdCheckVisitor idCheckVisitor = new AMLIdCheckVisitor(idVisitor.model);
        idCheckVisitor.visit(tree);

        // Build AppModel Model
        AppModelVisitor modelVisitor = new AppModelVisitor(idVisitor.model);
        modelVisitor.visit(tree);

        // Add behaviours
        BehaviourVisitor behaviourVisitor = new BehaviourVisitor(modelVisitor.getModel());
        behaviourVisitor.visit(tree);

        // Check AppModel Model
        PIMSemanticVisitor pimSemanticVisitor = new PIMSemanticVisitor(behaviourVisitor.getModel());
        pimSemanticVisitor.visit(tree);
        return pimSemanticVisitor.appModel;
    }

    // swaggerFilePath does not need to be provided (for example, if the AML does not need to make REST API calls)
    // projectId does not need to be (for example, casual command line usage)
    public Compiler(CompilerConfig compilerConfig, AppConfig appConfig) {
        Optional<Path> swagger = Optional.empty();
        if (appConfig.hasSwagger()) {
            swagger = Optional.of(new File(appConfig.swaggerFile).toPath());
        }

        Optional<String> id = Optional.empty();
        if (appConfig.hasProjectId()) {
            id = Optional.of(appConfig.projectId);
        }

        this.compilerConfig = compilerConfig;
        this.appConfig = appConfig;

        this.amlFilePath = new File(appConfig.amlFile).toPath();
        this.swaggerFilePath = swagger;
        this.projectId = id;
        this.projectName = appConfig.projectName;
        this.packageName = appConfig.packageName;
    }

    public void setAppModel(AppModel appModel) {
        this.appModel = Optional.of(appModel);
    }
    public Optional<AppModel> getAppModel() {
        // Will return Optional.empty() if appModel has not been set
        return this.appModel;
    }

    // TODO: Move into AML View
    // Return true if no user errors
    public boolean amlToModel() throws IOException {
        AMLParseModel amlParseModel = parse(amlFilePath.toString());
        if (amlParseModel.hasErrors()) {
            this.errors = new ArrayList<>(amlParseModel.getErrors());
            amlParseModel.clearErrors();
            return false;
        }
        AppModel appModel = parseAMLParseModel(amlParseModel.tree, projectName, packageName);
        if (appModel.hasErrors()) {
            this.errors = new ArrayList<>(amlParseModel.getErrors());
            amlParseModel.clearErrors();
            return false;
        }

        // overwrite existing appModel
        this.appModel = Optional.of(appModel);
        return true;
    }

    // Return true if no user errors
    public boolean didBuildModels(Path targetDirectory) throws IOException, IntermediateException {
        if (!appModel.isPresent()) {
            // AppModel has not been built. Build AppModel from AML.
            if (!amlToModel()) {
                return false;
            }
        }

        if (swaggerFilePath.isPresent()) {
            System.out.println("Swagger integration is not supported yet. API must be described using AML.");
        }
        intermediateModel = parseAppModel(appModel.get());
        androidModel = parseIntermediateModel(intermediateModel, appModel.get(), targetDirectory);

        return true;
    }

    public List<String> getErrors() {
        return this.errors;
    }

    public boolean generate() throws StringTemplateException, IntermediateException, SAXException, IOException {
        return generate(new File(compilerConfig.pathToStoreGeneratedApps).toPath(), appConfig.generateApiDoc, appConfig.generateApiServer);
    }

    /**
     * Generates project containing Android app, Swagger Doc, and Swagger server.
     *
     * @param targetDirectory Directory to generate project root directory within
     * @param genDoc True to generate API doc from Swagger spec (if Swagger spec provided)
     * @param genServer True to generate Node.js server from Swagger spec (if Swagger spec provided)
     * @return true if errors
     */
    public boolean generate(Path targetDirectory, boolean genDoc, boolean genServer) throws IOException, IntermediateException, StringTemplateException, SAXException {
        boolean hasErrors = true;

        if (!validateProjectName(this.projectName)) {
            this.errors = new ArrayList<String>();
            this.errors.add("Invalid projectName: " + this.projectName);
            return true;
        } else if (!validatePackageName(this.packageName)) {
            this.errors = new ArrayList<String>();
            this.errors.add("Invalid packageName: " + this.packageName);
            return true;
        }

        // If available, use the projectId to avoid naming conflicts
        String rootDir = projectName;
        if (projectId.isPresent()) {
            rootDir += "-" + projectId.get();
        }

        projectRootDir = targetDirectory.resolve(rootDir);

        if (didBuildModels(projectRootDir)) {
            generateApp();

            if (genDoc) {
                generateSwaggerDoc(projectRootDir);
            }
            if (genServer) {
                generateSwaggerServer(projectRootDir);
            }
            hasErrors = false;
        }
        return hasErrors;
    }

    /**
     * Checks project name matches expected patterns and passes other checks.
     *
     * @param projectName
     * @return true if valid
     */
    static public boolean validateProjectName(String projectName) {
        return PROJECT_NAME_PATTERN.matcher(projectName).matches() && !projectName.equals("api");
    }

    /**
     * Checks package name matches expected patterns and passes other checks.
     *
     * @param packageName
     * @return true if valid
     */
    public static boolean validatePackageName(String packageName) {
        return PACKAGE_NAME_PATTERN.matcher(packageName).matches() && !packageName.startsWith("java.");
    }

    public String generateZip() throws IOException {
        return generateZip(new File(this.compilerConfig.pathToStoreZips).toPath(), this.appConfig.zipFile, this.appConfig.generateZip);
    }

    /**
     * Returns the path to the generated project, optionally as a zip file.
     * Precondition: {@link #generate}.
     *
     * @param targetDirectory Directory to generate zip file within. Ignored if hasZip is false.
     * @param zip Name of zip directory. Ignored if hasZip is false.
     * @param hasZip True to generate zip file of project.
     * @return Path to zip file if hasZip. Project root directory if hasZip is false.
     */
    public String generateZip(Path targetDirectory, String zip, boolean hasZip) throws IOException {
        assert projectRootDir != null; // generate() must be called first
        String outputDir = projectRootDir.toString();
        if (hasZip) {
            File zipFile = new File(targetDirectory.resolve(zip + ".zip").toString());

            // Create parent directory if doesn't exist
            if (!zipFile.getParentFile().exists()){
                zipFile.getParentFile().mkdirs();
            }
            outputDir = zipDirectory(targetDirectory.resolve(zip + ".zip").toString(), outputDir);
            deleteDirectories(Paths.get(outputDir));
        }

        zipFileLocation = outputDir;
        return outputDir;
    }

    private void generateSwaggerDoc(Path outputDir) {
        if (!this.swaggerFilePath.isPresent()) {
            // cannot generate Swagger doc without Swagger file
            return;
        }

        Path swaggerFile = swaggerFilePath.get();
        SwaggerGenerator.generateSwaggerDoc(swaggerFile, outputDir);
    }

    private void generateSwaggerServer(Path outputDir) {
        if (!this.swaggerFilePath.isPresent()) {
            // cannot generate Swagger server without Swagger file
            return;
        }

        Path swaggerFile = swaggerFilePath.get();
        SwaggerGenerator.generateSwaggerServer(swaggerFile, outputDir);
    }

    private void deleteDirectories(Path directory) throws IOException {
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }


    private String zipDirectory(String outputDir, String directoryToZip) throws IOException {
        Map<String, String> zip_properties = new HashMap<>();
        zip_properties.put("create", "true");
        zip_properties.put("encoding", "UTF-8");
        URI zip_disk = URI.create("jar:file:" + outputDir);
        Path pathToZip = Paths.get(directoryToZip);
        try (FileSystem zipfs = FileSystems.newFileSystem(zip_disk, zip_properties)) {
            Files.walkFileTree(pathToZip, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path newPath = pathToZip.relativize(file);
                    Path pathInZipfile = zipfs.getPath(pathToZip.getFileName().toString(), newPath.toString());
                    Files.copy(file, pathInZipfile, StandardCopyOption.REPLACE_EXISTING);
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    Path directory = pathToZip.getParent().relativize(dir);
                    Path pathInZipfile = zipfs.getPath(directory.toString());
                    if (!Files.exists(pathInZipfile)) {
                        Files.createDirectory(pathInZipfile);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        return outputDir;
    }

    public static class IntermediateException extends Exception {
        public IntermediateException(String message) {
            super(message);
        }

        public IntermediateException(String message, Exception e) {
            super(message, e);
        }
    }

    public static class StringTemplateException extends Exception {
        public StringTemplateException(String error) {
            super(error);
        }
    }

    public String getProjectPath(){
        return projectRootDir.toString();
    }

    public void cleanCurrentProjectFiles() {
        if (projectRootDir != null) {
            File projectDir = new File(projectRootDir.toString());

            if (projectDir.exists()) {
                try {
                    FileUtils.deleteDirectory(projectDir);
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Compiler: Could not delete project", e);
                }
            }
        }
    }

    public void cleanCurrentProjectZip() {
        if (zipFileLocation != null && !zipFileLocation.isEmpty()) {
            File zipFile = new File(zipFileLocation);

            if (zipFile.exists()) {
                zipFile.delete();
            }
        }
    }
}

