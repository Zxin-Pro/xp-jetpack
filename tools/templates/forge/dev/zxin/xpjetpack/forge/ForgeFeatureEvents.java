package dev.zxin.xpjetpack.forge;

import dev.zxin.xpjetpack.feature.BlockBreakInfo;
import dev.zxin.xpjetpack.feature.DamageInfo;
import dev.zxin.xpjetpack.feature.FeatureRegistry;
import dev.zxin.xpjetpack.util.OreTags;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Forge 侧事件桥：把加载器事件翻译成通用结构后交给功能注册表。
 *
 * <p>本类<b>只做接线</b>，不含任何功能逻辑 —— 以后新增功能<b>不需要改这里</b>，
 * 只要功能实现 {@code onDamage} / {@code onBlockBreak} 即可自动收到事件。
 */
@Mod.EventBusSubscriber(modid = dev.zxin.xpjetpack.XpJetpack.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ForgeFeatureEvents {

    private ForgeFeatureEvents() {
    }

    /** 伤害前事件：可以直接改伤害值。 */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        DamageInfo info = new DamageInfo(event.getAmount(), "", false);
        if (FeatureRegistry.damage(player, info)) {
            event.setAmount((float) info.amount);
        }
    }

    /** 挖掘事件。 */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        Level level = event.getLevel() instanceof Level lvl ? lvl : null;
        BlockBreakInfo info = new BlockBreakInfo(OreTags.idOf(event.getState()), event.getPos())
                .with(event.getState(), level);
        FeatureRegistry.blockBreak(player, info);
    }
}
