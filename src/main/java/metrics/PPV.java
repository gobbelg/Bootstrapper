package metrics;

import java.util.Map;
import java.util.function.Function;

import core.Outcome;

public class PPV implements Function<Map<Outcome, Integer>, Double>{

	@Override
	public Double apply(Map<Outcome, Integer> outcomeRates) {
		return (double) (double) outcomeRates.get(Outcome.TP)/(outcomeRates.get(Outcome.TP)+outcomeRates.get(Outcome.FP));
	}

}
