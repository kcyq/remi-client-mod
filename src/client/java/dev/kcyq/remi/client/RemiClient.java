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
	public static boolean hudOn = true;
	private static KeyMapping menuKey, mapKey, hudKey;

	private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("remiclient", path); }

	private static boolean on(String module) { Module m = Modules.byId(module); return m != null && m.on(); }

	@Override
	public void onInitializeClient() {
		RemiConfig.get();

		// data from the Remi server
		ClientPlayNetworking.registerGlobalReceiver(RemiPayload.TYPE, (payload, context) -> RemiState.apply(payload.data()));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> { RemiState.reset(); WorldMap.save(); });

		// every movable module, each where you put it (HUD editor)
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, id("modules"), (g, dt) -> {
			Minecraft mc = Minecraft.getInstance();
			if (!hudOn || mc.options.hideGui || mc.screen instanceof HudEditorScreen) return;
			for (Module m : Modules.ALL) if (m.movable && m.on() && m.visible()) m.render(g, false);
		});

		// your crosshair instead of the vanilla one
		HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, original -> (g, tracker) -> {
			Minecraft mc = Minecraft.getInstance();
			if (on("crosshair") && mc.options.getCameraType().isFirstPerson() && !mc.options.hideGui) CrosshairModule.renderCentered(g);
			else original.extractRenderState(g, tracker);
		});

		// Remi-styled boss bars on Remi; the normal ones anywhere else
		HudElementRegistry.replaceElement(VanillaHudElements.BOSS_BAR, original -> (g, tracker) -> {
			if (RemiState.live() && hudOn && on("bosses")) {
				g.pose().pushMatrix();
				if (on("compass")) g.pose().translate(0, 26);
				boolean drawn = PartyHud.bosses(g);
				g.pose().popMatrix();
				if (drawn) return;
			}
			original.extractRenderState(g, tracker);
		});

		// keys
		KeyMapping.Category cat = KeyMapping.Category.register(id("remi"));
		menuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.remiclient.menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, cat));
		mapKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.remiclient.map", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, cat));
		hudKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.remiclient.hud", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, cat));

		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			while (menuKey.consumeClick()) if (mc.screen == null) mc.setScreen(new RemiMenuScreen());
			while (mapKey.consumeClick()) if (mc.screen == null) mc.setScreen(new WorldMapScreen());
			while (hudKey.consumeClick()) hudOn = !hudOn;
			if (mc.level != null && mc.player != null) WorldMap.tick();
		});
	}
}
