package io.rappt.antlr;

import org.antlr.v4.runtime.tree.TerminalNode;
import io.rappt.AMLParser;
import io.rappt.model.AppModel;
import io.rappt.model.ValuePath;
import io.rappt.model.View;

import java.util.ArrayList;
import java.util.List;

public class AppModelUtils {

    static public String formatSTRING(TerminalNode node) {
        String string = node.getText();
        return string.substring(1, string.length() - 1);
    }

    // TODO: Add to parser in some manner?
    static public String formatType(TerminalNode node, boolean isLast) {
        String type = isLast ? "STRING" : "OBJECT";
        if (node != null) {
            type = node.getText().substring(1).toUpperCase();
        }
        return type;
    }

    static public View getView(final AppModel appModel, final AMLParser.UiBlockContext ctx) {
        String id = ctx.variableDeclaration().ID().getText();
        View view = (View) appModel.ids.get(id);
        return view;
    }

    static public ValuePath buildField(AMLParser.FieldContext f) {
        List<ValuePath.JsonPath> path = new ArrayList<>();
        for (int i = 0; i < f.fieldValue().size(); i++) {
            String tempId = f.fieldValue(i).ID().getText();
            boolean isLast = i == f.fieldValue().size() - 1;
            String type = AppModelUtils.formatType(f.fieldValue(i).FIELD_TYPE(), isLast);
            path.add(new ValuePath.JsonPath(tempId, type));
        }
        return new ValuePath(path);
    }


}
