package gg.vape.module.blatant.autoladder;

import gg.vape.module.blatant.blockin.BlockPathPlanner;
import gg.vape.module.blatant.blockin.BlockPlacementGraph;
import gg.vape.movement.MovementInputHelper;
import gg.vape.utils.BlockUtil;
import gg.vape.utils.datas.BlockData;
import gg.vape.wrapper.impl.AxisAlignedBB;
import gg.vape.wrapper.impl.EntityPlayer;
import gg.vape.wrapper.impl.EntityPlayerSP;
import gg.vape.wrapper.impl.EnumFacing;
import gg.vape.wrapper.impl.ForgeVersion;
import gg.vape.wrapper.impl.GameSettings;
import gg.vape.wrapper.impl.Minecraft;
import gg.vape.wrapper.impl.World;

/** Receding-horizon movement controller that safely enters the ladder face. */
public final class AutoLadderMovementController {
    private static final double LEGACY_LADDER_THICKNESS = 0.125;
    private static final double MODERN_LADDER_THICKNESS = 0.1875;
    private static final double TOP_CLEARANCE = 0.004;
    private static final double LADDER_CONTACT_OVERLAP = 0.03;
    private static final double TOP_COLLISION_MARGIN = 0.002;
    private static final double SUPPORT_COLLISION_MARGIN = 0.02;
    private static final double UNSAFE_SCORE = 1.0E12;
    private static final CenterInput[] INPUTS = new CenterInput[]{
            new CenterInput(false, false, false, false),
            new CenterInput(true, false, false, false),
            new CenterInput(false, true, false, false),
            new CenterInput(false, false, true, false),
            new CenterInput(false, false, false, true),
            new CenterInput(true, false, true, false),
            new CenterInput(true, false, false, true),
            new CenterInput(false, true, true, false),
            new CenterInput(false, true, false, true)
    };

    private AutoLadderMovementController() {
    }

    public static CenterInput chooseCentering(EntityPlayerSP player, World world,
                                              AutoLadderPlan plan) {
        if (player.boolean_S()) {
            return INPUTS[0];
        }
        CenterInput bestInput = INPUTS[0];
        double bestScore = Double.POSITIVE_INFINITY;
        for (CenterInput input : INPUTS) {
            double score = simulateCenteringInput(
                    player, world, input, plan);
            if (score < bestScore) {
                bestScore = score;
                bestInput = input;
            }
        }
        return bestInput;
    }

    private static double simulateCenteringInput(EntityPlayerSP player, World world,
                                                 CenterInput input,
                                                 AutoLadderPlan plan) {
        BlockPlacementGraph graph = new BlockPlacementGraph(player);
        BlockPathPlanner simulation = new BlockPathPlanner(player, player, world, graph);
        simulation.applySnapshot(graph);
        simulation.setInput(input.forward, input.backward, input.left, input.right, false, false);
        EntityPlayer simulatedPlayer = simulation.getSimulatedPlayer();
        AxisAlignedBB previousBounds =
                simulatedPlayer.u$src$Lgg_vape_wrapper_impl_AxisAlignedBB_$kogbsu();
        double bestDistanceSq = Double.POSITIVE_INFINITY;
        for (int tick = 1; tick <= 2; ++tick) {
            simulation.simulateTick(false);
            AxisAlignedBB currentBounds =
                    simulatedPlayer.u$src$Lgg_vape_wrapper_impl_AxisAlignedBB_$kogbsu();
            if (crossesUnsafeTop(previousBounds, currentBounds, world, plan)) {
                return UNSAFE_SCORE + tick * 1000.0;
            }
            boolean captured = simulatedPlayer.boolean_S()
                    || isLadderContact(simulatedPlayer, world, plan);
            if (simulatedPlayer.b$src$Z$fqlxe4() && !captured) {
                return UNSAFE_SCORE + tick * 2000.0;
            }
            double[] target = getMovementTarget(simulatedPlayer, world,
                    plan.getLadderBlock(), plan.getLadderFacing());
            double deltaX = target[0] - simulatedPlayer.z();
            double deltaZ = target[1] - simulatedPlayer.h();
            bestDistanceSq = Math.min(bestDistanceSq,
                    deltaX * deltaX + deltaZ * deltaZ);
            if (captured) {
                return -1000000.0 + tick * 10000.0 + bestDistanceSq * 1000.0;
            }
            previousBounds = currentBounds;
        }
        double[] target = getMovementTarget(simulatedPlayer, world,
                plan.getLadderBlock(), plan.getLadderFacing());
        double deltaX = target[0] - simulatedPlayer.z();
        double deltaZ = target[1] - simulatedPlayer.h();
        double finalDistanceSq = deltaX * deltaX + deltaZ * deltaZ;
        double horizontalSpeedSq = simulatedPlayer.t() * simulatedPlayer.t()
                + simulatedPlayer.T() * simulatedPlayer.T();
        double velocityTowardCenter = simulatedPlayer.t() * deltaX
                + simulatedPlayer.T() * deltaZ;
        double overshootPenalty = Math.max(0.0, -velocityTowardCenter) * 4000.0;
        return finalDistanceSq * 10000.0 + bestDistanceSq * 1000.0
                + horizontalSpeedSq * 150.0 + overshootPenalty;
    }

