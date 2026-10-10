package local.jellyparty;

/** Immutable parameters copied into an entity's render state. */
public record DancePose(float widthScale, float heightScale, float lift, float rotationDegrees) {
    public static final DancePose IDENTITY = new DancePose(1, 1, 0, 0);

    public DancePose fade(double weight) {
        float w = (float) DanceMath.clamp(weight, 0, 1);
        if (w == 0) return IDENTITY;
        // Equivalent shortest angle avoids unwinding dozens of full turns on stop.
        float angle = (float) Math.IEEEremainder(rotationDegrees, 360);
        return new DancePose(1 + (widthScale - 1) * w, 1 + (heightScale - 1) * w,
                lift * w, angle * w);
    }
}
