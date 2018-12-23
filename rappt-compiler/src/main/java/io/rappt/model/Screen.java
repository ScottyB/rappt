package io.rappt.model;

import java.util.ArrayList;
import java.util.Collection;

public class Screen implements PIM {

    public View view;
    public Model model;
    public Collection<View.Action> actions = new ArrayList<>();
    public Collection<String> featureIds = new ArrayList<>();
    public String parameter;
    public String id = "";
    public String label = "";

    public boolean enableHomeBack;
    public boolean pullToRefresh;

    public Screen addLabel(String label) {
        this.label = label;
        return this;
    }

    public Screen(String id) {
        this.id = id;
    }

    @Override
    public void accept(PIMVisitor visitor) {
        actions.forEach(a -> a.accept(visitor));
        view.accept(visitor);
        visitor.visit(this);
    }
}
