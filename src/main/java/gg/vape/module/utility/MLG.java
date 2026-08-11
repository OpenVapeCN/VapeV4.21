package gg.vape.module.utility;

import gg.vape.Vape;
import gg.vape.event.EventHandler;
import gg.vape.event.impl.EventPreTick;
import gg.vape.inventory.InventoryClick;
import gg.vape.mapping.ItemMappingEntry;
import gg.vape.module.Category;
import gg.vape.module.Mod;
import gg.vape.module.blatant.Fly;
import gg.vape.module.blatant.blockin.BlockInHotbarSlotHelper;
import gg.vape.module.blatant.blockin.BlockPlacementGraph;
import gg.vape.module.blatant.blockin.BlockPlacementUtility;
import gg.vape.module.blatant.blockin.HotbarSlotResolution;
import gg.vape.module.blatant.blockin.HotbarSlotResolutionWithValue;
import gg.vape.module.control.SharedModuleControlClaims;
import gg.vape.module.utility.inventory.ItemStackActionPredicate;
import gg.vape.module.utility.mlg.MLGPlacementController;
import gg.vape.rotation.AdaptiveRotationController;
import gg.vape.rotation.FixedRotationController;
import gg.vape.rotation.RotationManager;
import gg.vape.unmap.ItemLimitData;
import gg.vape.utils.BlockUtil;
import gg.vape.utils.TimerUtil;
import gg.vape.utils.datas.BlockCoordinate;
import gg.vape.value.BooleanValue;
import gg.vape.value.LimitValue;
import gg.vape.value.NumberValue;
import gg.vape.value.RandomValue;
import gg.vape.wrapper.impl.Block;
import gg.vape.wrapper.impl.BlockPos;
import gg.vape.wrapper.impl.BlockState;
import gg.vape.wrapper.impl.EntityPlayerSP;
import gg.vape.wrapper.impl.ForgeVersion;
import gg.vape.wrapper.impl.Minecraft;
import gg.vape.wrapper.impl.ModelPlayer;
import gg.vape.wrapper.impl.Slot;
import gg.vape.wrapper.impl.World;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MLG
extends Mod {
    @NotNull
    public final TimerUtil inventoryClickTimer;
    private MLGState state;
    private final BooleanValue onLethalFall;
    @Nullable
    private BlockCoordinate lastPlacePos = null;
    public final NumberValue aimSpeed;
    public final BooleanValue checkInventory;
    private final BlockInHotbarSlotHelper slotHelper;
    private final BooleanValue onXDamage = BooleanValue.create(this, "On at least X damage", false, "Activate MLG when the fall will do at least X health");
    @NotNull
    private final TimerUtil conserveTimer;
    @Nullable
    private BlockCoordinate targetCoordinate = null;
    @Nullable
    private ItemMappingEntry mlgItem = null;
    private double lastHealth = 0.0;
    @Nullable
    public FixedRotationController pickupRotation = null;
    public final LimitValue nonRemovableItems;
    public final Queue<InventoryClick> clickQueue;
    private final RandomValue clickDelayValue;
    private boolean guiOpenedByMlg = false;
    private final NumberValue healthValue;
    @Nullable
    private BlockPlacementGraph placementGraph = null;
    public final BooleanValue useCobwebs;
    @NotNull
    public final TimerUtil placementTimer;
    private double accumulatedFall = 0.0;
    private static final long MAGIC_ID = 7584752828418109695L;
    private final BooleanValue pickUpWater;
    public final BooleanValue useBuckets;
    @Nullable
    public FixedRotationController placementRotation = null;
    public final MLGPlacementController placementController;
    public final BooleanValue silentAim;

    private boolean shouldActivate() {
        if (Minecraft.currentScreen().isNotNull()) {
            return false;
        }
        EntityPlayerSP localPlayer = Minecraft.thePlayer();
        if (localPlayer.isNull()) {
            return false;
        }
        ModelPlayer capabilities = localPlayer.C$src$Lgg_vape_wrapper_impl_ModelPlayer_$19uhx86();
        if (this.accumulatedFall < 2.0 || localPlayer.q() == 0.0 || capabilities.isCreativeMode() || capabilities.N() || capabilities.isFlying() || localPlayer.Q$src$Z$fh9faz() || localPlayer.M$src$Z$ff28xj() || localPlayer.k$src$Z$15enw27() || ForgeVersion.MC_1_16_5.d() && localPlayer.X$src$Z$1id4hz7() || localPlayer.b$src$Z$fqlxe4() || localPlayer.S$src$Z$151gttj() || localPlayer.h$src$Z$ftwoya() || localPlayer.d() || localPlayer.D$src$Z$fa43la()) {
            return false;
        }
        if (Vape.INSTANCE.getModManager().getMod(Fly.class).isEnabled()) {
            return false;
        }
        float predictedDropDistance = 0.0f;
        BlockCoordinate landingCoordinate = BlockPlacementUtility.predictLandingBlock(false, 50, localPlayer, this.getPlacementGraph(localPlayer, true));
        if (landingCoordinate != null) {
            World world = localPlayer.getWorld();
            for (int heightOffset = 0; heightOffset <= 3; ++heightOffset) {
                Block block;
                BlockPos blockPos = landingCoordinate.E$src$Lgg_vape_wrapper_impl_BlockPos_$1bb1czr().W(heightOffset);
                BlockState blockState = world.getBlockState(blockPos);
                if (!blockState.isNotNull() || !(block = blockState.getBlock()).isNotNull() || !BlockUtil.C(block)) continue;
                return false;
            }
            predictedDropDistance = (float)(localPlayer.N() - (double)(landingCoordinate.E() + 1));
        }
        float groundDistance = BlockPlacementUtility.getDistanceToGround(localPlayer, true, false, this.getPlacementGraph(localPlayer, false));
        float fallDistance = Math.max(predictedDropDistance, groundDistance);
        if (fallDistance <= 0.0f) {
            return false;
        }
        float fallDamage = BlockPlacementUtility.calculateFallDamage(localPlayer, fallDistance);
        double damageThreshold = (Double)this.healthValue.getValue();
        float remainingHealth = localPlayer.w$src$F$15l9epb() - fallDamage;
        if (this.onXDamage.getEffectiveValue().booleanValue() && (double)fallDamage >= damageThreshold) {
            return true;
        }
        return this.onLethalFall.getEffectiveValue() != false && remainingHealth <= 0.0f;
    }


    public MLG() {
        super("MLG", (int)MAGIC_ID, Category.WORLD, "Automatically places water under you when you fall");
        this.onLethalFall = BooleanValue.create(this, "On lethal fall", true, "Activate MLG when the fall would deal enough damage to kill you");
        this.aimSpeed = NumberValue.create(this, "Aim speed", "#.#", "", 5.0, 5.0, 15.0, 0.5, "How quickly MLG will change your look angles");
        this.pickUpWater = BooleanValue.create(this, "Pick up water", false, "Pick up placed water back into the bucket");
        this.inventoryClickTimer = new TimerUtil();
        this.clickDelayValue = RandomValue.createWithDescription(this, "Click Delay", "#", "ms", 50.0, 75.0, 125.0, 200.0, 5.0, "Delay used between inv clicks");
        this.placementTimer = new TimerUtil();
        this.conserveTimer = new TimerUtil();
        this.clickQueue = new ConcurrentLinkedQueue<InventoryClick>();
        this.silentAim = BooleanValue.create(this, "Silent aim", false, "Uses Silent Aim system");
        this.useBuckets = BooleanValue.create(this, "Use buckets", true, "Allow use of water buckets to save from fall damage");
        this.useCobwebs = BooleanValue.create(this, "Use cobwebs", true, "Allow use of cobwebs to save from fall damage");
        this.checkInventory = BooleanValue.create(this, "Check inventory", true, "Retrieves MLG Item to use from Inventory if not in Hotbar");
        this.nonRemovableItems = LimitValue.create(this, "mlg-whitelisteditems", "Non-removable Items", LimitValue.ALLOW_LIST_COLOR, Arrays.asList(new ItemLimitData("Water Bucket"), new ItemLimitData("Bucket"), new ItemLimitData("Cobweb")));
        this.healthValue = NumberValue.create(this, "Health", "#", "", 1.0, 5.0, 20.0, 1.0, "Min amount of fall damage for activation");
        this.state = MLGState.IDLE;
        this.useBuckets.addDependentValues(this.pickUpWater);
        this.onXDamage.addDependentValues(this.healthValue);
        this.checkInventory.addDependentValues(this.clickDelayValue);
        this.addValue(this.useBuckets, this.pickUpWater, this.useCobwebs, this.checkInventory, this.clickDelayValue, this.silentAim, this.aimSpeed, this.onLethalFall, this.onXDamage, this.healthValue);
        this.placementController = new MLGPlacementController(this);
        this.slotHelper = new BlockInHotbarSlotHelper(this);
        SharedModuleControlClaims.rotation.setPriority(this, 10);
    }

    private void releaseRotation(FixedRotationController fixedRotationController, boolean releaseClaim, boolean restoreAdaptive) {
        if (fixedRotationController == null) {
            return;
        }
        if (fixedRotationController.equals(RotationManager.INSTANCE.getActiveController())) {
            RotationManager.INSTANCE.releaseController(fixedRotationController);
        }
        fixedRotationController.setRetainAfterCompletion(false);
        if (restoreAdaptive && fixedRotationController instanceof AdaptiveRotationController) {
            ((AdaptiveRotationController)fixedRotationController).setRelativeMode(false);
            fixedRotationController.setComplete(true);
        }
        if (releaseClaim) {
            SharedModuleControlClaims.rotation.release(this);
        }
    }

    @NotNull
    public HotbarSlotResolution openInventory() {
        if (ItemStackActionPredicate.isInventoryScreenOpen()) {
            this.guiOpenedByMlg = true;
            return HotbarSlotResolution.success("Inventory GUI is open.");
        }
        if (ItemStackActionPredicate.isAnyScreenOpen()) {
            HotbarSlotResolution closeResult = this.closeInventory();
            if (closeResult.isFailure()) {
                return HotbarSlotResolution.failure(String.format("Cannot open inventory GUI because we cannot exit our current GUI due to: %s", closeResult.getMessage())).setForced(closeResult.canContinue());
            }
            return closeResult.isSuccess() ? HotbarSlotResolution.pending("Closed existing GUI, will open inventory next tick.") : HotbarSlotResolution.pending("Waiting to close GUI");
        }
        if (this.isInventoryClickReady()) {
            this.inventoryClickTimer.reset();
            boolean waitingForOpen = ItemStackActionPredicate.openInventory();
            if (!waitingForOpen) {
                return HotbarSlotResolution.success("Opened inventory GUI");
            }
            this.guiOpenedByMlg = true;
            return HotbarSlotResolution.pending("Waiting for inventory to open");
        }
        return HotbarSlotResolution.pending("Waiting for inventory delay before opening GUI.");
    }

    @NotNull
    public HotbarSlotResolution closeInventory() {
        if (!ItemStackActionPredicate.isAnyScreenOpen()) {
            this.guiOpenedByMlg = false;
            return HotbarSlotResolution.success("No GUI is open.");
        }
        if (!this.guiOpenedByMlg) {
            return HotbarSlotResolution.failure("In a GUI opened by the player (not MLG), cannot close it.").force();
        }
        if (this.isInventoryClickReady()) {
            this.inventoryClickTimer.reset();
            boolean waitingForClose = ItemStackActionPredicate.closeCurrentScreen();
            if (!waitingForClose) {
                return HotbarSlotResolution.success("GUI closed.");
            }
            this.guiOpenedByMlg = false;
            return HotbarSlotResolution.pending("Waiting for GUI to close.");
        }
        return HotbarSlotResolution.pending("Waiting for inventory click to be available");
    }

    public boolean acquireRotationControl() {
        return SharedModuleControlClaims.rotation.isOwnedBy(this) || SharedModuleControlClaims.rotation.acquire(this, this.silentAim.getEffectiveValue());
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.resetState();
    }

    private BlockPlacementGraph getPlacementGraph(EntityPlayerSP localPlayer, boolean forceRebuild) {
        if (forceRebuild || this.placementGraph == null) {
            this.placementGraph = new BlockPlacementGraph(localPlayer);
        }
        return this.placementGraph;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.resetState();
    }

    public boolean isInventoryClickReady() {
        return this.clickDelayValue.getMaximumValue() <= 0.0 || this.inventoryClickTimer.hasTimeElapsed((long)this.clickDelayValue.getRandomValue());
    }

    @EventHandler
    public void onTick(EventPreTick event) {
        HotbarSlotResolution hotbarSlotResolution;
        EntityPlayerSP localPlayer = event.getThePlayer();
        if (localPlayer.isNull()) {
            return;
        }
        double health = localPlayer.N();
        double healthDelta = health - this.lastHealth;
        if (healthDelta > 0.0) {
            this.accumulatedFall = 0.0;
        } else if (healthDelta < 0.0) {
            this.accumulatedFall -= healthDelta;
        }
        if (localPlayer.b$src$Z$fqlxe4()) {
            this.accumulatedFall = 0.0;
        }
        this.lastHealth = health;
        if (this.state == MLGState.IDLE && !this.shouldActivate()) {
            return;
        }
        if (localPlayer.b$src$Z$fqlxe4() && (this.state == MLGState.EQUIPPING_ITEM || this.state == MLGState.AIMING)) {
            this.resetState();
            return;
        }
        if (localPlayer.N() < (double)event.getWorld().R()) {
            this.resetState();
            return;
        }
        if (this.state == MLGState.AIMING || this.state == MLGState.EQUIPPING_ITEM) {
            this.targetCoordinate = BlockPlacementUtility.predictLandingBlock(false, 50, localPlayer, this.getPlacementGraph(localPlayer, true));
            HotbarSlotResolutionWithValue<Slot> slotResolution = this.slotHelper.equipAvailableMlgItem();
            if (!slotResolution.canContinue()) {
                this.resetState();
                return;
            }
            if (!slotResolution.isSuccess()) {
                return;
            }
            Slot slot = slotResolution.getValue();
            if (slot == null || slot.isNull()) {
                this.resetState();
                return;
            }
            this.mlgItem = BlockPlacementUtility.getSlotItem(slot);
            if (this.state == MLGState.EQUIPPING_ITEM) {
                this.state = MLGState.AIMING;
            }
        }
        switch (this.state) {
            case IDLE: {
                this.state = MLGState.EQUIPPING_ITEM;
            }
            case EQUIPPING_ITEM: {
                break;
            }
            case AIMING: {
                if (this.placementRotation == null) {
                    this.placementRotation = this.placementController.createRotationController(this.targetCoordinate, this.mlgItem);
                }
                if (this.mlgItem == null) {
                    this.resetState();
                    return;
                }
                hotbarSlotResolution = this.placementController.aimAtTarget(this.mlgItem, this.targetCoordinate, this.placementRotation, false);
                if (!hotbarSlotResolution.canContinue()) {
                    this.resetState();
                    return;
                }
                if (!hotbarSlotResolution.isSuccess()) {
                    return;
                }
                HotbarSlotResolutionWithValue<BlockPos> placementResolution = this.placementController.placeItem(this.mlgItem, this.targetCoordinate, null);
                if (!placementResolution.canContinue()) {
                    this.resetState();
                    return;
                }
                if (!placementResolution.isSuccess()) {
                    return;
                }
                BlockPos blockPos = placementResolution.getValue();
                if (blockPos != null && blockPos.isNotNull()) {
                    this.lastPlacePos = new BlockCoordinate(blockPos.getX(), blockPos.getY(), blockPos.getZ());
                }
                if (BlockPlacementUtility.getWaterBucketItem().equals(this.mlgItem) && this.pickUpWater.getEffectiveValue().booleanValue()) {
                    this.placementTimer.reset();
                    this.conserveTimer.reset();
                    this.state = MLGState.CONSERVING_WATER;
                    break;
                }
                this.resetState();
                return;
            }
            case CONSERVING_WATER: {
                if (!this.conserveTimer.hasTimeElapsed(100L)) {
                    return;
                }
                HotbarSlotResolution aimResolution = this.handleWaterAim();
                hotbarSlotResolution = this.slotHelper.placeMlgItem(this.lastPlacePos, this.conserveTimer);
                if (!hotbarSlotResolution.canContinue()) {
                    this.resetState();
                    return;
                }
                if (!aimResolution.canContinue()) {
                    this.resetState();
                    return;
                }
                if (hotbarSlotResolution.isSuccess()) {
                    this.resetState();
                }
                return;
            }
        }
    }

    private HotbarSlotResolution handleWaterAim() {
        if (this.lastPlacePos == null) {
            return HotbarSlotResolution.failure("Cannot handle water aim job, lastPlacePos is null.");
        }
        if (this.pickupRotation == null) {
            this.pickupRotation = this.placementController.createRotationController(this.lastPlacePos, BlockPlacementUtility.getEmptyBucketItem());
        }
        return this.placementController.aimAtTarget(BlockPlacementUtility.getEmptyBucketItem(), this.lastPlacePos, this.pickupRotation, true);
    }

    private void resetState() {
        this.placementTimer.reset();
        this.conserveTimer.reset();
        this.clickQueue.clear();
        if (this.placementRotation != null) {
            this.releaseRotation(this.placementRotation, true, false);
        }
        if (this.pickupRotation != null) {
            this.releaseRotation(this.pickupRotation, true, false);
        }
        this.placementRotation = null;
        this.pickupRotation = null;
        this.mlgItem = null;
        this.guiOpenedByMlg = false;
        this.lastPlacePos = null;
        this.placementGraph = null;
        this.targetCoordinate = null;
        this.state = MLGState.IDLE;
        this.accumulatedFall = 0.0;
        this.lastHealth = 0.0;
        this.inventoryClickTimer.reset();
    }

    enum MLGState {
        IDLE,
        EQUIPPING_ITEM,
        AIMING,
        CONSERVING_WATER
    }
}
