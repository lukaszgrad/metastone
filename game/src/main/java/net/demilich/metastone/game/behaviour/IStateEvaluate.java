package net.demilich.metastone.game.behaviour;

import net.demilich.metastone.game.GameContext;

/**
 * Created by Lukasz Grad on 29.12.16.
 */
public interface IStateEvaluate<T> {
	T evaluate(GameContext context, int playerId);
}
