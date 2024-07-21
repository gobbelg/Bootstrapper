package metrics;

import java.util.Map;
import java.util.function.Function;

import core.Outcome;

public class F1 implements Function<Map<Outcome, Integer>, Double> {

	@Override
	public Double apply(Map<Outcome, Integer> outcomeRates) {
		double numerator = (double) 2 * outcomeRates.get(Outcome.TP);
		double denominator = (double) numerator + outcomeRates.get(Outcome.FP)+ outcomeRates.get(Outcome.FN);
		
		return numerator/denominator;
	}

}
