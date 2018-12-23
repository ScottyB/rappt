package io.rappt.compiler;

import io.rappt.model.Instruction;

public class NavigationFlowBuilder {

    static public String toActivity(final IntermediateModel interModel, final Instruction.Navigate navigateInstruction) {
        return toActivity(interModel, navigateInstruction.toScreenId);
    }

    static public String toActivity(IntermediateModel interModel, String toScreenId) {
        IntermediateModel.Screen screen = interModel.intermediateScreens.get(toScreenId);
        return screen.rootActivity.isEmpty() ? screen.view.viewControllerName : screen.rootActivity;
    }
}
