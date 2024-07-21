package core;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;
import metrics.AUC;
import metrics.F1;
import metrics.NPV;
import metrics.PPV;
import metrics.Sensitivity;
import metrics.Specificity;

/**
 * The Boostrapper class implements bootstrapping to calculate confidence
 * intervals for AUC, sensitivity, specificity, negative predictive value,
 * positive predictive value, and F1. Confidence intervals are based on the
 * number of true positives, false positives, true negatives, and false
 * negatives in the sample.
 *
 * The class is meant to be run from the main() method of this class. The value
 * calculated is set within main() by setting the runType variables to one of
 * the RunType enum values. By default, the calculations will run 10000
 * iterations (as set by the static variable ITERATIONS) and calculate the 95%
 * confidence interval (as set by the static variable CONFIDENCE_INTERVAL).
 *
 * Setting main() to run the RunType of BASE_CASE allows insertion of particular
 * values of true positives, false positives, true negatives, and false
 * negatives directly in code, and the metric run (e.g AUC, F1, etc) can be
 * changed by setting the BASE_CASE_METRIC static variable to another function
 * of type Function<Map<Outcome, Integer>, Double> that implements the 'apply'
 * method. All the classes that currently do this are stored in the 'metrics'
 * package of this application.
 *
 * The various MULTI_VALUE methods allow calculations based on multiple sets of
 * values. The values should be stored in a tab-delimited file created so that
 * the first line of the file contains headers indicating which columns contain
 * the variable name, true positives (labeled as 'TP' without the quotes), false
 * positives (labeled as 'FP'), true negatives (labeled as 'TN'), and false
 * negatives (labeled as 'FN'). Each subsequent line should consist of the
 * variable for which the confidence interval is being calculated, followed by
 * the number of true positives, false positives, true negatives, and false
 * negatives in the order specified by the header. The Examples folder in this
 * project contains a file that can be used with the MULTI_VALUE RunTypes. The
 * full path to the file should be set within the various cases identified by
 * various RunTypes within the switch statement of main().
 *
 * @author Glenn Gobbel
 *
 */
public class Bootstrapper {

	private enum RunType {
		BASE_CASE, MULTI_VALUE_AUC, MULTI_VALUE_SENSITIVITY, MULTI_VALUE_SPECIFICITY, MULTI_VALUE_NPV, MULTI_VALUE_PPV,
		MULTI_VALUE_F1, MULTI_VALUE_F1_REPEAT;
	}

	private record PerformanceMetric(double calcuatedMetric, double lowCI, double highCI) {
	}

	private static int ITERATIONS = 100000;
	private static double CONFIDENCE_INTERVAL_RANGE = 0.95;
	private static Function<Map<Outcome, Integer>, Double> BASE_CASE_METRIC = new F1();

	private static RaptatPair<Double, Double> getConfidenceInterval(Map<Outcome, Integer> outcomeRates, int iterations,
			double confidenceIntervalRange, Function<Map<Outcome, Integer>, Double> performanceMetric) {

		if (confidenceIntervalRange <= 0 || confidenceIntervalRange >= 1.0) {
			throw new IllegalArgumentException("Confidence interval must be greater than 0 and less than 1.0");
		}
		int rateTotal = getRateTotal(outcomeRates);
		Map<Outcome, Double> outcomeFractions = getOutcomeFractions(outcomeRates, rateTotal);
		System.out.println(outcomeFractions);

		List<Double> sampleValueList = new ArrayList<>(2 * iterations);
		for (int i = 0; i < iterations; i++) {
			if (i % 10000 < 1) {
				System.out.println(i + " Iterations");
			}
			Map<Outcome, Integer> sampleRates = getSampleRates(outcomeFractions, rateTotal);
			double sampleMetricValue = performanceMetric.apply(sampleRates);
			sampleValueList.add(sampleMetricValue);
		}

		RaptatPair<Double, Double> confidenceInterval = getConfidenceIntervalRange(sampleValueList,
				confidenceIntervalRange);
		return confidenceInterval;
	}

