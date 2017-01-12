package net.demilich.metastone.game.behaviour.training;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.behaviour.advanced.SimpleFeatureExtractor;
import net.demilich.metastone.game.cards.Card;
import net.demilich.metastone.game.cards.CardCatalogue;
import net.demilich.metastone.game.cards.CardSet;
import net.demilich.metastone.game.decks.Deck;
import net.demilich.metastone.game.decks.DeckFormat;
import net.demilich.metastone.game.entities.heroes.HeroClass;
import net.demilich.metastone.game.entities.heroes.MetaHero;
import net.demilich.metastone.game.gameconfig.PlayerConfig;
import net.demilich.metastone.game.logic.GameLogic;
import net.demilich.metastone.utils.ResourceInputStream;
import net.demilich.metastone.utils.ResourceLoader;
import net.demilich.metastone.utils.UserHomeMetastone;
import org.neuroph.core.data.DataSetRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * TODO: Move deck loading functions to shared utils
 *
 * Created by Lukasz Grad on 30/12/2016.
 */
public class CreateTrainingSet {
	private static Logger logger = LoggerFactory.getLogger(CreateTrainingSet.class);
	private static final int GAMES_PER_DECK = 10;
	private static final String DECKS_FOLDER = "decks";
	private static final String DECKS_FOLDER_PATH =
		UserHomeMetastone.getPath() + File.separator + DECKS_FOLDER;
	private static final String DECK_FORMAT_FOLDER_PATH =
		UserHomeMetastone.getPath() + File.separator + "formats";
	private static final String DATA_SET_PATH =
		UserHomeMetastone.getPath() + File.separator + "data_set3.norm";
	private static final String DATA_SET_PATH_TXT =
		UserHomeMetastone.getPath() + File.separator + "data_set3.norm.txt";

	public static void main(String[] args) {
		SimpleFeatureExtractor extractor = new SimpleFeatureExtractor();
		TrainingSet dataSet = new TrainingSet(extractor.length(), 1);
		List<Deck> decks = new ArrayList<>();
		DeckFormat deckFormat = new DeckFormat();
		deckFormat.setName("All");
		deckFormat.addSet(CardSet.CLASSIC);
		deckFormat.addSet(CardSet.BASIC);
		deckFormat.addSet(CardSet.PROMO);
		deckFormat.addSet(CardSet.NAXXRAMAS);
		deckFormat.addSet(CardSet.GOBLINS_VS_GNOMES);
		deckFormat.addSet(CardSet.BLACKROCK_MOUNTAIN);
		deckFormat.addSet(CardSet.REWARD);
		deckFormat.addSet(CardSet.THE_GRAND_TOURNAMENT);
		deckFormat.addSet(CardSet.LEAGUE_OF_EXPLORERS);
		deckFormat.addSet(CardSet.THE_OLD_GODS);
		deckFormat.addSet(CardSet.ONE_NIGHT_IN_KARAZHAN);
		deckFormat.addSet(CardSet.MEAN_STREETS_OF_GADGETZAN);
		deckFormat.addSet(CardSet.CUSTOM);
		try {
			CardCatalogue.loadCards();
			decks = loadStandardDecks(
				ResourceLoader.loadJsonInputStreams(DECKS_FOLDER_PATH, true),
				new GsonBuilder().setPrettyPrinting().create()
			);
		} catch (Exception e) {
			logger.error("Error during loading " + e.getMessage());
		}
		logger.info("Number of standard decks " + decks.size());
		//for (DeckFormat deckFormat : deckFormats) {
		//	logger.info("DeckFormats:");
		//	logger.info(deckFormat.toString());
		//}
		RecGameStateValueBehaviour behaviour1 = new RecGameStateValueBehaviour(new SimpleFeatureExtractor());
		RecGameStateValueBehaviour behaviour2 = new RecGameStateValueBehaviour(new SimpleFeatureExtractor());
		for (Deck deck : decks) {
			PlayerConfig config1 = new PlayerConfig(deck, behaviour1);
			PlayerConfig config2 = new PlayerConfig(deck, behaviour2);
			config1.setHeroCard(MetaHero.getHeroCard(deck.getHeroClass()));
			config2.setHeroCard(MetaHero.getHeroCard(deck.getHeroClass()));
			config1.build();
			config2.build();
			//player1.setBehaviour(behaviour1);
			//player2.setBehaviour(behaviour2);
			for (int i = 0; i < GAMES_PER_DECK; i++) {
				Player player1 = new Player(config1);
				Player player2 = new Player(config2);
				GameContext context = new GameContext(player1, player2, new GameLogic(), deckFormat);
				context.play();
				logger.info("Game took turns: " + context.getTurn());
				context.dispose();
			}
		}
		//addDataSets(dataSet, behaviour1.getTrainingSet());
		//addDataSets(dataSet, behaviour2.getTrainingSet());
		logger.info("Data set size: " + dataSet.size());
		//for (int i = 0; i < 100; i++) {
		//	logger.info("State: {}, value: {}", dataSet.getRowAt(i).getInput(), dataSet.getRowAt(i).getDesiredOutput());
		//}
		logger.info(dataSet.toCSV());
		logger.info("Data set saved in " + DATA_SET_PATH);
		dataSet.save(DATA_SET_PATH);
		dataSet.saveAsTxt(DATA_SET_PATH_TXT, ",");
	}

	private static void addDataSets(TrainingSet set1, TrainingSet set2) {
		for (DataSetRow row : set2.getRows()) {
			set1.addRow(row);
		}
	}

	private static List<Deck> loadStandardDecks(Collection<ResourceInputStream> inputStreams, Gson gson)
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
		return decks;
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

	private static List<DeckFormat> loadDeckFormats(Collection<ResourceInputStream> inputStreams, Gson gson)
			throws FileNotFoundException {
		List<DeckFormat> deckFormats = new ArrayList<>();
		for (ResourceInputStream resourceInputStream : inputStreams) {
			Reader reader = new InputStreamReader(resourceInputStream.inputStream);
			HashMap<String, Object> map = gson.fromJson(reader, new TypeToken<HashMap<String, Object>>() {}.getType());

			if (!map.containsKey("sets")) {
				logger.error("Deck {} does not specify a value for 'sets' and is therefore not valid", resourceInputStream.fileName);
				continue;
			}

			String deckName = (String) map.get("name");
			DeckFormat deckFormat = null;
			// this one is a meta deck; we need to parse those after all other
			// decks are done
			deckFormat = parseStandardDeckFormat(map);
			deckFormat.setName(deckName);
			deckFormat.setFilename(resourceInputStream.fileName);
			deckFormats.add(deckFormat);
		}
		return deckFormats;
	}

	private static DeckFormat parseStandardDeckFormat(Map<String, Object> map) {
		DeckFormat deckFormat = new DeckFormat();
		@SuppressWarnings("unchecked")
		List<String> setIds = (List<String>) map.get("sets");
		for (String setId : setIds) {
			for (CardSet set : CardSet.values()) {
				if (set.toString().equalsIgnoreCase(setId)) {
					deckFormat.addSet(set);
				}
			}
		}
		return deckFormat;

	}
}
