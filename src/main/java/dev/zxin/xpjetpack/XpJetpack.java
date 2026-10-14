package dev.zxin.xpjetpack;

import net.minecraft.resources.ResourceLocation;

public final class XpJetpack {

    public static final String MOD_ID = "xpjetpack";
    public static final String MOD_NAME = "XP Jetpack";
    public static final String VERSION = "1.0.0";

    private XpJetpack() {
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

}
