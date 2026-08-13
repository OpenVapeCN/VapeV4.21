package gg.vape.module.world.bedbreaker;

import gg.vape.wrapper.impl.BlockPos;
import gg.vape.wrapper.impl.ItemStack;

public class BedCoverTarget {
    private final BlockPos blockPosition;
    private final ItemStack toolStack;
    private final int toolSlot;

    public BedCoverTarget(BlockPos blockPosition, ItemStack toolStack, int toolSlot) {
        this.blockPosition = blockPosition;
        this.toolStack = toolStack;
        this.toolSlot = toolSlot;
    }

    public BlockPos getBlockPosition() {
        return this.blockPosition;
    }

    public ItemStack getToolStack() {
        return this.toolStack;
    }

    public int getToolSlot() {
        return this.toolSlot;
    }
}
