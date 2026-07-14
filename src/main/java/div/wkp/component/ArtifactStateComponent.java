package div.wkp.component;

import net.minecraft.item.ItemStack;
import org.ladysnake.cca.api.v3.component.Component;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

import java.util.List;

public interface ArtifactStateComponent extends Component, AutoSyncedComponent {
    List<ItemStack> getStoredSoulboundArtifacts();

    void storeSoulboundArtifact(ItemStack stack);

    void clearStoredSoulboundArtifacts();
}
