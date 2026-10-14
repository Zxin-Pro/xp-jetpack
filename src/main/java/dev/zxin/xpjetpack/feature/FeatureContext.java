package dev.zxin.xpjetpack.feature;

import dev.zxin.xpjetpack.Bridge;
import dev.zxin.xpjetpack.config.JetpackConfig;
import dev.zxin.xpjetpack.player.JetState;
import dev.zxin.xpjetpack.player.PlayerStore;
import net.minecraft.server.level.ServerPlayer;

/**
 * 功能上下文：一次功能调用所需的全部环境。
 *
 * <p>功能实现里禁止出现任何加载器专属 import（forge / neoforge / fabric），
 * 一切平台能力走 {@link Bridge}。
 */
public final class FeatureContext {

    private final ServerPlayer player;
    private final JetState state;
    private final PlayerStore store;
    private final JetpackConfig config;
    private final long tick;

    public FeatureContext(ServerPlayer player, JetState state, PlayerStore store,
                          JetpackConfig config, long tick) {
        this.player = player;
        this.state = state;
        this.store = store;
        this.config = config;
        this.tick = tick;
    }

    public ServerPlayer player() {
        return player;
    }

    public JetState state() {
        return state;
    }

    public PlayerStore store() {
        return store;
    }

    public JetpackConfig config() {
        return config;
    }

    public long tick() {
        return tick;
    }

    /** 取本功能在本玩家的状态（不存在则按默认值创建）。 */
    public FeatureState featureState(Feature feature) {
        return FeatureStates.of(state, feature);
    }

    /** 因状态被修改而需要落盘。 */
    public void touch() {
        store.markDirty();
    }
}
