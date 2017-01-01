package net.demilich.metastone.game.behaviour.training;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.behaviour.IFeatureExtractor;
import net.demilich.metastone.game.behaviour.threat.FeatureVector;
import net.demilich.metastone.game.behaviour.threat.GameStateValueBehaviour;
import net.demilich.metastone.game.spells.DoubleAttackSpell;

import java.util.Arrays;
import java.util.Stack;

/**
 * TODO: Create TrainingSetCreator class?
 *
 * Created by Lukasz Grad on 30/12/2016.
 */
public class RecGameStateValueBehaviour extends GameStateValueBehaviour {
	private TrainingSet trainingSet;
	private IFeatureExtractor<Double> extractor;
	private Stack<Double[]> states;
	private static final double DISCOUNT = 0.8;
	private static final double WON_SCORE = 10.0;
	private static final double LOST_SCORE = -8.0;

	public RecGameStateValueBehaviour(IFeatureExtractor<Double> extractor) {
		super(FeatureVector.getFittest(), "");
		this.extractor = extractor;
		this.trainingSet = new TrainingSet(extractor.length(), 1);
		states = new Stack<>();
	}

	@Override
	public RecGameStateValueBehaviour clone() {
		RecGameStateValueBehaviour clone = new RecGameStateValueBehaviour(extractor);
		clone.trainingSet = trainingSet;
		return clone;
	}

	@Override
	public void onGameOver(GameContext context, int playerId, int winningPlayerId) {
		addStates(playerId == winningPlayerId);
	}

	@Override
	public void onTurnOver(GameContext context, int playerId) {
		states.push(extractor.extract(context, playerId));
	}

	public TrainingSet getTrainingSet() {
		return trainingSet;
	}

	private void addStates(boolean hasWon) {
		double score = (hasWon) ? WON_SCORE : LOST_SCORE;
		while (!states.empty()) {
			double[] featureState = Arrays
			.stream(states.pop())
			.mapToDouble(Double::doubleValue)
			.toArray();
			double[] featureScore = { score - LOST_SCORE };
			trainingSet.addRow(featureState, featureScore);
			score = score * DISCOUNT;
		}
	}

	private double normalizeScore(double score) {
		return (score - LOST_SCORE) / (WON_SCORE - LOST_SCORE);
	}
}
