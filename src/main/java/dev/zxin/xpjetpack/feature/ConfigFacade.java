package dev.zxin.xpjetpack.feature;

/**
 * 配置声明门面：功能通过它声明自己的配置键，三套配置系统自动出现这些键。
 *
 * <p>{@code path} 约定为 {@code <featureId>.<key>}，全局键用 {@code global.<key>}。
 */
public interface ConfigFacade {

    /** 布尔键。 */
    void bool(String path, String comment, boolean def);

    /** 浮点键（带范围）。 */
    void dbl(String path, String comment, double def, double min, double max);

    /** 整型键（带范围）。 */
    void intVal(String path, String comment, int def, int min, int max);

    /** 字符串键（如标签列表、通知模式）。 */
    void str(String path, String comment, String def);
}
