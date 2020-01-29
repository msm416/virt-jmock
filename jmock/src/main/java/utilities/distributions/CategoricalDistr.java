package utilities.distributions;

import org.apache.commons.math3.util.Pair;

import java.util.*;

/**
 * A Categorical Distribution is composed of a list of Distribution model, each
 * has a probability to be picked when sampling. Sum of probabilities must be 1.
 * <p>
 * Example usage:
 * We want to query an object in database. Because lookup might be slow, we might want to check first in
 * one of our caches has that object. There is a 20% chance that our cache has that object, and a lookup there
 * can be modelled by a constant distribution - it takes roughly 2 seconds to query.
 * There is a 80% chance that we will have to look up in our database,
 * estimated time for lookup will be determined by a NormalDistr with mean 100 and sd 10.
 * For this particular example:
 * <p>
 * ArrayList<Pair<Double, Distribution>> modelsWithProbs = new ArrayList<Pair<Double, Distribution>>() {{
 * add(new Pair<>(0.2, new ConstantDistr(10)));
 * add(new Pair<>(0.8, new NormalDistr(100 + 2, 10)));
 * }};
 * <p>
 * Distribution distribution = new CategoricalDistr(modelsWithProbs);
 * distribution.sample();
 */

public class CategoricalDistr implements Distribution {

    private final ArrayList<Pair<Double, Distribution>> models;

    public CategoricalDistr(ArrayList<Pair<Double, Distribution>> models) throws Exception {
        if (models.isEmpty()) {
            throw new Exception("Empty list of models.");
        }

        this.models = models;
        //this.models.sort(Comparator.comparing(Pair::getFirst));
    }

    @Override
    public double sample() {
        double rand = Math.random();
        double acc = 0.0;
        for (Pair<Double, Distribution> pick : models) {
            acc += pick.getFirst();
            if (acc >= rand) {
                return pick.getSecond().sample();
            }
        }

        return models.get(models.size() - 1).getSecond().sample();
    }
}
