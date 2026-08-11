package gg.vape.module.utility.clutch;

import gg.vape.module.blatant.Clutch;
import gg.vape.unmap.ItemLimitData;
import gg.vape.utils.BlockUtil;
import gg.vape.utils.FastAtanMath;
import gg.vape.utils.MathUtil;
import gg.vape.utils.RayTraceUtil;
import gg.vape.utils.RotationUtil;
import gg.vape.utils.Vec3d;
import gg.vape.utils.datas.BlockData;
import gg.vape.wrapper.impl.AxisAlignedBB;
import gg.vape.wrapper.impl.Block;
import gg.vape.wrapper.impl.BlockPos;
import gg.vape.wrapper.impl.Entity;
import gg.vape.wrapper.impl.EntityPlayerSP;
import gg.vape.wrapper.impl.EnumFacing;
import gg.vape.wrapper.impl.ForgeVersion;
import gg.vape.wrapper.impl.RayTraceResult;
import gg.vape.wrapper.impl.RayTraceResult_type;
import gg.vape.wrapper.impl.Vec3;
import gg.vape.wrapper.impl.Vec3i;
import gg.vape.wrapper.impl.World;
import java.util.List;
import java.util.Stack;
import java.util.Vector;

public class ClutchPlacementPathUtils {
    private static EnumFacing[] horizontalFacings;
    private static EnumFacing[] allFacings;

    private static Vec3[] createFaceCorners(int facingIndex, double minX, double minY, double minZ,
                                             double maxX, double maxY, double maxZ) {
        switch (facingIndex) {
            case 0:
                return new Vec3[]{Vec3.create(minX, minY, minZ), Vec3.create(minX, minY, maxZ), Vec3.create(maxX, minY, maxZ), Vec3.create(maxX, minY, minZ)};
            case 1:
                return new Vec3[]{Vec3.create(minX, maxY, minZ), Vec3.create(maxX, maxY, minZ), Vec3.create(maxX, maxY, maxZ), Vec3.create(minX, maxY, maxZ)};
            case 2:
                return new Vec3[]{Vec3.create(minX, minY, minZ), Vec3.create(maxX, minY, minZ), Vec3.create(maxX, maxY, minZ), Vec3.create(minX, maxY, minZ)};
            case 3:
                return new Vec3[]{Vec3.create(minX, minY, maxZ), Vec3.create(maxX, minY, maxZ), Vec3.create(maxX, maxY, maxZ), Vec3.create(minX, maxY, maxZ)};
            case 4:
                return new Vec3[]{Vec3.create(minX, minY, minZ), Vec3.create(minX, minY, maxZ), Vec3.create(minX, maxY, maxZ), Vec3.create(minX, maxY, minZ)};
            case 5:
                return new Vec3[]{Vec3.create(maxX, minY, minZ), Vec3.create(maxX, minY, maxZ), Vec3.create(maxX, maxY, maxZ), Vec3.create(maxX, maxY, minZ)};
            default:
                return new Vec3[0];
        }
    }

    private static boolean isExpectedBlockHit(RayTraceResult rayTrace, BlockData block, int facingIndex) {
        if (!rayTrace.isBlockHit() || rayTrace.getSideHit().Y() != facingIndex) {
            return false;
        }
        if (ForgeVersion.MC_1_7_10.Y()) {
            return rayTrace.getBlockPos().equals(BlockPos.d(block));
        }
        return rayTrace.g() == block.D() && rayTrace.T() == block.B() && rayTrace.a$src$I$8nuo9d() == block.G();
    }

