package gg.vape.module.blatant.autoladder;

import gg.vape.config.ClientSettings;
import gg.vape.module.blatant.blockin.BlockPathPlanner;
import gg.vape.module.blatant.blockin.BlockPlacementGraph;
import gg.vape.module.utility.clutch.ClutchPlacementPathUtils;
import gg.vape.module.utility.clutch.PlacementTarget;
import gg.vape.utils.BlockUtil;
import gg.vape.utils.MathUtil;
import gg.vape.utils.RotationVectorMath;
import gg.vape.utils.datas.BlockCoordinate;
import gg.vape.utils.datas.BlockData;
import gg.vape.wrapper.impl.AxisAlignedBB;
import gg.vape.wrapper.impl.Block;
import gg.vape.wrapper.impl.Blocks;
import gg.vape.wrapper.impl.EntityPlayer;
import gg.vape.wrapper.impl.EntityPlayerSP;
import gg.vape.wrapper.impl.EnumFacing;
import gg.vape.wrapper.impl.ForgeVersion;
import gg.vape.wrapper.impl.GameSettings;
import gg.vape.wrapper.impl.Minecraft;
import gg.vape.wrapper.impl.RayTraceResult;
import gg.vape.wrapper.impl.Vec3;
import gg.vape.wrapper.impl.World;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/**
 * Searches from future climbable player cells back to the placement actions needed
 * to create them. The exact placement and climb checks remain runtime-authoritative.
 */
public final class AutoLadderPlanner {
    private static final int MAX_SIMULATION_TICKS = 20;
    private static final double PLAYER_MARGIN = 0.06;
    private static final double SUPPORT_CLEARANCE_MARGIN = 0.04;
    private static final double NO_MOVEMENT_MAX_ERROR = 0.22;
    private static final double CONTROLLED_PATH_MAX_CORRECTION = 0.45;
    private static final double CONTROL_CORRECTION_PER_TICK = 0.12;
    private static final double LANDING_CENTER_SCORE_WEIGHT = 600.0;
    private static final double FALL_ADJUSTMENT_SCORE = 20.0;
    private static final double PLAN_COUNT_BONUS = 25.0;
    private static final EnumFacing[] HORIZONTAL_FACINGS =
            EnumFacing.c$src$ALgg_vape_wrapper_impl_EnumFacing_$1i3g4ft();
    private static final EnumFacing[] ALL_FACINGS = EnumFacing.t();

    private final World world;
    private final EntityPlayerSP player;
    private final boolean ladderAvailable;
    private final boolean supportBlockAvailable;
    private final boolean allowSupportBlock;
    private final boolean controlMovement;
    private final Set<String> rejectedPlans;
    private final BlockCoordinate landingBlock;
    private final double reach;
    private final boolean[] physicalInput;
    private int trajectoryPointCount;
    private int gridCellCount;
    private int landingCellCount;
    private int catchGeometryCount;
    private int existingLadderCount;
    private int directSupportCount;
    private int directOpportunityCount;
    private int fallbackSpaceCount;
    private int fallbackAnchorCount;
    private int fallbackBlockOpportunityCount;
    private int fallbackLandingRejectedCount;
    private int fallbackCollisionRejectedCount;
    private int fallbackLadderOpportunityCount;
    private int fallbackMovementRejectedCount;
    private int fallbackControlledCollisionRejectedCount;
    private int sweptCatchSampleCount;
    private int evaluatedTrajectoryCount;
    private AutoLadderFallAdjustment recommendedFallAdjustment =
            AutoLadderFallAdjustment.PHYSICAL;
    private double recommendedCenterError = Double.POSITIVE_INFINITY;
    private String auditSummary = "not-run";

    public AutoLadderPlanner(World world, EntityPlayerSP player, boolean ladderAvailable,
                             boolean supportBlockAvailable, boolean allowSupportBlock,
                             boolean controlMovement, Set<String> rejectedPlans,
                             BlockCoordinate landingBlock) {
        this.world = world;
        this.player = player;
        this.ladderAvailable = ladderAvailable;
        this.supportBlockAvailable = supportBlockAvailable;
        this.allowSupportBlock = allowSupportBlock;
        this.controlMovement = controlMovement;
        this.rejectedPlans = rejectedPlans;
        this.landingBlock = landingBlock;
        this.reach = Math.max(0.0, Minecraft.playerController().N());
        GameSettings settings = Minecraft.gameSettings();
        this.physicalInput = new boolean[]{
                ClientSettings.isPhysicalKeyDown(settings.Y()),
                ClientSettings.isPhysicalKeyDown(settings.s()),
                ClientSettings.isPhysicalKeyDown(
                        settings.x$src$Lgg_vape_wrapper_impl_KeyBinding_$1cf7isg()),
                ClientSettings.isPhysicalKeyDown(
                        settings.g$src$Lgg_vape_wrapper_impl_KeyBinding_$qqn5n3())
        };
    }

