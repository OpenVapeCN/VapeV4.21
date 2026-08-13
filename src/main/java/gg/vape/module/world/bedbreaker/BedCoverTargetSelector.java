package gg.vape.module.world.bedbreaker;

import gg.vape.utils.BlockUtil;
import gg.vape.wrapper.impl.Block;
import gg.vape.wrapper.impl.BlockBed;
import gg.vape.wrapper.impl.BlockPos;
import gg.vape.wrapper.impl.BlockState;
import gg.vape.wrapper.impl.EntityPlayerSP;
import gg.vape.wrapper.impl.EnumFacing;
import gg.vape.wrapper.impl.InventoryPlayer;
import gg.vape.wrapper.impl.ItemStack;
import gg.vape.wrapper.impl.WorldClient;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class BedCoverTargetSelector {
    private BedCoverTargetSelector() {
    }

    public static BedCoverTarget select(WorldClient world, EntityPlayerSP player,
                                        BedTargetRenderPosition bedPosition) {
        int bedX = bedPosition.getBlockX();
        int bedY = bedPosition.getBlockY();
        int bedZ = bedPosition.getBlockZ();
        Block bedBlock = world.getBlockByPos(bedX, bedY, bedZ);
        if (!BlockUtil.f(bedBlock)) {
            return null;
        }
        BlockBed bed = new BlockBed(bedBlock);
        EnumFacing facing = bed.getBedDirection(world, bedX, bedY, bedZ);
        List<BlockPos> coverPositions = getFirstLayerPositions(bedX, bedY, bedZ, facing.Y());
        for (BlockPos coverPosition : coverPositions) {
            Block coverBlock = world.getBlockByPos(
                    coverPosition.getX(), coverPosition.getY(), coverPosition.getZ());
            if (BlockUtil.p(coverBlock)) {
                return null;
            }
        }

        InventoryPlayer inventory = player.V$src$Lgg_vape_wrapper_impl_InventoryPlayer_$erqak6();
        int originalSlot = inventory.v();
        BlockPos bestPosition = null;
        ItemStack bestToolStack = null;
        int bestToolSlot = -1;
        float bestProgress = -1.0f;
        try {
            for (BlockPos coverPosition : coverPositions) {
                BlockState coverState = world.getBlockState(coverPosition);
                for (int slot = 0; slot < 9; ++slot) {
                    inventory.g(slot);
                    float progress = coverState.getPlayerRelativeDestroyProgress(player, world, coverPosition);
                    if (!(progress > bestProgress)) continue;
                    bestProgress = progress;
                    bestPosition = coverPosition;
                    bestToolSlot = slot;
                    ItemStack stack = inventory.c(slot);
                    bestToolStack = stack.isNull() ? null : stack.k();
                }
            }
        }
        finally {
            inventory.g(originalSlot);
        }
        return bestPosition == null ? null
                : new BedCoverTarget(bestPosition, bestToolStack, bestToolSlot);
    }

    private static List<BlockPos> getFirstLayerPositions(int bedX, int bedY, int bedZ, int facingIndex) {
        int otherBedX = bedX;
        int otherBedZ = bedZ;
        if (facingIndex == 2) {
            ++otherBedZ;
        } else if (facingIndex == 3) {
            --otherBedZ;
        } else if (facingIndex == 4) {
            ++otherBedX;
        } else if (facingIndex == 5) {
            --otherBedX;
        }
        Set<BlockPos> positions = new LinkedHashSet<BlockPos>();
        addSurroundingPositions(positions, bedX, bedY, bedZ);
        addSurroundingPositions(positions, otherBedX, bedY, otherBedZ);
        positions.remove(BlockPos.create(bedX, bedY, bedZ));
        positions.remove(BlockPos.create(otherBedX, bedY, otherBedZ));
        return new ArrayList<BlockPos>(positions);
    }

    private static void addSurroundingPositions(Set<BlockPos> positions, int x, int y, int z) {
        positions.add(BlockPos.create(x, y + 1, z));
        positions.add(BlockPos.create(x - 1, y, z));
        positions.add(BlockPos.create(x + 1, y, z));
        positions.add(BlockPos.create(x, y, z - 1));
        positions.add(BlockPos.create(x, y, z + 1));
    }
}
