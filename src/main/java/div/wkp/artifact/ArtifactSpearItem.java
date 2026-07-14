package div.wkp.artifact;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class ArtifactSpearItem extends ArtifactItem {
    private static final int ENERGY_COST = 1;

    public ArtifactSpearItem(Settings settings) {
        super(settings, 20, 1, 8, true);
    }

    @Override
    protected TypedActionResult<ItemStack> onArtifactUse(
            World world,
            PlayerEntity user,
            Hand hand,
            ItemStack stack
    ) {
        TypedActionResult<ItemStack> result =
                finishActivation(world, user, stack, ENERGY_COST);

        if (result.getResult().isAccepted() && !world.isClient) {
            user.sendMessage(
                    Text.literal("Копьё пока ждёт своей полноценной механики.")
                            .formatted(Formatting.GRAY),
                    true
            );
        }

        return result;
    }
}
