package net.demilich.metastone.game.behaviour.training;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.behaviour.IFeatureExtractor;
import net.demilich.metastone.game.behaviour.threat.FeatureVector;
import net.demilich.metastone.game.behaviour.threat.GameStateValueBehaviour;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

/**
 * Created by Lukasz Grad on 30/12/2016.
 */
public class RecGameStateValueBehaviour extends GameStateValueBehaviour {
	private List<DataSet> trainingSet;

	private IFeatureExtractor<Double> extractor;
	private Stack<Double[]> states;
	private static final double DISCOUNT = 0.9;
	private static final double WON_SCORE = 10.0;
	private static final double LOST_SCORE = -10.0;

	public RecGameStateValueBehaviour(IFeatureExtractor<Double> extractor) {
		super(FeatureVector.getFittest(), "");
		this.extractor = extractor;
		this.trainingSet = new LinkedList<>();
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
		Double[] features = extractor.extract(context, playerId);
		states.push(features);
	}

	public DataSet getTrainingSet() {
		return DataSet.merge(trainingSet);
	}

	private void addStates(boolean hasWon) {
		double score = (hasWon) ? WON_SCORE : LOST_SCORE;
		while (!states.empty()) {
			double[] featureState = Arrays
			.stream(states.pop())
			.mapToDouble(Double::doubleValue)
			.toArray();
			double[] featureScore = { normalizeScore(score) };
			trainingSet.add(new DataSet(Nd4j.create(featureState), Nd4j.create(featureScore)));
			score = score * DISCOUNT;
		}
	}

	private double normalizeScore(double score) {
		return (score - LOST_SCORE) / (WON_SCORE - LOST_SCORE);
	}
}
