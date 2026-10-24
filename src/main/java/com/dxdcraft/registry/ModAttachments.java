package com.dxdcraft.registry;

import com.dxdcraft.DxDCraft;
import com.dxdcraft.data.DxDData;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, DxDCraft.MODID);

    public static final Supplier<AttachmentType<DxDData>> DXD = ATTACHMENT_TYPES.register("dxd",
            () -> AttachmentType.serializable(() -> new DxDData()).copyOnDeath().build());
}
