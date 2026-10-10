package local.jellyparty;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import org.lwjgl.glfw.GLFW;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public final class JellyPartyGameTest implements FabricClientGameTest {
    private UUID pigId;
    private UUID farPigId;
    private UUID remotePlayerId;
    private final Map<String, Object> evidence = new LinkedHashMap<>();

    @Override
    public void runTest(ClientGameTestContext context) {
        JellyPartyClient.LOGGER.info("TEST: mathematical animation invariants");
        testMath();
        testConfigUpgrade();
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getServer().runCommand("time set day");
            world.getServer().runCommand("fill -16 79 -16 16 79 16 minecraft:grass_block");
            world.getServer().runCommand("fill -16 80 -16 16 87 16 minecraft:air");
            world.getServer().runCommand("gamemode creative @a");
            world.getServer().runCommand("tp @a 0 80 9 180 12");
            world.getServer().runOnServer(server -> {
                var level = server.overworld();
                pigId = spawn(level, EntityTypes.PIG, -3, 0).getUUID();
                spawn(level, EntityTypes.COW, -1, 0);
                spawn(level, EntityTypes.VILLAGER, 1, 0);
                spawn(level, EntityTypes.WARDEN, 4, 0);
                spawn(level, EntityTypes.CHICKEN, 0, 3);
                farPigId = spawn(level, EntityTypes.PIG, 35, 0).getUUID();
            });
            world.getClientLevel().waitForChunksRender();
            context.runOnClient(client -> {
                client.options.pauseOnLostFocus = false;
                client.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(1.0);
                client.options.getSoundSourceOptionInstance(SoundSource.RECORDS).set(0.3);
                client.options.renderDistance().set(5);
            });
            context.getInput().lookAt(new BlockPos(0, 81, 0));
            context.waitTicks(10);
            remotePlayerId = context.computeOnClient(client -> {
                RemotePlayer other = new RemotePlayer(client.level, new GameProfile(UUID.randomUUID(), "JellyGuest"));
                other.setId(-1000);
                other.setPos(2, 80, 3);
                other.setYRot(0);
                other.setNoGravity(true);
                client.level.addEntity(other);
                return other.getUUID();
            });
            context.takeScreenshot("01-normal");
            check(context.computeOnClient(c -> pose(c, pigId).equals(DancePose.IDENTITY)), "inactive mob changed");

            // Exercise the configurable key through Fabric's actual input path.
            context.runOnClient(client -> {
                JellyPartyClient.toggleKey.setKey(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_K));
                KeyMapping.resetMapping();
            });
            context.getInput().holdKey(GLFW.GLFW_KEY_K);
            context.waitTicks(4);
            context.getInput().releaseKey(GLFW.GLFW_KEY_K);
            context.waitFor(c -> JellyPartyClient.CONTROLLER.isAudible(), 240);
            context.runOnClient(c -> { playerPose(c); pose(c, remotePlayerId); });
            check(context.computeOnClient(c -> JellyPartyClient.CONTROLLER.isRequested()), "held key toggled repeatedly");
            evidence.put("remappedKeyAndHold", "passed");
            context.waitFor(c -> JellyPartyClient.CONTROLLER.songSeconds(c) >= 2.1, 240);
            check(context.computeOnClient(c -> Math.abs(pose(c, pigId).rotationDegrees()) < 0.001), "spin started before lyric");
            check(context.computeOnClient(c -> pose(c, farPigId).equals(DancePose.IDENTITY)), "distant mob animated");
            check(context.computeOnClient(c -> !playerPose(c).equals(DancePose.IDENTITY)
                    && Math.abs(playerPose(c).rotationDegrees()) < 0.001), "self pre-cue bounce failed");
            check(context.computeOnClient(c -> !pose(c, remotePlayerId).equals(DancePose.IDENTITY)
                    && Math.abs(pose(c, remotePlayerId).rotationDegrees()) < 0.001), "remote player pre-cue bounce failed");
            context.takeScreenshot("02-bounce-only");
            JellyPartyClient.LOGGER.info("TEST: key binding, pre-cue bounce and entity filters passed");
            evidence.put("preCueBounceForMobsAndBothPlayersAndRange", "passed");
            float playerYaw = context.computeOnClient(c -> c.player.getYRot());
            float playerPitch = context.computeOnClient(c -> c.player.getXRot());

            context.waitFor(c -> JellyPartyClient.CONTROLLER.songSeconds(c) >= 10.6, 500);
            check(context.computeOnClient(c -> Math.abs(pose(c, pigId).rotationDegrees()) > 5), "spin did not start at lyric");
            check(context.computeOnClient(c -> Math.abs(playerPose(c).rotationDegrees()) > 5), "self spin missing");
            check(context.computeOnClient(c -> Math.abs(pose(c, remotePlayerId).rotationDegrees()) > 5), "remote player spin missing");
            var cameraRotation = context.computeOnClient(c -> new org.joml.Quaternionf(c.gameRenderer.mainCamera().rotation()));
            context.takeScreenshot("03-bounce-and-spin");
            for (int i = 0; i < 12; i++) {
                context.takeScreenshot("spin-frame-" + i);
                context.waitTicks(2);
            }
            JellyPartyClient.LOGGER.info("TEST: post-cue rotation passed");
            evidence.put("postCueRotation", "passed");
            check(context.computeOnClient(c -> Math.abs(c.player.getYRot() - playerYaw) < 0.001
                    && Math.abs(c.player.getXRot() - playerPitch) < 0.001), "dance changed player yaw/pitch");
            check(context.computeOnClient(c -> Math.abs(cameraRotation.dot(c.gameRenderer.mainCamera().rotation())) > 0.99999),
                    "first person camera rotated with dance");
            evidence.put("localAndRemotePlayerRotation", "passed (local RemotePlayer fixture)");
            evidence.put("firstPersonCameraAndPlayerYawPitchUnchanged", "passed");

            // Use the real F5 input path, then front third-person to see the local model.
            context.getInput().pressKey(options -> options.keyTogglePerspective);
            context.waitTicks(5);
            check(context.computeOnClient(c -> c.options.getCameraType() == CameraType.THIRD_PERSON_BACK), "F5 failed");
            context.getInput().pressKey(options -> options.keyTogglePerspective);
            context.waitTicks(5);
            check(context.computeOnClient(c -> c.options.getCameraType() == CameraType.THIRD_PERSON_FRONT), "front F5 failed");
            var thirdPersonRotation = context.computeOnClient(c -> new org.joml.Quaternionf(c.gameRenderer.mainCamera().rotation()));
            for (int i = 0; i < 12; i++) {
                context.takeScreenshot("self-frame-" + i);
                context.waitTicks(2);
            }
            check(context.computeOnClient(c -> Math.abs(thirdPersonRotation.dot(c.gameRenderer.mainCamera().rotation())) > 0.99999),
                    "third person camera rotated with dance");
            evidence.put("F5LocalModelAndFixedThirdPersonCamera", "passed with screenshots");
            context.getInput().pressKey(options -> options.keyTogglePerspective);
            context.waitTicks(5);
            check(context.computeOnClient(c -> c.options.getCameraType() == CameraType.FIRST_PERSON), "return to first person failed");

            context.setScreen(() -> new PauseScreen(true));
            context.waitTicks(8);
            double pauseAt = context.computeOnClient(c -> JellyPartyClient.CONTROLLER.songSeconds(c));
            context.waitTicks(25);
            double pauseEnd = context.computeOnClient(c -> JellyPartyClient.CONTROLLER.songSeconds(c));
            check(Math.abs(pauseEnd - pauseAt) < 0.10, "music clock advanced during singleplayer pause");
            context.setScreen(() -> null);
            context.waitFor(c -> !c.isPaused(), 120);
            context.waitTicks(5);
            evidence.put("pauseCursorDeltaSeconds", pauseEnd - pauseAt);

            context.getInput().pressKey(GLFW.GLFW_KEY_K);
            context.waitTicks(10);
            check(context.computeOnClient(c -> !JellyPartyClient.CONTROLLER.isRequested()), "second press did not stop");
            check(context.computeOnClient(c -> pose(c, pigId).equals(DancePose.IDENTITY)), "stop did not restore model");
            check(context.computeOnClient(c -> playerPose(c).equals(DancePose.IDENTITY)
                    && pose(c, remotePlayerId).equals(DancePose.IDENTITY)), "stop did not restore players");
            context.takeScreenshot("04-stopped-restored");
            evidence.put("manualStopRestores", "passed");
            JellyPartyClient.LOGGER.info("TEST: pause and manual stop passed; testing full audio duration");

            context.getInput().pressKey(GLFW.GLFW_KEY_K);
            context.waitFor(c -> JellyPartyClient.CONTROLLER.isAudible(), 240);
            check(context.computeOnClient(c -> JellyPartyClient.CONTROLLER.songSeconds(c) < 1), "restart did not start at zero");
            context.waitFor(c -> !JellyPartyClient.CONTROLLER.isRequested(), 2200);
            context.waitTicks(10);
            check(context.computeOnClient(c -> pose(c, pigId).equals(DancePose.IDENTITY)), "natural end did not restore model");
            evidence.put("restartFromZeroAndNaturalEnd", "passed");
            evidence.put("serverMobPosition", world.getServer().computeOnServer(server -> {
                Entity pig = server.overworld().getEntity(pigId);
                check(Math.abs(pig.getX() + 3) < 0.01 && Math.abs(pig.getY() - 80) < 0.01 && Math.abs(pig.getZ()) < 0.01,
                        "server entity moved during visual animation");
                return pig.position().toString();
            }));

            // Start once more, then leave the world to verify disconnect cleanup.
            context.getInput().pressKey(GLFW.GLFW_KEY_K);
            context.waitFor(c -> JellyPartyClient.CONTROLLER.isAudible(), 240);
        }
        context.waitTicks(5);
        check(context.computeOnClient(c -> !JellyPartyClient.CONTROLLER.isRequested()), "world exit did not clean up");
        evidence.put("worldExitCleanup", "passed");
        JellyPartyClient.LOGGER.info("TEST: natural end and world exit passed");
        evidence.put("minecraft", "26.2");
        evidence.put("spinCueSeconds", 10.24);
        evidence.put("spinDegreesPerSecond", 240);
        try {
            Path output = Path.of("jellyparty-test-evidence.json");
            Files.writeString(output, new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(evidence), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new AssertionError("Could not save test evidence", e);
        }
    }

    private static Mob spawn(net.minecraft.server.level.ServerLevel level, EntityType<? extends Mob> type, double x, double z) {
        Mob mob = type.create(level, EntitySpawnReason.COMMAND);
        check(mob != null, "could not create test mob");
        mob.setPos(x, 80, z);
        mob.setYRot(0);
        mob.setNoAi(true);
        mob.setInvulnerable(true);
        mob.setPersistenceRequired();
        check(level.addFreshEntity(mob), "could not spawn test mob");
        return mob;
    }

    private static DancePose pose(Minecraft client, UUID id) {
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity.getUUID().equals(id)) {
                EntityRenderState state = client.getEntityRenderDispatcher().extractEntity(entity, 0);
                return ((DanceRenderState) state).jellyparty$getPose();
            }
        }
        throw new AssertionError("Test mob is not loaded: " + id);
    }

    private static DancePose playerPose(Minecraft client) {
        return ((DanceRenderState) client.getEntityRenderDispatcher().extractEntity(client.player, 0)).jellyparty$getPose();
    }

    private static void testMath() {
        DanceConfig config = new DanceConfig();
        check(Math.abs((DanceMath.pose(13, config).rotationDegrees() - DanceMath.pose(12, config).rotationDegrees() + 360) % 360 - 240) < 0.01,
                "default spin speed is not 240 degrees per second");
        for (int i = 0; i < 6000; i++) {
            DancePose p = DanceMath.pose(i / 100.0, config);
            check(Float.isFinite(p.widthScale()) && Float.isFinite(p.heightScale()), "nonfinite scaling");
            check(p.heightScale() >= 0.64 && p.heightScale() <= 1.25 && p.lift() >= 0, "invalid bounce bounds");
            check(Math.abs(p.widthScale() * p.widthScale() * p.heightScale() - 1) < 0.00001, "volume preservation failed");
            if (i / 100.0 <= config.spinStartSeconds) check(p.rotationDegrees() == 0, "early rotation");
        }
        check(DanceMath.rangeWeight(0, 24) == 1 && DanceMath.rangeWeight(24, 24) == 0, "invalid range fade");
        check(DanceMath.pose(12, config).fade(0).equals(DancePose.IDENTITY), "fade does not return identity");
        RangeTransition entry = new RangeTransition(DancePose.IDENTITY, new DancePose(1, 1, 0, 179), 0);
        DancePose beforeWrap = entry.inside(new DancePose(1, 1, 0, 179), 0.09, 0.2);
        DancePose afterWrap = entry.inside(new DancePose(1, 1, 0, -179), 0.10, 0.2);
        check(Math.abs(afterWrap.rotationDegrees() - beforeWrap.rotationDegrees()) < 20, "range entry snapped at angle wrap");
        entry.outside(0.11, 0.2);
        check(entry.outside(0.32, 0.2).equals(DancePose.IDENTITY), "range exit did not restore identity");
    }

    private void testConfigUpgrade() {
        try {
            Path path = Path.of("jellyparty-config-tests", "legacy.json");
            Files.createDirectories(path.getParent());
            Files.writeString(path, "{\"radius\":31,\"spinStartSeconds\":9,\"spinDegreesPerSecond\":180}");
            DanceConfig migrated = DanceConfig.load(path);
            check(migrated.spinDegreesPerSecond == 240 && migrated.radius == 31 && migrated.spinStartSeconds == 9,
                    "legacy default upgrade lost other settings");
            check(Files.readString(path).contains("configVersion"), "upgrade was not saved");
            Files.writeString(path, "{\"spinDegreesPerSecond\":225}");
            check(DanceConfig.load(path).spinDegreesPerSecond == 225, "custom legacy speed changed");
            Files.writeString(path, "{\"configVersion\":1,\"spinDegreesPerSecond\":180}");
            check(DanceConfig.load(path).spinDegreesPerSecond == 180, "explicit new speed changed on reload");
            evidence.put("legacyDefaultMigrationAndCustomSpeedPreservation", "passed");
        } catch (java.io.IOException e) {
            throw new AssertionError("Config migration test failed", e);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