    @Nullable
    public AutoLadderPlan findBestPlan() {
        List<TrajectoryCandidate> trajectories = this.simulateTrajectories();
        if (trajectories.isEmpty()) {
            this.finishAudit(null, 0, 0);
            return null;
        }

        List<CandidateEvaluation> evaluations = new ArrayList<>();
        int directPlanCount = 0;
        for (TrajectoryCandidate trajectory : trajectories) {
            CounterSnapshot before = new CounterSnapshot(this);
            List<AutoLadderPlan> directPlans = this.findDirectPlans(trajectory);
            CandidateEvaluation evaluation = new CandidateEvaluation(
                    trajectory, directPlans, before.progressSince(this));
            evaluations.add(evaluation);
            directPlanCount += directPlans.size();
        }
        CandidateEvaluation direct = this.selectPlanEvaluation(evaluations, true);
        if (direct != null) {
            AutoLadderPlan selected = direct.bestDirectPlan();
            this.recordRecommendation(direct, selected);
            this.finishAudit(selected, directPlanCount, 0);
            return selected;
        }
        if (!this.allowSupportBlock || !this.supportBlockAvailable || !this.ladderAvailable) {
            this.recordRecommendation(this.selectProgressEvaluation(evaluations), null);
            this.finishAudit(null, 0, 0);
            return null;
        }

        int supportPlanCount = 0;
        for (CandidateEvaluation evaluation : evaluations) {
            CounterSnapshot before = new CounterSnapshot(this);
            evaluation.supportPlans = this.findSupportPlans(evaluation.trajectory);
            evaluation.progressScore += before.progressSince(this);
            supportPlanCount += evaluation.supportPlans.size();
        }
        CandidateEvaluation support = this.selectPlanEvaluation(evaluations, false);
        AutoLadderPlan selected = support == null ? null : support.bestSupportPlan();
        this.recordRecommendation(support == null
                ? this.selectProgressEvaluation(evaluations) : support, selected);
        this.finishAudit(selected, 0, supportPlanCount);
        return selected;
    }

    public String getAuditSummary() {
        return this.auditSummary;
    }

    public AutoLadderFallAdjustment getRecommendedFallAdjustment() {
        return this.recommendedFallAdjustment;
    }

    public double getRecommendedCenterError() {
        return this.recommendedCenterError;
    }

    private void finishAudit(@Nullable AutoLadderPlan selected, int directPlans, int supportPlans) {
        this.auditSummary = "landing=[" + this.landingBlock.B() + ", "
                + this.landingBlock.E() + ", " + this.landingBlock.A() + "]"
                + " trajectories=" + this.evaluatedTrajectoryCount
                + " fallInput=" + this.recommendedFallAdjustment.describe()
                + " centerError=" + (Double.isInfinite(this.recommendedCenterError)
                ? "unknown" : Math.round(this.recommendedCenterError * 100.0) / 100.0)
                + " points=" + this.trajectoryPointCount
                + " grid=" + this.gridCellCount
                + " landingCells=" + this.landingCellCount
                + " catchGeometry=" + this.catchGeometryCount
                + " sweptCatch=" + this.sweptCatchSampleCount
                + " direct{ladders=" + this.existingLadderCount
                + ",supports=" + this.directSupportCount
                + ",windows=" + this.directOpportunityCount
                + ",plans=" + directPlans + '}'
                + " fallback{spaces=" + this.fallbackSpaceCount
                + ",anchors=" + this.fallbackAnchorCount
                + ",blockWindows=" + this.fallbackBlockOpportunityCount
                + ",landingRejected=" + this.fallbackLandingRejectedCount
                + ",preControlCollisionRejected=" + this.fallbackCollisionRejectedCount
                + ",ladderWindows=" + this.fallbackLadderOpportunityCount
                + ",movementRejected=" + this.fallbackMovementRejectedCount
                + ",controlledCollisionRejected=" + this.fallbackControlledCollisionRejectedCount
                + ",plans=" + supportPlans + '}'
                + " selected=" + (selected == null ? "none" : selected.describe());
    }

    private List<TrajectoryCandidate> simulateTrajectories() {
        BlockPlacementGraph graph = new BlockPlacementGraph(this.player);
        List<TrajectoryCandidate> trajectories = new ArrayList<>();
        for (AutoLadderFallAdjustment adjustment : AutoLadderFallAdjustment.values()) {
            if (!this.controlMovement && adjustment.overridesInput()) {
                continue;
            }
            List<TrajectoryPoint> points = this.simulateTrajectory(graph, adjustment);
            if (points.size() < 2) {
                continue;
            }
            TrajectoryPoint finalPoint = points.get(points.size() - 1);
            double centerError = Math.hypot(
                    finalPoint.x - (this.landingBlock.B() + 0.5),
                    finalPoint.z - (this.landingBlock.A() + 0.5));
            trajectories.add(new TrajectoryCandidate(points, adjustment, centerError));
            this.trajectoryPointCount += points.size();
            ++this.evaluatedTrajectoryCount;
        }
        return trajectories;
    }

    private List<TrajectoryPoint> simulateTrajectory(BlockPlacementGraph graph,
                                                      AutoLadderFallAdjustment adjustment) {
        BlockPathPlanner simulation = new BlockPathPlanner(this.player, this.player, this.world, graph);
        simulation.applySnapshot(graph);
        if (adjustment.overridesInput()) {
            simulation.setInput(adjustment.isForward(), adjustment.isBackward(),
                    adjustment.isLeft(), adjustment.isRight(), false, false);
        } else {
            this.applyPhysicalInput(simulation);
        }
        EntityPlayer simulatedPlayer = simulation.getSimulatedPlayer();
        List<TrajectoryPoint> points = new ArrayList<>();
        for (int tick = 0; tick <= MAX_SIMULATION_TICKS; ++tick) {
            boolean onGround = simulatedPlayer.b$src$Z$fqlxe4();
            points.add(TrajectoryPoint.capture(tick, simulatedPlayer, onGround));
            if (tick > 0 && onGround) {
                break;
            }
            simulation.simulateTick();
            if (tick == 0) {
                this.applyPhysicalInput(simulation);
            }
        }
        return points;
    }

