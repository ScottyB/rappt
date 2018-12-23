package io.rappt.layout;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

public class ViewElement {

    final public String id;

    private Map<String, String> attributesToAdd = new HashMap<>();

    ViewElement(String id, int paddingMultiplier) {
        this.id = id;
        if (paddingMultiplier > 0) {
            int padding = paddingMultiplier * 15;
            attributesToAdd.put("android:paddingTop", padding + "dp");
        }
    }

    @Override
    public String toString() {
        return id;
    }

    public void addAttrLayoutBelow(String id) {
        attributesToAdd.put("android:layout_below", droidId(id));
    }

    public void addWidthWeight(double weight) {
        String newWeight = new DecimalFormat("#.##").format(weight);
        attributesToAdd.put("android:layout_weight", newWeight);
    }

    public Map<String, String> getAttrs() {
        return attributesToAdd;
    }

    public void addAlignRight() {
        attributesToAdd.put("android:layout_alignParentRight", "true");
    }

    public void addAlignLeftTo(String id) {
        attributesToAdd.put("android:layout_toLeftOf", droidId(id));
    }

    static String droidId(String id) {
        if (!id.startsWith("@+id/"))
            id = "@+id/" + id;
        return id;
    }

    public void addLinearCenterContent() {
        attributesToAdd.put("android:layout_gravity", "center");
    }

    public void addRelativeCenterContent(String id) {
        addRelativeCenter();
        attributesToAdd.put("android:layout_alignBottom", droidId(id));
    }

    // TODO: Utils class overrides image parameters
    public void addRelativeCenter() {
        attributesToAdd.put("android:gravity", "center");
        attributesToAdd.put("android:layout_width", "match_parent");
    }

    public void addAlignLeft() {
        attributesToAdd.put("android:layout_alignParentLeft", "true");
    }

    public void addAlignRightTo(String id) {
        attributesToAdd.put("android:layout_toRightOf", droidId(id));
    }
}
