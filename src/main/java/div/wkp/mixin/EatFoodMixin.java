package div.wkp.mixin;

import div.wkp.PerkComponents;
import div.wkp.PerkUtil;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class EatFoodMixin {

    @Inject(method = "eatFood", at = @At("TAIL"))
    private void wkperks$onEatFood(World world, ItemStack stack, FoodComponent foodComponent, CallbackInfoReturnable<ItemStack> cir) {
        LivingEntity self = (LivingEntity)(Object) this;
        if (!world.isClient && self instanceof ServerPlayerEntity player) {
            var comp = PerkComponents.PERK_COMPONENT.get(player);
            if (comp.hasPerk("unstoppable")) {
                comp.addTempJumps(1);
                if (!PerkUtil.arePerksEnabled(player)) {
                    return;
                }
            }
        }
    }
}