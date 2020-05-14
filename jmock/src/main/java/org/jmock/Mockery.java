package org.jmock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hamcrest.Description;
import org.hamcrest.SelfDescribing;
import org.jmock.api.Expectation;
import org.jmock.api.ExpectationError;
import org.jmock.api.ExpectationErrorTranslator;
import org.jmock.api.Imposteriser;
import org.jmock.api.Invocation;
import org.jmock.api.InvocationDispatcher;
import org.jmock.api.Invokable;
import org.jmock.api.MockObjectNamingScheme;
import org.jmock.api.ThreadingPolicy;
import org.jmock.internal.CaptureControl;
import org.jmock.internal.ExpectationBuilder;
import org.jmock.internal.ExpectationCapture;
import org.jmock.internal.InvocationDiverter;
import org.jmock.internal.InvocationToExpectationTranslator;
import org.jmock.internal.NamedSequence;
import org.jmock.internal.ObjectMethodExpectationBouncer;
import org.jmock.internal.ProxiedObjectIdentity;
import org.jmock.internal.ReturnDefaultValueAction;
import org.jmock.internal.SingleThreadedPolicy;
import org.jmock.lib.CamelCaseNamingScheme;
import org.jmock.lib.IdentityExpectationErrorTranslator;
import org.jmock.lib.JavaReflectionImposteriser;
import org.jmock.lib.concurrent.Synchroniser;
import org.jmock.utils.LogsAndDistr;
import org.junit.runners.model.FrameworkMethod;

import static org.jmock.utils.LogsAndDistr.determineBucketCounts;


/**
 * A Mockery represents the context, or neighbourhood, of the object(s) under test.
 * <p>
 * The neighbouring objects in that context are mocked out. The test specifies the
 * expected interactions between the object(s) under test and its  neighbours and
 * the Mockery checks those expectations while the test is running.
 *
 * @author npryce
 * @author smgf
 * @author olibye
 * @author named by Ivan Moore.
 */
public class Mockery implements SelfDescribing {
    private Imposteriser imposteriser = JavaReflectionImposteriser.INSTANCE;
    private ExpectationErrorTranslator expectationErrorTranslator = IdentityExpectationErrorTranslator.INSTANCE;
    private MockObjectNamingScheme namingScheme = CamelCaseNamingScheme.INSTANCE;
    private ThreadingPolicy threadingPolicy = new SingleThreadedPolicy();

    private final Set<String> mockNames = new HashSet<String>();
    private final ReturnDefaultValueAction defaultAction = new ReturnDefaultValueAction(imposteriser);
    private final List<Invocation> actualInvocations = new ArrayList<Invocation>();
    private InvocationDispatcher dispatcher = threadingPolicy.dispatcher();

    private Error firstError = null;

    /*
     * Policies
     */

    /**
     * Sets the result returned for the given type when no return value has been explicitly
     * specified in the expectation.
     *
     * @param type   The type for which to return <var>result</var>.
     * @param result The value to return when a method of return type <var>type</var>
     *               is invoked for which an explicit return value has has not been specified.
     */
    public void setDefaultResultForType(Class<?> type, Object result) {
        defaultAction.addResult(type, result);
    }

    /**
     * Changes the imposteriser used to adapt mock objects to the mocked type.
     * <p>
     * The default imposteriser allows a test to mock interfaces but not
     * classes, so you'll have to plug a different imposteriser into the
     * Mockery if you want to mock classes.
     *
     * @param imposteriser makes mocks
     */
    public void setImposteriser(Imposteriser imposteriser) {
        this.imposteriser = imposteriser;
        this.defaultAction.setImposteriser(imposteriser);
    }

    /**
     * Changes the naming scheme used to generate names for mock objects that
     * have not been explicitly named in the test.
     * <p>
     * The default naming scheme names mock objects by lower-casing the first
     * letter of the class name, so a mock object of type BananaSplit will be
     * called "bananaSplit" if it is not explicitly named in the test.
     *
     * @param namingScheme names mocks for failure reports
     */
    public void setNamingScheme(MockObjectNamingScheme namingScheme) {
        this.namingScheme = namingScheme;
    }

