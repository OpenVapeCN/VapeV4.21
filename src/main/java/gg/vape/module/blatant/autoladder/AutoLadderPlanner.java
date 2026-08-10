package gg.vape.module.blatant.autoladder;

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
    private static final double TANGENTIAL_CATCH_INSET = 0.15;
    private static final double SUPPORT_CLEARANCE_MARGIN = 0.04;
    private static final double NO_MOVEMENT_MAX_ERROR = 0.22;
    private static final double CONTROLLED_PATH_MAX_CORRECTION = 0.45;
    private static final int POST_CATCH_CLEARANCE_TICKS = 1;
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
        this.reach = Math.max(0.0, gg.vape.wrapper.impl.Minecraft.playerController().N());
    }

    @Nullable
    public AutoLadderPlan findBestPlan() {
        List<List<TrajectoryPoint>> trajectories = this.simulateTrajectories();
        if (trajectories.isEmpty()) {
            this.finishAudit(null, 0, 0);
            return null;
        }

        List<AutoLadderPlan> directPlans = this.findDirectPlans(trajectories);
        if (!directPlans.isEmpty()) {
            AutoLadderPlan selected = directPlans.stream()
                    .min(Comparator.comparingDouble(AutoLadderPlan::getScore)).orElse(null);
            this.finishAudit(selected, directPlans.size(), 0);
            return selected;
        }
        if (!this.allowSupportBlock || !this.supportBlockAvailable || !this.ladderAvailable) {
            this.finishAudit(null, 0, 0);
            return null;
        }
        List<AutoLadderPlan> supportPlans = this.findSupportPlans(trajectories);
        AutoLadderPlan selected = supportPlans.stream()
                .min(Comparator.comparingDouble(AutoLadderPlan::getScore)).orElse(null);
        this.finishAudit(selected, 0, supportPlans.size());
        return selected;
    }

    public String getAuditSummary() {
        return this.auditSummary;
    }

    private void finishAudit(@Nullable AutoLadderPlan selected, int directPlans, int supportPlans) {
        this.auditSummary = "landing=[" + this.landingBlock.B() + ", "
                + this.landingBlock.E() + ", " + this.landingBlock.A() + "]"
                + " points=" + this.trajectoryPointCount
                + " grid=" + this.gridCellCount
                + " landingCells=" + this.landingCellCount
                + " catchGeometry=" + this.catchGeometryCount
                + " direct{ladders=" + this.existingLadderCount
                + ",supports=" + this.directSupportCount
                + ",windows=" + this.directOpportunityCount
                + ",plans=" + directPlans + '}'
                + " fallback{spaces=" + this.fallbackSpaceCount
                + ",anchors=" + this.fallbackAnchorCount
                + ",blockWindows=" + this.fallbackBlockOpportunityCount
                + ",landingRejected=" + this.fallbackLandingRejectedCount
                + ",collisionRejected=" + this.fallbackCollisionRejectedCount
                + ",ladderWindows=" + this.fallbackLadderOpportunityCount
                + ",plans=" + supportPlans + '}'
                + " selected=" + (selected == null ? "none" : selected.describe());
    }

    private List<List<TrajectoryPoint>> simulateTrajectories() {
        BlockPlacementGraph graph = new BlockPlacementGraph(this.player);
        List<List<TrajectoryPoint>> trajectories = new ArrayList<>();
        List<TrajectoryPoint> trajectory = this.simulateTrajectory(graph, null);
        if (trajectory.size() >= 2) {
            trajectories.add(trajectory);
            this.trajectoryPointCount += trajectory.size();
        }
        return trajectories;
    }

    private List<TrajectoryPoint> simulateTrajectory(BlockPlacementGraph graph,
                                                      @Nullable boolean[] input) {
        BlockPathPlanner simulation = new BlockPathPlanner(this.player, this.player, this.world, graph);
        simulation.applySnapshot(graph);
        if (input == null) {
            simulation.restoreSnapshotInput();
        } else {
            simulation.setInput(input[0], input[1], input[2], input[3], false, false);
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
        }
        return points;
    }

    private List<AutoLadderPlan> findDirectPlans(List<List<TrajectoryPoint>> trajectories) {
        Map<String, AutoLadderPlan> plans = new HashMap<>();
        for (List<TrajectoryPoint> trajectory : trajectories) {
            for (int pointIndex = 1; pointIndex < trajectory.size(); ++pointIndex) {
                TrajectoryPoint point = trajectory.get(pointIndex);
                if (point.motionY >= 0.0 || point.onGround) {
                    continue;
                }
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
                                existingMovementError * 1000.0 + point.tick * 4.0);
                        this.addPlan(plans, plan);
                        return;
                    }
                    if (!this.ladderAvailable || !BlockUtil.u(ladderState)
                            || !this.isStableSupport(supportBlock)) {
                        return;
                    }
                    ++this.directSupportCount;
                    PlacementOpportunity opportunity = this.findPlacementOpportunity(
                            ladderTarget, trajectory, 0, point.tick, false);
                    if (opportunity == null) {
                        return;
                    }
                    ++this.directOpportunityCount;
                    int slack = point.tick - opportunity.tick;
                    double score = movementError * 1000.0 + opportunity.rotationDistance * 2.0
                            + point.tick * 4.0 - slack * 35.0;
                    AutoLadderPlan plan = new AutoLadderPlan(
                            AutoLadderPlan.Mode.DIRECT, null, ladderTarget,
                            catchX, catchZ, point.tick, -1, opportunity.tick, score);
                    this.addPlan(plans, plan);
                });
            }
        }
        return new ArrayList<>(plans.values());
    }

    private List<AutoLadderPlan> findSupportPlans(List<List<TrajectoryPoint>> trajectories) {
        Map<String, AutoLadderPlan> plans = new HashMap<>();
        for (List<TrajectoryPoint> trajectory : trajectories) {
            for (int pointIndex = 1; pointIndex < trajectory.size(); ++pointIndex) {
                TrajectoryPoint point = trajectory.get(pointIndex);
                if (point.motionY >= 0.0 || point.onGround) {
                    continue;
                }
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
                                blockTarget, trajectory, 0, point.tick, true);
                        if (blockOpportunity == null) {
                            continue;
                        }
                        ++this.fallbackBlockOpportunityCount;
                        if (this.intersectsBlockAfterPlacement(
                                trajectory, supportBlock, blockOpportunity.tick, point.tick)) {
                            ++this.fallbackCollisionRejectedCount;
                            continue;
                        }
                        PlacementOpportunity ladderOpportunity = this.findFutureFaceOpportunity(
                                ladderTarget, trajectory,
                                blockOpportunity.tick + 1, point.tick);
                        if (ladderOpportunity == null) {
                            continue;
                        }
                        ++this.fallbackLadderOpportunityCount;
                        int slack = point.tick - ladderOpportunity.tick;
                        double score = movementError * 1000.0
                                + (blockOpportunity.rotationDistance + ladderOpportunity.rotationDistance) * 2.0
                                + point.tick * 5.0 - slack * 30.0;
                        AutoLadderPlan plan = new AutoLadderPlan(
                                AutoLadderPlan.Mode.BUILD_SUPPORT, blockTarget, ladderTarget,
                                catchX, catchZ, point.tick, blockOpportunity.tick,
                                ladderOpportunity.tick, score);
                        this.addPlan(plans, plan);
                    }
                });
            }
        }
        return new ArrayList<>(plans.values());
    }

    private void enumerateCatchCells(TrajectoryPoint point, CatchCellConsumer consumer) {
        int searchRadius = this.controlMovement ? 1 : 0;
        int baseX = MathUtil.floor(point.x);
        int baseY = MathUtil.floor(point.y + 1.0E-4);
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
        double playerInset = halfWidth + PLAYER_MARGIN;
        double catchX = clamp(point.x, ladderBlock.D() + TANGENTIAL_CATCH_INSET,
                ladderBlock.D() + 1.0 - TANGENTIAL_CATCH_INSET);
        double catchZ = clamp(point.z, ladderBlock.G() + TANGENTIAL_CATCH_INSET,
                ladderBlock.G() + 1.0 - TANGENTIAL_CATCH_INSET);
        int directionX = facing.getDirectionVector().getX();
        int directionZ = facing.getDirectionVector().getZ();
        if (directionX > 0) {
            catchX = ladderBlock.D() + playerInset;
        } else if (directionX < 0) {
            catchX = ladderBlock.D() + 1.0 - playerInset;
        }
        if (directionZ > 0) {
            catchZ = ladderBlock.G() + playerInset;
        } else if (directionZ < 0) {
            catchZ = ladderBlock.G() + 1.0 - playerInset;
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

    private boolean intersectsBlockAfterPlacement(List<TrajectoryPoint> trajectory,
                                                   BlockData block, int firstTick, int lastTick) {
        int safetyLastTick = Math.min(lastTick + POST_CATCH_CLEARANCE_TICKS,
                trajectory.get(trajectory.size() - 1).tick);
        TrajectoryPoint previous = null;
        for (TrajectoryPoint point : trajectory) {
            if (point.tick < firstTick - 1) {
                continue;
            }
            if (point.tick > safetyLastTick) {
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
            double sweptMinX = Math.min(this.minX, next.minX);
            double sweptMaxX = Math.max(this.maxX, next.maxX);
            double sweptMinY = Math.min(this.minY, next.minY);
            double sweptMaxY = Math.max(this.maxY, next.maxY);
            double sweptMinZ = Math.min(this.minZ, next.minZ);
            double sweptMaxZ = Math.max(this.maxZ, next.maxZ);
            return sweptMaxX + horizontalMargin > block.D()
                    && sweptMinX - horizontalMargin < block.D() + 1.0
                    && sweptMaxY > block.B() && sweptMinY < block.B() + 1.0
                    && sweptMaxZ + horizontalMargin > block.G()
                    && sweptMinZ - horizontalMargin < block.G() + 1.0;
        }
    }
}
