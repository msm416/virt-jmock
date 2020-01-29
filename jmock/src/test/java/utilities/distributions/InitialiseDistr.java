package utilities.distributions;

import org.apache.commons.math3.util.Pair;

import java.util.ArrayList;

import org.junit.Test;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class InitialiseDistr {

    @Test
    public void testSampleFromCategoricalDistr() throws Exception {
        ArrayList<Pair<Double, Distribution>> modelsWithProbs = new ArrayList<Pair<Double, Distribution>>() {{
            add(new Pair<>(0.2, new ConstantDistr(200)));
            add(new Pair<>(0.3, new ConstantDistr(300)));
            add(new Pair<>(0.5, new ConstantDistr(500)));
        }};

        Distribution distribution = new CategoricalDistr(modelsWithProbs);
        assertThat(distribution.sample(), anyOf(is(200.0), is(300.0), is(500.0)));

    }
}