    public static Vector<PlacementTarget> findPlacementPath(BlockData originBlock, Vec3 eyePosition, EntityPlayerSP player, World world, BlockData currentBlock, EnumFacing initialFacing, EnumFacing incomingFacing, BlockPathSearchStrategy<PlacementTarget> searchStrategy, int depth) {
        if (depth > searchStrategy.getMaxDepth() || !searchStrategy.canVisit(currentBlock)) {
            return null;
        }
        Block block = world.getBlockByPos(currentBlock.D(), currentBlock.B(), currentBlock.G());
        boolean isSolid = BlockUtil.J(block);
        boolean isReplaceable = BlockUtil.u(block);
        if (!isSolid) {
            if (isReplaceable && ClutchPlacementPathUtils.canRayTracePlacement(originBlock, player, world, currentBlock, incomingFacing.getOpposite())) {
                Vector<PlacementTarget> path = new Vector<PlacementTarget>();
                PlacementTarget target = new PlacementTarget(currentBlock, incomingFacing.getOpposite(), false);
                target.depth = depth;
                path.add(target);
                return path;
            }
            return null;
        }
        if (allFacings == null) {
            allFacings = EnumFacing.t();
            horizontalFacings = EnumFacing.c$src$ALgg_vape_wrapper_impl_EnumFacing_$1i3g4ft();
        }
        int horizontalIndex = initialFacing.c();
        if (horizontalIndex == -1) {
            horizontalIndex = 0;
        }
        int forward = horizontalFacings[horizontalIndex].Y();
        int right = horizontalFacings[(horizontalIndex + 1) % 4].Y();
        int left = horizontalFacings[(horizontalIndex + 3) % 4].Y();
        int backward = horizontalFacings[(horizontalIndex + 2) % 4].Y();
        int[] facingOrder = new int[]{0, forward, right, left, backward, 1};
        int bestScore = Integer.MAX_VALUE;
        Vector<PlacementTarget> bestPath = null;
        for (int facingIndex : facingOrder) {
            EnumFacing nextFacing = allFacings[facingIndex];
            if (nextFacing.Y() == initialFacing.getOpposite().Y()) continue;
            BlockData adjacentBlock = currentBlock.R(nextFacing);
            Block adjacentState = world.getBlockByPos(adjacentBlock.D(), adjacentBlock.B(), adjacentBlock.G());
            EnumFacing targetFacing = BlockUtil.u(adjacentState) ? null : nextFacing.getOpposite();
            if (!ClutchPlacementPathUtils.isBlockFaceVisible(eyePosition, world, adjacentBlock, targetFacing)) continue;
            Vector<PlacementTarget> candidatePath = new Vector<PlacementTarget>();
            if (searchStrategy.isValidBlock(adjacentBlock)) {
                boolean solid = BlockUtil.J(adjacentState);
                if (!solid && !ClutchPlacementPathUtils.canRayTracePlacement(originBlock, player, world, adjacentBlock, targetFacing)) continue;
                targetFacing = solid ? nextFacing.getOpposite() : targetFacing;
                PlacementTarget target = new PlacementTarget(adjacentBlock, targetFacing);
                target.depth = depth;
                candidatePath.addElement(target);
            } else {
                Vector<PlacementTarget> deeperPath = ClutchPlacementPathUtils.findPlacementPath(originBlock, eyePosition, player, world, adjacentBlock, initialFacing, nextFacing, searchStrategy, depth + 1);
                if (deeperPath != null && !deeperPath.isEmpty()) {
                    PlacementTarget target = new PlacementTarget(adjacentBlock, nextFacing.getOpposite());
                    target.depth = depth;
                    candidatePath.addAll(deeperPath);
                    candidatePath.add(target);
                }
            }
            if (candidatePath.isEmpty()) continue;
            int candidateScore = searchStrategy.scorePath(candidatePath);
            if (bestPath != null && candidateScore >= bestScore) continue;
            bestPath = candidatePath;
            bestScore = candidateScore;
        }
        return bestPath;
    }

    public static Vec3 findBestPlacementHitPoint(EntityPlayerSP player, World world, Vec3 eyePosition,
                                                   PlacementTarget placementTarget, float yaw, float pitch) {
        return findClosestPlacementHitPoint(player, world, eyePosition, placementTarget,
                yaw, pitch, true, Double.POSITIVE_INFINITY);
    }

