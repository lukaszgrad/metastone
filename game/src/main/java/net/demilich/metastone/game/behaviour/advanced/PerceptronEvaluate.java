package net.demilich.metastone.game.behaviour.advanced;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.behaviour.IFeatureExtractor;
import net.demilich.metastone.game.behaviour.IStateEvaluate;
import net.demilich.metastone.game.behaviour.threat.FeatureVector;
import net.demilich.metastone.game.behaviour.training.NormalizedThreatBasedHeuristic;
import org.neuroph.nnet.Perceptron;
import org.neuroph.nnet.learning.PerceptronLearning;
import org.neuroph.util.TransferFunctionType;

import java.util.Arrays;

/**
 * Created by lukaszgrad on 31/12/2016.
 */
public class PerceptronEvaluate extends Perceptron
	implements IStateEvaluate<Double> {
	private transient IFeatureExtractor<Double> extractor;

	public PerceptronEvaluate(IFeatureExtractor<Double> extractor) {
		super(extractor.length() + 1, 1, TransferFunctionType.LINEAR);
		this.extractor = extractor;
	}

	@Override
	public Double evaluate(GameContext context, int playerId) {
		double[] features = Arrays
			.stream(extractor.extract(context, playerId))
			.mapToDouble(Double::doubleValue)
			.toArray();
		double[] extended = Arrays.copyOf(features, features.length + 1);
		extended[features.length] = (new NormalizedThreatBasedHeuristic(FeatureVector.getFittest()).getScore(context, playerId));
		setInput(extended);
		calculate();
		return getOutput()[0];
	}

	public void setExtractor(IFeatureExtractor<Double> extractor) {
		this.extractor = extractor;
	}

	public PerceptronLearning getLearningRule() {
		return (PerceptronLearning) super.getLearningRule();
	}

	public static PerceptronEvaluate createFromFile(String filename, IFeatureExtractor<Double> extractor) {
		PerceptronEvaluate network = (PerceptronEvaluate) Perceptron.createFromFile(filename);
		network.setExtractor(extractor);
		return network;
	}
}
