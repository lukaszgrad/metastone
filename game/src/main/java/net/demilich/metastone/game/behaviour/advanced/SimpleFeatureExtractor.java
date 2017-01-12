package net.demilich.metastone.game.behaviour.advanced;

import net.demilich.metastone.game.Attribute;
import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.behaviour.IFeatureExtractor;
import net.demilich.metastone.game.behaviour.threat.FeatureVector;
import net.demilich.metastone.game.behaviour.threat.ThreatBasedHeuristic;
import net.demilich.metastone.game.behaviour.threat.ThreatLevel;
import net.demilich.metastone.game.behaviour.training.NormalizedThreatBasedHeuristic;
import net.demilich.metastone.game.entities.minions.Minion;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Created by Lukasz Grad on 30/12/2016.
 */
public class SimpleFeatureExtractor implements IFeatureExtractor<Double> {
	private static final int FEATURES_PER_MINION = 3;
	private static final int MINIONS = 7;
	private static final int HERO_FEATURES = 10;
	private static final int TOTAL_PLAYER_FEATURES =
		HERO_FEATURES + FEATURES_PER_MINION * MINIONS;
	private static final int PLAYERS = 2;
	public static final int TOTAL_FEATURES = PLAYERS * TOTAL_PLAYER_FEATURES + 1;

	// Some standard maximum values to normalize feature vector
	private static final double MAX_HP = 60;
	private static final double MAX_MANA = 10;
	private static final double HAND_SIZE = 10;
	private static final double MAX_WEAPON_DMG = 7;
	private static final double MAX_WEAPON_DUR = 5;
	private static final double DECK_COUNT = 30;
	private static final double MAX_MINION_HP = 12;
	private static final double MAX_MINION_DMG = 12;
	private static final double MAX_MINION_SCORE = 100; // Based on FeatureVector fittest vector
	private static final double MAX_SCORE = 200;

	private final ThreatBasedHeuristic heuristic;

	public SimpleFeatureExtractor() {
		heuristic = new NormalizedThreatBasedHeuristic(FeatureVector.getFittest());
	}

	@Override
	public Double[] extract(GameContext context, int playerId) {
		double[] features = new double[TOTAL_FEATURES];
		Arrays.fill(features, 0);
		Player player = context.getPlayer(playerId);
		ThreatLevel playerThreatLevel = ThreatBasedHeuristic.calcuateThreatLevel(context, playerId);
		ThreatLevel opponentThreatLevel =
			ThreatBasedHeuristic.calcuateThreatLevel(context, context.getOpponent(player).getId());
		extractPlayer(player, context.getOpponent(player), playerThreatLevel, features, 0);
		extractPlayer(context.getOpponent(player), player, opponentThreatLevel, features, TOTAL_PLAYER_FEATURES);
		features[TOTAL_FEATURES - 1] = heuristic.getScore(context, playerId) / MAX_SCORE;
		return Arrays.stream(features).boxed().toArray(Double[]::new);
	}

	private void extractPlayer(Player player,
	                           Player opponent,
	                           ThreatLevel threatLevel,
	                           double[] features,
	                           int shift) {
		int index = shift;
		features[index++] = (player.getHero().isDestroyed()) ? 1.0 : 0.0;
		features[index++] = /*1 / (8 * Math.exp(Math.max(*/(player.getHero().getEffectiveHp()) / MAX_HP;
		int remainingHp = remainingHp(player, opponent);
		features[index++] = /*1/(8 * Math.exp(Math.max((*/remainingHp / MAX_HP;
		features[index++] = (remainingHp < 15) ? 1.0 : 0.0;
		features[index++] = (remainingHp < 1) ? 1.0 : 0.0;
		//features[index++] = player.getHand().getCount() / HAND_SIZE;
		boolean hasWeapon = player.getHero().getWeapon() != null;
		features[index++] = (hasWeapon) ? player.getHero().getWeapon().getWeaponDamage() / MAX_WEAPON_DMG : 0;
		features[index++] = (hasWeapon) ? player.getHero().getWeapon().getDurability() / MAX_WEAPON_DUR: 0;
		features[index++] = player.getMinions().size() / MINIONS;
		//features[index++] = player.getDeck().getCount() / DECK_COUNT;
		features[index++] = player.getMaxMana() / MAX_MANA;
		features[index++] = player.getLockedMana() / MAX_MANA;
		for (Minion minion : player.getMinions()) {
			features[index++] = minion.getHp() / MAX_MINION_HP;
			features[index++] = minion.getAttack() / MAX_MINION_DMG;
			//features[index++] = heuristic.calculateMinionScore(minion, threatLevel) / MAX_MINION_SCORE;
			features[index++] = (minion.canAttackThisTurn()) ? 1.0 : 0.0;
		}
	}

	private int remainingHp(Player player, Player opponent) {
		int damageOnBoard = 0;
		for (Minion minion : opponent.getMinions()) {
			damageOnBoard += minion.getAttack() * minion.getAttributeValue(Attribute.NUMBER_OF_ATTACKS);
		}
		damageOnBoard += opponent.getHero().getAttack();
		return player.getHero().getEffectiveHp() - damageOnBoard;
	}

	@Override
	public int length(){
		return TOTAL_FEATURES;
	}
}