    private void applyPhysicalInput(BlockPathPlanner simulation) {
        simulation.setInput(this.physicalInput[0], this.physicalInput[1],
                this.physicalInput[2], this.physicalInput[3], false, false);
    }

    private List<AutoLadderPlan> findDirectPlans(TrajectoryCandidate candidate) {
        Map<String, AutoLadderPlan> plans = new HashMap<>();
        List<TrajectoryPoint> trajectory = candidate.points;
        for (CatchSample catchSample : this.findCatchSamples(trajectory)) {
            TrajectoryPoint point = catchSample.point;
            this.enumerateCatchCells(point, (ladderBlock, facing, catchX, catchZ, movementError) -> {
                    BlockData supportBlock = ladderBlock.R(facing.getOpposite());
                    PlacementTarget ladderTarget = new PlacementTarget(supportBlock, facing);
                    Block ladderState = this.blockAt(ladderBlock);
                    if (this.isLadder(ladderState)) {
                        ++this.existingLadderCount;
                        if (facing.Y() != HORIZONTAL_FACINGS[0].Y()) {
                            return;
                        }
                        double existingCatchX = ladderBlock.D() + 0.5;
                        double existingCatchZ = ladderBlock.G() + 0.5;
                        double existingMovementError = Math.hypot(
                                existingCatchX - point.x, existingCatchZ - point.z);
                        AutoLadderPlan plan = new AutoLadderPlan(
                                AutoLadderPlan.Mode.EXISTING_LADDER, null, ladderTarget,
                                existingCatchX, existingCatchZ, point.tick, -1, -1,
                                existingMovementError * 1000.0 + point.tick * 4.0
                                        + this.trajectoryCost(candidate),
                                candidate.adjustment);
                        this.addPlan(plans, plan);
                        return;
                    }
                    if (!this.ladderAvailable || !BlockUtil.u(ladderState)
                            || !this.isStableSupport(supportBlock)) {
                        return;
                    }
                    ++this.directSupportCount;
                    PlacementOpportunity opportunity = this.findPlacementOpportunity(
                            ladderTarget, trajectory, 0,
                            catchSample.latestPlacementTick, false);
                    if (opportunity == null) {
                        return;
                    }
                    ++this.directOpportunityCount;
                    int slack = point.tick - opportunity.tick;
                    if (!this.canCorrectToCatch(movementError, slack)) {
                        return;
                    }
                    double score = movementError * 1000.0 + opportunity.rotationDistance * 2.0
                            + point.tick * 4.0 - slack * 35.0
                            + this.trajectoryCost(candidate);
                    AutoLadderPlan plan = new AutoLadderPlan(
                            AutoLadderPlan.Mode.DIRECT, null, ladderTarget,
                            catchX, catchZ, point.tick, -1, opportunity.tick, score,
                            candidate.adjustment);
                    this.addPlan(plans, plan);
            });
        }
        return new ArrayList<>(plans.values());
    }

    private List<AutoLadderPlan> findSupportPlans(TrajectoryCandidate candidate) {
        Map<String, AutoLadderPlan> plans = new HashMap<>();
        List<TrajectoryPoint> trajectory = candidate.points;
        for (CatchSample catchSample : this.findCatchSamples(trajectory)) {
            TrajectoryPoint point = catchSample.point;
            this.enumerateCatchCells(point, (ladderBlock, ladderFacing, catchX, catchZ, movementError) -> {
                    if (!BlockUtil.u(this.blockAt(ladderBlock))) {
                        return;
                    }
                    BlockData supportBlock = ladderBlock.R(ladderFacing.getOpposite());
                    if (!BlockUtil.u(this.blockAt(supportBlock))) {
                        return;
                    }
                    if (this.isUnsafeLandingSupport(supportBlock)) {
                        ++this.fallbackLandingRejectedCount;
                        return;
                    }
                    if (!ClutchPlacementPathUtils.isPlacementSpaceClear(this.world, this.player, supportBlock)) {
                        return;
                    }
                    ++this.fallbackSpaceCount;
                    PlacementTarget ladderTarget = new PlacementTarget(supportBlock, ladderFacing);
                    for (EnumFacing blockFacing : ALL_FACINGS) {
                        BlockData anchorBlock = supportBlock.R(blockFacing.getOpposite());
                        if (!this.isStableSupport(anchorBlock)) {
                            continue;
                        }
                        ++this.fallbackAnchorCount;
                        PlacementTarget blockTarget = new PlacementTarget(anchorBlock, blockFacing);
                        PlacementOpportunity blockOpportunity = this.findPlacementOpportunity(
                                blockTarget, trajectory, 0,
                                catchSample.latestPlacementTick, true);
                        if (blockOpportunity == null) {
                            continue;
                        }
                        ++this.fallbackBlockOpportunityCount;
                        PlacementOpportunity ladderOpportunity = this.findFutureFaceOpportunity(
                                ladderTarget, trajectory,
                                blockOpportunity.tick + 1,
                                catchSample.latestPlacementTick);
                        if (ladderOpportunity == null) {
                            continue;
                        }
                        ++this.fallbackLadderOpportunityCount;
                        int slack = point.tick - ladderOpportunity.tick;
                        if (this.intersectsBlockBeforeCatchControl(
                                trajectory, supportBlock, blockOpportunity.tick,
                                ladderOpportunity.tick)) {
                            ++this.fallbackCollisionRejectedCount;
                            continue;
                        }
                        if (!this.canCorrectToCatch(movementError, slack)) {
                            ++this.fallbackMovementRejectedCount;
                            continue;
                        }
                        if (!this.avoidsSupportDuringCatchControl(
                                trajectory, point, supportBlock, ladderOpportunity.tick,
                                catchX, catchZ)) {
                            ++this.fallbackControlledCollisionRejectedCount;
                            continue;
                        }
                        double score = movementError * 1000.0
                                + (blockOpportunity.rotationDistance + ladderOpportunity.rotationDistance) * 2.0
                                + point.tick * 5.0 - slack * 30.0
                                + this.trajectoryCost(candidate);
                        AutoLadderPlan plan = new AutoLadderPlan(
                                AutoLadderPlan.Mode.BUILD_SUPPORT, blockTarget, ladderTarget,
                                catchX, catchZ, point.tick, blockOpportunity.tick,
                                ladderOpportunity.tick, score, candidate.adjustment);
                        this.addPlan(plans, plan);
                    }
            });
        }
        return new ArrayList<>(plans.values());
    }

