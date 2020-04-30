package org.jmock.lib.concurrent;

import java.util.*;

import org.hamcrest.Description;
import org.hamcrest.SelfDescribing;
import org.jmock.api.Expectation;
import org.jmock.api.ExpectationError;
import org.jmock.api.Invocation;
import org.jmock.api.InvocationDispatcher;
import org.jmock.internal.InvocationExpectation;
import org.jmock.internal.StateMachine;

public class UnsynchronisedInvocationDispatcher implements InvocationDispatcher {
    private final Collection<Expectation> expectations;
    private final Collection<StateMachine> stateMachines;
    private double singleVirtualTime = 0d;
    private Map<String, List<Double>> singleVirtualTimePerComponent= new HashMap<>();

    private double singleRealTime = 0d;

    private List<Double> multipleVirtualTimes;
    private Map<String, List<Double>> multipleVirtualTimesPerComponent;

    public UnsynchronisedInvocationDispatcher() {
        expectations = new ArrayList<Expectation>();
        stateMachines = new ArrayList<StateMachine>();
    }

    public UnsynchronisedInvocationDispatcher(Collection<Expectation> theExpectations, Collection<StateMachine> theStateMachines) {
        expectations = theExpectations;
        stateMachines = theStateMachines;
    }

    /* (non-Javadoc)
     * @see org.jmock.internal.InvocationDispatcher#newStateMachine(java.lang.String)
     */
    public StateMachine newStateMachine(String name) {
        StateMachine stateMachine = new StateMachine(name);
        stateMachines.add(stateMachine);
        return stateMachine;
    }

    /* (non-Javadoc)
     * @see org.jmock.internal.InvocationDispatcher#add(org.jmock.api.Expectation)
     */
    public void add(Expectation expectation) {
        expectations.add(expectation);
    }

    /* (non-Javadoc)
     * @see org.jmock.internal.InvocationDispatcher#describeTo(org.hamcrest.Description)
     */
    public void describeTo(Description description) {
        describe(description, expectations);
    }

    /* (non-Javadoc)
     * @see org.jmock.internal.InvocationDispatcher#describeMismatch(org.jmock.api.Invocation, org.hamcrest.Description)
     */
    public void describeMismatch(Invocation invocation, Description description) {
        describe(description, describedWith(expectations, invocation));
    }

    private Iterable<SelfDescribing> describedWith(Iterable<Expectation> expectations, final Invocation invocation) {
        final Iterator<Expectation> iterator = expectations.iterator();
        return new Iterable<SelfDescribing>() {
            public Iterator<SelfDescribing> iterator() {
                return new Iterator<SelfDescribing>() {
                    public boolean hasNext() {
                        return iterator.hasNext();
                    }

                    public SelfDescribing next() {
                        return new SelfDescribing() {
                            public void describeTo(Description description) {
                                iterator.next().describeMismatch(invocation, description);
                            }
                        };
                    }

                    public void remove() {
                        iterator.remove();
                    }
                };
            }
        };
    }

    private void describe(Description description, Iterable<? extends SelfDescribing> selfDescribingExpectations) {
        if (expectations.isEmpty()) {
            description.appendText("no expectations specified: did you...\n" +
                    " - forget to start an expectation with a cardinality clause?\n" +
                    " - call a mocked method to specify the parameter of an expectation?");
        } else {
            description.appendList("expectations:\n  ", "\n  ", "", selfDescribingExpectations);
            if (!stateMachines.isEmpty()) {
                description.appendList("\nstates:\n  ", "\n  ", "", stateMachines);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.jmock.internal.InvocationDispatcher#isSatisfied()
     */
    public boolean isSatisfied() {
        for (Expectation expectation : expectations) {
            if (!expectation.isSatisfied()) {
                return false;
            }
        }
        return true;
    }

    /* (non-Javadoc)
     * @see org.jmock.internal.InvocationDispatcher#dispatch(org.jmock.api.Invocation)
     */
    public Object dispatch(Invocation invocation) throws Throwable {
        for (Expectation expectation : expectations) {
            if (expectation.matches(invocation)) {
                try {
                    InvocationExpectation invocationExpectation = ((InvocationExpectation)expectation);
                    if(invocationExpectation.getPerformanceModel() != null) {
                        //double sample = invocationExpectation.getPerformanceModel().sample();
                        double sample = invocationExpectation.getPerformanceModel().inverseF(Math.random());
                        singleVirtualTime += sample;

                        String methodName = invocation.getInvokedMethod().toString(); // TODO: determine right name ?????
                        if(singleVirtualTimePerComponent.containsKey(methodName)) {
                            singleVirtualTimePerComponent.get(methodName).add(sample);
                        } else {
                            singleVirtualTimePerComponent.put(methodName, new ArrayList() {{add(sample);}});
                        }
                        //System.out.println("WE SAMPLED: " + sample);
                    }
                } catch (Exception ignored) {
                    // TODO: don't throw exception, rather verify the cast above
                }

                long startTime = System.currentTimeMillis();
                Object obj =  expectation.invoke(invocation);
                long endTime = System.currentTimeMillis();
                //System.out.println("WE TOOK (REAL EXECUTION TIME - mocked method): " + (endTime - startTime));
                singleRealTime += endTime - startTime;
                return obj;
            }
        }

        throw ExpectationError.unexpected("unexpected invocation", invocation);
    }

    @Override
    public double getSingleVirtualTime(boolean resetVirtualTime){
        double TVT = singleVirtualTime;

        if(resetVirtualTime) {
            singleVirtualTime = 0;
        }

        return TVT;
    }

    @Override
    public Map<String, List<Double>> getSingleVirtualTimePerComponent(boolean resetVirtualTime){
        Map<String, List<Double>> SVTPC = singleVirtualTimePerComponent;

        if(resetVirtualTime) {
            singleVirtualTimePerComponent = new HashMap<>();
        }

        return SVTPC;
    }

    @Override
    public void setMultipleVirtualTimes(List<Double> virtualTimes) {
        this.multipleVirtualTimes = virtualTimes;
    }

    @Override
    public void setMultipleVirtualTimesPerComponent(Map<String, List<Double>> virtualTimesPerComponent) {
        this.multipleVirtualTimesPerComponent = virtualTimesPerComponent;
    }

    @Override
    public List<Double> getMultipleVirtualTimes() {
        return multipleVirtualTimes;
    }

    @Override
    public Map<String, List<Double>> getMultipleVirtualTimesPerComponent() {
        return multipleVirtualTimesPerComponent;
    }

    public double getSingleRealTime() {
        return singleRealTime;
    }
}
