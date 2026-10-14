package dev.zxin.xpjetpack.neoforge;

import dev.zxin.xpjetpack.feature.BlockBreakInfo;
import dev.zxin.xpjetpack.feature.DamageInfo;
import dev.zxin.xpjetpack.feature.FeatureRegistry;
import dev.zxin.xpjetpack.util.OreTags;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;

// 1.21 起 NeoForge 把 LivingHurtEvent 改名为 LivingIncomingDamageEvent
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

// 26.2 起 BreakEvent 从 BlockEvent 内部类拆成独立的 BreakBlockEvent
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

/**
 * NeoForge 侧事件桥：只做接线，不含功能逻辑。
 * 以后新增功能只要实现 {@code onDamage} / {@code onBlockBreak} 即可自动收到事件。
 */
public final class NeoFeatureEvents {

    private NeoFeatureEvents() {
    }

    public static void register() {

        NeoForge.EVENT_BUS.addListener((LivingIncomingDamageEvent event) -> {
            if (!(event.getEntity() instanceof ServerPlayer player)) {
                return;
            }
            DamageInfo info = new DamageInfo(event.getAmount(), "", false);
            if (FeatureRegistry.damage(player, info)) {
                event.setAmount((float) info.amount);
            }
        });

        NeoForge.EVENT_BUS.addListener((BreakBlockEvent event) -> {
            notifyBreak(event.getPlayer(), event.getLevel(), event.getPos(), event.getState());
        });

    }

    /** 方块破坏 → 通用结构（三个分支共用）。 */
    private static void notifyBreak(net.minecraft.world.entity.player.Player rawPlayer,
                                    net.minecraft.world.level.LevelAccessor rawLevel,
                                    net.minecraft.core.BlockPos pos,
                                    net.minecraft.world.level.block.state.BlockState state) {
        if (!(rawPlayer instanceof ServerPlayer player)) {
            return;
        }
        Level level = rawLevel instanceof Level lvl ? lvl : null;
        BlockBreakInfo info = new BlockBreakInfo(OreTags.idOf(state), pos).with(state, level);
        FeatureRegistry.blockBreak(player, info);
    }
}
