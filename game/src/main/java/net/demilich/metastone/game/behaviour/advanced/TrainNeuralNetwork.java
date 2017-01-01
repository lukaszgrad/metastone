package net.demilich.metastone.game.behaviour.advanced;

import net.demilich.metastone.game.behaviour.training.TrainingSet;
import net.demilich.metastone.game.spells.DoubleAttackSpell;
import net.demilich.metastone.utils.UserHomeMetastone;

import java.io.File;

/**
 * Created by Lukasz Grad on 30/12/2016.
 */
public class TrainNeuralNetwork {
	private static final String NETWORK_FILE_PATH =
		UserHomeMetastone.getPath() + File.separator + "network";
	private static final String DATA_SET_PATH =
		UserHomeMetastone.getPath() + File.separator + "shaman_data_set2.norm";
	private static final double LEARNING_RATE = 0.1;
	private static final double MAX_ERROR = 0.0002;
	private static final int MAX_ITERATIONS = 1000;
	private static final int MAX_LEARNINGS = 10;

	public static void main(String[] args) {
		NeuralNetworkEvaluate network = new NeuralNetworkEvaluate(new SimpleFeatureExtractor());
		//network.connectInputsToOutputs();

		TrainingSet dataSet = TrainingSet.load(DATA_SET_PATH);
		//RangeNormalizer normalizer = new RangeNormalizer(0, 1);
		//normalizer.normalize(dataSet);

		for (int i = 0; i < 10; i++) {
			network.setInput(dataSet.getRowAt(i).getInput());
			network.calculate();
			System.out.printf("Got %f, expected %f\n", network.getOutput()[0], dataSet.getRowAt(i).getDesiredOutput()[0]);
		}

		System.out.println("Training size: " + dataSet.size());
		double error = MAX_ERROR;
		double rate = LEARNING_RATE;
		int iterations = MAX_ITERATIONS;
		double last = Double.MAX_VALUE;
		double best = Double.MAX_VALUE;
		for (int i = 0; i < MAX_LEARNINGS; i++) {
			network.getLearningRule().setMaxIterations(iterations);
			network.getLearningRule().setMaxError(error);
			network.getLearningRule().setLearningRate(rate);
			network.learn(dataSet);
			System.out.printf("Learning error on try %d: %.12f\n", i, network.getLearningRule().getTotalNetworkError());
			//error =  error * 2 / 3;
			double current= network.getLearningRule().getTotalNetworkError();
			if (current >= last) {
				System.out.println("Error increased, lowering rate and momentum");
				rate = Math.max(rate / 2, 0.02);
				network.setMomentum(0.05);
			} else if (current < best) {
				best = current;
				System.out.println("Saving best network");
				network.save(NETWORK_FILE_PATH);
			}
			last = current;
			//iterations *= 2;
		}

		for (int i = 0; i < 10; i++) {
			network.setInput(dataSet.getRowAt(i).getInput());
			network.calculate();
			System.out.printf("Got %f, expected %f\n", network.getOutput()[0], dataSet.getRowAt(i).getDesiredOutput()[0]);
		}
	}
}
