package gg.vape.mapping.mappings;

import com.google.common.collect.ImmutableMap;
import gg.vape.mapping.MappedClasses;
import gg.vape.mapping.Mapping;
import gg.vape.mapping.MappingField;
import gg.vape.mapping.MappingMethod;
import gg.vape.wrapper.impl.ForgeVersion;

public class MIBlockState
extends Mapping {
    private MappingField g;
    private MappingField o;
    private MappingMethod c;
    private static int[] M;
    private MappingMethod u;
    public final MappingMethod O;
    private MappingField N;
    private MappingMethod X;
    private MappingMethod l;
    private MappingField B;
    private MappingMethod playerRelativeBlockHardnessMethod;

    private Object t(Object object) {
        return this.X.invokeObject(object, new Object[0]);
    }

    public static int[] v() {
        return M;
    }

    static {
        MIBlockState.W(null);
    }

    public boolean e(Object object) {
        return this.o.getBoolean(object);
    }

    public boolean I(Object object) {
        return this.B.getBoolean(object);
    }


    public static Object j(MIBlockState mIBlockState, Object object) {
        return mIBlockState.t(object);
    }

    public boolean d(Object object) {
        return this.N.getBoolean(object);
    }

    public boolean W(Object object) {
        return this.l.invokeBoolean(object, new Object[0]);
    }

    public MIBlockState() {
        this(MIBlockState.v());
    }

    private MIBlockState(int[] nArray) {
        super(MappedClasses.Vv);
        Class[] classArray = new Class[]{};
        Class clazz = MappedClasses.Zk;
        boolean bl = true;
        String string = "getBlock";
        MIBlockState mIBlockState = this;
        this.O = this.Y(string, bl, clazz, classArray);
        int[] nArray2 = nArray;
        if (ForgeVersion.MC_1_12_2.d() && ForgeVersion.MC_1_16_5.v()) {
            this.playerRelativeBlockHardnessMethod = this.Y("getPlayerRelativeBlockHardness", true, Float.TYPE,
                    MappedClasses.Yl, MappedClasses.YU, MappedClasses.lf);
        }
        if (ForgeVersion.MC_1_20_6.d()) {
            Class[] classArray2 = new Class[]{MappedClasses.FN};
            Class<Comparable> clazz2 = Comparable.class;
            boolean bl2 = true;
            String string2 = "getValue";
            Class clazz3 = MappedClasses.FX;
            MIBlockState mIBlockState2 = this;
            this.u = this.registerInstanceMethodForOwner(clazz3, string2, bl2, clazz2, classArray2);
            Class[] classArray3 = new Class[]{};
            Class<Boolean> clazz4 = Boolean.TYPE;
            boolean bl3 = true;
            String string3 = "blocksMotion";
            MIBlockState mIBlockState3 = this;
            this.l = this.Y(string3, bl3, clazz4, classArray3);
            Class<Boolean> clazz5 = Boolean.TYPE;
            boolean bl4 = true;
            String string4 = "liquid";
            MIBlockState mIBlockState4 = this;
            this.o = this.J(string4, bl4, clazz5);
            Class<Boolean> clazz6 = Boolean.TYPE;
            boolean bl5 = true;
            String string5 = "replaceable";
            MIBlockState mIBlockState5 = this;
            this.N = this.J(string5, bl5, clazz6);
            Class<Boolean> clazz7 = Boolean.TYPE;
            boolean bl6 = true;
            String string6 = "legacySolid";
            MIBlockState mIBlockState6 = this;
            this.B = this.J(string6, bl6, clazz7);
        } else if (ForgeVersion.MC_1_16_5.d()) {
            Class<ImmutableMap> clazz8 = ImmutableMap.class;
            boolean bl7 = true;
            String string7 = "properties";
            Class clazz9 = MappedClasses.FX;
            MIBlockState mIBlockState7 = this;
            this.g = this.registerInstanceFieldForOwner(clazz9, string7, bl7, clazz8);
            Class[] classArray4 = new Class[]{};
            Class clazz10 = MappedClasses.Dw;
            boolean bl8 = true;
            String string8 = "getFluidState";
            Class clazz11 = MappedClasses.Fj;
            MIBlockState mIBlockState8 = this;
            this.X = this.registerInstanceMethodForOwner(clazz11, string8, bl8, clazz10, classArray4);
        } else {
            Class[] classArray5 = new Class[]{};
            Class<ImmutableMap> clazz12 = ImmutableMap.class;
            boolean bl9 = true;
            String string9 = "getProperties";
            MIBlockState mIBlockState9 = this;
            this.c = this.Y(string9, bl9, clazz12, classArray5);
        }
    }

    public Object v(Object object) {
        return this.O.invokeObject(object, new Object[0]);
    }

    public ImmutableMap w(Object object) {
        if (ForgeVersion.MC_1_16_5.d()) {
            return (ImmutableMap)this.g.getObject(object);
        }
        return (ImmutableMap)this.c.invokeObject(object, new Object[0]);
    }

    public static void W(int[] nArray) {
        M = nArray;
    }

    public float getPlayerRelativeBlockHardness(Object blockState, Object player, Object world, Object blockPos) {
        return this.playerRelativeBlockHardnessMethod.invokeFloat(blockState, player, world, blockPos);
    }

    public Object o(Object object, Object object2) {
        return this.u.invokeObject(object, object2);
    }
}

