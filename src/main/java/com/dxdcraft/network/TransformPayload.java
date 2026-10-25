package com.dxdcraft.network;

import com.dxdcraft.DxDCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server -> clients: player {@code entityId} is now in this armored form (or none). */
public record TransformPayload(int entityId, String gear, int mode) implements CustomPacketPayload {
    public static final int MODE_NONE = 0, MODE_BALANCE_BREAKER = 1, MODE_JD = 2, MODE_JD_COMPLETE = 3;

    public static final Type<TransformPayload> TYPE = new Type<>(DxDCraft.id("transform"));
    public static final StreamCodec<ByteBuf, TransformPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TransformPayload::entityId,
            ByteBufCodecs.STRING_UTF8, TransformPayload::gear,
            ByteBufCodecs.VAR_INT, TransformPayload::mode,
            TransformPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TransformPayload payload, IPayloadContext context) {
        if (!FMLEnvironment.dist.isClient()) return;
        context.enqueueWork(() -> com.dxdcraft.client.TransformState.accept(payload));
    }
}
