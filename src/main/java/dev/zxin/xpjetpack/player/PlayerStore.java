package dev.zxin.xpjetpack.player;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 世界级玩家状态存储：&lt;world&gt;/xpjetpack/players.json
 * 用纯文件 IO + Gson，跨 1.20.1 → 26.x 全部版本零 API 差异。
 */
public final class PlayerStore {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final long SAVE_INTERVAL_TICKS = 600L;

    private static PlayerStore instance;
    private static Path boundDir;

    private final Path dir;
    private final Path file;
    private final Map<String, JetState> players = new HashMap<String, JetState>();
    private boolean dirty;
    private long lastSaveTick = -1L;

    private PlayerStore(Path dir) {
        this.dir = dir;
        this.file = dir.resolve("players.json");
        load();
    }

    public static synchronized PlayerStore get(MinecraftServer server) {
        Path target = server.getWorldPath(LevelResource.ROOT).resolve("xpjetpack");
        if (instance == null || boundDir == null || !boundDir.equals(target)) {
            instance = new PlayerStore(target);
            boundDir = target;
        }
        return instance;
    }

    public synchronized JetState get(UUID id) {
        String key = id.toString();
        JetState state = players.get(key);
        if (state == null) {
            state = new JetState();
            players.put(key, state);
            dirty = true;
        }
        return state;
    }

    public synchronized void markDirty() {
        dirty = true;
    }

    public synchronized void load() {
        players.clear();
        if (!Files.exists(file)) {
            return;
        }
        try {
            String json = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            Data data = GSON.fromJson(json, Data.class);
            if (data != null && data.players != null) {
                for (Map.Entry<String, JetState> entry : data.players.entrySet()) {
                    if (entry.getKey() == null || entry.getValue() == null) {
                        continue;
                    }
                    JetState state = entry.getValue();
                    state.clampSelf();
                    players.put(entry.getKey(), state);
                }
            }
        } catch (Exception e) {
            System.err.println("[XP Jetpack] 读取 " + file + " 失败: " + e);
        }
    }

    public synchronized void saveNow() {
        try {
            Files.createDirectories(dir);
            Data data = new Data();
            data.players = new HashMap<String, JetState>(players);
            Files.write(file, GSON.toJson(data).getBytes(StandardCharsets.UTF_8));
            dirty = false;
        } catch (IOException e) {
            System.err.println("[XP Jetpack] 写入 " + file + " 失败: " + e);
        }
    }

    public synchronized void maybeSave(long tick) {
        if (!dirty) {
            return;
        }
        if (lastSaveTick < 0L || tick - lastSaveTick >= SAVE_INTERVAL_TICKS) {
            lastSaveTick = tick;
            saveNow();
        }
    }

    /** JSON 根对象。 */
    static final class Data {
        int version = 1;
        Map<String, JetState> players = new HashMap<String, JetState>();
    }
}
