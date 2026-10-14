package dev.zxin.xpjetpack.client;

import dev.zxin.xpjetpack.Bridge;
import dev.zxin.xpjetpack.config.JetpackConfig;
import dev.zxin.xpjetpack.network.InputPacket;
import dev.zxin.xpjetpack.physics.JetpackPhysics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

/**
 * 客户端：读按键 → 发输入包 → 本地预测施力 → 粒子音效。
 * 真正的判定仍在服务端，这里只保证手感顺滑。
 */
public final class JetpackClientLogic {

    private static final int RESEND_TICKS = 10;
    private static final int SOUND_TICKS = 4;

    private static boolean lastSent;
    private static int resendCooldown = RESEND_TICKS;
    private static int soundCooldown = SOUND_TICKS;

    private JetpackClientLogic() {
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }
        LocalPlayer player = minecraft.player;
        if (player == null) {
            lastSent = false;
            return;
        }

        ClientState state = ClientState.INSTANCE;
        JetpackConfig cfg = state.config();

        boolean pressed = minecraft.screen == null && minecraft.options.keyJump.isDown();

        boolean thrust = pressed && state.enabled && state.hasFuel();

        if (thrust != lastSent) {
            Bridge.sendToServer(new InputPacket(thrust));
            lastSent = thrust;
            resendCooldown = RESEND_TICKS;
        } else if (thrust) {
            resendCooldown--;
            if (resendCooldown <= 0) {
                Bridge.sendToServer(new InputPacket(true));
                resendCooldown = RESEND_TICKS;
            }
        }

        if (!thrust) {
            return;
        }

        boolean elytra = player.isFallFlying();
        if (elytra) {
            JetpackPhysics.applyElytraThrust(player, cfg);
        } else if (!player.onGround() && !cfg.elytraOnly) {
            JetpackPhysics.applyUprightThrust(player, cfg);
        } else {
            return;
        }

        // 本地预测：进度条按当前速率消耗；服务端始终是权威值
        state.burn = Math.max(0.0D, state.burn - cfg.burnPerTick(elytra));

        if (cfg.particles) {
            spawnParticles(minecraft, player, elytra);
        }
        if (cfg.sounds) {
            soundCooldown--;
            if (soundCooldown <= 0) {
                soundCooldown = SOUND_TICKS;
                playSound(minecraft, player, cfg, elytra);
            }
        }
    }

    private static void spawnParticles(Minecraft minecraft, LocalPlayer player, boolean elytra) {
        Level level = minecraft.level;
        if (level == null) {
            return;
        }
        double yaw = Math.toRadians(player.getYRot());
        double backX = -Math.sin(yaw) * 0.34D;
        double backZ = Math.cos(yaw) * 0.34D;
        double x = player.getX() + backX;
        double y = player.getY() + (elytra ? 0.85D : 0.6D);
        double z = player.getZ() + backZ;

        level.addParticle(ParticleTypes.CLOUD, x, y, z, backX * 0.6D, -0.32D, backZ * 0.6D);
        level.addParticle(ParticleTypes.FLAME, x, y, z, backX * 0.3D, -0.22D, backZ * 0.3D);
    }

    private static void playSound(Minecraft minecraft, LocalPlayer player, JetpackConfig cfg, boolean elytra) {
        Level level = minecraft.level;
        if (level == null) {
            return;
        }
        double speed = player.getDeltaMovement().length();
        double max = elytra ? cfg.elytraMaxSpeed : cfg.verticalMaxSpeed;
        float ratio = max <= 1.0E-6D ? 0.0F : (float) Math.min(1.0D, speed / max);
        float volume = (float) (cfg.soundVolume * (0.5D + 0.5D * ratio));
        float pitch = 1.0F + ratio * 0.3F;

        level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, volume, pitch, false);
    }

    /** 断开连接时复位。 */
    public static void reset() {
        lastSent = false;
        resendCooldown = RESEND_TICKS;
        ClientState.INSTANCE.reset();
    }
}
