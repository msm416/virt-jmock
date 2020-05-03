package org.jmock;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
import org.junit.runners.model.FrameworkMethod;


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
        List<Double> virtualTimes = new ArrayList<>(counter);
        Map<String, List<Double>> virtualTimesPerComponent = new HashMap<>();
        for (int i = 0; i < counter; i++) {
            procedure.run();
            virtualTimes.add(getSingleVirtualTime(true));
            getSingleVirtualTimePerComponent(true).forEach((k, v) -> virtualTimesPerComponent.merge(k, v,
                    ((a, b) -> Stream.concat(a.stream(), b.stream())
                            .collect(Collectors.toList()))));
        }

        dispatcher.setRepeatCounter(counter);
        dispatcher.setMultipleVirtualTimes(virtualTimes);
        dispatcher.setMultipleVirtualTimesPerComponent(virtualTimesPerComponent);
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
        double TVT = dispatcher.getSingleVirtualTime(resetVirtualTime);
        //System.out.println("Total virtual time: " + TVT);
        return TVT;
    }

    public Map<String, List<Double>> getSingleVirtualTimePerComponent(boolean resetVirtualTime) {
        Map<String, List<Double>> SVTPC = dispatcher.getSingleVirtualTimePerComponent(resetVirtualTime);
        return SVTPC;
    }

    public double getSingleRealTime() {
        double TRT = dispatcher.getSingleRealTime();
        //System.out.println("Total real time for mocked methods: " + TRT);
        return TRT;
    }

    public List<Double> getMultipleVirtualTimes() {
        return dispatcher.getMultipleVirtualTimes();
    }

    public Map<String, List<Double>> getMultipleVirtualTimesPerComponent() {
        return dispatcher.getMultipleVirtualTimesPerComponent();
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
            return;
        }
        //TODO: back.html +2 in var color.
        //String tmpDir = System.getProperty("java.io.tmpdir");
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");
        Path dirPath = Paths.get("target", dtf.format(LocalDateTime.now()));
        if (!Files.exists(dirPath)) {
            try {
                Files.createDirectories(dirPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Path filePath = Paths.get(dirPath.toString(),
                method.getDeclaringClass().getName() + "-" + method.getName() + ".html");
        BufferedReader brJs = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/d3.min.js")));
        Files.write(Paths.get(dirPath.toString(), "d3.min.js"), brJs.lines().collect(Collectors.toList()));
        BufferedReader brFront = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/front.html")));
        List<String> frontLines = brFront.lines().collect(Collectors.toList());
        Map<String, List<Double>> mvtpc = dispatcher.getMultipleVirtualTimesPerComponent();
        List<Map<String, String>> data = new ArrayList<>();
        int numOfBuckets = Math.min(50,dispatcher.getRepeatCounter());
        for(int i = 0; i < numOfBuckets; i++) {
            int finalI = i;
            data.add(new HashMap() {{put("name", "bucket" + finalI);}});
        }
        for (Map.Entry<String, List<Double>> comp : mvtpc.entrySet()) {
            //String compName = "\"met" + comp.getKey().length() + "\"";
            String compName = "\"" + comp.getKey() + "\"";
            List<Double> compSamples = comp.getValue();
            double coef = ((double) compSamples.size()) / dispatcher.getRepeatCounter();
            int bucketSize = compSamples.size() / numOfBuckets;
            for(int i = 0; i < numOfBuckets; i++) {
                int avgCompSample = 0;
                for (int j = (i * bucketSize); j < ((i + 1) * bucketSize); j++) {
                    avgCompSample += compSamples.get(j);
                }
                data.get(i).put(compName, "" + ((avgCompSample/bucketSize) * coef));
            }
        }
        List<String> columns = new ArrayList<>();
        frontLines.add("var data = " + "[");
        //frontLines.add("var data = " + data + ";");
        for (Map<String, String> percentileContent : data) {
            frontLines.add("{");
            for (Map.Entry<String, String> entry : percentileContent.entrySet()) {
                String key = entry.getKey();
                String val = entry.getValue();
                if (columns.size() < percentileContent.size() - 1) {
                    if (!key.equals("name")) {
                        columns.add(key);
                    }
                }
                frontLines.add(key + ":\"" + val + "\",");
            }
            frontLines.add("},");
        }
        frontLines.add("];");
        frontLines.add("var columns = " + columns + ";");
        Files.write(filePath, frontLines);
        BufferedReader brBack = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/back.html")));
        Files.write(filePath, brBack.lines().collect(Collectors.toList()), StandardOpenOption.APPEND, StandardOpenOption.WRITE);
    }

}
