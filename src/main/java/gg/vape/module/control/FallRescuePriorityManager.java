package gg.vape.module.control;

import gg.vape.Vape;
import gg.vape.module.Mod;
import gg.vape.module.blatant.AutoLadder;
import gg.vape.module.blatant.Clutch;
import gg.vape.module.utility.MLG;
import gg.vape.wrapper.impl.EntityPlayerSP;

public class FallRescuePriorityManager {
    public static final FallRescuePriorityManager INSTANCE = new FallRescuePriorityManager();

    private long lastStateKey = Long.MIN_VALUE;
    private Boolean lastClutchCovered;
    private Boolean lastAutoLadderCovered;

    private FallRescuePriorityManager() {
    }

    public boolean shouldStandDown(Mod self, EntityPlayerSP player) {
        if (player == null || player.isNull() || player.b$src$Z$fqlxe4()) {
            return false;
        }
        if (self instanceof AutoLadder) {
            return this.isCoveredByCached(Clutch.class, player);
        }
        if (self instanceof MLG) {
            return this.isCoveredByCached(Clutch.class, player)
                    || this.isCoveredByCached(AutoLadder.class, player);
        }
        return false;
    }

    private boolean isCoveredByCached(Class<? extends Mod> moduleClass, EntityPlayerSP player) {
        long stateKey = this.computeStateKey(player);
        if (stateKey != this.lastStateKey) {
            this.lastStateKey = stateKey;
            this.lastClutchCovered = null;
            this.lastAutoLadderCovered = null;
        }
        if (moduleClass == Clutch.class) {
            if (this.lastClutchCovered == null) {
                this.lastClutchCovered = this.isCoveredBy(Clutch.class, player);
            }
            return this.lastClutchCovered.booleanValue();
        }
        if (moduleClass == AutoLadder.class) {
            if (this.lastAutoLadderCovered == null) {
                this.lastAutoLadderCovered = this.isCoveredBy(AutoLadder.class, player);
            }
            return this.lastAutoLadderCovered.booleanValue();
        }
        return false;
    }

    private long computeStateKey(EntityPlayerSP player) {
        long hash = 1L;
        hash = hash * 31L + Double.doubleToLongBits(player.z());
        hash = hash * 31L + Double.doubleToLongBits(player.N());
        hash = hash * 31L + Double.doubleToLongBits(player.h());
        hash = hash * 31L + Double.doubleToLongBits(player.t());
        hash = hash * 31L + Double.doubleToLongBits(player.q());
        hash = hash * 31L + Double.doubleToLongBits(player.T());
        hash = hash * 31L + Float.floatToIntBits(player.getFallDistance());
        return hash;
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
