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
    private static final double CONTACT_PRESS_OVERLAP = 0.005;
    private static final int CENTERING_LOOKAHEAD_TICKS = 2;
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
        return chooseCentering(player, player, world,
                new BlockPlacementGraph(player), centerX, centerZ);
    }

    public static CenterInput chooseCentering(EntityPlayerSP player, World world,
                                              double centerX, double centerZ,
                                              BlockData ladderBlock, EnumFacing facing) {
        return chooseCentering(player, player, world,
                new BlockPlacementGraph(player), centerX, centerZ, ladderBlock, facing);
    }

    public static CenterInput chooseCentering(EntityPlayer sourcePlayer, EntityPlayerSP localPlayer,
                                              World world, BlockPlacementGraph graph,
                                              double centerX, double centerZ) {
        return chooseCentering(sourcePlayer, localPlayer, world, graph,
                centerX, centerZ, null, null);
    }

    public static CenterInput chooseCentering(EntityPlayer sourcePlayer, EntityPlayerSP localPlayer,
                                              World world, BlockPlacementGraph graph,
                                              double centerX, double centerZ,
                                              BlockData ladderBlock, EnumFacing facing) {
        double[] target = resolveCenteringTarget(sourcePlayer, centerX, centerZ,
                ladderBlock, facing);
        boolean reusableSimulation = ForgeVersion.MC_1_21_4.d() || ForgeVersion.MC_1_16_5.d();
        BlockPathPlanner simulation = reusableSimulation
                ? new BlockPathPlanner(sourcePlayer, localPlayer, world, graph) : null;
        CenterInput bestInput = INPUTS[0];
        double bestScore = Double.POSITIVE_INFINITY;
        for (CenterInput input : INPUTS) {
            double score = reusableSimulation
                    ? simulateCenteringInput(simulation, graph, input, target[0], target[1])
                    : simulateCenteringInput(sourcePlayer, localPlayer, world, graph,
                            input, target[0], target[1]);
            if (score < bestScore) {
                bestScore = score;
                bestInput = input;
            }
        }
        return bestInput;
    }

    /**
     * On versions where {@code isOnLadder} additionally requires a horizontal contact
     * (1.8.9/1.12.2), steering to the ladder cell center never touches the ladder box,
     * so the grab can never trigger. Aim the player's hitbox slightly INTO the ladder
     * face instead, so the collision keeps the contact pressed every tick.
     */
    private static double[] resolveCenteringTarget(EntityPlayer player, double centerX, double centerZ,
                                                   BlockData ladderBlock, EnumFacing facing) {
        if (!requiresLadderContact() || ladderBlock == null || facing == null) {
            return new double[]{centerX, centerZ};
        }
        AxisAlignedBB bounds = player.u$src$Lgg_vape_wrapper_impl_AxisAlignedBB_$kogbsu();
        double halfWidth = (bounds.getMaxX() - bounds.getMinX()) / 2.0;
        double halfDepth = (bounds.getMaxZ() - bounds.getMinZ()) / 2.0;
        double thickness = getLadderThickness();
        double targetX = centerX;
        double targetZ = centerZ;
        int directionX = facing.getDirectionVector().getX();
        int directionZ = facing.getDirectionVector().getZ();
        if (directionX > 0) {
            targetX = ladderBlock.D() + halfWidth + thickness - CONTACT_PRESS_OVERLAP;
        } else if (directionX < 0) {
            targetX = ladderBlock.D() + 1.0 - halfWidth - thickness + CONTACT_PRESS_OVERLAP;
        } else if (directionZ > 0) {
            targetZ = ladderBlock.G() + halfDepth + thickness - CONTACT_PRESS_OVERLAP;
        } else if (directionZ < 0) {
            targetZ = ladderBlock.G() + 1.0 - halfDepth - thickness + CONTACT_PRESS_OVERLAP;
        }
        return new double[]{targetX, targetZ};
    }

    static boolean requiresLadderContact() {
        return ForgeVersion.MC_1_8_9.d() || ForgeVersion.MC_1_12_2.d();
    }

    /**
     * Clearance margin used when checking the player against the support block. Contact
     * versions must be allowed to rest flush against the wall (the margin becomes slightly
     * negative to tolerate floating point while still rejecting real penetration).
     */
    static double getSupportClearanceMargin() {
        return requiresLadderContact() ? -CONTACT_PRESS_OVERLAP : 0.04;
    }

    static double getLadderTopClearanceMargin() {
        return requiresLadderContact() ? -CONTACT_PRESS_OVERLAP : 0.002;
    }

    private static double simulateCenteringInput(BlockPathPlanner simulation,
                                                 BlockPlacementGraph graph,
                                                 CenterInput input,
                                                 double centerX, double centerZ) {
        simulation.applySnapshot(graph);
        simulation.setInput(input.forward, input.backward, input.left, input.right, false, false);
        EntityPlayer simulatedPlayer = simulation.getSimulatedPlayer();
        double bestDistanceSq = Double.POSITIVE_INFINITY;
        for (int tick = 1; tick <= CENTERING_LOOKAHEAD_TICKS; ++tick) {
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

    private static double simulateCenteringInput(EntityPlayer sourcePlayer,
                                                 EntityPlayerSP localPlayer,
                                                 World world, BlockPlacementGraph graph,
                                                 CenterInput input,
                                                 double centerX, double centerZ) {
        BlockPathPlanner simulation = new BlockPathPlanner(sourcePlayer, localPlayer, world, graph);
        simulation.applySnapshot(graph);
        simulation.setInput(input.forward, input.backward, input.left, input.right, false, false);
        EntityPlayer simulatedPlayer = simulation.getSimulatedPlayer();
        double bestDistanceSq = Double.POSITIVE_INFINITY;
        for (int tick = 1; tick <= CENTERING_LOOKAHEAD_TICKS; ++tick) {
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
        applyDirectional(input.forward, input.backward, input.left, input.right);
    }

    public static void apply(AutoLadderFallAdjustment adjustment) {
        if (!adjustment.overridesInput()) {
            MovementInputHelper.restorePhysicalInput(false);
            return;
        }
        applyDirectional(adjustment.isForward(), adjustment.isBackward(),
                adjustment.isLeft(), adjustment.isRight());
    }

    private static void applyDirectional(boolean forward, boolean backward,
                                         boolean left, boolean right) {
        MovementInputHelper.synchronizeDirectionalInput(forward, backward, left, right);
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

        public boolean isForward() {
            return this.forward;
        }

        public boolean isBackward() {
            return this.backward;
        }

        public boolean isLeft() {
            return this.left;
        }

        public boolean isRight() {
            return this.right;
        }
    }

}
