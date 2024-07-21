package metrics;

import java.util.Map;
import java.util.function.Function;

import core.Outcome;

public class AUC implements Function<Map<Outcome, Integer>, Double>{

	@Override
	public Double apply(Map<Outcome, Integer> outcomeRates) {
		double truePositiveRate = (double) outcomeRates.get(Outcome.TP)/(outcomeRates.get(Outcome.TP)+outcomeRates.get(Outcome.FN));
		double falsePositiveRate = (double) outcomeRates.get(Outcome.FP)/(outcomeRates.get(Outcome.FP)+outcomeRates.get(Outcome.TN));
		
		return (1.0 + truePositiveRate - falsePositiveRate)/2.0;
	}
	
}
