package dev.zxin.xpjetpack.feature;

import dev.zxin.xpjetpack.player.JetState;

/** 功能状态表：惰性创建 + 旧存档兼容。 */
public final class FeatureStates {

    private FeatureStates() {
    }

    /** 取某功能在某玩家上的状态；不存在则按功能默认值创建（兼容旧存档）。 */
    public static FeatureState of(JetState state, Feature feature) {
        FeatureState found = state.features.get(feature.id());
        if (found != null) {
            return found;
        }
        FeatureState created = new FeatureState();
        created.enabled = feature.defaultEnabled();
        state.features.put(feature.id(), created);
        return created;
    }

    /** 读盘后统一自愈。 */
    public static void clampAll(JetState state) {
        if (state.features == null) {
            state.features = FeatureState.newMap();
            return;
        }
        for (FeatureState st : state.features.values()) {
            if (st != null) {
                st.clampSelf();
            }
        }
    }
}
