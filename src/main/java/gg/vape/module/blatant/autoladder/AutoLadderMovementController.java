package gg.vape.module.blatant.autoladder;

import gg.vape.module.blatant.blockin.BlockPathPlanner;
import gg.vape.module.blatant.blockin.BlockPlacementGraph;
import gg.vape.movement.MovementInputHelper;
import gg.vape.utils.datas.BlockData;
import gg.vape.wrapper.impl.AxisAlignedBB;
import gg.vape.wrapper.impl.EntityPlayer;
import gg.vape.wrapper.impl.EntityPlayerSP;
import gg.vape.wrapper.impl.EnumFacing;
import gg.vape.wrapper.impl.ForgeVersion;
import gg.vape.wrapper.impl.GameSettings;
import gg.vape.wrapper.impl.Minecraft;
import gg.vape.wrapper.impl.World;

/** Receding-horizon movement controller that keeps the player over a block center. */
public final class AutoLadderMovementController {
    private static final double LEGACY_LADDER_THICKNESS = 0.125;
    private static final double MODERN_LADDER_THICKNESS = 0.1875;
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
                                              double centerX, double centerZ) {
        BlockPlacementGraph graph = new BlockPlacementGraph(player);
        CenterInput bestInput = INPUTS[0];
        double bestScore = Double.POSITIVE_INFINITY;
        for (CenterInput input : INPUTS) {
            double score = simulateCenteringInput(
                    player, world, graph, input, centerX, centerZ);
            if (score < bestScore) {
                bestScore = score;
                bestInput = input;
            }
        }
        return bestInput;
    }

    private static double simulateCenteringInput(EntityPlayerSP player, World world,
                                                 BlockPlacementGraph graph,
                                                 CenterInput input,
                                                 double centerX, double centerZ) {
        BlockPathPlanner simulation = new BlockPathPlanner(player, player, world, graph);
        simulation.applySnapshot(graph);
        simulation.setInput(input.forward, input.backward, input.left, input.right, false, false);
        EntityPlayer simulatedPlayer = simulation.getSimulatedPlayer();
        double bestDistanceSq = Double.POSITIVE_INFINITY;
        for (int tick = 1; tick <= 2; ++tick) {
            simulation.simulateTick(false);
            double deltaX = centerX - simulatedPlayer.z();
            double deltaZ = centerZ - simulatedPlayer.h();
            bestDistanceSq = Math.min(bestDistanceSq,
                    deltaX * deltaX + deltaZ * deltaZ);
        }
        double deltaX = centerX - simulatedPlayer.z();
        double deltaZ = centerZ - simulatedPlayer.h();
        double finalDistanceSq = deltaX * deltaX + deltaZ * deltaZ;
        double horizontalSpeedSq = simulatedPlayer.t() * simulatedPlayer.t()
                + simulatedPlayer.T() * simulatedPlayer.T();
        double velocityTowardCenter = simulatedPlayer.t() * deltaX
                + simulatedPlayer.T() * deltaZ;
        double overshootPenalty = Math.max(0.0, -velocityTowardCenter) * 4000.0;
        return finalDistanceSq * 10000.0 + bestDistanceSq * 1000.0
                + horizontalSpeedSq * 150.0 + overshootPenalty;
    }

    static double getLadderThickness() {
        return ForgeVersion.MC_1_16_5.d()
                ? MODERN_LADDER_THICKNESS : LEGACY_LADDER_THICKNESS;
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

    }

}
