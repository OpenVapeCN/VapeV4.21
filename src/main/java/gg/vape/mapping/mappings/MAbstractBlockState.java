package gg.vape.mapping.mappings;

import gg.vape.Vape;
import gg.vape.mapping.MappedClasses;
import gg.vape.mapping.Mapping;
import gg.vape.mapping.MappingMethod;
import gg.vape.utils.datas.BlockData;
import gg.vape.wrapper.Wrapper;
import gg.vape.wrapper.impl.ForgeVersion;

public class MAbstractBlockState
extends Mapping {
    private MappingMethod getShapeMethod;
    private MappingMethod isSuffocatingMethod;
    private MappingMethod destroyProgressMethod;

    public MAbstractBlockState() {
        this(BlockData.W());
    }

    private MAbstractBlockState(String[] controlFlowState) {
        super(MappedClasses.Fj);
        String destroyProgressMethodName;
        if (ForgeVersion.MC_1_17.v()) {
            destroyProgressMethodName = "getPlayerRelativeBlockHardness";
        } else if (Vape.INSTANCE.isVanillaMinecraftPresent()) {
            destroyProgressMethodName = "getDestroyProgress";
        } else {
            destroyProgressMethodName = "m_60625_";
        }
        this.destroyProgressMethod = this.Y(destroyProgressMethodName, true, Float.TYPE,
                MappedClasses.Yl, MappedClasses.zJ, MappedClasses.lf);
        if (controlFlowState != null) {
            if (ForgeVersion.MC_1_20_6.d()) {
                this.getShapeMethod = this.Y("getShape", true, MappedClasses.la, new Class[]{MappedClasses.zJ, MappedClasses.lf});
            } else {
                this.getShapeMethod = this.Y("func_196954_c", Wrapper.isNativeAvailable, MappedClasses.la, new Class[]{MappedClasses.zJ, MappedClasses.lf});
                this.isSuffocatingMethod = this.Y("func_229980_m_", Wrapper.isNativeAvailable, Boolean.TYPE, new Class[]{MappedClasses.zJ, MappedClasses.lf});
            }
            return;
        }
        this.isSuffocatingMethod = this.Y("func_229980_m_", Wrapper.isNativeAvailable, Boolean.TYPE, new Class[]{MappedClasses.zJ, MappedClasses.lf});
    }

    public Object getShape(Object blockState, Object blockReader, Object blockPosition) {
        return this.getShapeMethod.invokeObject(blockState, blockReader, blockPosition);
    }

    public boolean isSuffocating(Object blockState, Object blockReader, Object blockPosition) {
        return this.isSuffocatingMethod.invokeBoolean(blockState, blockReader, blockPosition);
    }

    public float getDestroyProgress(Object blockState, Object player, Object blockReader, Object blockPosition) {
        return this.destroyProgressMethod.invokeFloat(blockState, player, blockReader, blockPosition);
    }

}
