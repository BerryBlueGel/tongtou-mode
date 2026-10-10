package local.jellyparty;

public final class DanceMath {
    private static final double[] PHASES = {0, 0.16, 0.38, 0.64, 0.82, 1};
    private static final double[] HEIGHTS = {0.65, 1.24, 1.08, 0.67, 1.06, 0.65};
    private static final double[] LIFTS = {0, 0.08, 0.18, 0, 0.045, 0};

    private DanceMath() { }

    public static DancePose pose(double seconds, DanceConfig config) {
        double beat = Math.max(0, seconds - config.beatOffsetSeconds) * config.bpm / 60;
        double phase = beat - Math.floor(beat);
        double height = curve(phase, HEIGHTS);
        double width = 1 / Math.sqrt(height);
        double ramp = 0.3;
        double spinAge = Math.max(0, seconds - config.spinStartSeconds);
        double spinTime = spinAge < ramp ? spinAge * spinAge / (2 * ramp) : spinAge - ramp / 2;
        return new DancePose((float) width, (float) height,
                (float) (curve(phase, LIFTS) * config.hopHeight / 0.18),
                (float) ((spinTime * config.spinDegreesPerSecond) % 360));
    }

    private static double curve(double phase, double[] values) {
        for (int i = 0; i < PHASES.length - 1; i++) {
            if (phase <= PHASES[i + 1]) {
                double t = smooth((phase - PHASES[i]) / (PHASES[i + 1] - PHASES[i]));
                return values[i] + (values[i + 1] - values[i]) * t;
            }
        }
        return values[0];
    }

    public static double rangeWeight(double distance, double radius) {
        double band = Math.min(3, radius / 3);
        return 1 - smooth((distance - (radius - band)) / band);
    }

    public static double smooth(double value) {
        double x = clamp(value, 0, 1);
        return x * x * (3 - 2 * x);
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
