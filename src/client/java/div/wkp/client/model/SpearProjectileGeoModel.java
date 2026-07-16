package div.wkp.client.model;

import div.wkp.WKPerks;
import div.wkp.artifact.ArtifactSpearItem;
import div.wkp.entity.SpearProjectileEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class SpearProjectileGeoModel extends GeoModel<SpearProjectileEntity> {
    private static final Identifier MODEL = Identifier.of(WKPerks.MOD_ID, "geo/artifact_spear.geo.json");
    private static final Identifier ANIMATION = Identifier.of(WKPerks.MOD_ID, "animations/artifact_spear.animation.json");

    @Override
    public Identifier getModelResource(SpearProjectileEntity animatable) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(SpearProjectileEntity animatable) {
        return ArtifactSpearItem.getTextureForStack(animatable.getStack());
    }

    @Override
    public Identifier getAnimationResource(SpearProjectileEntity animatable) {
        return ANIMATION;
    }
}
