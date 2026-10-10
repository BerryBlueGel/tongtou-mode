package local.jellyparty;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public final class RenderTransform {
    private RenderTransform() { }

    public static void apply(EntityRenderState state, PoseStack matrices) {
        DancePose pose = ((DanceRenderState) state).jellyparty$getPose();
        if (pose.equals(DancePose.IDENTITY)) return;
        // Called inside the renderer's own push/pop, at its foot-origin.
        matrices.translate(0, pose.lift(), 0);
        matrices.mulPose(Axis.YP.rotationDegrees(pose.rotationDegrees()));
        matrices.scale(pose.widthScale(), pose.heightScale(), pose.widthScale());
    }
}
