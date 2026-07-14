package div.wkp;

import div.wkp.component.ArtifactStateComponent;
import div.wkp.component.PlayerArtifactStateComponent;
import net.minecraft.util.Identifier;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentInitializer;
import org.ladysnake.cca.api.v3.entity.RespawnCopyStrategy;

public final class ArtifactComponents implements EntityComponentInitializer {
    public static final ComponentKey<ArtifactStateComponent> ARTIFACT_STATE_COMPONENT =
            ComponentRegistry.getOrCreate(
                    Identifier.of(WKPerks.MOD_ID, "artifact_state"),
                    ArtifactStateComponent.class
            );

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        registry.registerForPlayers(
                ARTIFACT_STATE_COMPONENT,
                PlayerArtifactStateComponent::new,
                RespawnCopyStrategy.ALWAYS_COPY
        );
    }
}
