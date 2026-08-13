package gg.vape.module.world;

import func.skidline.RectData;
import gg.vape.event.EventHandler;
import gg.vape.event.impl.EventBedBreakerUpdate;
import gg.vape.event.impl.EventRender3D;
import gg.vape.module.Category;
import gg.vape.module.Mod;
import gg.vape.module.control.SharedModuleControlClaims;
import gg.vape.module.world.bedbreaker.BedBreakPhase;
import gg.vape.module.world.bedbreaker.BedCoverTarget;
import gg.vape.module.world.bedbreaker.BedCoverTargetSelector;
import gg.vape.module.world.bedbreaker.BedTargetRenderPosition;
import gg.vape.module.world.bedbreaker.BedTargetRenderState;
import gg.vape.unmap.ModeOption;
import gg.vape.unmap.ModeSelection;
import gg.vape.utils.BlockUtil;
import gg.vape.utils.MathUtil;
import gg.vape.utils.PlayerSimulationUtil;
import gg.vape.utils.RotationUtil;
import gg.vape.utils.Vec3d;
import gg.vape.utils.render.GuiRenderPrimitives;
import gg.vape.utils.render.OpenGlBackendHolder;
import gg.vape.utils.render.RenderUtils;
import gg.vape.value.ModeValue;
import gg.vape.wrapper.impl.AxisAlignedBB;
import gg.vape.wrapper.impl.Block;
import gg.vape.wrapper.impl.BlockBed;
import gg.vape.wrapper.impl.BlockPos;
import gg.vape.wrapper.impl.EntityOtherPlayerMP;
import gg.vape.wrapper.impl.EntityPlayerSP;
import gg.vape.wrapper.impl.EnumFacing;
import gg.vape.wrapper.impl.Minecraft;
import gg.vape.wrapper.impl.RayTraceResult;
import gg.vape.wrapper.impl.RayTraceResult_type;
import gg.vape.wrapper.impl.Vec3;
import gg.vape.wrapper.impl.World;
import gg.vape.wrapper.impl.WorldClient;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

