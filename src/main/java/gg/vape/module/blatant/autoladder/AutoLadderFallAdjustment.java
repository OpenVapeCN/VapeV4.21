package gg.vape.module.blatant.autoladder;

/** One-tick pre-placement movement adjustment evaluated together with a ladder plan. */
public enum AutoLadderFallAdjustment {
    PHYSICAL(false, false, false, false, false),
    FORWARD(true, true, false, false, false),
    BACKWARD(true, false, true, false, false),
    LEFT(true, false, false, true, false),
    RIGHT(true, false, false, false, true);

    private final boolean overrideInput;
    private final boolean forward;
    private final boolean backward;
    private final boolean left;
    private final boolean right;

    AutoLadderFallAdjustment(boolean overrideInput, boolean forward, boolean backward,
                             boolean left, boolean right) {
        this.overrideInput = overrideInput;
        this.forward = forward;
        this.backward = backward;
        this.left = left;
        this.right = right;
    }

    public boolean overridesInput() {
        return this.overrideInput;
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
