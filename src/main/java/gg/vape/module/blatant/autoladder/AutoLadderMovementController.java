package gg.vape.module.blatant.autoladder;

import gg.vape.module.blatant.blockin.BlockPathPlanner;
import gg.vape.module.blatant.blockin.BlockPlacementGraph;
import gg.vape.movement.MovementInputHelper;
import gg.vape.utils.datas.BlockData;
import gg.vape.wrapper.impl.AxisAlignedBB;
import gg.vape.wrapper.impl.EntityPlayer;
import gg.vape.wrapper.impl.EntityPlayerSP;
import gg.vape.wrapper.impl.GameSettings;
import gg.vape.wrapper.impl.Minecraft;
import gg.vape.wrapper.impl.World;

/** Receding-horizon movement controller used while creating and catching the ladder. */
public final class AutoLadderMovementController {
    private static final double LADDER_THICKNESS = 0.1875;
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
        if (player.boolean_S() || isCatchableNow(player, plan)) {
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
        int firstCatchTick = -1;
        int caughtTicks = 0;
        double bestInterceptScore = Double.POSITIVE_INFINITY;
        for (int tick = 1; tick <= horizon; ++tick) {
            simulation.simulateTick(false);
            double deltaX = plan.getCatchX() - simulatedPlayer.z();
            double deltaZ = plan.getCatchZ() - simulatedPlayer.h();
            double horizontalDistanceSq = deltaX * deltaX + deltaZ * deltaZ;
            int playerBlockY = (int)Math.floor(simulatedPlayer.N() + 1.0E-4);
            int ladderY = plan.getLadderBlock().B();
            double verticalMiss = playerBlockY == ladderY ? 0.0 : Math.abs(playerBlockY - ladderY);
            bestInterceptScore = Math.min(bestInterceptScore,
                    horizontalDistanceSq * 1000.0 + verticalMiss * 20000.0 + tick * 10.0);
            if (simulatedPlayer.boolean_S()
                    && simulatedPlayer.getFallDistance() <= 0.5f) {
                if (firstCatchTick == -1) {
                    firstCatchTick = tick;
                }
                ++caughtTicks;
            }
            if (firstCatchTick == -1 && simulatedPlayer.b$src$Z$fqlxe4()) {
                return Double.POSITIVE_INFINITY;
            }
            if (firstCatchTick == -1 && isAboveSupportTop(simulatedPlayer, plan.getSupportBlock())) {
                bestInterceptScore += 250000.0;
            }
        }

        double deltaX = plan.getCatchX() - simulatedPlayer.z();
        double deltaZ = plan.getCatchZ() - simulatedPlayer.h();
        double horizontalDistanceSq = deltaX * deltaX + deltaZ * deltaZ;
        double velocityTowardTarget = simulatedPlayer.t() * deltaX + simulatedPlayer.T() * deltaZ;
        double overshootPenalty = Math.max(0.0, -velocityTowardTarget) * 2500.0;
        if (firstCatchTick != -1) {
            return -100000.0 + firstCatchTick * 10000.0 - caughtTicks * 1500.0
                    + horizontalDistanceSq * 100.0 + overshootPenalty;
        }
        if (simulatedPlayer.N() < plan.getLadderBlock().B() - 0.05) {
            return Double.POSITIVE_INFINITY;
        }
        return bestInterceptScore + horizontalDistanceSq * 200.0 + overshootPenalty;
    }

    private static int estimateCatchHorizon(EntityPlayerSP player, AutoLadderPlan plan) {
        double y = player.N();
        double motionY = player.q();
        double targetY = plan.getLadderBlock().B() + 0.95;
        for (int tick = 1; tick <= 6; ++tick) {
            y += motionY;
            if (y <= targetY) {
                return tick;
            }
            motionY = (motionY - 0.08) * 0.98;
        }
        return 6;
    }

    public static boolean isCatchableNow(EntityPlayer player, AutoLadderPlan plan) {
        AxisAlignedBB bounds = player.u$src$Lgg_vape_wrapper_impl_AxisAlignedBB_$kogbsu();
        BlockData ladder = plan.getLadderBlock();
        if (bounds.getMaxY() <= ladder.B() || bounds.getMinY() >= ladder.B() + 1.0) {
            return false;
        }
        int directionX = plan.getLadderFacing().getDirectionVector().getX();
        int directionZ = plan.getLadderFacing().getDirectionVector().getZ();
        if (directionX > 0) {
            return intersects(bounds.getMinX(), bounds.getMaxX(), ladder.D(),
                    ladder.D() + LADDER_THICKNESS)
                    && intersects(bounds.getMinZ(), bounds.getMaxZ(), ladder.G(), ladder.G() + 1.0);
        }
        if (directionX < 0) {
            return intersects(bounds.getMinX(), bounds.getMaxX(),
                    ladder.D() + 1.0 - LADDER_THICKNESS, ladder.D() + 1.0)
                    && intersects(bounds.getMinZ(), bounds.getMaxZ(), ladder.G(), ladder.G() + 1.0);
        }
        if (directionZ > 0) {
            return intersects(bounds.getMinZ(), bounds.getMaxZ(), ladder.G(),
                    ladder.G() + LADDER_THICKNESS)
                    && intersects(bounds.getMinX(), bounds.getMaxX(), ladder.D(), ladder.D() + 1.0);
        }
        return directionZ < 0
                && intersects(bounds.getMinZ(), bounds.getMaxZ(),
                ladder.G() + 1.0 - LADDER_THICKNESS, ladder.G() + 1.0)
                && intersects(bounds.getMinX(), bounds.getMaxX(), ladder.D(), ladder.D() + 1.0);
    }

    private static boolean intersects(double minimum, double maximum,
                                      double targetMinimum, double targetMaximum) {
        return maximum > targetMinimum && minimum < targetMaximum;
    }

    private static boolean isAboveSupportTop(EntityPlayer player, BlockData support) {
        if (player.N() < support.B() + 0.95) {
            return false;
        }
        AxisAlignedBB bounds = player.u$src$Lgg_vape_wrapper_impl_AxisAlignedBB_$kogbsu();
        return bounds.getMaxX() > support.D() && bounds.getMinX() < support.D() + 1.0
                && bounds.getMaxZ() > support.G() && bounds.getMinZ() < support.G() + 1.0;
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
