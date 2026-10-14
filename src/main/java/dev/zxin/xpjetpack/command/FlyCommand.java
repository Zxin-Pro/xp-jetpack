package dev.zxin.xpjetpack.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.zxin.xpjetpack.Bridge;
import dev.zxin.xpjetpack.config.JetpackConfig;
import dev.zxin.xpjetpack.player.JetState;
import dev.zxin.xpjetpack.player.PlayerStore;
import dev.zxin.xpjetpack.util.XpUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * /fly 指令树。权限等级由配置项 jetpack.command_permission_level 动态决定。
 *
 * <p>燃料就是玩家经验点本身，没有中间资源，因此不提供 refill / air 子指令。
 */
public final class FlyCommand {

    private FlyCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        // 1.21.11 / 26.x 引入了全新的 PermissionSet 系统，不再有 hasPermission(int)
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("fly")
                .requires(source -> true)
                .executes(ctx -> toggle(ctx))
                .then(Commands.literal("on").executes(ctx -> setState(ctx, true)))
                .then(Commands.literal("off").executes(ctx -> setState(ctx, false)))
                .then(Commands.literal("toggle").executes(ctx -> toggle(ctx)))
                .then(Commands.literal("status").executes(ctx -> status(ctx)))
                .then(Commands.literal("reload")
                        .requires(source -> true)
                        .executes(ctx -> reload(ctx)));

        dispatcher.register(root);
    }

    private static JetState stateOf(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        ServerPlayer player = source.getPlayer();
        return PlayerStore.get(server).get(player.getUUID());
    }

    private static int toggle(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        JetState state = stateOf(source);
        boolean next = !state.enabled;
        state.setEnabled(next);
        PlayerStore.get(source.getServer()).markDirty();
        source.sendSuccess(() -> Component.translatable(
                next ? "xpjetpack.msg.enabled" : "xpjetpack.msg.disabled")
                .withStyle(next ? ChatFormatting.GREEN : ChatFormatting.GRAY), false);
        return 1;
    }

    private static int setState(CommandContext<CommandSourceStack> ctx, boolean value) {
        CommandSourceStack source = ctx.getSource();
        JetState state = stateOf(source);
        state.setEnabled(value);
        PlayerStore.get(source.getServer()).markDirty();
        source.sendSuccess(() -> Component.translatable(
                value ? "xpjetpack.msg.enabled" : "xpjetpack.msg.disabled")
                .withStyle(value ? ChatFormatting.GREEN : ChatFormatting.GRAY), false);
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        JetState state = stateOf(source);
        ServerPlayer player = source.getPlayer();
        JetpackConfig cfg = Bridge.config();

        int points = XpUtil.available(player);
        // 每点经验可喷 xpTicksPerPoint 个 tick
        double seconds = XpUtil.seconds((int) Math.floor(points * cfg.xpTicksPerPoint));
        String stateText = state.enabled ? "开" : "关";

        source.sendSuccess(() -> Component.literal(String.format(
                "喷气状态：%s · 经验点：%d · 剩余飞行约 %.1f 秒",
                stateText, points, seconds)).withStyle(ChatFormatting.AQUA), false);
        return 1;
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        Bridge.reload();
        ctx.getSource().sendSuccess(() -> Component.literal("XP Jetpack 配置已重载")
                .withStyle(ChatFormatting.YELLOW), false);
        return 1;
    }
}
