package div.wkp.mixin;

import div.wkp.PerkComponents;
import div.wkp.PerkUtil;
import div.wkp.perk.perks.HeavyStrikePerk;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class HeavyStrikeDamageMixin {
    /*
     * Защита от повторного входа в damage().
     *
     * Mixin отменяет исходный вызов и один раз запускает его снова
     * с удвоенным количеством урона.
     */
    @Unique
    private boolean wkperks$processingHeavyStrikeDamage = false;

    @Inject(
            method = "damage",
            at = @At("HEAD"),
            cancellable = true
    )
    private void wkperks$applyHeavyStrike(
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (wkperks$processingHeavyStrikeDamage) {
            return;
        }

        Entity attacker = source.getAttacker();

        if (!(attacker instanceof ServerPlayerEntity player)) {
            return;
        }

        LivingEntity victim = (LivingEntity) (Object) this;

        // Не удваиваем урон, если игрок каким-то образом атакует сам себя.
        if (victim == player) {
            return;
        }

        if (!PerkUtil.arePerksEnabled(player)) {
            return;
        }

        if (!PerkComponents.PERK_COMPONENT
                .get(player)
                .hasPerk(HeavyStrikePerk.ID)) {
            return;
        }

        wkperks$processingHeavyStrikeDamage = true;

        try {
            boolean result = victim.damage(
                    source,
                    amount * 2.0F
            );

            cir.setReturnValue(result);
        } finally {
            wkperks$processingHeavyStrikeDamage = false;
        }
    }
}