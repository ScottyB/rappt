package io.rappt.compiler;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.*;
import org.apache.commons.lang3.text.WordUtils;
import io.rappt.android.AndroidModel;
import io.rappt.model.AppModel;
import io.rappt.model.Instruction;
import io.rappt.model.ValuePath;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

// Contains the logic for constructing the Data classes
public class DataModelBuilder {

    private static final Logger logger = Logger.getLogger(DataModelBuilder.class.getName());

    public IntermediateModel buildDataModel(final AppModel appModel, final IntermediateModel model) {
        Multimap<String, IntermediateModel.JsonPath> dataClassFields = HashMultimap.create();
        IntermediateModel newModel = model;
        dataClassFields = addResourceFields(model, dataClassFields);
        // dataClassFields = addTrackerFieldsAndClasses(model, dataClassFields);
        newModel.dataClassFields = dataClassFields;
        return newModel;
    }

    private Multimap<String, IntermediateModel.JsonPath> addTrackerFieldsAndClasses(IntermediateModel model, Multimap<String, IntermediateModel.JsonPath> dataClassFields) {
        Multimap<String, IntermediateModel.JsonPath> fields = dataClassFields;
        fields.put("LocationUpdate", new IntermediateModel.JsonPath("latitude", "latitude", "double", false));
        fields.put("LocationUpdate", new IntermediateModel.JsonPath("longitude", "longitude", "double", false));
        fields.put("LocationUpdate", new IntermediateModel.JsonPath("issuedAt", "issuedAt", "Date", true));

        fields.put("ActivityUpdate", new IntermediateModel.JsonPath("kind", "kind", "int", false));
        fields.put("ActivityUpdate", new IntermediateModel.JsonPath("confidence", "confidence", "int", false));
        fields.put("ActivityUpdate", new IntermediateModel.JsonPath("issuedAt", "issuedAt", "Date", true));
        return fields;
    }

    static JsonElement getElement(Multimap<String, IntermediateModel.JsonPath> dataClassFields, IntermediateModel.JsonPath path) {
        JsonElement resultElement;
        if (path.isObjectType) {
            resultElement = processObject(dataClassFields, WordUtils.capitalize(path.variableName));
        } else {
            resultElement = new JsonPrimitive("");
        }
        return resultElement;
    }

    static JsonObject processObject(Multimap<String, IntermediateModel.JsonPath> dataClassFields, String key) {
        JsonObject jsonObject = new JsonObject();
        dataClassFields.get(key).forEach(jp -> {
            jsonObject.add(jp.variableName, getElement(dataClassFields, jp));
        });
        return jsonObject;
    }

    // TODO: Does not handle lists nested within the object
    // TODO: Infinite nesting not supported
    static private Map<String, JsonElement> createJsonFiles(final IntermediateModel intermediateModel) {
        Map<String, JsonElement> objects = new HashMap<>();
        Multimap<String, IntermediateModel.JsonPath> dataClassFields = intermediateModel.dataClassFields;
        intermediateModel.resources.values().forEach(r -> {
            String jsonFile = WordUtils.capitalize(r.functionName) + ".json";
            boolean isList = r.responseObject.startsWith("List<");
            String key = r.responseClassName;
            JsonObject newObject = processObject(dataClassFields, key);
            JsonElement newElement;
            if (isList) {
                JsonArray newArray = new JsonArray();
                newArray.add(newObject);
                newElement = newArray;
            } else {
                newElement = newObject;
            }
            objects.put(jsonFile, newElement);
        });
        return objects;
    }

