package dev.kcyq.remi;

import java.nio.charset.StandardCharsets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** One frame of Remi data: plain UTF-8 text lines, sent by the server's remi_hud.sk as a plugin message. */
public record RemiPayload(String data) implements CustomPacketPayload {
	public static final Type<RemiPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("remi", "hud"));

	public static final StreamCodec<FriendlyByteBuf, RemiPayload> CODEC = StreamCodec.of(
			(buf, payload) -> buf.writeBytes(payload.data().getBytes(StandardCharsets.UTF_8)),
			buf -> {
				byte[] bytes = new byte[buf.readableBytes()];
				buf.readBytes(bytes);
				return new RemiPayload(new String(bytes, StandardCharsets.UTF_8));
			});

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
