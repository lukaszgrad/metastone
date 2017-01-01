package net.demilich.metastone.game.behaviour.advanced;

import net.demilich.metastone.game.behaviour.training.TrainingSet;
import net.demilich.metastone.utils.UserHomeMetastone;
import org.neuroph.nnet.learning.BinaryDeltaRule;

import java.io.File;

/**
 * Created by lukaszgrad on 31/12/2016.
 */
public class TrainPerceptron {
	private static final String NETWORK_FILE_PATH =
		UserHomeMetastone.getPath() + File.separator + "perceptron";
	private static final String DATA_SET_PATH =
		UserHomeMetastone.getPath() + File.separator + "shaman_data_set2.norm";
	private static final double LEARNING_RATE = 0.1;
	private static final double MAX_ERROR = 0.0008;
	private static final int MAX_ITERATIONS = 1000;
	private static final int MAX_LEARNINGS = 10;

	public static void main(String[] args) {
		PerceptronEvaluate network = new PerceptronEvaluate(new SimpleFeatureExtractor());
		TrainingSet dataSet = TrainingSet.load(DATA_SET_PATH);

		network.setInput(dataSet.getRowAt(0).getInput());
		network.calculate();
		System.out.printf("%f\n", network.getOutput()[0]);

		System.out.println("Training size: " + dataSet.size());
		double error = MAX_ERROR;
		double rate = LEARNING_RATE;
		int iterations = MAX_ITERATIONS;
		for (int i = 0; i < MAX_LEARNINGS; i++) {
			network.getLearningRule().setMaxError(MAX_ERROR);
			network.getLearningRule().setMaxIterations(iterations);
			network.getLearningRule().setLearningRate(rate);
			network.learn(dataSet);
			System.out.printf("Learning error on try %d: %f\n", i,
				network.getLearningRule().getTotalNetworkError());
			network.getLearningRule().getErrorFunction().reset();
			//error =  error * 2 / 3;
			rate = Math.max(rate / 2, 0.01);
			//iterations += MAX_ITERATIONS / 2;
		}

		for (double weight : network.getWeights()) {
			System.out.printf("%f ", weight);
			System.out.println();
		}

		for (int i = 0; i < 10; i++) {
			network.setInput(dataSet.getRowAt(i).getInput());
			network.calculate();
			System.out.printf("Got %f, expected %f\n", network.getOutput()[0], dataSet.getRowAt(i).getDesiredOutput()[0]);
		}

		network.save(NETWORK_FILE_PATH);
	}
}
