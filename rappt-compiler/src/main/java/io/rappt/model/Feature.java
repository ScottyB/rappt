package io.rappt.model;


import java.util.ArrayList;
import java.util.Collection;

public class Feature implements PIM {
    public String id;
    public Collection<View.Action> actions = new ArrayList<>();

    public Feature() {

    }

    public Feature(String id) {
        this.id = id;
    }

    @Override
    public void accept(PIMVisitor visitor) {
        actions.forEach(i -> i.accept(visitor));
        visitor.visit(this);
    }
}
