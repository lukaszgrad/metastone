package net.demilich.metastone.game.behaviour.advanced;

import net.demilich.metastone.utils.UserHomeMetastone;
import org.deeplearning4j.api.storage.StatsStorage;
import org.deeplearning4j.datasets.iterator.impl.ListDataSetIterator;
import org.deeplearning4j.earlystopping.EarlyStoppingConfiguration;
import org.deeplearning4j.earlystopping.EarlyStoppingResult;
import org.deeplearning4j.earlystopping.scorecalc.DataSetLossCalculator;
import org.deeplearning4j.earlystopping.termination.MaxEpochsTerminationCondition;
import org.deeplearning4j.earlystopping.trainer.EarlyStoppingTrainer;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.eval.RegressionEvaluation;
import org.deeplearning4j.nn.api.Model;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.*;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.GravesLSTM;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.api.IterationListener;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.ui.api.UIServer;
import org.deeplearning4j.ui.stats.StatsListener;
import org.deeplearning4j.ui.storage.FileStatsStorage;
import org.deeplearning4j.ui.storage.InMemoryStatsStorage;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;

import java.io.File;
import java.util.*;

/**
 * Created by lukaszgrad on 01/01/2017.
 */
public class TrainDeepNetwork {

	private static final String NETWORK_FILE_PATH =
		UserHomeMetastone.getPath() + File.separator + "deep_network";
	private static final String DATA_SET_PATH =
		UserHomeMetastone.getPath() + File.separator + "shaman_data_set100.data";
	private static final String VALIDATION_SET_PATH =
		UserHomeMetastone.getPath() + File.separator + "shaman_data_set20.data";
	private static final int ITERATIONS = 1;