    private double trajectoryCost(TrajectoryCandidate candidate) {
        return candidate.centerError * LANDING_CENTER_SCORE_WEIGHT
                + (candidate.adjustment.overridesInput() ? FALL_ADJUSTMENT_SCORE : 0.0);
    }

    @Nullable
    private CandidateEvaluation selectPlanEvaluation(List<CandidateEvaluation> evaluations,
                                                     boolean direct) {
        CandidateEvaluation selected = null;
        double selectedScore = Double.POSITIVE_INFINITY;
        for (CandidateEvaluation evaluation : evaluations) {
            List<AutoLadderPlan> plans = direct
                    ? evaluation.directPlans : evaluation.supportPlans;
            if (plans == null || plans.isEmpty()) {
                continue;
            }
            AutoLadderPlan bestPlan = direct
                    ? evaluation.bestDirectPlan() : evaluation.bestSupportPlan();
            double score = bestPlan.getScore()
                    - Math.min(8, plans.size()) * PLAN_COUNT_BONUS;
            if (selected == null || score < selectedScore
                    || score == selectedScore
                    && evaluation.trajectory.centerError < selected.trajectory.centerError) {
                selected = evaluation;
                selectedScore = score;
            }
        }
        return selected;
    }

    @Nullable
    private CandidateEvaluation selectProgressEvaluation(List<CandidateEvaluation> evaluations) {
        CandidateEvaluation selected = null;
        for (CandidateEvaluation evaluation : evaluations) {
            if (selected == null || evaluation.progressScore > selected.progressScore
                    || evaluation.progressScore == selected.progressScore
                    && evaluation.trajectory.centerError < selected.trajectory.centerError
                    || evaluation.progressScore == selected.progressScore
                    && evaluation.trajectory.centerError == selected.trajectory.centerError
                    && !evaluation.trajectory.adjustment.overridesInput()
                    && selected.trajectory.adjustment.overridesInput()) {
                selected = evaluation;
            }
        }
        return selected;
    }

    private void recordRecommendation(@Nullable CandidateEvaluation evaluation,
                                      @Nullable AutoLadderPlan plan) {
        if (plan != null) {
            this.recommendedFallAdjustment = plan.getFallAdjustment();
            this.recommendedCenterError = evaluation == null
                    ? Double.POSITIVE_INFINITY : evaluation.trajectory.centerError;
            return;
        }
        if (evaluation == null) {
            this.recommendedFallAdjustment = AutoLadderFallAdjustment.PHYSICAL;
            this.recommendedCenterError = Double.POSITIVE_INFINITY;
            return;
        }
        this.recommendedFallAdjustment = evaluation.trajectory.adjustment;
        this.recommendedCenterError = evaluation.trajectory.centerError;
    }

    private boolean canCorrectToCatch(double movementError, int controlTicks) {
        if (!this.controlMovement) {
            return movementError <= NO_MOVEMENT_MAX_ERROR;
        }
        double correctableDistance = NO_MOVEMENT_MAX_ERROR
                + Math.max(0, controlTicks) * CONTROL_CORRECTION_PER_TICK;
        return movementError <= Math.min(CONTROLLED_PATH_MAX_CORRECTION, correctableDistance);
    }

    private boolean avoidsSupportDuringCatchControl(List<TrajectoryPoint> trajectory,
                                                     TrajectoryPoint catchPoint,
                                                     BlockData supportBlock,
                                                     int controlStartTick,
                                                     double catchX, double catchZ) {
        int controlTicks = catchPoint.tick - controlStartTick;
        double correctionX = catchX - catchPoint.x;
        double correctionZ = catchZ - catchPoint.z;
        TrajectoryPoint previous = null;
        for (TrajectoryPoint naturalPoint : trajectory) {
            if (naturalPoint.tick < controlStartTick) {
                continue;
            }
            if (naturalPoint.tick > catchPoint.tick) {
                break;
            }
            TrajectoryPoint pathPoint = naturalPoint.tick == catchPoint.tick
                    ? catchPoint : naturalPoint;
            double progress = controlTicks <= 0 ? 1.0
                    : (double)(pathPoint.tick - controlStartTick) / controlTicks;
            progress = Math.max(0.0, Math.min(1.0, progress));
            progress *= progress;
            TrajectoryPoint controlledPoint = pathPoint.offsetHorizontal(
                    correctionX * progress, correctionZ * progress);
            if (controlledPoint.intersectsUnitBlock(supportBlock, SUPPORT_CLEARANCE_MARGIN)
                    || previous != null && previous.sweptIntersectsUnitBlock(
                    controlledPoint, supportBlock, SUPPORT_CLEARANCE_MARGIN)) {
                return false;
            }
            previous = controlledPoint;
        }
        return previous != null;
    }

