package local.jellyparty;

/** Makes entering/leaving the radius continuous, including across 360-degree wraps. */
final class RangeTransition {
    private final DancePose initial;
    private final double enteredAt;
    private final double targetAngle;
    private double previousAngle;
    private double angleTravel;
    private double exitedAt = -1;
    private DancePose exitPose;
    private DancePose lastPose;

    RangeTransition(DancePose initial, DancePose target, double now) {
        this.initial = initial;
        this.enteredAt = now;
        this.targetAngle = initial.rotationDegrees()
                + Math.IEEEremainder(target.rotationDegrees() - initial.rotationDegrees(), 360);
        this.previousAngle = target.rotationDegrees();
        this.lastPose = initial;
    }

    DancePose inside(DancePose target, double now, double duration) {
        angleTravel += Math.IEEEremainder(target.rotationDegrees() - previousAngle, 360);
        previousAngle = target.rotationDegrees();
        double w = DanceMath.smooth((now - enteredAt) / duration);
        if (w >= 1) {
            lastPose = target;
        } else {
            lastPose = new DancePose(
                    (float) (initial.widthScale() + (target.widthScale() - initial.widthScale()) * w),
                    (float) (initial.heightScale() + (target.heightScale() - initial.heightScale()) * w),
                    (float) (initial.lift() + (target.lift() - initial.lift()) * w),
                    (float) (initial.rotationDegrees() + (targetAngle + angleTravel - initial.rotationDegrees()) * w));
        }
        return lastPose;
    }

    DancePose outside(double now, double duration) {
        if (exitedAt < 0) {
            exitedAt = now;
            exitPose = lastPose;
        }
        lastPose = exitPose.fade(1 - DanceMath.smooth((now - exitedAt) / duration));
        return lastPose;
    }

    boolean isExiting() { return exitedAt >= 0; }
    DancePose lastPose() { return lastPose; }
}
