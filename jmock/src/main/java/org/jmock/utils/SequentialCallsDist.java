package org.jmock.utils;

import umontreal.ssj.probdist.*;

import static org.jmock.utils.LogsAndDistr.*;

public class SequentialCallsDist extends ContinuousDistribution {
    // This Distribution corresponds to the Random Variable that is
    // the product between a Uniform Discrete Random Variable and a Continuous Random Variable

    private final DiscreteDistributionInt nbOfCalls;
    private final Distribution timePerCall;

    public SequentialCallsDist(DiscreteDistributionInt nbOfCalls, Distribution timePerCall) {
        this.nbOfCalls = nbOfCalls;
        this.timePerCall = timePerCall;
    }

    //TODO WARNING: MIGHT WANT TO IMPLEMENT, BUT NOT USED BY ANY METHOD CURRENTLY
    //      NOTE that the PDF does not always exist for this particular distribution
    @Override
    public double density(double v) {
        return 0;
    }

    @Override
    public double cdf(double v) {
        double cdf_acc = 0;
        int left = nbOfCalls.getXinf();
        int right = nbOfCalls.getXsup();
        for(int i = left; i <= right; i++) {
            cdf_acc += nbOfCalls.prob(i) * timePerCall.cdf(v / i);
        }
        return cdf_acc;
    }

    //TODO WARNING: MIGHT WANT TO IMPLEMENT, BUT NOT USED BY ANY METHOD CURRENTLY
    @Override
    public double[] getParams() {
        return new double[0];
    }

    @Override
    public String toString() {
        return "SequentialCallsDist: (nbOfCalls = "
                + nbOfCalls.toString()
                + ", timePerCall = "
                + timePerCall.toString() + ")";
    }

    public double getSample() {
        double sample = 0d;
        int calls = (int) nbOfCalls.inverseF(Math.random());
        while(calls -- > 0) {
            sample += timePerCall.inverseF(Math.random());
        }
        return sample;
    }

    public static void main(String[] args) {
//        SequentialCallsDist sequentialCallsDist =
//                new SequentialCallsDist(new UniformIntDist(1, 5), new NormalDist(50,5));
//        for(int i = 0; i < 10; i++) {
//            System.out.println(sequentialCallsDist.inverseF(Math.random()));
//        }
//
//        System.out.println(sequentialCallsDist.nbOfCalls.getXinf());
//        System.out.println(sequentialCallsDist.nbOfCalls.getXsup());
//
//        for(int i = 0; i < 10; i++) {
//            System.out.println(sequentialCallsDist.getSample());
//        }

        try {
            final Distribution lookupIngredientNutritionDistr = getBestDistributionFromEmpiricalData(
                    getSamplesFromLog("jmock/src/main/java/org/jmock/utils/logs.txt", "lookupIngredientNutrition", 0.2),
                    "lookupIngredientNutritionDistr");

            double adjFactor = computeAdjustmentFactor(
//                    getBestDistributionFromEmpiricalData(
//                            getSamplesFromLog("jmock/src/main/java/org/jmock/utils/logs.txt", "lookupIngredientNutritionCombinedSequential", 0.2),
//                            "lookupIngredientNutritionCombinedDistr"),

                    new SequentialCallsDist(new UniformIntDist(1, 5), lookupIngredientNutritionDistr),

                    getBestDistributionFromEmpiricalData(
                            getSamplesFromLog("jmock/src/main/java/org/jmock/utils/logs.txt", "lookupIngredientNutritionCombinedParallel",0.2),
                            "lookupIngredientNutritionCombinedParallelDistr"));

            System.out.println("ADJUSTMENT FACTOR: " + adjFactor);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
