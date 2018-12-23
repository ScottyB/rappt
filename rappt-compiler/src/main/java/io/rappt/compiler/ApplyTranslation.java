package io.rappt.compiler;

import io.rappt.android.AndroidModel;
import io.rappt.model.AppModel;

@FunctionalInterface
public interface ApplyTranslation {

    public AndroidModel translation(final AppModel appModel, final IntermediateModel interModel, final AndroidModel androidModel);
}
