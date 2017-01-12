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
public class DeepNetworkEvaluate implements IStateEvaluate<Double> {
	private MultiLayerNetwork model;
	private IFeatureExtractor<Double> extractor;

	public DeepNetworkEvaluate(IFeatureExtractor<Double> extractor, MultiLayerNetwork model) {
		this.extractor = extractor;
		this.model = model;
	}

	@Override
	public Double evaluate(GameContext context, int playerId) {
		double[] extracted = Arrays.stream(extractor.extract(context, playerId))
			.mapToDouble(Double::doubleValue)
			.toArray();
		double value = model.output(Nd4j.create(extracted)).getDouble(0);
		//System.out.println("Evaluate value: " + value);
		return value;
	}
}
