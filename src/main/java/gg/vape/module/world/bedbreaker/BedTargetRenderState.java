package gg.vape.module.world.bedbreaker;

import func.skidline.RectData;
import gg.vape.module.render.entity.ProjectedEntityBounds;
import gg.vape.module.world.bedbreaker.BedTargetRenderPosition;
import gg.vape.utils.TimerUtil;
import gg.vape.utils.Vec3d;
import gg.vape.utils.render.GuiRenderPrimitives;
import gg.vape.utils.render.ItemIconRenderer;
import gg.vape.utils.render.RenderUtil;
import gg.vape.wrapper.impl.AxisAlignedBB;
import gg.vape.wrapper.impl.ItemStack;
import gg.vape.wrapper.impl.Minecraft;
import gg.vape.wrapper.impl.RenderWorldLastEvent;
import gg.vape.wrapper.impl.Vec3;
import java.awt.Color;

public class BedTargetRenderState {
    private static final float TOOL_ICON_SIZE_RATIO = 0.55f;
    private ProjectedEntityBounds projectedBounds;
    private final BedTargetRenderPosition targetPosition;
    private float visibilityProgress = 0.0f;
    private boolean insideReticle;
    private Vec3d obstructionPoint;
    private BedBreakPhase breakPhase = BedBreakPhase.UNRESOLVED;
    private BedCoverTarget coverTarget;
    private final TimerUtil animationTimer = new TimerUtil();

    private void setInsideReticle(boolean insideReticle) {
        this.insideReticle = insideReticle;
    }

    public void setObstructionPoint(Vec3d obstructionPoint) {
        this.obstructionPoint = obstructionPoint;
    }

    public BedTargetRenderPosition getTargetPosition() {
        return this.targetPosition;
    }

    public void renderIndicator(RectData rectData, boolean selected, float breakProgress, ItemStack toolIcon) {
        Vec3 playerView = Minecraft.F().O(1.0f);
        double targetDistance = playerView.distanceTo(new Vec3d((double)this.targetPosition.getBlockX() + 0.5, (double)this.targetPosition.getBlockY() + 0.5, (double)this.targetPosition.getBlockZ() + 0.5).toVec3());
        float renderResolutionMultiplier = RenderWorldLastEvent.getRenderResolutionMultiplier();
        float screenHeight = Minecraft.h();
        ProjectedEntityBounds bounds = this.projectedBounds;
        double left = bounds.minX / (double)renderResolutionMultiplier;
        double right = bounds.maxX / (double)renderResolutionMultiplier;
        double top = (double)screenHeight - bounds.maxY / (double)renderResolutionMultiplier;
        double bottom = (double)screenHeight - bounds.minY / (double)renderResolutionMultiplier;
        double projectedWidth = right - left;
        double projectedHeight = bottom - top;
        float indicatorSize = (float)Math.min(projectedWidth, projectedHeight);
        boolean reticleContainsTarget = rectData.z(left + projectedWidth / 2.0, top + projectedHeight / 2.0, indicatorSize / 2.0f);
        boolean obstructionReachable = false;
        if (this.obstructionPoint != null) {
            double obstructionDistance = playerView.distanceTo(this.obstructionPoint.toVec3());
            if (obstructionDistance < 4.5) {
                obstructionReachable = true;
            }
            if (!obstructionReachable) {
                reticleContainsTarget = false;
            }
        }
        float distanceFade = 1.0f;
        if (targetDistance < 20.0) {
            distanceFade = targetDistance > 10.0 ? (float)((20.0 - targetDistance) / 10.0) : 1.0f;
        }
        this.setInsideReticle(reticleContainsTarget);
        float visibility = this.getVisibilityProgress();
        int backgroundAlpha = (int)((float)(obstructionReachable ? 160 : 150) * distanceFade);
        int outlineAlpha = (int)((float)(obstructionReachable ? 250 : 170) * distanceFade);
        indicatorSize += 20.0f * visibility;
        float indicatorX = (float)(left + projectedWidth / 2.0 - (double)(indicatorSize / 2.0f));
        float indicatorY = (float)(top + projectedHeight / 2.0 - (double)(indicatorSize / 2.0f));
        GuiRenderPrimitives.V(indicatorX, indicatorY, indicatorSize, 1.0, new Color(10, 10, 10, backgroundAlpha));
        int greenBoost = (int)(75.0f * visibility);
        if (obstructionReachable) {
            GuiRenderPrimitives.m(indicatorX, indicatorY, indicatorSize, indicatorSize * 0.12f, 1.0f, new Color(10, 100 + greenBoost, 10, 255));
        }
        GuiRenderPrimitives.m(indicatorX, indicatorY, indicatorSize, indicatorSize * 0.1f, 1.0f, new Color(10, 10, 10, outlineAlpha));
        if (selected) {
            float breakPercent = 100.0f * breakProgress;
            float breakDegrees = 360.0f * (breakPercent / 100.0f);
            if (breakPercent < 100.0f && breakPercent > 0.0f) {
                GuiRenderPrimitives.p(indicatorX, indicatorY, indicatorSize, indicatorSize * 0.12f, 1.0f, 270.0f, -breakDegrees, new Color(10, 100 + greenBoost, 10, 255));
            }
        }
        if (toolIcon != null && toolIcon.isNotNull()) {
            int iconSize = Math.max(1, Math.round(indicatorSize * TOOL_ICON_SIZE_RATIO));
            float iconX = indicatorX + (indicatorSize - (float)iconSize) / 2.0f;
            float iconY = indicatorY + (indicatorSize - (float)iconSize) / 2.0f;
            ItemIconRenderer.renderItemStack(toolIcon, iconX, iconY,
                    iconSize, iconSize, distanceFade);
        }
    }

    public float getVisibilityProgress() {
        return this.visibilityProgress;
    }

    public BedTargetRenderState(BedTargetRenderPosition bedTargetRenderPosition) {
        this.targetPosition = bedTargetRenderPosition;
    }

    public BedBreakPhase getBreakPhase() {
        return this.breakPhase;
    }

    public BedCoverTarget getCoverTarget() {
        return this.coverTarget;
    }

    public void resolveCoverTarget(BedCoverTarget coverTarget) {
        this.coverTarget = coverTarget;
        this.breakPhase = coverTarget == null ? BedBreakPhase.BED : BedBreakPhase.COVER;
    }

    public void finishCover() {
        this.coverTarget = null;
        this.breakPhase = BedBreakPhase.BED;
    }

    public void resetBreakState() {
        this.coverTarget = null;
        this.breakPhase = BedBreakPhase.UNRESOLVED;
        this.obstructionPoint = null;
    }

    public void updateVisibilityAnimation() {
        if (this.animationTimer.hasTimeElapsed(10L)) {
            this.visibilityProgress = this.insideReticle ? (float)((double)this.visibilityProgress + 0.05) : (float)((double)this.visibilityProgress - 0.05);
            this.visibilityProgress = Math.min(1.0f, Math.max(0.0f, this.visibilityProgress));
            this.animationTimer.reset();
        }
    }

    public boolean isInsideReticle() {
        return this.insideReticle;
    }

    public void updateProjectedBounds() {
        AxisAlignedBB axisAlignedBB = AxisAlignedBB.create(0.2, 0.0, 0.2, 0.8, 0.8, 0.8);
        RenderUtil.d();
        this.projectedBounds = new ProjectedEntityBounds(this.targetPosition.getRelativeX(), this.targetPosition.getRelativeY() + 0.4, this.targetPosition.getRelativeZ(), axisAlignedBB, null, null, null);
        RenderUtil.Y();
    }

}
