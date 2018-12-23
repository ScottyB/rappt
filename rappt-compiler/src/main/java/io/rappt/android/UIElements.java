package io.rappt.android;

class AndroidView extends Template {
    public String id;
    public String styleName;

    public AndroidView(String templateName, String id) {
        super(templateName);
        this.id = id;
    }
}

public class UIElements {

    static public class TextView extends AndroidView {
        public String textId;

        public TextView(String id, String textId) {
            super("textView", id);
            this.textId = textId;
        }
    }

    static public class EditText extends AndroidView {
        public String hintTextId;
        public boolean isPassword;

        public EditText(String id, String hintTextId, boolean isPassword) {
            super("editText", id);
            this.hintTextId = hintTextId;
            this.isPassword = isPassword;
        }
    }

    static public class ImageView extends AndroidView {
        public String drawable;

        public ImageView(String id) {
            super("imageView", id);
        }
    }

    static public class Button extends AndroidView {
        @STIgnore
        public static final String DEFAULT_BUTTON_STYLE = "Widget.Button";

        public String textId;

        public Button(String id, String textId) {
            super("button", id);
            this.styleName = DEFAULT_BUTTON_STYLE;
            this.textId = textId;
        }
    }

    static public class WebView extends AndroidView {

        public WebView(String id) {
            super("webView", id);
        }

    }

    static public class ListView extends AndroidView {
        public boolean hasOnItemEvent;

        public ListView(String id) {
            super("listView", id);
        }
    }

    static public class Map extends AndroidView {
        public Map(String id) {
            super("map", id);
        }
    }
}
