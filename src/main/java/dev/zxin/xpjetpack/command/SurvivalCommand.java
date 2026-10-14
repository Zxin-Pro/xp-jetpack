package dev.zxin.xpjetpack.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.zxin.xpjetpack.Bridge;
import dev.zxin.xpjetpack.feature.Feature;
import dev.zxin.xpjetpack.feature.FeatureConfig;
import dev.zxin.xpjetpack.feature.FeatureContext;
import dev.zxin.xpjetpack.feature.FeatureRegistry;
import dev.zxin.xpjetpack.feature.FeatureState;
import dev.zxin.xpjetpack.player.JetState;
import dev.zxin.xpjetpack.player.PlayerStore;
import dev.zxin.xpjetpack.util.XpCost;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Map;

/**
 * 生存套件指令树 —— <b>完全由功能注册表自动生成</b>。
 *
 * <p>根指令：{@code /xpsurvival}，别名 {@code /xps}
 * <pre>
 * /xps                        → help
 * /xps help                   → 列出全部功能
 * /xps status                 → 汇总状态
 * /xps on|off|toggle &lt;功能&gt;   → 开关
 * /xps config [功能]          → 列出配置键与当前值
 * /xps reload                 → 重载配置（权限 2）
 * </pre>
 *
 * <p>每个功能还会注册一个独立根指令（{@code /respiration}、{@code /fireproof} …），
 * 由 {@link Feature#commandName()} 决定，新增功能无需改动本类。
 */
public final class SurvivalCommand {

