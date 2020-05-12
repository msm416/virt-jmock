package org.jmock.api;

import org.hamcrest.Description;
import org.hamcrest.SelfDescribing;
import org.jmock.internal.StateMachine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface InvocationDispatcher extends SelfDescribing, ExpectationCollector {

    StateMachine newStateMachine(String name);

    void add(Expectation expectation);

    void describeTo(Description description);

    void describeMismatch(Invocation invocation, Description description);

    boolean isSatisfied();

    Object dispatch(Invocation invocation) throws Throwable;

    default void setRepeatCounter(int repeatCounter){}

    default int getRepeatCounter() {
        return 1;
    }

    default SingleContextIteration getSingleContextIteration(boolean resetSingleContextIteration) {
        return new SingleContextIteration();
    }

    default double getSingleRealTime() {
        return 0d;
    }

    default void setMultipleSingleContextIterations(List<SingleContextIteration> multipleSingleContextIterations) {
    }

    default List<SingleContextIteration> getMultipleSingleContextIterations
            (boolean resetMultipleSingleContextIterations) {
        return new ArrayList<>();
    }

    class SingleContextIteration {
        public double totalVirtualTime;

        public Map<String, List<Double>> virtualTimesPerComponent;

        public SingleContextIteration() {
            this.totalVirtualTime = 0d;
            virtualTimesPerComponent = new HashMap<>();
        }

        public void addComponent(String componentName, double time) {
            if(virtualTimesPerComponent.containsKey(componentName)) {
                List<Double> virtualTimes = virtualTimesPerComponent.get(componentName);
                virtualTimes.add(time);
                //virtualTimesPerComponent.put(componentName, virtualTimes);
            } else {
                virtualTimesPerComponent.put(componentName, new ArrayList(){{add(time);}});
            }

            totalVirtualTime += time;
        }
    }
}