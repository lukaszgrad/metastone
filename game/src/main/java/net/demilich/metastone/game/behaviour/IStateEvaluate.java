package net.demilich.metastone.game.behaviour;

import net.demilich.metastone.game.GameContext;

public interface IStateEvaluate<T> {
	T evaluate(GameContext context, int playerId);
}
