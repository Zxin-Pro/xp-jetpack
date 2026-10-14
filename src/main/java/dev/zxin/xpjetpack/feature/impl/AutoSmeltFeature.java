package dev.zxin.xpjetpack.feature.impl;

import dev.zxin.xpjetpack.feature.BlockBreakInfo;
import dev.zxin.xpjetpack.feature.ConfigFacade;
import dev.zxin.xpjetpack.feature.Feature;
import dev.zxin.xpjetpack.feature.FeatureConfig;
import dev.zxin.xpjetpack.feature.FeatureContext;
import dev.zxin.xpjetpack.feature.FeatureState;
import dev.zxin.xpjetpack.util.FeatureNotifier;
import dev.zxin.xpjetpack.util.OreTags;
import dev.zxin.xpjetpack.util.XpCost;

import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import net.minecraft.world.SimpleContainer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 经验熔炉：挖矿石直接掉熔炼产物（原矿 → 锭），按块扣经验。
 *
 * <p>原版掉落物是在方块破坏「之后」才生成的，所以这里延迟一 tick 再处理：
 * 破坏时只登记坐标，下一 tick 找回附近的掉落物实体，就地替换物品与数量。
 * 这样不需要取消原版破坏（工具耐久、统计、精准采集、时运全部照旧）。
 *
 * <p>精准采集：掉落物就是矿石方块本身（例如 {@code iron_ore}），
 * 而原版熔炼配方只认原矿（{@code raw_iron}），查不到配方即自动跳过，无需额外判断附魔。
 */
public final class AutoSmeltFeature implements Feature {

    /** 待处理的一次挖掘。 */
    private static final class Pending {

        final Level level;
        final BlockPos pos;
        int age;

        Pending(Level level, BlockPos pos) {
            this.level = level;
            this.pos = pos;
        }
    }

    /** 每 tick 最多熔炼几块，避免一次性爆矿卡顿。 */
    private static final int MAX_PER_TICK = 8;
    /** 最多等几 tick 找掉落物，超时丢弃（避免残留堆积）。 */
    private static final int MAX_AGE = 3;

    private final Map<UUID, List<Pending>> pending = new HashMap<UUID, List<Pending>>();

    @Override
    public String id() {
        return "autosmelt";
    }

    @Override
    public Component displayName() {
        return Component.translatable("xpjetpack.feature.autosmelt");
    }

    @Override
    public String commandName() {
        return "autosmelt";
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    @Override
    public void declareConfig(ConfigFacade cfg) {
        cfg.bool("autosmelt.enabled", "是否开启经验熔炉（挖矿石直接出锭）", true);
        cfg.dbl("autosmelt.costPerOre", "每熔炼一块矿石消耗的经验点", 10.0D, 0.0D, 1000.0D);
        cfg.str("autosmelt.oreTags", "视为矿石的方块标签（英文逗号分隔）", "c:ores,forge:ores,minecraft:ores");
        cfg.bool("autosmelt.nameFallback", "名称以 _ore 结尾的方块也算矿石", true);
        cfg.str("autosmelt.blacklist", "永不熔炼的方块注册 id（英文逗号分隔）", "");
    }

    @Override
    public void onBlockBreak(FeatureContext ctx, BlockBreakInfo info) {
        if (info.cancelled || info.pos == null || info.level == null) {
            return;
        }
        if (!OreTags.isOre(info.stateId, info.state)) {
            return;
        }
        ServerPlayer player = ctx.player();
        if (player == null) {
            return;
        }
        List<Pending> list = pending.get(player.getUUID());
        if (list == null) {
            list = new ArrayList<Pending>();
            pending.put(player.getUUID(), list);
        }
        if (list.size() < 64) {
            list.add(new Pending(info.level, info.pos.immutable()));
        }
    }

    @Override
    public void onServerTick(FeatureContext ctx) {
        ServerPlayer player = ctx.player();
        if (player == null) {
            return;
        }
        List<Pending> list = pending.get(player.getUUID());
        if (list == null || list.isEmpty()) {
            return;
        }
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            pending.remove(player.getUUID());
            return;
        }

        FeatureState st = ctx.featureState(this);
        RecipeManager recipes = server.getRecipeManager();
        RegistryAccess regs = server.registryAccess();
        double cost = FeatureConfig.dbl("autosmelt.costPerOre", 10.0D);

        List<Pending> keep = new ArrayList<Pending>();
        int done = 0;
        for (int i = 0; i < list.size(); i++) {
            Pending p = list.get(i);
            if (done >= MAX_PER_TICK) {
                keep.add(p);
                continue;
            }
            if (handle(player, st, p, recipes, regs, cost)) {
                done++;
            } else {
                p.age++;
                if (p.age <= MAX_AGE) {
                    keep.add(p);
                }
            }
        }
        if (keep.isEmpty()) {
            pending.remove(player.getUUID());
        } else {
            pending.put(player.getUUID(), keep);
        }
    }

    /** 处理一次挖掘：找到掉落物 → 查熔炼配方 → 扣经验 → 就地替换。 */
    private boolean handle(ServerPlayer player, FeatureState st, Pending p,
                           RecipeManager recipes, RegistryAccess regs, double cost) {
        List<ItemEntity> items = p.level.getEntitiesOfClass(ItemEntity.class, new AABB(p.pos).inflate(1.6D));
        for (int i = 0; i < items.size(); i++) {
            ItemEntity ie = items.get(i);
            ItemStack stack = ie.getItem();
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack smelted = smelt(recipes, regs, p.level, stack);
            if (smelted.isEmpty()) {
                continue;
            }
            if (!XpCost.pay(player, st, cost)) {
                FeatureNotifier.notifyExhausted(player, st, "经验熔炉");
                return true;
            }
            ItemStack out = smelted.copy();
            out.setCount(smelted.getCount() * stack.getCount());
            ie.setItem(out);
            FeatureNotifier.notifyOnce(player, st, "autosmelt:" + out.getHoverName().getString(),
                    Component.literal("经验熔炉：已熔炼 " + out.getCount() + " × " + out.getHoverName().getString()));
            return true;
        }
        return false;
    }

    /**
     * 查原版熔炼产物。输入是「掉落物」（原矿），不是矿石方块本身。
     * 三个分支覆盖 1.20.1 → 26.3 的配方 API 差异。
     */
    private static ItemStack smelt(RecipeManager recipes, RegistryAccess regs, Level level, ItemStack stack) {
        if (stack.isEmpty() || level == null) {
            return ItemStack.EMPTY;
        }
        ItemStack probe = stack.copy();
        probe.setCount(1);
        try {

            // 1.20.2 - 1.20.6：配方已被 RecipeHolder 包裹，取 value()
            Container container = new SimpleContainer(probe);
            var found = recipes.getRecipeFor(RecipeType.SMELTING, container, level);
            if (found.isPresent()) {
                return found.get().value().getResultItem(regs);
            }

        } catch (Throwable ignored) {
            // 配方查询失败（异常或该版本无此配方）时静默跳过，不影响原版掉落
        }
        return ItemStack.EMPTY;
    }

    @Override
    public Component statusLine(FeatureContext ctx) {
        return Component.literal("每块矿石 " + FeatureConfig.dbl("autosmelt.costPerOre", 10.0D) + " 点经验");
    }
}
