package io.rappt.model;

import io.rappt.compiler.Compiler;
import org.apache.commons.lang.StringUtils;
import io.rappt.view.AMLStringRenderer;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import java.util.logging.Level;
import java.util.logging.Logger;

public interface PIM {
    Logger LOGGER = Logger.getLogger(PIM.class.getName());

    default void accept(PIMVisitor visitor) {
        visitor.visit(this);
    }

    // Renders this object as AML.
    default String getAml() throws Compiler.StringTemplateException {
        STGroup group = new STGroupFile("templates/aml/appModel.stg", '$', '$');
        AMLStringRenderer.registerWith(group);
        String templateName = getAmlTemplateName();

        ST st = group.getInstanceOf(templateName);

        if (st == null) {
            String error = "Could not find template: " + templateName;
            Compiler.StringTemplateException e = new Compiler.StringTemplateException(error);
            LOGGER.log(Level.SEVERE, error, e);
            throw e;
        }

        // Inject this object into the template
        String parameter = getAmlTemplateParameterName();
        st.add(parameter, this);

        // TODO: Listen for errors
        String aml = st.render();

        return aml;
    }

    // StringTemplate parameter name to inject this object into
    default String getAmlTemplateParameterName() {
        // example: "appModel" parameter "AppModel(appModel) ::= << ... >>"
        return StringUtils.uncapitalize(this.getClass().getSimpleName());
    }

    // StringTemplate instance to use for rendering this object as AML
    default String getAmlTemplateName() {
        // example: "AppModel" template "AppModel(appModel) ::= << ... >>"
        return this.getClass().getSimpleName();
    }
}

interface MarkerPim extends PIM {
}

interface ListPim extends PIM {
}

interface Data extends PIM {
}

interface Preference extends PIM {
}

