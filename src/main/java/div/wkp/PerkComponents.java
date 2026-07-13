package div.wkp;

import div.wkp.component.PerkComponent;
import div.wkp.component.PlayerPerkComponent;
import net.minecraft.util.Identifier;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentInitializer;
import org.ladysnake.cca.api.v3.entity.RespawnCopyStrategy;

public final class PerkComponents implements EntityComponentInitializer {
    public static final ComponentKey<PerkComponent> PERK_COMPONENT =
            ComponentRegistry.getOrCreate(
                    Identifier.of("wkperks", "perks"),
                    PerkComponent.class
            );

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        registry.registerForPlayers(
                PERK_COMPONENT,
                PlayerPerkComponent::new,
                RespawnCopyStrategy.ALWAYS_COPY
        );
    }
}