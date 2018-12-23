package io.rappt.model;

import java.util.ArrayList;
import java.util.List;

public class UICollection extends UIBase {

    public Event.OnItemClick onItemClick = new Event.OnItemClick();

    public static class ListItem {
        public View layout;
    }

    public List<ListItem> listItemLayouts = new ArrayList<>();

    public UICollection(String id) {
        super(id);
    }

    @Override
    public void accept(PIMVisitor visitor) {
        super.accept(visitor);
        onItemClick.accept(visitor);
        listItemLayouts.forEach(l -> {
            if (l.layout != null) l.layout.accept(visitor);
        });
    }
}
