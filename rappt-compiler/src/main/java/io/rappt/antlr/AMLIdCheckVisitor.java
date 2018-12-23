package io.rappt.antlr;

import io.rappt.AMLParser;
import io.rappt.model.LookupTables;
import io.rappt.model.PIM;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.NotNull;
import io.rappt.AMLBaseVisitor;
import io.rappt.model.AppModel;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

// This class catches errors that prevent construction of the model
public class AMLIdCheckVisitor extends AMLBaseVisitor {

    private AppModel model;

    @Override
    public Object visitVariable(@NotNull AMLParser.VariableContext ctx) {
        Class<? extends ParserRuleContext> parentClass = ctx.getParent().getClass();
        List<Class<? extends PIM>> expectedTypes = LookupTables.antlrClassToReferencedPimClass.get(parentClass).stream().collect(toList());

        try {
            Method method = parentClass.getDeclaredMethod("variable");
            method.setAccessible(true);

            List<AMLParser.VariableContext> vars = new ArrayList<>(0);
            Object result = method.invoke(ctx.getParent());
            if (result instanceof List<?>) {
                vars = (List<AMLParser.VariableContext>) result;
            } else {
                vars.add((AMLParser.VariableContext) result);
            }
            assert expectedTypes.size() >= vars.size() : "Missing rule in LookupTable for " + parentClass.getCanonicalName();

            for (AMLParser.VariableContext var : vars) {
                String id = var.ID().getText();
                String actualClass = model.newIds.get(id);
                PIM pimClass = model.ids.get(id);
                boolean error = !expectedTypes.stream()
                        .anyMatch(eT -> eT.isInstance(pimClass));

                String value = expectedTypes.stream()
                        .map(Class::getCanonicalName)
                        .reduce((str1, str2) -> str1.concat(" " + str2)).get();
                if (error) {
                    model.addError(ctx.getStart(), "No " + value + " with id " + id + " found");
                }
            }
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return super.visitVariable(ctx);
    }

    public AMLIdCheckVisitor(final AppModel model) {
        this.model = model;
    }
}
