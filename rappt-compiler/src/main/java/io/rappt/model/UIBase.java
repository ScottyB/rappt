package io.rappt.model;

public class UIBase implements PIM {
    public String id;
    public Event.OnTouch onTouch = new Event.OnTouch();
    public Event.OnLoad onLoad = new Event.OnLoad();

    UIBase(String id) {
        this.id = id;
    }

    @Override
    public void accept(PIMVisitor visitor) {
        onTouch.accept(visitor);
        onLoad.accept(visitor);
    }
}