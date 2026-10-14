package dev.zxin.xpjetpack.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 挖掘信息：加载器事件 → 通用结构。
 *
 * <p>{@link #stateId} 是方块注册 id（如 {@code minecraft:iron_ore}），供功能判断是否为矿石。
 * 需要标签判断或查询配方时，用 {@link #state} 与 {@link #level}（加载器尽量填，可能为 null）。
 */
public final class BlockBreakInfo {

    /** 被挖掉的方块注册 id。 */
    public String stateId;
    /** 方块坐标。 */
    public BlockPos pos;
    /** 掉落物是否已被取消。 */
    public boolean cancelled;
    /** 被挖方块的完整状态（可空）。 */
    public BlockState state;
    /** 所在世界（可空）。 */
    public Level level;

    public BlockBreakInfo(String stateId, BlockPos pos) {
        this.stateId = stateId == null ? "" : stateId;
        this.pos = pos;
    }

    /** 补充方块状态与世界，返回自身便于链式调用。 */
    public BlockBreakInfo with(BlockState state, Level level) {
        this.state = state;
        this.level = level;
        return this;
    }
}
