package net.demilich.metastone.game.behaviour.advanced;

import net.demilich.metastone.utils.UserHomeMetastone;
import org.deeplearning4j.datasets.iterator.impl.ListDataSetIterator;
import org.deeplearning4j.earlystopping.EarlyStoppingConfiguration;
import org.deeplearning4j.earlystopping.EarlyStoppingResult;
import org.deeplearning4j.earlystopping.scorecalc.DataSetLossCalculator;
import org.deeplearning4j.earlystopping.termination.MaxEpochsTerminationCondition;
import org.deeplearning4j.earlystopping.trainer.EarlyStoppingTrainer;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.LearningRatePolicy;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.ui.api.UIServer;
import org.deeplearning4j.ui.storage.FileStatsStorage;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.io.File;
import java.util.List;

/**
 * Created by lukaszgrad on 03/01/2017.
 */
public class EnhanceDeepNetwork {

	private static final String NETWORK_FILE_PATH =
		UserHomeMetastone.getPath() + File.separator + "deep_network";
	private static final String TRAINED_FILE_PATH =
		UserHomeMetastone.getPath() + File.separator + "deep_network";
	private static final String DATA_SET_PATH =
		UserHomeMetastone.getPath() + File.separator + "shaman_data_set500.data";
	private static final String VALIDATION_SET_PATH =
		UserHomeMetastone.getPath() + File.separator + "shaman_data_set100.data";
	private static final int ITERATIONS = 1;

	public static void main(String[] args) throws Exception {
		long seed = System.currentTimeMillis();
		double learningRate = 0.0001;
		int batchSize = 128;
		int nEpochs = 128;
		boolean collectStats = true;

		if (!collectStats) {
			UIServer uiServer = UIServer.getInstance();
			uiServer.attach(new FileStatsStorage(new File("stats.dl4j")));
		} else {
			DataSet validSet = new DataSet();
			validSet.load(new File(VALIDATION_SET_PATH));
			validSet.normalize();
			DataSet dataSet = new DataSet();
			dataSet.load(new File(DATA_SET_PATH));
			dataSet.normalize();
			System.out.println("Data set loaded, size: " + dataSet.asList().size());

			List<DataSet> dataList = dataSet.asList();
			//Collections.shuffle(dataList);
			DataSetIterator dataIter = new ListDataSetIterator(dataList, batchSize);

			MultiLayerNetwork model = null;
			try {
				model = ModelSerializer.restoreMultiLayerNetwork(NETWORK_FILE_PATH);
			} catch (Exception e) {
				System.out.println("Error loading model: " + e.getMessage());
			}

			EarlyStoppingConfiguration esConf = new EarlyStoppingConfiguration.Builder()
				.epochTerminationConditions(new MaxEpochsTerminationCondition(nEpochs))
				.scoreCalculator(new DataSetLossCalculator(new ListDataSetIterator(validSet.asList(), batchSize), true))
				.evaluateEveryNEpochs(1)
				.build();

			model.conf().setLearningRatePolicy(LearningRatePolicy.Inverse);
			EarlyStoppingTrainer trainer = new EarlyStoppingTrainer(esConf, model, dataIter);

			EarlyStoppingResult result = trainer.fit();

			//Print out the results:
			System.out.println("Termination reason: " + result.getTerminationReason());
			System.out.println("Termination details: " + result.getTerminationDetails());
			System.out.println("Total epochs: " + result.getTotalEpochs());
			System.out.println("Best epoch number: " + result.getBestModelEpoch());
			System.out.println("Score at best epoch: " + result.getBestModelScore());

			//for (int n = 0; n < nEpochs; n++) {
			//	while (dataIter.hasNext()) {
			//		model.fit(dataIter.next());
			//	}
			//	dataIter.reset();
			//	System.out.println("Model validation score: " + model.score(validSet));
			//}

			//System.out.println("Evaluate model....");
			//System.out.println("Model score: " + model.score(validSet));
			System.out.println("Saving network to " + TRAINED_FILE_PATH);
			ModelSerializer.writeModel(result.getBestModel(), new File(TRAINED_FILE_PATH), false);
		}
	}
}