    public static Vec3 findBestPlacementHitPointWithinReach(
            EntityPlayerSP player, World world, Vec3 eyePosition,
            PlacementTarget placementTarget, float yaw, float pitch, double maximumDistance) {
        return findClosestPlacementHitPoint(player, world, eyePosition, placementTarget,
                yaw, pitch, true, maximumDistance);
    }

    private static Vec3 findClosestPlacementHitPoint(EntityPlayerSP player, World world, Vec3 eyePosition,
                                                       PlacementTarget placementTarget, float yaw, float pitch,
                                                       boolean useRayTraceUtility, double maximumDistance) {
        BlockData supportBlock = placementTarget.supportBlock;
        EnumFacing facing = placementTarget.facing;
        AxisAlignedBB bounds = BlockUtil.F(world, supportBlock);
        if (facing == null) {
            return RotationUtil.M(eyePosition, bounds, 0.0, 0.0, 0.0).toVec3();
        }
        bounds = bounds.contract(0.002, 0.002, 0.002);
        double distanceLimit = maximumDistance + 1.0E-4;
        double dx = Math.max(0.0, Math.max(bounds.getMinX() - eyePosition.getX(),
                eyePosition.getX() - bounds.getMaxX()));
        double dy = Math.max(0.0, Math.max(bounds.getMinY() - eyePosition.getY(),
                eyePosition.getY() - bounds.getMaxY()));
        double dz = Math.max(0.0, Math.max(bounds.getMinZ() - eyePosition.getZ(),
                eyePosition.getZ() - bounds.getMaxZ()));
        if (dx * dx + dy * dy + dz * dz > distanceLimit * distanceLimit) {
            return null;
        }
        Vec3i direction = facing.getDirectionVector();
        double width = bounds.getMaxX() - bounds.getMinX();
        double depth = bounds.getMaxZ() - bounds.getMinZ();
        double height = bounds.getMaxY() - bounds.getMinY();
        double directionX = direction.getX();
        double directionY = direction.getY();
        double directionZ = direction.getZ();
        double bestRotationDistance = Double.MAX_VALUE;
        Vec3 bestHitPoint = null;
        Vec3d rotationOrigin = new Vec3d(eyePosition.getX(), eyePosition.getY(), eyePosition.getZ());
        int facingIndex = facing.Y();
        for (int horizontalInset = 0; horizontalInset <= 70; horizontalInset += 10) {
            double horizontalScale = 1.0 - (double)horizontalInset / 100.0;
            double insetX = width / 2.0 * horizontalScale;
            double insetZ = depth / 2.0 * horizontalScale;
            double minX = bounds.getMinX() + (directionX > 0.0 ? width : (directionX < 0.0 ? 0.0 : insetX));
            double minZ = bounds.getMinZ() + (directionZ > 0.0 ? depth : (directionZ < 0.0 ? 0.0 : insetZ));
            double maxX = bounds.getMaxX() - (directionX < 0.0 ? width : (directionX > 0.0 ? 0.0 : insetX));
            double maxZ = bounds.getMaxZ() - (directionZ < 0.0 ? depth : (directionZ > 0.0 ? 0.0 : insetZ));
            verticalLoop: for (int verticalInset = 0; verticalInset <= 70; verticalInset += 10) {
                double verticalScale = 1.0 - (double)verticalInset / 100.0;
                double insetY = height / 2.0 * verticalScale;
                double minY = bounds.getMinY() + (directionY > 0.0 ? height : (directionY < 0.0 ? 0.0 : insetY));
                double maxY = bounds.getMaxY() - (directionY < 0.0 ? height : (directionY > 0.0 ? 0.0 : insetY));
                Vec3[] corners = createFaceCorners(facingIndex, minX, minY, minZ, maxX, maxY, maxZ);
                for (Vec3 corner : corners) {
                    if (eyePosition.distanceTo(corner) > maximumDistance + 1.0E-4) {
                        continue;
                    }
                    Vec3d candidate = new Vec3d(corner.getX(), corner.getY(), corner.getZ());
                    double rotationDistance = calculateRotationDistance(rotationOrigin, candidate, yaw, pitch);
                    if (rotationDistance >= bestRotationDistance || rotationDistance <= 0.5) {
                        if (verticalInset == 0) continue verticalLoop;
                        continue;
                    }
                    RayTraceResult rayTrace = useRayTraceUtility
                            ? RayTraceUtil.b(eyePosition, corner, world, player, false, false, ForgeVersion.MC_1_16_5.v(), null)
                            : world.K(eyePosition, corner, false, false, ForgeVersion.MC_1_16_5.v(), player);
                    if (!isExpectedBlockHit(rayTrace, supportBlock, facingIndex)) continue;
                    bestRotationDistance = rotationDistance;
                    bestHitPoint = corner;
                    if (bestRotationDistance < 1.0) {
                        return bestHitPoint;
                    }
                }
            }
        }
        return bestHitPoint;
    }
    public static Vec3 findFirstPlacementHitPoint(EntityPlayerSP player, World world, Vec3 eyePosition, PlacementTarget placementTarget) {
        BlockData supportBlock = placementTarget.supportBlock;
        EnumFacing facing = placementTarget.facing;
        AxisAlignedBB bounds = BlockUtil.F(world, supportBlock);
        if (facing == null) {
            return RotationUtil.M(eyePosition, bounds, 0.0, 0.0, 0.0).toVec3();
        }
        Vec3i direction = facing.getDirectionVector();
        bounds = bounds.contract(0.002, 0.002, 0.002);
        double width = bounds.getMaxX() - bounds.getMinX();
        double depth = bounds.getMaxZ() - bounds.getMinZ();
        double height = bounds.getMaxY() - bounds.getMinY();
        double directionX = direction.getX();
        double directionY = direction.getY();
        double directionZ = direction.getZ();
        int facingIndex = facing.Y();
        for (int insetPercent = 0; insetPercent <= 100; insetPercent += 10) {
            double scale = 1.0 - (double)insetPercent / 100.0;
            double insetX = width / 2.0 * scale;
            double insetY = depth / 2.0 * scale;
            double insetZ = height / 2.0 * scale;
            double minX = bounds.getMinX() + (directionX > 0.0 ? width : (directionX < 0.0 ? 0.0 : insetX));
            double minY = bounds.getMinY() + (directionY > 0.0 ? height : (directionY < 0.0 ? 0.0 : insetY));
            double minZ = bounds.getMinZ() + (directionZ > 0.0 ? depth : (directionZ < 0.0 ? 0.0 : insetZ));
            double maxX = bounds.getMaxX() - (directionX < 0.0 ? width : (directionX > 0.0 ? 0.0 : insetX));
            double maxY = bounds.getMaxY() - (directionY < 0.0 ? height : (directionY > 0.0 ? 0.0 : insetY));
            double maxZ = bounds.getMaxZ() - (directionZ < 0.0 ? depth : (directionZ > 0.0 ? 0.0 : insetZ));
            Vec3[] corners = createFaceCorners(facingIndex, minX, minY, minZ, maxX, maxY, maxZ);
            for (Vec3 corner : corners) {
                RayTraceResult rayTrace = world.K(eyePosition, corner, false, false, ForgeVersion.MC_1_16_5.v(), player);
                if (isExpectedBlockHit(rayTrace, supportBlock, facingIndex)) {
                    return corner;
                }
            }
        }
        return null;
    }

