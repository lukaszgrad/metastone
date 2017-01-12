package net.demilich.metastone.game.decks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.demilich.metastone.game.cards.Card;
import net.demilich.metastone.game.cards.CardCatalogue;
import net.demilich.metastone.game.cards.CardParseException;
import net.demilich.metastone.game.entities.heroes.HeroClass;
import net.demilich.metastone.utils.ResourceInputStream;
import net.demilich.metastone.utils.ResourceLoader;
import net.demilich.metastone.utils.UserHomeMetastone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URISyntaxException;
import java.util.*;

/**
 * Created by lukaszgrad on 05/01/2017.
 */
public class DeckCatalogue {

	private static final String DECKS_FOLDER_PATH =
		UserHomeMetastone.getPath() + File.separator + "decks";
	private static Logger logger = LoggerFactory.getLogger(DeckCatalogue.class);
	private static List<Deck> deckCatalogue = new ArrayList<>();

	public static void loadLocalDecks() throws IOException, URISyntaxException, CardParseException {
		CardCatalogue.loadCards();
		loadStandardDecks(
			ResourceLoader.loadJsonInputStreams(DECKS_FOLDER_PATH, true),
			new GsonBuilder().setPrettyPrinting().create()
		);
	}

	private static void loadStandardDecks(Collection<ResourceInputStream> inputStreams, Gson gson)
		throws FileNotFoundException {
		List<Deck> decks = new ArrayList<>();
		for (ResourceInputStream resourceInputStream : inputStreams) {

			Reader reader = new InputStreamReader(resourceInputStream.inputStream);
			HashMap<String, Object> map = gson.fromJson(reader, new TypeToken<HashMap<String, Object>>() {
			}.getType());
			if (!map.containsKey("heroClass")) {
				logger.error("Deck {} does not speficy a value for 'heroClass' and is therefor not valid", resourceInputStream.fileName);
				continue;
			}
			HeroClass heroClass = HeroClass.valueOf((String) map.get("heroClass"));
			String deckName = (String) map.get("name");
			Deck deck = null;
			// this one is a meta deck; we need to parse those after all other
			// decks are done
			if (map.containsKey("decks")) {
				continue;
			} else {
				deck = parseStandardDeck(deckName, heroClass, map);
			}
			deck.setName(deckName);
			deck.setFilename(resourceInputStream.fileName);
			decks.add(deck);
		}
		deckCatalogue = decks;
	}

	private static Deck parseStandardDeck(String deckName, HeroClass heroClass, Map<String, Object> map) {
		boolean arbitrary = false;
		if (map.containsKey("arbitrary")) {
			arbitrary = (boolean) map.get("arbitrary");
		}
		Deck deck = new Deck(heroClass, arbitrary);
		@SuppressWarnings("unchecked")
		List<String> cardIds = (List<String>) map.get("cards");
		for (String cardId : cardIds) {
			Card card = CardCatalogue.getCardById(cardId);
			if (card == null) {
				logger.error("Deck {} contains invalid cardId '{}'", deckName, cardId);
				continue;
			}
			deck.getCards().add(card);
		}
		return deck;
	}

	public static Deck getDeckByName(String name) {
		for (Deck deck : deckCatalogue) {
			if (deck.getName().equals(name)) {
				return deck;
			}
		}
		return null;
	}
}
