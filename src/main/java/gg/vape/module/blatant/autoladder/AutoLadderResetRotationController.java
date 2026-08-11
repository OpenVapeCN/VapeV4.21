package gg.vape.module.blatant.autoladder;

import gg.vape.module.blatant.AutoLadder;
import gg.vape.rotation.ThresholdFixedRotationController;
import gg.vape.wrapper.impl.EntityPlayerSP;

public final class AutoLadderResetRotationController extends ThresholdFixedRotationController {
    private final AutoLadder autoLadder;

    public AutoLadderResetRotationController(AutoLadder autoLadder, EntityPlayerSP player,
                                             float yawDelta, float pitchDelta) {
        super(player, yawDelta, pitchDelta);
        this.autoLadder = autoLadder;
    }

    @Override
    public void setComplete(boolean complete) {
        super.setComplete(complete);
        if (complete) {
            this.autoLadder.onRotationResetComplete(this);
        }
    }
}
