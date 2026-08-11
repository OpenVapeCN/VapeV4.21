package gg.vape.module.blatant;

import gg.vape.Vape;
import gg.vape.event.EventHandler;
import gg.vape.event.EventPriority;
import gg.vape.event.impl.EventClickMouse;
import gg.vape.event.impl.EventMouseButton;
import gg.vape.event.impl.EventPacketReceive;
import gg.vape.event.impl.EventPostTick;
import gg.vape.event.impl.EventPreLocalPlayerTick;
import gg.vape.event.impl.EventPreTick;
import gg.vape.input.KeyboardCodeUtil;
import gg.vape.input.MovementInputLock;
import gg.vape.mapping.MappedClasses;
import gg.vape.module.Category;
import gg.vape.module.Mod;
import gg.vape.module.ModuleDisplayInfo;
import gg.vape.module.VisibleModuleList;
import gg.vape.module.blatant.blockin.BlockInBooleanState;
import gg.vape.module.blatant.clutch.ClutchPlacementSearchStrategy;
import gg.vape.module.blatant.clutch.ClutchSearchPlanner;
import gg.vape.module.blatant.blockin.BlockInTargetRotationState;
import gg.vape.module.blatant.clutch.ClutchThresholdRotationController;
import gg.vape.module.blatant.blockin.BlockPathPlanner;
import gg.vape.module.blatant.blockin.BlockPlacementGraph;
import gg.vape.module.blatant.blockin.BlockPlacementPathSegment;
import gg.vape.module.blatant.blockin.BlockPlacementPathSegmentState;
import gg.vape.module.blatant.clutch.EntityFixedRotationController;
import gg.vape.module.control.SharedModuleControlClaims;
import gg.vape.module.none.ClientSettings;
import gg.vape.module.render.hud.FreeLookHudModule;
import gg.vape.module.utility.clutch.ClutchPlacementPathUtils;
import gg.vape.module.utility.clutch.PlacementTarget;
import gg.vape.movement.MovementInputHelper;
import gg.vape.notification.Notification;
import gg.vape.notification.NotificationType;
import gg.vape.notification.TextNotificationContent;
import gg.vape.rotation.AdaptiveRotationController;
import gg.vape.rotation.FixedRotationController;
import gg.vape.rotation.RotationAngles;
import gg.vape.rotation.RotationControlClaim;
import gg.vape.rotation.RotationManager;
import gg.vape.ui.click.frame.impl.hud.ActiveModuleStackFrame;
import gg.vape.ui.theme.ThemeColors;
import gg.vape.unmap.ItemLimitData;
import gg.vape.utils.BlockUtil;
import gg.vape.utils.MathUtil;
import gg.vape.utils.PlayerSimulationUtil;
import gg.vape.utils.RotationUtil;
import gg.vape.utils.RotationVectorMath;
import gg.vape.utils.TimerUtil;
import gg.vape.utils.Vec3d;
import gg.vape.utils.datas.BlockCoordinate;
import gg.vape.utils.datas.BlockData;
import gg.vape.value.BooleanValue;
import gg.vape.value.LimitValue;
import gg.vape.value.NumberValue;
import gg.vape.value.RandomValue;
import gg.vape.wrapper.impl.AxisAlignedBB;
import gg.vape.wrapper.impl.Block;
import gg.vape.wrapper.impl.BlockPos;
import gg.vape.wrapper.impl.BlockState;
import gg.vape.wrapper.impl.EntityPlayer;
import gg.vape.wrapper.impl.EntityPlayerSP;
import gg.vape.wrapper.impl.EnumFacing;
import gg.vape.wrapper.impl.ForgeVersion;
import gg.vape.wrapper.impl.GameSettings;
import gg.vape.wrapper.impl.GuiScreen;
import gg.vape.wrapper.impl.InventoryPlayer;
import gg.vape.wrapper.impl.Item;
import gg.vape.wrapper.impl.ItemStack;
import gg.vape.wrapper.impl.KeyBinding;
import gg.vape.wrapper.impl.Minecraft;
import gg.vape.wrapper.impl.Packet;
import gg.vape.wrapper.impl.PacketVelocityBridge;
import gg.vape.wrapper.impl.RayTraceResult;
import gg.vape.wrapper.impl.SPacketBlockChange;
import gg.vape.wrapper.impl.SPacketEntityVelocity;
import gg.vape.wrapper.impl.Vec3;
import gg.vape.wrapper.impl.World;
import gg.vape.wrapper.impl.WorldClient;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;
import java.util.Vector;
import org.jetbrains.annotations.Nullable;

