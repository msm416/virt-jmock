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

    private SingleContextIteration singleContextIteration = new SingleContextIteration();
    private List<SingleContextIteration> multipleSingleContextIterations;

    private double singleRealTime = 0d;

    private int repeatCounter = 1;

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
                        double sample = Math.max(0d,
                                invocationExpectation.getPerformanceModel().inverseF(Math.random()) /
                                invocationExpectation.getAdjustmentFactor());

                        String methodName = invocation.getInvokedMethod().toString();

                        singleContextIteration.addComponent(methodName, sample);

                        //System.out.println("WE SAMPLED: " + sample);
                    }

                    if(invocationExpectation.getRemainingTimeModel() != null) {
                        // No methods can have '#' as a starting character in their name
                        String name = "#ConstantRemainingTime";
                        if(!singleContextIteration.virtualTimesPerComponent.containsKey(name)) {
                            double constantRemTimeSample = Math.max(0d,
                                    invocationExpectation.getRemainingTimeModel().inverseF(Math.random()));

                            singleContextIteration.addComponent(name, constantRemTimeSample);
                        }
                    }
                } catch (Exception ignored) {
                    // TODO FUTURE WORK: don't throw exception, rather verify the cast above
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
    public SingleContextIteration getSingleContextIteration(boolean resetSingleContextIteration) {
        SingleContextIteration SCI = singleContextIteration;

        if(resetSingleContextIteration) {
            singleContextIteration = new SingleContextIteration();
        }

        return SCI;
    }

    @Override
    public void setSingleContextIteration(SingleContextIteration singleContextIteration) {
        this.singleContextIteration = singleContextIteration;
    }

    @Override
    public void setMultipleSingleContextIterations(List<SingleContextIteration> multipleSingleContextIterations) {
        this.multipleSingleContextIterations = multipleSingleContextIterations;
    }

    @Override
    public List<SingleContextIteration> getMultipleSingleContextIterations(boolean resetMultipleContextIterations) {
        List<SingleContextIteration> MSCI = multipleSingleContextIterations;

        if(resetMultipleContextIterations) {
            multipleSingleContextIterations = new ArrayList<>();
        }

        return MSCI;
    }

    public double getSingleRealTime() {
        return singleRealTime;
    }

    @Override
    public void setRepeatCounter(int repeatCounter) {
        this.repeatCounter = repeatCounter;
    }

    @Override
    public int getRepeatCounter() {
        return repeatCounter;
    }
}
