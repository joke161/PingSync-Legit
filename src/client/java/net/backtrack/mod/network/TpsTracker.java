package net.backtrack.mod.network;

import java.util.ArrayDeque;
import java.util.Deque;

public class TpsTracker {

	private static long lastWorldTimePacketMs = 0;
	private static final Deque<Float> timeDiffs = new ArrayDeque<>();
	private static float currentTps = 20.0f;

	public static void onWorldTimePacket() {
		long now = System.currentTimeMillis();
		if (lastWorldTimePacketMs > 0) {
			long diff = now - lastWorldTimePacketMs;
			if (diff > 0) {
				// Minecraft server sends ClientboundSetTimePacket once every 20 ticks (1 second).
				// So 20 ticks elapsed during 'diff' milliseconds.
				// Calculated TPS = (20 ticks * 1000 ms) / diff_ms = 20000.0f / diff
				float instantTps = 20000.0f / diff;
				timeDiffs.addLast(instantTps);
				while (timeDiffs.size() > 10) {
					timeDiffs.pollFirst();
				}

				float sum = 0;
				for (float tps : timeDiffs) {
					sum += tps;
				}
				currentTps = Math.max(5.0f, Math.min(20.0f, sum / timeDiffs.size()));
			}
		}
		lastWorldTimePacketMs = now;
	}

	public static float getTps() {
		return currentTps;
	}

	public static void reset() {
		lastWorldTimePacketMs = 0;
		timeDiffs.clear();
		currentTps = 20.0f;
	}
}
