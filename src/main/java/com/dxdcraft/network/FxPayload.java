package com.dxdcraft.network;

import com.dxdcraft.DxDCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server -> clients tracking a player: "play this gear effect on that player".
 * One small packet per effect; every client then draws the particles itself, instead of the
 * server streaming thousands of particle packets. kind: see FX_* constants.
 */
public record FxPayload(int entityId, String gear, int kind, int stage) implements CustomPacketPayload {
    public static final int FX_DRAW = 0, FX_SUMMON = 1, FX_STAGE_UP = 2, FX_CUTSCENE = 3, FX_STOP = 4;

    public static final Type<FxPayload> TYPE = new Type<>(DxDCraft.id("fx"));
    public static final StreamCodec<ByteBuf, FxPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, FxPayload::entityId,
            ByteBufCodecs.STRING_UTF8, FxPayload::gear,
            ByteBufCodecs.VAR_INT, FxPayload::kind,
            ByteBufCodecs.VAR_INT, FxPayload::stage,
            FxPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FxPayload payload, IPayloadContext context) {
        if (!FMLEnvironment.dist.isClient()) return;
        context.enqueueWork(() -> com.dxdcraft.client.ClientFx.play(payload));
    }
}
