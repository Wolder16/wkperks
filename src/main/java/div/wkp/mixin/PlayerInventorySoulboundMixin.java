package div.wkp.mixin;

import div.wkp.artifact.ArtifactUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerInventory.class)
public abstract class PlayerInventorySoulboundMixin {
    @Shadow
    @Final
    public PlayerEntity player;

    @Inject(method = "dropAll", at = @At("HEAD"))
    private void wkperks$preserveSoulboundArtifacts(CallbackInfo ci) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            ArtifactUtil.storeSoulboundArtifacts(serverPlayer);
        }
    }
}
