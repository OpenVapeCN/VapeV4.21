package gg.vape.wrapper.impl;

import gg.vape.Vape;
import gg.vape.mapping.MappedClasses;
import gg.vape.mapping.mappings.MEntity;
import gg.vape.utils.MathUtil;
import gg.vape.wrapper.Wrapper;

import java.util.Random;
import java.util.UUID;

public class Entity
extends Wrapper {
    private static String j;
    public static Entity d;

    public float b() {
        return MEntity.j(Entity.vapeInstance.getMappings().Rr, this.I);
    }

    public void R(boolean bl) {
        MEntity.U(Entity.vapeInstance.getMappings().Rr, this.I, bl);
    }

    public BlockPos x$src$Lgg_vape_wrapper_impl_BlockPos_$1izbb73() {
        return new BlockPos(MEntity.M(Entity.vapeInstance.getMappings().Rr, this.I));
    }

    public void p(boolean bl) {
        if (ForgeVersion.MC_1_21_4.d()) {
            MEntity.K(Entity.vapeInstance.getMappings().Rr, this.I, bl);
        }
    }

    public void s(double d) {
        MEntity.A(Entity.vapeInstance.getMappings().Rr, this.I, d);
    }

    public double t() {
        if (ForgeVersion.MC_1_16_5.d()) {
            return this.C$src$Lgg_vape_wrapper_impl_Vec3_$1q93kwi().getX();
        }
        return MEntity.W(Entity.vapeInstance.getMappings().Rr, this.I);
    }

    public boolean x$src$Z$g2peg2() {
        return MEntity.j$src$Z$1z0qoap(Entity.vapeInstance.getMappings().Rr, this.I);
    }

    public boolean M$src$Z$ff28xj() {
        return MEntity.c$src$Z$1x2cbcq(Entity.vapeInstance.getMappings().Rr, this.I);
    }

    public float f$src$F$fst3ac() {
        if (ForgeVersion.MC_1_16_5.d()) {
            return this.H$src$Lgg_vape_wrapper_impl_EntitySize_$16z3n5o().c();
        }
        return MEntity.h(Entity.vapeInstance.getMappings().Rr, this.I);
    }

    public boolean b$src$Z$fqlxe4() {
        return MEntity.W$src$Z$ez5dgu(Entity.vapeInstance.getMappings().Rr, this.I);
    }

    public boolean r() {
        return MEntity.D$src$Z$tyo4m3(Entity.vapeInstance.getMappings().Rr, this.I);
    }

    public void k(double d) {
        if (ForgeVersion.MC_1_16_5.d()) {
            this.C$src$Lgg_vape_wrapper_impl_Vec3_$1q93kwi().m(d);
            return;
        }
        MEntity.q(Entity.vapeInstance.getMappings().Rr, this.I, d);
    }

    public void O$src$V$fg5u0t() {
        if (ForgeVersion.MC_1_21_4.d()) {
            MEntity.C$src$V$jjb7j2(Entity.vapeInstance.getMappings().Rr, this.I);
        }
    }

    public void D(float f) {
        MEntity.p(Entity.vapeInstance.getMappings().Rr, this.I, f);
    }

    public boolean d(Material material) {
        if (ForgeVersion.MC_1_20_6.d()) {
            return false;
        }
        if (ForgeVersion.MC_1_16_5.d()) {
            if (this.S$src$Lgg_vape_wrapper_impl_Entity_$dgzs12().isInstance(MappedClasses.Vn)) {
                return false;
            }
            double d = this.N() + (double)this.X();
            BlockPos blockPos = BlockPos.create((int)this.z(), (int)d, (int)this.h());
            BlockState blockState = Minecraft.theWorld().getBlockState(blockPos);
            return blockState.getBlock().H() == material;
        }
        return MEntity.v(Entity.vapeInstance.getMappings().Rr, this.I, material.getObject());
    }

    public boolean Q$src$Z$fh9faz() {
        return MEntity.s$src$Z$msqt8q(Entity.vapeInstance.getMappings().Rr, this.I);
    }

    public Vec3 G$src$Lgg_vape_wrapper_impl_Vec3_$efeys6() {
        if (ForgeVersion.MC_1_21_4.d()) {
            return new Vec3(MEntity.t(Entity.vapeInstance.getMappings().Rr, this.I));
        }
        throw new UnsupportedOperationException("getStuckSpeedMultiplier is not supported in this version");
    }

    public float D() {
        return MEntity.U(Entity.vapeInstance.getMappings().Rr, this.I);
    }

    public void L(double d, boolean bl, Block block, BlockPos blockPos) {
        if (ForgeVersion.MC_1_12_2.d()) {
            WorldClient worldClient = Minecraft.theWorld();
            if (worldClient.isNull()) {
                return;
            }
            if (ForgeVersion.MC_1_16_5.d()) {
                BlockStatePredicate blockStatePredicate = Minecraft.m$src$Lgg_vape_wrapper_impl_EntityRenderer_$13begmf().l().x$src$Lgg_vape_wrapper_impl_BlockReader_$120g8sh().f(blockPos);
                MEntity.G(Entity.vapeInstance.getMappings().Rr, this.I, d, bl, blockStatePredicate.getObject(), blockPos.getObject());
            } else {
                MEntity.q(Entity.vapeInstance.getMappings().Rr, this.I, d, bl, worldClient.getBlockState(blockPos).getObject(), blockPos.getObject());
            }
        } else {
            MEntity.O(Entity.vapeInstance.getMappings().Rr, this.I, d, bl, block.getObject(), blockPos.getObject());
        }
    }

    public boolean t$src$Z$g0i82m() {
        return MEntity.U$src$Z$1t5jlgs(Entity.vapeInstance.getMappings().Rr, this.I);
    }

    public void r(MoverType moverType, double d, double d2, double d3) {
        if (ForgeVersion.MC_1_16_5.d()) {
            this.B(moverType, Vec3.create(d, d2, d3));
            return;
        }
        MEntity.x(Entity.vapeInstance.getMappings().Rr, this.I, moverType.getObject(), d, d2, d3);
    }

    public void t(double d, double d2, double d3, float f, float f2) {
        MEntity.m(Entity.vapeInstance.getMappings().Rr, this.I, d, d2, d3, f, f2);
    }

    public float Y() {
        if (ForgeVersion.MC_1_16_5.d()) {
            return this.H$src$Lgg_vape_wrapper_impl_EntitySize_$16z3n5o().u();
        }
        return MEntity.W$src$F$ez5czm(Entity.vapeInstance.getMappings().Rr, this.I);
    }

    public void q(double d, boolean bl) {
        MEntity.k(Entity.vapeInstance.getMappings().Rr, this.I, d, bl);
    }

    public void M(Entity entity, boolean bl) {
        MEntity.t(Entity.vapeInstance.getMappings().Rr, this.I, entity.getObject(), bl);
    }

    public void u(double d) {
        if (ForgeVersion.MC_1_16_5.d()) {
            this.I$src$Lgg_vape_wrapper_impl_Vec3_$q14opk().m(d);
            return;
        }
        MEntity.i(Entity.vapeInstance.getMappings().Rr, this.I, d);
    }

    public UUID X$src$Ljava_util_UUID_$1o5dyg6() {
        return MEntity.v$src$Ljava_util_UUID_$1bst88r(Entity.vapeInstance.getMappings().Rr, this.I);
    }

    public void F(double d, double d2, double d3) {
        this.h(Vec3.create(d, d2, d3));
    }

    public boolean i$src$Z$avhpwd(double d, double d2, double d3) {
        return MEntity.A(Entity.vapeInstance.getMappings().Rr, this.I, d, d2, d3);
    }

    public int S() {
        return MEntity.m$src$I$v9pcvp(Entity.vapeInstance.getMappings().Rr, this.I);
    }

    public void K(float f) {
        if (ForgeVersion.MC_1_20_6.d()) {
            Vape.notifyNativeStackTrace();
        }
        MEntity.r(Entity.vapeInstance.getMappings().Rr, this.I, f);
    }

    public void k() {
        MEntity.n(Entity.vapeInstance.getMappings().Rr, this.I);
    }

    public void k(int n, boolean bl) {
        MEntity.G(Entity.vapeInstance.getMappings().Rr, this.I, n, bl);
    }

    public int V$src$I$fk0dv5() {
        return MEntity.X(Entity.vapeInstance.getMappings().Rr, this.I);
    }

    public float V() {
        return MEntity.r(Entity.vapeInstance.getMappings().Rr, this.I);
    }

    public double z() {
        if (ForgeVersion.MC_1_16_5.d()) {
            return this.I$src$Lgg_vape_wrapper_impl_Vec3_$q14opk().getX();
        }
        return MEntity.p(Entity.vapeInstance.getMappings().Rr, this.I);
    }

    public double i(double d, double d2, double d3) {
        double d4 = this.z() - d;
        double d5 = this.N() - d2;
        double d6 = this.h() - d3;
        return MathUtil.sqrt(d4 * d4 + d5 * d5 + d6 * d6);
    }

    public boolean Z$src$Z$fm7kn8() {
        if (ForgeVersion.MC_1_21_4.d()) {
            return MEntity.E$src$Z$14e11lo(Entity.vapeInstance.getMappings().Rr, this.I);
        }
        return false;
    }

    public double h() {
        if (ForgeVersion.MC_1_16_5.d()) {
            return this.I$src$Lgg_vape_wrapper_impl_Vec3_$q14opk().getZ();
        }
        return MEntity.q(Entity.vapeInstance.getMappings().Rr, this.I);
    }

    public void K(boolean bl) {
        if (!ForgeVersion.MC_1_21_4.d()) {
            throw new UnsupportedOperationException("setMinorHorizontalCollision is not supported in this version");
        }
        MEntity.i(Entity.vapeInstance.getMappings().Rr, this.I, bl);
    }

    public boolean H$src$Z$fcb9yq() {
        return MEntity.L(Entity.vapeInstance.getMappings().Rr, this.I);
    }

    public void g(boolean bl) {
        if (ForgeVersion.MC_1_21_4.d()) {
            MEntity.I(Entity.vapeInstance.getMappings().Rr, this.I, bl);
        }
    }

    public Vec3 E$src$Lgg_vape_wrapper_impl_Vec3_$2tp8us() {
        return new Vec3(MEntity.R$src$Ljava_lang_Object_$19zdfx3(Entity.vapeInstance.getMappings().Rr, this.I));
    }

    public boolean D$src$Z$fa43la() {
        return MEntity.n$src$Z$15p2a9x(Entity.vapeInstance.getMappings().Rr, this.I);
    }

    public void D(AxisAlignedBB axisAlignedBB) {
        MEntity.u(Entity.vapeInstance.getMappings().Rr, this.I, axisAlignedBB.getObject());
    }

    public float c$src$F$fr5pi9() {
        return MEntity.K(Entity.vapeInstance.getMappings().Rr, this.I);
    }

    public boolean n$src$Z$fx7gig() {
        return MEntity.a(Entity.vapeInstance.getMappings().Rr, this.I);
    }

    public void C(double d) {
        MEntity.E(Entity.vapeInstance.getMappings().Rr, this.I, d);
    }

    public boolean G$src$Z$fbrhdd() {
        return MEntity.o$src$Z$1g4f79i(Entity.vapeInstance.getMappings().Rr, this.I);
    }

    public void t(boolean bl) {
        MEntity.D(Entity.vapeInstance.getMappings().Rr, this.I, bl);
    }

    public double H() {
        return MEntity.C(Entity.vapeInstance.getMappings().Rr, this.I);
    }

    public void l$src$V$fw3v8a() {
        MEntity.O(Entity.vapeInstance.getMappings().Rr, this.I);
    }

    public boolean d() {
        return MEntity.O$src$Z$2le3ja(Entity.vapeInstance.getMappings().Rr, this.I);
    }

    public boolean U$src$Z$fjglof() {
        return MEntity.r$src$Z$cddw95(Entity.vapeInstance.getMappings().Rr, this.I);
    }

    public AxisAlignedBB R$src$Lgg_vape_wrapper_impl_AxisAlignedBB_$r19dfl() {
        return new AxisAlignedBB(MEntity.Y$src$Ljava_lang_Object_$qb2bao(Entity.vapeInstance.getMappings().Rr, this.I));
    }

    public Vec3 C$src$Lgg_vape_wrapper_impl_Vec3_$1q93kwi() {
        return new Vec3(MEntity.P$src$Ljava_lang_Object_$vb56e1(Entity.vapeInstance.getMappings().Rr, this.I));
    }

    public void l(double d) {
        if (ForgeVersion.MC_1_16_5.d()) {
            this.I$src$Lgg_vape_wrapper_impl_Vec3_$q14opk().Z(d);
            return;
        }
        MEntity.n(Entity.vapeInstance.getMappings().Rr, this.I, d);
    }

    public void z(boolean bl) {
        MEntity.N(Entity.vapeInstance.getMappings().Rr, this.I, bl);
    }

    public void n() {
        MEntity.N(Entity.vapeInstance.getMappings().Rr, this.I);
    }

    public void i(float f) {
        Entity.vapeInstance.getMappings().Rr.k(this.I, f);
    }

    public double Z() {
        float f = Minecraft.getTimer().renderPartialTicks();
        return this.m$src$D$fwnne5() + (this.h() - this.m$src$D$fwnne5()) * (double)f;
    }

    public float getDistanceToEntity(Entity entity) {
        return MEntity.Q(Entity.vapeInstance.getMappings().Rr, this.I, entity.I);
    }

    public int l() {
        return MEntity.s(Entity.vapeInstance.getMappings().Rr, this.I);
    }

    public void T(Entity entity) {
        MEntity.d(Entity.vapeInstance.getMappings().Rr, this.I, entity.getObject());
    }

    public void C(float f) {
        MEntity.Q(Entity.vapeInstance.getMappings().Rr, this.I, f);
    }

    public boolean v$src$Z$g1lt9c() {
        return MEntity.l$src$Z$kucgar(Entity.vapeInstance.getMappings().Rr, this.I);
    }

    public double U() {
        double d = this.R$src$Lgg_vape_wrapper_impl_AxisAlignedBB_$r19dfl().getMinY() + (double)this.X();
        if (ForgeVersion.MC_1_7_10.L() && this.isInstance(MappedClasses.z5)) {
            d = this.N();
        }
        return d;
    }

    public boolean I$src$Z$fcv2k3() {
        return MEntity.q$src$Z$1y0z9k(Entity.vapeInstance.getMappings().Rr, this.I);
    }

    public Entity(Object object) {
        super(object);
    }

    public double R() {
        return MEntity.i(Entity.vapeInstance.getMappings().Rr, this.I);
    }

    public Entity S$src$Lgg_vape_wrapper_impl_Entity_$dgzs12() {
        return new Entity(MEntity.T(Entity.vapeInstance.getMappings().Rr, this.I));
    }

    public void b(double d, double d2, double d3) {
        if (ForgeVersion.MC_1_12_2.d()) {
            this.r(MoverType.X(), d, d2, d3);
            return;
        }
        MEntity.c(Entity.vapeInstance.getMappings().Rr, this.I, d, d2, d3);
    }

    public void B(MoverType moverType, Vec3 vec3) {
        MEntity.k(Entity.vapeInstance.getMappings().Rr, this.I, moverType.getObject(), vec3.getObject());
    }

    public void w(double d) {
        MEntity.h(Entity.vapeInstance.getMappings().Rr, this.I, d);
    }

    public double M() {
        return MEntity.k(Entity.vapeInstance.getMappings().Rr, this.I);
    }

    public EnumFacing J$src$Lgg_vape_wrapper_impl_EnumFacing_$10aeq5x() {
        return EnumFacing.E(MathUtil.floor((double)(this.J() * 4.0f / 360.0f) + 0.5) & 3);
    }

    public void H(float f) {
        MEntity.W(Entity.vapeInstance.getMappings().Rr, this.I, f);
    }

    public void U(BlockStatePredicate blockStatePredicate) {
        Object object = blockStatePredicate == null ? null : blockStatePredicate.getObject();
        MEntity.Q$src$V$1g5b9e8(Entity.vapeInstance.getMappings().Rr, this.I, object);
    }

    public void y(UUID uUID) {
        MEntity.f(Entity.vapeInstance.getMappings().Rr, this.I, uUID);
    }

    public void l(float f) {
        MEntity.E(Entity.vapeInstance.getMappings().Rr, this.I, f);
    }

    public void E(double d, double d2, double d3) {
        if (ForgeVersion.MC_1_21_4.d()) {
            MEntity.W$src$V$1v5iu3a(Entity.vapeInstance.getMappings().Rr, this.I, Vec3.create(d, d2, d3).getObject());
        } else {
            MEntity.s(Entity.vapeInstance.getMappings().Rr, this.I, d, d2, d3);
        }
    }

    public void h(Vec3 vec3) {
        MEntity.i(Entity.vapeInstance.getMappings().Rr, this.I, vec3.getObject());
    }

    public void F(boolean bl) {
        MEntity.a(Entity.vapeInstance.getMappings().Rr, this.I, bl);
    }

    public void Z(Vec3 vec3) {
        if (!ForgeVersion.MC_1_21_4.d()) {
            throw new UnsupportedOperationException("setStuckSpeedMultiplier is not supported in this version");
        }
        MEntity.E(Entity.vapeInstance.getMappings().Rr, this.I, vec3.getObject());
    }

    public boolean C$src$Z$f9kazx() {
        if (Vape.INSTANCE.isForgeAbsent()) {
            return false;
        }
        return MEntity.m$src$Z$v9pdac(Entity.vapeInstance.getMappings().Rr, this.I);
    }

    public boolean u$src$Z$g120nz() {
        return MEntity.V(Entity.vapeInstance.getMappings().Rr, this.I);
    }

    public double g(Vec3 vec3) {
        double d = this.z() - vec3.getX();
        double d2 = this.N() - vec3.getY();
        double d3 = this.h() - vec3.getZ();
        return d * d + d2 * d2 + d3 * d3;
    }

    public void q(boolean bl) {
        MEntity.J(Entity.vapeInstance.getMappings().Rr, this.I, bl);
    }

    public BlockStatePredicate y() {
        return new BlockStatePredicate(MEntity.X$src$Ljava_lang_Object_$iyy6j5(Entity.vapeInstance.getMappings().Rr, this.I));
    }

    public void r(double d) {
        if (ForgeVersion.MC_1_16_5.d()) {
            this.C$src$Lgg_vape_wrapper_impl_Vec3_$1q93kwi().N(d);
            return;
        }
        MEntity.t(Entity.vapeInstance.getMappings().Rr, this.I, d);
    }

    public double Z(Object object) {
        return MEntity.Y(Entity.vapeInstance.getMappings().Rr, this.I, object);
    }

    public double f() {
        return MEntity.u(Entity.vapeInstance.getMappings().Rr, this.I);
    }

    public double c() {
        float f = Minecraft.getTimer().renderPartialTicks();
        return this.M() + (this.z() - this.M()) * (double)f;
    }

    public void A$src$V$f8gppr() {
        if (ForgeVersion.MC_1_21_4.d()) {
            MEntity.h$src$V$1e60u83(Entity.vapeInstance.getMappings().Rr, this.I);
        }
    }

    public static String v$src$Ljava_lang_String_$16ys7iq() {
        return j;
    }

    private Vec3 getPositionCodecBase() {
        if (ForgeVersion.MC_1_20_6.d()) {
            return new GameSettingsGuiScale(MEntity.U$src$Ljava_lang_Object_$1vzpu7o(Entity.vapeInstance.getMappings().Rr, this.I)).getBase();
        }
        return new Vec3(MEntity.U$src$Ljava_lang_Object_$1vzpu7o(Entity.vapeInstance.getMappings().Rr, this.I));
    }

    public float M$src$F$ff28gb() {
        return MEntity.m(Entity.vapeInstance.getMappings().Rr, this.I);
    }

    public float getFallDistance() {
        return this.M$src$F$ff28gb();
    }

    public void B(double d, double d2, double d3) {
        MEntity.y(Entity.vapeInstance.getMappings().Rr, this.I, d, d2, d3);
    }

    public void B(float f) {
        MEntity.O(Entity.vapeInstance.getMappings().Rr, this.I, f);
    }

    public float G() {
        return MEntity.H(Entity.vapeInstance.getMappings().Rr, this.I);
    }

    public float X() {
        return MEntity.y(Entity.vapeInstance.getMappings().Rr, this.I);
    }

    public boolean m$src$Z$fwnnx3() {
        if (ForgeVersion.MC_1_21_4.d()) {
            return MEntity.u$src$Z$17ngn7w(Entity.vapeInstance.getMappings().Rr, this.I);
        }
        return false;
    }

    public Vec3 k(float f) {
        return new Vec3(MEntity.L(Entity.vapeInstance.getMappings().Rr, this.I, f));
    }

    public boolean f$src$Z$fst3rk() {
        if (ForgeVersion.MC_1_16_5.d()) {
            return MEntity.T(Entity.vapeInstance.getMappings().Rr, this.I) != null;
        }
        return MEntity.T$src$Z$1iq6oh7(Entity.vapeInstance.getMappings().Rr, this.I);
    }

    public static void q(String string) {
        j = string;
    }

    public double W() {
        return MEntity.Y(Entity.vapeInstance.getMappings().Rr, this.I);
    }

    public String getName() {
        if (ForgeVersion.MC_1_16_5.d()) {
            ITextComponent iTextComponent = new ITextComponent(MEntity.W$src$Ljava_lang_Object_$bmu1rm(Entity.vapeInstance.getMappings().Rr, this.I));
            return iTextComponent.getFormattedText();
        }
        return MEntity.z$src$Ljava_lang_String_$ch83r5(Entity.vapeInstance.getMappings().Rr, this.I);
    }

    public boolean P() {
        return MEntity.R$src$Z$xvgui1(Entity.vapeInstance.getMappings().Rr, this.I);
    }

    public void n(double d) {
        MEntity.K(Entity.vapeInstance.getMappings().Rr, this.I, d);
    }

    public void Q(int n) {
        MEntity.h(Entity.vapeInstance.getMappings().Rr, this.I, n);
    }

    public float J() {
        return MEntity.l$src$F$kucftj(Entity.vapeInstance.getMappings().Rr, this.I);
    }

    public void J(boolean bl) {
        MEntity.G(Entity.vapeInstance.getMappings().Rr, this.I, bl);
    }

    public ITextComponent Q() {
        return new ITextComponent(MEntity.c$src$Ljava_lang_Object_$sn3kyu(Entity.vapeInstance.getMappings().Rr, this.I));
    }

    public World getWorld() {
        return new World(MEntity.S$src$Ljava_lang_Object_$1hbhkom(Entity.vapeInstance.getMappings().Rr, this.I));
    }

    public void H(double d) {
        if (ForgeVersion.MC_1_16_5.d()) {
            this.I$src$Lgg_vape_wrapper_impl_Vec3_$q14opk().N(d);
            return;
        }
        MEntity.m(Entity.vapeInstance.getMappings().Rr, this.I, d);
    }

    public double N() {
        if (ForgeVersion.MC_1_16_5.d()) {
            return this.I$src$Lgg_vape_wrapper_impl_Vec3_$q14opk().getY();
        }
        return MEntity.E(Entity.vapeInstance.getMappings().Rr, this.I);
    }

    public boolean q$src$Z$fyuuaj() {
        if (ForgeVersion.MC_1_21_4.d()) {
            return MEntity.e(Entity.vapeInstance.getMappings().Rr, this.I);
        }
        return false;
    }

    public boolean c$src$Z$fr5pzh() {
        return MEntity.I$src$Z$b2cnkw(Entity.vapeInstance.getMappings().Rr, this.I);
    }

    public Entity q$src$Lgg_vape_wrapper_impl_Entity_$1ujnemc() {
        return new Entity(MEntity.K$src$Ljava_lang_Object_$1tnokji(Entity.vapeInstance.getMappings().Rr, this.I));
    }

    public void J(float f, float f2) {
        MEntity.P(Entity.vapeInstance.getMappings().Rr, this.I, f, f2);
    }

    public double m$src$D$fwnne5() {
        return MEntity.D(Entity.vapeInstance.getMappings().Rr, this.I);
    }

    public Random Q$src$Ljava_util_Random_$186dqr9() {
        return MEntity.M$src$Ljava_util_Random_$1y5810s(Entity.vapeInstance.getMappings().Rr, this.I);
    }

    public void A(double d) {
        MEntity.a(Entity.vapeInstance.getMappings().Rr, this.I, d);
    }

    public boolean O$src$Z$fg5u49() {
        return MEntity.J(Entity.vapeInstance.getMappings().Rr, this.I);
    }

    public Vec3 I$src$Lgg_vape_wrapper_impl_Vec3_$q14opk() {
        if (ForgeVersion.MC_1_16_5.d()) {
            return new Vec3(MEntity.I(Entity.vapeInstance.getMappings().Rr, this.I));
        }
        return Vec3.create(this.z(), this.N(), this.h());
    }

    public long E$src$J$fanvsv() {
        if (ForgeVersion.MC_1_16_5.d()) {
            return (long)this.getPositionCodecBase().getY();
        }
        return MEntity.R(Entity.vapeInstance.getMappings().Rr, this.I);
    }

    private EntitySize H$src$Lgg_vape_wrapper_impl_EntitySize_$16z3n5o() {
        return new EntitySize(MEntity.g(Entity.vapeInstance.getMappings().Rr, this.I));
    }

    public void X(boolean bl, boolean bl2, Vec3 vec3) {
        if (!ForgeVersion.MC_1_21_4.d()) {
            throw new UnsupportedOperationException("setOnGroundWithMovement is not supported in this version");
        }
        MEntity.e(Entity.vapeInstance.getMappings().Rr, this.I, bl, bl2, vec3.getObject());
    }

    public BlockPos C$src$Lgg_vape_wrapper_impl_BlockPos_$y7f4vu() {
        if (ForgeVersion.MC_1_21_4.d()) {
            return new BlockPos(MEntity.v(Entity.vapeInstance.getMappings().Rr, this.I));
        }
        return BlockPos.D(this.z(), this.N() - (double)0.2f, this.h());
    }

    public float u() {
        return MEntity.c(Entity.vapeInstance.getMappings().Rr, this.I);
    }

    public Vec3 y(Vec3 vec3) {
        if (ForgeVersion.MC_1_21_4.d()) {
            return new Vec3(MEntity.F(Entity.vapeInstance.getMappings().Rr, this.I, vec3.getObject()));
        }
        return vec3;
    }

    public void h(boolean bl) {
        if (!ForgeVersion.MC_1_21_4.d()) {
            throw new UnsupportedOperationException("setVerticalCollisionBelow is not supported in this version");
        }
        MEntity.z(Entity.vapeInstance.getMappings().Rr, this.I, bl);
    }

    public void i(float f, float f2, float f3) {
        if (ForgeVersion.MC_1_16_5.d()) {
            MEntity.h(Entity.vapeInstance.getMappings().Rr, this.I, f3, Vec3.create(f, 0.0, f2).getObject());
            return;
        }
        MEntity.V(Entity.vapeInstance.getMappings().Rr, this.I, f, 0.0f, f2, f3);
    }

    public boolean h$src$Z$ftwoya() {
        return MEntity.M$src$Z$1grsbj8(Entity.vapeInstance.getMappings().Rr, this.I);
    }

    public boolean B$src$Z$f90iek() {
        return MEntity.i$src$Z$1oldrb4(Entity.vapeInstance.getMappings().Rr, this.I);
    }

    public DamageSources B() {
        return new DamageSources(MEntity.o(Entity.vapeInstance.getMappings().Rr, this.I));
    }

    public void L(double d) {
        MEntity.Q(Entity.vapeInstance.getMappings().Rr, this.I, d);
    }

    public void U(boolean bl) {
        MEntity.A(Entity.vapeInstance.getMappings().Rr, this.I, bl);
    }

    public double q() {
        if (ForgeVersion.MC_1_16_5.d()) {
            return this.C$src$Lgg_vape_wrapper_impl_Vec3_$1q93kwi().getY();
        }
        return MEntity.P(Entity.vapeInstance.getMappings().Rr, this.I);
    }

    static {
        Entity.q("tJJV3b");
        d = new Entity(null);
    }

    public void x(float f, float f2, float f3, float f4) {
        if (ForgeVersion.MC_1_16_5.d()) {
            MEntity.h(Entity.vapeInstance.getMappings().Rr, this.I, f4, Vec3.create(f, f2, f3).getObject());
            return;
        }
        MEntity.V(Entity.vapeInstance.getMappings().Rr, this.I, f, f2, f3, f4);
    }

    public void U(float f) {
        MEntity.f(Entity.vapeInstance.getMappings().Rr, this.I, f);
    }

    public float j() {
        return MEntity.S(Entity.vapeInstance.getMappings().Rr, this.I);
    }

    public BlockPos J$src$Lgg_vape_wrapper_impl_BlockPos_$kv8a0x() {
        if (ForgeVersion.MC_1_16_5.d()) {
            return new BlockPos(MEntity.z(Entity.vapeInstance.getMappings().Rr, this.I));
        }
        return BlockPos.D(this.z(), this.N(), this.h());
    }

    public AxisAlignedBB u$src$Lgg_vape_wrapper_impl_AxisAlignedBB_$kogbsu() {
        if (ForgeVersion.MC_1_7_10.L()) {
            return this.R$src$Lgg_vape_wrapper_impl_AxisAlignedBB_$r19dfl();
        }
        return new AxisAlignedBB(MEntity.y$src$Ljava_lang_Object_$1bzedu8(Entity.vapeInstance.getMappings().Rr, this.I));
    }

    public Vec3 K(Vec3 vec3) {
        if (ForgeVersion.MC_1_21_4.d()) {
            return new Vec3(MEntity.W(Entity.vapeInstance.getMappings().Rr, this.I, vec3.getObject()));
        }
        return vec3;
    }

    public double A() {
        float f = Minecraft.getTimer().renderPartialTicks();
        return this.W() + (this.N() - this.W()) * (double)f;
    }

    public void i(float f, Vec3 vec3) {
        MEntity.h(Entity.vapeInstance.getMappings().Rr, this.I, f, vec3.getObject());
    }

    public boolean E$src$Z$fanw6n() {
        if (ForgeVersion.MC_1_21_4.d()) {
            return MEntity.P$src$Z$d0r0iv(Entity.vapeInstance.getMappings().Rr, this.I);
        }
        return false;
    }

    public void T(boolean bl) {
        MEntity.o(Entity.vapeInstance.getMappings().Rr, this.I, bl);
    }

    public boolean J$src$Z$fdev5g() {
        return MEntity.K$src$Z$vx2hk2(Entity.vapeInstance.getMappings().Rr, this.I);
    }

    private static UnsupportedOperationException a(UnsupportedOperationException unsupportedOperationException) {
        return unsupportedOperationException;
    }

    public double T() {
        if (ForgeVersion.MC_1_16_5.d()) {
            return this.C$src$Lgg_vape_wrapper_impl_Vec3_$1q93kwi().getZ();
        }
        return MEntity.l(Entity.vapeInstance.getMappings().Rr, this.I);
    }

    public double x() {
        float f = Minecraft.getTimer().renderPartialTicks();
        return this.W() + (Minecraft.thePlayer().R$src$Lgg_vape_wrapper_impl_AxisAlignedBB_$r19dfl().getMinY() - this.W()) * (double)f;
    }

    public void i(double d) {
        if (ForgeVersion.MC_1_16_5.d()) {
            this.C$src$Lgg_vape_wrapper_impl_Vec3_$1q93kwi().Z(d);
            return;
        }
        MEntity.o(Entity.vapeInstance.getMappings().Rr, this.I, d);
    }

    public World gg_vape_wrapper_impl_World_Z() {
        return this.getWorld();
    }

    public double double_N() {
        return this.N();
    }

    public double double_z() {
        return this.z();
    }

    public double double_h() {
        return this.h();
    }

    public boolean boolean_M() {
        return this.M$src$Z$ff28xj();
    }

    public boolean boolean_d() {
        return this.d();
    }

    public boolean boolean_u() {
        return this.u$src$Z$g120nz();
    }

    public float float_M() {
        return this.M$src$F$ff28gb();
    }

    public boolean boolean_h() {
        return this.h$src$Z$ftwoya();
    }

    public boolean boolean_r() {
        return this.r();
    }

    public boolean boolean_b() {
        return this.b$src$Z$fqlxe4();
    }

    public double double_t() {
        return this.t();
    }

    public double double_T() {
        return this.T();
    }
}
