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

/** Receding-horizon movement controller used while creating and catching the ladder. */
public final class AutoLadderMovementController {
    private static final double LEGACY_LADDER_THICKNESS = 0.125;
    private static final double MODERN_LADDER_THICKNESS = 0.1875;
    private static final CatchInput[] INPUTS = new CatchInput[]{
            new CatchInput(false, false, false, false),
            new CatchInput(true, false, false, false),
            new CatchInput(false, true, false, false),
            new CatchInput(false, false, true, false),
            new CatchInput(false, false, false, true),
            new CatchInput(true, false, true, false),
            new CatchInput(true, false, false, true),
            new CatchInput(false, true, true, false),
            new CatchInput(false, true, false, true)
    };

    private AutoLadderMovementController() {
    }

    public static CatchInput choose(EntityPlayerSP player, World world, AutoLadderPlan plan) {
        if (isSecureCatch(player, world, plan)) {
            return INPUTS[0];
        }
        int horizon = estimateCatchHorizon(player, plan);
        CatchInput bestInput = INPUTS[0];
        double bestScore = Double.POSITIVE_INFINITY;
        for (CatchInput input : INPUTS) {
            double score = simulateInput(player, world, plan, input, horizon);
            if (score < bestScore) {
                bestScore = score;
                bestInput = input;
            }
        }
        return bestInput;
    }

    private static double simulateInput(EntityPlayerSP player, World world, AutoLadderPlan plan,
                                        CatchInput input, int horizon) {
        BlockPlacementGraph graph = new BlockPlacementGraph(player);
        BlockPathPlanner simulation = new BlockPathPlanner(player, player, world, graph);
        simulation.applySnapshot(graph);
        simulation.setInput(input.forward, input.backward, input.left, input.right, false, false);
        EntityPlayer simulatedPlayer = simulation.getSimulatedPlayer();
        int firstSecureCatchTick = -1;
        int secureCatchTicks = 0;
        double bestInterceptScore = Double.POSITIVE_INFINITY;
        for (int tick = 1; tick <= horizon; ++tick) {
            simulation.simulateTick(false);
            double deltaX = plan.getCatchX() - simulatedPlayer.z();
            double deltaZ = plan.getCatchZ() - simulatedPlayer.h();
            double horizontalDistanceSq = deltaX * deltaX + deltaZ * deltaZ;
            int playerBlockY = (int)Math.floor(simulatedPlayer.N() + 1.0E-4);
            int ladderY = plan.getLadderBlock().B();
            double verticalMiss = playerBlockY == ladderY ? 0.0 : Math.abs(playerBlockY - ladderY);
            if (isSecureCatch(simulatedPlayer, world, plan)) {
                if (firstSecureCatchTick == -1) {
                    firstSecureCatchTick = tick;
                }
                ++secureCatchTicks;
            }
            if (firstSecureCatchTick == -1 && simulatedPlayer.b$src$Z$fqlxe4()) {
                return Double.POSITIVE_INFINITY;
            }
            double interceptScore = horizontalDistanceSq * 1000.0
                    + verticalMiss * 20000.0 + tick * 10.0;
            if (firstSecureCatchTick == -1) {
                interceptScore += supportTopRisk(simulatedPlayer, plan.getSupportBlock());
            }
            bestInterceptScore = Math.min(bestInterceptScore, interceptScore);
        }

        double deltaX = plan.getCatchX() - simulatedPlayer.z();
        double deltaZ = plan.getCatchZ() - simulatedPlayer.h();
        double horizontalDistanceSq = deltaX * deltaX + deltaZ * deltaZ;
        double velocityTowardTarget = simulatedPlayer.t() * deltaX + simulatedPlayer.T() * deltaZ;
        double overshootPenalty = Math.max(0.0, -velocityTowardTarget) * 2500.0;
        if (firstSecureCatchTick != -1) {
            return -150000.0 + firstSecureCatchTick * 10000.0 - secureCatchTicks * 2000.0
                    + horizontalDistanceSq * 100.0 + overshootPenalty;
        }
        if (simulatedPlayer.N() < plan.getLadderBlock().B() - 0.05) {
            return Double.POSITIVE_INFINITY;
        }
        return bestInterceptScore + horizontalDistanceSq * 200.0 + overshootPenalty;
    }

    private static boolean isSecureCatch(EntityPlayer player, World world, AutoLadderPlan plan) {
        return player.boolean_S() && isCatchableNow(player, world, plan);
    }

    private static int estimateCatchHorizon(EntityPlayerSP player, AutoLadderPlan plan) {
        double y = player.N();
        double motionY = player.q();
        double passedLadderY = plan.getLadderBlock().B() - 0.05;
        for (int tick = 1; tick <= 6; ++tick) {
            y += motionY;
            if (y < passedLadderY) {
                return tick;
            }
            motionY = (motionY - 0.08) * 0.98;
        }
        return 6;
    }

    public static boolean isCatchableNow(EntityPlayer player, World world, AutoLadderPlan plan) {
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
        int directionZ = facing.getDirectionVector().getZ();
        if (!actual.isNull()) {
            double normalSize = directionX == 0
                    ? actual.getMaxZ() - actual.getMinZ()
                    : actual.getMaxX() - actual.getMinX();
            if (normalSize > 0.0 && normalSize <= 0.25) {
                return actual;
            }
        }
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

    private static double supportTopRisk(EntityPlayer player, BlockData support) {
        AxisAlignedBB bounds = player.u$src$Lgg_vape_wrapper_impl_AxisAlignedBB_$kogbsu();
        boolean horizontalOverlap = bounds.getMaxX() > support.D()
                && bounds.getMinX() < support.D() + 1.0
                && bounds.getMaxZ() > support.G()
                && bounds.getMinZ() < support.G() + 1.0;
        if (!horizontalOverlap) {
            return 0.0;
        }
        double verticalGap = Math.max(0.0, player.N() - (support.B() + 1.0));
        return 50000.0 / Math.max(0.2, verticalGap + 0.2);
    }

    public static void apply(CatchInput input) {
        MovementInputHelper.synchronizeDirectionalInput(
                input.forward, input.backward, input.left, input.right);
        MovementInputHelper.setJumpPressed(false);
        GameSettings settings = Minecraft.gameSettings();
        MovementInputHelper.synchronizeKeyState(
                settings.d$src$Lgg_vape_wrapper_impl_KeyBinding_$adn2z0(), false);
    }

    public static final class CatchInput {
        private final boolean forward;
        private final boolean backward;
        private final boolean left;
        private final boolean right;

        private CatchInput(boolean forward, boolean backward, boolean left, boolean right) {
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