    static double[] getMovementTarget(EntityPlayer player, World world,
                                      BlockData ladder, EnumFacing facing) {
        AxisAlignedBB playerBounds =
                player.u$src$Lgg_vape_wrapper_impl_AxisAlignedBB_$kogbsu();
        AxisAlignedBB ladderBounds = getLadderBounds(world, ladder, facing);
        int directionX = facing.getDirectionVector().getX();
        int directionZ = facing.getDirectionVector().getZ();
        boolean aboveLadderTop = playerBounds.getMinY() >= ladder.B() + 1.0 - 1.0E-4;
        double normalSpacing = aboveLadderTop ? TOP_CLEARANCE : -LADDER_CONTACT_OVERLAP;
        double targetX = ladder.D() + 0.5;
        double targetZ = ladder.G() + 0.5;
        if (directionX != 0) {
            double halfWidth = (playerBounds.getMaxX() - playerBounds.getMinX()) / 2.0;
            double boundary = directionX > 0
                    ? ladderBounds.getMaxX() : ladderBounds.getMinX();
            targetX = boundary + directionX * (halfWidth + normalSpacing);
        } else if (directionZ != 0) {
            double halfWidth = (playerBounds.getMaxZ() - playerBounds.getMinZ()) / 2.0;
            double boundary = directionZ > 0
                    ? ladderBounds.getMaxZ() : ladderBounds.getMinZ();
            targetZ = boundary + directionZ * (halfWidth + normalSpacing);
        }
        targetX = Math.max(ladder.D() + 0.02, Math.min(ladder.D() + 0.98, targetX));
        targetZ = Math.max(ladder.G() + 0.02, Math.min(ladder.G() + 0.98, targetZ));
        return new double[]{targetX, targetZ};
    }

    private static boolean isLadderContact(EntityPlayer player, World world,
                                           AutoLadderPlan plan) {
        AxisAlignedBB playerBounds =
                player.u$src$Lgg_vape_wrapper_impl_AxisAlignedBB_$kogbsu();
        AxisAlignedBB ladderBounds = getLadderBounds(
                world, plan.getLadderBlock(), plan.getLadderFacing());
        boolean centerInsideCell = player.z() > plan.getLadderBlock().D()
                && player.z() < plan.getLadderBlock().D() + 1.0
                && player.h() > plan.getLadderBlock().G()
                && player.h() < plan.getLadderBlock().G() + 1.0;
        return centerInsideCell
                && intersects(playerBounds.getMinX(), playerBounds.getMaxX(),
                ladderBounds.getMinX(), ladderBounds.getMaxX())
                && intersects(playerBounds.getMinY(), playerBounds.getMaxY(),
                ladderBounds.getMinY(), ladderBounds.getMaxY())
                && intersects(playerBounds.getMinZ(), playerBounds.getMaxZ(),
                ladderBounds.getMinZ(), ladderBounds.getMaxZ());
    }

    private static boolean crossesUnsafeTop(AxisAlignedBB previous, AxisAlignedBB current,
                                            World world, AutoLadderPlan plan) {
        double top = plan.getLadderBlock().B() + 1.0;
        if (previous.getMinY() < top || current.getMinY() >= top) {
            return false;
        }
        double verticalDelta = current.getMinY() - previous.getMinY();
        double progress = Math.abs(verticalDelta) < 1.0E-9
                ? 0.0 : (top - previous.getMinY()) / verticalDelta;
        progress = Math.max(0.0, Math.min(1.0, progress));
        double minX = lerp(previous.getMinX(), current.getMinX(), progress);
        double maxX = lerp(previous.getMaxX(), current.getMaxX(), progress);
        double minZ = lerp(previous.getMinZ(), current.getMinZ(), progress);
        double maxZ = lerp(previous.getMaxZ(), current.getMaxZ(), progress);
        AxisAlignedBB ladderBounds = getLadderBounds(
                world, plan.getLadderBlock(), plan.getLadderFacing());
        if (horizontalIntersects(minX, maxX, minZ, maxZ,
                ladderBounds, TOP_COLLISION_MARGIN)) {
            return true;
        }
        BlockData support = plan.getSupportBlock();
        return maxX + SUPPORT_COLLISION_MARGIN > support.D()
                && minX - SUPPORT_COLLISION_MARGIN < support.D() + 1.0
                && maxZ + SUPPORT_COLLISION_MARGIN > support.G()
                && minZ - SUPPORT_COLLISION_MARGIN < support.G() + 1.0;
    }

