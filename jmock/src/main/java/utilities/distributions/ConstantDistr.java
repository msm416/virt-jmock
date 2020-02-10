package utilities.distributions;

public class ConstantDistr implements Distribution {

    private final double val;

    public ConstantDistr(double val) {
        this.val = val;
    }

    @Override
    public double sample() {
        return val;
    }
}
