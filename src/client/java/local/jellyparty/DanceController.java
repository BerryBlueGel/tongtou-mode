package local.jellyparty;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class DanceController {
    private final SongPlayback playback = new SongPlayback();
    private DanceConfig config = new DanceConfig();
    private ClientLevel sessionLevel;
    private boolean requested;
    private boolean audible;
    private DancePose lastPose = DancePose.IDENTITY;
    private DancePose endPose = DancePose.IDENTITY;
    private long endingAt;
    private final Map<UUID, RangeTransition> ranges = new HashMap<>();

    public void toggle(Minecraft client) {
        if (requested) {
            finish(client, false);
        } else if (client.level != null && client.player != null) {
            config = DanceConfig.load();
            endingAt = 0;
            ranges.clear();
            lastPose = DancePose.IDENTITY;
            sessionLevel = client.level;
            requested = playback.start(client, config.volume);
            audible = false;
            if (requested) message(client, "jellyparty.started");
            else message(client, "jellyparty.sound_failed");
        }
    }

    public void tick(Minecraft client) {
        if (client.level == null || client.player == null || client.level != sessionLevel) {
            reset(client);
            return;
        }
        if (!requested) {
            if (endingAt != 0 && (System.nanoTime() - endingAt) / 1_000_000_000.0 >= config.transitionSeconds) {
                endingAt = 0;
                ranges.clear();
            }
            return;
        }
        SongPlayback.Status status = playback.tick(client);
        audible = status == SongPlayback.Status.PLAYING || status == SongPlayback.Status.PAUSED;
        if (audible) lastPose = activePose(client);
        if (status == SongPlayback.Status.FINISHED || status == SongPlayback.Status.FAILED) {
            finish(client, status == SongPlayback.Status.FAILED);
        }
    }

    public DancePose extractPose(Minecraft client, Entity entity) {
        if (client.player == null || client.level != sessionLevel
                || !(entity instanceof Mob || entity instanceof Player)
                || entity.isRemoved() || !entity.isAlive()
                || !"minecraft".equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getNamespace())) {
            return DancePose.IDENTITY;
        }
        double distance = Math.sqrt(entity.distanceToSqr(client.player));
        double now = System.nanoTime() / 1_000_000_000.0;
        UUID id = entity.getUUID();
        RangeTransition transition = ranges.get(id);
        if (distance >= config.radius) {
            if (transition == null) return DancePose.IDENTITY;
            DancePose outside = transition.outside(now, config.transitionSeconds);
            if (outside.equals(DancePose.IDENTITY)) ranges.remove(id);
            return outside;
        }
        DancePose pose;
        if (requested && audible) {
            pose = activePose(client);
        } else if (endingAt != 0) {
            double age = (System.nanoTime() - endingAt) / 1_000_000_000.0;
            pose = endPose.fade(1 - DanceMath.smooth(age / config.transitionSeconds));
        } else {
            return DancePose.IDENTITY;
        }
        if (transition == null || transition.isExiting()) {
            transition = new RangeTransition(transition == null ? DancePose.IDENTITY : transition.lastPose(), pose, now);
            ranges.put(id, transition);
        }
        return transition.inside(pose, now, config.transitionSeconds);
    }

    private DancePose activePose(Minecraft client) {
        double seconds = playback.seconds(client.isPaused());
        return DanceMath.pose(seconds, config).fade(DanceMath.smooth(seconds / config.transitionSeconds));
    }

    private void finish(Minecraft client, boolean failed) {
        endPose = lastPose;
        endingAt = System.nanoTime();
        playback.stop(client);
        requested = false;
        audible = false;
        message(client, failed ? "jellyparty.sound_failed" : "jellyparty.stopped");
    }

    public void reset(Minecraft client) {
        if (requested) playback.stop(client);
        requested = false;
        audible = false;
        endingAt = 0;
        sessionLevel = null;
        lastPose = DancePose.IDENTITY;
        endPose = DancePose.IDENTITY;
        ranges.clear();
    }

    public boolean isRequested() { return requested; }
    public boolean isAudible() { return audible; }
    public double songSeconds(Minecraft client) { return playback.seconds(client.isPaused()); }

    private static void message(Minecraft client, String key) {
        if (client.player != null) client.player.sendOverlayMessage(Component.translatable(key));
    }
}
