package io.rappt.android;

import io.rappt.compiler.IntermediateModel;

import java.util.Collection;

public class AdaptarData extends Template {

    public String dataClass;
    public Field stateField;
    public boolean hasMultipleViews;
    public Collection<IntermediateModel.ListItem> states;

    public AdaptarData(String responseClassName, Collection<IntermediateModel.ListItem> values, Field stateField) {
        super("adapterData");
        this.dataClass = responseClassName;
        this.states = values;
        this.hasMultipleViews = values.size() > 1;
        this.stateField = stateField;
    }

}
