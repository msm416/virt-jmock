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

    default double getSingleVirtualTime(boolean resetVirtualTime) {
        return 0d;
    }

    default Map<String, List<Double>> getSingleVirtualTimePerComponent(boolean resetVirtualTime) {
        return new HashMap<>();
    }

    default double getSingleRealTime() {
        return 0d;
    }

    default void setMultipleVirtualTimes(List<Double> virtualTimes) {
    }

    default void setMultipleVirtualTimesPerComponent(Map<String, List<Double>> virtualTimesPerComponent) {
    }

    default List<Double> getMultipleVirtualTimes() {
        return new ArrayList<>(0);
    }

    default Map<String, List<Double>> getMultipleVirtualTimesPerComponent() {
        return new HashMap<>();
    }

    default void setRepeatCounter(int repeatCounter){}

    default int getRepeatCounter() {
        return 1;
    }
}