    private List<CatchSample> findCatchSamples(List<TrajectoryPoint> trajectory) {
        List<CatchSample> samples = new ArrayList<>();
        double ladderBottom = this.landingBlock.E() + 1.0;
        double ladderTop = ladderBottom + 1.0;
        TrajectoryPoint initial = trajectory.get(0);
        if (!initial.onGround && initial.motionY < 0.0
                && initial.y >= ladderBottom && initial.y < ladderTop) {
            samples.add(new CatchSample(initial, initial.tick));
        }
        for (int index = 1; index < trajectory.size(); ++index) {
            TrajectoryPoint previous = trajectory.get(index - 1);
            TrajectoryPoint current = trajectory.get(index);
            if (previous.motionY >= 0.0 && current.motionY >= 0.0) {
                continue;
            }
            if (previous.y >= ladderTop && current.y < ladderTop) {
                TrajectoryPoint sweptPoint = TrajectoryPoint.interpolateAtY(
                        previous, current, ladderTop - 1.0E-4);
                samples.add(new CatchSample(sweptPoint, previous.tick));
                ++this.sweptCatchSampleCount;
                continue;
            }
            if (!current.onGround && current.motionY < 0.0
                    && current.y >= ladderBottom && current.y < ladderTop) {
                samples.add(new CatchSample(current, current.tick));
            }
        }
        return samples;
    }

    private void enumerateCatchCells(TrajectoryPoint point, CatchCellConsumer consumer) {
        int searchRadius = this.controlMovement ? 1 : 0;
        int baseX = MathUtil.floor(point.x);
        // Swept samples sit just below the layer ceiling; the ladder layer comes from the landing.
        int baseY = this.landingBlock.E() + 1;
        int baseZ = MathUtil.floor(point.z);
        for (int xOffset = -searchRadius; xOffset <= searchRadius; ++xOffset) {
            for (int zOffset = -searchRadius; zOffset <= searchRadius; ++zOffset) {
                BlockData ladderBlock = new BlockData(baseX + xOffset, baseY, baseZ + zOffset);
                ++this.gridCellCount;
                if (!this.isNearLandingCell(ladderBlock)) {
                    continue;
                }
                ++this.landingCellCount;
                for (EnumFacing facing : HORIZONTAL_FACINGS) {
                    double[] catchPoint = this.computeCatchPoint(ladderBlock, facing, point);
                    double movementError = Math.hypot(catchPoint[0] - point.x, catchPoint[1] - point.z);
                    double allowedError = this.controlMovement
                            ? CONTROLLED_PATH_MAX_CORRECTION : NO_MOVEMENT_MAX_ERROR;
                    if (movementError > allowedError) {
                        continue;
                    }
                    ++this.catchGeometryCount;
                    consumer.accept(ladderBlock, facing, catchPoint[0], catchPoint[1], movementError);
                }
            }
        }
    }

    private double[] computeCatchPoint(BlockData ladderBlock, EnumFacing facing, TrajectoryPoint point) {
        double halfWidth = Math.max(0.2, Math.min(0.45, point.width / 2.0));
        double tangentialInset = Math.min(0.49, halfWidth + PLAYER_MARGIN);
        double normalInset = halfWidth + AutoLadderMovementController.getLadderThickness() / 2.0;
        double catchX = clamp(point.x, ladderBlock.D() + tangentialInset,
                ladderBlock.D() + 1.0 - tangentialInset);
        double catchZ = clamp(point.z, ladderBlock.G() + tangentialInset,
                ladderBlock.G() + 1.0 - tangentialInset);
        int directionX = facing.getDirectionVector().getX();
        int directionZ = facing.getDirectionVector().getZ();
        if (directionX > 0) {
            catchX = ladderBlock.D() + normalInset;
        } else if (directionX < 0) {
            catchX = ladderBlock.D() + 1.0 - normalInset;
        }
        if (directionZ > 0) {
            catchZ = ladderBlock.G() + normalInset;
        } else if (directionZ < 0) {
            catchZ = ladderBlock.G() + 1.0 - normalInset;
        }
        return new double[]{catchX, catchZ};
    }

    private boolean isNearLandingCell(BlockData ladderBlock) {
        return ladderBlock.B() == this.landingBlock.E() + 1
                && Math.abs(ladderBlock.D() - this.landingBlock.B()) <= 2
                && Math.abs(ladderBlock.G() - this.landingBlock.A()) <= 2;
    }

    @Nullable
    private PlacementOpportunity findPlacementOpportunity(PlacementTarget target,
                                                           List<TrajectoryPoint> trajectory,
                                                           int earliestTick,
                                                           int latestTick,
                                                           boolean placingSolidBlock) {
        if (latestTick < earliestTick) {
            return null;
        }
        PlacementOpportunity best = null;
        for (TrajectoryPoint point : trajectory) {
            if (point.tick < earliestTick) {
                continue;
            }
            if (point.tick > latestTick) {
                break;
            }
            if (placingSolidBlock && point.intersectsUnitBlock(target.getPlacedBlock())) {
                continue;
            }
            Vec3 eye = point.eyePosition();
            if (!ClutchPlacementPathUtils.isBlockFaceVisible(
                    eye, this.world, target.supportBlock, target.facing)) {
                continue;
            }
            Vec3 hit = ClutchPlacementPathUtils.findBestPlacementHitPointWithinReach(
                    this.player, this.world, eye, target, point.yaw, point.pitch, this.reach);
            if (hit == null) {
                continue;
            }
            double rotationDistance = this.rotationDistance(eye, hit, point.yaw, point.pitch);
            PlacementOpportunity candidate = new PlacementOpportunity(point.tick, rotationDistance);
            if (best == null || candidate.tick < best.tick
                    || candidate.tick == best.tick
                    && candidate.rotationDistance < best.rotationDistance) {
                best = candidate;
            }
        }
        return best;
    }