public class Clutch
extends Mod {
    private float placeYaw;
    private BlockPlacementGraph currentGraph;
    private final LimitValue blacklistBlocks;
    private final VisibleModuleList<BlockData> placedBlocks;
    private final BooleanValue limitBlocks;
    private final RandomValue resetAngleDelay;
    private final BooleanValue silentAim;
    private boolean inputRight;
    private TimerUtil failTimer;
    private final RandomValue returnDelay;
    private final BooleanValue heldWhitelist;
    private BlockPlacementGraph graph;
    private boolean claimActive = false;
    private boolean staircaseQueued;
    private Notification failNotification;
    private ArrayList<String> defaultBlockNames;
    private final BooleanValue resetAngle;
    private FixedRotationController rotationController;
    private Boolean pendingInputBack;
    private final RandomValue clutchMoveDelay;
    private double savedYaw = -999.0;
    private final NumberValue maxBlocks;
    private boolean inputForward;
    private double savedPitch = -999.0;
    private int pendingFailDelayTicks;
    private TimerUtil staircaseTimer;
    private Boolean pendingInputLeft;
    private boolean pendingInputApply;
    private int moveDelayTicks;
    private final NumberValue blocksThreshold;
    private final ArrayList<BlockPlacementPathSegment> pendingSegments;
    private boolean takingKnockback;
    private HashSet<BlockData> rejectedBlocks;
    private final MovementInputLock movementLock;
    private BlockPathPlanner pathPlanner;
    private final BooleanValue showBlockCount;
    private final BooleanValue returnToLastSlot;
    private boolean inputBack;
    private int returnDelayTicks;
    private Boolean pendingInputForward;
    private TimerUtil landTimer;
    private HashSet<BlockData> placeableBlocks;
    private float originalYaw;
    private boolean prevRightClickHeld;
    private final BooleanValue blacklist;
    private int resetAngleDelayTicks;
    private boolean pauseRequested;
    private boolean inputLeft;
    private PlacementTarget placeTarget;
    private BlockPathPlanner pathPlannerReturn;
    private int groundStuckTicks;
    private final LimitValue whitelistBlocks;
    private RayTraceResult rayTrace;
    public static final ArrayList<Vector<PlacementTarget>> DEBUG_PLACEMENT_PATHS = new ArrayList<>();
    private double fallTargetY = -999.0;
    private String pendingFailMessage;
    private boolean forcingCounterMotion;
    private boolean counterMotion;
    private boolean recentlyClutched;
    private Boolean pendingInputRight;
    private final BooleanValue onLethalFall;
    private final NumberValue speed;
    private final RotationControlClaim rotationClaim;
    private final BooleanValue onVoid = BooleanValue.create(this, "On void", true, "Catches the player if they are about to fall into the void");
    private boolean placementRejected = false;
    private BlockPlacementPathSegment clutchPath;
    private final NumberValue failDelay;
    private int knockbackTicks;
    private final HashMap<BlockData, HashSet<BlockData>> blockGraphMap;
    private int previousSlot = -1;
    private final BooleanValue onMoreThanXBlocks;
    private final BooleanValue allowStaircaseUp;

    private void resetPendingFail() {
        this.pendingFailMessage = null;
        this.pendingFailDelayTicks = 0;
    }

    private void resetMovementInputs() {
        if (Minecraft.currentScreen().getObject() == null) {
            KeyboardCodeUtil.disableLegacyRepeatEvents();
        }
        MovementInputHelper.restorePhysicalInput(false);
        this.inputForward = false;
        this.inputRight = false;
        this.inputLeft = false;
        this.inputBack = false;
    }

    private boolean hasPlacementTargets() {
        return this.clutchPath != null && this.clutchPath.placementState != null && !this.clutchPath.placementState.pendingTargets.isEmpty();
    }

    private int simulatePlacementTick(BlockPlacementPathSegment pathSegment, PlacementTarget placementTarget, EntityPlayer player, EntityPlayerSP localPlayer, World world, float yaw, float pitch, BlockPlacementGraph placementGraph) {
        BlockPathPlanner simulation = new BlockPathPlanner(player, localPlayer, world, placementGraph);
        if (this.counterMotion) {
            simulation.setForwardOnly();
        } else {
            simulation.restoreSnapshotInput();
        }
        EntityPlayer simulatedPlayer = simulation.getSimulatedPlayer();
        simulatedPlayer.H(yaw);
        simulatedPlayer.z(yaw);
        simulatedPlayer.C(pitch);
        double stationaryFallMotion = -0.0784000015258789;
        if (this.forcingCounterMotion && !this.takingKnockback && player.b$src$Z$fqlxe4() && player.q() == stationaryFallMotion) {
            simulation.clearInput();
        }
        double eyeHeight = ForgeVersion.MC_1_7_10.Y() ? (double)localPlayer.X() : 0.0;
        boolean facingAwayFromTarget = false;
        boolean targetReached = false;
        int simulatedTicks = 0;
        while (simulatedTicks < 6) {
            ++simulatedTicks;
            Vec3d simulatedPosition = simulation.getSimulatedPosition();
            Vec3 eyePosition = Vec3.create(simulatedPosition.getX(), simulatedPosition.getY() + eyeHeight, simulatedPosition.getZ());
            if (ClutchPlacementPathUtils.isBlockFaceVisible(eyePosition, world, placementTarget.supportBlock, placementTarget.facing)) {
                Vec3 placementHit = ClutchPlacementPathUtils.findPlacementHitPoint(localPlayer, world, eyePosition, placementTarget, yaw, pitch);
                if (placementHit != null && eyePosition.distanceTo(placementHit) <= 5.0) {
                    targetReached = true;
                    placementTarget.hitPoint = placementHit;
                    Vec3d hitPosition = simulation.getSimulatedPosition();
                    pathSegment.hitVector = Vec3.create(hitPosition.getX(), hitPosition.getY() + eyeHeight, hitPosition.getZ());
                    break;
                }
            }
            double targetX = (double)placementTarget.supportBlock.D() + 0.5;
            double targetZ = (double)placementTarget.supportBlock.G() + 0.5;
            float targetYawDelta = (float)RotationUtil.N(eyePosition.getX(), eyePosition.getZ(), yaw, targetX, targetZ);
            facingAwayFromTarget = Math.abs(MathUtil.wrapAngleTo180((double)targetYawDelta)) > 110.0f;
            if (simulatedPosition.getY() <= (double)pathSegment.targetCoordinate.E()) break;
            BlockInBooleanState strafeState = null;
            if (!this.silentAim.getEffectiveValue().booleanValue() && (strafeState = this.computeStrafeState(simulatedPlayer, this.placeYaw, this.graph.forwardKeyDown, this.graph.backwardKeyDown, this.graph.leftKeyDown, this.graph.rightKeyDown)) != null) {
                simulation.setDirectionalKeys(strafeState.correctedForward, strafeState.correctedBackward, strafeState.correctedLeft, strafeState.correctedRight);
            }
            simulation.simulateTick();
            if (strafeState == null) continue;
            simulation.setDirectionalKeys(strafeState.originalForward, strafeState.originalBackward, strafeState.originalLeft, strafeState.originalRight);
        }
        Vec3d finalPosition = simulation.getSimulatedPosition();
        if (targetReached) {
            pathSegment.hitVector = Vec3.create(finalPosition.getX(), finalPosition.getY() + eyeHeight, finalPosition.getZ());
            boolean targetTouchesStart = placementTarget.supportBlock.L(pathSegment.startCoordinate.O());
            if (targetTouchesStart && player.b$src$Z$fqlxe4()) {
                return facingAwayFromTarget ? 2 : 1;
            }
            return simulatedTicks;
        }
        BlockData supportBlock = placementTarget.supportBlock;
        BlockData placedBlock = placementTarget.getPlacedBlock();
        Vec3 fallbackEyePosition = Vec3.create((double)placedBlock.D() + 0.5, player.N() + eyeHeight, (double)placedBlock.G() + 0.5);
        Vec3 finalHit = ClutchPlacementPathUtils.findPlacementHitPoint(localPlayer, world, fallbackEyePosition, placementTarget, yaw, pitch);
        pathSegment.hitVector = fallbackEyePosition;
        placementTarget.hitPoint = finalHit == null ? RotationUtil.M(fallbackEyePosition, BlockUtil.F(world, supportBlock), 0.0, 0.0, 0.0).toVec3() : finalHit;
        return 2;
    }

    private void queueFailMessage(String message) {
        this.pendingFailMessage = message;
        if (this.pendingFailDelayTicks == 0) {
            this.pendingFailDelayTicks = Math.min(3, Math.max(1, ((Double)this.failDelay.getValue()).intValue() / 50));
        }
    }

    private boolean isValidBlockItem(ItemStack itemStack) {
        if (itemStack.isNull() || itemStack.getItem().isNull()) {
            return false;
        }
        Item item = itemStack.getItem();
        if (!item.isInstance(MappedClasses.Vw)) {
            return false;
        }
        return this.blacklist.getEffectiveValue() == false || this.blacklistBlocks.doesNotMatch(itemStack);
    }

    private void tickFailDelay() {
        if (this.pendingFailDelayTicks > 0) {
            --this.pendingFailDelayTicks;
            if (this.pendingFailDelayTicks == 0 && this.pendingFailMessage != null) {
                this.showFailNotification(this.pendingFailMessage, false);
                this.pendingFailMessage = null;
            }
        }
    }

    private BlockCoordinate findLandingBlock(int maxTicks, EntityPlayer player, EntityPlayerSP localPlayer, ArrayList<Vec3d> simulatedPositions) {
        int worldBottom = ForgeVersion.MC_1_20_6.d() ? localPlayer.getWorld().R() : 0;
        BlockPathPlanner simulation = new BlockPathPlanner(player, localPlayer, localPlayer.getWorld(), this.graph);
        simulation.applySnapshot(this.graph);
        simulation.restoreSnapshotInput();
        if (simulatedPositions != null) {
            simulatedPositions.add(simulation.getSimulatedPosition());
        }
        boolean returnFinalPosition = !this.onVoid.getEffectiveValue();
        int requiredGroundTicks = this.onMoreThanXBlocks.getEffectiveValue() ? 3 : 1;
        EntityPlayer simulatedPlayer = simulation.getSimulatedPlayer();
        int groundedTicks = 0;
        BlockCoordinate landingBlock = null;
        for (int tick = 0; tick <= maxTicks; ++tick) {
            simulation.simulateTick();
            if (simulatedPositions != null) {
                simulatedPositions.add(simulation.getSimulatedPosition());
            }
            int blockX = MathUtil.floor(simulatedPlayer.z());
            int blockY = MathUtil.floor(simulatedPlayer.N() - 0.015625);
            int blockZ = MathUtil.floor(simulatedPlayer.h());
            Block block = localPlayer.getWorld().getBlockByPos(blockX, blockY, blockZ);
            boolean specialLandingBlock = block.isNotNull() && (block.isInstance(MappedClasses.q_) || block.isInstance(MappedClasses.b));
            if (simulatedPlayer.b$src$Z$fqlxe4() || specialLandingBlock) {
                ++groundedTicks;
                if (landingBlock == null) {
                    landingBlock = new BlockCoordinate(blockX, blockY, blockZ);
                }
                if (groundedTicks >= requiredGroundTicks || simulatedPlayer.e$src$Z$15bd4i1()) {
                    return landingBlock;
                }
            } else {
                landingBlock = null;
                groundedTicks = 0;
            }
            if (simulatedPlayer.N() <= (double)worldBottom) {
                return null;
            }
        }
        if (returnFinalPosition) {
            return new BlockCoordinate(MathUtil.floor(simulatedPlayer.z()), MathUtil.floor(simulatedPlayer.N()) - 1, MathUtil.floor(simulatedPlayer.h()));
        }
        return null;
    }

    private void applyMovementInputs() {
        if (Minecraft.currentScreen().isNotNull()) {
            return;
        }
        if (Minecraft.currentScreen().getObject() == null) {
            KeyboardCodeUtil.disableLegacyRepeatEvents();
        }
        if (this.pendingInputForward != null) {
            MovementInputHelper.synchronizeDirectionalInput(this.pendingInputForward, this.pendingInputBack, this.pendingInputLeft, this.pendingInputRight);
        } else {
            MovementInputHelper.synchronizeDirectionalInput(this.inputForward, this.inputBack, this.inputLeft, this.inputRight);
        }
        if (this.forcingCounterMotion && !this.takingKnockback) {
            MovementInputHelper.setJumpPressed(false);
        } else {
            MovementInputHelper.setJumpPressed(this.graph.jumpInput);
        }
    }

    @EventHandler
    public void onMouseButton(EventMouseButton eventMouseButton) {
        GameSettings gameSettings = eventMouseButton.getGameSettings();
        if (this.clutchPath != null && eventMouseButton.getButton() == EventMouseButton.LEFT_BUTTON && eventMouseButton.getButtonState()) {
            eventMouseButton.setCancelled(true);
            gameSettings.F().e();
        }
    }

    public RotationControlClaim getRotationClaim() {
        return this.rotationClaim;
    }

    public Clutch() {
        super("Clutch", -65404, Category.UTILITY, "Saves yourself from falling");
        this.onLethalFall = BooleanValue.create(this, "On lethal fall", true, "Catches the player if they are about to die due to fall damage");
        this.onMoreThanXBlocks = BooleanValue.create(this, "On more than x blocks", false, "Catches the player if their landing block is more than x amount of blocks");
        this.blocksThreshold = NumberValue.create((Object)this, "Blocks", "#", "", 3.0, 6.0, 10.0, 1.0);
        this.speed = NumberValue.create(this, "Speed", "#.#", "", 1.0, 3.5, 10.0, 0.1, "Maximum flick speed when placing blocks");
        this.silentAim = BooleanValue.create(this, "Silent aim", false, "Silent aim when placing blocks");
        this.allowStaircaseUp = BooleanValue.create(this, "Allow staircase up", true, "Allows clutch to staircase on repeat jumps");
        this.clutchMoveDelay = RandomValue.createWithDescription(this, "Clutch move delay", "#", "tick", 0.0, 3.0, 6.0, 10.0, 1.0, "Freezes movement for a few ticks after completing a long clutch\nNOTE: Only affects clutches related to high velocity.");
        this.failDelay = NumberValue.create(this, "Fail delay", "#", "ms", 0.0, 100.0, 500.0, 50.0, "Delay before retrying to find a clutch path");
        this.showBlockCount = BooleanValue.create(this, "Show block count", false);
        this.limitBlocks = BooleanValue.create(this, "Limit blocks", false, "Only clutch if it requires fewer than the max number of blocks");
        this.maxBlocks = NumberValue.create(this, "Max blocks", "#", "", 1.0, 5.0, 10.0, 1.0, "Maximum blocks allowed for a clutch");
        this.returnToLastSlot = BooleanValue.create(this, "Return to last slot", true, "Selects previously selected slot when clutch is completed");
        this.returnDelay = RandomValue.createWithDescription(this, "Return delay", "#", "tick", 0.0, 3.0, 6.0, 10.0, 1.0, "Delay before returning to the last slot");
        this.resetAngle = BooleanValue.create(this, "Reset angle", true, "Looks back to your original angle after clutching\nNOTE: Only affects non silent aim.");
        this.resetAngleDelay = RandomValue.createWithDescription(this, "Reset angle delay", "#", "tick", 0.0, 3.0, 6.0, 10.0, 1.0, "Delay before resetting your angles after clutching");
        this.blacklist = BooleanValue.create(this, "Blacklist", true, "Clutch will not use these blocks");
        this.blacklistBlocks = LimitValue.create(this, "clutch-blacklist", "Block blacklist", LimitValue.BLOCK_LIST_COLOR, ItemLimitData.DEFAULT_BLOCK_BLACKLIST);
        this.heldWhitelist = BooleanValue.create(this, "Held whitelist", false, "Only activates clutch when a whitelisted block is held\nWill only use held block for Clutching");
        this.whitelistBlocks = LimitValue.create(this, "clutch-allowedblocks", "Held block whitelist", LimitValue.ALLOW_LIST_COLOR, new ItemLimitData("blocks"));
        this.defaultBlockNames = new ArrayList<String>(Arrays.asList("Wool", "Stone", "Wood Planks", "Red Sandstone", "Stained Clay", "End Stone", "Obsidian"));
        this.rotationClaim = SharedModuleControlClaims.rotation;
        this.movementLock = SharedModuleControlClaims.movementInput;
        this.landTimer = new TimerUtil();
        this.failTimer = new TimerUtil();
        this.staircaseTimer = new TimerUtil();
        this.placedBlocks = new VisibleModuleList(5000L);
        this.rejectedBlocks = new HashSet();
        this.placeableBlocks = new HashSet();
        this.pendingSegments = new ArrayList();
        this.blockGraphMap = new HashMap();
        this.blacklist.addDependentValues(this.blacklistBlocks);
        this.heldWhitelist.addDependentValues(this.whitelistBlocks);
        this.onMoreThanXBlocks.addDependentValues(this.blocksThreshold);
        this.returnToLastSlot.addDependentValues(this.returnDelay);
        this.limitBlocks.addDependentValues(this.maxBlocks);
        this.silentAim.getDisabledCondition().applyTo(this.resetAngle);
        this.resetAngle.setOverrideColor(ThemeColors.J.r);
        this.addValue(this.onVoid, this.onLethalFall, this.onMoreThanXBlocks, this.blocksThreshold, this.speed, this.silentAim, this.resetAngle, this.resetAngleDelay, this.returnToLastSlot, this.returnDelay, this.clutchMoveDelay, this.failDelay, this.allowStaircaseUp, this.showBlockCount, this.limitBlocks, this.maxBlocks, this.blacklist, this.blacklistBlocks, this.heldWhitelist, this.whitelistBlocks);
        this.rotationClaim.setPriority(this, 50);
    }

    @EventHandler
    public void onPostTick(EventPostTick eventPostTick) {
        EntityPlayerSP localPlayer = eventPostTick.getThePlayer();
        WorldClient world = eventPostTick.getWorld();
        if (localPlayer.isNull() || world.isNull()) {
            this.resetState();
            return;
        }
        boolean onGround = localPlayer.b$src$Z$fqlxe4();
        if (onGround) {
            if (this.knockbackTicks > 0) {
                --this.knockbackTicks;
                this.debugLog("Knockback ticks: " + this.knockbackTicks + " " + this.graph.jumpKeyDown);
            }
            if (this.clutchPath != null) {
                if (this.knockbackTicks == 0) {
                    this.debugLog("Reset knockback");
                    this.knockbackTicks = 0;
                }
                if (this.forcingCounterMotion && this.takingKnockback) {
                    this.debugLog("Revert to original yaw");
                    this.placeYaw = this.originalYaw;
                }
            }
            this.currentGraph = new BlockPlacementGraph(localPlayer);
            if (this.clutchPath != null && this.clutchPath.placementState != null && this.placeTarget != null) {
                float pitch;
                float yaw;
                if (this.rotationController == null) {
                    yaw = localPlayer.J();
                    pitch = localPlayer.V();
                } else if (this.rotationController instanceof AdaptiveRotationController) {
                    yaw = ((AdaptiveRotationController)this.rotationController).getRenderedYaw();
                    pitch = ((AdaptiveRotationController)this.rotationController).getRenderedPitch();
                } else {
                    yaw = this.rotationController.getCurrentYaw();
                    pitch = this.rotationController.getCurrentPitch();
                }
                int placementTicks = this.simulatePlacementTick(this.clutchPath, this.placeTarget, localPlayer, localPlayer, world, yaw, pitch, this.currentGraph);
                Vec3 eyePosition = this.clutchPath.hitVector;
                this.rotationController = this.buildRotation(localPlayer, eyePosition, this.placeTarget.hitPoint, this.rotationController, placementTicks, this.placeYaw);
                RotationManager.INSTANCE.setController(this.rotationController);
            }
            return;
        }
        if (this.knockbackTicks > 0) {
            --this.knockbackTicks;
            this.debugLog("Knockback ticks: " + this.knockbackTicks + " " + this.graph.jumpKeyDown);
        }
        if (this.clutchPath != null && this.knockbackTicks == 0 && this.forcingCounterMotion && this.takingKnockback) {
            this.debugLog("Revert to original yaw");
            this.placeYaw = this.originalYaw;
        }
        this.currentGraph = new BlockPlacementGraph(localPlayer);
        if (this.clutchPath != null && this.clutchPath.placementState != null && this.placeTarget != null) {
            float pitch;
            float yaw;
            if (this.rotationController == null) {
                yaw = localPlayer.J();
                pitch = localPlayer.V();
            } else if (this.rotationController instanceof AdaptiveRotationController) {
                yaw = ((AdaptiveRotationController)this.rotationController).getRenderedYaw();
                pitch = ((AdaptiveRotationController)this.rotationController).getRenderedPitch();
            } else {
                yaw = this.rotationController.getCurrentYaw();
                pitch = this.rotationController.getCurrentPitch();
            }
            int placementTicks = this.simulatePlacementTick(this.clutchPath, this.placeTarget, localPlayer, localPlayer, world, yaw, pitch, this.currentGraph);
            Vec3 eyePosition = this.clutchPath.hitVector;
            this.rotationController = this.buildRotation(localPlayer, eyePosition, this.placeTarget.hitPoint, this.rotationController, placementTicks, this.placeYaw);
            RotationManager.INSTANCE.setController(this.rotationController);
        }
    }

    @Override
    public void onDisable() {
        ClientSettings.getFrame(ActiveModuleStackFrame.class).removeModule(this);
    }

    public int scorePlacementPath(ArrayList<Vec3d> candidatePositions, World world, Vector<PlacementTarget> path) {
        if (path == null || path.isEmpty()) {
            return Integer.MAX_VALUE;
        }
        Stack<PlacementTarget> remainingTargets = new Stack<>();
        for (PlacementTarget placementTarget : path) {
            remainingTargets.push(placementTarget);
        }
        boolean noTargetReached = true;
        double score = path.size() * 100;
        for (Vec3d candidatePosition : candidatePositions) {
            if (remainingTargets.isEmpty()) break;
            PlacementTarget placementTarget = remainingTargets.peek();
            Vec3 eyePosition = Vec3.create(candidatePosition.getX(), candidatePosition.getY(), candidatePosition.getZ());
            if (ClutchPlacementPathUtils.isBlockFaceVisible(eyePosition, world, placementTarget.supportBlock, placementTarget.facing)) {
                remainingTargets.pop();
                noTargetReached = false;
            } else {
                score += 100000.0;
            }
            BlockData placedBlock = placementTarget.getPlacedBlock();
            Vec3 targetPosition = Vec3.create((double)placedBlock.D() + 0.5, eyePosition.getY(), (double)placedBlock.G() + 0.5);
            score += eyePosition.distanceTo(targetPosition) * 50000.0;
        }
        score += (double)(remainingTargets.size() * 100000);
        if (noTargetReached) {
            score = Integer.MAX_VALUE;
        }
        return (int)score;
    }

    private void planPlacementSearch(BlockPlacementPathSegment pathSegment, EntityPlayerSP localPlayer, World world, ArrayList<Vec3d> candidatePositions) {
        if (pathSegment == null) {
            return;
        }
        if (pathSegment.placementState != null) {
            return;
        }
        BlockData startBlock = pathSegment.startCoordinate.O();
        BlockData targetBlock = pathSegment.targetCoordinate.O();
        int horizontalDepth = Math.abs(startBlock.D() - targetBlock.D());
        int lateralDepth = Math.abs(startBlock.G() - targetBlock.G());
        int verticalDepth = Math.abs(startBlock.B() - targetBlock.B());
        ClutchPlacementSearchStrategy strategy = new ClutchPlacementSearchStrategy(this, horizontalDepth, lateralDepth, verticalDepth, world, localPlayer, candidatePositions);
        Block block = world.getBlockByPos(targetBlock.D(), targetBlock.B(), targetBlock.G());
        if (BlockUtil.u(block)) {
            Vector<PlacementTarget> path = new ClutchSearchPlanner(strategy).findPath(startBlock, targetBlock);
            if (path != null && !path.isEmpty()) {
                pathSegment.placementState = new BlockPlacementPathSegmentState(path.lastElement().facing, path);
            }
        }
    }

    private BlockInBooleanState computeStrafeState(EntityPlayer player, float targetYaw, boolean forward, boolean backward, boolean left, boolean right) {
        if (forward || backward || left || right) {
            BlockInBooleanState state = new BlockInBooleanState();
            state.originalForward = forward;
            state.originalBackward = backward;
            state.originalLeft = left;
            state.originalRight = right;
            float viewYaw = FreeLookHudModule.isActive() ? FreeLookHudModule.getSavedPitch() : player.J();
            float movementYaw = RotationManager.INSTANCE.adjustMovementYaw(viewYaw, forward, backward, left, right);
            float yawDeltaRadians = MathUtil.wrapAngleTo180(MathUtil.wrapAngleTo180(movementYaw) - targetYaw) * ((float)Math.PI / 180);
            float forwardComponent = (float)Math.cos(yawDeltaRadians);
            float rightComponent = (float)(-Math.sin(yawDeltaRadians));
            double threshold = 0.45f;
            state.correctedForward = (double)forwardComponent >= threshold;
            state.correctedBackward = (double)forwardComponent <= -threshold;
            state.correctedLeft = (double)rightComponent <= -threshold;
            state.correctedRight = (double)rightComponent >= threshold;
            return state;
        }
        return null;
    }

    private boolean simulateLandsOnTarget(EntityPlayer player, BlockPathPlanner simulation, BlockPlacementPathSegment pathSegment) {
        boolean leavesGround = false;
        boolean wasOnGround = player.b$src$Z$fqlxe4();
        BlockCoordinate targetCoordinate = pathSegment.targetCoordinate;
        EntityPlayer simulatedPlayer = simulation.getSimulatedPlayer();
        simulation.restoreSnapshotInput();
        for (int i = 0; i < 10; ++i) {
            simulation.simulateTick();
            double motionX = simulatedPlayer.t();
            double motionZ = simulatedPlayer.T();
            if (simulatedPlayer.b$src$Z$fqlxe4()) {
                boolean insideTargetX = simulatedPlayer.z() > (double)targetCoordinate.B() && simulatedPlayer.z() < (double)(targetCoordinate.B() + 1);
                boolean insideTargetZ = simulatedPlayer.h() > (double)targetCoordinate.A() && simulatedPlayer.h() < (double)(targetCoordinate.A() + 1);
                if (Math.abs(motionX) < 0.005 && Math.abs(motionZ) < 0.005 && insideTargetX && insideTargetZ) {
                    leavesGround = false;
                    break;
                }
            } else {
                if (wasOnGround) {
                    leavesGround = true;
                    break;
                }
            }
            wasOnGround = simulatedPlayer.b$src$Z$fqlxe4();
        }
        return leavesGround;
    }

    @Override
    public ModuleDisplayInfo getModuleDisplayInfo() {
        if (!this.showBlockCount.getEffectiveValue().booleanValue()) {
            return null;
        }
        int blockCount = this.clutchPath == null ? this.countBlocks() : this.clutchPath.getPendingPlacementCount();
        Color countColor = new Color(255, 20, 20);
        if (blockCount >= 32) {
            countColor = new Color(2, 190, 58);
        } else if (blockCount >= 16) {
            countColor = new Color(255, 249, 18);
        }
        String statusFormatting;
        if (this.clutchPath != null) {
            statusFormatting = "\u00a7f\u00a7l";
        } else {
            boolean resettingAngle = this.resetAngleDelayTicks > 0 && this.resetAngleDelay.getMaximumInt() > 0;
            boolean returningSlot = this.previousSlot != -1 && this.returnToLastSlot.getEffectiveValue() != false && this.returnDelayTicks > 0 && this.returnDelay.getMaximumInt() > 0;
            boolean delayingMovement = this.moveDelayTicks > 0 && this.clutchMoveDelay.getMaximumInt() > 0;
            statusFormatting = resettingAngle || returningSlot || delayingMovement ? "\u00a7e" : "\u00a77";
        }
        String countText = (this.clutchPath == null ? "\u00a7r" : "\u00a76\u00a7l") + blockCount;
        String suffix = " " + statusFormatting + "(" + this.getName() + ")";
        return new ModuleDisplayInfo(countText, countColor, suffix);
    }

    private Vec3 computePlacementHit(EntityPlayer player, World world, BlockData blockData, EnumFacing facing) {
        Vec3 hitPosition;
        double playerX = player.z();
        double playerZ = player.h();
        if (facing == null || facing.isNull()) {
            hitPosition = Vec3.create((double)blockData.D() + 0.5, (double)blockData.B() + 0.5, (double)blockData.G() + 0.5);
        } else {
            AxisAlignedBB bounds = BlockUtil.F(world, blockData);
            double hitX = bounds.getMinX() + (bounds.getMaxX() - bounds.getMinX()) / 2.0;
            double hitY = bounds.getMinY() + (bounds.getMaxY() - bounds.getMinY()) / 2.0;
            double hitZ = bounds.getMinZ() + (bounds.getMaxZ() - bounds.getMinZ()) / 2.0;
            double xOffset = hitX - playerX;
            double zOffset = hitZ - playerZ;
            switch (facing.Y()) {
                case 2: {
                    hitZ = bounds.getMinZ();
                    hitX = Math.abs(xOffset) < (bounds.getMaxX() - bounds.getMinX()) / 2.0 ? playerX : (playerX < (double)blockData.D() ? bounds.getMinX() : bounds.getMaxX());
                    break;
                }
                case 3: {
                    hitZ = bounds.getMaxZ();
                    hitX = Math.abs(xOffset) < (bounds.getMaxX() - bounds.getMinX()) / 2.0 ? playerX : (playerX < (double)blockData.D() ? bounds.getMinX() : bounds.getMaxX());
                    break;
                }
                case 4: {
                    hitX = bounds.getMinX();
                    hitZ = Math.abs(zOffset) < (bounds.getMaxZ() - bounds.getMinZ()) / 2.0 ? playerZ : (playerZ < (double)blockData.G() ? bounds.getMinZ() : bounds.getMaxZ());
                    break;
                }
                case 5: {
                    hitX = bounds.getMaxX();
                    hitZ = Math.abs(zOffset) < (bounds.getMaxZ() - bounds.getMinZ()) / 2.0 ? playerZ : (playerZ < (double)blockData.G() ? bounds.getMinZ() : bounds.getMaxZ());
                    break;
                }
                case 1: {
                    hitY = bounds.getMaxY();
                    break;
                }
                case 0: {
                    hitY = bounds.getMinY();
                }
            }
            hitPosition = Vec3.create(hitX, hitY, hitZ);
        }
        return hitPosition;
    }

    private void selectHotbarSlot(int slot) {
        Minecraft.thePlayer().V$src$Lgg_vape_wrapper_impl_InventoryPlayer_$erqak6().g(slot);
    }

    private void showFailNotification(String message, boolean forceUpdate) {
        boolean shouldEnqueue = false;
        boolean expired = this.failNotification != null && this.failNotification.isExpired();
        if (this.failNotification == null) {
            this.failNotification = new Notification(NotificationType.ALERT, "Clutch Failed", new TextNotificationContent(message), 0.0, 0.0, 3500L);
            shouldEnqueue = true;
        } else if (expired || forceUpdate) {
            shouldEnqueue = expired;
            TextNotificationContent content = (TextNotificationContent)this.failNotification.getContent();
            content.setText(message);
            this.failNotification.setDuration(3500L);
        }
        if (shouldEnqueue) {
            Vape.INSTANCE.getNotificationManager().enqueue(this.failNotification, false);
        }
    }

    private boolean isPlayerMoving() {
        EntityPlayerSP entityPlayerSP = Minecraft.thePlayer();
        if (entityPlayerSP.isNull()) {
            return false;
        }
        return entityPlayerSP.t() != 0.0 || entityPlayerSP.q() != 0.0 || entityPlayerSP.T() != 0.0;
    }

    private BlockPlacementPathSegment simulateClutchPath(int maxTicks, BlockPlacementPathSegment pathSegment, World world, ItemStack itemStack, EntityPlayerSP localPlayer) {
        int worldBottom = ForgeVersion.MC_1_20_6.d() ? world.R() : 0;
        long startNanos = System.nanoTime();
        BlockState placedBlockState = BlockUtil.E(itemStack);
        HashMap<BlockData, BlockState> replacedStates = new HashMap<>();
        BlockPathPlanner simulation = new BlockPathPlanner(localPlayer, localPlayer, world, this.graph, this.pathPlanner);
        PlayerSimulationUtil.s(simulation.getSimulatedPlayer(), localPlayer);
        simulation.applySnapshot(this.graph);
        EntityPlayer simulatedPlayer = simulation.getSimulatedPlayer();
        if (this.counterMotion) {
            simulation.setForwardOnly();
        } else {
            simulation.restoreSnapshotInput();
        }
        int initialTargetCount = pathSegment.placementState.pendingTargets.size();
        boolean landedOnTarget = false;
        BlockPlacementPathSegment simulatedPath = new BlockPlacementPathSegment(pathSegment.startCoordinate, pathSegment.targetCoordinate, new ArrayList<Vec3d>());
        simulatedPath.placementState = new BlockPlacementPathSegmentState(pathSegment.placementState.placementFacing, new Vector<PlacementTarget>(pathSegment.placementState.pendingTargets));
        simulatedPath.simulatedPositions.add(simulation.getSimulatedPosition());
        FixedRotationController simulatedRotation = null;
        if (this.rotationController instanceof AdaptiveRotationController && RotationManager.INSTANCE.hasAdaptiveController()) {
            simulatedRotation = new AdaptiveRotationController(simulatedPlayer);
            ((AdaptiveRotationController)simulatedRotation).copyFrom((AdaptiveRotationController)this.rotationController);
            simulation.setRotationController(simulatedRotation);
        }
        int groundedTicks = 0;
        int remainingKnockbackTicks = this.knockbackTicks;
        boolean knockbackSettled = false;
        boolean simulationFinished = false;
        boolean pathExtended = false;
        try {
            for (int tick = 0; tick <= maxTicks; ++tick) {
                BlockInTargetRotationState rotationState = simulation.createRotationState();
                BlockPlacementGraph tickGraph = rotationState.getPlacementGraph();
                RayTraceResult rayTraceResult = simulation.rayTrace(3.0, 0.0f, false);
                rotationState.setRayTraceResult(rayTraceResult);
                simulatedPath.rotationStates.add(rotationState);
                if (simulatedPlayer.b$src$Z$fqlxe4()) {
                    BlockPathPlanner landingSimulation = new BlockPathPlanner(simulatedPlayer, localPlayer, world, tickGraph);
                    boolean wouldLeaveGround = this.simulateLandsOnTarget(simulatedPlayer, landingSimulation, simulatedPath);
                    if (++groundedTicks >= 5 || !wouldLeaveGround) {
                        simulationFinished = true;
                        break;
                    }
                } else {
                    groundedTicks = 0;
                }

                PlacementTarget placementTarget = null;
                float currentYaw = localPlayer.J();
                if (simulatedPath.placementState != null) {
                    placementTarget = this.resolvePlaceTarget(simulatedPath, localPlayer, world, placedBlockState);
                    if (placementTarget != null) {
                        float currentPitch;
                        if (simulatedRotation == null) {
                            if (RotationManager.INSTANCE.hasAdaptiveController()) {
                                AdaptiveRotationController activeRotation = (AdaptiveRotationController)RotationManager.INSTANCE.getActiveController();
                                currentYaw = activeRotation.getRenderedYaw();
                                currentPitch = activeRotation.getRenderedPitch();
                            } else {
                                currentYaw = localPlayer.J();
                                currentPitch = localPlayer.V();
                            }
                        } else if (simulatedRotation instanceof AdaptiveRotationController) {
                            currentYaw = ((AdaptiveRotationController)simulatedRotation).getRenderedYaw();
                            currentPitch = ((AdaptiveRotationController)simulatedRotation).getRenderedPitch();
                        } else {
                            currentYaw = simulatedRotation.getCurrentYaw();
                            currentPitch = simulatedRotation.getCurrentPitch();
                        }
                        if (this.canPlaceOnTarget(placementTarget, simulation)) {
                            BlockData placedBlock = placementTarget.getPlacedBlock();
                            BlockPos placedBlockPos = BlockPos.d(placedBlock);
                            replacedStates.put(placedBlock, world.getBlockState(placedBlockPos));
                            BlockUtil.z(world, placedBlockPos, placedBlockState);
                            if (simulatedPath.placementState != null) {
                                placementTarget = null;
                                Vector<PlacementTarget> pendingTargets = simulatedPath.placementState.pendingTargets;
                                if (!pendingTargets.isEmpty()) {
                                    pendingTargets.removeElementAt(0);
                                }
                                BlockPathPlanner landingSimulation = new BlockPathPlanner(simulatedPlayer, localPlayer, world, tickGraph);
                                if (pendingTargets.isEmpty() && initialTargetCount > 3 && !this.counterMotion && !this.graph.jumpKeyDown && this.simulateLandsOnTarget(simulatedPlayer, landingSimulation, simulatedPath)) {
                                    EntityPlayer landingPlayer = landingSimulation.getSimulatedPlayer();
                                    int landingX = MathUtil.floor(landingPlayer.z());
                                    int landingZ = MathUtil.floor(landingPlayer.h());
                                    int deltaX = landingX - simulatedPath.targetCoordinate.B();
                                    int deltaZ = landingZ - simulatedPath.targetCoordinate.A();
                                    if (Math.abs(deltaX) + Math.abs(deltaZ) < 3) {
                                        BlockCoordinate extensionStart = simulatedPath.targetCoordinate;
                                        BlockCoordinate extensionTarget = new BlockCoordinate(landingX, simulatedPath.targetCoordinate.E(), landingZ);
                                        BlockPlacementPathSegment extension = new BlockPlacementPathSegment(extensionStart, extensionTarget, new ArrayList<Vec3d>());
                                        this.planPlacementSearch(extension, localPlayer, world, new ArrayList<Vec3d>());
                                        if (extension.placementState != null && extension.placementState.pendingTargets != null && !extension.placementState.pendingTargets.isEmpty()) {
                                            pendingTargets.addAll(extension.placementState.pendingTargets);
                                            pathSegment.placementState.addTargets(extension.placementState.pendingTargets);
                                            pathSegment.targetCoordinate = extension.targetCoordinate;
                                            pathExtended = true;
                                        }
                                    }
                                }
                                while (!pendingTargets.isEmpty()) {
                                    PlacementTarget candidate = pendingTargets.firstElement();
                                    if (candidate == null) continue;
                                    BlockData candidateBlock = candidate.getPlacedBlock();
                                    Block block = world.getBlockByPos(candidateBlock.D(), candidateBlock.B(), candidateBlock.G());
                                    if (BlockUtil.u(block)) {
                                        if (ClutchPlacementPathUtils.isPlacementSpaceClear(world, localPlayer, candidateBlock)) {
                                            placementTarget = candidate;
                                            break;
                                        }
                                        simulatedPath.fail("[SIM] Entity collision");
                                        break;
                                    }
                                    simulatedPath.hitVector = null;
                                    pendingTargets.removeElementAt(0);
                                }
                                if (placementTarget == null && this.forcingCounterMotion && simulatedRotation != null) {
                                    float targetYaw = this.takingKnockback ? this.originalYaw : this.placeYaw;
                                    float yawDistance = Math.abs(MathUtil.wrapAngleTo180(targetYaw - currentYaw));
                                    simulatedRotation.setSpeed(yawDistance / 1.8f / 3.0f);
                                    simulatedRotation.setTargetRotation(targetYaw, simulatedRotation.getTargetPitch());
                                }
                            }
                        }
                    } else if (simulatedPath.placementState != null && simulatedPath.getExpectedPlacementCount() > maxTicks) {
                        break;
                    }
                }

                boolean wasOnGround = simulatedPlayer.b$src$Z$fqlxe4();
                BlockInBooleanState strafeState = null;
                float movementYaw = this.takingKnockback && knockbackSettled ? this.originalYaw : this.placeYaw;
                if (simulatedRotation != null && !(simulatedRotation instanceof AdaptiveRotationController)) {
                    strafeState = this.computeStrafeState(simulatedPlayer, movementYaw, this.graph.forwardKeyDown, this.graph.backwardKeyDown, this.graph.leftKeyDown, this.graph.rightKeyDown);
                    if (strafeState != null) {
                        simulation.setDirectionalKeys(strafeState.correctedForward, strafeState.correctedBackward, strafeState.correctedLeft, strafeState.correctedRight);
                    }
                }
                simulation.simulateTick(false);
                if (strafeState != null) {
                    simulation.setDirectionalKeys(strafeState.originalForward, strafeState.originalBackward, strafeState.originalLeft, strafeState.originalRight);
                    tickGraph.forwardKeyDown = strafeState.originalForward;
                    tickGraph.backwardKeyDown = strafeState.originalBackward;
                    tickGraph.leftKeyDown = strafeState.originalLeft;
                    tickGraph.rightKeyDown = strafeState.originalRight;
                }
                if (!wasOnGround && simulatedPlayer.b$src$Z$fqlxe4() && MathUtil.floor(simulatedPlayer.N()) >= simulatedPath.targetCoordinate.E() + 1) {
                    landedOnTarget = true;
                    if (!this.forcingCounterMotion || this.takingKnockback && this.graph.jumpKeyDown) {
                        simulationFinished = true;
                    }
                } else if (wasOnGround && !simulatedPlayer.b$src$Z$fqlxe4() && pathExtended) {
                    simulationFinished = false;
                }
                if (this.takingKnockback && (simulatedPlayer.b$src$Z$fqlxe4() || --remainingKnockbackTicks == 0)) {
                    knockbackSettled = true;
                }

                BlockInTargetRotationState nextRotationState = simulation.createRotationState();
                nextRotationState.setRayTraceResult(rayTraceResult);
                if (placementTarget != null) {
                    float yaw;
                    float pitch;
                    if (simulatedRotation == null) {
                        if (RotationManager.INSTANCE.hasAdaptiveController()) {
                            AdaptiveRotationController activeRotation = (AdaptiveRotationController)RotationManager.INSTANCE.getActiveController();
                            yaw = activeRotation.getRenderedYaw();
                            pitch = activeRotation.getRenderedPitch();
                        } else {
                            yaw = localPlayer.J();
                            pitch = localPlayer.V();
                        }
                    } else if (simulatedRotation instanceof AdaptiveRotationController) {
                        yaw = ((AdaptiveRotationController)simulatedRotation).getRenderedYaw();
                        pitch = ((AdaptiveRotationController)simulatedRotation).getRenderedPitch();
                    } else {
                        yaw = simulatedRotation.getCurrentYaw();
                        pitch = simulatedRotation.getCurrentPitch();
                    }
                    if (placementTarget.hitPoint != null) {
                        rotationState.setTargetVector(placementTarget.hitPoint.toVec3d());
                        rotationState.setHitVector(simulatedPath.hitVector.toVec3d());
                    }
                    int placementTicks = this.simulatePlacementTick(simulatedPath, placementTarget, simulatedPlayer, localPlayer, world, yaw, pitch, tickGraph);
                    Vec3 eyePosition = simulatedPath.hitVector;
                    float targetYaw = this.takingKnockback && knockbackSettled ? this.originalYaw : this.placeYaw;
                    simulatedRotation = this.buildRotation(simulatedPlayer, eyePosition, placementTarget.hitPoint, simulatedRotation, placementTicks, targetYaw);
                    simulation.setRotationController(simulatedRotation);
                }
                simulation.updateRotation();
                if (this.forcingCounterMotion && !this.takingKnockback) {
                    simulation.finishSimulation();
                }
                simulatedPath.simulatedPositions.add(simulation.getSimulatedPosition());
                if (simulatedPlayer.N() <= (double)worldBottom) {
                    simulatedPath.fail("Player would be below the world");
                    this.debugLog("[SIM] Player is below the world at tick " + tick);
                } else if (simulatedPlayer.N() < (double)(simulatedPath.targetCoordinate.E() - 1) && simulatedPlayer.N() < (double)(simulatedPath.startCoordinate.E() - 1)) {
                    simulatedPath.fail(landedOnTarget && !simulatedPlayer.b$src$Z$fqlxe4() ? "Player would fall off after landing" : "Player would be too low to land");
                    this.debugLog("[SIM] Player is too low to land on block at tick " + tick + " " + landedOnTarget);
                } else if ((double)pathSegment.targetCoordinate.E() > simulatedPlayer.R$src$Lgg_vape_wrapper_impl_AxisAlignedBB_$r19dfl().getMinY() && (double)pathSegment.startCoordinate.E() > simulatedPlayer.R$src$Lgg_vape_wrapper_impl_AxisAlignedBB_$r19dfl().getMinY()) {
                    simulatedPath.fail(landedOnTarget && !simulatedPlayer.b$src$Z$fqlxe4() ? "Player would fall off after landing" : "Player would be too low to land");
                    this.debugLog("[SIM] Player is too low to land on block at tick " + tick + " " + landedOnTarget);
                } else if (placementTarget != null || simulatedPath.placementState != null || !simulationFinished) {
                    continue;
                }
                break;
            }
        } catch (Exception exception) {
            Vape.logThrowable(exception);
        }
        for (Map.Entry<BlockData, BlockState> entry : replacedStates.entrySet()) {
            BlockUtil.z(world, BlockPos.d(entry.getKey()), entry.getValue());
        }
        pathSegment.rotationStates.addAll(simulatedPath.rotationStates);
        if (landedOnTarget && (simulatedPlayer.b$src$Z$fqlxe4() || simulationFinished)) {
            pathSegment.simulatedPositions.clear();
            pathSegment.simulatedPositions.addAll(simulatedPath.simulatedPositions);
            return pathSegment;
        }
        int remainingTargets = simulatedPath.placementState != null ? simulatedPath.placementState.pendingTargets.size() : -1;
        if (simulatedPath.hasFailed()) {
            pathSegment.failureReason = simulatedPath.failureReason;
            return pathSegment;
        }
        if (remainingTargets > 0) {
            pathSegment.failureReason = simulatedPath.failureReason;
            return pathSegment;
        }
        double elapsedMillis = (double)(System.nanoTime() - startNanos) / 1000000.0;
        this.debugLog("Failed to simulate clutch path for " + maxTicks + " ticks in " + elapsedMillis + "ms with " + remainingTargets + "/" + initialTargetCount + " blocks remaining");
        return null;
    }

    private boolean isWhitelistedBlock(ItemStack itemStack) {
        if (itemStack.isNull() || itemStack.getItem().isNull()) {
            return false;
        }
        return this.whitelistBlocks.matches(itemStack);
    }

    private PlacementTarget findPlaceTarget(BlockPlacementPathSegment pathSegment, EntityPlayer player, World world) {
        return this.resolvePlaceTarget(pathSegment, player, world, null);
    }

    private void sortClutchPaths(ArrayList<BlockPlacementPathSegment> paths, Vec3d playerPosition) {
        paths.sort(Comparator.comparingDouble(path -> this.clutchPathSortCost(playerPosition, path)));
    }

    private boolean selectBlockSlot(EntityPlayerSP localPlayer) {
        int slot = this.findBestBlockSlot();
        if (slot == -1) {
            return false;
        }
        if (this.previousSlot == -1) {
            this.previousSlot = localPlayer.V$src$Lgg_vape_wrapper_impl_InventoryPlayer_$erqak6().v();
        }
        this.selectHotbarSlot(slot);
        return true;
    }

    @EventHandler
    public void onClickMouse(EventClickMouse eventClickMouse) {
        GameSettings gameSettings = eventClickMouse.getGameSettings();
        if (this.clutchPath != null) {
            eventClickMouse.setCancelled(true);
            gameSettings.F().e();
        }
    }

    private int findBestBlockSlot() {
        ArrayList<Integer> validSlots = new ArrayList<>();
        for (int i = 0; i < 9; ++i) {
            ItemStack stack = Minecraft.thePlayer().V$src$Lgg_vape_wrapper_impl_InventoryPlayer_$erqak6().c(i);
            if (!stack.isNotNull() || !this.isValidBlockItem(stack)) continue;
            validSlots.add(i);
        }
        if (validSlots.isEmpty()) {
            return -1;
        }
        for (String preferredName : this.defaultBlockNames) {
            for (Integer slot : validSlots) {
                if (!Minecraft.thePlayer().V$src$Lgg_vape_wrapper_impl_InventoryPlayer_$erqak6().c(slot).x().contains(preferredName)) continue;
                return slot;
            }
        }
        return validSlots.get(0);
    }

    private int countBlocks() {
        EntityPlayerSP localPlayer = Minecraft.thePlayer();
        InventoryPlayer inventory = localPlayer.V$src$Lgg_vape_wrapper_impl_InventoryPlayer_$erqak6();
        if (inventory.isNull() || localPlayer.isNull()) {
            return 0;
        }
        int blockCount = 0;
        if (this.heldWhitelist.getEffectiveValue().booleanValue()) {
            ItemStack heldStack = localPlayer.B$src$Lgg_vape_wrapper_impl_ItemStack_$impdvt();
            if (heldStack.isNull() || heldStack.getItem().isNull()) {
                return 0;
            }
            if (!this.isWhitelistedBlock(heldStack)) {
                return 0;
            }
            if (!this.isValidBlockItem(heldStack)) {
                return 0;
            }
            return heldStack.t();
        }
        for (int i = 0; i < 9; ++i) {
            ItemStack itemStack = inventory.c(i);
            if (itemStack.isNull() || !itemStack.getItem().isInstance(MappedClasses.Vw)) continue;
            if (this.isValidBlockItem(itemStack)) {
                blockCount += itemStack.t();
                continue;
            }
            for (String preferredName : this.defaultBlockNames) {
                if (!itemStack.x().contains(preferredName)) continue;
                blockCount += itemStack.t();
            }
        }
        return blockCount;
    }

    public void setSavedYaw(double savedYaw) {
        this.savedYaw = savedYaw;
    }

    public VisibleModuleList<BlockData> getPlacedBlocks() {
        return this.placedBlocks;
    }

    private void resetRotation(EntityPlayerSP localPlayer) {
        boolean releaseCurrentRotation = true;
        if (this.resetAngle.getEffectiveValue().booleanValue() && !this.silentAim.getEffectiveValue().booleanValue() && !this.rotationClaim.isBlockedFor(this) && this.savedYaw != -999.0 && this.rotationController != null) {
            RotationManager.INSTANCE.releaseController(this.rotationController);
            this.rotationController = null;
            float wrappedYawDelta = MathUtil.wrapAngleTo180(localPlayer.J() - (float)this.savedYaw);
            float resetSpeed = Math.max(wrappedYawDelta / 90.0f * 5.0f, 1.0f);
            float yawDelta = localPlayer.J() - (float)this.savedYaw;
            ClutchThresholdRotationController resetController = new ClutchThresholdRotationController(this, Minecraft.thePlayer(), yawDelta, localPlayer.V() - (float)this.savedPitch);
            resetController.setRandomizeMovement(true);
            resetController.setScaleAxesProportionally(true);
            resetController.setLinearAcceleration(true);
            resetController.setSpeed(resetSpeed);
            RotationManager.INSTANCE.setController(resetController);
            releaseCurrentRotation = false;
        }
        if (releaseCurrentRotation && this.rotationController != null) {
            this.rotationController.setClampStepToRemaining(true);
            this.rotationController.setCubicAcceleration(true);
            this.rotationController.setScaleAxesProportionally(true);
            this.rotationController.setTolerance(0.0f);
            this.rotationController.setSpeed(3.0f);
            RotationManager.INSTANCE.releaseController(this.rotationController);
        }
    }

    private boolean canPlaceOnTarget(PlacementTarget placementTarget, BlockPathPlanner simulation) {
        boolean validTarget = false;
        if (placementTarget != null) {
            EntityPlayer simulatedPlayer = simulation.getSimulatedPlayer();
            AxisAlignedBB placementBounds = BlockUtil.F(simulatedPlayer.getWorld(), placementTarget.getPlacedBlock());
            AxisAlignedBB playerBounds = simulatedPlayer.u$src$Lgg_vape_wrapper_impl_AxisAlignedBB_$kogbsu();
            if (playerBounds.intersects(placementBounds)) {
                return false;
            }
            RayTraceResult rayTraceResult = simulation.rayTrace(3.0, 0.0f, false);
            if (rayTraceResult.isBlockHit()) {
                boolean hitSupportBlock;
                if (ForgeVersion.MC_1_7_10.Y()) {
                    hitSupportBlock = rayTraceResult.getBlockPos().equals(BlockPos.d(placementTarget.supportBlock));
                } else {
                    hitSupportBlock = rayTraceResult.g() == placementTarget.supportBlock.D() && rayTraceResult.T() == placementTarget.supportBlock.B() && rayTraceResult.a$src$I$8nuo9d() == placementTarget.supportBlock.G();
                }
                if (hitSupportBlock) {
                    EnumFacing requiredFacing = placementTarget.offsetFromSupport ? placementTarget.facing : null;
                    validTarget = requiredFacing == null || requiredFacing.equals(rayTraceResult.getSideHit());
                } else {
                    BlockPos blockPos = rayTraceResult.getBlockPos();
                    BlockData placedBlock = placementTarget.getPlacedBlock();
                    if (placedBlock.y(blockPos.offset(rayTraceResult.getSideHit()))) {
                        validTarget = true;
                    }
                }
            }
        }
        return validTarget;
    }

    private BlockData getCachedBlockData(Map<Integer, BlockData> cache, int x, int y, int z) {
        int key = Objects.hash(x, y, z);
        return cache.computeIfAbsent(key, ignored -> new BlockData(x, y, z));
    }

    private BlockPlacementPathSegment searchClutchPath(World world, BlockPathPlanner simulation, ItemStack itemStack) {
        if (!this.pendingSegments.isEmpty()) {
            return null;
        }
        int worldBottom = ForgeVersion.MC_1_20_6.d() ? world.R() : 0;
        BlockPlacementGraph initialGraph = simulation.createRotationState().getPlacementGraph();
        HashMap<Integer, BlockData> coordinateCache = new HashMap<>();
        EntityPlayerSP localPlayer = Minecraft.thePlayer();
        ArrayList<Vec3d> simulatedPositions = new ArrayList<>();
        EntityPlayer simulatedPlayer = simulation.getSimulatedPlayer();
        boolean staircaseSearch = this.staircaseQueued && simulatedPlayer.q() > 0.0;
        if (staircaseSearch) {
            this.fallTargetY = localPlayer.N() + simulatedPlayer.q();
        }

        int estimatedTicks = 0;
        double estimatedY = simulatedPlayer.N();
        double estimatedMotionY = simulatedPlayer.q();
        boolean distantFallTarget = Math.abs(this.fallTargetY - localPlayer.N()) > 1.0;
        while (estimatedTicks < 20) {
            estimatedY += estimatedMotionY;
            if (!distantFallTarget && estimatedY < this.fallTargetY) break;
            estimatedMotionY = (estimatedMotionY - 0.08) * (double)0.98f;
            ++estimatedTicks;
        }

        double initialMotionY = simulatedPlayer.q();
        boolean initiallyGrounded = simulatedPlayer.b$src$Z$fqlxe4();
        simulation.restoreSnapshotInput();
        simulation.simulateTick();
        double projectedX = simulatedPlayer.z() - simulatedPlayer.f();
        double projectedZ = simulatedPlayer.h() - simulatedPlayer.R();
        double projectedMotionX = simulatedPlayer.t();
        double projectedMotionZ = simulatedPlayer.T();
        simulation.applySnapshot(initialGraph);
        for (int tick = 0; tick < estimatedTicks - 1; ++tick) {
            double friction = (initiallyGrounded && initialMotionY > 0.0 ? (double)0.6f : 1.0) * (double)0.91f;
            if (Math.abs(projectedMotionX) < 0.005) projectedMotionX = 0.0;
            if (Math.abs(projectedMotionZ) < 0.005) projectedMotionZ = 0.0;
            projectedX += projectedMotionX;
            projectedZ += projectedMotionZ;
            projectedMotionX *= friction;
            projectedMotionZ *= friction;
            initiallyGrounded = false;
        }
        int estimatedBlockDistance = (int)Math.round(Math.sqrt(projectedX * projectedX + projectedZ * projectedZ));
        if (estimatedBlockDistance >= 4) {
            this.debugLog("Clutch is not possible standing still. Forcing counter motion.");
            this.graph.jumpKeyDown = this.takingKnockback && gg.vape.config.ClientSettings.isPhysicalKeyDown(Minecraft.gameSettings().O());
            this.graph.forwardKeyDown = true;
            this.graph.backwardKeyDown = false;
            this.graph.leftKeyDown = false;
            this.graph.rightKeyDown = false;
            this.counterMotion = true;
            this.forcingCounterMotion = true;
            this.placeYaw = MovementInputHelper.getMovementYaw(localPlayer);
        } else {
            this.forcingCounterMotion = false;
            this.counterMotion = false;
        }
        if (this.counterMotion) {
            simulation.setForwardOnly();
        } else {
            simulation.restoreSnapshotInput();
        }
        this.debugLog("Estimated ticks: " + estimatedTicks + " Blocks: " + estimatedBlockDistance);

        double motionX = simulatedPlayer.t();
        double motionZ = simulatedPlayer.T();
        BlockData blockBelow = this.getCachedBlockData(coordinateCache, MathUtil.floor(simulatedPlayer.z()), MathUtil.floor(simulatedPlayer.N() - 0.015625), MathUtil.floor(simulatedPlayer.h()));
        boolean standingAboveBlock = !BlockUtil.u(world.getBlockByPos(blockBelow.D(), blockBelow.B(), blockBelow.G()));
        Vec3 eyePosition = Vec3.create(simulatedPlayer.z(), simulatedPlayer.N() + (double)simulatedPlayer.X(), simulatedPlayer.h());
        Vec3 rotationTarget;
        if (this.forcingCounterMotion) {
            rotationTarget = eyePosition.addVector(-motionX, 0.0, -motionZ);
        } else {
            Vec3 lookDirection = simulatedPlayer.J(1.0f);
            rotationTarget = eyePosition.addVector(lookDirection.getX() * 5.0, lookDirection.getY() * 5.0, lookDirection.getZ() * 5.0);
        }
        FixedRotationController searchRotation = this.buildRotation(simulatedPlayer, eyePosition, rotationTarget, null, 1, this.placeYaw);
        simulation.setRotationController(searchRotation);

        HashMap<BlockData, Double> pathScores = new HashMap<>();
        int maxSearchRadius = 4;
        int initialSearchRadius = standingAboveBlock ? 1 : maxSearchRadius;
        Vec3d simulatedPosition = simulation.getSimulatedPosition();
        simulatedPositions.add(simulatedPosition);
        int lowestYOffset = localPlayer.b$src$Z$fqlxe4() ? -2 : (localPlayer.q() > 0.0 ? -3 : -1);
        boolean knockbackSettled = false;
        int simulatedTicks = 0;
        int remainingKnockbackTicks = this.knockbackTicks;
        for (int tick = 0; tick <= Math.max(estimatedTicks, 15); ++tick) {
            double playerFeetY = simulatedPlayer.R$src$Lgg_vape_wrapper_impl_AxisAlignedBB_$r19dfl().getMinY();
            ++simulatedTicks;
            for (int yOffset = 0; yOffset >= lowestYOffset; --yOffset) {
                for (int radius = 0; radius < maxSearchRadius; ++radius) {
                    int minXOffset = motionX >= 0.0 ? -initialSearchRadius : -radius;
                    int maxXOffset = motionX >= 0.0 ? radius : initialSearchRadius;
                    int minZOffset = motionZ >= 0.0 ? -initialSearchRadius : -radius;
                    int maxZOffset = motionZ >= 0.0 ? radius : initialSearchRadius;
                    for (int xOffset = minXOffset; xOffset <= maxXOffset; ++xOffset) {
                        for (int zOffset = minZOffset; zOffset <= maxZOffset; ++zOffset) {
                            if (Math.abs(xOffset) != radius && Math.abs(zOffset) != radius) continue;
                            BlockData candidate = this.getCachedBlockData(coordinateCache,
                                    MathUtil.floor(simulatedPlayer.z()) + xOffset,
                                    MathUtil.floor(simulatedPlayer.N()) + yOffset,
                                    MathUtil.floor(simulatedPlayer.h()) + zOffset);
                            if (this.rejectedBlocks.contains(candidate)) continue;
                            Block block = world.getBlockByPos(candidate.D(), candidate.B(), candidate.G());
                            if (!this.placeableBlocks.contains(candidate) && BlockUtil.p(block) || BlockUtil.C(block) || !BlockUtil.b(block)) {
                                this.rejectedBlocks.add(candidate);
                                continue;
                            }
                            if ((double)(candidate.B() + 1) > playerFeetY) continue;
                            boolean placeable = this.placeableBlocks.contains(candidate) || !BlockUtil.u(block) && !ClutchPlacementPathUtils.isBlacklistedPlacementBlock(block);
                            if (!placeable) {
                                this.rejectedBlocks.add(candidate);
                                continue;
                            }
                            this.placeableBlocks.add(candidate);
                            Vec3 candidateCenter = Vec3.create((double)candidate.D() + 0.5, (double)candidate.B() + 0.5, (double)candidate.G() + 0.5);
                            Vec3 simulatedEyePosition = simulatedPosition.toVec3().addVector(0.0, simulatedPlayer.X(), 0.0);
                            RotationAngles rotation = RotationVectorMath.d(simulatedEyePosition, candidateCenter, searchRotation.getCurrentYaw(), searchRotation.getCurrentPitch());
                            double yawDistance = Math.abs(MathUtil.wrapAngleTo180(rotation.getYaw() - searchRotation.getCurrentYaw()));
                            if (yawDistance > 120.0) {
                                yawDistance = Math.abs(MathUtil.wrapAngleTo180(yawDistance + 180.0));
                            }
                            this.blockGraphMap.putIfAbsent(candidate, new HashSet<>());
                            Double previousScore = pathScores.get(candidate);
                            if (previousScore == null || yawDistance < previousScore) {
                                pathScores.put(candidate, yawDistance);
                            }
                        }
                    }
                }
            }

            BlockPlacementGraph tickGraph = simulation.createRotationState().getPlacementGraph();
            BlockInBooleanState strafeState = null;
            if (!this.silentAim.getEffectiveValue().booleanValue()) {
                strafeState = this.computeStrafeState(simulatedPlayer, this.placeYaw, this.graph.forwardKeyDown, this.graph.backwardKeyDown, this.graph.leftKeyDown, this.graph.rightKeyDown);
                if (strafeState != null) {
                    simulation.setDirectionalKeys(strafeState.correctedForward, strafeState.correctedBackward, strafeState.correctedLeft, strafeState.correctedRight);
                }
            }
            if (knockbackSettled && searchRotation instanceof AdaptiveRotationController) {
                ((AdaptiveRotationController)searchRotation).setReferenceYawOverride(Float.valueOf(this.originalYaw));
            }
            simulation.simulateTick();
            if (strafeState != null) {
                simulation.setDirectionalKeys(strafeState.originalForward, strafeState.originalBackward, strafeState.originalLeft, strafeState.originalRight);
                tickGraph.forwardKeyDown = strafeState.originalForward;
                tickGraph.backwardKeyDown = strafeState.originalBackward;
                tickGraph.leftKeyDown = strafeState.originalLeft;
                tickGraph.rightKeyDown = strafeState.originalRight;
            }
            simulation.finishSimulation();
            simulatedPosition = simulation.getSimulatedPosition();
            simulatedPositions.add(simulatedPosition);
            if (this.takingKnockback && remainingKnockbackTicks > 0 && --remainingKnockbackTicks == 0) {
                knockbackSettled = true;
            }
            if (simulatedPlayer.q() <= 0.0) {
                int landingY = MathUtil.floor(simulatedPlayer.N()) - 1;
                BlockData landingBlock = new BlockData(MathUtil.floor(simulatedPlayer.z()), landingY, MathUtil.floor(simulatedPlayer.h()));
                for (BlockData supportBlock : this.blockGraphMap.keySet()) {
                    boolean validHeight = staircaseSearch ? landingY >= supportBlock.B() : landingY == supportBlock.B();
                    int distance = Math.abs(supportBlock.D() - landingBlock.D())
                            + Math.abs(supportBlock.G() - landingBlock.G())
                            + Math.abs(supportBlock.B() - landingBlock.B()) - 1;
                    if (distance <= simulatedTicks && validHeight && !supportBlock.L(landingBlock)) {
                        this.blockGraphMap.get(supportBlock).add(landingBlock);
                    }
                }
            }
            if (simulatedPlayer.N() <= (double)worldBottom) break;
        }

        Vec3d playerPosition = new Vec3d(localPlayer.z(), localPlayer.N(), localPlayer.h());
        ArrayList<BlockPlacementPathSegment> candidatePaths = new ArrayList<>();
        for (Map.Entry<BlockData, Double> entry : pathScores.entrySet()) {
            BlockData supportBlock = entry.getKey();
            HashSet<BlockData> landingBlocks = this.blockGraphMap.get(supportBlock);
            if (supportBlock == null || landingBlocks == null || landingBlocks.isEmpty()) continue;
            ArrayList<BlockPlacementPathSegment> supportPaths = new ArrayList<>();
            landingLoop: for (BlockData landingBlock : landingBlocks) {
                BlockCoordinate start = new BlockCoordinate(supportBlock.D(), supportBlock.B(), supportBlock.G());
                BlockCoordinate target = new BlockCoordinate(landingBlock.D(), landingBlock.B(), landingBlock.G());
                int deltaX = target.B() - start.B();
                int deltaY = target.E() - start.E();
                int deltaZ = target.A() - start.A();
                int xFacing = deltaX > 0 ? 5 : (deltaX < 0 ? 4 : -1);
                int yFacing = deltaY > 0 ? 1 : (deltaY < 0 ? 0 : -1);
                int zFacing = deltaZ > 0 ? 3 : (deltaZ < 0 ? 2 : -1);
                for (int facingIndex : new int[]{xFacing, yFacing, zFacing}) {
                    if (facingIndex == -1) continue;
                    EnumFacing facing = EnumFacing.t()[facingIndex];
                    BlockData placedBlock = start.O().R(facing);
                    Block block = world.getBlockByPos(placedBlock.D(), placedBlock.B(), placedBlock.G());
                    if (this.placedBlocks.Y(placedBlock) || !BlockUtil.u(block)) continue;
                    supportPaths.add(new BlockPlacementPathSegment(start, target, new ArrayList<Vec3d>()));
                    continue landingLoop;
                }
            }
            if (supportPaths.isEmpty()) continue;
            this.sortClutchPaths(supportPaths, playerPosition);
            candidatePaths.add(supportPaths.get(0));
        }
        if (candidatePaths.isEmpty()) {
            return null;
        }
        this.debugLog("Temp paths: " + candidatePaths.size());
        this.sortClutchPaths(candidatePaths, playerPosition);
        int checkedPaths = 0;
        for (BlockPlacementPathSegment candidatePath : candidatePaths) {
            if (checkedPaths > 3) {
                this.debugLog("Checked 3 paths already");
                break;
            }
            this.planPlacementSearch(candidatePath, localPlayer, world, simulatedPositions);
            if (candidatePath.placementState == null || candidatePath.placementState.pendingTargets.isEmpty()) continue;
            ++checkedPaths;
            candidatePath.simulationTickLimit = estimatedTicks;
            int extraFallTicks = (double)candidatePath.targetCoordinate.E() < localPlayer.N()
                    ? (int)Math.ceil((localPlayer.N() - (double)candidatePath.targetCoordinate.E()) / 0.08)
                    : 7;
            BlockPlacementPathSegment result = this.simulateClutchPath(estimatedTicks + Math.min(extraFallTicks, 25), candidatePath, world, itemStack, localPlayer);
            if (result != null && !result.hasFailed()) {
                return result;
            }
        }
        BlockPlacementPathSegment firstPath = candidatePaths.get(0);
        return firstPath.hasFailed() ? firstPath : null;
    }

    @Override
    public void onEnable() {
        ClientSettings.getFrame(ActiveModuleStackFrame.class).addModule(this);
    }

    private void resetClutch(EntityPlayerSP localPlayer) {
        this.clutchPath = null;
        this.placeTarget = null;
        this.returnDelayTicks = (int)Math.round(this.returnDelay.getRandomValue());
        if (this.rotationController != null) {
            this.resetRotation(localPlayer);
        }
    }

    private double clutchPathSortCost(Vec3d playerPosition, BlockPlacementPathSegment pathSegment) {
        BlockData targetBlock = pathSegment.targetCoordinate.O();
        BlockData startBlock = pathSegment.startCoordinate.O();
        int horizontalDistance = Math.abs(startBlock.D() - targetBlock.D()) + Math.abs(startBlock.G() - targetBlock.G());
        double cost = (double)(horizontalDistance * 100);
        if (targetBlock.B() > startBlock.B()) {
            cost -= (double)((targetBlock.B() - startBlock.B()) * 200);
        }
        if (this.recentlyClutched) {
            cost += Math.sqrt(Math.pow((double)startBlock.D() + 0.5 - playerPosition.getX(), 2.0) + Math.pow((double)startBlock.G() + 0.5 - playerPosition.getZ(), 2.0)) * 1000.0;
        }
        return cost + Math.abs((double)(startBlock.B() + 1) - this.fallTargetY) * 200.0;
    }

    private void captureMovementInputs() {
        if (this.counterMotion) {
            this.inputForward = true;
            this.inputBack = false;
            this.inputLeft = false;
            this.inputRight = false;
        } else {
            this.inputForward = this.graph.forwardKeyDown;
            this.inputBack = this.graph.backwardKeyDown;
            this.inputLeft = this.graph.leftKeyDown;
            this.inputRight = this.graph.rightKeyDown;
        }
    }


    private PlacementTarget resolvePlaceTarget(BlockPlacementPathSegment pathSegment, EntityPlayer player, World world, @Nullable BlockState simulatedBlockState) {
        PlacementTarget resolvedTarget = null;
        Vector<PlacementTarget> pendingTargets = pathSegment.placementState.pendingTargets;
        while (!pendingTargets.isEmpty()) {
            PlacementTarget candidate = pendingTargets.firstElement();
            if (candidate == null) continue;
            BlockData supportBlock = candidate.supportBlock;
            Block block = world.getBlockByPos(supportBlock.D(), supportBlock.B(), supportBlock.G());
            if (simulatedBlockState != null && BlockUtil.p(block) && !BlockUtil.p(simulatedBlockState.getBlock())) {
                block = simulatedBlockState.getBlock();
            }
            if (BlockUtil.u(block) && (candidate.offsetFromSupport || BlockUtil.J(block))) {
                pathSegment.fail("Block to click on was removed");
                return null;
            }
            BlockData placedBlock = candidate.getPlacedBlock();
            Block placedBlockState = world.getBlockByPos(placedBlock.D(), placedBlock.B(), placedBlock.G());
            if (BlockUtil.u(placedBlockState)) {
                if (ClutchPlacementPathUtils.isPlacementSpaceClear(world, player, placedBlock)) {
                    resolvedTarget = candidate;
                    break;
                }
                pathSegment.fail("Entity blocking placement");
                break;
            }
            pathSegment.hitVector = null;
            pendingTargets.removeElementAt(0);
        }
        return resolvedTarget;
    }

    private void resetState() {
        if (this.pendingInputApply) {
            this.clearPendingInputs();
            if (Minecraft.currentScreen().isNull()) {
                MovementInputHelper.restorePhysicalInput(false);
            }
            this.pendingInputApply = false;
        }
        if (this.clutchPath != null) {
            if (this.knockbackTicks <= 0) {
                this.takingKnockback = false;
                this.forcingCounterMotion = false;
                this.debugLog("\nNO LONGER TAKING KNOCKBACK\n");
            }
            if (this.forcingCounterMotion && !this.takingKnockback && this.clutchMoveDelay.getMaximumInt() > 0) {
                this.inputRight = false;
                this.inputLeft = false;
                this.inputBack = false;
                this.inputForward = false;
                this.applyMovementInputs();
                this.forcingCounterMotion = false;
                this.moveDelayTicks = (int)Math.round(this.clutchMoveDelay.getRandomValue());
            } else {
                MovementInputHelper.restorePhysicalInput(false);
            }
            this.returnDelayTicks = (int)Math.round(this.returnDelay.getRandomValue());
            this.resetAngleDelayTicks = (int)Math.round(this.resetAngleDelay.getRandomValue());
        }
        if (this.clutchPath != null) {
            this.landTimer.reset();
        }
        this.clutchPath = null;
        if (this.claimActive) {
            this.claimActive = false;
            SharedModuleControlClaims.mouseButtons.unlock();
        }
        this.placeTarget = null;
        this.blockGraphMap.clear();
        this.pendingSegments.clear();
        DEBUG_PLACEMENT_PATHS.clear();
        this.rayTrace = null;
        if (RotationManager.INSTANCE.getActiveController() == null || RotationManager.INSTANCE.getActiveController() != this.rotationController || this.rotationController != null && !this.rotationController.shouldRetainAfterCompletion() && this.rotationController.isComplete()) {
            this.rotationController = null;
            this.rotationClaim.release(this);
            if (this.pauseRequested) {
                this.pauseRequested = false;
                super.setEnabled(false, true);
            }
        }
        this.forcingCounterMotion = false;
        this.counterMotion = false;
        this.movementLock.unlock();
    }

    private BlockPlacementPathSegment computeClutchPath(World world, EntityPlayerSP localPlayer, ItemStack itemStack) {
        this.rejectedBlocks.clear();
        this.placeableBlocks.clear();
        this.blockGraphMap.clear();
        this.pendingSegments.clear();
        this.placeYaw = this.savedYaw != -999.0 ? (float)this.savedYaw : (FreeLookHudModule.isActive() ? FreeLookHudModule.getSavedPitch() : localPlayer.J());
        this.originalYaw = this.placeYaw;
        this.counterMotion = false;
        this.forcingCounterMotion = false;
        this.recentlyClutched = !this.landTimer.hasTimeElapsed(250L);
        this.pathPlanner = new BlockPathPlanner(localPlayer, localPlayer, world, this.graph);
        this.pathPlannerReturn = new BlockPathPlanner(localPlayer, localPlayer, world, this.graph);
        BlockPathPlanner searchSimulation = new BlockPathPlanner(localPlayer, localPlayer, world, this.graph);
        BlockPlacementPathSegment pathSegment = this.searchClutchPath(world, searchSimulation, itemStack);
        if (pathSegment != null && !pathSegment.hasFailed() && this.limitBlocks.getEffectiveValue().booleanValue()) {
            int requiredBlocks = pathSegment.getPendingPlacementCount();
            if (requiredBlocks > ((Double)this.maxBlocks.getValue()).intValue()) {
                pathSegment.fail("Requires " + requiredBlocks + " blocks (max: " + ((Double)this.maxBlocks.getValue()).intValue() + ")");
            }
        }
        return pathSegment;
    }

    private boolean isLookingAtTarget() {
        boolean validTarget = false;
        if (this.placeTarget != null) {
            EntityPlayerSP localPlayer = Minecraft.thePlayer();
            AxisAlignedBB placementBounds = BlockUtil.F(localPlayer.getWorld(), this.placeTarget.getPlacedBlock());
            AxisAlignedBB playerBounds = localPlayer.u$src$Lgg_vape_wrapper_impl_AxisAlignedBB_$kogbsu();
            if (playerBounds.intersects(placementBounds)) {
                return false;
            }
            RayTraceResult rayTraceResult = RotationManager.INSTANCE.getNormalReachRayTrace();
            if (rayTraceResult.isBlockHit()) {
                boolean hitSupportBlock;
                if (ForgeVersion.MC_1_7_10.Y()) {
                    hitSupportBlock = rayTraceResult.getBlockPos().equals(BlockPos.d(this.placeTarget.supportBlock));
                } else {
                    hitSupportBlock = rayTraceResult.g() == this.placeTarget.supportBlock.D() && rayTraceResult.T() == this.placeTarget.supportBlock.B() && rayTraceResult.a$src$I$8nuo9d() == this.placeTarget.supportBlock.G();
                }
                if (hitSupportBlock) {
                    EnumFacing requiredFacing = this.placeTarget.offsetFromSupport ? this.placeTarget.facing : null;
                    validTarget = requiredFacing == null || requiredFacing.equals(rayTraceResult.getSideHit());
                } else {
                    BlockPos blockPos = rayTraceResult.getBlockPos();
                    BlockData placedBlock = this.placeTarget.getPlacedBlock();
                    if (placedBlock.y(blockPos.offset(rayTraceResult.getSideHit()))) {
                        validTarget = true;
                    }
                }
            }
        }
        return validTarget;
    }


    private void clearPendingInputs() {
        this.pendingInputForward = null;
        this.pendingInputBack = null;
        this.pendingInputLeft = null;
        this.pendingInputRight = null;
    }


    private void debugLog(String message) {
    }

    private ItemStack findBlockItem(EntityPlayerSP localPlayer) {
        InventoryPlayer inventory = localPlayer.V$src$Lgg_vape_wrapper_impl_InventoryPlayer_$erqak6();
        if (inventory.isNull() || localPlayer.isNull()) {
            return null;
        }
        if (this.heldWhitelist.getEffectiveValue().booleanValue()) {
            ItemStack heldStack = localPlayer.B$src$Lgg_vape_wrapper_impl_ItemStack_$impdvt();
            if (heldStack.isNull() || heldStack.getItem().isNull()) {
                return null;
            }
            if (!this.isWhitelistedBlock(heldStack)) {
                return null;
            }
            return heldStack;
        }
        for (int i = 0; i < 9; ++i) {
            ItemStack itemStack = inventory.c(i);
            if (itemStack.isNull() || !itemStack.getItem().isInstance(MappedClasses.Vw)) continue;
            if (this.isValidBlockItem(itemStack)) {
                return itemStack;
            }
            for (String preferredName : this.defaultBlockNames) {
                if (!itemStack.x().contains(preferredName)) continue;
                return itemStack;
            }
        }
        return null;
    }

    private BlockCoordinate findLandingBlockSimple(int maxTicks, EntityPlayerSP localPlayer) {
        return this.findLandingBlock(maxTicks, localPlayer, localPlayer, null);
    }

    @EventHandler
    public void onTick(EventPreTick eventPreTick) {
        GuiScreen currentScreen = eventPreTick.getCurrentScreen();
        GameSettings gameSettings = eventPreTick.getGameSettings();
        EntityPlayerSP localPlayer = eventPreTick.getThePlayer();
        WorldClient world = eventPreTick.getWorld();
        if (localPlayer.isNull() || world.isNull()) {
            this.resetState();
            return;
        }
        if (this.placementRejected) {
            this.showFailNotification("Server rejected block placement!", true);
            this.resetClutch(localPlayer);
            this.placementRejected = false;
        }
        this.tickSlotAndReset(localPlayer, currentScreen);
        double stationaryFallMotion = -0.0784000015258789;
        if (localPlayer.b$src$Z$fqlxe4() && localPlayer.q() == stationaryFallMotion) {
            this.fallTargetY = localPlayer.N();
        }
        if (Minecraft.currentScreen().getObject() == null) {
            KeyboardCodeUtil.disableLegacyRepeatEvents();
        }
        boolean forwardPressed = gg.vape.config.ClientSettings.isPhysicalKeyDown(Minecraft.gameSettings().Y());
        this.graph = new BlockPlacementGraph(localPlayer);
        if (this.pendingInputApply) {
            this.graph.forwardKeyDown = this.pendingInputForward;
            this.graph.backwardKeyDown = this.pendingInputBack;
            this.graph.leftKeyDown = this.pendingInputLeft;
            this.graph.rightKeyDown = this.pendingInputRight;
            this.clearPendingInputs();
            this.pendingInputApply = false;
            if (currentScreen.isNull()) {
                MovementInputHelper.restorePhysicalInput(false);
            }
        }
        this.graph.forwardKeyDown = forwardPressed;
        boolean flying = localPlayer.C$src$Lgg_vape_wrapper_impl_ModelPlayer_$19uhx86().isFlying();
        if (this.pauseRequested || this.movementLock.isLocked() || this.rotationClaim.isBlockedFor(this) || Minecraft.currentScreen().isNotNull()) {
            this.resetState();
            return;
        }
        ItemStack blockItem = this.findBlockItem(localPlayer);
        if (flying || localPlayer.S$src$Z$151gttj() || localPlayer.f$src$Z$fst3rk() || blockItem == null) {
            this.resetState();
            return;
        }
        if (!(this.onVoid.getEffectiveValue().booleanValue() || this.onLethalFall.getEffectiveValue().booleanValue() || this.onMoreThanXBlocks.getEffectiveValue().booleanValue())) {
            this.resetState();
            return;
        }
        boolean jumpPressed = gg.vape.config.ClientSettings.isPhysicalKeyDown(eventPreTick.getGameSettings().O());
        if (jumpPressed) {
            if (this.clutchPath != null) {
                if (localPlayer.b$src$Z$fqlxe4()) {
                    BlockPathPlanner landingSimulation = new BlockPathPlanner(localPlayer, localPlayer, world, this.graph);
                    boolean wouldLeaveGround = this.simulateLandsOnTarget(localPlayer, landingSimulation, this.clutchPath);
                    if (++this.groundStuckTicks >= 5 || !wouldLeaveGround) {
                        this.resetState();
                    }
                } else {
                    this.groundStuckTicks = 0;
                }
            }
            if (this.allowStaircaseUp.getEffectiveValue().booleanValue()) {
                if (!this.prevRightClickHeld && !this.staircaseTimer.hasTimeElapsed(500L)) {
                    this.staircaseQueued = true;
                }
                this.prevRightClickHeld = jumpPressed;
            } else {
                this.prevRightClickHeld = false;
                this.staircaseQueued = false;
            }
            if (this.clutchPath != null) {
                if (localPlayer.N() < (double)this.clutchPath.startCoordinate.E()) {
                    this.resetState();
                } else {
                    double currentDistance = RotationUtil.V(localPlayer.z(), localPlayer.h(), (double)this.clutchPath.targetCoordinate.B() + 0.5, (double)this.clutchPath.targetCoordinate.A() + 0.5);
                    double previousDistance = RotationUtil.V(localPlayer.f(), localPlayer.R(), (double)this.clutchPath.targetCoordinate.B() + 0.5, (double)this.clutchPath.targetCoordinate.A() + 0.5);
                    if (currentDistance > previousDistance && currentDistance > 1.2 && !localPlayer.b$src$Z$fqlxe4()) {
                        this.resetState();
                    }
                }
            }
            if (this.isPlayerMoving() && this.clutchPath == null) {
                boolean fallingOrRising = !localPlayer.b$src$Z$fqlxe4() || localPlayer.q() >= 0.0;
                if (fallingOrRising && !localPlayer.S$src$Z$151gttj() && !localPlayer.f$src$Z$fst3rk() && !localPlayer.h$src$Z$ftwoya() && this.failTimer.hasTimeElapsed(((Double)this.failDelay.getValue()).longValue())) {
                    BlockCoordinate landingBlock = this.findLandingBlockSimple(50, localPlayer);
                    boolean fallingIntoVoid = false;
                    boolean lethalFall = false;
                    boolean exceedsBlockThreshold = false;
                    if (landingBlock != null) {
                        if (this.onLethalFall.getEffectiveValue().booleanValue() && localPlayer.N() - (double)landingBlock.E() - 3.0 > (double)localPlayer.w$src$F$15l9epb()) {
                            lethalFall = true;
                        }
                        if (this.onMoreThanXBlocks.getEffectiveValue().booleanValue() && localPlayer.N() - (double)(landingBlock.E() + 1) >= (Double)this.blocksThreshold.getValue()) {
                            exceedsBlockThreshold = true;
                        }
                    } else {
                        fallingIntoVoid = this.onVoid.getEffectiveValue();
                    }
                    if (fallingIntoVoid || lethalFall || exceedsBlockThreshold) {
                        if (!this.rotationClaim.isOwnedBy(this) && !this.rotationClaim.acquire(this, this.silentAim.getEffectiveValue())) {
                            return;
                        }
                        long searchStartNanos = System.nanoTime();
                        BlockPlacementPathSegment clutchPath = this.computeClutchPath(world, localPlayer, blockItem);
                        long searchEndNanos = System.nanoTime();
                        if (clutchPath != null && !clutchPath.hasFailed()) {
                            this.debugLog("\n\n\nFound Clutch Path (" + (double)(searchEndNanos - searchStartNanos) / 1000000.0 + "ms)\n\n\n");
                            this.captureMovementInputs();
                            this.resetPendingFail();
                            this.groundStuckTicks = 0;
                            this.resetAngleDelayTicks = 0;
                            this.moveDelayTicks = 0;
                            this.returnDelayTicks = 0;
                            this.clutchPath = clutchPath;
                            this.prevRightClickHeld = jumpPressed;
                            gameSettings.F().e();
                            if (!this.silentAim.getEffectiveValue().booleanValue()) {
                                if (this.savedYaw == -999.0) {
                                    this.savedYaw = FreeLookHudModule.isActive() ? (double)FreeLookHudModule.getSavedPitch() : (double)localPlayer.J();
                                    this.savedPitch = localPlayer.V();
                                }
                            } else {
                                this.savedYaw = -999.0;
                            }
                        } else {
                            String failureMessage = null;
                            if (clutchPath == null) {
                                if (this.blockGraphMap.size() > 0) {
                                    failureMessage = "Could not find a clutch path!";
                                }
                            } else {
                                failureMessage = clutchPath.failureReason;
                                if (failureMessage == null) {
                                    failureMessage = "Could not find a clutch path!";
                                }
                            }
                            if (failureMessage != null && !failureMessage.isEmpty()) {
                                this.queueFailMessage(failureMessage);
                            }
                            this.resetMovementInputs();
                            this.clutchPath = null;
                            this.failTimer.reset();
                        }
                    }
                }
            }
            this.tickFailDelay();
            this.rayTrace = RotationManager.INSTANCE.getNormalReachRayTrace();
            if (this.clutchPath != null) {
                if (!this.claimActive) {
                    this.claimActive = true;
                    SharedModuleControlClaims.mouseButtons.lock();
                }
                this.placeTarget = null;
                if (this.clutchPath.placementState != null) {
                    this.placeTarget = this.findPlaceTarget(this.clutchPath, localPlayer, world);
                    if (this.placeTarget == null && this.clutchPath.placementState != null) {
                        this.clutchPath.fail("Failed to find a place target");
                    }
                } else if (!this.forcingCounterMotion || this.takingKnockback) {
                    this.resetState();
                    return;
                }
            } else {
                this.resetState();
                return;
            }
            if (this.placeTarget != null && this.selectBlockSlot(localPlayer)) {
                float currentYaw;
                this.pathPlannerReturn = new BlockPathPlanner(localPlayer, localPlayer, world, this.graph);
                if (RotationManager.INSTANCE.getActiveController() == null) {
                    this.rotationController = null;
                }
                if (this.rotationController == null) {
                    if (RotationManager.INSTANCE.hasAdaptiveController()) {
                        AdaptiveRotationController adaptiveRotationController = (AdaptiveRotationController)RotationManager.INSTANCE.getActiveController();
                        currentYaw = adaptiveRotationController.getRenderedYaw();
                    } else {
                        currentYaw = localPlayer.J();
                    }
                } else if (this.rotationController instanceof AdaptiveRotationController) {
                    currentYaw = ((AdaptiveRotationController)this.rotationController).getRenderedYaw();
                } else {
                    currentYaw = this.rotationController.getCurrentYaw();
                }
                if (this.isLookingAtTarget()) {
                    KeyBinding useKey = Minecraft.gameSettings().F();
                    if (useKey.u() || useKey.isPressed()) {
                        KeyBinding.setKeyBindState(useKey, false);
                    }
                    KeyBinding placementKey = Minecraft.gameSettings().b$src$Lgg_vape_wrapper_impl_KeyBinding_$1yi3362();
                    KeyBinding.setKeyBindState(placementKey, true);
                    KeyBinding.onTick(placementKey);
                    KeyBinding.setKeyBindState(placementKey, false);
                    if (this.clutchPath.placementState != null) {
                        this.placeTarget = null;
                        Vector<PlacementTarget> pendingTargets = this.clutchPath.placementState.pendingTargets;
                        if (!pendingTargets.isEmpty()) {
                            pendingTargets.removeElementAt(0);
                        }
                        while (!pendingTargets.isEmpty()) {
                            PlacementTarget placementTarget = pendingTargets.firstElement();
                            if (placementTarget == null) continue;
                            BlockData blockData = placementTarget.getPlacedBlock();
                            Block block = world.getBlockByPos(blockData.D(), blockData.B(), blockData.G());
                            if (BlockUtil.u(block)) {
                                if (ClutchPlacementPathUtils.isPlacementSpaceClear(world, localPlayer, blockData)) {
                                    this.placeTarget = placementTarget;
                                    break;
                                }
                                this.clutchPath.fail("Entity blocking placement");
                                break;
                            }
                            this.clutchPath.hitVector = null;
                            pendingTargets.removeElementAt(0);
                        }
                        if (this.placeTarget == null && this.forcingCounterMotion && this.rotationController != null) {
                            float targetYaw = this.takingKnockback ? this.originalYaw : this.placeYaw;
                            float yawDistance = Math.abs(MathUtil.wrapAngleTo180(targetYaw - currentYaw));
                            this.rotationController.setSpeed(yawDistance / 1.8f / 3.0f);
                            this.rotationController.setTargetRotation(targetYaw, this.rotationController.getTargetPitch());
                        }
                    }
                }
            }
            return;
        }
        if (this.clutchPath != null) {
            if (localPlayer.b$src$Z$fqlxe4()) {
                BlockPathPlanner landingSimulation = new BlockPathPlanner(localPlayer, localPlayer, world, this.graph);
                boolean wouldLeaveGround = this.simulateLandsOnTarget(localPlayer, landingSimulation, this.clutchPath);
                if (++this.groundStuckTicks >= 5 || !wouldLeaveGround) {
                    this.resetState();
                }
            } else {
                this.groundStuckTicks = 0;
            }
        }
        if (this.allowStaircaseUp.getEffectiveValue().booleanValue()) {
            if (this.prevRightClickHeld != jumpPressed) {
                if (this.staircaseQueued) {
                    this.staircaseQueued = false;
                } else {
                    this.staircaseTimer.reset();
                }
            }
            this.prevRightClickHeld = jumpPressed;
        } else {
            this.prevRightClickHeld = false;
            this.staircaseQueued = false;
        }
        if (this.clutchPath != null) {
            if (localPlayer.N() < (double)this.clutchPath.startCoordinate.E()) {
                this.resetState();
            } else {
                double currentDistance = RotationUtil.V(localPlayer.z(), localPlayer.h(), (double)this.clutchPath.targetCoordinate.B() + 0.5, (double)this.clutchPath.targetCoordinate.A() + 0.5);
                double previousDistance = RotationUtil.V(localPlayer.f(), localPlayer.R(), (double)this.clutchPath.targetCoordinate.B() + 0.5, (double)this.clutchPath.targetCoordinate.A() + 0.5);
                if (currentDistance > previousDistance && currentDistance > 1.2 && !localPlayer.b$src$Z$fqlxe4()) {
                    this.resetState();
                }
            }
        }
        if (this.isPlayerMoving() && this.clutchPath == null) {
            boolean fallingOrRising = !localPlayer.b$src$Z$fqlxe4() || localPlayer.q() >= 0.0;
            if (fallingOrRising && !localPlayer.S$src$Z$151gttj() && !localPlayer.f$src$Z$fst3rk() && !localPlayer.h$src$Z$ftwoya() && this.failTimer.hasTimeElapsed(((Double)this.failDelay.getValue()).longValue())) {
                BlockCoordinate landingBlock = this.findLandingBlockSimple(50, localPlayer);
                boolean fallingIntoVoid = false;
                boolean lethalFall = false;
                boolean exceedsBlockThreshold = false;
                if (landingBlock != null) {
                    if (this.onLethalFall.getEffectiveValue().booleanValue() && localPlayer.N() - (double)landingBlock.E() - 3.0 > (double)localPlayer.w$src$F$15l9epb()) {
                        lethalFall = true;
                    }
                    if (this.onMoreThanXBlocks.getEffectiveValue().booleanValue() && localPlayer.N() - (double)(landingBlock.E() + 1) >= (Double)this.blocksThreshold.getValue()) {
                        exceedsBlockThreshold = true;
                    }
                } else {
                    fallingIntoVoid = this.onVoid.getEffectiveValue();
                }
                if (fallingIntoVoid || lethalFall || exceedsBlockThreshold) {
                    if (!this.rotationClaim.isOwnedBy(this) && !this.rotationClaim.acquire(this, this.silentAim.getEffectiveValue())) {
                        return;
                    }
                    long searchStartNanos = System.nanoTime();
                    BlockPlacementPathSegment clutchPath = this.computeClutchPath(world, localPlayer, blockItem);
                    long searchEndNanos = System.nanoTime();
                    if (clutchPath != null && !clutchPath.hasFailed()) {
                        this.debugLog("\n\n\nFound Clutch Path (" + (double)(searchEndNanos - searchStartNanos) / 1000000.0 + "ms)\n\n\n");
                        this.captureMovementInputs();
                        this.resetPendingFail();
                        this.groundStuckTicks = 0;
                        this.resetAngleDelayTicks = 0;
                        this.moveDelayTicks = 0;
                        this.returnDelayTicks = 0;
                        this.clutchPath = clutchPath;
                        this.prevRightClickHeld = jumpPressed;
                        gameSettings.F().e();
                        if (!this.silentAim.getEffectiveValue().booleanValue()) {
                            if (this.savedYaw == -999.0) {
                                this.savedYaw = FreeLookHudModule.isActive() ? (double)FreeLookHudModule.getSavedPitch() : (double)localPlayer.J();
                                this.savedPitch = localPlayer.V();
                            }
                        } else {
                            this.savedYaw = -999.0;
                        }
                    } else {
                        String failureMessage = null;
                        if (clutchPath == null) {
                            if (this.blockGraphMap.size() > 0) {
                                failureMessage = "Could not find a clutch path!";
                            }
                        } else {
                            failureMessage = clutchPath.failureReason;
                            if (failureMessage == null) {
                                failureMessage = "Could not find a clutch path!";
                            }
                        }
                        if (failureMessage != null && !failureMessage.isEmpty()) {
                            this.queueFailMessage(failureMessage);
                        }
                        this.resetMovementInputs();
                        this.clutchPath = null;
                        this.failTimer.reset();
                    }
                }
            }
        }
        this.tickFailDelay();
        this.rayTrace = RotationManager.INSTANCE.getNormalReachRayTrace();
        if (this.clutchPath != null) {
            if (!this.claimActive) {
                this.claimActive = true;
                SharedModuleControlClaims.mouseButtons.lock();
            }
            this.placeTarget = null;
            if (this.clutchPath.placementState != null) {
                this.placeTarget = this.findPlaceTarget(this.clutchPath, localPlayer, world);
                if (this.placeTarget == null && this.clutchPath.placementState != null) {
                    this.clutchPath.fail("Failed to find a place target");
                }
            } else if (!this.forcingCounterMotion || this.takingKnockback) {
                this.resetState();
                return;
            }
        } else {
            this.resetState();
            return;
        }
        if (this.placeTarget != null && this.selectBlockSlot(localPlayer)) {
            float currentYaw;
            this.pathPlannerReturn = new BlockPathPlanner(localPlayer, localPlayer, world, this.graph);
            if (RotationManager.INSTANCE.getActiveController() == null) {
                this.rotationController = null;
            }
            if (this.rotationController == null) {
                if (RotationManager.INSTANCE.hasAdaptiveController()) {
                    AdaptiveRotationController adaptiveRotationController = (AdaptiveRotationController)RotationManager.INSTANCE.getActiveController();
                    currentYaw = adaptiveRotationController.getRenderedYaw();
                } else {
                    currentYaw = localPlayer.J();
                }
            } else if (this.rotationController instanceof AdaptiveRotationController) {
                currentYaw = ((AdaptiveRotationController)this.rotationController).getRenderedYaw();
            } else {
                currentYaw = this.rotationController.getCurrentYaw();
            }
            if (this.isLookingAtTarget()) {
                KeyBinding useKey = Minecraft.gameSettings().F();
                if (useKey.u() || useKey.isPressed()) {
                    KeyBinding.setKeyBindState(useKey, false);
                }
                KeyBinding placementKey = Minecraft.gameSettings().b$src$Lgg_vape_wrapper_impl_KeyBinding_$1yi3362();
                KeyBinding.setKeyBindState(placementKey, true);
                KeyBinding.onTick(placementKey);
                KeyBinding.setKeyBindState(placementKey, false);
                if (this.clutchPath.placementState != null) {
                    this.placeTarget = null;
                    Vector<PlacementTarget> pendingTargets = this.clutchPath.placementState.pendingTargets;
                    if (!pendingTargets.isEmpty()) {
                        pendingTargets.removeElementAt(0);
                    }
                    while (!pendingTargets.isEmpty()) {
                        PlacementTarget placementTarget = pendingTargets.firstElement();
                        if (placementTarget == null) continue;
                        BlockData blockData = placementTarget.getPlacedBlock();
                        Block block = world.getBlockByPos(blockData.D(), blockData.B(), blockData.G());
                        if (BlockUtil.u(block)) {
                            if (ClutchPlacementPathUtils.isPlacementSpaceClear(world, localPlayer, blockData)) {
                                this.placeTarget = placementTarget;
                                break;
                            }
                            this.clutchPath.fail("Entity blocking placement");
                            break;
                        }
                        this.clutchPath.hitVector = null;
                        pendingTargets.removeElementAt(0);
                    }
                    if (this.placeTarget == null && this.forcingCounterMotion && this.rotationController != null) {
                        float targetYaw = this.takingKnockback ? this.originalYaw : this.placeYaw;
                        float yawDistance = Math.abs(MathUtil.wrapAngleTo180(targetYaw - currentYaw));
                        this.rotationController.setSpeed(yawDistance / 1.8f / 3.0f);
                        this.rotationController.setTargetRotation(targetYaw, this.rotationController.getTargetPitch());
                    }
                }
            }
        }
    }

    @Override
    public void setEnabled(boolean enabled, boolean bypassVisibilityCheck) {
        if (!enabled && this.rotationController instanceof AdaptiveRotationController) {
            this.pauseRequested = !this.pauseRequested;
        } else {
            super.setEnabled(enabled, bypassVisibilityCheck);
            this.pauseRequested = false;
            if (!enabled) {
                this.resetRotation(Minecraft.thePlayer());
                this.counterMotion = false;
                this.forcingCounterMotion = false;
                this.resetAngleDelayTicks = 0;
                this.moveDelayTicks = 0;
                this.returnDelayTicks = 0;
                this.pendingSegments.clear();
                DEBUG_PLACEMENT_PATHS.clear();
                this.resetState();
            } else {
                this.pendingSegments.clear();
                DEBUG_PLACEMENT_PATHS.clear();
            }
        }
    }


    private FixedRotationController buildRotation(EntityPlayer player, Vec3 eyePosition, Vec3 targetPosition, FixedRotationController existingController, int ticksAvailable, float referenceYaw) {
        float sourceYaw;
        float sourcePitch;
        FixedRotationController controller = existingController;
        if (existingController == null && !player.isInstance(MappedClasses.z5) && RotationManager.INSTANCE.hasAdaptiveController()) {
            AdaptiveRotationController activeController = (AdaptiveRotationController)RotationManager.INSTANCE.getActiveController();
            sourceYaw = activeController.getRenderedYaw();
            sourcePitch = activeController.getRenderedPitch();
        } else if (existingController == null) {
            sourceYaw = player.J();
            sourcePitch = player.V();
        } else {
            sourceYaw = existingController.getCurrentYaw();
            sourcePitch = existingController.getCurrentPitch();
        }
        RotationAngles targetRotation = RotationVectorMath.d(eyePosition, targetPosition, sourceYaw, sourcePitch);
        if (existingController == null) {
            controller = this.silentAim.getEffectiveValue() != false ? new AdaptiveRotationController(player) : new EntityFixedRotationController(targetRotation, player);
        }
        controller.setTargetRotation(targetRotation);
        if (controller instanceof AdaptiveRotationController) {
            ((AdaptiveRotationController)controller).setReferenceYawOverride(Float.valueOf(referenceYaw));
            ((AdaptiveRotationController)controller).setRelativeMode(false);
        }
        float currentYaw;
        float currentPitch;
        if (controller instanceof AdaptiveRotationController) {
            currentYaw = ((AdaptiveRotationController)controller).getRenderedYaw();
            currentPitch = ((AdaptiveRotationController)controller).getRenderedPitch();
        } else {
            currentYaw = player.J();
            currentPitch = player.V();
        }
        float yawDistance = Math.abs(MathUtil.wrapAngleTo180(targetRotation.getYaw() - currentYaw));
        float pitchDistance = Math.abs(targetRotation.getPitch() - currentPitch);
        float rotationSpeed = (yawDistance + pitchDistance) / 1.8f / (float)Math.max(ticksAvailable, 1);
        float maximumSpeed = 15.0f + 85.0f * (((Double)this.speed.getValue()).floatValue() / 10.0f);
        rotationSpeed = Math.min(maximumSpeed + (float)((int)(player.N() * 100.0) % 5), rotationSpeed);
        controller.setRestoreCapturedRotation(true);
        controller.setSpeed(rotationSpeed);
        controller.setTolerance(0.0f);
        controller.setClampStepToRemaining(true);
        controller.setCubicAcceleration(false);
        controller.setLinearAcceleration(false);
        controller.setScaleAxesProportionally(true);
        controller.setRandomizeMovement(false);
        controller.setRetainAfterCompletion(true);
        return controller;
    }

    private void tickSlotAndReset(EntityPlayerSP localPlayer, GuiScreen currentScreen) {
        if (this.clutchPath == null) {
            if (this.moveDelayTicks-- >= 0) {
                if (this.moveDelayTicks <= 0) {
                    if (currentScreen.isNull()) {
                        MovementInputHelper.restorePhysicalInput(false);
                    }
                    this.moveDelayTicks = -1;
                } else {
                    MovementInputHelper.releaseInput(false);
                }
            }
            if (this.returnToLastSlot.getEffectiveValue().booleanValue() && this.previousSlot != -1 && this.returnDelayTicks-- <= 0) {
                this.selectHotbarSlot(this.previousSlot);
                this.previousSlot = -1;
            }
            if (this.resetAngleDelayTicks >= 0) {
                if (this.resetAngleDelayTicks-- <= 0) {
                    this.resetRotation(localPlayer);
                } else if (this.rotationController != null && !this.rotationController.isComplete()) {
                    this.rotationController.setSpeed(((Double)this.speed.getValue()).floatValue() / (float)this.resetAngleDelayTicks);
                    this.rotationController.setRandomizeMovement(true);
                }
            }
        }
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onPreLocalPlayerTick(EventPreLocalPlayerTick eventPreLocalPlayerTick) {
        EntityPlayerSP localPlayer = eventPreLocalPlayerTick.getThePlayer();
        if (this.clutchPath != null && Minecraft.currentScreen().isNull() && localPlayer.isNotNull()) {
            BlockInBooleanState strafeState = null;
            if (this.rotationController != null && !(this.rotationController instanceof AdaptiveRotationController)) {
                strafeState = this.computeStrafeState(localPlayer, this.placeYaw, this.inputForward, this.inputBack, this.inputLeft, this.inputRight);
            }
            if (strafeState != null) {
                this.pendingInputApply = true;
                this.pendingInputForward = strafeState.correctedForward;
                this.pendingInputRight = strafeState.correctedRight;
                this.pendingInputLeft = strafeState.correctedLeft;
                this.pendingInputBack = strafeState.correctedBackward;
            }
            this.applyMovementInputs();
        }
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onPacketReceive(EventPacketReceive eventPacketReceive) {
        EntityPlayerSP localPlayer = eventPacketReceive.getThePlayer();
        if (localPlayer.isNull()) {
            return;
        }
        if (!eventPacketReceive.isCanceled()) {
            Packet packet = eventPacketReceive.getPacket();
            if (packet.isInstance(MappedClasses.YX) || packet.isInstance(MappedClasses.qe)) {
                boolean appliesToPlayer;
                if (packet.isInstance(MappedClasses.YX)) {
                    SPacketEntityVelocity velocityPacket = new SPacketEntityVelocity(packet.getObject());
                    appliesToPlayer = velocityPacket.getEntityId() == localPlayer.S();
                    if (appliesToPlayer) {
                        double packetMotionX = (double)velocityPacket.getMotionX() / 8000.0;
                        double packetMotionZ = (double)velocityPacket.getMotionZ() / 8000.0;
                        double packetHorizontalSpeed = Math.sqrt(packetMotionX * packetMotionX + packetMotionZ * packetMotionZ);
                        double playerHorizontalSpeed = Math.sqrt(localPlayer.t() * localPlayer.t() + localPlayer.T() * localPlayer.T());
                        double combinedSpeed = packetHorizontalSpeed + playerHorizontalSpeed;
                        this.debugLog("Total velocity: " + packetHorizontalSpeed + " Player velocity: " + playerHorizontalSpeed + " New velocity: " + combinedSpeed);
                        this.takingKnockback = true;
                        this.knockbackTicks = 4;
                    }
                } else {
                    PacketVelocityBridge velocityPacket = new PacketVelocityBridge(packet.getObject());
                    double motionX = velocityPacket.getMotionX();
                    double motionY = velocityPacket.getMotionY();
                    double motionZ = velocityPacket.getMotionZ();
                    appliesToPlayer = Math.abs(motionX) >= 0.005 || Math.abs(motionY) >= 0.005 || Math.abs(motionZ) >= 0.005;
                }
                if (appliesToPlayer) {
                    this.resetClutch(localPlayer);
                }
            } else if (packet.isInstance(MappedClasses.zw)) {
                if (this.clutchPath != null && this.clutchPath.placementState != null) {
                    this.showFailNotification("Server teleported you!", true);
                    this.resetClutch(localPlayer);
                }
            } else if (packet.isInstance(MappedClasses.DD) && this.clutchPath != null && this.clutchPath.placementState != null) {
                SPacketBlockChange blockChangePacket = new SPacketBlockChange(packet.getObject());
                BlockState blockState = blockChangePacket.getBlockState();
                BlockPos blockPos = blockChangePacket.getBlockPosition();
                if (BlockUtil.p(blockState.getBlock()) && this.clutchPath.placementState.containsPosition(blockPos.getX(), blockPos.getY(), blockPos.getZ())) {
                    this.placementRejected = true;
                    this.placedBlocks.N(new BlockData(blockPos.getX(), blockPos.getY(), blockPos.getZ()));
                }
            }
        }
    }
}