	private static RaptatPair<Double, Double> getConfidenceIntervalRange(List<Double> aucResultList,
			double confidenceIntervalRange) {

		int aucListLength = aucResultList.size();
		Collections.sort(aucResultList);

		int lowerIndex = (int) Math.floor(aucListLength * (1 - confidenceIntervalRange));
		double lowerValue = aucResultList.get(lowerIndex);

		int upperIndex = (int) Math.floor(aucListLength * confidenceIntervalRange);
		double upperValue = aucResultList.get(upperIndex);

		return new RaptatPair<>(lowerValue, upperValue);
	}

	/**
	 * Reads in a tab-delimited file with the "outcomes" which should be integers
	 * representing true positives, true negatives, false positives, and false
	 * negatives. Note that the first line must contain headers as strings separated
	 * by tabs. This line is ignored, the method will return an empty Optional
	 * instance if it is blank.
	 *
	 * The method returns an Optional<Map<String, Map<Outcome, Integer>>> mapping
	 * from the name of a concept for which the calculation is being performed to
	 * another map representing an outcome (TP, FP, TN, FN) to the number of
	 * occurrences of the outcome for the particular concept.
	 *
	 * @param outcomesPath
	 * @return
	 */
	private static Optional<Map<String, Map<Outcome, Integer>>> getLabelToOutcomeMap(Path outcomesPath) {
		Map<String, Map<Outcome, Integer>> labelToOutcomeMap = new HashMap<>();
		try (BufferedReader br = new BufferedReader(new FileReader(outcomesPath.toFile()))) {
			/*
			 * Get headings from first line
			 */
			String line = br.readLine();
			if (line == null || line.isBlank()) {
				return Optional.empty();
			}
			Map<String, Map<Outcome, Integer>> resultMap = new HashMap<>();
			String[] outcomeArray = line.split("\t");
			while ((line = br.readLine()) != null) {
				if (line.isBlank()) {
					continue;
				}
				/*
				 * The beginning of each line is the label for which we want to get the outcome
				 * values
				 */
				String[] lineArray = line.split("\t");
				if (lineArray.length != 5) {
					System.err.println("Misconfigured data in file " + outcomesPath.toAbsolutePath());
					continue;
				}
				Map<Outcome, Integer> outcomeMap = new HashMap<>();
				int arrayIndex = 0;
				resultMap.put(lineArray[arrayIndex++], outcomeMap);
				while (arrayIndex < lineArray.length) {
					outcomeMap.put(Outcome.valueOf(outcomeArray[arrayIndex].toUpperCase()),
							Integer.parseInt(lineArray[arrayIndex]));
					arrayIndex++;
				}
				labelToOutcomeMap.put(lineArray[0], outcomeMap);
			}

		} catch (IOException e1) {
			e1.printStackTrace();
		}

		return Optional.of(labelToOutcomeMap);
	}

	/**
	 * Returns one from a set of outcomes. The returned outcome depends upon the
	 * random value supplied as a parameter. If the random value supplied is from a
	 * uniform distribution between 0 and 1, and if the fractions mapped to by each
	 * of the outcomes sum to zero, the probably of a given outcome will be equal to
	 * the fraction mapped to by that outcome.
	 * 
	 * @param outcomeFractions
	 * @param randomValue
	 * @return
	 */
	private static Outcome getOutcome(Map<Outcome, Double> outcomeFractions, double randomValue) {

		double fractionTotal = 0;
		for (Outcome outcome : outcomeFractions.keySet()) {
			fractionTotal += outcomeFractions.get(outcome);
			if (randomValue < fractionTotal) {
				return outcome;
			}
		}

		return null;
	}

	private static Map<Outcome, Double> getOutcomeFractions(Map<Outcome, Integer> outcomeRates, int rateTotal) {

		Map<Outcome, Double> outcomeFractions = new HashMap<>();

		for (Outcome outcome : outcomeRates.keySet()) {
			double fraction = (double) outcomeRates.get(outcome) / rateTotal;
			outcomeFractions.put(outcome, fraction);
		}

		return outcomeFractions;
	}