	public static void main(String[] args) throws Exception {
		long seed = System.currentTimeMillis();
		double learningRate = 0.1;
		int batchSize = 128;
		int nEpochs = 16;
		boolean collectStats = true;

		if (!collectStats) {
			UIServer uiServer = UIServer.getInstance();
			uiServer.attach(new FileStatsStorage(new File("stats.dl4j")));
		} else {
			int numInputs = DeckFeatureExtractor.MIDRANGE_SHAMAN_TOTAL_FEATURES;
			int numOutputs = 1;
			int lstmLayerSize = 2 * numInputs;
			int numHiddenNodes = numInputs / 2;

			DataSet validSet = new DataSet();
			validSet.load(new File(VALIDATION_SET_PATH));
			//validSet = filterDataset(validSet, 0.43, 0.57);
		 	List<DataSet> validList = addTimeSteps(validSet, numInputs);
			//System.out.println(validSet.toString());
			//validSet.normalize();
			DataSet dataSet = new DataSet();
			dataSet.load(new File(DATA_SET_PATH));
			//dataSet.normalize();
			System.out.println("Data set loaded, size: " + dataSet.asList().size());
			//dataSet = filterDataset(dataSet, 0.43, 0.57);
			List<DataSet> dataList = addTimeSteps(dataSet, numInputs);
			//for (DataSet example : dataList.subList(0, 30))
				//System.out.println(example.toString());
			System.out.println("Examples set loaded, size: " + dataList.size());


			//List<DataSet> dataList = dataSet.asList();
			//Collections.shuffle(dataList);
			DataSetIterator dataIter = new ListDataSetIterator(dataList, 20);
			DataSetIterator validIter = new ListDataSetIterator(validList, 1);

			MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
				.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
				.iterations(1)
				.learningRate(0.01)
				//.rmsDecay(0.95)
				.seed(seed)
				.regularization(true)
				.l2(0.0001)
				.weightInit(WeightInit.XAVIER)
				.updater(Updater.RMSPROP)
				//.momentum(0.7)
				.list()
				.layer(0, new GravesLSTM.Builder().nIn(numInputs).nOut(lstmLayerSize)
					.activation(Activation.SIGMOID).build())
				//.layer(1, new GravesLSTM.Builder().nIn(lstmLayerSize).nOut(lstmLayerSize)
				//	.activation(Activation.SIGMOID).build())
				.layer(1, new RnnOutputLayer.Builder(LossFunctions.LossFunction.MSE).activation(Activation.SIGMOID)
					.nIn(lstmLayerSize).nOut(numOutputs).build())
				.backpropType(BackpropType.TruncatedBPTT).tBPTTForwardLength(2).tBPTTBackwardLength(2)
				.pretrain(false).backprop(true)
				.build();

			MultiLayerNetwork net = new MultiLayerNetwork(conf);
			net.init();

			net.setListeners(new ScoreIterationListener(100));

			// ----- Train the network, evaluating the test set performance at each epoch -----
			//int nEpochs = 50;

			for (int i = 0; i < nEpochs; i++) {
				while (dataIter.hasNext()) {
					net.fit(dataIter.next());
				}
				dataIter.reset();
				//System.out.println("Epoch " + i + " complete. Time series evaluation:");

				RegressionEvaluation evaluation = new RegressionEvaluation(numInputs);

				/*while(validIter.hasNext()){
					DataSet t = validIter.next();
					//INDArray features = t.getFeatureMatrix();
					//INDArray lables = t.getLabels();
					//INDArray predicted = net.output(features,false,t.getFeaturesMaskArray(), t.getLabelsMaskArray());
					System.out.println("Validation set score: " + net.score(t, false));
					//evaluation.evalTimeSeries(lables,predicted, t.getLabelsMaskArray());
				}8*/
				//System.out.println(evaluation.stats());
				if (i % 8 == 0)
					System.out.println("Validation set score: " + net.score(DataSet.merge(validList), false));

				//validIter.reset();
			}

			/*MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
				.seed(seed)
				.iterations(ITERATIONS)
				.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
				.learningRate(learningRate)
				.weightInit(WeightInit.RELU)
				.updater(Updater.NESTEROVS).momentum(0.7)
				.regularization(true)
				//.l1(0.001)
				.l2(0.0001)
				//.dropOut(0.5)
				.list()
				.layer(0, new DenseLayer.Builder().nIn(numInputs).nOut(numHiddenNodes)
					.activation(Activation.RELU).build())
				//.layer(1, new DenseLayer.Builder().nIn(numHiddenNodes).nOut(numHiddenNodes)
				//	.activation(Activation.RELU).build())
				//.layer(2, new DenseLayer.Builder().nIn(numHiddenNodes).nOut(numHiddenNodes)
				//	.activation(Activation.RELU).build())
				.layer(1, new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
					.activation(Activation.SIGMOID)
					.nIn(numHiddenNodes).nOut(numOutputs).build())
				.pretrain(false).backprop(true).build();
*/
			/*EarlyStoppingConfiguration esConf = new EarlyStoppingConfiguration.Builder()
				.epochTerminationConditions(new MaxEpochsTerminationCondition(nEpochs))
				.scoreCalculator(new DataSetLossCalculator(new ListDataSetIterator(validList, 20), true))
				.evaluateEveryNEpochs(1)
				.build();

			EarlyStoppingTrainer trainer = new EarlyStoppingTrainer(esConf, conf, dataIter);

			EarlyStoppingResult result = trainer.fit();

			//Print out the results:
			System.out.println("Termination reason: " + result.getTerminationReason());
			System.out.println("Termination details: " + result.getTerminationDetails());
			System.out.println("Total epochs: " + result.getTotalEpochs());
			System.out.println("Best epoch number: " + result.getBestModelEpoch());
			System.out.println("Score at best epoch: " + result.getBestModelScore());
			*/
			for (int i = 0; i < 4; i++) {
				//MultiLayerNetwork network = (MultiLayerNetwork) result.getBestModel();
				//double value = network.output(validSet.getFeatureMatrix().getRow(i)).getDouble(0);
				//double output = validSet.getLabels().getDouble(i);
				//System.out.printf("Network score: %f, label: %f\n", value, output);
				DataSet row = validList.get(i);
				INDArray output = net.output(row.getFeatures(), false, row.getFeaturesMaskArray(), row.getLabelsMaskArray());
				System.out.println("Prediction: " + output.toString());
				System.out.println("Real values: " + row.getLabels().toString());
			}

			System.out.println("Saving network to " + NETWORK_FILE_PATH);
			ModelSerializer.writeModel(net /*result.getBestModel()*/, new File(NETWORK_FILE_PATH), false);
		}
	}

