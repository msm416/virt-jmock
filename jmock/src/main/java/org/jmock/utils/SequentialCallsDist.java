package org.jmock.utils;

import umontreal.ssj.probdist.ContinuousDistribution;
import umontreal.ssj.probdist.Distribution;
import umontreal.ssj.probdist.NormalDist;
import umontreal.ssj.probdist.UniformIntDist;

public class SequentialCallsDist extends ContinuousDistribution {

    private final UniformIntDist nbOfCalls;
    private final Distribution timePerCall;

    public SequentialCallsDist(UniformIntDist nbOfCalls, Distribution timePerCall) {
        this.nbOfCalls = nbOfCalls;
        this.timePerCall = timePerCall;
    }

    //TODO: BUT NOT USED BY ANY METHOD
    //      The PDF does not always exist for this particular distribution
    @Override
    public double density(double v) {
        return 0;
    }

    @Override
    public double cdf(double v) {
        double cdf_acc = 0;
        int left = nbOfCalls.getI();
        int right = nbOfCalls.getJ();
        for(int i = left; i <= right; i++) {
            cdf_acc += timePerCall.cdf(v / i);
        }
        return cdf_acc / (right - left + 1);
    }

    //TODO: BUT NOT USED BY ANY METHOD
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

    public static void main(String[] args) {
        SequentialCallsDist sequentialCallsDist =
                new SequentialCallsDist(new UniformIntDist(1, 5), new NormalDist(50,5));
        for(int i = 0; i < 10; i++) {
            System.out.println(sequentialCallsDist.inverseF(Math.random()));
        }
    }
}