    /**
     * Changes the expectation error translator used to translate expectation
     * errors into errors that report test failures.
     * <p>
     * By default, expectation errors are not translated and are thrown as
     * errors of type {@link ExpectationError}.  Plug in a new expectation error
     * translator if you want your favourite test framework to report expectation
     * failures using its own error type.
     *
     * @param expectationErrorTranslator translator for your test framework
     */
    public void setExpectationErrorTranslator(ExpectationErrorTranslator expectationErrorTranslator) {
        this.expectationErrorTranslator = expectationErrorTranslator;
    }

    /**
     * Changes the policy by which the Mockery copes with multiple threads.
     * <p>
     * The default policy throws an exception if the Mockery is called from different
     * threads.
     *
     * @param threadingPolicy how to handle different threads.
     * @see Synchroniser
     */
    public void setThreadingPolicy(ThreadingPolicy threadingPolicy) {
        this.threadingPolicy = threadingPolicy;
        this.dispatcher = threadingPolicy.dispatcher();
    }

    /*
     * API
     */

    /**
     * Creates a mock object of type <var>typeToMock</var> and generates a name for it.
     *
     * @param <T>        is the class of the mock
     * @param typeToMock is the class of the mock
     * @return the mock of typeToMock
     */
    public <T> T mock(Class<T> typeToMock) {
        return mock(typeToMock, namingScheme.defaultNameFor(typeToMock));
    }

    /**
     * Creates a mock object of type <var>typeToMock</var> with the given name.
     *
     * @param <T>        is the class of the mock
     * @param typeToMock is the class of the mock
     * @param name       is the name of the mock object that will appear in failures
     * @return the mock of typeToMock
     */
    public <T> T mock(Class<T> typeToMock, String name) {
        if (mockNames.contains(name)) {
            throw new IllegalArgumentException("a mock with name " + name + " already exists");
        }

        final MockObject mock = new MockObject(typeToMock, name);
        mockNames.add(name);

        Invokable invokable =
                threadingPolicy.synchroniseAccessTo(
                        new ProxiedObjectIdentity(
                                new InvocationDiverter<CaptureControl>(
                                        CaptureControl.class, mock, mock)));

        return imposteriser.imposterise(invokable, typeToMock, CaptureControl.class);
    }

    /**
     * Returns a new sequence that is used to constrain the order in which
     * expectations can occur.
     *
     * @param name The name of the sequence.
     * @return A new sequence with the given name.
     */
    public Sequence sequence(String name) {
        return new NamedSequence(name);
    }

    /**
     * Returns a new state machine that is used to constrain the order in which
     * expectations can occur.
     *
     * @param name The name of the state machine.
     * @return A new state machine with the given name.
     */
    public States states(String name) {
        return dispatcher.newStateMachine(name);
    }

    /**
     * Specifies the expected invocations that the object under test will perform upon
     * objects in its context during the test.
     * <p>
     * The builder is responsible for interpreting high-level, readable API calls to
     * construct an expectation.
     * <p>
     * This method can be called multiple times per test and the expectations defined in
     * each block are combined as if they were defined in same order within a single block.
     *
     * @param expectations that will be checked
     */
    public void checking(ExpectationBuilder expectations) {
        expectations.buildExpectations(defaultAction, dispatcher);
    }

    /**
     * Adds an expected invocation that the object under test will perform upon
     * objects in its context during the test.
     * <p>
     * This method allows a test to define an expectation explicitly, bypassing the
     * high-level API, if desired.
     *
     * @param expectation to check
     */
    public void addExpectation(Expectation expectation) {
        dispatcher.add(expectation);
    }

    /**
     * Fails the test if there are any expectations that have not been met.
     */
    public void assertIsSatisfied() {
        if (firstError != null) {
            throw firstError;
        } else if (!dispatcher.isSatisfied()) {
            throw expectationErrorTranslator.translate(
                    ExpectationError.notAllSatisfied(this));
        }
    }

    public void describeTo(Description description) {
        description.appendDescriptionOf(dispatcher);
        describeHistory(description);
    }

