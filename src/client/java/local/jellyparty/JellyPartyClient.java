package local.jellyparty;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JellyPartyClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("jellyparty");
    public static final DanceController CONTROLLER = new DanceController();
    public static KeyMapping toggleKey;

    @Override
    public void onInitializeClient() {
        DanceConfig.load();
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("jellyparty", "party"));
        toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.jellyparty.toggle",
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, category));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.consumeClick()) {
                if (client.gui.screen() == null) CONTROLLER.toggle(client);
            }
            CONTROLLER.tick(client);
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(CONTROLLER::reset);
        LOGGER.info("Jelly Party ready: client-only, J to toggle, rotation cue 10.24s");
    }
}
