package net.backtrack.mod;

import net.backtrack.mod.command.BacktrackCommands;
import net.backtrack.mod.config.BacktrackConfig;
import net.backtrack.mod.keybinding.BacktrackKeybindings;
import net.backtrack.mod.render.BacktrackRenderer;
import net.backtrack.mod.tracker.BacktrackTracker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BacktrackClientMod implements ClientModInitializer {
	public static final String MOD_ID = "backtrack";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		LOGGER.info("Initializing PingSync (Legit Edition) for Fabric 1.21.1...");

		// Load config settings from JSON
		BacktrackConfig.load();

		// Register keybindings
		BacktrackKeybindings.register();

		// Register /pingsync client command
		BacktrackCommands.register();

		// Register entity tick position tracker
		ClientTickEvents.END_CLIENT_TICK.register(BacktrackTracker::onClientTick);

		// Register 3D renderer
		BacktrackRenderer.register();

		LOGGER.info("PingSync (Legit Edition) successfully initialized!");
	}
}