    private void describeMismatch(Invocation invocation, Description description) {
        dispatcher.describeMismatch(invocation, description);
        describeHistory(description);
    }

    private void describeHistory(Description description) {
        description.appendText("\nwhat happened before this:");
        final List<Invocation> invocationsSoFar = new ArrayList<Invocation>(actualInvocations);
        if (invocationsSoFar.isEmpty()) {
            description.appendText(" nothing!");
        } else {
            description.appendList("\n  ", "\n  ", "\n", invocationsSoFar);
        }
    }

    private Object dispatch(Invocation invocation) throws Throwable {
        if (firstError != null) {
            throw firstError;
        }

        try {
            Object result = dispatcher.dispatch(invocation);
            actualInvocations.add(invocation);
            return result;
        } catch (ExpectationError e) {
            firstError = expectationErrorTranslator.translate(mismatchDescribing(e));
            firstError.setStackTrace(e.getStackTrace());
            throw firstError;
        } catch (Throwable t) {
            actualInvocations.add(invocation);
            throw t;
        }
    }

    public void repeat(int counter, Runnable procedure) {
        List<InvocationDispatcher.SingleContextIteration> singleContextIterations = new ArrayList<>();

        for (int i = 0; i < counter; i++) {
            procedure.run();

            singleContextIterations.add(dispatcher.getSingleContextIteration(true));
        }

        dispatcher.setRepeatCounter(counter);

        dispatcher.setMultipleSingleContextIterations(singleContextIterations);
    }

    private ExpectationError mismatchDescribing(final ExpectationError e) {
        ExpectationError filledIn = new ExpectationError(e.getMessage(), new SelfDescribing() {
            public void describeTo(Description description) {
                describeMismatch(e.invocation, description);
            }
        }, e.invocation);
        filledIn.setStackTrace(e.getStackTrace());
        return filledIn;
    }

    /**
     * Get the virtual time of the previous call to checking.
     * Boolean resetVirtualTime specifies if we want to reset the virtual time accumulated in dispatcher
     * (resetVirtualTime should be true when reusing the same JUnitRuleMockery @Rule as context).
     */
    public double getSingleVirtualTime(boolean resetVirtualTime) {
        return dispatcher.getSingleContextIteration(resetVirtualTime).totalVirtualTime;
    }

    public double getSingleRealTime() {
        double TRT = dispatcher.getSingleRealTime();
        //System.out.println("Total real time for mocked methods: " + TRT);
        return TRT;
    }

    public List<Double> getMultipleVirtualTimes(boolean resetMultipleSingleContextIterations) {
        return dispatcher.getMultipleSingleContextIterations(resetMultipleSingleContextIterations)
                .stream()
                .map(a -> a.totalVirtualTime)
                .collect(Collectors.toList());
    }

    private class MockObject implements Invokable, CaptureControl {
        private Class<?> mockedType;
        private String name;

        public MockObject(Class<?> mockedType, String name) {
            this.name = name;
            this.mockedType = mockedType;
        }

        @Override
        public String toString() {
            return name;
        }

        public Object invoke(Invocation invocation) throws Throwable {
            return dispatch(invocation);
        }

        public Object captureExpectationTo(ExpectationCapture capture) {
            return imposteriser.imposterise(
                    new ObjectMethodExpectationBouncer(new InvocationToExpectationTranslator(capture, defaultAction)),
                    mockedType);
        }
    }

    public void writeHtml(FrameworkMethod method) throws Exception {
        if(dispatcher.getRepeatCounter() == 1) {
            //flush the virtual times
            dispatcher.setSingleContextIteration(new InvocationDispatcher.SingleContextIteration());
            return;
        }

        //TODO: back.html +2 in var color.
        List<String> frontLines = new ArrayList<>();

        Class thisClass = Mockery.class;

        Path filePath = LogsAndDistr.writeFrontSectionHTML(
                frontLines,
                method.getDeclaringClass().getName() + "-" + method.getName() + ".html",
                "/front.html",
                thisClass);

        writeMidSectionHTML(frontLines, filePath);

        // Important: Write the content from the sections below to the file
        Files.write(filePath, frontLines);

        LogsAndDistr.writeBackSectionHTML(filePath, "/back.html", thisClass);

        //flush the virtual times
        dispatcher.setMultipleSingleContextIterations(new ArrayList<>());
    }

