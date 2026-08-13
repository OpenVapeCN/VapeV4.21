package gg.vape.mapping.mappings;

import gg.vape.mapping.MappedClasses;
import gg.vape.mapping.Mapping;
import gg.vape.mapping.MappingField;
import gg.vape.mapping.MappingFieldBuilder;
import gg.vape.mapping.MappingMethod;
import gg.vape.mapping.MappingMethodBuilder;
import gg.vape.ui.click.component.GuiComponent;
import gg.vape.wrapper.Wrapper;
import gg.vape.wrapper.impl.BlockPos;
import gg.vape.wrapper.impl.ForgeVersion;

public class MBlock
extends Mapping {
    public MappingMethod J;
    private MappingMethod r;
    private MappingMethod E;
    private MappingMethod B;
    private MappingMethod Z;
    private MappingField x;
    public MappingField g;
    private MappingMethod P;
    private MappingMethod k;
    public MappingMethod u;
    private MappingField T;
    private MappingMethod H;
    private MappingMethod j;
    public MappingMethod S;
    private static GuiComponent[] Y;
    public final MappingField D;
    private MappingMethod O;
    private MappingMethod q;
    private MappingField o;
    public MappingMethod w;
    private MappingMethod K;
    private MappingField U;
    public MappingMethod v;
    public final MappingMethod n;
    private MappingMethod W;
    public MappingMethod M;
    private MappingMethod F;
    private final MappingField Q;
    public MappingMethod X;
    private MappingMethod playerRelativeBlockHardnessMethod;

    public int h(Object object) {
        return this.n.invokeInt(null, object);
    }

    public static GuiComponent[] m() {
        return Y;
    }

    public static int E(MBlock mBlock, Object object) {
        return mBlock.k(object);
    }

    public static void Y(GuiComponent[] guiComponentArray) {
        Y = guiComponentArray;
    }

    private Object z(Object object, Object object2, int n, int n2, int n3) {
        return this.v.invokeObject(object, object2, n, n2, n3);
    }

    private boolean X(Object object, Object object2, Object object3, Object object4, Object object5, Object object6, Object object7, float f, float f2, float f3) {
        return this.q.invokeBoolean(object, object2, object3, object4, object5, object6, object7, Float.valueOf(f), Float.valueOf(f2), Float.valueOf(f3));
    }

    private Object x(Object object, Object object2, Object object3, Object object4) {
        if (ForgeVersion.MC_1_21_4.d()) {
            return this.Z.invokeObject(object, object2, object3, object4, Boolean.FALSE);
        }
        return this.Z.invokeObject(object, object2, object3, object4);
    }

    private Object N(Object object) {
        return this.X.invokeObject(object, new Object[0]);
    }

    private void X(Object object, Object object2, Object object3, Object object4, Object object5, double d) {
        this.H.invokeVoid(object, object2, object3, object4, object5, d);
    }

    private int N(Object object, Object object2) {
        return this.k.invokeInt(object, object2);
    }

    public Object a(String string) {
        return this.F.invokeObject(null, string);
    }

    private void i(Object object, Object object2, int n, int n2, int n3, Object object3) {
        this.r.invokeVoid(object, object2, n, n2, n3, object3);
    }

    public static boolean L(MBlock mBlock, Object object, Object object2, Object object3, Object object4, Object object5, Object object6, float f, float f2, float f3) {
        return mBlock.i(object, object2, object3, object4, object5, object6, f, f2, f3);
    }

    public Object y(Object object, Object object2, Object object3, Object object4) {
        return this.j.invokeObject(object, object2, object3, object4);
    }

    public static Object N(MBlock mBlock, Object object) {
        return mBlock.B(object);
    }

    public static float w(MBlock mBlock, Object object) {
        return mBlock.T(object);
    }

    public static Object N(MBlock mBlock) {
        return mBlock.T();
    }

    public static int V(MBlock mBlock, Object object, Object object2, Object object3) {
        return mBlock.n(object, object2, object3);
    }

    private int O(Object object, Object object2, int n, int n2, int n3) {
        return this.k.invokeInt(object, object2, n, n2, n3);
    }

    private Object T() {
        return this.U.getObject(null);
    }

    public static boolean X(MBlock mBlock, Object object) {
        return mBlock.q(object);
    }

    public boolean Z(Object object) {
        return this.K.invokeBoolean(object, new Object[0]);
    }

    private Object U(Object object, Object object2, Object object3) {
        return this.v.invokeObject(object, object2, object3);
    }

    public boolean r(Object object, Object object2) {
        return this.K.invokeBoolean(object, object2);
    }

    private Object V(Object object, Object object2, int n, int n2, int n3) {
        if (ForgeVersion.MC_1_7_10.L()) {
            return this.u.invokeObject(object, object2, n, n2, n3);
        }
        return this.u.invokeObject(object, object2, BlockPos.create(n, n2, n3).getObject(), null);
    }

    public static int Q(MBlock mBlock, Object object, Object object2, int n, int n2, int n3) {
        return mBlock.O(object, object2, n, n2, n3);
    }

    public static int W(MBlock mBlock, Object object, Object object2) {
        return mBlock.N(object, object2);
    }

    public boolean E(Object object, Object object2) {
        return this.P.invokeBoolean(object, object2);
    }

    private boolean i(Object object, Object object2, Object object3, Object object4, Object object5, Object object6, float f, float f2, float f3) {
        return this.q.invokeBoolean(object, object2, object3, object4, object5, object6, Float.valueOf(f), Float.valueOf(f2), Float.valueOf(f3));
    }

    public static void u(MBlock mBlock, Object object, Object object2, Object object3, Object object4, Object object5, double d) {
        mBlock.X(object, object2, object3, object4, object5, d);
    }

    static {
        MBlock.Y(null);
    }

    public static String l(MBlock mBlock, Object object) {
        return mBlock.f(object);
    }

    private boolean q(Object object) {
        return this.o.getBoolean(object);
    }

    private Object W(Object object, Object object2) {
        return this.X.invokeObject(object, object2);
    }

    private Object B(Object object) {
        return this.T.getObject(object);
    }

    public boolean F$src$Z$6w97vr(Object object) {
        return this.P.invokeBoolean(object, new Object[0]);
    }

    public static void v(MBlock mBlock, Object object, Object object2, int n, int n2, int n3, Object object3) {
        mBlock.i(object, object2, n, n2, n3, object3);
    }

    private int n(Object object, Object object2, Object object3) {
        return this.k.invokeInt(object, object2, object3);
    }

    private String f(Object object) {
        return (String)this.D.getObject(object);
    }

    private boolean k$src$Z$1m9upos(Object object) {
        return this.B.invokeBoolean(object, new Object[0]);
    }

    public static Object h(MBlock mBlock, Object object) {
        return mBlock.N(object);
    }

    public static Object Q(MBlock mBlock, Object object, Object object2, Object object3, Object object4) {
        return mBlock.x(object, object2, object3, object4);
    }

    public static Object J(MBlock mBlock, Object object, Object object2, int n, int n2, int n3) {
        return mBlock.V(object, object2, n, n2, n3);
    }

    public static void y(MBlock mBlock, Object object, Object object2, Object object3) {
        mBlock.Z(object, object2, object3);
    }

    private int k(Object object) {
        return this.W.invokeInt(object, new Object[0]);
    }


    public boolean C(Object object, Object object2, boolean bl) {
        return this.E.invokeBoolean(object, object2, bl);
    }

    public static Object d(MBlock mBlock, Object object, Object object2, Object object3) {
        return mBlock.U(object, object2, object3);
    }

    public MBlock() {
        this(MBlock.m());
    }

    private MBlock(GuiComponent[] guiComponentArray) {
        super(MappedClasses.Zk);
        GuiComponent[] guiComponentArray2 = guiComponentArray;
        if (ForgeVersion.MC_1_8_9.L()) {
            this.playerRelativeBlockHardnessMethod = this.Y("getPlayerRelativeBlockHardness", true, Float.TYPE,
                    MappedClasses.Yl, MappedClasses.YU, MappedClasses.lf);
        }
        if (ForgeVersion.MC_1_20_6.v()) {
            Class<Float> clazz = Float.TYPE;
            boolean bl = true;
            if (ForgeVersion.MC_1_16_5.d() && ForgeVersion.MC_1_17.v()) {
                this.x = this.registerInstanceFieldForOwner(MappedClasses.Fj, "hardness", bl, clazz);
            } else {
                String string = "blockHardness";
                MBlock mBlock = this;
                this.x = mBlock.J(string, bl, clazz);
            }
        }
        if (!ForgeVersion.MC_1_16_5.d()) {
            if (ForgeVersion.MC_1_12_2.d()) {
                Class[] classArray = new Class[]{MappedClasses.Vv, MappedClasses.YU, MappedClasses.lf};
                Class clazz = MappedClasses.uk;
                boolean bl = true;
                String string = "getSelectedBoundingBox";
                MBlock mBlock = this;
                this.v = mBlock.Y(string, bl, clazz, classArray);
            } else if (ForgeVersion.MC_1_7_10.Y()) {
                Class[] classArray = new Class[]{MappedClasses.YU, MappedClasses.lf};
                Class clazz = MappedClasses.uk;
                boolean bl = true;
                String string = "getSelectedBoundingBox";
                MBlock mBlock = this;
                this.v = mBlock.Y(string, bl, clazz, classArray);
            } else {
                Class[] classArray = new Class[]{MappedClasses.YU, Integer.TYPE, Integer.TYPE, Integer.TYPE};
                Class clazz = MappedClasses.uk;
                boolean bl = true;
                String string = "getSelectedBoundingBoxFromPool";
                MBlock mBlock = this;
                this.v = mBlock.Y(string, bl, clazz, classArray);
            }
        }
        if (ForgeVersion.MC_1_7_10.Y()) {
            Class clazz = MappedClasses.Vv;
            String string = "defaultBlockState";
            MBlock mBlock = this;
            this.T = ((MappingFieldBuilder)((MappingFieldBuilder)mBlock.fieldBuilder(string, clazz).setNameForVersion(ForgeVersion.MC_1_16_5.n(), "defaultState")).setTypeForVersion(ForgeVersion.MC_1_16_5.n(), MappedClasses.Zl)).buildField();
        }
        if (ForgeVersion.MC_1_16_5.d()) {
            Class[] classArray = new Class[]{MappedClasses.Zl};
            Class<Integer> clazz = Integer.TYPE;
            boolean bl = true;
            String string = "getStateId";
            MBlock mBlock = this;
            this.n = mBlock.registerStaticMethod(string, bl, clazz, classArray);
            if (ForgeVersion.MC_1_20_6.v()) {
                Class clazz2 = MappedClasses.ZN;
                boolean bl2 = true;
                String string2 = "material";
                Class clazz3 = MappedClasses.za;
                MBlock mBlock2 = this;
                this.g = this.registerInstanceFieldForOwner(clazz3, string2, bl2, clazz2);
            }
            Class<Boolean> clazz4 = Boolean.TYPE;
            boolean bl3 = true;
            String string3 = "canCollide";
            Class clazz5 = MappedClasses.za;
            MBlock mBlock3 = this;
            this.o = this.registerInstanceFieldForOwner(clazz5, string3, bl3, clazz4);
            if (ForgeVersion.MC_1_20_6.d()) {
                Class[] classArray2 = new Class[]{MappedClasses.Zl, MappedClasses.YU, MappedClasses.lf, MappedClasses.Yl, MappedClasses.qF};
                Class clazz6 = MappedClasses.zr;
                boolean bl4 = true;
                String string4 = "useWithoutItem";
                Class clazz7 = MappedClasses.za;
                MBlock mBlock4 = this;
                this.q = this.registerInstanceMethodForOwner(clazz7, string4, bl4, clazz6, classArray2);
            } else {
                Class[] classArray3 = new Class[]{MappedClasses.Zl, MappedClasses.YU, MappedClasses.lf, MappedClasses.Yl, MappedClasses.Yf, MappedClasses.qF};
                Class clazz8 = MappedClasses.zr;
                boolean bl5 = true;
                String string5 = "onBlockActivated";
                Class clazz9 = MappedClasses.za;
                MBlock mBlock5 = this;
                this.q = this.registerInstanceMethodForOwner(clazz9, string5, bl5, clazz8, classArray3);
            }
            Class clazz10 = MappedClasses.Zl;
            boolean bl6 = true;
            String string6 = "defaultState";
            MBlock mBlock6 = this;
            this.T = this.J(string6, bl6, clazz10);
            if (ForgeVersion.MC_1_21_0.d()) {
                if (ForgeVersion.MC_1_21_4.d()) {
                    Class[] classArray4 = new Class[]{MappedClasses.F7, MappedClasses.lf, MappedClasses.Zl, Boolean.TYPE};
                    Class clazz11 = MappedClasses.VK;
                    boolean bl7 = true;
                    String string7 = "getCloneItemStack";
                    Class clazz12 = MappedClasses.za;
                    MBlock mBlock7 = this;
                    this.Z = this.registerInstanceMethodForOwner(clazz12, string7, bl7, clazz11, classArray4);
                } else {
                    Class[] classArray5 = new Class[]{MappedClasses.F7, MappedClasses.lf, MappedClasses.Zl};
                    Class clazz13 = MappedClasses.VK;
                    boolean bl8 = true;
                    String string8 = "getCloneItemStack";
                    MBlock mBlock8 = this;
                    this.Z = this.Y(string8, bl8, clazz13, classArray5);
                }
            } else {
                Class[] classArray6 = new Class[]{MappedClasses.zJ, MappedClasses.lf, MappedClasses.Zl};
                Class clazz14 = MappedClasses.VK;
                boolean bl9 = true;
                String string9 = "getItem";
                MBlock mBlock9 = this;
                this.Z = this.Y(string9, bl9, clazz14, classArray6);
            }
        } else {
            Class[] classArray = new Class[]{MappedClasses.Zk};
            Class<Integer> clazz = Integer.TYPE;
            boolean bl = true;
            String string = "getIdFromBlock";
            MBlock mBlock = this;
            this.n = mBlock.registerStaticMethod(string, bl, clazz, classArray);
            Class[] classArray7 = new Class[]{String.class};
            Class clazz15 = MappedClasses.Zk;
            boolean bl10 = true;
            String string10 = "getBlockFromName";
            MBlock mBlock10 = this;
            this.F = this.registerStaticMethod(string10, bl10, clazz15, classArray7);
            if (ForgeVersion.MC_1_12_2.d()) {
                Class[] classArray8 = new Class[]{MappedClasses.Vv};
                Class clazz16 = MappedClasses.ZN;
                boolean bl11 = true;
                String string11 = "getMaterial";
                MBlock mBlock11 = this;
                this.X = this.Y(string11, bl11, clazz16, classArray8);
            } else {
                Class[] classArray9 = new Class[]{};
                Class clazz17 = MappedClasses.ZN;
                boolean bl12 = true;
                String string12 = "getMaterial";
                MBlock mBlock12 = this;
                this.X = this.Y(string12, bl12, clazz17, classArray9);
            }
        }
        if (ForgeVersion.MC_1_12_2.v()) {
            Class[] classArray = new Class[]{};
            Class<Integer> clazz = Integer.TYPE;
            boolean bl = true;
            String string = "getRenderType";
            MBlock mBlock = this;
            this.W = mBlock.Y(string, bl, clazz, classArray);
        }
        if (ForgeVersion.MC_1_7_10.L()) {
            Class[] classArray = new Class[]{MappedClasses.YU, Integer.TYPE, Integer.TYPE, Integer.TYPE, MappedClasses.zc};
            Class<Void> clazz = Void.TYPE;
            boolean bl = true;
            String string = "onEntityWalking";
            MBlock mBlock = this;
            this.r = mBlock.Y(string, bl, clazz, classArray);
            Class[] classArray10 = new Class[]{};
            Class<Integer> clazz18 = Integer.TYPE;
            boolean bl13 = true;
            String string13 = "getRenderBlockPass";
            MBlock mBlock13 = this;
            this.w = this.Y(string13, bl13, clazz18, classArray10);
            Class[] classArray11 = new Class[]{MappedClasses.YU, Integer.TYPE, Integer.TYPE, Integer.TYPE};
            Class clazz19 = MappedClasses.uk;
            boolean bl14 = true;
            String string14 = "getCollisionBoundingBoxFromPool";
            MBlock mBlock14 = this;
            this.u = this.Y(string14, bl14, clazz19, classArray11);
            Class[] classArray12 = new Class[]{Integer.TYPE, Boolean.TYPE};
            Class<Boolean> clazz20 = Boolean.TYPE;
            boolean bl15 = true;
            String string15 = Wrapper.vapeInstance.isVanillaMinecraftPresent() ? "canStopRayTrace" : "canCollideCheck";
            MBlock mBlock15 = this;
            this.E = this.Y(string15, bl15, clazz20, classArray12);
            Class[] classArray13 = new Class[]{MappedClasses.YU, Integer.TYPE, Integer.TYPE, Integer.TYPE, MappedClasses.Yl, Integer.TYPE, Float.TYPE, Float.TYPE, Float.TYPE};
            Class<Boolean> clazz21 = Boolean.TYPE;
            boolean bl16 = true;
            String string16 = "onBlockActivated";
            MBlock mBlock16 = this;
            this.q = this.Y(string16, bl16, clazz21, classArray13);
            Class[] classArray14 = new Class[]{};
            Class<Boolean> clazz22 = Boolean.TYPE;
            boolean bl17 = true;
            String string17 = "isNormalCube";
            MBlock mBlock17 = this;
            this.K = this.Y(string17, bl17, clazz22, classArray14);
        } else {
            if (ForgeVersion.MC_1_12_2.d() && ForgeVersion.MC_1_16_5.v()) {
                Class[] classArray = new Class[]{MappedClasses.YU, MappedClasses.lf, MappedClasses.Vv, MappedClasses.Yl, MappedClasses.Yf, MappedClasses.q0, Float.TYPE, Float.TYPE, Float.TYPE};
                Class<Boolean> clazz = Boolean.TYPE;
                boolean bl = true;
                String string = "onBlockActivated";
                MBlock mBlock = this;
                this.q = mBlock.Y(string, bl, clazz, classArray);
            } else if (ForgeVersion.MC_1_8_9.d() && ForgeVersion.MC_1_12_2.v()) {
                Class[] classArray = new Class[]{MappedClasses.YU, MappedClasses.lf, MappedClasses.Vv, MappedClasses.Yl, MappedClasses.q0, Float.TYPE, Float.TYPE, Float.TYPE};
                Class<Boolean> clazz = Boolean.TYPE;
                boolean bl = true;
                String string = "onBlockActivated";
                MBlock mBlock = this;
                this.q = mBlock.Y(string, bl, clazz, classArray);
            }
            if (ForgeVersion.MC_1_12_2.d()) {
                if (ForgeVersion.MC_1_16_5.v()) {
                    Class[] classArray = new Class[]{MappedClasses.Vv, MappedClasses.zR, MappedClasses.lf, MappedClasses.q0};
                    Class<Boolean> clazz = Boolean.TYPE;
                    boolean bl = true;
                    String string = "shouldSideBeRendered";
                    MBlock mBlock = this;
                    this.J = mBlock.Y(string, bl, clazz, classArray);
                    Class[] classArray15 = new Class[]{MappedClasses.Vv};
                    Class<Boolean> clazz23 = Boolean.TYPE;
                    boolean bl18 = true;
                    String string18 = "isBlockNormalCube";
                    MBlock mBlock18 = this;
                    this.P = this.Y(string18, bl18, clazz23, classArray15);
                    Class[] classArray16 = new Class[]{MappedClasses.Vv};
                    Class<Boolean> clazz24 = Boolean.TYPE;
                    boolean bl19 = true;
                    String string19 = "isNormalCube";
                    MBlock mBlock19 = this;
                    this.K = this.Y(string19, bl19, clazz24, classArray16);
                    Class[] classArray17 = new Class[]{MappedClasses.Vv};
                    Class<Float> clazz25 = Float.TYPE;
                    boolean bl20 = true;
                    String string20 = "getAmbientOcclusionLightValue";
                    MBlock mBlock20 = this;
                    this.M = this.Y(string20, bl20, clazz25, classArray17);
                    Class[] classArray18 = new Class[]{MappedClasses.Vv, MappedClasses.zR, MappedClasses.lf};
                    Class clazz26 = MappedClasses.uk;
                    boolean bl21 = true;
                    String string21 = "getCollisionBoundingBox";
                    MBlock mBlock21 = this;
                    this.u = this.Y(string21, bl21, clazz26, classArray18);
                }
            } else {
                Class[] classArray = new Class[]{MappedClasses.YU, MappedClasses.lf, MappedClasses.Vv};
                Class clazz = MappedClasses.uk;
                boolean bl = true;
                String string = "getCollisionBoundingBox";
                MBlock mBlock = this;
                this.u = mBlock.Y(string, bl, clazz, classArray);
                Class[] classArray19 = new Class[]{};
                Class<Boolean> clazz27 = Boolean.TYPE;
                boolean bl22 = true;
                String string22 = "isBlockNormalCube";
                MBlock mBlock22 = this;
                this.P = this.Y(string22, bl22, clazz27, classArray19);
                Class[] classArray20 = new Class[]{};
                Class<Boolean> clazz28 = Boolean.TYPE;
                boolean bl23 = true;
                String string23 = "isNormalCube";
                MBlock mBlock23 = this;
                this.K = this.Y(string23, bl23, clazz28, classArray20);
                Class[] classArray21 = new Class[]{};
                Class<Float> clazz29 = Float.TYPE;
                boolean bl24 = true;
                String string24 = "getAmbientOcclusionLightValue";
                MBlock mBlock24 = this;
                this.M = this.Y(string24, bl24, clazz29, classArray21);
                if (ForgeVersion.MC_1_16_5.v()) {
                    Class[] classArray22 = new Class[]{MappedClasses.zR, MappedClasses.lf, MappedClasses.q0};
                    Class<Boolean> clazz30 = Boolean.TYPE;
                    boolean bl25 = true;
                    String string25 = "shouldSideBeRendered";
                    MBlock mBlock25 = this;
                    this.J = this.Y(string25, bl25, clazz30, classArray22);
                }
            }
            if (ForgeVersion.MC_1_16_5.v()) {
                Class[] classArray = new Class[]{};
                Class clazz = MappedClasses.E;
                boolean bl = true;
                String string = ForgeVersion.c() >= 23 ? "getRenderLayer" : "getBlockLayer";
                MBlock mBlock = this;
                this.S = mBlock.Y(string, bl, clazz, classArray);
                Class[] classArray23 = new Class[]{MappedClasses.Vv, MappedClasses.zR, MappedClasses.lf};
                Class clazz31 = MappedClasses.Vv;
                boolean bl26 = true;
                String string26 = "getActualState";
                MBlock mBlock26 = this;
                this.j = this.Y(string26, bl26, clazz31, classArray23);
                Class[] classArray24 = new Class[]{MappedClasses.Vv, Boolean.TYPE};
                Class<Boolean> clazz32 = Boolean.TYPE;
                boolean bl27 = true;
                String string27 = "canCollideCheck";
                MBlock mBlock27 = this;
                this.E = this.Y(string27, bl27, clazz32, classArray24);
            }
        }
        if (ForgeVersion.MC_1_8_9.B()) {
            if (ForgeVersion.MC_1_7_10.L()) {
                Class[] classArray = new Class[]{MappedClasses.YU, Integer.TYPE, Integer.TYPE, Integer.TYPE};
                Class<Integer> clazz = Integer.TYPE;
                boolean bl = true;
                String string = "getDamageValue";
                MBlock mBlock = this;
                this.k = mBlock.Y(string, bl, clazz, classArray);
            } else {
                Class[] classArray = new Class[]{MappedClasses.YU, MappedClasses.lf};
                Class<Integer> clazz = Integer.TYPE;
                boolean bl = true;
                String string = "getDamageValue";
                MBlock mBlock = this;
                this.k = mBlock.Y(string, bl, clazz, classArray);
            }
        } else if (ForgeVersion.MC_1_16_5.v()) {
            Class[] classArray = new Class[]{MappedClasses.Vv};
            Class<Integer> clazz = Integer.TYPE;
            boolean bl = true;
            String string = "getMetaFromState";
            MBlock mBlock = this;
            this.k = mBlock.Y(string, bl, clazz, classArray);
        }
        if (ForgeVersion.MC_1_16_5.d()) {
            Class[] classArray = new Class[]{};
            Class<Boolean> clazz = Boolean.TYPE;
            String string = "isSolid";
            MBlock mBlock = this;
            this.O = ((MappingMethodBuilder)mBlock.methodBuilder(string, clazz, classArray).setOwnerClass(ForgeVersion.MC_1_20_6.v() ? MappedClasses.ZN : MappedClasses.Fj)).buildMethod();
            Class<Float> clazz33 = Float.TYPE;
            boolean bl = true;
            String string28 = "slipperiness";
            Class clazz34 = MappedClasses.za;
            MBlock mBlock28 = this;
            this.Q = this.registerInstanceFieldForOwner(clazz34, string28, bl, clazz33);
            if (ForgeVersion.MC_1_20_6.d()) {
                Class clazz35 = MappedClasses.lz;
                boolean bl28 = true;
                String string29 = "BLOCK";
                Class clazz36 = MappedClasses.R;
                MBlock mBlock29 = this;
                this.U = this.registerStaticFieldForOwner(clazz36, string29, bl28, clazz35);
            } else {
                Class clazz37 = MappedClasses.lz;
                boolean bl29 = true;
                String string30 = "BLOCK";
                Class clazz38 = MappedClasses.Fk;
                MBlock mBlock30 = this;
                this.U = this.registerStaticFieldForOwner(clazz38, string30, bl29, clazz37);
            }
            if (ForgeVersion.MC_1_21_4.d()) {
                Class<String> clazz39 = String.class;
                boolean bl30 = true;
                String string31 = "translationKey";
                Class clazz40 = MappedClasses.za;
                MBlock mBlock31 = this;
                this.D = this.registerInstanceFieldForOwner(clazz40, string31, bl30, clazz39);
                Class[] classArray25 = new Class[]{MappedClasses.zc};
                Class<Boolean> clazz41 = Boolean.TYPE;
                String string32 = "canEntityWalkOnPowderSnow";
                MBlock mBlock32 = this;
                this.B = ((MappingMethodBuilder)((MappingMethodBuilder)this.methodBuilder(string32, clazz41, classArray25).setOwnerClass(MappedClasses.DM)).setStaticMember(true)).buildMethod();
                Class[] classArray26 = new Class[]{MappedClasses.zJ, MappedClasses.zc};
                Class<Void> clazz42 = Void.TYPE;
                String string33 = "updateEntityMovementAfterFallOn";
                MBlock mBlock33 = this;
            this.H = this.methodBuilder(string33, clazz42, classArray26).buildMethod();
            } else {
                Class<String> clazz43 = String.class;
                boolean bl31 = true;
                String string34 = "translationKey";
                MBlock mBlock34 = this;
                this.D = this.J(string34, bl31, clazz43);
            }
        } else {
            Class<Float> clazz = Float.TYPE;
            boolean bl = true;
            String string = "slipperiness";
            MBlock mBlock = this;
            this.Q = mBlock.J(string, bl, clazz); 
            Class<String> clazz44 = String.class;
            boolean bl32 = true;
            String string35 = ForgeVersion.c() >= 23 ? "translationKey" : "unlocalizedName";
            MBlock mBlock35 = this;
            this.D = this.J(string35, bl32, clazz44);
        }
    }

    public static Object d(MBlock mBlock, Object object, Object object2, int n, int n2, int n3) {
        return mBlock.z(object, object2, n, n2, n3);
    }

    public float F(Object object) {
        if (ForgeVersion.MC_1_16_5.d() && ForgeVersion.MC_1_17.v()) {
            return this.x.getFloat(this.T.getObject(object));
        }
        return this.x.getFloat(object);
    }

    public static Object E(MBlock mBlock, Object object, Object object2, Object object3, Object object4, Object object5, Object object6, Object object7) {
        return mBlock.d(object, object2, object3, object4, object5, object6, object7);
    }

    public static Object y(MBlock mBlock, Object object, Object object2, Object object3, Object object4) {
        return mBlock.o(object, object2, object3, object4);
    }

    private Object d(Object object, Object object2, Object object3, Object object4, Object object5, Object object6, Object object7) {
        if (ForgeVersion.MC_1_20_6.d()) {
            return this.q.invokeObject(object, object2, object3, object4, object5, object7);
        }
        return this.q.invokeObject(object, object2, object3, object4, object5, object6, object7);
    }

    private boolean W(Object object, Object object2, int n, int n2, int n3, Object object3, int n4, float f, float f2, float f3) {
        return this.q.invokeBoolean(object, object2, n, n2, n3, object3, n4, Float.valueOf(f), Float.valueOf(f2), Float.valueOf(f3));
    }

    public static boolean G(MBlock mBlock, Object object, Object object2, Object object3, Object object4, Object object5, Object object6, Object object7, float f, float f2, float f3) {
        return mBlock.X(object, object2, object3, object4, object5, object6, object7, f, f2, f3);
    }

    private Object o(Object object, Object object2, Object object3, Object object4) {
        return this.v.invokeObject(object, object2, object3, object4);
    }

    public static boolean w(MBlock mBlock, Object object, Object object2, int n, int n2, int n3, Object object3, int n4, float f, float f2, float f3) {
        return mBlock.W(object, object2, n, n2, n3, object3, n4, f, f2, f3);
    }

    public static boolean k$src$Z$8ngnn6(MBlock mBlock, Object object) {
        return mBlock.k$src$Z$1m9upos(object);
    }

    public boolean A(Object object) {
        return this.O.invokeBoolean(object, new Object[0]);
    }

    public static Object z(MBlock mBlock, Object object, Object object2) {
        return mBlock.W(object, object2);
    }

    public static Object k(MBlock mBlock, Object object) {
        return mBlock.x(object);
    }

    private void Z(Object object, Object object2, Object object3) {
        this.H.invokeVoid(object, object2, object3);
    }

    private Object x(Object object) {
        return this.g.getObject(object);
    }

    private float T(Object object) {
        return this.Q.getFloat(object);
    }

    public float getPlayerRelativeBlockHardness(Object block, Object player, Object world, Object blockPos) {
        return this.playerRelativeBlockHardnessMethod.invokeFloat(block, player, world, blockPos);
    }
}

