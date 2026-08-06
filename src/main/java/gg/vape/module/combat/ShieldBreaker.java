package gg.vape.module.combat;

import gg.vape.event.Event;
import gg.vape.event.EventHandler;
import gg.vape.event.impl.EventKeyPress;
import gg.vape.event.impl.EventMouseButton;
import gg.vape.event.impl.EventPreTick;
import gg.vape.input.AttackKeyController;
import gg.vape.mapping.MappedClasses;
import gg.vape.module.Category;
import gg.vape.module.Mod;
import gg.vape.utils.RotationUtil;
import gg.vape.utils.TimerUtil;
import gg.vape.value.BooleanValue;
import gg.vape.value.RandomValue;
import gg.vape.wrapper.impl.EntityLivingBase;
import gg.vape.wrapper.impl.EntityOtherPlayerMP;
import gg.vape.wrapper.impl.ItemStack;
import gg.vape.wrapper.impl.Minecraft;

public class ShieldBreaker extends Mod {

    private static final long MODULE_ID = -7666507152973844354L;
    private static final String AXE_KEYWORD = "axe";

    private final TimerUtil timer = new TimerUtil();
    private final RandomValue switchDelay;
    private final RandomValue switchBackDelay;

    private int oldSlot = -1;
    private int pendingAxe = -1;

    private final BooleanValue autoSwitchBack;
    private boolean switching;
    private boolean switchPending;
    private boolean attackPending;
    private boolean releasePending;

    private long attackDelay;
    private long switchBackDelayMs;

    public ShieldBreaker() {
        super("ShieldBreaker", (int)MODULE_ID, Category.COMBAT, "Switch axe when attacking");

        this.switchDelay = RandomValue.createWithDescription(this, "Switch delay", "#", "ms", 10.0, 130.0, 180.0, 500.0, 0.1, "Automatically switch to an axe to disable shields");

        this.autoSwitchBack = BooleanValue.create(this, "Auto Switch back", true, "Auto switch original slot");

        this.switchBackDelay = RandomValue.createWithDescription(this, "Switch back delay", "#", "ms", 10.0, 130.0, 180.0, 500.0, 0.1, "Delay before switching back");

        this.autoSwitchBack.addDependentValues(this.switchBackDelay);

        this.addValue(this.switchDelay, this.switchBackDelay, this.autoSwitchBack);

        this.switchBackDelay.setMaximumFractionDigits(0);
    }

    @EventHandler
    public void onMouseButton(EventMouseButton e) {
        if(e.isKeybinding(Minecraft.gameSettings().F()) && e.getButtonState())
            handleAttack(e);
    }

    @SuppressWarnings("unused")
    @EventHandler
    public void onKeyPress(EventKeyPress e) {

        if(e.isKeybinding(Minecraft.gameSettings().F()) && e.isDown())
            handleAttack(e);
    }

    private void handleAttack(Event e) {

        if(Minecraft.currentScreen().isNotNull())
            return;

        if(switching)
            return;

        EntityLivingBase target = RotationUtil.u(6.0,180.0);

        if(target == null || !target.isInstance(MappedClasses.lG))
            return;

        EntityOtherPlayerMP player = new EntityOtherPlayerMP(target.getObject());

        if(!RotationUtil.n(player))
            return;

        int axe = findAxe();

        if(axe == -1)
            return;

        oldSlot = Minecraft.thePlayer().V$src$Lgg_vape_wrapper_impl_InventoryPlayer_$erqak6().v();

        pendingAxe = axe;

        switching = true;
        switchPending = true;

        double[] delayRange = this.switchDelay.getValue();

        double randomDelay = delayRange[0] + Math.random() * (delayRange[1] - delayRange[0]);

        attackDelay = (long)randomDelay;

        timer.reset();

        e.setCancelled(true);
    }

    @SuppressWarnings("unused")
    @EventHandler
    public void onTick(EventPreTick e) {

        if(!switching)
            return;

        if(switchPending) {
            if(!timer.hasTimeElapsed(attackDelay))
                return;

            Minecraft.thePlayer().V$src$Lgg_vape_wrapper_impl_InventoryPlayer_$erqak6().g(pendingAxe);

            switchPending = false;
            attackPending = true;

            timer.reset();

            return;
        }

        if(attackPending) {
            if(!timer.hasTimeElapsed(50))
                return;

            AttackKeyController.releaseAttackKey();

            AttackKeyController.requestSyntheticAttack(this);

            attackPending = false;
            releasePending = true;

            timer.reset();

            return;
        }

        if(releasePending) {
            AttackKeyController.releaseAttackKey();

            double[] delayRange = this.switchBackDelay.getValue();

            double randomDelay = delayRange[0] + Math.random() * (delayRange[1] - delayRange[0]);

            switchBackDelayMs = (long)randomDelay;

            releasePending = false;

            timer.reset();

            return;
        }

        if(!this.autoSwitchBack.getEffectiveValue()) {
            oldSlot = -1;
            pendingAxe = -1;
            switching = false;

            return;
        }

        if(!timer.hasTimeElapsed(switchBackDelayMs))
            return;

        AttackKeyController.releaseAttackKey();

        if(oldSlot != -1)
            Minecraft.thePlayer().V$src$Lgg_vape_wrapper_impl_InventoryPlayer_$erqak6().g(oldSlot);

        oldSlot = -1;
        pendingAxe = -1;

        switching = false;

        switchBackDelayMs = 0;
    }

    @Override
    public void onDisable() {
        super.onDisable();

        AttackKeyController.releaseAttackKey();

        oldSlot = -1;
        pendingAxe = -1;

        switching = false;
        switchPending = false;
        attackPending = false;
        releasePending = false;

        attackDelay = 0;
        switchBackDelayMs = 0;
    }

    private int findAxe() {
        for(int i = 0; i < 9; i++) {

            ItemStack stack = Minecraft.thePlayer().V$src$Lgg_vape_wrapper_impl_InventoryPlayer_$erqak6().c(i);

            if(stack.getObject() == null || stack.getItem().getObject() == null)
                continue;

            String name = stack.getItem().getItemStackDisplayName(stack);

            if(name.toLowerCase().contains(AXE_KEYWORD))
                return i;
        }

        return -1;
    }
}