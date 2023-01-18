package me.voidxwalker.worldpreview;

import me.voidxwalker.autoreset.Atum;

public class AtumInterface {
    public void atumReset() {
        if (Atum.isRunning) {
//            Atum.isRunning = true;
            Atum.hotkeyPressed = false;
            Atum.loopPrevent2 = true;
        }
    }
}
