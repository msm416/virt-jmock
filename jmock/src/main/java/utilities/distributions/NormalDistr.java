package utilities.distributions;


import org.apache.commons.math3.distribution.NormalDistribution;

public class NormalDistr implements Distribution {

    private final NormalDistribution model;

    public NormalDistr(double mean, double sd) {
        this.model = new NormalDistribution(mean, sd);
    }

    @Override
    public double sample() {
        return model.sample();
    }
}
