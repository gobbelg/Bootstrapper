package metrics;

import java.util.Map;
import java.util.function.Function;

import core.Outcome;

public class Sensitivity implements Function<Map<Outcome, Integer>, Double>{

	@Override
	public Double apply(Map<Outcome, Integer> outcomeRates) {
		return (double) outcomeRates.get(Outcome.TP)/(outcomeRates.get(Outcome.TP)+outcomeRates.get(Outcome.FN));
	}

}
