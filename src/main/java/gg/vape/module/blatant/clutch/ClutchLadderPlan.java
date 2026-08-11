package gg.vape.module.blatant.clutch;

import gg.vape.module.blatant.blockin.BlockPlacementPathSegment;
import gg.vape.module.blatant.blockin.BlockPlacementPathSegmentState;
import gg.vape.module.utility.clutch.PlacementTarget;
import gg.vape.utils.datas.BlockCoordinate;
import gg.vape.utils.datas.BlockData;
import gg.vape.wrapper.impl.EnumFacing;
import java.util.ArrayList;
import java.util.Vector;

public final class ClutchLadderPlan {
    private final BlockPlacementPathSegment pathSegment;
    private final BlockData ladderBlock;
    private final EnumFacing ladderFacing;
    private final double catchX;
    private final double catchZ;
    private final int catchTick;
    private final double score;

    public ClutchLadderPlan(Vector<PlacementTarget> targets, BlockData ladderBlock,
                            EnumFacing ladderFacing, double catchX, double catchZ,
                            int catchTick, double score) {
        this.ladderBlock = ladderBlock;
        this.ladderFacing = ladderFacing;
        this.catchX = catchX;
        this.catchZ = catchZ;
        this.catchTick = catchTick;
        this.score = score;
        BlockPlacementPathSegmentState state = new BlockPlacementPathSegmentState(ladderFacing, targets);
        this.pathSegment = new BlockPlacementPathSegment(
                new BlockCoordinate(ladderBlock.D(), ladderBlock.B() - 1, ladderBlock.G()),
                new BlockCoordinate(ladderBlock.D(), ladderBlock.B(), ladderBlock.G()),
                new ArrayList<>());
        this.pathSegment.placementState = state;
    }

    public BlockPlacementPathSegment getPathSegment() {
        return this.pathSegment;
    }

    public BlockData getLadderBlock() {
        return this.ladderBlock;
    }

    public EnumFacing getLadderFacing() {
        return this.ladderFacing;
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

    public double getScore() {
        return this.score;
    }

    public int getPendingPlacementCount() {
        return this.pathSegment.getPendingPlacementCount();
    }
}
