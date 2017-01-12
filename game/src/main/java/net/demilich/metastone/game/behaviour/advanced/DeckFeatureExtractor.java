package net.demilich.metastone.game.behaviour.advanced;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.behaviour.IFeatureExtractor;
import net.demilich.metastone.game.cards.Card;
import net.demilich.metastone.game.cards.CardCollection;
import net.demilich.metastone.game.decks.Deck;

import java.util.Arrays;
import java.util.Comparator;
import java.util.TreeSet;

/**
 * Created by lukaszgrad on 05/01/2017.
 */
public class DeckFeatureExtractor extends SimpleFeatureExtractor implements IFeatureExtractor<Double> {
	private static final int DECK_SIZE = 30;
	public static final int MAX_FEATURES = SimpleFeatureExtractor.TOTAL_FEATURES + DECK_SIZE;
	public static final int MIDRANGE_SHAMAN_TOTAL_FEATURES = 80;

	private final CardCollection cards; // Unique cards of given deck

	private class CardComparator implements Comparator<Card> {
		@Override
		public int compare(Card o1, Card o2) {
			return o1.getCardId().compareTo(o2.getCardId());
		}
	}

	public DeckFeatureExtractor(Deck deck) {
		TreeSet<Card> uniqueCards = new TreeSet<>(new CardComparator());
		uniqueCards.addAll(deck.getCardsCopy().toList());
		System.out.println("Unique size: " + uniqueCards.size());
		this.cards = new CardCollection();
		this.cards.addAll(uniqueCards);
	}

	@Override
	public Double[] extract(GameContext context, int playerId) {
		Double[] features = super.extract(context, playerId);
		Double[] extracted = Arrays.copyOf(features, length());
		int index = SimpleFeatureExtractor.TOTAL_FEATURES;
		for (Card card : cards) {
			extracted[index++] = (context.getPlayer(playerId).getHand().contains(card)) ? 1.0 : 0.0;
		}
		return extracted;
	}

	@Override
	public int length() {
		return SimpleFeatureExtractor.TOTAL_FEATURES + cards.toList().size();
	}
}
