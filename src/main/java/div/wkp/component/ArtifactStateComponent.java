package div.wkp.component;

import net.minecraft.item.ItemStack;
import org.ladysnake.cca.api.v3.component.Component;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

import java.util.List;

public interface ArtifactStateComponent extends Component, AutoSyncedComponent {
    record StoredStack(int slot, ItemStack stack) {
    }

    List<StoredStack> getStoredSoulboundArtifacts();

    void storeSoulboundArtifact(int slot, ItemStack stack);

    void clearStoredSoulboundArtifacts();

    boolean hasActiveSpear();

    String getActiveSpearUuid();

    int getActiveSpearEntityId();

    int getActiveSpearSlot();

    void setActiveSpear(String spearUuid, int entityId, int slot);

    void clearActiveSpear();

    boolean isUsingArtifact();

    String getActiveArtifactId();

    net.minecraft.util.Hand getActiveHand();

    int getUseTicks();

    boolean isCharged();

    void startUsingArtifact(String artifactId, net.minecraft.util.Hand hand);

    void tickUsingArtifact();

    void setCharged(boolean charged);

    void stopUsingArtifact();
}
