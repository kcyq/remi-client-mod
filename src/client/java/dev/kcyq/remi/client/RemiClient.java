package dev.kcyq.remi.client;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

import dev.kcyq.remi.RemiPayload;

public class RemiClient implements ClientModInitializer {
	public static boolean hudOn = true, compassOn = true;
	private static KeyMapping mapKey, hudKey, compassKey;

	private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("remiclient", path); }

	@Override
	public void onInitializeClient() {
		// data from the Remi server
		ClientPlayNetworking.registerGlobalReceiver(RemiPayload.TYPE, (payload, context) -> RemiState.apply(payload.data()));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> { RemiState.reset(); WorldMap.save(); });

		// HUD pieces
		HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, id("essence"), EssenceHud::render);
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, id("dungeon_map"), DungeonMapHud::render);
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, id("party"), PartyHud::party);
		HudElementRegistry.attachElementAfter(VanillaHudElements.BOSS_BAR, id("compass"), CompassHud::render);
		HudElementRegistry.replaceElement(VanillaHudElements.BOSS_BAR, original -> (graphics, tracker) -> {
			// Remi-styled boss bars on Remi; the normal ones anywhere else
			if (RemiState.live() && hudOn) {
				graphics.pose().pushMatrix();
				if (compassOn) graphics.pose().translate(0, 16);
				boolean drawn = PartyHud.bosses(graphics);
				graphics.pose().popMatrix();
				if (drawn) return;
			}
			original.extractRenderState(graphics, tracker);
		});

		// keys
		KeyMapping.Category cat = KeyMapping.Category.register(id("remi"));
		mapKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.remiclient.map", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, cat));
		hudKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.remiclient.hud", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, cat));
		compassKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.remiclient.compass", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, cat));

		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			while (mapKey.consumeClick()) if (mc.screen == null) mc.setScreen(new WorldMapScreen());
			while (hudKey.consumeClick()) hudOn = !hudOn;
			while (compassKey.consumeClick()) compassOn = !compassOn;
			if (mc.level != null && mc.player != null) WorldMap.tick();
		});
	}

	public static Minecraft mc() { return Minecraft.getInstance(); }
}
