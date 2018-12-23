package io.rappt.compiler;

import io.rappt.model.Screen;
import io.rappt.android.AndroidModel;
import io.rappt.android.AndroidScreen;
import io.rappt.model.AppModel;

@FunctionalInterface
public interface UpdateScreen {
    AndroidScreen addFunctionality(final AppModel appModel, final IntermediateModel interModel, final AndroidModel model, final AndroidScreen androidScreen, final Screen screen);
}
