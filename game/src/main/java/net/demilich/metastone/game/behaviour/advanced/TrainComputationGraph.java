package net.demilich.metastone.game.behaviour.advanced;

import net.demilich.metastone.utils.UserHomeMetastone;
import org.deeplearning4j.api.storage.StatsStorage;
import org.deeplearning4j.datasets.iterator.IteratorMultiDataSetIterator;
import org.deeplearning4j.datasets.iterator.impl.ListDataSetIterator;
import org.deeplearning4j.earlystopping.EarlyStoppingConfiguration;
import org.deeplearning4j.earlystopping.EarlyStoppingResult;
import org.deeplearning4j.earlystopping.scorecalc.DataSetLossCalculator;
import org.deeplearning4j.earlystopping.scorecalc.DataSetLossCalculatorCG;
import org.deeplearning4j.earlystopping.scorecalc.ScoreCalculator;
import org.deeplearning4j.earlystopping.termination.MaxEpochsTerminationCondition;
import org.deeplearning4j.earlystopping.trainer.EarlyStoppingGraphTrainer;
import org.deeplearning4j.earlystopping.trainer.EarlyStoppingTrainer;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.nn.api.Model;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.*;
import org.deeplearning4j.nn.conf.graph.MergeVertex;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.graph.ComputationGraph;
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
import org.nd4j.linalg.dataset.MultiDataSet;
import org.nd4j.linalg.dataset.api.iterator.MultiDataSetIterator;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Created by lukaszgrad on 04/01/2017.
 */
public class TrainComputationGraph {

	private static final String NETWORK_FILE_PATH =
		UserHomeMetastone.getPath() + File.separator + "comp_graph";
	private static final String DATA_SET_PATH =
		UserHomeMetastone.getPath() + File.separator + "shaman_data_set500.data";
	private static final String VALIDATION_SET_PATH =
		UserHomeMetastone.getPath() + File.separator + "shaman_data_set200.data";
	private static final int ITERATIONS = 1;

	private static class MultiDataSetLossCalculator implements ScoreCalculator {
		MultiDataSet dataSet;

		public MultiDataSetLossCalculator(MultiDataSet dataSet) {
			this.dataSet = dataSet;
		}

		@Override
		public double calculateScore(Model network) {
			ComputationGraph graph = ((ComputationGraph) network).clone();
			graph.fit(dataSet);
			return graph.score();
		}
	}

	public static void main(String[] args) throws Exception {
		long seed = System.currentTimeMillis();
		double learningRate = 0.05;
		int batchSize = 128;
		int nEpochs = 64;
		boolean collectStats = true;

		if (!collectStats) {
			UIServer uiServer = UIServer.getInstance();
			uiServer.attach(new FileStatsStorage(new File("stats.dl4j")));
		} else {

			int numInputs = (new SimpleFeatureExtractor()).length();
			int numOutputs = 1;
			int numHiddenNodes = 1;

			DataSet validSet = new DataSet();
			validSet.load(new File(VALIDATION_SET_PATH));
			//validSet.normalize();
			DataSet dataSet = new DataSet();
			dataSet.load(new File(DATA_SET_PATH));
			//System.out.println(dataSet.toString());
			//dataSet.normalize();
			System.out.println("Data set loaded, size: " + dataSet.asList().size());
			//List<DataSet> dataList = dataSet.asList();
			//Collections.shuffle(dataList);
			//DataSetIterator dataIter = new ListDataSetIterator(dataList, batchSize);
			MultiDataSetIterator trainingIter = createMultiDataSetIterator(dataSet, batchSize);
			MultiDataSetIterator validationIter = createMultiDataSetIterator(validSet, batchSize);

			/*MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
				.seed(seed)
				.iterations(ITERATIONS)
				.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
				.learningRate(learningRate)
				.weightInit(WeightInit.RELU)
				.updater(Updater.NESTEROVS).momentum(0.7)
				//.regularization(true)
				//.l1(0.001)
				//.l2(0.0001)
				//.dropOut(0.5)
				.list()
				//.layer(0, new DenseLayer.Builder().nIn(numInputs).nOut(numHiddenNodes)
				//	.activation(Activation.RELU).build())
				//.layer(1, new DenseLayer.Builder().nIn(numHiddenNodes).nOut(numHiddenNodes)
				//	.activation(Activation.RELU).build())
				//.layer(2, new DenseLayer.Builder().nIn(numHiddenNodes).nOut(numHiddenNodes)
				//	.activation(Activation.RELU).build())
				.layer(0, new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
					.activation(Activation.IDENTITY)
					.nIn(numHiddenNodes).nOut(numOutputs).build())
				.pretrain(false).backprop(true).build();*/

			ComputationGraphConfiguration conf = new NeuralNetConfiguration.Builder()
				.seed(seed)
				.iterations(ITERATIONS)
				.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
				.learningRate(learningRate)
				.weightInit(WeightInit.RELU)
				.updater(Updater.NESTEROVS).momentum(0.7)
				.graphBuilder()
				.addInputs("input1", "input2")
				.addLayer("L0", new DenseLayer.Builder().nIn(numInputs - 1).nOut(numHiddenNodes)
					.activation(Activation.RELU).build(), "input1")
				//.addLayer("L1", new DenseLayer.Builder().nIn(numHiddenNodes).nOut(numHiddenNodes)
				//	.activation(Activation.RELU).build(), "L0")
				.addLayer("Out", new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
					.activation(Activation.SIGMOID)
					.nIn(numHiddenNodes + 1).nOut(numOutputs).build(), "L0", "input2")
				.setOutputs("Out")
				.pretrain(false)
				.backprop(true)
				.build();

			ComputationGraph graph = new ComputationGraph(conf);

			//MultiLayerNetwork model = new MultiLayerNetwork(conf);
			//model.init();
			//StatsStorage statsStorage = new FileStatsStorage(new File("stats.dl4j"));

			//model.setListeners(
			//	new ScoreIterationListener(10),
			//	new StatsListener(statsStorage, 10)
			//);  //Print score every 128 parameter updates
			EarlyStoppingConfiguration esConf = new EarlyStoppingConfiguration.Builder()
				.epochTerminationConditions(new MaxEpochsTerminationCondition(nEpochs))
				.scoreCalculator(new DataSetLossCalculatorCG(validationIter, true))
				.build();

			EarlyStoppingGraphTrainer trainer = new EarlyStoppingGraphTrainer(esConf, graph, trainingIter, null);

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
			System.out.println("Saving network to " + NETWORK_FILE_PATH);
			ModelSerializer.writeModel(result.getBestModel(), new File(NETWORK_FILE_PATH), false);
		}
	}

	private static MultiDataSet createMultiDataSet(DataSet dataSet) {
		int featuresSize = dataSet.getFeatures().columns();
		INDArray[] inputs = {
			dataSet.getFeatures().getColumns(IntStream.range(0, featuresSize - 1).toArray()),
			dataSet.getFeatures().getColumn(featuresSize - 1)
		};
		//System.out.printf("Input sizes: %d, %d\n", inputs[0].columns(), inputs[1].columns());
		INDArray[] outputs = { dataSet.getLabels() };
		return new MultiDataSet(inputs, outputs);
	}

	private static MultiDataSetIterator createMultiDataSetIterator(DataSet dataSet, int batchSize) {
		return new ListMultiDataSetIterator(createMultiDataSet(dataSet).asList(), batchSize);
	}
}
