package net.demilich.metastone.game.behaviour.advanced;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.actions.GameAction;
import net.demilich.metastone.game.behaviour.Behaviour;
import net.demilich.metastone.game.cards.Card;
import org.neuroph.core.NeuralNetwork;
import org.neuroph.nnet.Perceptron;

import java.util.List;

public class AdvancedBehaviour extends Behaviour {
	NeuralNetwork nn = new Perceptron(2, 1);

	@Override
	public String getName() {
		return null;
	}

	@Override
	public List<Card> mulligan(GameContext context, Player player, List<Card> cards) {
		return null;
	}

	@Override
	public GameAction requestAction(GameContext context, Player player, List<GameAction> validActions) {
		return null;
	}
}