    public static Stack<BlockPlacementNode> findBlockPlacementPath(BlockData currentBlock, EnumFacing incomingFacing, EnumFacing initialFacing, BlockPathSearchStrategy<BlockPlacementNode> searchStrategy, int depth) {
        if (depth > searchStrategy.getMaxDepth() || !searchStrategy.canVisit(currentBlock)) {
            return null;
        }
        if (allFacings == null) {
            allFacings = EnumFacing.t();
            horizontalFacings = EnumFacing.c$src$ALgg_vape_wrapper_impl_EnumFacing_$1i3g4ft();
        }
        int horizontalIndex = initialFacing.c();
        int forward = horizontalFacings[horizontalIndex].Y();
        int left = horizontalFacings[horizontalIndex == 0 ? 3 : horizontalIndex - 1].Y();
        int right = horizontalFacings[(horizontalIndex + 1) % 4].Y();
        int down = 0;
        int[] facingOrder = new int[]{forward, left, right, down};
        if (incomingFacing != null && incomingFacing.Y() != 0 && incomingFacing.Y() != initialFacing.Y()) {
            facingOrder = new int[]{forward};
        }
        EnumFacing reverseIncomingFacing = incomingFacing == null ? null : incomingFacing.getOpposite();
        int bestScore = Integer.MAX_VALUE;
        Stack<BlockPlacementNode> bestPath = null;
        for (int facingIndex : facingOrder) {
            EnumFacing nextFacing = allFacings[facingIndex];
            if (reverseIncomingFacing != null && nextFacing.Y() == reverseIncomingFacing.Y() || nextFacing.Y() == initialFacing.getOpposite().Y()) continue;
            BlockData adjacentBlock = currentBlock.R(nextFacing);
            boolean useSupportBlock = incomingFacing == null || incomingFacing.Y() != 0;
            Stack<BlockPlacementNode> candidatePath = new Stack<BlockPlacementNode>();
            if (searchStrategy.isValidBlock(adjacentBlock)) {
                BlockPlacementNode node = new BlockPlacementNode(currentBlock, incomingFacing == null ? null : reverseIncomingFacing, useSupportBlock);
                node.baseFacing = nextFacing;
                candidatePath.add(node);
            } else {
                Stack<BlockPlacementNode> deeperPath = ClutchPlacementPathUtils.findBlockPlacementPath(adjacentBlock, nextFacing, initialFacing, searchStrategy, depth + 1);
                if (deeperPath != null && !deeperPath.isEmpty()) {
                    BlockPlacementNode node = new BlockPlacementNode(currentBlock, incomingFacing == null ? null : reverseIncomingFacing, useSupportBlock);
                    node.baseFacing = nextFacing;
                    candidatePath.addAll(deeperPath);
                    candidatePath.add(node);
                }
            }
            if (candidatePath.isEmpty()) continue;
            int candidateScore = searchStrategy.scorePath(candidatePath);
            if (bestPath != null && candidateScore >= bestScore) continue;
            bestPath = candidatePath;
            bestScore = candidateScore;
        }
        return bestPath;
    }


