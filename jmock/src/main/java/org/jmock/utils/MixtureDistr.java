package org.jmock.utils;

import umontreal.ssj.probdist.ContinuousDistribution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class MixtureDistr extends ContinuousDistribution {

    protected double[] weights;
    protected ContinuousDistribution[] distributions;

    public MixtureDistr(double[] weights, ContinuousDistribution[] distributions) {
        if (weights.length != distributions.length || weights.length == 0) {
            throw new IllegalArgumentException("Arrays must match length and have size >=1: weights[] length is "
                    + weights.length + " and distributions[] length is " + distributions.length);
        }

        double weightSum = Arrays.stream(weights).sum();
        if (1.0 - weightSum > 0.001) {
            throw new IllegalArgumentException("Weights must add up to exactly 1.0 " +
                    "(with error 0.001), but sum is " + weightSum);
        }

        this.weights = weights;
        this.distributions = distributions;
    }

    public MixtureDistr(ContinuousDistribution[] distributions) {
        this.distributions = distributions;
    }

    // Heuristic to find a locally-good mixture distribution
    public static MixtureDistr findBestMixedDistribution(double[] data,
                                                         List<ContinuousDistribution> distributions) {
        int runs = (int) Math.max(Math.pow(2, distributions.size()), 100);

        double[][] weights = new double[runs + 1][distributions.size()];

        double toleranceVal = 0.01;

        Random r = new Random();

        for (int i = 0; i < runs; i++) {
            List<Integer> indices = new ArrayList<>();
            for (int j = 0; j < distributions.size(); j++) {
                indices.add(j);
            }

            double remainingWeight = 1.0;

            while (indices.size() > 1) {
                if (remainingWeight < toleranceVal) {
                    // that should be sufficient since array is initialized with 0.0
                    break;
                }
                int index = indices.remove(r.nextInt(indices.size()));
                double getWeight = r.nextDouble() * remainingWeight;
                weights[i][index] = getWeight;
                remainingWeight -= getWeight;
            }
            weights[i][indices.remove(0)] = remainingWeight;
            for (int j = 0; j < distributions.size(); j++) {
            }
        }

        // Consider equally weighted distributions - which is often the case
        // i.e. adding three normal distributions
        for (int j = 0; j < distributions.size(); j++) {
            weights[runs][j] = 1.0 / distributions.size();
        }

        List<ContinuousDistribution> mixtureDistributionList = new ArrayList<>();

        for (int i = 0; i < runs + 1; i++) {
            mixtureDistributionList.add(new MixtureDistr(weights[i],
                    distributions.toArray(new ContinuousDistribution[0])));
        }

        return (MixtureDistr)
                (LogsAndDistr.getBestDistributionViaGoodnessToFitTest(data, mixtureDistributionList, true, "dist"));

    }

    @Override
    public double density(double v) {
        double density = 0d;
        for (int i = 0; i < distributions.length; i++) {
            density += distributions[i].density(v) * weights[i];
        }
        return density;
    }

    @Override
    public double cdf(double v) {
        double cdf = 0d;
        for (int i = 0; i < distributions.length; i++) {
            cdf += distributions[i].cdf(v) * weights[i];
        }
        return cdf;
    }

    //TODO WARNING: MIGHT WANT TO IMPLEMENT, BUT NOT USED BY ANY METHOD CURRENTLY
    @Override
    public double[] getParams() {
        return new double[0];
    }

    @Override
    public String toString() {
        StringBuilder acc = new StringBuilder("Mixture Distribution:");
        for (int i = 0; i < weights.length; i++) {
            acc.append(" ( weight = ")
                    .append(weights[i])
                    .append(", ")
                    .append(distributions[i].toString())
                    .append(")");
        }
        return acc.toString();
    }
}
