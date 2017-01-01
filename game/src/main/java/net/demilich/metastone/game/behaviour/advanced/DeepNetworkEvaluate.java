package net.demilich.metastone.game.behaviour.advanced;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.behaviour.IStateEvaluate;

/**
 * Created by lukaszgrad on 01/01/2017.
 */
public class DeepNetworkEvaluate implements IStateEvaluate<Double> {
	MultiLayerNetwork model = new MultiLayerNetwork();

	@Override
	public Double evaluate(GameContext context, int playerId) {
		return null;
	}
}
