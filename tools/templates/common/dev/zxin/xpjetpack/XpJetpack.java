package dev.zxin.xpjetpack;

//#if MC>=12111
// MC 1.21.11 / 26.x 起：Mojang 统一重命名为 Identifier
import net.minecraft.resources.Identifier;
//#else
import net.minecraft.resources.ResourceLocation;
//#endif

public final class XpJetpack {

    public static final String MOD_ID = "xpjetpack";
    public static final String MOD_NAME = "XP Jetpack";
    public static final String VERSION = "1.0.0";

    private XpJetpack() {
    }

//#if MC>=12111
    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
//#elif MC>=12100
    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
//#else
    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
//#endif
}
