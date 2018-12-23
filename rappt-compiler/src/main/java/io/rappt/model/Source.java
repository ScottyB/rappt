package io.rappt.model;

import java.util.HashMap;
import java.util.Map;

public class Source implements PIM {
    public String id = "";
    public Api api;
    public Resource resource;

    public Map<String, ValuePath> fromBindings = new HashMap<>();
    public Map<String, ValuePath> toBindings = new HashMap<>();

    public Source(String id) {
        this.id = id;
    }
}