    private void writeMidSectionHTML(List<String> frontLines, Path filePath) throws IOException {
        List<InvocationDispatcher.SingleContextIteration> MSCI =
                dispatcher.getMultipleSingleContextIterations(false);

        assert(MSCI.size() == dispatcher.getRepeatCounter());

        MSCI.sort(Comparator.comparingDouble(t -> t.totalVirtualTime));

        int nbOfBuckets = Math.min(50, dispatcher.getRepeatCounter());

        int[] xTickValues = new int[nbOfBuckets];

        int repeatCnt = MSCI.size();

        Set<String> componentNames = new HashSet<>();

        for(InvocationDispatcher.SingleContextIteration SCI : MSCI) {
            componentNames.addAll(SCI.virtualTimesPerComponent.keySet());
        }

        Map<String, int[]> componentToDataArrayYValues = new HashMap<>();

        for(String componentName : componentNames) {
            componentToDataArrayYValues.put(componentName, new int[nbOfBuckets]);
        }

        componentToDataArrayYValues.put("Number of samples per bucket", new int[nbOfBuckets]);

        double minVal = MSCI.get(0).totalVirtualTime;
        double maxVal = MSCI.get(repeatCnt-1).totalVirtualTime;

        if(maxVal - minVal < 50) {
            maxVal += 50;
            System.out.println("Adjusting tickvalues...");
            //ensure that the distance between tickValues is >1.
        }

        xTickValues[0] = (int) minVal;
        xTickValues[nbOfBuckets - 1] = (int) maxVal;

        double tickDistance = (maxVal - minVal) * (1.0 / (nbOfBuckets - 1));

        for (int j = 1; j < nbOfBuckets - 1; j++) {
            xTickValues[j] = (int) (minVal + (maxVal - minVal) * ((double) j / (nbOfBuckets - 1)));
        }

        for(int i = 0; i < MSCI.size(); i++) {
            Map<String, List<Double>> VTPC = MSCI.get(i).virtualTimesPerComponent;
            for(String componentName : componentNames) {
                determineBucketCounts(
                        nbOfBuckets,
                        MSCI.get(i).totalVirtualTime,
                        componentToDataArrayYValues.get(componentName),
                        minVal,
                        tickDistance,
                        VTPC.getOrDefault(componentName, new ArrayList<>()).size());
            }

            determineBucketCounts(
                    nbOfBuckets,
                    MSCI.get(i).totalVirtualTime,
                    componentToDataArrayYValues.get("Number of samples per bucket"),
                    minVal,
                    tickDistance,
                    1);
        }

        frontLines.add("var dataNested = [");

        for(int i = 0; i < nbOfBuckets; i++) {
            for(String componentName : componentNames) {
                frontLines.add("{");
                frontLines.add("\"name\":\"" + componentName +
                        "\", \"tickVal\":\"" + xTickValues[i] +
                        "\", \"value\":\"" + componentToDataArrayYValues.get(componentName)[i] + "\"");
                frontLines.add("},");
            }
            frontLines.add("{");
            frontLines.add("\"name\":\"" + "Number of samples per bucket" +
                    "\", \"tickVal\":\"" + xTickValues[i] +
                    "\", \"value\":\"" + componentToDataArrayYValues.get("Number of samples per bucket")[i] + "\"");
            frontLines.add("},");
        }

        frontLines.add("]");

        frontLines.add("var xTickValues = [");

        for(int i = 0; i < nbOfBuckets; i++) {
            frontLines.add("\"" + xTickValues[i] + "\",");
        }

        frontLines.add("];");

        frontLines.add("var nbOfBuckets = \"" + nbOfBuckets + "\";");

        frontLines.add("var componentNames = [");

        for(String componentName : componentNames) {
            frontLines.add("\"" + componentName + "\",");
        }

        frontLines.add("];");
    }

}
