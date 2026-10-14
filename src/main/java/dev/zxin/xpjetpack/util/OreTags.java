package dev.zxin.xpjetpack.util;

import dev.zxin.xpjetpack.feature.FeatureConfig;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * 矿石识别：不硬编码方块清单，走「标签 + 命名兜底」。
 *
 * <p>判定顺序：黑名单 → 标签（{@code c:ores} / {@code forge:ores} / {@code minecraft:ores} 等，可配置）
 * → 名称以 {@code _ore} 结尾兜底（可关）。全部走配置，未来加矿不用改代码。
 */
public final class OreTags {

    /** 名称兜底时额外认的方块（原版下界合金矿不叫 _ore）。 */
    private static final String[] EXTRA_NAMES = {"ancient_debris"};

    private static List<TagKey<Block>> tagCache = new ArrayList<TagKey<Block>>();
    private static String tagSpec = null;
    private static List<String> blacklist = new ArrayList<String>();
    private static String blacklistSpec = null;
    private static boolean nameFallback = true;

    private OreTags() {
    }

    /** 标签 id → TagKey，跨版本写在同一个分支里。 */
    private static TagKey<Block> blockTag(String id) {

        Identifier rl = Identifier.tryParse(id);
        return rl == null ? null : TagKey.create(Registries.BLOCK, rl);

    }

    /** 方块 → 注册 id 字符串。 */
    public static String idOf(BlockState state) {
        if (state == null) {
            return "";
        }
        try {

            Identifier rl = BuiltInRegistries.BLOCK.getKey(state.getBlock());

            return rl == null ? "" : rl.toString();
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static void refresh() {
        String spec = FeatureConfig.str("autosmelt.oreTags", "c:ores,forge:ores,minecraft:ores");
        String bl = FeatureConfig.str("autosmelt.blacklist", "");
        nameFallback = FeatureConfig.bool("autosmelt.nameFallback", true);
        if (!spec.equals(tagSpec)) {
            tagSpec = spec;
            List<TagKey<Block>> list = new ArrayList<TagKey<Block>>();
            String[] parts = spec.split(",");
            for (int i = 0; i < parts.length; i++) {
                String id = parts[i].trim();
                if (id.isEmpty()) {
                    continue;
                }
                TagKey<Block> key = blockTag(id);
                if (key != null) {
                    list.add(key);
                }
            }
            tagCache = list;
        }
        if (!bl.equals(blacklistSpec)) {
            blacklistSpec = bl;
            List<String> list = new ArrayList<String>();
            String[] parts = bl.split(",");
            for (int i = 0; i < parts.length; i++) {
                String id = parts[i].trim();
                if (!id.isEmpty()) {
                    list.add(id);
                }
            }
            blacklist = list;
        }
    }

    /** 是否为矿石。优先用方块状态走标签，状态缺失时退回注册 id 命名判断。 */
    public static boolean isOre(String stateId, BlockState state) {
        refresh();
        String id = state != null ? idOf(state) : "";
        if (id.isEmpty()) {
            id = stateId == null ? "" : stateId;
        }
        if (id.isEmpty()) {
            return false;
        }
        for (int i = 0; i < blacklist.size(); i++) {
            if (id.equals(blacklist.get(i))) {
                return false;
            }
        }
        if (state != null) {
            for (int i = 0; i < tagCache.size(); i++) {
                try {
                    if (state.is(tagCache.get(i))) {
                        return true;
                    }
                } catch (Throwable ignored) {
                    // 单个标签查询失败（比如该标签在本版本不存在）不影响其它标签
                }
            }
        }
        if (nameFallback) {
            String path = id;
            int colon = path.indexOf(':');
            if (colon >= 0) {
                path = path.substring(colon + 1);
            }
            if (path.endsWith("_ore")) {
                return true;
            }
            for (int i = 0; i < EXTRA_NAMES.length; i++) {
                if (path.equals(EXTRA_NAMES[i])) {
                    return true;
                }
            }
        }
        return false;
    }
}
