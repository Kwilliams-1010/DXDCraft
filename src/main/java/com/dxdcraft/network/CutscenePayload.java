package com.dxdcraft.network;

import com.dxdcraft.DxDCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server -> client: start (or stop) the awakening cutscene for a gear. */
public record CutscenePayload(String gear, boolean start) implements CustomPacketPayload {
    public static final Type<CutscenePayload> TYPE = new Type<>(DxDCraft.id("cutscene"));
    public static final StreamCodec<ByteBuf, CutscenePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, CutscenePayload::gear,
            ByteBufCodecs.BOOL, CutscenePayload::start,
            CutscenePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CutscenePayload payload, IPayloadContext context) {
        if (!net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) return;
        // Only ever received on the client; ClientCutscene is only loaded there.
        context.enqueueWork(() -> {
            if (payload.start()) com.dxdcraft.client.ClientCutscene.start(payload.gear());
            else com.dxdcraft.client.ClientCutscene.stop();
        });
    }
}
