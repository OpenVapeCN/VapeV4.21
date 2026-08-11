package gg.vape.module.control;

import gg.vape.Vape;
import gg.vape.module.Mod;
import gg.vape.module.blatant.AutoLadder;
import gg.vape.module.blatant.Clutch;
import gg.vape.module.utility.MLG;
import gg.vape.wrapper.impl.EntityPlayerSP;

public class FallRescuePriorityManager {
    public static final FallRescuePriorityManager INSTANCE = new FallRescuePriorityManager();

    private FallRescuePriorityManager() {
    }

    public boolean shouldStandDown(Mod self, EntityPlayerSP player) {
        if (player == null || player.isNull() || player.b$src$Z$fqlxe4()) {
            return false;
        }
        if (self instanceof AutoLadder) {
            return this.isCoveredBy(Clutch.class, player);
        }
        if (self instanceof MLG) {
            return this.isCoveredBy(Clutch.class, player) || this.isCoveredBy(AutoLadder.class, player);
        }
        return false;
    }

    private boolean isCoveredBy(Class<? extends Mod> moduleClass, EntityPlayerSP player) {
        Mod module = Vape.INSTANCE.getModManager().getMod(moduleClass);
        if (module == null || !module.isEnabled()) {
            return false;
        }
        if (module instanceof Clutch) {
            Clutch clutch = (Clutch)module;
            return clutch.isRescueEngaged() || clutch.canHandleFall(player);
        }
        if (module instanceof AutoLadder) {
            AutoLadder autoLadder = (AutoLadder)module;
            return autoLadder.isRescueEngaged() || autoLadder.canHandleFall(player);
        }
        return false;
    }
}