    public static float calculateRotationDistance(Vec3d source, Vec3d target, float yaw, float pitch) {
        double deltaX = source.getX() - target.getX();
        double deltaZ = source.getZ() - target.getZ();
        double deltaY = source.getY() - target.getY();
        double horizontalDistance = MathUtil.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        float yawDelta = (float)RotationUtil.N(source.getX(), source.getZ(), yaw, target.getX(), target.getZ());
        float targetPitch = MathUtil.wrapAngleTo180((float)Math.toDegrees(FastAtanMath.atan2Approximation((float)deltaY, (float)horizontalDistance)));
        float absoluteYawDelta = Math.abs(MathUtil.wrapAngleTo180(yawDelta));
        float absolutePitchDelta = Math.abs(MathUtil.wrapAngleTo180(targetPitch - pitch));
        return (float)Math.sqrt(absoluteYawDelta * absoluteYawDelta + absolutePitchDelta * absolutePitchDelta);
    }

    public static boolean isEntityPathClear(World world, Entity sourceEntity, AxisAlignedBB searchBounds, Vec3 rayStart, Vec3 rayEnd) {
        List<?> nearbyEntities = world.F(sourceEntity, searchBounds);
        if (!nearbyEntities.isEmpty()) {
            for (Object handle : nearbyEntities) {
                if (handle == null) continue;
                Entity candidate = new Entity(handle);
                if (!candidate.n$src$Z$fx7gig()) continue;
                float collisionBorder = candidate.b();
                AxisAlignedBB collisionBounds = candidate.R$src$Lgg_vape_wrapper_impl_AxisAlignedBB_$r19dfl().expand(collisionBorder, collisionBorder, collisionBorder);
                RayTraceResult intercept = collisionBounds.calculateIntercept(rayStart, rayEnd);
                if (!collisionBounds.isVecInside(rayStart) && (intercept == null || !intercept.isNotNull())) continue;
                return false;
            }
        }
        return true;
    }

