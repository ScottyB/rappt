package io.rappt.model;

import java.util.ArrayList;
import java.util.Collection;

public class Navigation extends Feature {

    public Collection<Tab> tabs = new ArrayList<>();
    public NAVIGATION navigationMethod;
    public boolean disableSwipe;
    public boolean enableSubHeader = false;

    public enum NAVIGATION {
        TABBAR, DRAWER
    }

    static public class Tab implements PIM {
        public String id;
        public String text;
        public Instruction.Navigate to;
    }

    public Navigation() {
        super();
    }

    public Navigation(String id) {
        super(id);
    }

    // For StringTemplate
    public String getNavigationMethod() {
        return navigationMethod.toString();
    }
}