    private SurvivalCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        FeatureRegistry.ensureBootstrapped();
        // 主根 + 短别名，各建一棵独立指令树（不依赖 redirect，避免目标未注册的问题）
        dispatcher.register(buildRoot("xpsurvival"));
        dispatcher.register(buildRoot("xps"));
        registerFeatureCommands(dispatcher);
    }

    /** 构建根指令树（供主根与别名复用）。 */
    private static LiteralArgumentBuilder<CommandSourceStack> buildRoot(String literal) {

        // 1.21.11 / 26.x：hasPermission(int) 已被 PermissionSet 取代
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(literal)
                .requires(source -> true)
                .executes(SurvivalCommand::help);

        root.then(Commands.literal("help").executes(SurvivalCommand::help));
        root.then(Commands.literal("status").executes(SurvivalCommand::status));

        LiteralArgumentBuilder<CommandSourceStack> onNode = Commands.literal("on");
        LiteralArgumentBuilder<CommandSourceStack> offNode = Commands.literal("off");
        LiteralArgumentBuilder<CommandSourceStack> toggleNode = Commands.literal("toggle");
        LiteralArgumentBuilder<CommandSourceStack> statusNode = Commands.literal("status");

        List<Feature> features = FeatureRegistry.list();
        for (int i = 0; i < features.size(); i++) {
            Feature feature = features.get(i);
            onNode.then(Commands.literal(feature.id())
                    .executes(ctx -> setEnabled(ctx, feature, true)));
            offNode.then(Commands.literal(feature.id())
                    .executes(ctx -> setEnabled(ctx, feature, false)));
            toggleNode.then(Commands.literal(feature.id())
                    .executes(ctx -> toggle(ctx, feature)));
            statusNode.then(Commands.literal(feature.id())
                    .executes(ctx -> statusOne(ctx, feature)));
        }
        root.then(onNode);
        root.then(offNode);
        root.then(toggleNode);
        root.then(statusNode);

        LiteralArgumentBuilder<CommandSourceStack> configNode = Commands.literal("config");
        configNode.executes(ctx -> config(ctx, null));
        for (int i = 0; i < features.size(); i++) {
            Feature feature = features.get(i);
            configNode.then(Commands.literal(feature.id())
                    .executes(ctx -> config(ctx, feature.id())));
        }
        root.then(configNode);

        root.then(Commands.literal("reload")
                .requires(source -> true)
                .executes(SurvivalCommand::reload));

        return root;
    }

    /** 每个功能的独立指令根，由注册表自动生成。 */
    private static void registerFeatureCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        List<Feature> features = FeatureRegistry.list();
        for (int i = 0; i < features.size(); i++) {
            Feature feature = features.get(i);
            String name = feature.commandName();
            if (name == null || name.isEmpty()) {
                continue;
            }
            LiteralArgumentBuilder<CommandSourceStack> node = Commands.literal(name)
                    .executes(ctx -> statusOne(ctx, feature))
                    .then(Commands.literal("on").executes(ctx -> setEnabled(ctx, feature, true)))
                    .then(Commands.literal("off").executes(ctx -> setEnabled(ctx, feature, false)))
                    .then(Commands.literal("toggle").executes(ctx -> toggle(ctx, feature)))
                    .then(Commands.literal("status").executes(ctx -> statusOne(ctx, feature)));
            dispatcher.register(node);
        }
    }

    // ------------------------------------------------------------------
    // 上下文
    // ------------------------------------------------------------------

    private static FeatureContext contextOf(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        ServerPlayer player = source.getPlayer();
        JetState state = PlayerStore.get(server).get(player.getUUID());
        return new FeatureContext(player, state, PlayerStore.get(server), Bridge.config(), server.getTickCount());
    }

    // ------------------------------------------------------------------
    // 子指令实现
    // ------------------------------------------------------------------

    private static int help(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        source.sendSuccess(() -> Component.literal("—— 经验生存套件 ——")
                .withStyle(ChatFormatting.GOLD), false);

        List<Feature> features = FeatureRegistry.list();
        for (int i = 0; i < features.size(); i++) {
            Feature feature = features.get(i);
            String line = "/" + feature.id() + "  ·  " + feature.id();
            source.sendSuccess(() -> Component.literal(line).withStyle(ChatFormatting.AQUA), false);
        }
        source.sendSuccess(() -> Component.literal(
                "/xps status | on <功能> | off <功能> | config [功能]").withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        FeatureContext fc = contextOf(source);
        ServerPlayer player = fc.player();

        source.sendSuccess(() -> Component.literal(String.format(
                        "经验点：%d · 喷气：%s",
                        XpCost.available(player),
                        fc.state().enabled ? "开" : "关"))
                .withStyle(ChatFormatting.GOLD), false);

        List<Feature> features = FeatureRegistry.list();
        for (int i = 0; i < features.size(); i++) {
            Feature feature = features.get(i);
            FeatureState st = fc.featureState(feature);
            Component line = Component.literal(String.format("%s：%s%s",
                    feature.id(),
                    st.enabled ? "开" : "关",
                    st.manualDisabled ? "（手动关闭）" : ""))
                    .withStyle(st.enabled ? ChatFormatting.GREEN : ChatFormatting.GRAY);
            source.sendSuccess(() -> line, false);
        }
        return 1;
    }

    private static int statusOne(CommandContext<CommandSourceStack> ctx, Feature feature) {
        CommandSourceStack source = ctx.getSource();
        FeatureContext fc = contextOf(source);
        Component line = feature.statusLine(fc);
        source.sendSuccess(() -> line, false);
        return 1;
    }

    private static int setEnabled(CommandContext<CommandSourceStack> ctx, Feature feature, boolean value) {
        CommandSourceStack source = ctx.getSource();
        FeatureContext fc = contextOf(source);
        FeatureState st = fc.featureState(feature);

        if (value) {
            st.enable();
            feature.onEnable(fc);
        } else {
            st.disableManually();
            feature.onDisable(fc);
        }
        fc.touch();

        source.sendSuccess(() -> Component.literal(String.format("%s：%s",
                        feature.id(), value ? "已开启" : "已关闭"))
                .withStyle(value ? ChatFormatting.GREEN : ChatFormatting.GRAY), false);
        return 1;
    }

    private static int toggle(CommandContext<CommandSourceStack> ctx, Feature feature) {
        CommandSourceStack source = ctx.getSource();
        FeatureContext fc = contextOf(source);
        FeatureState st = fc.featureState(feature);
        return setEnabled(ctx, feature, !st.enabled);
    }

    private static int config(CommandContext<CommandSourceStack> ctx, String prefix) {
        CommandSourceStack source = ctx.getSource();
        source.sendSuccess(() -> Component.literal("—— 生存套件配置 ——")
                .withStyle(ChatFormatting.GOLD), false);

        Map<String, Object> snapshot = FeatureConfig.snapshot();
        int shown = 0;
        for (Map.Entry<String, Object> entry : snapshot.entrySet()) {
            if (prefix != null && !entry.getKey().startsWith(prefix + ".")) {
                continue;
            }
            shown++;
            String line = entry.getKey() + " = " + entry.getValue();
            source.sendSuccess(() -> Component.literal(line).withStyle(ChatFormatting.GRAY), false);
        }
        if (shown == 0) {
            source.sendSuccess(() -> Component.literal("（无匹配项）").withStyle(ChatFormatting.RED), false);
        }
        return 1;
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        Bridge.reload();
        ctx.getSource().sendSuccess(() -> Component.literal("经验套件配置已重载")
                .withStyle(ChatFormatting.YELLOW), false);
        return 1;
    }
}
