package org.jmock.utils;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.jmock.Mockery;
import umontreal.ssj.gof.GofStat;
import umontreal.ssj.probdist.*;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class LogsAndDistr {
    public static void main(String[] args) {
//        try {
//            runForSomeTimeAndGenerateLogsOnHeroku();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

//        long start = System.currentTimeMillis();
//
//        ArrayList<Double> samples = getSamplesFromLog("logs.txt",
//                "lookupIngredientNutrition");

//        System.out.println(System.currentTimeMillis() - start);

        try {
            getBestDistributionFromEmpiricalData(
                    getSamplesFromLog("jmock/src/main/java/org/jmock/utils/logs.txt",
                            "lookupOnApiIngredientDetails"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void writeDistributionSummaryHTML(List<ContinuousDistribution> distributionList,
                                                     double[] dataArray) {
        List<String> frontLines = new ArrayList<>();

        Class mockeryClass = Mockery.class;

        try {
            Path filePath = LogsAndDistr.writeFrontSectionHTML(
                    frontLines,
                    "DistributionComparison.html",
                    "/frontDistr.html",
                    mockeryClass);

            LogsAndDistr.writeMidSection(frontLines, distributionList, dataArray);

            Files.write(filePath, frontLines);

            LogsAndDistr.writeBackSectionHTML(filePath, "/backDistr.html", mockeryClass);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeMidSection(List<String> frontLines, List<ContinuousDistribution> distributionList,
                                        double[] dataArray) {
        //TODO: make buckets
        int n = dataArray.length;

        assert (n >= 50);
        //dataArray = Arrays.copyOfRange(dataArray, 0, 50);
        int nbOfBuckets = 50;

        double[] totalOver = new double[n];
        double[] totalUnder = new double[n];

        double[] xTickValues = new double[nbOfBuckets];

        double[][] xTickValuesNested = new double[distributionList.size()][nbOfBuckets];

//        var data = [
//        {name: "8 - Track", year: 1973.4, value: 26},
//        {name: "8 - Track", year: 1974.322, value: 17},
//        {name: "Cassette", year: 1973.4, value: 41},
//        {name: "Cassette", year: 1974.322, value: 2} ];

        frontLines.add("var dataNested = [");

        for (int i = 0; i < distributionList.size(); i++) {
            ContinuousDistribution distribution = distributionList.get(i);
            double[] samplesFromDistr = new double[n];

            List<Double> samplesFromDistrBuckets = new ArrayList<>();
            List<Double> over = new ArrayList<>();
            List<Double> under = new ArrayList<>();

            for (int j = 0; j < n; j++) {
                samplesFromDistr[j] = distribution.inverseF(Math.random());
            }

            Arrays.sort(samplesFromDistr);

            int[] dataArrayYValues = new int[nbOfBuckets];
            int[] distributionYValues = new int[nbOfBuckets];

            double minVal = Math.max(Math.min(dataArray[n/10], samplesFromDistr[n/10]), 0);
            double maxVal = Math.max(dataArray[n * 9 / 10], samplesFromDistr[n * 9 / 10]);

            System.out.println(minVal + " minval and " + maxVal + " maxval.");

            xTickValues[0] = minVal;
            xTickValues[nbOfBuckets - 1] = maxVal;

            xTickValuesNested[i][0] = minVal;
            xTickValuesNested[i][nbOfBuckets - 1] = maxVal;

            double tickDistance = (maxVal - minVal) * (1.0 / (nbOfBuckets - 1));

            for (int j = 1; j < nbOfBuckets - 1; j++) {
                xTickValues[j] = minVal + (maxVal - minVal) * ((double) j / (nbOfBuckets - 1));
                xTickValuesNested[i][j] = xTickValues[j];
            }

            for (int j = 0; j < n; j++) {

                int bucketForDataArr = (int) ((dataArray[j] - minVal) / tickDistance);

                if (bucketForDataArr < 0) {
                    dataArrayYValues[0]++;
                } else if (bucketForDataArr > nbOfBuckets - 1) {
                    dataArrayYValues[nbOfBuckets - 1]++;
                } else {
                    dataArrayYValues[bucketForDataArr]++;
                }

                int bucketForSampleDistr = (int) ((samplesFromDistr[j] - minVal) / tickDistance);

                if (bucketForSampleDistr < 0) {
                    distributionYValues[0]++;
                } else if (bucketForSampleDistr > nbOfBuckets - 1) {
                    distributionYValues[nbOfBuckets - 1]++;
                } else {
                    distributionYValues[bucketForSampleDistr]++;
                }
            }

            //{name: "8 - Track", year: 1973.4, value: 26},
            frontLines.add("[");
            for (int j = 0; j < nbOfBuckets; j++) {
                int dataArrayYValue = dataArrayYValues[j];
                int distributionYValue = distributionYValues[j];

                int overValue, underValue, diff;
                diff = distributionYValue - dataArrayYValue;

                if (diff > 0) {
                    overValue = diff;
                    underValue = 0;
                    distributionYValue -= diff;

                    totalOver[i] += diff;
                } else {
                    overValue = 0;
                    underValue = -diff;

                    totalUnder[i] -= diff;
                }

                frontLines.add("{");
                frontLines.add("\"name\":\"" + distribution.toString() +
                        "\", \"year\":\"" + xTickValues[j] +
                        "\", \"value\":\"" + distributionYValue + "\"");
                frontLines.add("},");
                frontLines.add("{");
                frontLines.add("\"name\":\"Over" +
                        "\", \"year\":\"" + xTickValues[j] +
                        "\", \"value\":\"" + overValue + "\"");
                frontLines.add("},");
                frontLines.add("{");
                frontLines.add("\"name\":\"Under" +
                        "\", \"year\":\"" + xTickValues[j] +
                        "\", \"value\":\"" + underValue + "\"");
                frontLines.add("},");
            }
            frontLines.add("],");
        }
        frontLines.add("];");

        frontLines.add("var xTickValuesNested = [");
        for (int i = 0; i < distributionList.size(); i++) {
            frontLines.add("[");
            for (int j = 0; j < nbOfBuckets; j++) {
                frontLines.add("\"" + xTickValuesNested[i][j] + "\", ");
            }
            frontLines.add("],");
        }
        frontLines.add("];");

        frontLines.add("var distributionInfo = [");
        for (int i = 0; i < distributionList.size(); i++) {
            frontLines.add("{");
            frontLines.add("name:\"" + distributionList.get(i).toString() + "\"");
            frontLines.add(",\"totalOver\":\"" + totalOver[i] + "\"");
            frontLines.add(",\"totalUnder\":\"" + totalUnder[i] + "\"");
            frontLines.add(",\"totalDiff\":\"" + (totalOver[i] - totalUnder[i]) + "\"");
            frontLines.add("},");
        }
        frontLines.add("];");

        frontLines.add("var nbOfBuckets = \"" + nbOfBuckets + "\"");

        frontLines.add("var columns = [\"distribution\", \"over\", \"under\"];");
    }

    public static void runForSomeTimeAndGenerateLogsOnHeroku() throws InterruptedException,
            UnsupportedEncodingException, UnirestException {
        HttpResponse<String> response;
        String[] qParams = {"greek salad", "ham sandwich", "cheese omelette", "sardines on toast", "canned spam"};
        for (int j = 0; j < 100; j++) {
            for (int i = 0; i < 5; i++) {
                response = Unirest.get("https://mmorpg-perf.herokuapp.com/?q=" +
                        URLEncoder.encode(qParams[i], "UTF-8"))
                        .asString();
                System.out.println(response);
                Thread.sleep(2000);
                response = Unirest.get("https://mmorpg-perf.herokuapp.com/?t=" +
                        (i + 1))
                        .asString();
                System.out.println(response);
                Thread.sleep(2000);
            }
            Thread.sleep(2);
        }
    }

    public static ArrayList<Double> getSamplesFromLog(String logfile, String methodName) {
        ArrayList<Double> samples = new ArrayList<>();
        String logfileContent = null;
        try {
            logfileContent = new String(Files.readAllBytes(Paths.get(logfile)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Pattern pattern = Pattern.compile(methodName + "\\(\\):\\[(?<samplesAsStr>[0-9]+(,[0-9]+)+)\\]");
        Matcher matcher = pattern.matcher(logfileContent);
        if (matcher.find()) {
            String[] parts = matcher.group("samplesAsStr").split(",");
            for (String part : parts) {
                if (part.length() == 0) {
                    continue;
                }
                samples.add(Double.parseDouble(part));
            }
        }
        return samples;
    }

    public static Distribution getBestDistributionFromEmpiricalData(ArrayList<Double> data) throws Exception {
        Class[] distClasses = {NormalDist.class, LaplaceDist.class, UniformDist.class};
        return getBestDistributionFromEmpiricalData(data, distClasses);
    }

    // This method is based on Kolmogorov Smirnov test, but any other could work
    // TODO: make this method support non-cont distr.
    public static Distribution getBestDistributionFromEmpiricalData(ArrayList<Double> data,
                                                                    Class[] distributionClasses)
            throws Exception {

        double[] dataArray = data.stream().mapToDouble(Double::doubleValue).toArray();

        Arrays.sort(dataArray);

        List<ContinuousDistribution> distributionList = getBestInstancesFromDistList(distributionClasses, dataArray);

        return getBestDistributionViaGoodnessToFitTest(dataArray, distributionList, false);
    }

    private static List<ContinuousDistribution> getBestInstancesFromDistList(Class[] distributionClasses, double[] dataArray) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        List<ContinuousDistribution> distributionList = new ArrayList<>();

        for (Class distributionClass : distributionClasses) {
            Class[] parameters = {double[].class, int.class};
            Method method = distributionClass.getDeclaredMethod("getInstanceFromMLE", parameters);
            distributionList.add(
                    (ContinuousDistribution) method.invoke(distributionClass, dataArray, dataArray.length));
        }

        distributionList.add(MixtureDistr.findBestMixedDistribution(dataArray, distributionList));

        return distributionList;
    }

    public static Distribution getBestDistributionViaGoodnessToFitTest(double[] dataArray,
                                                                       List<ContinuousDistribution> distributionList,
                                                                       boolean isFindingBestMixtureDistr) {
        int distributionListLen = distributionList.size();
        double[][] sval = new double[distributionList.size()][3];
        double[][] pval = new double[distributionList.size()][3];

        int maxPvalIndex = -1;
        double maxPval = -1d;

        for (int i = 0; i < distributionListLen; i++) {
            GofStat.kolmogorovSmirnov(dataArray, distributionList.get(i), sval[i], pval[i]);
            if (maxPval < pval[i][2]) {
                maxPval = pval[i][2];
                maxPvalIndex = i;
            }
        }

        if (!isFindingBestMixtureDistr) {
            System.out.println("Best distribution is: " + distributionList.get(maxPvalIndex));
            writeDistributionSummaryHTML(distributionList, dataArray);
        }

        return distributionList.get(maxPvalIndex);
    }

    public static void writeBackSectionHTML(Path filePath, String sectionPath, Class currClass) throws IOException {
        BufferedReader brBack = new BufferedReader(new InputStreamReader(currClass.getResourceAsStream(sectionPath)));
        Files.write(filePath, brBack.lines().collect(Collectors.toList()), StandardOpenOption.APPEND, StandardOpenOption.WRITE);
    }

    public static Path writeFrontSectionHTML(List<String> frontLines, String htmlName,
                                             String sectionPath, Class thisClass) throws IOException {

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
                htmlName);
        BufferedReader brJs = new BufferedReader(new InputStreamReader(thisClass.getResourceAsStream("/d3.min.js")));
        Files.write(Paths.get(dirPath.toString(), "d3.min.js"), brJs.lines().collect(Collectors.toList()));
        BufferedReader brFront = new BufferedReader(new InputStreamReader(thisClass.getResourceAsStream(sectionPath)));
        frontLines.addAll(brFront.lines().collect(Collectors.toList()));
        return filePath;
    }
}
