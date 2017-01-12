package net.demilich.metastone.game.behaviour.training;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.behaviour.advanced.DeckFeatureExtractor;
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
import org.nd4j.linalg.dataset.DataSet;
import org.neuroph.core.data.DataSetRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * Created by lukaszgrad on 31/12/2016.
 */
public class CreateTrainingSetDeck {

	private static Logger logger = LoggerFactory.getLogger(CreateTrainingSet.class);
	private static final int GAMES_PER_DECK = 20;
	private static final String DECKS_FOLDER = "decks";
	private static final String DECK_NAME = "Midrange Shaman";
	private static final String DECKS_FOLDER_PATH =
		UserHomeMetastone.getPath() + File.separator + DECKS_FOLDER;
	private static final String DATA_SET_PATH =
		UserHomeMetastone.getPath() + File.separator + "shaman_data_set20.data";
	private static final String DATA_SET_PATH_TXT =
		UserHomeMetastone.getPath() + File.separator + "shaman_data_set20.data.txt";

	public static void main(String[] args) {
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
		Deck trainingDeck = null;
		for (Deck deck : decks) {
			if (deck.getName().equals(DECK_NAME))
				trainingDeck = deck;
		}
		logger.info(trainingDeck.getName());
		DeckFeatureExtractor extractor = new DeckFeatureExtractor(trainingDeck);
		logger.info("Extractor length: {}", extractor.length());
		RecGameStateValueBehaviour behaviour1 = new RecGameStateValueBehaviour(new DeckFeatureExtractor(trainingDeck));
		RecGameStateValueBehaviour behaviour2 = new RecGameStateValueBehaviour(new DeckFeatureExtractor(trainingDeck));
		PlayerConfig config1 = new PlayerConfig(trainingDeck, behaviour1);
		PlayerConfig config2 = new PlayerConfig(trainingDeck, behaviour2);
		config1.setHeroCard(MetaHero.getHeroCard(trainingDeck.getHeroClass()));
		config2.setHeroCard(MetaHero.getHeroCard(trainingDeck.getHeroClass()));
		config1.build();
		config2.build();
		for (int i = 0; i < GAMES_PER_DECK; i++) {
			Player player1 = new Player(config1);
			Player player2 = new Player(config2);
			GameContext context = new GameContext(player1, player2, new GameLogic(), deckFormat);
			context.play();
			logger.info("Game took turns: " + context.getTurn());
			context.dispose();
		}
		DataSet[] sets = {behaviour1.getTrainingSet(), behaviour2.getTrainingSet()};
		DataSet dataSet = DataSet.merge(Arrays.asList(sets));
		logger.info("Data set size: " + dataSet.asList().size());
		//logger.info(dataSet.toString());
		logger.info("Data set saved in " + DATA_SET_PATH);
		dataSet.save(new File(DATA_SET_PATH));
		try{
			PrintWriter writer = new PrintWriter(DATA_SET_PATH_TXT, "UTF-8");
			for (DataSet dataRow : dataSet.asList()) {
				writer.printf("%f, %f\n",
					dataRow.getFeatures().getDouble(extractor.length() - 1),
					dataRow.getLabels().getDouble(0)
				);
			}
			writer.close();
		} catch (IOException e) {
			// do something
		}
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
