package utilities.distributions;

import org.apache.commons.math3.distribution.ExponentialDistribution;

public class ExponentialDistr implements Distribution {

    private final ExponentialDistribution model;

    public ExponentialDistr(ExponentialDistribution model) {
        this.model = model;
    }

    @Override
    public double sample() {
        return model.sample();
    }
}
