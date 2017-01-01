package net.demilich.metastone.game.behaviour;

import net.demilich.metastone.game.GameContext;

import java.util.Collection;

/**
 * Created by Lukasz Grad on 30/12/2016.
 */
public interface IFeatureExtractor<T> {
	T[] extract(GameContext context, int playerId);
	int length();
}