	private static Optional<Map<String, PerformanceMetric>> getPerformanceMetricFromPath(Path outcomesPath,
			int iterations, double confidenceIntervalRange,
			Function<Map<Outcome, Integer>, Double> performanceMetricFunction) {

		Optional<Map<String, Map<Outcome, Integer>>> labelToOutcomeMap = getLabelToOutcomeMap(outcomesPath);
		if (labelToOutcomeMap.isEmpty()) {
			return Optional.empty();
		}

		HashMap<String, PerformanceMetric> resultMap = new HashMap<>();
		Map<String, Map<Outcome, Integer>> labelMapping = labelToOutcomeMap.get();
		for (String label : labelMapping.keySet()) {
			Map<Outcome, Integer> outcomeMap = labelMapping.get(label);
			double performanceMetricValue = performanceMetricFunction.apply(outcomeMap);
			System.out.println("Average performance metric value for " + label + ":" + performanceMetricValue);
			RaptatPair<Double, Double> ci = getConfidenceInterval(outcomeMap, iterations, confidenceIntervalRange,
					performanceMetricFunction);
			resultMap.put(label, new PerformanceMetric(performanceMetricValue, ci.left, ci.right));
		}
		return Optional.of(resultMap);
	}

	private static int getRateTotal(Map<Outcome, Integer> outcomeRates) {
		int rateTotal = 0;
		for (int rate : outcomeRates.values()) {
			rateTotal += rate;
		}

		return rateTotal;
	}

	private static Map<Outcome, Integer> getSampleRates(Map<Outcome, Double> outcomeFractions, int rateTotal) {

		Map<Outcome, Integer> sampleRates = new HashMap<>();
		for (Outcome outcome : outcomeFractions.keySet()) {
			sampleRates.put(outcome, 0);
		}

		long randSeed = System.currentTimeMillis();
		Random numberGenerator = new Random(randSeed);

		for (int i = 0; i < rateTotal; i++) {
			double randomValue = numberGenerator.nextDouble();
			Outcome outcome = getOutcome(outcomeFractions, randomValue);
			sampleRates.replace(outcome, sampleRates.get(outcome) + 1);
		}

		return sampleRates;
	}

