package io.rappt.android;

import java.util.ArrayList;
import java.util.Collection;

public class AndroidScreen {


    public String screenId;

    //    public DynamicLayout view;
//    public Controller controller;
    public ViewController viewController = new ViewController();
    public Collection<ViewController> elements = new ArrayList<>();

    // Class to keep track of independent View/Controller elements
    static public class ViewController {
        public DynamicLayout view;
        public Controller controller;

        private ViewController() {
        }

        public ViewController(Controller controller, DynamicLayout view) {
            this.view = view;
            this.controller = controller;
        }
    }

    public AndroidScreen(DynamicLayout view, Controller controller, String id) {
        this.viewController.view = view;
        this.screenId = id;
        this.viewController.controller = controller;
    }

    public Collection<FileTemplate> getTemplates() {
        Collection<FileTemplate> files = new ArrayList<>();
        files.add(viewController.controller);
        files.add(viewController.view);
        for (ViewController e : elements) {
            files.add(e.view);
            files.add(e.controller);
        }
        return files;
    }
}
