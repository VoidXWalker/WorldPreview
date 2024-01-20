package me.voidxwalker.worldpreview;

import dev.tildejustin.stateoutput.*;

public class StateOutputInterface {
    public static void outputPreviewing() {
        // ideally it would take the progress of State.GENERATING, but that is inaccessible and always 0 anyway
        StateOutputHelper.outputState(State.PREVIEW.withProgress(0));
    }
}