	public static void main(String[] args) {

		RunType runType = RunType.MULTI_VALUE_SENSITIVITY;

		switch (runType) {
		case BASE_CASE: {
			Map<Outcome, Integer> outcomeRates = new HashMap<>();
			outcomeRates.put(Outcome.TP, 648);
			outcomeRates.put(Outcome.FP, 59);
			outcomeRates.put(Outcome.TN, 0);
			outcomeRates.put(Outcome.FN, 69);

			double metricValue = BASE_CASE_METRIC.apply(outcomeRates);
			System.out.println("F1:" + metricValue);

			int iterations = ITERATIONS;
			double confidenceIntervalRange = CONFIDENCE_INTERVAL_RANGE;
			try {
				RaptatPair<Double, Double> confidenceInterval = getConfidenceInterval(outcomeRates, iterations,
						confidenceIntervalRange, BASE_CASE_METRIC);
				System.out.println("Confidence Interval: " + confidenceInterval);
			} catch (IllegalArgumentException e) {
				System.err.println(e.getMessage());
				System.exit(-1);
			}
		}
			break;

		case MULTI_VALUE_AUC: {
			Path outcomesPath = Path.of("C:/Users/gtony/OneDrive - VUMC/Grants/ViraniStatins/"
					+ "Manuscripts/NLP Canary Tool/ResultsData/StructuredVsUnstructuredVsBoth_TP_FP_TN_FN_211017.txt");
			int iterations = ITERATIONS;
			double confidenceIntervalRange = CONFIDENCE_INTERVAL_RANGE;
			Optional<Map<String, PerformanceMetric>> performanceMetrics = getPerformanceMetricFromPath(outcomesPath,
					iterations, confidenceIntervalRange, new AUC());

			if (performanceMetrics.isEmpty()) {
				System.err.println("Unable to find or process data at: " + outcomesPath.toAbsolutePath());
				System.exit(-1);
			}
			printBootstrapResults("AUC", performanceMetrics);

		}
			break;

		case MULTI_VALUE_F1: {
			Path outcomesPath = Path.of(
					"C:\\Users\\gobbelgt\\OneDrive - VUMC\\CirrhosisProject\\BootstrappingDataForLiverCirrhosisNLPPublication_240322.txt");
			int iterations = ITERATIONS;
			double confidenceIntervalRange = CONFIDENCE_INTERVAL_RANGE;
			Optional<Map<String, PerformanceMetric>> performanceMetrics = getPerformanceMetricFromPath(outcomesPath,
					iterations, confidenceIntervalRange, new F1());

			if (performanceMetrics.isEmpty()) {
				System.err.println("Unable to find or process data at: " + outcomesPath.toAbsolutePath());
				System.exit(-1);
			}
			printBootstrapResults("F1", performanceMetrics);
		}
			break;

		/**
		 * This case handles when we want to repeat botstrapping multiple times to see
		 * how many bootstrapping iterations are needed to accurately determine the
		 * confidence interval for an F1 value.
		 */
		case MULTI_VALUE_F1_REPEAT: {
			Path outcomesPath = Path
					.of("C:\\Users\\gtony\\OneDrive - VUMC\\Grants\\Meystre_PCORI_NLP\\BootstrapperTestData.txt");
			int repetitions = 100;
			int iterations = ITERATIONS;
			double confidenceIntervalRange = CONFIDENCE_INTERVAL_RANGE;
			Map<String, List<List<String>>> repetitionResultMap = new HashMap<>();

			for (int i = 0; i < repetitions; i++) {

				System.out.println("------------------------------------------------------------");
				System.out.println("Starting repetition " + i + " of " + repetitions + " repetitions");
				System.out.println("------------------------------------------------------------");

				Optional<Map<String, PerformanceMetric>> performanceMetrics = getPerformanceMetricFromPath(outcomesPath,
						iterations, confidenceIntervalRange, new F1());

				if (performanceMetrics.isEmpty()) {
					System.err.println("Unable to find or process data at: " + outcomesPath.toAbsolutePath());
					System.exit(-1);
				}

				storePerformanceMetrics(performanceMetrics.get(), repetitionResultMap);
				printBootstrapResults("F1", performanceMetrics);
			}
			printRepetitionResults(repetitionResultMap);
		}
			break;

		case MULTI_VALUE_PPV: {
			Path outcomesPath = Path.of(
					"C:\\Users\\gobbelgt\\OneDrive - VUMC\\CirrhosisProject\\BootstrappingDataForLiverCirrhosisNLPPublication_240322.txt");
			int iterations = ITERATIONS;
			double confidenceIntervalRange = CONFIDENCE_INTERVAL_RANGE;
			Optional<Map<String, PerformanceMetric>> performanceMetrics = getPerformanceMetricFromPath(outcomesPath,
					iterations, confidenceIntervalRange, new PPV());

			if (performanceMetrics.isEmpty()) {
				System.err.println("Unable to find or process data at: " + outcomesPath.toAbsolutePath());
			}
			printBootstrapResults("PPV", performanceMetrics);

		}
			break;

		case MULTI_VALUE_NPV: {
			Path outcomesPath = Path.of("C:/Users/gtony/OneDrive - VUMC/Grants/ViraniStatins/"
					+ "Manuscripts/NLP Canary Tool/ResultsData/StructuredVsUnstructuredVsBoth_TP_FP_TN_FN_211017.txt");
			int iterations = ITERATIONS;
			double confidenceIntervalRange = CONFIDENCE_INTERVAL_RANGE;
			Optional<Map<String, PerformanceMetric>> performanceMetrics = getPerformanceMetricFromPath(outcomesPath,
					iterations, confidenceIntervalRange, new NPV());

			if (performanceMetrics.isEmpty()) {
				System.err.println("Unable to find or process data at: " + outcomesPath.toAbsolutePath());
			}
			printBootstrapResults("NPV", performanceMetrics);

		}
			break;

		case MULTI_VALUE_SENSITIVITY: {
			Path outcomesPath = Path.of(
					"C:\\Users\\gobbelgt\\OneDrive - VUMC\\CirrhosisProject\\BootstrappingDataForLiverCirrhosisNLPPublication_240322.txt");
			int iterations = ITERATIONS;
			double confidenceIntervalRange = CONFIDENCE_INTERVAL_RANGE;
			Optional<Map<String, PerformanceMetric>> performanceMetrics = getPerformanceMetricFromPath(outcomesPath,
					iterations, confidenceIntervalRange, new Sensitivity());

			if (performanceMetrics.isEmpty()) {
				System.err.println("Unable to find or process data at: " + outcomesPath.toAbsolutePath());
			}
			printBootstrapResults("Sensitivity", performanceMetrics);

		}
			break;

		case MULTI_VALUE_SPECIFICITY: {
			Path outcomesPath = Path.of("C:/Users/gtony/OneDrive - VUMC/Grants/ViraniStatins/"
					+ "Manuscripts/NLP Canary Tool/ResultsData/StructuredVsUnstructuredVsBoth_TP_FP_TN_FN_211017.txt");
			int iterations = ITERATIONS;
			double confidenceIntervalRange = CONFIDENCE_INTERVAL_RANGE;
			Optional<Map<String, PerformanceMetric>> performanceMetrics = getPerformanceMetricFromPath(outcomesPath,
					iterations, confidenceIntervalRange, new Specificity());

			if (performanceMetrics.isEmpty()) {
				System.err.println("Unable to find or process data at: " + outcomesPath.toAbsolutePath());
			}
			printBootstrapResults("Specificity", performanceMetrics);

		}
			break;

		default:
			break;
		}

		System.exit(0);

	}

