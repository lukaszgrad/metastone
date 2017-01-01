package net.demilich.metastone.game.behaviour.advanced;

import net.demilich.metastone.game.behaviour.training.TrainingSet;
import net.demilich.metastone.utils.UserHomeMetastone;
import org.neuroph.nnet.learning.MomentumBackpropagation;

import java.io.File;

/**
 * Created by lukaszgrad on 31/12/2016.
 */
public class EnhanceNeuralNetwork {
	private static final String NETWORK_FILE_PATH =
		UserHomeMetastone.getPath() + File.separator + "network";
	private static final String TRAINED_NETWORK_FILE_PATH =
		UserHomeMetastone.getPath() + File.separator + "network";
	private static final String DATA_SET_PATH =
		UserHomeMetastone.getPath() + File.separator + "shaman_data_set2.norm";
	private static final double LEARNING_RATE = 0.01;
	private static final double MOMENTUM = 0.01;
	private static final double MAX_ERROR = 0.002;
	private static final int MAX_ITERATIONS = 500;
	private static final int MAX_EPOCHS = 10;

	public static void main(String[] args) {

		NeuralNetworkEvaluate network = NeuralNetworkEvaluate.createFromFile(
			NETWORK_FILE_PATH,
			new SimpleFeatureExtractor()
		);

		TrainingSet dataSet = TrainingSet.load(DATA_SET_PATH);
		System.out.println("Training size: " + dataSet.size());



		double error = MAX_ERROR;
		double rate = LEARNING_RATE;
		double decay = LEARNING_RATE / MAX_EPOCHS;
		double lastError = Double.MAX_VALUE;
		network.setMomentum(MOMENTUM);
		//boolean better = true;
		//for (int i = 0; i < MAX_EPOCHS; i++) {
		//	rate = rate * 1 / (1 + decay * i);
			network.getLearningRule().setMaxIterations(MAX_ITERATIONS);
			network.getLearningRule().setMaxError(error);
			network.getLearningRule().setLearningRate(rate);
			System.out.println("Learning with rate: " + rate);
			network.learn(dataSet.sample(50)[0]);
			System.out.printf("Error: %f\n", network.getLearningRule().getTotalNetworkError());
			//if (network.getLearningRule().getTotalNetworkError() > lastError) {
			//	System.out.println("Total error increased, finishing");
				//better = false;
				//break;
			//}
			//lastError = network.getLearningRule().getTotalNetworkError();
			//error =  error * 2 / 3;
			//iterations *= 2;
			//if (better) {
			//}
			//better = true;
		//}
		network.save(TRAINED_NETWORK_FILE_PATH);
	}
}
