package dev.zxin.xpjetpack.logic;

import dev.zxin.xpjetpack.Bridge;
import dev.zxin.xpjetpack.config.JetpackConfig;
import dev.zxin.xpjetpack.feature.FeatureContext;
import dev.zxin.xpjetpack.feature.FeatureRegistry;
import dev.zxin.xpjetpack.network.StatePacket;
import dev.zxin.xpjetpack.physics.JetpackPhysics;
import dev.zxin.xpjetpack.player.JetState;
import dev.zxin.xpjetpack.player.PlayerStore;
import dev.zxin.xpjetpack.util.XpUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * 服务端权威 tick：判定、消耗经验、施力、同步。
 * 由加载器的「服务器 tick 事件」每 tick 调用一次。
 *
 * <p>燃料模型：喷气<b>直接</b>消耗玩家经验点。起喷时先付掉 1 点，
 * 之后每 tick 扣 {@code 1 / xpTicksPerPoint} 的比例，比例归零再付下一点。
 */
public final class JetpackServerLogic {

    private JetpackServerLogic() {
    }

    public static void tick(MinecraftServer server) {
        if (server == null) {
            return;
        }
        JetpackConfig cfg = Bridge.config();
        PlayerStore store = PlayerStore.get(server);
        int tick = server.getTickCount();
        List<ServerPlayer> players = server.getPlayerList().getPlayers();

        for (int i = 0; i < players.size(); i++) {
            ServerPlayer player = players.get(i);
            if (player == null) {
                continue;
            }
            JetState state = store.get(player.getUUID());
            try {
                tickPlayer(player, state, cfg, tick);
            } catch (Throwable t) {
                Bridge.info("玩家 tick 异常: " + t);
            }
            // 生存套件：把本 tick 分发给所有已注册功能（喷气逻辑不受影响）
            try {
                FeatureRegistry.tick(new FeatureContext(player, state, store, cfg, tick));
            } catch (Throwable t) {
                Bridge.info("功能分发异常: " + t);
            }
        }
        store.maybeSave(tick);
    }

    private static void tickPlayer(ServerPlayer player, JetState state, JetpackConfig cfg, int tick) {
        if (state.inputTicks < 1000) {
            state.inputTicks++;
        }

        if (!state.enabled) {
            state.burning = false;
            state.burn = 1.0D;
            state.xpPoints = XpUtil.available(player);
            syncIfNeeded(player, state, cfg, tick);
            return;
        }

        boolean inputFresh = state.thrusting && state.inputTicks <= cfg.inputTimeoutTicks;
        boolean creativeFree = cfg.creativeInfinite && player.isCreative();
        boolean hasFuel = creativeFree || XpUtil.hasFuel(player, cfg);
        boolean flying = player.isFallFlying();

        boolean wantElytra = inputFresh && hasFuel && flying;
        boolean wantUpright = inputFresh && hasFuel && !flying;

        boolean thrusted = false;
        boolean elytra = false;

        if (wantElytra && JetpackPhysics.canThrustElytra(player, cfg)) {
            JetpackPhysics.applyElytraThrust(player, cfg);
            thrusted = true;
            elytra = true;
        } else if (wantUpright && JetpackPhysics.canThrustUpright(player, cfg)) {
            JetpackPhysics.applyUprightThrust(player, cfg);
            thrusted = true;
        }

        if (thrusted) {
            state.exhaustedNotified = false;
            if (creativeFree) {
                state.burn = 1.0D;
                state.burning = true;
            } else {
                burnXp(player, state, cfg, elytra);
            }
            protect(player, cfg);
        } else {
            state.burning = false;
            // 松开空格即停喷：进度条回到满格（下一次起喷重新付 1 点）
            state.burn = 1.0D;
            if (inputFresh && !hasFuel && !state.exhaustedNotified) {
                notifyExhausted(player, state);
            }
        }

        state.xpPoints = XpUtil.available(player);
        syncIfNeeded(player, state, cfg, tick);
    }

    /** 直接烧经验：起喷先付 1 点，之后按比例续费。 */
    private static void burnXp(ServerPlayer player, JetState state, JetpackConfig cfg, boolean elytra) {
        if (!state.burning) {
            // 刚起喷：立刻扣掉 1 点
            if (XpUtil.consume(player, 1) <= 0) {
                state.burn = 0.0D;
                notifyExhausted(player, state);
                return;
            }
            state.startBurn();
        }

        state.burn -= cfg.burnPerTick(elytra);

        // 比例归零 → 续下一点；没经验了就地停喷
        while (state.burn <= 0.0D) {
            if (XpUtil.consume(player, 1) > 0) {
                state.burn += 1.0D;
            } else {
                state.burn = 0.0D;
                state.burning = false;
                notifyExhausted(player, state);
                break;
            }
        }
    }

    private static void notifyExhausted(ServerPlayer player, JetState state) {
        if (state.exhaustedNotified) {
            return;
        }
        state.exhaustedNotified = true;
        player.sendSystemMessage(Component.translatable("xpjetpack.msg.no_xp").withStyle(ChatFormatting.RED));
    }

    /** 防止原版「悬浮太久」踢人与摔落伤害。 */
    private static void protect(ServerPlayer player, JetpackConfig cfg) {
        if (cfg.preventFallDamage) {
            player.resetFallDistance();
        }
        if (cfg.preventFlyingKick) {
            AntiKick.reset(player);
        }
    }

    private static void syncIfNeeded(ServerPlayer player, JetState state, JetpackConfig cfg, int tick) {
        boolean changed = Math.abs(state.burn - state.lastSyncedBurn) > 0.01D
                || state.xpPoints != state.lastSyncedXp
                || state.enabled != state.lastSyncedEnabled
                || state.thrusting != state.lastSyncedThrust;
        boolean periodic = cfg.syncIntervalTicks > 0 && tick % cfg.syncIntervalTicks == 0;
        if (!changed && !periodic) {
            return;
        }
        state.lastSyncedBurn = state.burn;
        state.lastSyncedXp = state.xpPoints;
        state.lastSyncedEnabled = state.enabled;
        state.lastSyncedThrust = state.thrusting;
        Bridge.sendToClient(player, StatePacket.of(state.burn, state.xpPoints, state.enabled, cfg));
    }
}
