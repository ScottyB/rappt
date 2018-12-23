package io.rappt.compiler;

import io.rappt.model.AppModel;

public interface ApplyPredicate {
    boolean test(final AppModel app, final IntermediateModel interModel);
}