	private static void printBootstrapResults(String metricTag, Optional<Map<String, PerformanceMetric>> results) {

		System.out.println("---------------");
		System.out.println("  " + metricTag + " Results");
		System.out.println("---------------");

		Map<String, PerformanceMetric> labelMappings = results.get();
		for (String label : labelMappings.keySet()) {
			PerformanceMetric auc = labelMappings.get(label);
			StringBuilder reportedResults = new StringBuilder(label);
			reportedResults.append("\t").append(auc.calcuatedMetric);
			reportedResults.append("\t").append(auc.lowCI);
			reportedResults.append("\t").append(auc.highCI);
			System.out.println(reportedResults.toString());
		}
	}

	private static void printRepetitionResults(Map<String, List<List<String>>> repetitionResultMap) {

		for (String mapKey : repetitionResultMap.keySet()) {
			System.out.println("\n\n\n---------------");
			System.out.println(mapKey.toUpperCase() + " Repetition Results:");
			System.out.println("---------------\n");

			List<List<String>> resultLists = repetitionResultMap.get(mapKey);

			List<String> singleResultList;
			System.out.println("Metric:\n-----------------");
			singleResultList = resultLists.get(0);
			singleResultList.stream().forEach(result -> System.out.println(result));
			System.out.println("\n\n\n");

			System.out.println("LowConfidenceInterval:\n-----------------");
			singleResultList = resultLists.get(1);
			singleResultList.stream().forEach(result -> System.out.println(result));
			System.out.println("\n\n\n");

			System.out.println("HighConfidenceInterval:\n-----------------");
			singleResultList = resultLists.get(2);
			singleResultList.stream().forEach(result -> System.out.println(result));
			System.out.println("\n\n\n");
		}

	}

	private static void storePerformanceMetrics(Map<String, PerformanceMetric> metricMap,
			Map<String, List<List<String>>> multiResultMap) {

		for (String mappedResult : metricMap.keySet()) {
			List<List<String>> mappedLists = null;
			if ((mappedLists = multiResultMap.get(mappedResult)) == null) {
				mappedLists = new ArrayList<>();
				multiResultMap.put(mappedResult, mappedLists);
				for (int i = 0; i < 3; i++) {
					mappedLists.add(new ArrayList<String>());
				}
			}
			PerformanceMetric performanceMetric = metricMap.get(mappedResult);
			mappedLists.get(0).add(Double.toString(performanceMetric.calcuatedMetric));
			mappedLists.get(1).add(Double.toString(performanceMetric.lowCI));
			mappedLists.get(2).add(Double.toString(performanceMetric.highCI));
		}

	}

}