    private static boolean horizontalIntersects(double minX, double maxX,
                                                double minZ, double maxZ,
                                                AxisAlignedBB target, double margin) {
        return maxX + margin > target.getMinX()
                && minX - margin < target.getMaxX()
                && maxZ + margin > target.getMinZ()
                && minZ - margin < target.getMaxZ();
    }

    private static double lerp(double start, double end, double progress) {
        return start + (end - start) * progress;
    }

    public static boolean isInsideLadderBounds(EntityPlayer player, World world,
                                               AutoLadderPlan plan) {
        AxisAlignedBB bounds = player.u$src$Lgg_vape_wrapper_impl_AxisAlignedBB_$kogbsu();
        AxisAlignedBB ladderBounds = getLadderBounds(world, plan.getLadderBlock(),
                plan.getLadderFacing());
        return intersects(bounds.getMinX(), bounds.getMaxX(),
                ladderBounds.getMinX(), ladderBounds.getMaxX())
                && intersects(bounds.getMinY(), bounds.getMaxY(),
                ladderBounds.getMinY(), ladderBounds.getMaxY())
                && intersects(bounds.getMinZ(), bounds.getMaxZ(),
                ladderBounds.getMinZ(), ladderBounds.getMaxZ());
    }

    static double getLadderThickness() {
        return ForgeVersion.MC_1_16_5.d()
                ? MODERN_LADDER_THICKNESS : LEGACY_LADDER_THICKNESS;
    }

    private static AxisAlignedBB getLadderBounds(World world, BlockData ladder,
                                                  EnumFacing facing) {
        AxisAlignedBB actual = BlockUtil.F(world, ladder);
        int directionX = facing.getDirectionVector().getX();
        if (!actual.isNull()) {
            double normalSize = directionX == 0
                    ? actual.getMaxZ() - actual.getMinZ()
                    : actual.getMaxX() - actual.getMinX();
            if (normalSize > 0.0 && normalSize <= 0.25) {
                return actual;
            }
        }
        return getExpectedLadderBounds(ladder, facing);
    }

    static AxisAlignedBB getExpectedLadderBounds(BlockData ladder, EnumFacing facing) {
        int directionX = facing.getDirectionVector().getX();
        int directionZ = facing.getDirectionVector().getZ();
        double thickness = getLadderThickness();
        double minX = ladder.D();
        double maxX = ladder.D() + 1.0;
        double minZ = ladder.G();
        double maxZ = ladder.G() + 1.0;
        if (directionX > 0) {
            maxX = minX + thickness;
        } else if (directionX < 0) {
            minX = maxX - thickness;
        } else if (directionZ > 0) {
            maxZ = minZ + thickness;
        } else if (directionZ < 0) {
            minZ = maxZ - thickness;
        }
        return AxisAlignedBB.create(minX, ladder.B(), minZ,
                maxX, ladder.B() + 1.0, maxZ);
    }

    private static boolean intersects(double minimum, double maximum,
                                      double targetMinimum, double targetMaximum) {
        return maximum > targetMinimum && minimum < targetMaximum;
    }

    public static void apply(CenterInput input) {
        MovementInputHelper.synchronizeDirectionalInput(
                input.forward, input.backward, input.left, input.right);
        MovementInputHelper.setJumpPressed(false);
        GameSettings settings = Minecraft.gameSettings();
        MovementInputHelper.synchronizeKeyState(
                settings.d$src$Lgg_vape_wrapper_impl_KeyBinding_$adn2z0(), false);
    }

    public static void apply(AutoLadderFallAdjustment adjustment) {
        if (!adjustment.overridesInput()) {
            MovementInputHelper.restorePhysicalInput(false);
            return;
        }
        MovementInputHelper.synchronizeDirectionalInput(
                adjustment.isForward(), adjustment.isBackward(),
                adjustment.isLeft(), adjustment.isRight());
        MovementInputHelper.setJumpPressed(false);
        GameSettings settings = Minecraft.gameSettings();
        MovementInputHelper.synchronizeKeyState(
                settings.d$src$Lgg_vape_wrapper_impl_KeyBinding_$adn2z0(), false);
    }

    public static final class CenterInput {
        private final boolean forward;
        private final boolean backward;
        private final boolean left;
        private final boolean right;

        private CenterInput(boolean forward, boolean backward, boolean left, boolean right) {
            this.forward = forward;
            this.backward = backward;
            this.left = left;
            this.right = right;
        }

        public String describe() {
            return "F=" + this.forward + ",B=" + this.backward
                    + ",L=" + this.left + ",R=" + this.right;
        }

    }

}
