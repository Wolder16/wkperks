package div.wkp.client.mixin;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(HandledScreen.class)
public interface HandledScreenAccessor {
    @Accessor("x")
    int wkperks$getX();

    @Accessor("y")
    int wkperks$getY();

    @Accessor("backgroundWidth")
    int wkperks$getBackgroundWidth();
}
