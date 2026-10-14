package dev.zxin.xpjetpack.forge;

import dev.zxin.xpjetpack.config.JetpackConfig;
import dev.zxin.xpjetpack.feature.ConfigFacade;
import dev.zxin.xpjetpack.feature.FeatureConfig;
import dev.zxin.xpjetpack.feature.FeatureRegistry;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.LinkedHashMap;
import java.util.Map;

/** NeoForge 配置：ForgeConfigSpec。键名与接口约定文档一致。 */
public final class ForgeConfig {

    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.DoubleValue TICKS_PER_POINT;
    private static final ForgeConfigSpec.DoubleValue ELYTRA_MULTIPLIER;
    private static final ForgeConfigSpec.BooleanValue REQUIRE_LEVEL;
    private static final ForgeConfigSpec.IntValue COMMAND_PERMISSION;
    private static final ForgeConfigSpec.BooleanValue ELYTRA_ONLY;
    private static final ForgeConfigSpec.BooleanValue CREATIVE_INFINITE;
    private static final ForgeConfigSpec.BooleanValue PREVENT_FALL_DAMAGE;
    private static final ForgeConfigSpec.BooleanValue PREVENT_FLYING_KICK;
    private static final ForgeConfigSpec.DoubleValue VERTICAL_ACCELERATION;
    private static final ForgeConfigSpec.DoubleValue VERTICAL_MAX_SPEED;
    private static final ForgeConfigSpec.DoubleValue VERTICAL_DAMPING;
    private static final ForgeConfigSpec.DoubleValue ELYTRA_THRUST;
    private static final ForgeConfigSpec.DoubleValue ELYTRA_MAX_SPEED;
    private static final ForgeConfigSpec.BooleanValue ELYTRA_REQUIRES_FALLFLYING;
    private static final ForgeConfigSpec.IntValue INPUT_TIMEOUT;
    private static final ForgeConfigSpec.BooleanValue PARTICLES;
    private static final ForgeConfigSpec.BooleanValue SOUNDS;
    private static final ForgeConfigSpec.DoubleValue SOUND_VOLUME;
    private static final ForgeConfigSpec.IntValue SYNC_INTERVAL;
    private static final ForgeConfigSpec.BooleanValue HUD_ENABLED;
    private static final ForgeConfigSpec.BooleanValue HUD_ALWAYS_VISIBLE;
    private static final ForgeConfigSpec.IntValue HUD_OFFSET_Y;

    /** 生存套件：功能声明出来的配置值（路径 → ForgeConfigSpec 值）。 */
    private static final Map<String, ForgeConfigSpec.ConfigValue<?>> FEATURE_VALUES =
            new LinkedHashMap<String, ForgeConfigSpec.ConfigValue<?>>();

    /** 配置门面：功能通过它声明配置键，键自动出现在 ForgeConfigSpec 里。 */
    private static final class NeoFacade implements ConfigFacade {

        private final ForgeConfigSpec.Builder builder;

        NeoFacade(ForgeConfigSpec.Builder builder) {
            this.builder = builder;
        }

        @Override
        public void bool(String path, String comment, boolean def) {
            FeatureConfig.declare(path, comment, def);
            FEATURE_VALUES.put(path, builder.comment(comment).define(path, def));
        }

        @Override
        public void dbl(String path, String comment, double def, double min, double max) {
            FeatureConfig.declare(path, comment, def);
            FEATURE_VALUES.put(path, builder.comment(comment).defineInRange(path, def, min, max));
        }

        @Override
        public void intVal(String path, String comment, int def, int min, int max) {
            FeatureConfig.declare(path, comment, def);
            FEATURE_VALUES.put(path, builder.comment(comment).defineInRange(path, def, min, max));
        }

        @Override
        public void str(String path, String comment, String def) {
            FeatureConfig.declare(path, comment, def);
            FEATURE_VALUES.put(path, builder.comment(comment).define(path, def));
        }
    }

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("experience");
        TICKS_PER_POINT = builder.comment("直立模式：消耗 1 点经验可以喷气多少 tick")
                .defineInRange("ticks_per_point", 50.0D, 1.0D, 100000.0D);
        ELYTRA_MULTIPLIER = builder.comment("鞘翅模式消耗倍率（2.0 = 每 25 tick 烧 1 点）")
                .defineInRange("elytra_multiplier", 2.0D, 0.01D, 100.0D);
        REQUIRE_LEVEL = builder.comment("是否要求经验等级大于 0 才能起飞")
                .define("require_level", false);
        builder.pop();

        builder.push("jetpack");
        COMMAND_PERMISSION = builder.comment("/fly 指令所需权限等级")
                .defineInRange("command_permission_level", 0, 0, 4);
        ELYTRA_ONLY = builder.comment("是否只允许鞘翅滑翔时喷气")
                .define("elytra_only", false);
        CREATIVE_INFINITE = builder.comment("创造模式是否零消耗")
                .define("creative_infinite", true);
        PREVENT_FALL_DAMAGE = builder.comment("喷气时是否清零摔落距离")
                .define("prevent_fall_damage", true);
        PREVENT_FLYING_KICK = builder.comment("是否阻止原版「悬浮太久」踢人")
                .define("prevent_flying_kick", true);
        builder.pop();

