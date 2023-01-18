package me.voidxwalker.worldpreview;

import me.voidxwalker.autoreset.Atum;

public class AtumInterface {
    public static void atumReset() {
        if (Atum.isRunning) {
            Atum.hotkeyPressed = false;
            Atum.loopPrevent2 = true;
        }
    }
}
