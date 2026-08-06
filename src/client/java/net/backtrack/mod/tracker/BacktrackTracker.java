package net.backtrack.mod.tracker;

import net.backtrack.mod.config.BacktrackConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BacktrackTracker {
	private static final Map<UUID, Deque<PlayerPositionRecord>> entityHistory = new ConcurrentHashMap<>();

	public static void onClientTick(Minecraft client) {
		if (client.level == null || client.player == null) {
			entityHistory.clear();
			return;
		}

		long now = System.currentTimeMillis();
		LocalPlayer localPlayer = client.player;

		int maxCap = 200; // History buffer cap (up to 10 seconds)

		for (Entity entity : client.level.entitiesForRendering()) {
			if (entity == localPlayer || !entity.isAlive()) {
				entityHistory.remove(entity.getUUID());
				continue;
			}

			if (!shouldTrackEntity(entity)) {
				entityHistory.remove(entity.getUUID());
				continue;
			}

			Deque<PlayerPositionRecord> history = entityHistory.computeIfAbsent(entity.getUUID(), k -> new ArrayDeque<>());

			// Age existing records by 1 tick
			Deque<PlayerPositionRecord> updatedHistory = new ArrayDeque<>();
			for (PlayerPositionRecord record : history) {
				int newAge = record.ageTicks() + 1;
				if (newAge <= maxCap) {
					updatedHistory.addLast(new PlayerPositionRecord(
						record.boundingBox(),
						record.position(),
						record.timestampMs(),
						newAge
					));
				}
			}

			// Add current position snapshot (age = 0)
			updatedHistory.addFirst(new PlayerPositionRecord(
				entity.getBoundingBox(),
				entity.position(),
				now,
				0
			));

			entityHistory.put(entity.getUUID(), updatedHistory);
		}
	}

	public static boolean shouldTrackEntity(Entity entity) {
		if (entity instanceof Player) {
			return BacktrackConfig.trackPlayers;
		} else if (entity instanceof Enemy) {
			return BacktrackConfig.trackHostile;
		} else if (entity instanceof Animal || entity instanceof AmbientCreature) {
			return BacktrackConfig.trackPassive;
		} else {
			return BacktrackConfig.trackOther;
		}
	}

	public static List<PlayerPositionRecord> getValidRecordsForEntity(Entity entity) {
		Deque<PlayerPositionRecord> history = entityHistory.get(entity.getUUID());
		if (history == null || history.isEmpty()) {
			return Collections.emptyList();
		}

		// Exact Server Lag Compensation Match (Your Ping RTT / 50ms)
		int totalPingMs = getTotalBacktrackPingMs(entity);
		int targetTicks = Math.max(1, Math.min(12, (int) Math.round(totalPingMs / 50.0)));

		List<PlayerPositionRecord> validRecords = new ArrayList<>();
		for (PlayerPositionRecord record : history) {
			if (record.ageTicks() > 0 && record.ageTicks() <= targetTicks) {
				validRecords.add(record);
			}
		}

		return validRecords;
	}

	public static int getTotalBacktrackPingMs(Entity targetEntity) {
		Minecraft client = Minecraft.getInstance();
		int yourPing = client.player != null ? getEntityPingMs(client.player) : 0;

		// Safe default fallback if tab list ping packet not received
		if (yourPing <= 0) yourPing = 50;

		// Strictly your own real RTT ping to perfectly match server-side lag compensation rewind
		return yourPing;
	}

	public static int getEntityPingMs(Entity entity) {
		if (entity instanceof Player player) {
			Minecraft client = Minecraft.getInstance();
			ClientPacketListener connection = client.getConnection();
			if (connection != null) {
				PlayerInfo info = connection.getPlayerInfo(player.getUUID());
				if (info != null) {
					return Math.max(0, info.getLatency());
				}
			}
		}
		return 0;
	}

	public static Map<UUID, Deque<PlayerPositionRecord>> getAllHistory() {
		return entityHistory;
	}
}
