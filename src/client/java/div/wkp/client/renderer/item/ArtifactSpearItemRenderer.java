package div.wkp.client.renderer.item;

import div.wkp.artifact.ArtifactSpearItem;
import div.wkp.client.model.ArtifactSpearItemGeoModel;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class ArtifactSpearItemRenderer extends GeoItemRenderer<ArtifactSpearItem> {
    public ArtifactSpearItemRenderer() {
        super(new ArtifactSpearItemGeoModel());
    }

    @Override
    public Identifier getTextureLocation(ArtifactSpearItem animatable) {
        return ArtifactSpearItem.getTextureForStack(this.currentItemStack);
    }
}
