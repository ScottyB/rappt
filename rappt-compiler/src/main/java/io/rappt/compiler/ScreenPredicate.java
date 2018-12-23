package io.rappt.compiler;

import io.rappt.model.Screen;
import io.rappt.model.AppModel;

@FunctionalInterface
public interface ScreenPredicate {
    boolean testScreen(final AppModel appModel, final IntermediateModel.Screen interScreen, final Screen screen);
}