    @Nullable
    private PlacementOpportunity findFutureFaceOpportunity(PlacementTarget target,
                                                            List<TrajectoryPoint> trajectory,
                                                            int earliestTick,
                                                            int latestTick) {
        if (latestTick < earliestTick) {
            return null;
        }
        Vec3 faceCenter = this.faceCenter(target.supportBlock, target.facing);
        PlacementOpportunity best = null;
        for (TrajectoryPoint point : trajectory) {
            if (point.tick < earliestTick) {
                continue;
            }
            if (point.tick > latestTick) {
                break;
            }
            Vec3 eye = point.eyePosition();
            if (!ClutchPlacementPathUtils.isBlockFaceVisible(
                    eye, this.world, target.supportBlock, target.facing)
                    || eye.distanceTo(faceCenter) > this.reach
                    || !this.isFutureFaceUnobstructed(eye, faceCenter)) {
                continue;
            }
            double rotationDistance = this.rotationDistance(eye, faceCenter, point.yaw, point.pitch);
            PlacementOpportunity candidate = new PlacementOpportunity(point.tick, rotationDistance);
            if (best == null || candidate.tick < best.tick
                    || candidate.tick == best.tick && candidate.rotationDistance < best.rotationDistance) {
                best = candidate;
            }
        }
        return best;
    }

    private double rotationDistance(Vec3 eye, Vec3 hit, float yaw, float pitch) {
        gg.vape.rotation.RotationAngles rotation = RotationVectorMath.d(eye, hit, yaw, pitch);
        return Math.abs(MathUtil.wrapAngleTo180(rotation.getYaw() - yaw))
                + Math.abs(rotation.getPitch() - pitch);
    }

    private Vec3 faceCenter(BlockData block, EnumFacing facing) {
        double x = block.D() + 0.5 + facing.getDirectionVector().getX() * 0.5;
        double y = block.B() + 0.5 + facing.getDirectionVector().getY() * 0.5;
        double z = block.G() + 0.5 + facing.getDirectionVector().getZ() * 0.5;
        return Vec3.create(x, y, z);
    }

    private boolean intersectsBlockBeforeCatchControl(List<TrajectoryPoint> trajectory,
                                                       BlockData block, int firstTick, int lastTick) {
        TrajectoryPoint previous = null;
        for (TrajectoryPoint point : trajectory) {
            if (point.tick < firstTick - 1) {
                continue;
            }
            if (point.tick > lastTick) {
                break;
            }
            if (point.tick >= firstTick
                    && (point.intersectsUnitBlock(block, SUPPORT_CLEARANCE_MARGIN)
                    || previous != null && previous.sweptIntersectsUnitBlock(
                    point, block, SUPPORT_CLEARANCE_MARGIN))) {
                return true;
            }
            previous = point;
        }
        return false;
    }

    private boolean isUnsafeLandingSupport(BlockData supportBlock) {
        return supportBlock.B() == this.landingBlock.E() + 1
                && supportBlock.D() == this.landingBlock.B()
                && supportBlock.G() == this.landingBlock.A();
    }

    private boolean isFutureFaceUnobstructed(Vec3 eye, Vec3 faceCenter) {
        RayTraceResult rayTrace = this.world.K(eye, faceCenter, false, false,
                ForgeVersion.MC_1_16_5.v(), this.player);
        return rayTrace.isNull() || !rayTrace.isBlockHit();
    }

    private void addPlan(Map<String, AutoLadderPlan> plans, AutoLadderPlan plan) {
        String key = plan.rejectionKey();
        if (this.rejectedPlans.contains(key)) {
            return;
        }
        AutoLadderPlan previous = plans.get(key);
        if (previous == null || plan.getScore() < previous.getScore()) {
            plans.put(key, plan);
        }
    }

    private Block blockAt(BlockData block) {
        return this.world.getBlockByPos(block.D(), block.B(), block.G());
    }

    private boolean isLadder(Block block) {
        return block.isNotNull() && block.equals(Blocks.ladder());
    }

