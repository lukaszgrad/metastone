package net.demilich.metastone.game.behaviour.advanced;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.behaviour.IStateEvaluate;
import net.demilich.metastone.game.behaviour.threat.FeatureVector;
import net.demilich.metastone.game.behaviour.threat.ThreatBasedHeuristic;

/**
 * Using ThreatBasedHeuristic
 */
public class SimpleStateEvaluate implements IStateEvaluate<Double> {
	private final ThreatBasedHeuristic heuristic;

	public SimpleStateEvaluate() {
		this.heuristic = new ThreatBasedHeuristic(FeatureVector.getFittest());
	}

	@Override
	public Double evaluate(GameContext context, int playerId) {
		return heuristic.getScore(context, playerId);
	}
}
