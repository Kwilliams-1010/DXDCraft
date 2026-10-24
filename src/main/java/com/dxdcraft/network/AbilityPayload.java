package com.dxdcraft.network;

import com.dxdcraft.DxDCraft;
import com.dxdcraft.logic.Abilities;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client -> server: the player pressed the Ultimate or Racial Ability key. */
public record AbilityPayload(int action) implements CustomPacketPayload {
    public static final Type<AbilityPayload> TYPE = new Type<>(DxDCraft.id("ability"));
    public static final StreamCodec<ByteBuf, AbilityPayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(AbilityPayload::new, AbilityPayload::action);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AbilityPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !player.isAlive()) return;
            if (!com.dxdcraft.logic.DxDLogic.data(player).allowAbilityPacket(player.level().getGameTime(),
                    com.dxdcraft.DxDConfig.get(com.dxdcraft.DxDConfig.MAX_ABILITY_PACKETS_PER_SECOND::get, 10))) return;
            if (com.dxdcraft.logic.DxDLogic.data(player).inCutscene()) return; // no casting mid-cutscene
            switch (payload.action()) {
                case Abilities.ACTION_ULTIMATE -> Abilities.ultimateKey(player);
                case Abilities.ACTION_RACE_ABILITY -> Abilities.raceAbility(player);
                case Abilities.ACTION_ELEMENT_1 -> com.dxdcraft.logic.ElementCasting.cast(player, 0);
                case Abilities.ACTION_ELEMENT_2 -> com.dxdcraft.logic.ElementCasting.cast(player, 1);
                case Abilities.ACTION_JUGGERNAUT -> com.dxdcraft.logic.JuggernautDrive.tryActivate(player);
                default -> {}
            }
        });
    }
}
