package io.rappt.model;

import io.rappt.compiler.FormatUtils;
import io.rappt.compiler.IntermediateModel;

import java.util.ArrayList;
import java.util.Collection;

public class Event implements PIM {

    public Collection<Instruction> instructions = new ArrayList<>();
    public boolean doesCloseActivity;

    @Override
    public void accept(PIMVisitor visitor) {
        this.instructions.forEach(i -> i.accept(visitor));
    }

    static public class OnTouch extends Event {
    }

    static public class OnItemClick extends Event {
    }

    static public class OnLoad extends Event {
    }

    static public class OnMapClick extends Event {
    }

    public IntermediateModel.Event transformComponent(final FormatUtils utils, final AppModel appModel, final IntermediateModel model) {
        IntermediateModel.Event interEvent = new IntermediateModel.Event(appModel);
        boolean isPreCall = true;
        for (Instruction i : this.instructions) {
            IntermediateModel.Instruction instruction;
            if (i instanceof Instruction.Call) {
                isPreCall = false;
                interEvent.callInstruction = (Instruction.Call) i;
            } else if (isPreCall) {
                interEvent.instructions.add(i);
            } else {
                interEvent.postCall.add(i);
            }
            instruction = i.transformComponent(utils, appModel, model);
            interEvent.allInstructions.put(i.id, instruction);
        }
        return interEvent;
    }

    public IntermediateModel transformModel(final FormatUtils utils, final IntermediateModel model) {
        IntermediateModel newModel = model;
        for (Instruction i : this.instructions) {
            newModel = i.transformModel(utils, model);
        }
        return newModel;
    }
}
