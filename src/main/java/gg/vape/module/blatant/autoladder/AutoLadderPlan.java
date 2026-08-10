package gg.vape.module.blatant.autoladder;

import gg.vape.module.utility.clutch.PlacementTarget;
import gg.vape.utils.datas.BlockData;
import gg.vape.wrapper.impl.EnumFacing;
import org.jetbrains.annotations.Nullable;

public final class AutoLadderPlan {
    public enum Mode {
        EXISTING_LADDER,
        DIRECT,
        BUILD_SUPPORT
    }

    private final Mode mode;
    @Nullable
    private final PlacementTarget blockTarget;
    private final PlacementTarget ladderTarget;
    private final double catchX;
    private final double catchZ;
    private final int catchTick;
    private final int blockPlacementTick;
    private final int ladderPlacementTick;
    private final double score;

    public AutoLadderPlan(Mode mode, @Nullable PlacementTarget blockTarget,
                          PlacementTarget ladderTarget, double catchX, double catchZ,
                          int catchTick, int blockPlacementTick,
                          int ladderPlacementTick, double score) {
        this.mode = mode;
        this.blockTarget = blockTarget;
        this.ladderTarget = ladderTarget;
        this.catchX = catchX;
        this.catchZ = catchZ;
        this.catchTick = catchTick;
        this.blockPlacementTick = blockPlacementTick;
        this.ladderPlacementTick = ladderPlacementTick;
        this.score = score;
    }

    public Mode getMode() {
        return this.mode;
    }

    @Nullable
    public PlacementTarget getBlockTarget() {
        return this.blockTarget;
    }

    public PlacementTarget getLadderTarget() {
        return this.ladderTarget;
    }

    public BlockData getSupportBlock() {
        return this.ladderTarget.supportBlock;
    }

    public BlockData getLadderBlock() {
        return this.ladderTarget.getPlacedBlock();
    }

    public EnumFacing getLadderFacing() {
        return this.ladderTarget.facing;
    }

    public double getCatchX() {
        return this.catchX;
    }

    public double getCatchZ() {
        return this.catchZ;
    }

    public int getCatchTick() {
        return this.catchTick;
    }

    public int getBlockPlacementTick() {
        return this.blockPlacementTick;
    }

    public int getLadderPlacementTick() {
        return this.ladderPlacementTick;
    }

    public double getScore() {
        return this.score;
    }

    public String describe() {
        BlockData ladder = this.getLadderBlock();
        StringBuilder description = new StringBuilder(this.mode.name())
                .append(" ladder=").append(ladder)
                .append(" support=").append(this.getSupportBlock())
                .append(" face=").append(this.getLadderFacing().Y())
                .append(" catchPos=[")
                .append(Math.round(this.catchX * 100.0) / 100.0).append(',')
                .append(Math.round(this.catchZ * 100.0) / 100.0).append(']')
                .append(" catchEta=").append(this.catchTick)
                .append(" blockReach=").append(this.blockPlacementTick)
                .append(" ladderReach=").append(this.ladderPlacementTick)
                .append(" score=").append(Math.round(this.score * 10.0) / 10.0);
        if (this.blockTarget != null) {
            description.append(" anchor=").append(this.blockTarget.supportBlock)
                    .append('/').append(this.blockTarget.facing.Y());
        }
        return description.toString();
    }

    public String rejectionKey() {
        BlockData ladder = this.getLadderBlock();
        StringBuilder key = new StringBuilder(this.mode.name())
                .append(':').append(ladder.D())
                .append(':').append(ladder.B())
                .append(':').append(ladder.G())
                .append(':').append(this.getLadderFacing().Y());
        if (this.blockTarget != null) {
            key.append(':').append(this.blockTarget.supportBlock.D())
                    .append(':').append(this.blockTarget.supportBlock.B())
                    .append(':').append(this.blockTarget.supportBlock.G())
                    .append(':').append(this.blockTarget.facing.Y());
        }
        return key.toString();
    }
}
