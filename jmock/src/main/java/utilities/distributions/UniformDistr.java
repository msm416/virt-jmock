package utilities.distributions;

import org.apache.commons.math3.distribution.UniformRealDistribution;

public class UniformDistr implements Distribution {

    private final UniformRealDistribution model;

    public UniformDistr(UniformRealDistribution model) {
        this.model = model;
    }

    @Override
    public double sample() {
        return model.sample();
    }
}
