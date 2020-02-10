package org.jmock.api;

import org.hamcrest.Description;
import org.hamcrest.SelfDescribing;
import org.jmock.internal.StateMachine;

import java.util.ArrayList;
import java.util.List;

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

    default double getSingleRealTime() {
        return 0d;
    }

    default void setMultipleVirtualTimes(List<Double> virtualTimes) {
    }

    default List<Double> getMultipleVirtualTimes() {
        return new ArrayList<>(0);
    }
}