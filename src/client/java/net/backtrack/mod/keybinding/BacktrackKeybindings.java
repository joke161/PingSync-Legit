package net.backtrack.mod.keybinding;

import net.backtrack.mod.config.BacktrackConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class BacktrackKeybindings {
	public static KeyMapping toggleKeyBinding;

	public static void register() {
		toggleKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.backtrack.toggle",
			GLFW.GLFW_KEY_B,
			"category.backtrack.mod"
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (toggleKeyBinding.consumeClick()) {
				BacktrackConfig.enabled = !BacktrackConfig.enabled;
				if (client.player != null) {
					client.player.sendSystemMessage(Component.literal(
						"Backtrack rendering: " + (BacktrackConfig.enabled ? "ENABLED" : "DISABLED")
					));
				}
			}
		});
	}
}