    public static boolean isBlockFaceVisible(Vec3 eyePosition, World world, BlockData blockData, EnumFacing facing) {
        if (facing != null) {
            int facingIndex = facing.Y();
            AxisAlignedBB bounds = BlockUtil.F(world, blockData);
            double eyeX = eyePosition.getX();
            double eyeZ = eyePosition.getZ();
            double eyeY = eyePosition.getY();
            switch (facingIndex) {
                case 1: {
                    return eyeY > bounds.getMaxY();
                }
                case 0: {
                    return bounds.getMinY() > eyeY;
                }
                case 2: {
                    return bounds.getMinZ() > eyeZ;
                }
                case 3: {
                    return eyeZ > bounds.getMaxZ();
                }
                case 4: {
                    return bounds.getMinX() > eyeX;
                }
                case 5: {
                    return eyeX > bounds.getMaxX();
                }
            }
            return false;
        }
        return true;
    }

    public static boolean isBlacklistedPlacementBlock(Block block) {
        for (ItemLimitData itemLimitData : ItemLimitData.INTERACTIVE_BLOCKS) {
            if (!block.U().equalsIgnoreCase(itemLimitData.getName())) continue;
            return true;
        }
        return false;
    }

    public static Vector<PlacementTarget> findDirectPlacementPath(BlockData currentBlock, BlockData targetBlock, BlockPathSearchStrategy<PlacementTarget> searchStrategy, int depth) {
        if (depth > searchStrategy.getMaxDepth()) {
            return null;
        }
        if (allFacings == null) {
            allFacings = EnumFacing.t();
            horizontalFacings = EnumFacing.c$src$ALgg_vape_wrapper_impl_EnumFacing_$1i3g4ft();
        }
        int deltaX = targetBlock.D() - currentBlock.D();
        int deltaY = targetBlock.B() - currentBlock.B();
        int deltaZ = targetBlock.G() - currentBlock.G();
        int xFacing = deltaX > 0 ? 5 : (deltaX < 0 ? 4 : -1);
        int yFacing = deltaY > 0 ? 1 : (deltaY < 0 ? 0 : -1);
        int zFacing = deltaZ > 0 ? 3 : (deltaZ < 0 ? 2 : -1);
        int[] facingOrder = new int[]{xFacing, yFacing, zFacing};
        int bestScore = Integer.MAX_VALUE;
        Vector<PlacementTarget> bestPath = null;
        for (int facingIndex : facingOrder) {
            if (facingIndex == -1) continue;
            EnumFacing facing = allFacings[facingIndex];
            BlockData adjacentBlock = currentBlock.R(facing);
            Vector<PlacementTarget> candidatePath = new Vector<PlacementTarget>();
            if (searchStrategy.isValidBlock(adjacentBlock)) {
                if (adjacentBlock.equals(targetBlock)) {
                    PlacementTarget target = new PlacementTarget(currentBlock, facing);
                    target.depth = depth;
                    candidatePath.addElement(target);
                } else {
                    Vector<PlacementTarget> deeperPath = ClutchPlacementPathUtils.findDirectPlacementPath(adjacentBlock, targetBlock, searchStrategy, depth + 1);
                    if (deeperPath != null && !deeperPath.isEmpty()) {
                        PlacementTarget target = new PlacementTarget(currentBlock, facing);
                        target.depth = depth;
                        candidatePath.addAll(deeperPath);
                        candidatePath.add(target);
                    }
                }
            }
            if (candidatePath.isEmpty()) continue;
            if (depth == 0) {
                Clutch.DEBUG_PLACEMENT_PATHS.add(new Vector(candidatePath));
            }
            int candidateScore = searchStrategy.scorePath(candidatePath, depth);
            if (bestPath != null && candidateScore >= bestScore) continue;
            bestPath = candidatePath;
            bestScore = candidateScore;
        }
        return bestPath;
    }


