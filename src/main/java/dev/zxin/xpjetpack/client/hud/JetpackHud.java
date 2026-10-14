package dev.zxin.xpjetpack.client.hud;

import dev.zxin.xpjetpack.Bridge;
import dev.zxin.xpjetpack.client.ClientState;
import dev.zxin.xpjetpack.config.JetpackConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import net.minecraft.client.gui.GuiGraphics;

/**
 * 经验燃料 HUD：热键栏正上方一行居中的文字。
 *
 * <p>显示可用经验点总数（{@code 经验 37}），经验不足时显示红色提示。纯文字，不画进度条。
 *
 * <p>绘制上下文随版本切换：MC 26.x 起 {@code GuiGraphics} 被 {@code GuiGraphicsExtractor}
 * 取代（方法名 {@code drawString} → {@code text}），guiWidth/guiHeight 两代一致。
 */
public final class JetpackHud {

    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_LOW = 0xFFD9A93B;
    private static final int COLOR_WARN = 0xFFFF5555;

    /** 低于该经验点数时文字转黄提示。 */
    private static final int LOW_THRESHOLD = 3;

    private JetpackHud() {
    }

    public static void render(GuiGraphics graphics) {

        ClientState state = ClientState.INSTANCE;
        JetpackConfig cfg = Bridge.config();
        if (cfg == null || !cfg.hudEnabled) {
            return;
        }
        if (!state.enabled && !cfg.hudAlwaysVisible) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null) {
            return;
        }

        boolean empty = !state.hasFuel();
        String text;
        int color;
        if (empty) {
            text = "经验不足";
            color = COLOR_WARN;
        } else {
            text = "经验 " + state.xpPoints;
            if (cfg.elytraOnly) {
                text += " · 仅鞘翅";
            }
            color = state.xpPoints <= LOW_THRESHOLD ? COLOR_LOW : COLOR_TEXT;
        }

        Font font = minecraft.font;
        int textWidth = font.width(text);
        int x = (graphics.guiWidth() - textWidth) / 2;
        int y = graphics.guiHeight() - cfg.hudOffsetY;

        graphics.drawString(font, Component.literal(text), x, y, color);

    }
}
