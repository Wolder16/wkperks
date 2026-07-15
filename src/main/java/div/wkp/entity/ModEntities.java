package div.wkp.entity;

import div.wkp.WKPerks;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModEntities {
    private ModEntities() {
    }

    public static final EntityType<SpearProjectileEntity> SPEAR_PROJECTILE =
            Registry.register(
                    Registries.ENTITY_TYPE,
                    Identifier.of(WKPerks.MOD_ID, "spear_projectile"),
                    EntityType.Builder
                            .<SpearProjectileEntity>create(SpearProjectileEntity::new, SpawnGroup.MISC)
                            .dimensions(0.5F, 0.5F)
                            .maxTrackingRange(64)
                            .trackingTickInterval(10)
                            .build("spear_projectile")
            );

    public static void initialize() {
        WKPerks.LOGGER.info("Registering WKPerks entities");
    }
}
