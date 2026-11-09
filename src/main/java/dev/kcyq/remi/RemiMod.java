package dev.kcyq.remi;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class RemiMod implements ModInitializer {
	public static final String ID = "remiclient";

	@Override
	public void onInitialize() {
		// the Remi server talks to the mod on the "remi:hud" channel
		PayloadTypeRegistry.clientboundPlay().register(RemiPayload.TYPE, RemiPayload.CODEC);
	}
}
