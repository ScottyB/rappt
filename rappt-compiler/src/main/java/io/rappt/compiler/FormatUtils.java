package io.rappt.compiler;

import io.rappt.model.Api;
import io.rappt.model.Screen;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import io.rappt.model.Resource;
import io.rappt.model.ValuePath;

import java.util.Arrays;

public class FormatUtils {

    static public String formatClassName(String classString) {
        StringBuilder sb = new StringBuilder();
        if (classString.contains("_")) {
            String[] strs = classString.split("_");
            for (int i = 0; i < strs.length; i++) {
                sb.append(WordUtils.capitalize(strs[i]));
            }
        } else {
            sb.append(WordUtils.capitalize(classString));
        }
        return sb.toString();
    }

    public String formatViewId(String id) {
        return formatId(id);
    }

    public String formatStringId(String id) {
        return WordUtils.uncapitalize(id) + "_string";
    }

    public String formatStringIdContent(String id) {
        return formatStringId(id + "_content");
    }

    public String formatStringIdTitle(String id) {
        return formatStringId(id + "_title");
    }


    public String formatTitleString(Screen screen) {
        return formatHeaderStringId(screen.id);
    }

    private String formatHeaderStringId(String id) {
        return WordUtils.uncapitalize(id) + "_title";
    }

    public String formatMarkerTitleStringId(String id) {
        return "marker_" + WordUtils.uncapitalize(id) + "_title";
    }

    public String formatMarkerDescriptionStringId(String id) {
        return "marker_" + WordUtils.uncapitalize(id) + "_description";
    }

    public String formatLayoutString(Screen screen) {
        return formatLayoutString(screen.id);
    }

    public String formatLayoutString(String id) {
        return id.toLowerCase();
    }

    public String formatId(String id) {
        return WordUtils.uncapitalize(id);
    }

    public String formatVariable(String variable) {
        return WordUtils.uncapitalize(variable);
    }

    // API and Data model utils
    public String formatClassName(Api api) {
        return FormatUtils.formatClassName(api.id) + "Api";
    }

    public String formatResourceURL(Resource resource) {
        String[] strs = new String[0];
        String temp = resource.endPoint.substring(1);
        if (temp.contains("{")) {
            temp = temp.replace("{" + resource.urlParam + "}", "");
        }
        if (temp.contains("/")) {
            strs = temp.split("/");
            temp = strs[strs.length - 1];
        }
        if (temp.contains("_")) {
            StringBuilder sb = new StringBuilder();
            strs = temp.split("_");
            sb.append(strs[0]);
            for (int i = 1; i < strs.length; i++) {
                sb.append(WordUtils.capitalize(strs[i]));
            }
            temp = sb.toString();
        }
        if (temp.contains(".")) {
            temp = temp.split("\\.")[0];
        }
        // TODO: Fix hack and find generic way to get resource className
        if (temp.isEmpty()) {
            if (strs.length >= 2) {
                temp = strs[strs.length - 2];
            } else {
                temp = resource.id;
            }
        }

        return formatDataClassName(temp);
    }

    public String formatMenuStringId(String screenId, String actionId) {
        return screenId.toLowerCase() + "_" + actionId.toLowerCase() + "_menu_item";
    }

    public String formatDataItemClass(String id) {
        return formatDataClassName(id) + "ItemView";
    }

    static public String formatDataClassName(String jsonFieldName) {
        return FormatUtils.formatClassName(jsonFieldName);
    }

    public String formatViewFunction(String id) {
        return formatVariable(id) + "Layout";
    }

    static public String formatFragmentClass(String id) {
        return formatClassName(id) + "Fragment";
    }

    static public String formatActivityClass(String id) {
        return formatClassName(id) + "Activity";
    }

    public String formatFieldTypeToJavaType(ValuePath.JsonPath jsonPath) {
        return AndroidTranslatorUtils.fieldTypeToJavaType(jsonPath);
    }

    public String formatFunction(String id) {
        return WordUtils.uncapitalize(id) + "Method";
    }

    public String formatVariableResource(String id) {
        return formatVariable(id) + "Resource";
    }

    public String formatVariableResourceTitle(String id) {
        return formatVariableResource(id + "Title");
    }

    public String formatVariableResourceContent(String id) {
        return formatVariableResource(id + "Content");
    }

    public String formatEventDataClass(String fromId, String toId) {
        return formatClassName(fromId) + "To" + formatClassName(toId);
    }

    public String formatPassedVariable(String id) {
        return id.isEmpty() ? id : WordUtils.uncapitalize(id) + "Passed";
    }

    public String formatVariableName(ValuePath.JsonPath jsonPath) {
        String temp = Arrays.asList(jsonPath.fieldName.split("[-_]")).stream()
                .map(StringUtils::capitalize)
                .reduce(String::concat)
                .get();
        return StringUtils.uncapitalize(temp);
    }

    public String formatViewId(ValuePath valuePath) {
        return formatViewId(formatVariableName(valuePath.lastPath()));
    }

    public String formatPrefsName(String projectName) {
        return StringUtils.capitalize(projectName) + "Preferences";
    }
}