public class BedBreaker
extends Mod {
    private World lastWorld;
    private BedTargetRenderState selectedTarget;
    private BedTargetRenderState lastProgressTarget;
    private static final long MODULE_ID = -5914606721811702784L;
    private final ModeOption normalMode = new ModeOption("Normal");
    private final ModeOption hypixelMode = new ModeOption("Hypixel");
    private final ModeValue mode;
    private final List<BedTargetRenderPosition> targets = new CopyOnWriteArrayList<BedTargetRenderPosition>();
    private final HashMap<BedTargetRenderPosition, BedTargetRenderState> renderStates = new HashMap();

    @Override
    public void onScheduledAction() {
        EntityPlayerSP entityPlayerSP = Minecraft.thePlayer();
        WorldClient worldClient = Minecraft.theWorld();
        if (worldClient.isNull() || !worldClient.equals(this.lastWorld)) {
            this.targets.clear();
        }
        int radius = 100;
        for (int dx = -radius; dx < radius; ++dx) {
            for (int dz = -radius; dz < radius; ++dz) {
                int direction = 0;
                while (direction != -1) {
                    direction = direction == 0 ? 1 : -1;
                    for (int step = 0; step < 20; ++step) {
                        for (BedTargetRenderPosition bedTargetRenderPosition : this.targets) {
                            if (!this.renderStates.containsKey(bedTargetRenderPosition)) continue;
                            this.renderStates.get(bedTargetRenderPosition).updateVisibilityAnimation();
                        }
                        if (entityPlayerSP.isNull() || worldClient.isNull()) {
                            return;
                        }
                        int dy = step * direction;
                        int blockX = (int)entityPlayerSP.z() + dx;
                        int blockY = (int)entityPlayerSP.N() + dy;
                        int blockZ = (int)entityPlayerSP.h() + dz;
                        Block block = worldClient.getBlockByPos(blockX, blockY, blockZ);
                        int blockId = Block.R(block);
                        String blockName = block.U();
                        if (blockId != 26 && (blockName == null || !blockName.matches("block.minecraft.(.+_bed)"))) continue;
                        BedTargetRenderPosition bedTargetRenderPosition = new BedTargetRenderPosition(blockX, blockY, blockZ);
                        BlockBed blockBed = new BlockBed(block);
                        boolean isFoot = blockBed.isFoot(worldClient, blockX, blockY, blockZ);
                        if (this.targets.contains(bedTargetRenderPosition) || isFoot) continue;
                        this.targets.add(bedTargetRenderPosition);
                    }
                }
            }
        }
        this.lastWorld = worldClient;
    }

    public BedBreaker() {
        super("BedBreaker", (int)MODULE_ID, Category.WORLD, "Allows you to break beds through walls\n\u00a7cWarning: This behavior is normally impossible and may be detected on servers");
        this.mode = ModeValue.create((Object)this, "Mode", (ModeSelection)this.normalMode,
                this.normalMode, this.hypixelMode);
        this.addValue(this.mode);
        this.mode.addChangeListener(this::onModeChanged);
    }

    @EventHandler
    public void onRender3D(EventRender3D eventRender3D) {
        WorldClient worldClient = Minecraft.theWorld();
        EntityPlayerSP player = Minecraft.thePlayer();
        if (worldClient.isNull() || player.isNull()) {
            this.clearTargetingState();
            return;
        }
        if (this.lastWorld != null && !worldClient.equals(this.lastWorld)) {
            this.targets.clear();
            this.resetRenderStates();
            this.clearTargetingState();
            return;
        }
        ArrayList<BedTargetRenderState> activeRenderStates = new ArrayList<BedTargetRenderState>();
        for (BedTargetRenderPosition bedTargetRenderPosition : this.targets) {
            boolean isFoot;
            int blockZ;
            int blockY;
            int blockX = bedTargetRenderPosition.getBlockX();
            Block block = worldClient.getBlockByPos(blockX, blockY = bedTargetRenderPosition.getBlockY(), blockZ = bedTargetRenderPosition.getBlockZ());
            int blockId = Block.R(block);
            BlockBed blockBed = new BlockBed(block);
            if (blockId != 26 && !block.U().matches("block.minecraft.(.+_bed)") || (isFoot = blockBed.isFoot(worldClient, blockX, blockY, blockZ))) continue;
            BedTargetRenderState renderState;
            if (this.renderStates.containsKey(bedTargetRenderPosition)) {
                renderState = this.renderStates.get(bedTargetRenderPosition);
            } else {
                renderState = new BedTargetRenderState(bedTargetRenderPosition);
                this.renderStates.put(bedTargetRenderPosition, renderState);
            }
            renderState.updateProjectedBounds();
            if (this.isHypixelMode()) {
                this.updateHypixelState(worldClient, player, renderState);
            }
            activeRenderStates.add(renderState);
        }
        OpenGlBackendHolder.backend.pushMatrix();
        GuiRenderPrimitives.Y();
        RenderUtils.g();
        OpenGlBackendHolder.backend.pushMatrix();
        OpenGlBackendHolder.backend.scale(0.5f, 0.5f, 0.5f);
        double reticleSize = 20.0;
        RectData rectData = new RectData((double)(Minecraft.J() / 2) - reticleSize / 2.0, (double)(Minecraft.h() / 2) - reticleSize / 2.0, reticleSize, reticleSize);
        for (BedTargetRenderState bedTargetRenderState : activeRenderStates) {
            boolean selected = this.selectedTarget == bedTargetRenderState;
            float breakProgress = selected && this.lastProgressTarget == bedTargetRenderState
                    ? Minecraft.playerController().c() : 0.0f;
            BedCoverTarget coverTarget = this.isHypixelMode() ? bedTargetRenderState.getCoverTarget() : null;
            bedTargetRenderState.renderIndicator(rectData, selected, breakProgress,
                    coverTarget == null ? null : coverTarget.getToolStack());
        }
        BedTargetRenderState selectedState = null;
        for (BedTargetRenderState candidateState : activeRenderStates) {
            if (!candidateState.isInsideReticle()) continue;
            selectedState = candidateState;
        }
        this.selectedTarget = selectedState;
        this.lastProgressTarget = selectedState;
        OpenGlBackendHolder.backend.popMatrix();
        RenderUtils.f();
        GuiRenderPrimitives.D();
        OpenGlBackendHolder.backend.popMatrix();
    }

    @EventHandler
    public void onBedBreakerUpdate(EventBedBreakerUpdate eventBedBreakerUpdate) {
        if (this.selectedTarget == null) {
            SharedModuleControlClaims.mouseOverUpdate.clearClaimed();
            return;
        }
        WorldClient world = Minecraft.theWorld();
        EntityPlayerSP player = Minecraft.thePlayer();
        if (world.isNull() || player.isNull() || !this.isBed(this.selectedTarget, world)) {
            this.clearTargetingState();
            return;
        }
        if (this.isHypixelMode()) {
            this.updateHypixelState(world, player, this.selectedTarget);
        }
        BedCoverTarget coverTarget = this.isHypixelMode() ? this.selectedTarget.getCoverTarget() : null;
        BlockPos targetPosition = coverTarget == null
                ? this.createBedPosition(this.selectedTarget) : coverTarget.getBlockPosition();
        this.updateMouseOver(this.selectedTarget, targetPosition);
    }

    private void updateMouseOver(BedTargetRenderState renderState, BlockPos blockPos) {
        int blockX = blockPos.getX();
        int blockY = blockPos.getY();
        int blockZ = blockPos.getZ();
        AxisAlignedBB axisAlignedBB = AxisAlignedBB.create(blockX, blockY, blockZ, blockX + 1, blockY + 1, blockZ + 1);
        EnumFacing enumFacing = null;
        EntityOtherPlayerMP entityOtherPlayerMP = PlayerSimulationUtil.y();
        if (entityOtherPlayerMP.R$src$Lgg_vape_wrapper_impl_AxisAlignedBB_$r19dfl().getMinY() > (double)blockY) {
            enumFacing = EnumFacing.T(1);
        } else {
            entityOtherPlayerMP.u((double)blockY + 0.5 + MathUtil.randomRange(new Random(), -0.2, 0.2));
        }
        Vec3d vec3d = RotationUtil.T(entityOtherPlayerMP, axisAlignedBB, 0.0, 0.0, 0.0);
        if (enumFacing == null) {
            double bestDist = 10.0;
            for (EnumFacing enumFacing2 : EnumFacing.t()) {
                BlockPos blockPos2 = blockPos.offset(enumFacing2);
                if (enumFacing2.Y() <= 1) continue;
                double diffX = (double)blockPos2.getX() + 0.5 - vec3d.getX();
                double diffZ = (double)blockPos2.getZ() + 0.5 - vec3d.getZ();
                double dist = Math.abs(diffX) + Math.abs(diffZ);
                if (!(dist < bestDist)) continue;
                bestDist = dist;
                enumFacing = enumFacing2;
            }
        }
        renderState.setObstructionPoint(vec3d);
        Vec3 vec3 = Minecraft.F().O(1.0f);
        double eyeDist = vec3.distanceTo(vec3d.toVec3());
        if (eyeDist < 4.5) {
            RayTraceResult rayTraceResult = RayTraceResult.create(RayTraceResult_type.block(), vec3d.toVec3(), enumFacing, blockPos);
            Minecraft.O(rayTraceResult);
            SharedModuleControlClaims.mouseOverUpdate.setClaimed(true);
        } else {
            SharedModuleControlClaims.mouseOverUpdate.clearClaimed();
        }
    }

    @Override
    public void onEnable() {
        this.clearTargetingState();
        this.v(50L, true);
    }

    @Override
    public void onDisable() {
        this.clearTargetingState();
    }

    @Override
    public String getSimpleSuffix() {
        return this.mode.getDisplayValue();
    }

    private void onModeChanged(ModeValue changedMode) {
        this.resetRenderStates();
        this.clearTargetingState();
    }

    private boolean isHypixelMode() {
        return ((ModeSelection)this.mode.getValue()).equals(this.hypixelMode);
    }

    private void updateHypixelState(WorldClient world, EntityPlayerSP player,
                                    BedTargetRenderState renderState) {
        if (renderState.getBreakPhase() == BedBreakPhase.UNRESOLVED) {
            renderState.resolveCoverTarget(BedCoverTargetSelector.select(
                    world, player, renderState.getTargetPosition()));
        }
        BedCoverTarget coverTarget = renderState.getCoverTarget();
        if (coverTarget == null) {
            return;
        }
        BlockPos coverPosition = coverTarget.getBlockPosition();
        Block coverBlock = world.getBlockByPos(
                coverPosition.getX(), coverPosition.getY(), coverPosition.getZ());
        if (!BlockUtil.p(coverBlock)) {
            return;
        }
        renderState.finishCover();
        this.lastProgressTarget = null;
    }

    private boolean isBed(BedTargetRenderState renderState, WorldClient world) {
        BedTargetRenderPosition position = renderState.getTargetPosition();
        return BlockUtil.f(world.getBlockByPos(
                position.getBlockX(), position.getBlockY(), position.getBlockZ()));
    }

    private BlockPos createBedPosition(BedTargetRenderState renderState) {
        BedTargetRenderPosition position = renderState.getTargetPosition();
        return BlockPos.create(position.getBlockX(), position.getBlockY(), position.getBlockZ());
    }

    private void resetRenderStates() {
        for (BedTargetRenderState renderState : this.renderStates.values()) {
            renderState.resetBreakState();
        }
    }

    private void clearTargetingState() {
        if (this.selectedTarget != null) {
            this.selectedTarget.setObstructionPoint(null);
        }
        this.selectedTarget = null;
        this.lastProgressTarget = null;
        SharedModuleControlClaims.mouseOverUpdate.clearClaimed();
    }
}
