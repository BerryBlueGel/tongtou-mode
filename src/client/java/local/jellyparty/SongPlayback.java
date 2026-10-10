package local.jellyparty;

import local.jellyparty.mixin.ChannelAccessor;
import local.jellyparty.mixin.SoundEngineAccessor;
import local.jellyparty.mixin.SoundManagerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;

import java.util.concurrent.atomic.AtomicBoolean;

/** Reads the real OpenAL playback cursor on Minecraft's sound executor. */
public final class SongPlayback {
    public enum Status { WAITING, PLAYING, PAUSED, FINISHED, FAILED }
    private record Cursor(double seconds, long sampledAt, int sourceState) { }

    private final AtomicBoolean queryQueued = new AtomicBoolean();
    private volatile Cursor cursor;
    private volatile long generation;
    private LocalSong sound;
    private long requestedAt;
    private boolean started;
    private Status status = Status.FINISHED;

    public boolean start(Minecraft client, float volume) {
        stop(client);
        sound = new LocalSong(volume);
        requestedAt = System.nanoTime();
        cursor = null;
        started = false;
        status = Status.WAITING;
        SoundEngine.PlayResult result = client.getSoundManager().play(sound);
        if (result == SoundEngine.PlayResult.NOT_STARTED) {
            stop(client);
            status = Status.FAILED;
            return false;
        }
        return true;
    }

    public Status tick(Minecraft client) {
        if (sound == null) return status;
        SoundEngine engine = ((SoundManagerAccessor) client.getSoundManager()).jellyparty$getSoundEngine();
        ChannelAccess.ChannelHandle handle = ((SoundEngineAccessor) engine).jellyparty$getChannels().get(sound);
        if (handle != null && !handle.isStopped() && queryQueued.compareAndSet(false, true)) {
            long token = generation;
            handle.execute(channel -> {
                try {
                    if (token != generation) return;
                    int source = ((ChannelAccessor) channel).jellyparty$getSource();
                    int state = AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE);
                    double seconds = AL10.alGetSourcef(source, AL11.AL_SEC_OFFSET);
                    cursor = new Cursor(seconds, System.nanoTime(), state);
                } finally {
                    if (token == generation) queryQueued.set(false);
                }
            });
        }
        Cursor current = cursor;
        if (current != null) {
            if (current.sourceState == AL10.AL_PLAYING) {
                started = true;
                status = Status.PLAYING;
            } else if (current.sourceState == AL10.AL_PAUSED && started) {
                status = Status.PAUSED;
            } else if (current.sourceState == AL10.AL_STOPPED && started) {
                status = Status.FINISHED;
            }
        }
        if (started && (handle == null || handle.isStopped() || !client.getSoundManager().isActive(sound))) {
            status = Status.FINISHED;
        } else if (!started && System.nanoTime() - requestedAt > 8_000_000_000L) {
            status = Status.FAILED;
        }
        return status;
    }

    public double seconds(boolean gamePaused) {
        Cursor current = cursor;
        if (current == null) return 0;
        double extra = current.sourceState == AL10.AL_PLAYING && !gamePaused
                ? Math.min(0.15, (System.nanoTime() - current.sampledAt) / 1_000_000_000.0) : 0;
        return current.seconds + Math.max(0, extra);
    }

    public void stop(Minecraft client) {
        generation++;
        queryQueued.set(false);
        if (sound != null) client.getSoundManager().stop(sound);
        sound = null;
        status = Status.FINISHED;
    }

    private static final class LocalSong extends AbstractSoundInstance {
        private LocalSong(float gain) {
            super(Identifier.fromNamespaceAndPath("jellyparty", "party"), SoundSource.RECORDS,
                    SoundInstance.createUnseededRandom());
            volume = gain;
            pitch = 1;
            relative = true;
            attenuation = SoundInstance.Attenuation.NONE;
            looping = false;
        }
    }
}
