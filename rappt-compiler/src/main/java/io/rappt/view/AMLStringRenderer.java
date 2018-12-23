package io.rappt.view;

import org.stringtemplate.v4.AttributeRenderer;
import org.stringtemplate.v4.STGroup;

import java.util.Locale;

// Usage in StringTemplate: $myStringId;format="amlString"$
public class AMLStringRenderer implements AttributeRenderer {
    public String toString(Object o, String formatString, Locale locale) {
        if ("amlString".equals(formatString)) {
            return '"' + (String)o + '"';
        } else if ("lowercase".equals(formatString)) {
            return ((String)o).toLowerCase();
        }

        // format not recognized
        return o.toString();
    }

    public static void registerWith(STGroup group) {
        group.registerRenderer(String.class, new AMLStringRenderer());
    }
}