    static public void writeJsonFiles(final IntermediateModel interModel, final AndroidModel android, boolean isVerbose) {
        if (android.isOffline) {
            if (isVerbose) logger.info("Building JSON file stubs...");
            Map<String, JsonElement> objects = createJsonFiles(interModel);
            objects.forEach((k, v) -> {
                Path file = Paths.get(android.project.assetsFolder, k);
                if (Files.exists(file)) {
                    if (isVerbose) logger.log(Level.SEVERE, file + " already exists");
                } else {
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    try {
                        Files.write(file, gson.toJson(v).getBytes("utf-8"), StandardOpenOption.CREATE_NEW);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private Multimap<String, IntermediateModel.JsonPath> addResourceFields(final IntermediateModel model, final Multimap<String, IntermediateModel.JsonPath> dataClassFields) {
        final Multimap<String, IntermediateModel.JsonPath> newFields = dataClassFields;
        FormatUtils utils = new FormatUtils();
        model.intermediateApis.values().forEach(a -> {
            a.resources.values().forEach(r -> {
                Consumer<ValuePath> buildFields = (v) -> {
                    if (!v.path.isEmpty())
                        newFields.put(r.responseClassName, v.path.get(0).transformComponent(utils, null, model));
                    for (int i = 0; i < v.path.size() - 1; i++) {
                        ValuePath.JsonPath jPath = v.path.get(i);
                        newFields.put(FormatUtils.formatDataClassName(jPath.fieldName), v.path.get(i + 1).transformComponent(utils, null, model));
                    }
                };
                r.requestValuePaths.forEach(buildFields);
                r.responseValuePaths.forEach(buildFields);
                if (r.requestValuePaths.isEmpty() && r.responseValuePaths.isEmpty())
                    newFields.put(r.responseClassName, new ValuePath.JsonPath().transformComponent(utils, null, model));
            });
        });
        return newFields;
    }

    // TODO: Split into types of requests and handle multiple calls
    // TODO: Need multiple passes over the instructions
    // TODO: Single unified instruction method needed
    // Adds request values to a resource
    public IntermediateModel addFieldsForRequest(final AppModel appModel, final IntermediateModel model) {
        final IntermediateModel newModel = model;
        appModel.screens.forEach(s -> {
            Collection<ValuePath> requestPaths = new ArrayList<>();
            Collection<ValuePath> responsePaths = new ArrayList<>();
            s.view.images.forEach(p -> {
                if (p.valuePath != null) requestPaths.add(p.valuePath);
            });
            s.view.textInputs.forEach(t -> {
                if (t.valuePath != null) requestPaths.add(t.valuePath);
            });
            //requestPaths.add(l.valuePath));
            s.view.labels.forEach(l -> l.valuePath.ifPresent(requestPaths::add));
            s.view.onLoad.instructions.forEach(i -> {
                if (i instanceof Instruction.Call) {
                    Instruction.Call instruction = (Instruction.Call) i;
                    IntermediateModel.Resource resource = newModel
                            .resources.get(instruction.apiId, instruction.resourceId);
                    resource.requestValuePaths.addAll(requestPaths);

                    s.view.map.ifPresent(m -> {
                        m.dynamicMarkers.forEach(dm -> {
//                            resource.requestMapFields.add(dm.descriptionPath);
                            resource.requestValuePaths.add(dm.titlePath);
                            resource.requestValuePaths.add(dm.latitudePath);
                            resource.requestValuePaths.add(dm.longitudePath);

                        });
                    });

                    s.view.dynamicLists.forEach(l -> {
                        String dataClassName = resource.responseClassName;
                        if (l.listFieldPath != null) {
                            responsePaths.add(l.listFieldPath);
                            IntermediateModel.ListField fd = model.resourceListFields.get(instruction.resourceId);
                            dataClassName = fd.listClassName;
                        }
                        // TODO: Move this out to be handled with a view
                        l.accept(p -> {
                            if (p instanceof ValuePath) {
                                ValuePath newPath = (ValuePath) p;
                                if (l.listFieldPath != null) {
                                   newPath.addPrefixPath(l.listFieldPath.path);
                                }
                                responsePaths.add(newPath);
                            }
                        });
                        resource.responseValuePaths.addAll(responsePaths);
                        model.intermediateScreens.get(s.id).listItemDataClassName = dataClassName;

                    });
                }
            });


            s.view.buttons.forEach(b -> {
                // Request paths need to be updated before an instruction call
                b.onTouch.instructions.forEach(i -> {
                    if (i instanceof Instruction.DynamicNotification) {
                        Instruction.DynamicNotification dN = (Instruction.DynamicNotification) i;
                        requestPaths.add(dN.contentPath);
                        requestPaths.add(dN.titlePath);
                    }
                });
                b.onTouch.instructions.forEach(i -> {
                    if (i instanceof Instruction.Call) {
                        Instruction.Call instruction = (Instruction.Call) i;
                        IntermediateModel.Resource resource = newModel
                                .resources.get(instruction.apiId, instruction.resourceId);
                        resource.requestValuePaths.addAll(requestPaths);
                    }

                });
            });
        });
        //  IntermediateModel newestModel = addTrackerModel(appModel, newModel);
        return newModel;
    }

}
