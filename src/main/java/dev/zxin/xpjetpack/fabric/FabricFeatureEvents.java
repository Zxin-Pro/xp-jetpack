package dev.zxin.xpjetpack.fabric;

import dev.zxin.xpjetpack.feature.BlockBreakInfo;
import dev.zxin.xpjetpack.feature.DamageInfo;
import dev.zxin.xpjetpack.feature.FeatureRegistry;
import dev.zxin.xpjetpack.util.OreTags;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Fabric 侧事件桥：只做接线，不含功能逻辑。
 *
 * <p>伤害事件按版本分叉：
 * <ul>
 *   <li><b>1.21+</b>：有 {@code AFTER_DAMAGE}（伤害已结算），置 {@code postApplied = true}，
 *       需要减伤的功能按「回补」处理（见 {@code ShieldFeature}；{@code heal()} 全版本通用，
 *       不受 {@code hurt → hurtServer} 改名影响）。</li>
 *   <li><b>1.20.x</b>：Fabric API 没有伤害后事件，只有 {@code ALLOW_DAMAGE}（只能整体取消）。
 *       替代写法：先扣经验算出削减后的数值 → 取消原伤害 → 按削减值补一次，
 *       用 {@code REAPPLYING} 防重入，减伤比例与新版保持一致。</li>
 * </ul>
 */
public final class FabricFeatureEvents {

    /** 重入保护：我们自己补刀时触发的那次 ALLOW_DAMAGE 直接放行。 */
    private static boolean REAPPLYING = false;

    private FabricFeatureEvents() {
    }

    public static void register() {

        // 1.20.x 的 Fabric API 没有「伤害后」事件，只有 ALLOW_DAMAGE（只能整体取消）。
        // 替代写法：先扣经验算出削减后的数值 → 取消原伤害 → 按削减值补一次，减伤比例保持一致。
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(
                (LivingEntity entity, DamageSource source, float amount) -> {
                    if (!(entity instanceof ServerPlayer player) || amount <= 0.0F || REAPPLYING) {
                        return true;
                    }
                    DamageInfo info = new DamageInfo(amount, "", false);
                    if (!FeatureRegistry.damage(player, info)) {
                        return true;
                    }
                    float reduced = (float) info.amount;
                    if (reduced >= amount) {
                        return true;    // 没有功能改动伤害，原样放行
                    }
                    if (reduced <= 0.0F) {
                        return false;   // 完全抵挡
                    }
                    REAPPLYING = true;
                    try {
                        player.hurt(source, reduced);
                    } finally {
                        REAPPLYING = false;
                    }
                    return false;       // 原伤害取消，已按削减值补上
                });

        // 挖掘事件：签名 1.20.1 → 26.3 完全一致
        PlayerBlockBreakEvents.AFTER.register(
                (Level world, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity) -> {
                    if (!(player instanceof ServerPlayer serverPlayer)) {
                        return;
                    }
                    BlockBreakInfo info = new BlockBreakInfo(OreTags.idOf(state), pos).with(state, world);
                    FeatureRegistry.blockBreak(serverPlayer, info);
                });
    }
}
