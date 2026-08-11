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
import gg.vape.mapping.MappedClasses;
import gg.vape.module.Category;
import gg.vape.module.Mod;
import gg.vape.module.ModuleDisplayInfo;
import gg.vape.module.blatant.autoladder.AutoLadderFallAdjustment;
import gg.vape.module.blatant.autoladder.AutoLadderMovementController;
import gg.vape.module.blatant.autoladder.AutoLadderPlan;
import gg.vape.module.blatant.autoladder.AutoLadderPlanner;
import gg.vape.module.blatant.autoladder.AutoLadderResetRotationController;
import gg.vape.module.blatant.autoladder.AutoLadderState;
import gg.vape.module.blatant.blockin.BlockPlacementGraph;
import gg.vape.module.blatant.blockin.BlockPlacementUtility;
import gg.vape.module.blatant.clutch.EntityFixedRotationController;
import gg.vape.module.control.FallRescuePriorityManager;
import gg.vape.module.control.SharedModuleControlClaims;
import gg.vape.module.none.ClientSettings;
import gg.vape.module.utility.RescueModuleUtil;
import gg.vape.module.utility.clutch.ClutchPlacementPathUtils;
import gg.vape.module.utility.clutch.PlacementTarget;
import gg.vape.movement.MovementInputHelper;
import gg.vape.notification.Notification;
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
import gg.vape.utils.RotationVectorMath;
import gg.vape.utils.TimerUtil;
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
import gg.vape.wrapper.impl.Blocks;
import gg.vape.wrapper.impl.EntityPlayer;
import gg.vape.wrapper.impl.EntityPlayerSP;
import gg.vape.wrapper.impl.EnumFacing;
import gg.vape.wrapper.impl.ForgeVersion;
import gg.vape.wrapper.impl.GameSettings;
import gg.vape.wrapper.impl.GuiScreen;
import gg.vape.wrapper.impl.InventoryPlayer;
import gg.vape.wrapper.impl.Item;
import gg.vape.wrapper.impl.ItemBlock;
import gg.vape.wrapper.impl.ItemStack;
import gg.vape.wrapper.impl.KeyBinding;
import gg.vape.wrapper.impl.Minecraft;
import gg.vape.wrapper.impl.ModelPlayer;
import gg.vape.wrapper.impl.Packet;
import gg.vape.wrapper.impl.RayTraceResult;
import gg.vape.wrapper.impl.SPacketBlockChange;
import gg.vape.wrapper.impl.SPacketEntityVelocity;
import gg.vape.wrapper.impl.Vec3;
import gg.vape.wrapper.impl.World;
import gg.vape.wrapper.impl.WorldClient;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class AutoLadder extends Mod {
    private static final float UNSET_ANGLE = -999.0f;
    private static final int PLACEMENT_RETRY_INTERVAL = 2;
    private static final int SAFE_HOLD_TICKS = 2;
    private static final float MAXIMUM_ROTATION_SPEED = 100.0f;
    private static final double DEFAULT_RESET_ANGLE_DELAY_MIN_TICKS = 3.0;
    private static final double DEFAULT_RESET_ANGLE_DELAY_MAX_TICKS = 6.0;
    private static final int LANDING_PREDICTION_INTERVAL_TICKS = 4;
    private static final double LANDING_PREDICTION_DRIFT_THRESHOLD = 0.35;
    private static final int PLAN_SEARCH_INTERVAL_TICKS = 2;

    private final BooleanValue onLethalFall;
    private final BooleanValue onMoreThanXBlocks;
    private final NumberValue blocksThreshold;
    private final BooleanValue silentAim;
    private final NumberValue failDelay;
    private final BooleanValue returnToLastSlot;
    private final RandomValue returnDelay;
    private final BooleanValue resetAngle;
    private final BooleanValue showLadderCount;
    private final BooleanValue blacklist;
    private final LimitValue blacklistBlocks;
    private final BooleanValue heldWhitelist;
    private final LimitValue whitelistBlocks;
    private final RotationControlClaim rotationClaim;
    private final TimerUtil failTimer;
    private final Set<String> rejectedPlans;
    private final List<String> preferredBlockNames;
    private Notification failNotification;

    private AutoLadderState state = AutoLadderState.IDLE;
    private AutoLadderPlan plan;
    private FixedRotationController rotationController;
    private BlockCoordinate landingBlock;
    private AutoLadderFallAdjustment pendingFallAdjustment =
            AutoLadderFallAdjustment.PHYSICAL;
    private int stateTicks;
    private int ladderSlot = -1;
    private int supportSlot = -1;
    private int previousSlot = -1;
    private int returnDelayTicks = -1;
    private int resetAngleDelayTicks = -1;
    private int placementAttempts;
    private int lastPlacementAttemptTick = -1000;
    private int caughtTicks;
    private float fallDistanceBeforeTick;
    private float savedYaw = UNSET_ANGLE;
    private float savedPitch = UNSET_ANGLE;
    private float referenceYaw;
    private float activationHealth;
    private boolean placementRejected;
    private boolean trajectoryInvalidated;
    private boolean movementControlled;
    private boolean movementAvailable;
    private boolean buttonsLocked;
    private boolean rightClickBlocked;
    private boolean placementKeyHeld;
    private boolean fallEpisodeResolved;
    private boolean supportPlacementRequested;
    private boolean supportConfirmed;
    private boolean fallAdjustmentPending;
    private boolean lastActivateResult;
    private int lastLandingPredictionTick = -100;
    private int lastSearchTick = -100;
    private double lastPredictionX;
    private double lastPredictionZ;
    private boolean warmupPending;
    private int warmupStage;
    private BlockCoordinate warmupLanding;

    public AutoLadder() {
        super("AutoLadder", 7043655, Category.UTILITY,
                "Places and catches a ladder to prevent fall damage");
        this.onLethalFall = BooleanValue.create(this, "On lethal fall", true,
                "Activate when the predicted fall damage would be lethal");
        this.onMoreThanXBlocks = BooleanValue.create(this, "On more than x blocks", false,
                "Activate when the predicted total fall exceeds the configured height");
        this.blocksThreshold = NumberValue.create(
                this, "Blocks", "#", "", 3.0, 6.0, 10.0, 1.0);
        this.silentAim = BooleanValue.create(this, "Silent aim", false,
                "Uses server-side aim while placing");
        this.failDelay = NumberValue.create(this, "Fail delay", "#", "ms",
                0.0, 100.0, 500.0, 50.0,
                "Delay before searching again after a failed rescue");
        this.returnToLastSlot = BooleanValue.create(this, "Return to last slot", true,
                "Returns to the previously selected slot after the rescue");
        this.returnDelay = RandomValue.createWithDescription(this, "Return delay", "#", "tick",
                0.0, 3.0, 6.0, 10.0, 1.0,
                "Delay before returning to the previous slot");
        this.resetAngle = BooleanValue.create(this, "Reset angle", true,
                "Returns to the original view angle after non-silent placement");
        this.showLadderCount = BooleanValue.create(this, "Show ladder count", false,
                "Renders your ladder count on the center of your screen");
        this.blacklist = BooleanValue.create(this, "Blacklist", true,
                "Do not use blacklisted blocks as the ladder support");
        List<ItemLimitData> defaultBlacklist = new ArrayList<>(
                ItemLimitData.DEFAULT_BLOCK_BLACKLIST);
        defaultBlacklist.add(new ItemLimitData("Obsidian"));
        this.blacklistBlocks = LimitValue.create(this, "autoladder-blacklist", "Block blacklist",
                LimitValue.BLOCK_LIST_COLOR, defaultBlacklist);
        this.heldWhitelist = BooleanValue.create(this, "Held whitelist", false,
                "Only use the currently held whitelisted block as support");
        this.whitelistBlocks = LimitValue.create(this, "autoladder-allowedblocks",
                "Held block whitelist", LimitValue.ALLOW_LIST_COLOR,
                Arrays.asList(new ItemLimitData("blocks"), new ItemLimitData("Ladder")));
        this.rotationClaim = SharedModuleControlClaims.rotation;
        this.failTimer = new TimerUtil();
        this.rejectedPlans = new HashSet<>();
        this.preferredBlockNames = RescueModuleUtil.PREFERRED_BLOCK_NAMES;

        this.onMoreThanXBlocks.addDependentValues(this.blocksThreshold);
        this.returnToLastSlot.addDependentValues(this.returnDelay);
        this.blacklist.addDependentValues(this.blacklistBlocks);
        this.heldWhitelist.addDependentValues(this.whitelistBlocks);
        this.silentAim.getDisabledCondition().applyTo(this.resetAngle);
        this.resetAngle.setOverrideColor(ThemeColors.J.r);
        this.addValue(this.onLethalFall, this.onMoreThanXBlocks, this.blocksThreshold,
                this.silentAim, this.resetAngle, this.returnToLastSlot, this.returnDelay,
                this.failDelay, this.showLadderCount, this.blacklist, this.blacklistBlocks,
                this.heldWhitelist, this.whitelistBlocks);
        this.rotationClaim.setPriority(this, 50);
    }

    @Override
    public void onEnable() {
        this.resetImmediately();
        this.warmupPending = true;
        this.warmupStage = 0;
        this.warmupLanding = null;
        ClientSettings.getFrame(ActiveModuleStackFrame.class).addModule(this);
    }

    @Override
    public void onDisable() {
        ClientSettings.getFrame(ActiveModuleStackFrame.class).removeModule(this);
        this.resetImmediately();
    }

    @Override
    public ModuleDisplayInfo getModuleDisplayInfo() {
        if (!this.showLadderCount.getEffectiveValue().booleanValue()
                || Minecraft.thePlayer().isNull()) {
            return null;
        }
        int count = this.countLadders();
        Color color = RescueModuleUtil.countColor(count);
        return new ModuleDisplayInfo(String.valueOf(count), color);
    }

    @EventHandler(priority = EventPriority.LOWEREST)
    public void onTick(EventPreTick event) {
        EntityPlayerSP player = event.getThePlayer();
        WorldClient world = event.getWorld();
        if (player.isNull() || world.isNull()) {
            this.resetImmediately();
            return;
        }
        if (this.warmupPending) {
            if (this.state == AutoLadderState.IDLE && this.plan == null
                    && player.b$src$Z$fqlxe4() && event.getCurrentScreen().isNull()) {
                this.runWarmupTick(player, world);
            } else if (this.state != AutoLadderState.IDLE || !player.b$src$Z$fqlxe4()) {
                this.warmupPending = false;
            }
        }
        this.tickPostRun(player, event.getCurrentScreen());
        this.fallDistanceBeforeTick = player.getFallDistance();
        if (this.state != AutoLadderState.IDLE) {
            ++this.stateTicks;
        }
        if (this.placementRejected) {
            this.placementRejected = false;
            if (this.isExecutingPlan()) {
                this.showFailNotification("Server rejected block placement!", true);
                this.invalidatePlan(true);
            }
        }
        if (this.trajectoryInvalidated) {
            this.trajectoryInvalidated = false;
            if (this.isExecutingPlan()) {
                this.invalidatePlan(false);
            }
        }

        if (player.b$src$Z$fqlxe4()) {
            this.handleGroundedPlayer(player);
            return;
        }
        if (player.boolean_S() && !this.isExecutingPlan()) {
            this.transition(AutoLadderState.IDLE);
            return;
        }
        if (this.fallEpisodeResolved) {
            if (player.q() >= -0.05 || player.getFallDistance() <= 0.5f) {
                return;
            }
            this.fallEpisodeResolved = false;
            this.transition(AutoLadderState.FALLING);
        }
        if (!this.canRun(player, event.getCurrentScreen())) {
            if (this.isExecutingPlan()) {
                this.enterFail(true);
            } else {
                this.transition(AutoLadderState.IDLE);
            }
            return;
        }
        if (FallRescuePriorityManager.INSTANCE.shouldStandDown(this, player)) {
            if (this.isExecutingPlan() || this.state == AutoLadderState.SEARCHING_BLOCK) {
                this.enterFail(true);
            } else {
                this.transition(AutoLadderState.IDLE);
            }
            return;
        }
        if (SharedModuleControlClaims.movementInput.isLocked()
                && this.shouldControlMovement()) {
            this.enterFail(true);
            return;
        }
        if (this.requiresPlacementRotation() && !this.rotationClaim.isOwnedBy(this)) {
            this.enterFail(true);
            return;
        }

        switch (this.state) {
            case IDLE:
                if (player.q() < -0.05
                        && this.failTimer.hasTimeElapsed(((Double)this.failDelay.getValue()).longValue())) {
                    this.transition(AutoLadderState.FALLING);
                }
                break;
            case FALLING:
                if (player.q() >= 0.0) {
                    this.transition(AutoLadderState.IDLE);
                } else if (this.shouldActivate(player)) {
                    this.activationHealth = this.effectiveHealth(player);
                    this.transition(AutoLadderState.SEARCHING_BLOCK);
                }
                break;
            case SEARCHING_BLOCK:
                if (this.stateTicks - this.lastSearchTick >= PLAN_SEARCH_INTERVAL_TICKS) {
                    this.searchForPlan(player, world);
                }
                break;
            case PLACING_BLOCK:
                this.handleBlockPlacement(player, world);
                break;
            case PLACING_LADDER:
                this.handleLadderPlacement(player, world);
                break;
            case CENTERING:
                this.handleCentering(player, world);
                break;
            case SAFE:
                if (this.stateTicks >= SAFE_HOLD_TICKS) {
                    this.completeSafeRun();
                }
                break;
            case FAIL:
                if (this.stateTicks >= 1) {
                    this.transition(AutoLadderState.IDLE);
                }
                break;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlacementRayReady(EventPreTick event) {
        if (this.plan == null || event.getThePlayer().isNull() || event.getWorld().isNull()) {
            return;
        }
        PlacementTarget target;
        boolean hold;
        if (this.state == AutoLadderState.PLACING_BLOCK && this.plan.getBlockTarget() != null) {
            target = this.plan.getBlockTarget();
            hold = false;
        } else if (this.state == AutoLadderState.PLACING_LADDER) {
            if (!this.isStableSupport(event.getWorld(), this.plan.getSupportBlock())) {
                return;
            }
            target = this.plan.getLadderTarget();
            hold = true;
        } else {
            return;
        }
        boolean looking = this.isLookingAt(target);
        boolean canAttempt = this.canAttemptPlacement();
        if (looking && canAttempt) {
            this.requestPlacement(hold);
            if (!hold) {
                this.supportPlacementRequested = true;
                this.supportConfirmed = false;
            }
        } else if (hold && !looking) {
            this.releaseHeldPlacementKey();
        }
    }

    @EventHandler
    public void onPostTick(EventPostTick event) {
        EntityPlayerSP player = event.getThePlayer();
        WorldClient world = event.getWorld();
        if (player.isNull() || world.isNull()) {
            return;
        }
        if (this.state == AutoLadderState.PLACING_BLOCK && this.supportPlacementRequested) {
            this.confirmSupport(player, world);
        }
        if (this.state == AutoLadderState.PLACING_LADDER) {
            this.confirmLadder(world);
        }
        if (this.state != AutoLadderState.CENTERING || this.plan == null
                || !this.isLadder(world, this.plan.getLadderBlock())) {
            return;
        }
        boolean fallDistanceReset = player.getFallDistance() <= 0.5f
                || player.getFallDistance() + 0.05f < this.fallDistanceBeforeTick;
        if (player.boolean_S() && fallDistanceReset) {
            ++this.caughtTicks;
            if (this.caughtTicks >= 1) {
                this.releasePlacementButtons();
                this.restoreMovementInput();
                this.transition(AutoLadderState.SAFE);
            }
        } else {
            this.caughtTicks = 0;
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreLocalPlayerTick(EventPreLocalPlayerTick event) {
        EntityPlayerSP player = event.getThePlayer();
        if (this.state == AutoLadderState.PLACING_LADDER && this.plan != null
                && !player.isNull() && this.isLadder(player.getWorld(), this.plan.getLadderBlock())) {
            this.confirmLadder(player.getWorld());
        }
        if (Minecraft.currentScreen().isNotNull() || player.isNull()) {
            this.fallAdjustmentPending = false;
            this.restoreMovementInput();
            return;
        }
        if (this.fallAdjustmentPending && this.state != AutoLadderState.CENTERING) {
            AutoLadderFallAdjustment adjustment = this.pendingFallAdjustment;
            this.fallAdjustmentPending = false;
            AutoLadderMovementController.apply(adjustment);
            this.movementControlled = adjustment.overridesInput();
            return;
        }
        if (this.state == AutoLadderState.CENTERING) {
            this.fallAdjustmentPending = false;
        }
        if (this.plan == null || !this.shouldControlMovement()) {
            this.restoreMovementInput();
            return;
        }
        AutoLadderMovementController.CenterInput input =
                AutoLadderMovementController.chooseCentering(
                        player, player.getWorld(),
                        this.plan.getCatchX(), this.plan.getCatchZ(),
                        this.plan.getLadderBlock(), this.plan.getLadderFacing());
        AutoLadderMovementController.apply(input);
        this.movementControlled = true;
    }

    @EventHandler
    public void onClickMouse(EventClickMouse event) {
        if (this.isExecutingPlan()) {
            event.setCancelled(true);
            event.getGameSettings().F().e();
        }
    }

    @EventHandler
    public void onMouseButton(EventMouseButton event) {
        if (this.isExecutingPlan() && event.getButton() == EventMouseButton.LEFT_BUTTON
                && event.getButtonState()) {
            event.setCancelled(true);
            event.getGameSettings().F().e();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPacketReceive(EventPacketReceive event) {
        EntityPlayerSP player = event.getThePlayer();
        if (player.isNull() || event.isCanceled() || !this.isExecutingPlan()) {
            return;
        }
        Packet packet = event.getPacket();
        if (packet.isInstance(MappedClasses.YX)) {
            SPacketEntityVelocity velocity = new SPacketEntityVelocity(packet.getObject());
            if (velocity.getEntityId() == player.S()) {
                this.trajectoryInvalidated = true;
            }
            return;
        }
        if (packet.isInstance(MappedClasses.qe) || packet.isInstance(MappedClasses.zw)) {
            this.trajectoryInvalidated = true;
            return;
        }
        if (!packet.isInstance(MappedClasses.DD) || this.plan == null) {
            return;
        }
        SPacketBlockChange change = new SPacketBlockChange(packet.getObject());
        BlockPos position = change.getBlockPosition();
        BlockState state = change.getBlockState();
        if (this.plan.getMode() == AutoLadderPlan.Mode.BUILD_SUPPORT
                && this.supportPlacementRequested && !this.supportConfirmed
                && this.plan.getSupportBlock().y(position)
                && !this.isStableSupport(state)) {
            this.placementRejected = true;
        } else if (this.state == AutoLadderState.PLACING_LADDER && this.placementAttempts > 0
                && this.plan.getLadderBlock().y(position)
                && !state.getBlock().equals(Blocks.ladder())) {
            this.placementRejected = true;
        }
    }

    private boolean canRun(EntityPlayerSP player, GuiScreen screen) {
        ModelPlayer capabilities = player.C$src$Lgg_vape_wrapper_impl_ModelPlayer_$19uhx86();
        return screen.isNull() && !capabilities.isFlying() && !capabilities.isCreativeMode()
                && !player.f$src$Z$fst3rk() && !player.Q$src$Z$fh9faz()
                && !player.h$src$Z$ftwoya();
    }

    private boolean shouldActivate(EntityPlayerSP player) {
        if (!(this.onLethalFall.getEffectiveValue().booleanValue()
                || this.onMoreThanXBlocks.getEffectiveValue().booleanValue())) {
            return false;
        }
        boolean refreshed = this.refreshLandingPrediction(player);
        BlockCoordinate landing = this.landingBlock;
        if (landing == null) {
            return false;
        }
        double landingTop = landing.E() + 1.0;
        float remainingDrop = (float)Math.max(0.0, player.N() - landingTop);
        float currentFallDistance = player.getFallDistance();
        float totalFallDistance = currentFallDistance + remainingDrop;
        float predictedDamage = BlockPlacementUtility.calculateFallDamage(player, totalFallDistance);
        boolean lethal = this.onLethalFall.getEffectiveValue().booleanValue()
                && predictedDamage >= this.effectiveHealth(player);
        boolean exceedsThreshold = this.onMoreThanXBlocks.getEffectiveValue().booleanValue()
                && totalFallDistance >= ((Double)this.blocksThreshold.getValue()).floatValue();
        boolean activate = lethal || exceedsThreshold;
        if (refreshed || activate != this.lastActivateResult) {
            this.lastActivateResult = activate;
        }
        return activate;
    }

    private boolean refreshLandingPrediction(EntityPlayerSP player) {
        if (this.lastLandingPredictionTick >= 0
                && this.stateTicks - this.lastLandingPredictionTick
                < LANDING_PREDICTION_INTERVAL_TICKS) {
            double deltaX = player.z() - this.lastPredictionX;
            double deltaZ = player.h() - this.lastPredictionZ;
            if (deltaX * deltaX + deltaZ * deltaZ
                    < LANDING_PREDICTION_DRIFT_THRESHOLD * LANDING_PREDICTION_DRIFT_THRESHOLD) {
                return false;
            }
        }
        BlockPlacementGraph graph = new BlockPlacementGraph(player);
        this.landingBlock = BlockPlacementUtility.predictLandingBlock(false, 50, player, graph);
        this.lastLandingPredictionTick = this.stateTicks;
        this.lastPredictionX = player.z();
        this.lastPredictionZ = player.h();
        return true;
    }

    private void runWarmupTick(EntityPlayerSP player, WorldClient world) {
        try {
            switch (this.warmupStage) {
                case 0:
                    Blocks.ladder();
                    BlockPlacementGraph graph = new BlockPlacementGraph(player);
                    this.warmupLanding = BlockPlacementUtility.predictLandingBlock(
                            false, 50, player, graph);
                    if (this.warmupLanding == null) {
                        this.warmupLanding = new BlockCoordinate(
                                MathUtil.floor(player.z()),
                                MathUtil.floor(player.N() - 1.0),
                                MathUtil.floor(player.h()));
                    }
                    BlockPlacementUtility.calculateFallDamage(player, 10.0f);
                    Vec3 warmupEye = Vec3.create(
                            player.z(), player.N() + player.X(), player.h());
                    RotationVectorMath.d(warmupEye,
                            Vec3.create(warmupEye.getX() + 1.0, warmupEye.getY(), warmupEye.getZ()),
                            player.J(), player.V());
                    break;
                case 1:
                    AutoLadderPlanner planner = new AutoLadderPlanner(
                            world, player, true, true, true, true,
                            new HashSet<>(), this.warmupLanding);
                    planner.findBestPlan();
                    break;
                case 2:
                    AutoLadderMovementController.chooseCentering(
                            player, world, player.z(), player.h());
                    Vec3 eye = Vec3.create(
                            player.z(), player.N() + player.X(), player.h());
                    EnumFacing facing = EnumFacing.t()[1];
                    BlockData support = this.warmupLanding.O();
                    PlacementTarget target = new PlacementTarget(support, facing);
                    ClutchPlacementPathUtils.isBlockFaceVisible(eye, world, support, facing);
                    ClutchPlacementPathUtils.findBestPlacementHitPointWithinReach(
                            player, world, eye, target,
                            player.J(), player.V(), Minecraft.playerController().N());
                    break;
                default:
                    break;
            }
            ++this.warmupStage;
            if (this.warmupStage > 2) {
                this.warmupPending = false;
            }
        } catch (Throwable error) {
            this.warmupPending = false;
        }
    }

    private void searchForPlan(EntityPlayerSP player, World world) {
        this.restoreMovementInput();
        this.lastSearchTick = this.stateTicks;
        if (!this.shouldActivate(player)) {
            this.transition(AutoLadderState.FALLING);
            return;
        }
        this.ladderSlot = this.findLadderSlot(player);
        this.supportSlot = this.findSupportSlot(player);
        boolean movementAvailable = !SharedModuleControlClaims.movementInput.isLocked();
        this.movementAvailable = movementAvailable;
        AutoLadderPlanner planner = new AutoLadderPlanner(world, player,
                this.ladderSlot != -1, this.supportSlot != -1,
                true,
                movementAvailable, this.rejectedPlans, this.landingBlock);
        AutoLadderPlan candidate;
        try {
            candidate = planner.findBestPlan();
        } catch (Throwable error) {
            this.enterFail(false);
            return;
        }
        this.queueFallAdjustment(planner.getRecommendedFallAdjustment());
        if (candidate == null) {
            boolean landingTooClose = this.isLandingTooClose(player);
            if (landingTooClose) {
                if (this.ladderSlot == -1) {
                    this.showFailNotification("No ladder available!", true);
                } else if (this.supportSlot == -1) {
                    this.showFailNotification("No support block available!", true);
                } else {
                    this.showFailNotification("Could not find a valid laddering solution!", true);
                }
                this.enterFail(false);
            }
            return;
        }
        if (candidate.getMode() != AutoLadderPlan.Mode.EXISTING_LADDER) {
            this.cancelPendingRotationReset();
            if (!this.acquireRotation()) {
                return;
            }
            this.captureOriginalRotation(player);
        }
        this.plan = candidate;
        this.queueFallAdjustment(candidate.getFallAdjustment());
        this.referenceYaw = player.J();
        this.supportPlacementRequested = false;
        this.supportConfirmed = candidate.getMode() != AutoLadderPlan.Mode.BUILD_SUPPORT;
        this.resetPlacementAttempts();
        if (candidate.getMode() == AutoLadderPlan.Mode.EXISTING_LADDER) {
            this.transition(AutoLadderState.CENTERING);
        } else {
            this.lockPlacementButtons();
            this.transition(candidate.getMode() == AutoLadderPlan.Mode.BUILD_SUPPORT
                    ? AutoLadderState.PLACING_BLOCK : AutoLadderState.PLACING_LADDER);
            if (candidate.getMode() == AutoLadderPlan.Mode.BUILD_SUPPORT) {
                this.handleBlockPlacement(player, world);
            } else {
                this.handleLadderPlacement(player, world);
            }
        }
    }

    private void handleBlockPlacement(EntityPlayerSP player, World world) {
        if (this.plan == null || this.plan.getBlockTarget() == null) {
            this.invalidatePlan(false);
            return;
        }
        if (this.isStableSupport(world, this.plan.getSupportBlock())) {
            this.confirmSupport(player, world);
            return;
        }
        if (!BlockUtil.u(this.blockAt(world, this.plan.getSupportBlock()))) {
            this.invalidatePlan(true);
            return;
        }
        this.supportSlot = this.ensureSupportSlot(player, this.supportSlot);
        boolean placementClear = this.isSupportPlacementClear(player, world);
        if (this.supportSlot == -1 || !placementClear) {
            if (this.isLandingTooClose(player)) {
                this.showFailNotification(this.supportSlot == -1
                        ? "No support block is available."
                        : "Could not find a valid AutoLadder plan.", true);
                this.enterFail(true);
            }
            return;
        }
        this.selectSlot(player, this.supportSlot);
        PlacementTarget target = this.plan.getBlockTarget();
        this.prepareRotation(player, world, target, 1);
    }

    private void handleLadderPlacement(EntityPlayerSP player, World world) {
        if (this.plan == null) {
            this.invalidatePlan(false);
            return;
        }
        if (this.confirmLadder(world)) {
            return;
        }
        this.ladderSlot = this.ensureLadderSlot(player, this.ladderSlot);
        if (this.ladderSlot == -1) {
            this.showFailNotification("No ladder is available.", true);
            this.enterFail(true);
            return;
        }
        this.selectSlot(player, this.ladderSlot);
        PlacementTarget target = this.plan.getLadderTarget();
        this.prepareRotation(player, world, target, 1);
        if (!this.isStableSupport(world, this.plan.getSupportBlock())) {
            this.invalidatePlan(true);
            return;
        }
        if (!this.supportConfirmed) {
            this.supportConfirmed = true;
        }
        Block ladderState = this.blockAt(world, this.plan.getLadderBlock());
        if (!BlockUtil.u(ladderState)) {
            this.invalidatePlan(true);
            return;
        }
    }

    private void handleCentering(EntityPlayerSP player, World world) {
        if (this.plan == null || !this.isLadder(world, this.plan.getLadderBlock())) {
            this.invalidatePlan(true);
            return;
        }
        if (player.N() < this.plan.getLadderBlock().B() - 0.05) {
            this.enterFail(true);
        }
    }

    private void handleGroundedPlayer(EntityPlayerSP player) {
        this.rejectedPlans.clear();
        this.fallEpisodeResolved = false;
        if (this.state == AutoLadderState.CENTERING && this.plan != null
                && this.isLadder(player.getWorld(), this.plan.getLadderBlock())
                && player.boolean_S() && player.getFallDistance() <= 0.5f) {
            this.transition(AutoLadderState.SAFE);
            this.completeSafeRun();
            return;
        }
        if (this.state == AutoLadderState.IDLE || this.state == AutoLadderState.FALLING) {
            this.transition(AutoLadderState.IDLE);
            return;
        }
        if (this.state == AutoLadderState.FAIL) {
            this.transition(AutoLadderState.IDLE);
            return;
        }
        if (this.state == AutoLadderState.SAFE) {
            this.completeSafeRun();
        } else {
            this.enterFail(true);
        }
    }

    public boolean canHandleFall(EntityPlayerSP player) {
        if (!this.isEnabled() || player == null || player.isNull()) {
            return false;
        }
        if (!this.failTimer.hasTimeElapsed(((Double)this.failDelay.getValue()).longValue())) {
            return false;
        }
        return this.findLadderSlot(player) != -1 && this.shouldActivate(player);
    }

    public boolean isRescueEngaged() {
        return this.state == AutoLadderState.SEARCHING_BLOCK || this.isExecutingPlan();
    }

    private boolean shouldControlMovement() {
        return this.plan != null && this.movementAvailable
                && (this.state == AutoLadderState.PLACING_BLOCK
                || this.state == AutoLadderState.PLACING_LADDER
                || this.state == AutoLadderState.CENTERING);
    }

    private void queueFallAdjustment(AutoLadderFallAdjustment adjustment) {
        this.pendingFallAdjustment = adjustment;
        this.fallAdjustmentPending = true;
    }

    private boolean isExecutingPlan() {
        return this.plan != null && (this.state == AutoLadderState.PLACING_BLOCK
                || this.state == AutoLadderState.PLACING_LADDER
                || this.state == AutoLadderState.CENTERING
                || this.state == AutoLadderState.SAFE);
    }

    private boolean requiresPlacementRotation() {
        return this.plan != null && (this.state == AutoLadderState.PLACING_BLOCK
                || this.state == AutoLadderState.PLACING_LADDER);
    }

    private boolean isLandingTooClose(EntityPlayerSP player) {
        if (this.landingBlock == null) {
            return false;
        }
        double remaining = player.N() - (this.landingBlock.E() + 1.0);
        return remaining <= Math.max(1.0, Math.abs(player.q()) * 1.5);
    }

    private boolean acquireRotation() {
        return this.rotationClaim.isOwnedBy(this)
                || this.rotationClaim.acquire(this, this.silentAim.getEffectiveValue());
    }

    private void captureOriginalRotation(EntityPlayerSP player) {
        if (this.silentAim.getEffectiveValue().booleanValue()) {
            this.savedYaw = UNSET_ANGLE;
            this.savedPitch = UNSET_ANGLE;
        } else if (this.savedYaw == UNSET_ANGLE) {
            this.savedYaw = player.J();
            this.savedPitch = player.V();
        }
        this.resetAngleDelayTicks = -1;
    }

    private void cancelPendingRotationReset() {
        if (!(this.rotationController instanceof AutoLadderResetRotationController)) {
            return;
        }
        FixedRotationController resetController = this.rotationController;
        RotationManager.INSTANCE.releaseController(resetController);
        if (this.rotationController == resetController) {
            this.rotationController = null;
        }
        this.savedYaw = UNSET_ANGLE;
        this.savedPitch = UNSET_ANGLE;
    }

    private void prepareRotation(EntityPlayerSP player, World world,
                                 PlacementTarget target, int ticksAvailable) {
        Vec3 eye = Vec3.create(player.z(), player.N() + player.X(), player.h());
        float currentYaw = this.getCurrentPlacementYaw(player);
        float currentPitch = this.getCurrentPlacementPitch(player);
        Vec3 hit = ClutchPlacementPathUtils.findBestPlacementHitPointWithinReach(
                player, world, eye, target, currentYaw, currentPitch,
                Minecraft.playerController().N());
        if (hit == null) {
            hit = this.faceCenter(target.supportBlock, target.facing);
        }
        target.hitPoint = hit;
        this.rotationController = this.buildRotation(
                player, eye, hit, this.rotationController, ticksAvailable, this.referenceYaw);
        if (RotationManager.INSTANCE.getActiveController() != this.rotationController) {
            RotationManager.INSTANCE.setController(this.rotationController);
        }
    }

    private FixedRotationController buildRotation(EntityPlayer player, Vec3 eye, Vec3 target,
                                                   FixedRotationController existing,
                                                   int ticksAvailable, float referenceYaw) {
        float sourceYaw;
        float sourcePitch;
        if (existing == null && RotationManager.INSTANCE.hasAdaptiveController()) {
            AdaptiveRotationController active =
                    (AdaptiveRotationController)RotationManager.INSTANCE.getActiveController();
            sourceYaw = active.getRenderedYaw();
            sourcePitch = active.getRenderedPitch();
        } else if (existing == null) {
            sourceYaw = player.J();
            sourcePitch = player.V();
        } else {
            sourceYaw = existing.getCurrentYaw();
            sourcePitch = existing.getCurrentPitch();
        }
        RotationAngles targetRotation = RotationVectorMath.d(eye, target, sourceYaw, sourcePitch);
        FixedRotationController controller = existing;
        if (controller == null) {
            controller = this.silentAim.getEffectiveValue().booleanValue()
                    ? new AdaptiveRotationController(player)
                    : new EntityFixedRotationController(targetRotation, player);
        }
        controller.setTargetRotation(targetRotation);
        if (controller instanceof AdaptiveRotationController) {
            AdaptiveRotationController adaptive = (AdaptiveRotationController)controller;
            adaptive.setReferenceYawOverride(Float.valueOf(referenceYaw));
            adaptive.setRelativeMode(false);
        }
        float currentYaw = controller instanceof AdaptiveRotationController
                ? ((AdaptiveRotationController)controller).getRenderedYaw() : player.J();
        float currentPitch = controller instanceof AdaptiveRotationController
                ? ((AdaptiveRotationController)controller).getRenderedPitch() : player.V();
        float yawDistance = Math.abs(MathUtil.wrapAngleTo180(targetRotation.getYaw() - currentYaw));
        float pitchDistance = Math.abs(targetRotation.getPitch() - currentPitch);
        float requestedSpeed = (yawDistance + pitchDistance) / 1.8f / Math.max(ticksAvailable, 1);
        controller.setRestoreCapturedRotation(true);
        controller.setSpeed(Math.min(MAXIMUM_ROTATION_SPEED, requestedSpeed));
        controller.setTolerance(0.0f);
        controller.setClampStepToRemaining(true);
        controller.setCubicAcceleration(false);
        controller.setLinearAcceleration(false);
        controller.setScaleAxesProportionally(true);
        controller.setRandomizeMovement(false);
        controller.setRetainAfterCompletion(true);
        return controller;
    }

    private float getCurrentPlacementYaw(EntityPlayerSP player) {
        if (this.rotationController instanceof AdaptiveRotationController) {
            return ((AdaptiveRotationController)this.rotationController).getRenderedYaw();
        }
        if (this.rotationController != null) {
            return this.rotationController.getCurrentYaw();
        }
        return player.J();
    }

    private float getCurrentPlacementPitch(EntityPlayerSP player) {
        if (this.rotationController instanceof AdaptiveRotationController) {
            return ((AdaptiveRotationController)this.rotationController).getRenderedPitch();
        }
        if (this.rotationController != null) {
            return this.rotationController.getCurrentPitch();
        }
        return player.V();
    }

    private boolean isLookingAt(PlacementTarget target) {
        RayTraceResult rayTrace = this.getPlacementRayTrace();
        if (rayTrace == null || rayTrace.isNull() || !rayTrace.isBlockHit()) {
            return false;
        }
        EntityPlayerSP player = Minecraft.thePlayer();
        Vec3 eye = Vec3.create(player.z(), player.N() + player.X(), player.h());
        Vec3 actualHit = rayTrace.getHitVec();
        if (actualHit == null || actualHit.isNull()
                || eye.distanceTo(actualHit) > Minecraft.playerController().N() + 1.0E-4) {
            return false;
        }
        boolean supportHit;
        if (ForgeVersion.MC_1_7_10.Y()) {
            supportHit = rayTrace.getBlockPos().equals(BlockPos.d(target.supportBlock));
        } else {
            supportHit = rayTrace.g() == target.supportBlock.D()
                    && rayTrace.T() == target.supportBlock.B()
                    && rayTrace.a$src$I$8nuo9d() == target.supportBlock.G();
        }
        return supportHit && target.facing.equals(rayTrace.getSideHit());
    }

    private Vec3 faceCenter(BlockData block, EnumFacing facing) {
        return Vec3.create(
                block.D() + 0.5 + facing.getDirectionVector().getX() * 0.5,
                block.B() + 0.5 + facing.getDirectionVector().getY() * 0.5,
                block.G() + 0.5 + facing.getDirectionVector().getZ() * 0.5);
    }

    private boolean isSupportPlacementClear(EntityPlayerSP player, World world) {
        if (this.plan == null || this.plan.getBlockTarget() == null) {
            return false;
        }
        BlockData placedBlock = this.plan.getBlockTarget().getPlacedBlock();
        if (!BlockUtil.u(this.blockAt(world, placedBlock))
                || !ClutchPlacementPathUtils.isPlacementSpaceClear(world, player, placedBlock)) {
            return false;
        }
        AxisAlignedBB blockBounds = AxisAlignedBB.create(
                placedBlock.D(), placedBlock.B(), placedBlock.G(),
                placedBlock.D() + 1.0, placedBlock.B() + 1.0, placedBlock.G() + 1.0);
        return !player.u$src$Lgg_vape_wrapper_impl_AxisAlignedBB_$kogbsu().intersects(blockBounds);
    }

    private boolean canAttemptPlacement() {
        int retryInterval = this.state == AutoLadderState.PLACING_LADDER
                ? 1 : PLACEMENT_RETRY_INTERVAL;
        return this.stateTicks - this.lastPlacementAttemptTick >= retryInterval;
    }

    private boolean confirmLadder(World world) {
        if (this.plan == null || !this.isLadder(world, this.plan.getLadderBlock())) {
            return false;
        }
        this.releasePlacementButtons();
        this.releaseSilentPlacementRotation();
        this.resetPlacementAttempts();
        this.transition(AutoLadderState.CENTERING);
        return true;
    }

    private boolean confirmSupport(EntityPlayerSP player, World world) {
        if (this.plan == null || this.plan.getMode() != AutoLadderPlan.Mode.BUILD_SUPPORT
                || !this.isStableSupport(world, this.plan.getSupportBlock())) {
            return false;
        }
        this.supportConfirmed = true;
        this.resetPlacementAttempts();
        this.transition(AutoLadderState.PLACING_LADDER);
        this.handleLadderPlacement(player, world);
        return true;
    }

    private void requestPlacement(boolean hold) {
        KeyBinding useKey = Minecraft.gameSettings().b$src$Lgg_vape_wrapper_impl_KeyBinding_$1yi3362();
        if (useKey.u() || useKey.isPressed()) {
            KeyBinding.setKeyBindState(useKey, false);
        }
        KeyBinding.setKeyBindState(useKey, true);
        KeyBinding.onTick(useKey);
        if (!hold) {
            KeyBinding.setKeyBindState(useKey, false);
        }
        this.placementKeyHeld = hold;
        this.lastPlacementAttemptTick = this.stateTicks;
        ++this.placementAttempts;
    }

    private void resetPlacementAttempts() {
        this.placementAttempts = 0;
        this.lastPlacementAttemptTick = -1000;
    }

    private void lockPlacementButtons() {
        if (!this.buttonsLocked) {
            SharedModuleControlClaims.mouseButtons.lock();
            this.buttonsLocked = true;
        }
        if (!this.rightClickBlocked) {
            SharedModuleControlClaims.rightClickUse.blockUse();
            this.rightClickBlocked = true;
        }
    }

    private void releasePlacementButtons() {
        this.releaseHeldPlacementKey();
        if (this.buttonsLocked) {
            SharedModuleControlClaims.mouseButtons.unlock();
            this.buttonsLocked = false;
        }
        if (this.rightClickBlocked) {
            SharedModuleControlClaims.rightClickUse.clearClaimed();
            this.rightClickBlocked = false;
        }
    }

    private void releaseHeldPlacementKey() {
        if (!this.placementKeyHeld) {
            return;
        }
        KeyBinding useKey = Minecraft.gameSettings().b$src$Lgg_vape_wrapper_impl_KeyBinding_$1yi3362();
        KeyBinding.setKeyBindState(useKey, false);
        this.placementKeyHeld = false;
    }

    private void invalidatePlan(boolean reject) {
        if (this.plan != null && reject) {
            this.rejectedPlans.add(this.plan.rejectionKey());
        }
        this.releaseRunControls(true);
        if (this.isLandingTooClose(Minecraft.thePlayer())) {
            this.enterFail(false);
        } else {
            this.transition(AutoLadderState.SEARCHING_BLOCK);
        }
    }

    private void enterFail(boolean rejectPlan) {
        if (rejectPlan && this.plan != null) {
            this.rejectedPlans.add(this.plan.rejectionKey());
        }
        this.releaseRunControls(true);
        this.failTimer.reset();
        this.transition(AutoLadderState.FAIL);
    }

    private void completeSafeRun() {
        this.fallEpisodeResolved = true;
        this.releaseRunControls(true);
        this.transition(AutoLadderState.IDLE);
    }

    private void releaseRunControls(boolean scheduleRestore) {
        this.plan = null;
        this.pendingFallAdjustment = AutoLadderFallAdjustment.PHYSICAL;
        this.fallAdjustmentPending = false;
        this.supportPlacementRequested = false;
        this.supportConfirmed = false;
        this.releasePlacementButtons();
        this.resetPlacementAttempts();
        this.caughtTicks = 0;
        this.restoreMovementInput();
        if (scheduleRestore) {
            this.returnDelayTicks = (int)Math.round(this.returnDelay.getRandomValue());
            if (this.silentAim.getEffectiveValue().booleanValue()) {
                this.resetAngleDelayTicks = -1;
                this.releaseRotationImmediately();
            } else {
                this.resetAngleDelayTicks = this.sampleDefaultResetAngleDelayTicks();
            }
            if (!this.returnToLastSlot.getEffectiveValue().booleanValue()) {
                this.previousSlot = -1;
            }
        } else {
            this.restoreSlotImmediately();
            this.releaseRotationImmediately();
        }
    }

    private int sampleDefaultResetAngleDelayTicks() {
        double delay = ThreadLocalRandom.current().nextDouble(
                DEFAULT_RESET_ANGLE_DELAY_MIN_TICKS,
                DEFAULT_RESET_ANGLE_DELAY_MAX_TICKS);
        return (int)Math.round(delay);
    }

    private void tickPostRun(EntityPlayerSP player, GuiScreen screen) {
        if (this.plan != null) {
            return;
        }
        if (this.returnToLastSlot.getEffectiveValue().booleanValue() && this.previousSlot != -1
                && this.returnDelayTicks-- <= 0) {
            this.selectHotbarSlot(this.previousSlot);
            this.previousSlot = -1;
            this.returnDelayTicks = -1;
        }
        if (this.resetAngleDelayTicks >= 0 && this.resetAngleDelayTicks-- <= 0) {
            this.resetRotation(player);
            this.resetAngleDelayTicks = -1;
        }
        if (screen.isNotNull() && this.movementControlled) {
            MovementInputHelper.restorePhysicalInput(false);
            this.movementControlled = false;
        }
    }

    private void resetRotation(EntityPlayerSP player) {
        if (this.rotationController == null) {
            this.rotationClaim.release(this);
            this.savedYaw = UNSET_ANGLE;
            this.savedPitch = UNSET_ANGLE;
            return;
        }
        boolean shouldReset = this.resetAngle.getEffectiveValue().booleanValue()
                && !this.silentAim.getEffectiveValue().booleanValue()
                && this.savedYaw != UNSET_ANGLE;
        if (!shouldReset) {
            this.releaseRotationImmediately();
            return;
        }
        RotationManager.INSTANCE.releaseController(this.rotationController);
        float yawDelta = player.J() - this.savedYaw;
        float pitchDelta = player.V() - this.savedPitch;
        if (Math.abs(yawDelta) < 0.01f && Math.abs(pitchDelta) < 0.01f) {
            this.releaseRotationImmediately();
            return;
        }
        AutoLadderResetRotationController reset = new AutoLadderResetRotationController(
                this, player, yawDelta, pitchDelta);
        reset.setRandomizeMovement(true);
        reset.setScaleAxesProportionally(true);
        reset.setLinearAcceleration(true);
        reset.setClampStepToRemaining(true);
        reset.setTolerance(0.0f);
        reset.setSpeed(Math.max(1.0f,
                Math.abs(MathUtil.wrapAngleTo180(yawDelta)) / 90.0f * 5.0f));
        this.rotationController = reset;
        RotationManager.INSTANCE.setController(reset);
    }

    public void onRotationResetComplete(AutoLadderResetRotationController controller) {
        if (this.rotationController != controller) {
            return;
        }
        this.rotationClaim.release(this);
        this.rotationController = null;
        this.savedYaw = UNSET_ANGLE;
        this.savedPitch = UNSET_ANGLE;
    }

    private void releaseRotationImmediately() {
        if (this.rotationController != null) {
            RotationManager.INSTANCE.releaseController(this.rotationController);
            if (RotationManager.INSTANCE.getActiveController() == this.rotationController) {
                this.rotationController.setRetainAfterCompletion(false);
                this.rotationController.setComplete(true);
                if (this.rotationController instanceof AdaptiveRotationController) {
                    ((AdaptiveRotationController)this.rotationController).setRelativeMode(true);
                    this.rotationController.setComplete(true);
                }
            }
            this.rotationController = null;
        }
        this.rotationClaim.release(this);
        this.savedYaw = UNSET_ANGLE;
        this.savedPitch = UNSET_ANGLE;
    }

    private void releaseSilentPlacementRotation() {
        if (!this.silentAim.getEffectiveValue().booleanValue()
                || !(this.rotationController instanceof AdaptiveRotationController)) {
            return;
        }
        this.releaseRotationImmediately();
        this.resetAngleDelayTicks = -1;
    }

    private void showFailNotification(String message, boolean forceUpdate) {
        this.failNotification = RescueModuleUtil.updateFailNotification(
                this.failNotification, "AutoLadder Failed", message, forceUpdate);
    }

    private void resetImmediately() {
        this.releaseRunControls(false);
        this.state = AutoLadderState.IDLE;
        this.stateTicks = 0;
        this.ladderSlot = -1;
        this.supportSlot = -1;
        this.returnDelayTicks = -1;
        this.resetAngleDelayTicks = -1;
        this.landingBlock = null;
        this.placementRejected = false;
        this.trajectoryInvalidated = false;
        this.movementAvailable = false;
        this.fallEpisodeResolved = false;
        this.lastActivateResult = false;
        this.lastLandingPredictionTick = -100;
        this.lastSearchTick = -100;
        this.warmupPending = false;
        this.rejectedPlans.clear();
    }

    private void transition(AutoLadderState next) {
        if (this.state == next) {
            return;
        }
        this.state = next;
        this.stateTicks = 0;
        if (next == AutoLadderState.PLACING_BLOCK || next == AutoLadderState.PLACING_LADDER) {
            this.resetPlacementAttempts();
        }
        if (next == AutoLadderState.FALLING || next == AutoLadderState.SEARCHING_BLOCK) {
            this.lastLandingPredictionTick = -100;
            this.lastPredictionX = 0.0;
            this.lastPredictionZ = 0.0;
        }
        if (next == AutoLadderState.SEARCHING_BLOCK) {
            this.lastSearchTick = -100;
        }
    }

    private float effectiveHealth(EntityPlayerSP player) {
        return player.w$src$F$15l9epb() + player.p();
    }

    private Block blockAt(World world, BlockData block) {
        return world.getBlockByPos(block.D(), block.B(), block.G());
    }

    private boolean isLadder(World world, BlockData block) {
        Block worldBlock = this.blockAt(world, block);
        return worldBlock.isNotNull() && worldBlock.equals(Blocks.ladder());
    }

    private boolean isStableSupport(World world, BlockData blockData) {
        return this.isStableSupport(world.getBlockState(BlockPos.d(blockData)));
    }

    private boolean isStableSupport(BlockState state) {
        Block block = state.getBlock();
        return block.isNotNull() && BlockUtil.k(block)
                && !ClutchPlacementPathUtils.isBlacklistedPlacementBlock(block);
    }

    private int ensureLadderSlot(EntityPlayerSP player, int slot) {
        if (slot >= 0 && slot < 9
                && this.isLadderStack(player.V$src$Lgg_vape_wrapper_impl_InventoryPlayer_$erqak6().c(slot))) {
            return slot;
        }
        return this.findLadderSlot(player);
    }

    private int ensureSupportSlot(EntityPlayerSP player, int slot) {
        if (slot >= 0 && slot < 9
                && this.isSupportStack(player.V$src$Lgg_vape_wrapper_impl_InventoryPlayer_$erqak6().c(slot))) {
            return slot;
        }
        return this.findSupportSlot(player);
    }

    private int findLadderSlot(EntityPlayerSP player) {
        InventoryPlayer inventory = player.V$src$Lgg_vape_wrapper_impl_InventoryPlayer_$erqak6();
        for (int slot = 0; slot < 9; ++slot) {
            if (this.isLadderStack(inventory.c(slot))) {
                return slot;
            }
        }
        return -1;
    }

    private int findSupportSlot(EntityPlayerSP player) {
        InventoryPlayer inventory = player.V$src$Lgg_vape_wrapper_impl_InventoryPlayer_$erqak6();
        if (this.heldWhitelist.getEffectiveValue().booleanValue()) {
            int currentSlot = inventory.v();
            ItemStack held = inventory.c(currentSlot);
            return this.whitelistBlocks.matches(held) && this.isSupportStack(held) ? currentSlot : -1;
        }
        List<Integer> validSlots = new ArrayList<>();
        for (int slot = 0; slot < 9; ++slot) {
            if (this.isSupportStack(inventory.c(slot))) {
                validSlots.add(slot);
            }
        }
        return RescueModuleUtil.findPreferredSlot(
                inventory, validSlots, this.preferredBlockNames);
    }

    private boolean isLadderStack(ItemStack stack) {
        if (stack.isNull() || stack.getItem().isNull()) {
            return false;
        }
        Item item = stack.getItem();
        return item.isInstance(MappedClasses.Vw)
                && new ItemBlock(item).C().equals(Blocks.ladder());
    }

    private boolean isSupportStack(ItemStack stack) {
        if (stack.isNull() || stack.getItem().isNull()) {
            return false;
        }
        Item item = stack.getItem();
        if (!item.isInstance(MappedClasses.Vw)) {
            return false;
        }
        Block block = new ItemBlock(item).C();
        return block.isNotNull() && !block.equals(Blocks.ladder()) && BlockUtil.b(block)
                && !ClutchPlacementPathUtils.isBlacklistedPlacementBlock(block)
                && (!this.blacklist.getEffectiveValue().booleanValue()
                || this.blacklistBlocks.doesNotMatch(stack));
    }

    private int countLadders() {
        EntityPlayerSP player = Minecraft.thePlayer();
        if (player.isNull()) {
            return 0;
        }
        int count = 0;
        InventoryPlayer inventory = player.V$src$Lgg_vape_wrapper_impl_InventoryPlayer_$erqak6();
        for (int slot = 0; slot < 9; ++slot) {
            ItemStack stack = inventory.c(slot);
            if (this.isLadderStack(stack)) {
                count += stack.t();
            }
        }
        return count;
    }

    private void selectSlot(EntityPlayerSP player, int slot) {
        InventoryPlayer inventory = player.V$src$Lgg_vape_wrapper_impl_InventoryPlayer_$erqak6();
        if (this.previousSlot == -1) {
            this.previousSlot = inventory.v();
        }
        this.selectHotbarSlot(slot);
    }

    private void selectHotbarSlot(int slot) {
        EntityPlayerSP player = Minecraft.thePlayer();
        if (player.isNotNull()) {
            player.V$src$Lgg_vape_wrapper_impl_InventoryPlayer_$erqak6().g(slot);
        }
    }

    private void restoreSlotImmediately() {
        if (this.previousSlot != -1) {
            this.selectHotbarSlot(this.previousSlot);
            this.previousSlot = -1;
        }
    }

    private void restoreMovementInput() {
        if (!this.movementControlled) {
            return;
        }
        if (Minecraft.currentScreen().isNull()) {
            MovementInputHelper.restorePhysicalInput(false);
        }
        this.movementControlled = false;
    }

    private RayTraceResult getPlacementRayTrace() {
        return RotationManager.INSTANCE.rayTraceUsingManagedRotation(
                Minecraft.playerController().N() + 0.1, 0.0f, false);
    }
}
