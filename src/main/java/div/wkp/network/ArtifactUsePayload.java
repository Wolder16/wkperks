package div.wkp.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

public record ArtifactUsePayload(Hand hand, Action action) implements CustomPayload {
    public static final CustomPayload.Id<ArtifactUsePayload> ID =
            new CustomPayload.Id<>(Identifier.of("wkperks", "artifact_use"));

    public static final PacketCodec<RegistryByteBuf, ArtifactUsePayload> CODEC =
            new PacketCodec<>() {
                @Override
                public void encode(RegistryByteBuf buf, ArtifactUsePayload payload) {
                    buf.writeEnumConstant(payload.hand());
                    buf.writeEnumConstant(payload.action());
                }

                @Override
                public ArtifactUsePayload decode(RegistryByteBuf buf) {
                    return new ArtifactUsePayload(
                            buf.readEnumConstant(Hand.class),
                            buf.readEnumConstant(Action.class)
                    );
                }
            };

    public enum Action {
        START,
        RELEASE
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
