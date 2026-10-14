package dev.zxin.xpjetpack.feature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 功能配置读取层：按路径取值的统一入口。
 *
 * <p>加载器在配置加载/重载时把全部已声明键的当前值灌进 {@link #replaceAll(Map)}，
 * 功能通过 {@code getBool("respiration.enabled", true)} 之类读取。
 * 这样新增功能只声明键，不需要改任何总配置类。
 */
public final class FeatureConfig {

    private static final Map<String, Object> VALUES = new LinkedHashMap<String, Object>();
    private static final Map<String, String> COMMENTS = new LinkedHashMap<String, String>();

    private FeatureConfig() {
    }

    /** 记录一个键的默认值（由加载器在声明阶段调用）。 */
    public static synchronized void declare(String path, String comment, Object def) {
        if (path == null || path.isEmpty()) {
            return;
        }
        COMMENTS.put(path, comment == null ? "" : comment);
        if (!VALUES.containsKey(path)) {
            VALUES.put(path, def);
        }
    }

    /** 加载器灌入当前值（覆盖已声明键）。 */
    public static synchronized void replaceAll(Map<String, Object> loaded) {
        if (loaded == null) {
            return;
        }
        VALUES.putAll(loaded);
    }

    public static synchronized void put(String path, Object value) {
        if (path != null && value != null) {
            VALUES.put(path, value);
        }
    }

    public static synchronized void clear() {
        VALUES.clear();
        COMMENTS.clear();
    }

    /** 全部已声明键（按字典序，供 /xps config 展示）。 */
    public static synchronized List<String> keys() {
        List<String> list = new ArrayList<String>(VALUES.keySet());
        Collections.sort(list);
        return list;
    }

    public static synchronized Map<String, Object> snapshot() {
        return new TreeMap<String, Object>(VALUES);
    }

    public static synchronized String comment(String path) {
        return COMMENTS.get(path);
    }

    public static synchronized boolean has(String path) {
        return VALUES.containsKey(path);
    }

    // ------------------------------------------------------------------
    // 读取
    // ------------------------------------------------------------------

    public static synchronized boolean bool(String path, boolean def) {
        Object v = VALUES.get(path);
        if (v instanceof Boolean) {
            return (Boolean) v;
        }
        if (v instanceof String) {
            return Boolean.parseBoolean((String) v);
        }
        return def;
    }

    public static synchronized double dbl(String path, double def) {
        Object v = VALUES.get(path);
        if (v instanceof Number) {
            return ((Number) v).doubleValue();
        }
        if (v instanceof String) {
            try {
                return Double.parseDouble((String) v);
            } catch (NumberFormatException ignored) {
                return def;
            }
        }
        return def;
    }

    public static synchronized int intVal(String path, int def) {
        Object v = VALUES.get(path);
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        if (v instanceof String) {
            try {
                return (int) Double.parseDouble((String) v);
            } catch (NumberFormatException ignored) {
                return def;
            }
        }
        return def;
    }

    public static synchronized String str(String path, String def) {
        Object v = VALUES.get(path);
        return v == null ? def : String.valueOf(v);
    }

    /** 逗号分隔列表。 */
    public static synchronized List<String> strList(String path, String def) {
        String raw = str(path, def);
        List<String> out = new ArrayList<String>();
        if (raw == null || raw.trim().isEmpty()) {
            return out;
        }
        String[] parts = raw.split(",");
        for (int i = 0; i < parts.length; i++) {
            String item = parts[i].trim();
            if (!item.isEmpty()) {
                out.add(item);
            }
        }
        return out;
    }
}
