package dev.zxin.xpjetpack.fabric;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.zxin.xpjetpack.config.JetpackConfig;
import dev.zxin.xpjetpack.feature.ConfigFacade;
import dev.zxin.xpjetpack.feature.FeatureConfig;
import dev.zxin.xpjetpack.feature.FeatureRegistry;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** Fabric 配置：config/xpjetpack.json。 */
public final class FabricConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static volatile JetpackConfig current = JetpackConfig.defaults();

    private FabricConfig() {
    }

    public static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("xpjetpack.json");
    }

    /** 启动时读盘；文件不存在则写出默认配置。 */
    public static void loadFromDisk() {
        Path path = file();
        try {
            if (Files.exists(path)) {
                String json = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
                JetpackConfig parsed = GSON.fromJson(json, JetpackConfig.class);
                if (parsed != null) {
                    parsed.clamp();
                    current = parsed;
                    return;
                }
            }
            current = JetpackConfig.defaults();
            write(current);
        } catch (Exception e) {
            System.err.println("[XP Jetpack] 读取配置失败，使用默认值: " + e);
            current = JetpackConfig.defaults();
        }
    }

    private static void write(JetpackConfig cfg) {
        try {
            Path path = file();
            Files.createDirectories(path.getParent());
            Files.write(path, GSON.toJson(cfg).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            System.err.println("[XP Jetpack] 写出配置失败: " + e);
        }
    }

    /** 配置加载器回调：/fly reload 会走到这里。 */
    public static JetpackConfig load() {
        loadFromDisk();
        declareFeatures();
        return current.copy();
    }

    // ------------------------------------------------------------------
    // 生存套件：功能配置（扁平「路径 → 值」表，独立文件，跟喷气配置互不干扰）
    // ------------------------------------------------------------------

    public static Path featureFile() {
        return FabricLoader.getInstance().getConfigDir().resolve("xpjetpack-features.json");
    }

    /** 让每个功能声明自己的配置键（自动注册），再读盘覆盖、回写补齐新增键。 */
    public static void declareFeatures() {
        FeatureRegistry.declareAllConfig(new FabricFacade());
        Path path = featureFile();
        try {
            if (!Files.exists(path)) {
                saveFeatures();
                return;
            }
            String json = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            @SuppressWarnings("unchecked")
            Map<String, Object> saved = GSON.fromJson(json, Map.class);
            if (saved != null) {
                for (Map.Entry<String, Object> entry : saved.entrySet()) {
                    if (entry.getValue() != null) {
                        FeatureConfig.put(entry.getKey(), entry.getValue());
                    }
                }
            }
            saveFeatures();
        } catch (Exception e) {
            System.err.println("[XP Jetpack] 读取功能配置失败，使用默认值: " + e);
        }
    }

    /** 把当前功能配置写盘。 */
    public static void saveFeatures() {
        try {
            Path path = featureFile();
            Files.createDirectories(path.getParent());
            Files.write(path, GSON.toJson(FeatureConfig.snapshot()).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            System.err.println("[XP Jetpack] 写出功能配置失败: " + e);
        }
    }

    /** 配置门面：功能通过它声明配置键，Fabric 侧落到独立 JSON。 */
    private static final class FabricFacade implements ConfigFacade {

        @Override
        public void bool(String path, String comment, boolean def) {
            FeatureConfig.declare(path, comment, def);
        }

        @Override
        public void dbl(String path, String comment, double def, double min, double max) {
            FeatureConfig.declare(path, comment, def);
        }

        @Override
        public void intVal(String path, String comment, int def, int min, int max) {
            FeatureConfig.declare(path, comment, def);
        }

        @Override
        public void str(String path, String comment, String def) {
            FeatureConfig.declare(path, comment, def);
        }
    }
}