        builder.push("thrust");
        VERTICAL_ACCELERATION = builder.comment("直立模式每 tick 向上加速度")
                .defineInRange("vertical_acceleration", 0.08D, 0.0D, 10.0D);
        VERTICAL_MAX_SPEED = builder.comment("直立模式最大上升速度")
                .defineInRange("vertical_max_speed", 0.5D, 0.0D, 10.0D);
        VERTICAL_DAMPING = builder.comment("超过最大速度时的阻尼系数")
                .defineInRange("vertical_damping", 0.9D, 0.0D, 1.0D);
        ELYTRA_THRUST = builder.comment("鞘翅模式每 tick 沿视线推力")
                .defineInRange("elytra_thrust_per_tick", 0.015D, 0.0D, 1.0D);
        ELYTRA_MAX_SPEED = builder.comment("鞘翅模式速度硬上限")
                .defineInRange("elytra_max_speed", 1.5D, 0.05D, 10.0D);
        ELYTRA_REQUIRES_FALLFLYING = builder.comment("鞘翅模式是否必须处于滑翔状态")
                .define("elytra_requires_fallflying", true);
        builder.pop();

        builder.push("input");
        INPUT_TIMEOUT = builder.comment("多少 tick 没收到输入包就视为松手")
                .defineInRange("timeout_ticks", 20, 1, 200);
        builder.pop();

        builder.push("effects");
        PARTICLES = builder.comment("喷气粒子").define("particles", true);
        SOUNDS = builder.comment("喷气音效").define("sounds", true);
        SOUND_VOLUME = builder.comment("音效音量")
                .defineInRange("sound_volume", 0.6D, 0.0D, 4.0D);
        builder.pop();

        builder.push("network");
        SYNC_INTERVAL = builder.comment("服务端状态同步周期（tick）")
                .defineInRange("sync_interval_ticks", 40, 1, 200);
        builder.pop();

        builder.push("hud");
        HUD_ENABLED = builder.comment("是否显示经验燃料 HUD")
                .define("enabled", true);
        HUD_ALWAYS_VISIBLE = builder.comment("未开启喷气时也显示 HUD")
                .define("always_visible", false);
        HUD_OFFSET_Y = builder.comment("HUD 距屏幕底部像素（热键栏上方）")
                .defineInRange("offset_y", 45, 10, 400);
        builder.pop();

        // 生存套件：每个功能自己声明配置键（自动注册，本类不需要为每个功能加字段）
        FeatureRegistry.declareAllConfig(new NeoFacade(builder));

        SPEC = builder.build();
    }

    private ForgeConfig() {
    }

    public static JetpackConfig load() {
        JetpackConfig cfg = new JetpackConfig();
        try {
            cfg.xpTicksPerPoint = TICKS_PER_POINT.get();
            cfg.xpElytraMultiplier = ELYTRA_MULTIPLIER.get();
            cfg.xpRequireLevel = REQUIRE_LEVEL.get();
            cfg.commandPermissionLevel = COMMAND_PERMISSION.get();
            cfg.elytraOnly = ELYTRA_ONLY.get();
            cfg.creativeInfinite = CREATIVE_INFINITE.get();
            cfg.preventFallDamage = PREVENT_FALL_DAMAGE.get();
            cfg.preventFlyingKick = PREVENT_FLYING_KICK.get();
            cfg.verticalAcceleration = VERTICAL_ACCELERATION.get();
            cfg.verticalMaxSpeed = VERTICAL_MAX_SPEED.get();
            cfg.verticalDamping = VERTICAL_DAMPING.get();
            cfg.elytraThrustPerTick = ELYTRA_THRUST.get();
            cfg.elytraMaxSpeed = ELYTRA_MAX_SPEED.get();
            cfg.elytraRequiresFallFlying = ELYTRA_REQUIRES_FALLFLYING.get();
            cfg.inputTimeoutTicks = INPUT_TIMEOUT.get();
            cfg.particles = PARTICLES.get();
            cfg.sounds = SOUNDS.get();
            cfg.soundVolume = SOUND_VOLUME.get();
            cfg.syncIntervalTicks = SYNC_INTERVAL.get();
            cfg.hudEnabled = HUD_ENABLED.get();
            cfg.hudAlwaysVisible = HUD_ALWAYS_VISIBLE.get();
            cfg.hudOffsetY = HUD_OFFSET_Y.get();

            // 生存套件：把 ForgeConfigSpec 当前值灌进 FeatureConfig（功能从这里读）
            Map<String, Object> featureValues = new LinkedHashMap<String, Object>();
            for (Map.Entry<String, ForgeConfigSpec.ConfigValue<?>> entry : FEATURE_VALUES.entrySet()) {
                try {
                    featureValues.put(entry.getKey(), entry.getValue().get());
                } catch (Throwable ignored) {
                    // 单个键读取失败不影响其它键
                }
            }
            FeatureConfig.replaceAll(featureValues);
        } catch (IllegalStateException notLoadedYet) {
            return JetpackConfig.defaults();
        }
        return cfg;
    }
}
