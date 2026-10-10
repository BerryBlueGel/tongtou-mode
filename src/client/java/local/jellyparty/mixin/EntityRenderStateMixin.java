package local.jellyparty.mixin;

import local.jellyparty.DancePose;
import local.jellyparty.DanceRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public abstract class EntityRenderStateMixin implements DanceRenderState {
    @Unique private DancePose jellyparty$pose = DancePose.IDENTITY;
    @Override public DancePose jellyparty$getPose() { return jellyparty$pose; }
    @Override public void jellyparty$setPose(DancePose pose) { jellyparty$pose = pose; }
}