    private boolean isStableSupport(BlockData blockData) {
        Block block = this.blockAt(blockData);
        return block.isNotNull() && BlockUtil.k(block)
                && !ClutchPlacementPathUtils.isBlacklistedPlacementBlock(block);
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private interface CatchCellConsumer {
        void accept(BlockData ladderBlock, EnumFacing facing,
                    double catchX, double catchZ, double movementError);
    }

    private static final class CatchSample {
        private final TrajectoryPoint point;
        private final int latestPlacementTick;

        private CatchSample(TrajectoryPoint point, int latestPlacementTick) {
            this.point = point;
            this.latestPlacementTick = latestPlacementTick;
        }
    }

    private static final class TrajectoryCandidate {
        private final List<TrajectoryPoint> points;
        private final AutoLadderFallAdjustment adjustment;
        private final double centerError;

        private TrajectoryCandidate(List<TrajectoryPoint> points,
                                    AutoLadderFallAdjustment adjustment,
                                    double centerError) {
            this.points = points;
            this.adjustment = adjustment;
            this.centerError = centerError;
        }
    }

    private static final class CandidateEvaluation {
        private final TrajectoryCandidate trajectory;
        private final List<AutoLadderPlan> directPlans;
        private List<AutoLadderPlan> supportPlans = new ArrayList<>();
        private int progressScore;

        private CandidateEvaluation(TrajectoryCandidate trajectory,
                                    List<AutoLadderPlan> directPlans,
                                    int progressScore) {
            this.trajectory = trajectory;
            this.directPlans = directPlans;
            this.progressScore = progressScore;
        }

        private AutoLadderPlan bestDirectPlan() {
            return this.directPlans.stream()
                    .min(Comparator.comparingDouble(AutoLadderPlan::getScore)).orElse(null);
        }

        private AutoLadderPlan bestSupportPlan() {
            return this.supportPlans.stream()
                    .min(Comparator.comparingDouble(AutoLadderPlan::getScore)).orElse(null);
        }
    }

    private static final class CounterSnapshot {
        private final int catchGeometry;
        private final int existingLadders;
        private final int directSupports;
        private final int directOpportunities;
        private final int fallbackSpaces;
        private final int fallbackAnchors;
        private final int fallbackBlockOpportunities;
        private final int fallbackLadderOpportunities;
        private final int fallbackPreControlRejects;
        private final int fallbackMovementRejects;
        private final int fallbackControlledRejects;

        private CounterSnapshot(AutoLadderPlanner planner) {
            this.catchGeometry = planner.catchGeometryCount;
            this.existingLadders = planner.existingLadderCount;
            this.directSupports = planner.directSupportCount;
            this.directOpportunities = planner.directOpportunityCount;
            this.fallbackSpaces = planner.fallbackSpaceCount;
            this.fallbackAnchors = planner.fallbackAnchorCount;
            this.fallbackBlockOpportunities = planner.fallbackBlockOpportunityCount;
            this.fallbackLadderOpportunities = planner.fallbackLadderOpportunityCount;
            this.fallbackPreControlRejects = planner.fallbackCollisionRejectedCount;
            this.fallbackMovementRejects = planner.fallbackMovementRejectedCount;
            this.fallbackControlledRejects = planner.fallbackControlledCollisionRejectedCount;
        }

        private int progressSince(AutoLadderPlanner planner) {
            int catchGeometryDelta = planner.catchGeometryCount - this.catchGeometry;
            int existingDelta = planner.existingLadderCount - this.existingLadders;
            int directSupportDelta = planner.directSupportCount - this.directSupports;
            int directOpportunityDelta = planner.directOpportunityCount - this.directOpportunities;
            int fallbackSpaceDelta = planner.fallbackSpaceCount - this.fallbackSpaces;
            int fallbackAnchorDelta = planner.fallbackAnchorCount - this.fallbackAnchors;
            int fallbackBlockDelta = planner.fallbackBlockOpportunityCount
                    - this.fallbackBlockOpportunities;
            int fallbackLadderDelta = planner.fallbackLadderOpportunityCount
                    - this.fallbackLadderOpportunities;
            int rejectionDelta = planner.fallbackCollisionRejectedCount
                    - this.fallbackPreControlRejects
                    + planner.fallbackMovementRejectedCount - this.fallbackMovementRejects
                    + planner.fallbackControlledCollisionRejectedCount
                    - this.fallbackControlledRejects;
            int viableLadderWindows = Math.max(0, fallbackLadderDelta - rejectionDelta);
            return viableLadderWindows * 1000
                    + (existingDelta + directOpportunityDelta) * 800
                    + fallbackLadderDelta * 120
                    + fallbackBlockDelta * 30
                    + fallbackAnchorDelta * 8
                    + fallbackSpaceDelta * 4
                    + directSupportDelta * 4
                    + catchGeometryDelta
                    - rejectionDelta * 5;
        }
    }

    private static final class PlacementOpportunity {
        private final int tick;
        private final double rotationDistance;

        private PlacementOpportunity(int tick, double rotationDistance) {
            this.tick = tick;
            this.rotationDistance = rotationDistance;
        }
    }

    private static final class TrajectoryPoint {
        private final int tick;
        private final double x;
        private final double y;
        private final double z;
        private final double eyeY;
        private final double motionY;
        private final float yaw;
        private final float pitch;
        private final double minX;
        private final double minY;
        private final double minZ;
        private final double maxX;
        private final double maxY;
        private final double maxZ;
        private final double width;
        private final boolean onGround;
        private TrajectoryPoint(int tick, double x, double y, double z, double eyeY,
                                double motionY, float yaw, float pitch, AxisAlignedBB bounds,
                                boolean onGround) {
            this.tick = tick;
            this.x = x;
            this.y = y;
            this.z = z;
            this.eyeY = eyeY;
            this.motionY = motionY;
            this.yaw = yaw;
            this.pitch = pitch;
            this.minX = bounds.getMinX();
            this.minY = bounds.getMinY();
            this.minZ = bounds.getMinZ();
            this.maxX = bounds.getMaxX();
            this.maxY = bounds.getMaxY();
            this.maxZ = bounds.getMaxZ();
            this.width = Math.max(this.maxX - this.minX, this.maxZ - this.minZ);
            this.onGround = onGround;
        }

        private static TrajectoryPoint capture(int tick, EntityPlayer player, boolean onGround) {
            AxisAlignedBB bounds = player.u$src$Lgg_vape_wrapper_impl_AxisAlignedBB_$kogbsu();
            return new TrajectoryPoint(tick, player.z(), player.N(), player.h(),
                    player.N() + player.X(), player.q(), player.J(), player.V(), bounds, onGround);
        }

        private static TrajectoryPoint interpolateAtY(TrajectoryPoint start,
                                                      TrajectoryPoint end,
                                                      double targetY) {
            double verticalDelta = end.y - start.y;
            double progress = Math.abs(verticalDelta) < 1.0E-9
                    ? 0.0 : (targetY - start.y) / verticalDelta;
            progress = Math.max(0.0, Math.min(1.0, progress));
            double x = lerp(start.x, end.x, progress);
            double z = lerp(start.z, end.z, progress);
            double eyeY = lerp(start.eyeY, end.eyeY, progress);
            double motionY = lerp(start.motionY, end.motionY, progress);
            float yaw = start.yaw + MathUtil.wrapAngleTo180(end.yaw - start.yaw)
                    * (float)progress;
            float pitch = (float)lerp(start.pitch, end.pitch, progress);
            AxisAlignedBB bounds = AxisAlignedBB.create(
                    lerp(start.minX, end.minX, progress),
                    lerp(start.minY, end.minY, progress),
                    lerp(start.minZ, end.minZ, progress),
                    lerp(start.maxX, end.maxX, progress),
                    lerp(start.maxY, end.maxY, progress),
                    lerp(start.maxZ, end.maxZ, progress));
            return new TrajectoryPoint(end.tick, x, targetY, z, eyeY,
                    motionY, yaw, pitch, bounds, false);
        }

        private static double lerp(double start, double end, double progress) {
            return start + (end - start) * progress;
        }

        private Vec3 eyePosition() {
            return Vec3.create(this.x, this.eyeY, this.z);
        }

        private boolean intersectsUnitBlock(BlockData block) {
            return this.maxX > block.D() && this.minX < block.D() + 1.0
                    && this.maxY > block.B() && this.minY < block.B() + 1.0
                    && this.maxZ > block.G() && this.minZ < block.G() + 1.0;
        }

        private boolean intersectsUnitBlock(BlockData block, double horizontalMargin) {
            return this.maxX + horizontalMargin > block.D()
                    && this.minX - horizontalMargin < block.D() + 1.0
                    && this.maxY > block.B() && this.minY < block.B() + 1.0
                    && this.maxZ + horizontalMargin > block.G()
                    && this.minZ - horizontalMargin < block.G() + 1.0;
        }

        private boolean sweptIntersectsUnitBlock(TrajectoryPoint next,
                                                 BlockData block, double horizontalMargin) {
            double startX = (this.minX + this.maxX) / 2.0;
            double startY = (this.minY + this.maxY) / 2.0;
            double startZ = (this.minZ + this.maxZ) / 2.0;
            double endX = (next.minX + next.maxX) / 2.0;
            double endY = (next.minY + next.maxY) / 2.0;
            double endZ = (next.minZ + next.maxZ) / 2.0;
            double halfWidthX = (this.maxX - this.minX) / 2.0 + horizontalMargin;
            double halfHeight = (this.maxY - this.minY) / 2.0;
            double halfWidthZ = (this.maxZ - this.minZ) / 2.0 + horizontalMargin;
            double xEntry = axisEntry(startX, endX,
                    block.D() - halfWidthX, block.D() + 1.0 + halfWidthX);
            double yEntry = axisEntry(startY, endY,
                    block.B() - halfHeight, block.B() + 1.0 + halfHeight);
            double zEntry = axisEntry(startZ, endZ,
                    block.G() - halfWidthZ, block.G() + 1.0 + halfWidthZ);
            double xExit = axisExit(startX, endX,
                    block.D() - halfWidthX, block.D() + 1.0 + halfWidthX);
            double yExit = axisExit(startY, endY,
                    block.B() - halfHeight, block.B() + 1.0 + halfHeight);
            double zExit = axisExit(startZ, endZ,
                    block.G() - halfWidthZ, block.G() + 1.0 + halfWidthZ);
            double entry = Math.max(0.0, Math.max(xEntry, Math.max(yEntry, zEntry)));
            double exit = Math.min(1.0, Math.min(xExit, Math.min(yExit, zExit)));
            return entry <= exit;
        }

        private static double axisEntry(double start, double end,
                                        double minimum, double maximum) {
            double delta = end - start;
            if (Math.abs(delta) < 1.0E-9) {
                return start >= minimum && start <= maximum
                        ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
            }
            return Math.min((minimum - start) / delta, (maximum - start) / delta);
        }

        private static double axisExit(double start, double end,
                                       double minimum, double maximum) {
            double delta = end - start;
            if (Math.abs(delta) < 1.0E-9) {
                return start >= minimum && start <= maximum
                        ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
            }
            return Math.max((minimum - start) / delta, (maximum - start) / delta);
        }

        private TrajectoryPoint offsetHorizontal(double xOffset, double zOffset) {
            AxisAlignedBB shiftedBounds = AxisAlignedBB.create(
                    this.minX + xOffset, this.minY, this.minZ + zOffset,
                    this.maxX + xOffset, this.maxY, this.maxZ + zOffset);
            return new TrajectoryPoint(this.tick, this.x + xOffset, this.y,
                    this.z + zOffset, this.eyeY, this.motionY,
                    this.yaw, this.pitch, shiftedBounds, this.onGround);
        }
    }
}
