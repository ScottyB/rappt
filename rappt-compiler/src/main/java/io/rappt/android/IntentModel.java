package io.rappt.android;

import io.rappt.compiler.AndroidTranslatorUtils;
import io.rappt.model.Instruction;
import io.rappt.compiler.IntermediateModel;

import java.util.ArrayList;
import java.util.Collection;

public class IntentModel extends Template {

    public Template template;
    public boolean removeFromStack = false;

    // Passed to Template
    public boolean isFromFragment;

    public IntentModel() {
        super("intent");
    }


    static public class To extends Template {
        public String toClassName;
        public Collection<String> parameters = new ArrayList<>();
        public Field fieldParameters;
        public boolean isFromViewController; // TODO: may need to move this to IntentModel
        public boolean hasEvent;
        public String eventClassName;
        public boolean isFragmentToFragment;


        public To(String toClassName) {
            super("toIntent");
            this.toClassName = toClassName;
        }
    }

    static public class Url extends Template {
        public String stringId;

        public Url(String stringId) {
            super("url");
            this.stringId = stringId;
        }
    }

    static public IntentModel buildNavigationIntent(boolean doesCloseActivity, boolean isFromFragment, Instruction i, IntermediateModel.Instruction instr, boolean isFromView, boolean isFragmentToFragment) {
        IntentModel intent;
        Instruction.Navigate nav = (Instruction.Navigate) i;
        IntermediateModel.Navigate navigate = (IntermediateModel.Navigate) instr;
        String toClass = navigate.toClassName;
        Field field = new Field();
        if (navigate.parameter != null)
            field = AndroidTranslatorUtils.buildField(AndroidModel.DATA_VARIABLE, "", navigate.parameter);
        intent = IntentModel.createToIntent(isFromFragment, toClass, navigate.formatedParameters, field, isFromView, isFragmentToFragment);
        intent.removeFromStack = doesCloseActivity;
        return intent;
    }

    static public IntentModel buildUrlIntent(boolean doesCloseActivity, boolean isFromFragment, IntermediateModel.Instruction instr) {
        IntentModel intent;
        IntermediateModel.Url url = (IntermediateModel.Url) instr;
        intent = IntentModel.createToUrl(isFromFragment, url.urlStringId);
        intent.removeFromStack = doesCloseActivity;
        return intent;
    }


    static private IntentModel createToIntent(boolean isFromFragment, String className, Collection<String> params, Field field, boolean isFromView, boolean isFragmentToFragment) {
        IntentModel intentModel = new IntentModel();
        To to = new To(className);
        to.parameters = params;
        to.fieldParameters = field;
        to.isFromViewController = isFromView;
        to.isFragmentToFragment = isFragmentToFragment;
        intentModel.template = to;
        intentModel.isFromFragment = isFromFragment;
        return intentModel;
    }

    static private IntentModel createToUrl(boolean isFromFragment, String urlStringId) {
        IntentModel intentModel = new IntentModel();
        intentModel.template = new Url(urlStringId);
        intentModel.isFromFragment = isFromFragment;
        return intentModel;
    }

    public boolean isFragment;   // Intent triggered from a fragment
}
