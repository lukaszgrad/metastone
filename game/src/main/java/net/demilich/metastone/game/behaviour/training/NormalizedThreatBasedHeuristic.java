package net.demilich.metastone.game.behaviour.training;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.behaviour.threat.FeatureVector;
import net.demilich.metastone.game.behaviour.threat.ThreatBasedHeuristic;
import net.demilich.metastone.game.behaviour.threat.WeightedFeature;
import net.demilich.metastone.game.cards.Card;
import net.demilich.metastone.game.entities.minions.Minion;

/**
 * Created by lukaszgrad on 02/01/2017.
 */
public class NormalizedThreatBasedHeuristic extends ThreatBasedHeuristic {
	public NormalizedThreatBasedHeuristic(FeatureVector vector) {
		super(vector);
	}

	@Override
	public double getScore(GameContext context, int playerId) {
		double score = super.getScore(context, playerId);
		if (score == Double.POSITIVE_INFINITY || score == Double.NEGATIVE_INFINITY)
			score = 0;
		return score;
	}
}