    public static boolean isPlacementSpaceClear(World world, Entity sourceEntity, BlockData blockData) {
        AxisAlignedBB placementBounds = BlockUtil.F(world, blockData);
        List<?> nearbyEntities = world.F(sourceEntity, placementBounds);
        if (!nearbyEntities.isEmpty()) {
            for (Object handle : nearbyEntities) {
                if (handle == null) continue;
                Entity candidate = new Entity(handle);
                if (candidate.M$src$Z$ff28xj() || !candidate.t$src$Z$g0i82m() || candidate.equals(sourceEntity)) continue;
                AxisAlignedBB candidateBounds = candidate.R$src$Lgg_vape_wrapper_impl_AxisAlignedBB_$r19dfl();
                if (!candidateBounds.intersects(placementBounds)) continue;
                return false;
            }
        }
        return true;
    }

    public static Vec3 findPlacementHitPoint(EntityPlayerSP player, World world, Vec3 eyePosition,
                                               PlacementTarget placementTarget, float yaw, float pitch) {
        return findClosestPlacementHitPoint(player, world, eyePosition, placementTarget,
                yaw, pitch, false, Double.POSITIVE_INFINITY);
    }
    public static boolean canRayTracePlacement(BlockData sourceBlock, EntityPlayerSP player, World world, BlockData targetBlock, EnumFacing facing) {
        AxisAlignedBB targetBounds = BlockUtil.F(world, targetBlock).contract(0.002, 0.002, 0.002);
        double legacyEyeOffset = ForgeVersion.MC_1_7_10.Y() ? player.X() : 0.0;
        Vec3 rayStart = Vec3.create(sourceBlock.D() + 0.5, player.A() + legacyEyeOffset, sourceBlock.G() + 0.5);
        if (facing == null) {
            Vec3 rayEnd = RotationUtil.T(player, targetBounds, 0.0, 0.0, 0.0).toVec3();
            RayTraceResult rayTrace = world.K(rayStart, rayEnd, false, false, ForgeVersion.MC_1_16_5.v(), player);
            return isMissOrTargetBlock(rayTrace, targetBlock);
        }
        Vec3[] faceCorners = createFaceCorners(facing.Y(), targetBounds.getMinX(), targetBounds.getMinY(), targetBounds.getMinZ(),
                targetBounds.getMaxX(), targetBounds.getMaxY(), targetBounds.getMaxZ());
        for (Vec3 corner : faceCorners) {
            RayTraceResult rayTrace = world.K(rayStart, corner, false, false, ForgeVersion.MC_1_16_5.v(), player);
            if (isMissOrTargetBlock(rayTrace, targetBlock)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isMissOrTargetBlock(RayTraceResult rayTrace, BlockData targetBlock) {
        if (rayTrace.isNull() || rayTrace.getTypeOfHit().equals(RayTraceResult_type.miss())) {
            return true;
        }
        if (!rayTrace.isBlockHit()) {
            return false;
        }
        if (ForgeVersion.MC_1_7_10.Y()) {
            return rayTrace.getBlockPos().equals(BlockPos.d(targetBlock));
        }
        return rayTrace.g() == targetBlock.D() && rayTrace.T() == targetBlock.B() && rayTrace.a$src$I$8nuo9d() == targetBlock.G();
    }
}
