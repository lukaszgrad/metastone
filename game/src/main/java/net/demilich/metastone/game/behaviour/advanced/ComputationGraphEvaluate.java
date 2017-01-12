package net.demilich.metastone.game.behaviour.advanced;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.behaviour.IFeatureExtractor;
import net.demilich.metastone.game.behaviour.IStateEvaluate;
import net.demilich.metastone.game.behaviour.threat.FeatureVector;
import net.demilich.metastone.game.behaviour.training.NormalizedThreatBasedHeuristic;
import net.demilich.metastone.game.spells.DoubleAttackSpell;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.factory.Nd4j;

import java.util.Arrays;

/**
 * Created by lukaszgrad on 01/01/2017.
 */
public class ComputationGraphEvaluate implements IStateEvaluate<Double> {
	private ComputationGraph model;
	private IFeatureExtractor<Double> extractor;

	public ComputationGraphEvaluate(IFeatureExtractor<Double> extractor, ComputationGraph model) {
		this.extractor = extractor;
		this.model = model;
	}

	@Override
	public Double evaluate(GameContext context, int playerId) {
		double[] extracted = Arrays.stream(extractor.extract(context, playerId))
			.mapToDouble(Double::doubleValue)
			.toArray();
		double[] features = Arrays.copyOf(extracted, extracted.length - 1);
		double[] score = { extracted[extracted.length - 1] };
		double value = model.output(Nd4j.create(features), Nd4j.create(score))[0].getDouble(0);
		//System.out.println("Evaluate value: " + value);
		return value;
	}
}