	private static DataSet filterDataset(DataSet set, double minLabel, double maxLabel) {
		List<DataSet> filtered = new ArrayList<>();
		for (DataSet row : set.asList()) {
			if ((row.getLabels().getDouble(0) < minLabel) || (row.getLabels().getDouble(0) > maxLabel)) {
				filtered.add(row);
			}
		}
		return DataSet.merge(filtered);
	}

	private static int maxSequencelength(DataSet set) {
		List<DataSet> setList = set.asList();
		int maxLength = 0;
		int currentLength = 0;
		for (DataSet row : set.asList()) {
			if (startsNewSequence(row)) {
				maxLength = Math.max(maxLength, currentLength);
				currentLength = 0;
			}
			currentLength++;
		}
		// Do not forget to check last sequence
		return Math.max(maxLength, currentLength);
	}

	private static List<DataSet> addTimeSteps(DataSet set, int numInputs) {
		List<DataSet> setList = set.asList();
		List<DataSet> dataSet = new ArrayList<>();
		LinkedList<DataSet> currentSet = new LinkedList<>();
		currentSet.add(setList.get(0));
		setList.remove(0);
		int maxSequenceLength = maxSequencelength(set);
		//System.out.println(maxSequenceLength);
		for (DataSet row : setList) {
			if (startsNewSequence(row)) {
				INDArray input = Nd4j.zeros(1, numInputs, maxSequenceLength);
				INDArray labels = Nd4j.zeros(1, 1, maxSequenceLength);
				INDArray mask = Nd4j.zeros(1, maxSequenceLength);
				int current = 0;
				Iterator<DataSet> iter = currentSet.descendingIterator();
				while (iter.hasNext()) {
					DataSet r = iter.next();
					for (int i = 0; i < numInputs; i++)
						input.putScalar(new int[]{0, i, current}, r.getFeatures().getDouble(i));
					labels.putScalar(new int[]{0, 0, current}, r.getLabels().getDouble(0));
					mask.putScalar(new int[] {0, current}, 1);
					current++;
				}
				currentSet.clear();
				dataSet.add(new DataSet(input, labels, mask, mask));
			}
			currentSet.add(row);
		}
		INDArray input = Nd4j.zeros(1, numInputs, maxSequenceLength);
		INDArray labels = Nd4j.zeros(1, 1, maxSequenceLength);
		INDArray mask = Nd4j.zeros(1, maxSequenceLength);
		int current = 0;
		Iterator<DataSet> iter = currentSet.descendingIterator();
		while (iter.hasNext()) {
			DataSet r = iter.next();
			for (int i = 0; i < numInputs; i++)
				input.putScalar(new int[]{0, i, current}, r.getFeatures().getDouble(i));
			labels.putScalar(new int[]{0, 0, current}, r.getLabels().getDouble(0));
			mask.putScalar(new int[] {0, current}, 1);
			current++;
		}
		currentSet.clear();
		dataSet.add(new DataSet(input, labels, mask, mask));
		return dataSet;//DataSet.merge(dataSet);
	}

	private static boolean startsNewSequence(DataSet row) {
		return (row.getLabels().getDouble(0) == 0.0 || row.getLabels().getDouble(0) == 1.0);
	}
}