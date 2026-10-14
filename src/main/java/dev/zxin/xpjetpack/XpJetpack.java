package dev.zxin.xpjetpack;

// MC 1.21.11 / 26.x 起：Mojang 统一重命名为 Identifier
import net.minecraft.resources.Identifier;

public final class XpJetpack {

    public static final String MOD_ID = "xpjetpack";
    public static final String MOD_NAME = "XP Jetpack";
    public static final String VERSION = "1.0.0";

    private XpJetpack() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

}
