package net.demilich.metastone.game.behaviour.advanced;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.behaviour.IFeatureExtractor;
import net.demilich.metastone.game.behaviour.IStateEvaluate;
import org.neuroph.core.Layer;
import org.neuroph.core.NeuralNetwork;
import org.neuroph.core.Neuron;
import org.neuroph.core.input.Sum;
import org.neuroph.core.input.WeightedSum;
import org.neuroph.core.transfer.Linear;
import org.neuroph.core.transfer.Ramp;
import org.neuroph.core.transfer.Sigmoid;
import org.neuroph.core.transfer.Tanh;
import org.neuroph.nnet.MultiLayerPerceptron;
import org.neuroph.nnet.comp.layer.InputLayer;
import org.neuroph.nnet.comp.neuron.BiasNeuron;
import org.neuroph.nnet.comp.neuron.InputNeuron;
import org.neuroph.nnet.learning.BackPropagation;
import org.neuroph.nnet.learning.MomentumBackpropagation;
import org.neuroph.util.*;
import org.neuroph.util.random.GaussianRandomizer;
import org.neuroph.util.random.NguyenWidrowRandomizer;

import java.util.Arrays;

/**
 * Created by Lukasz Grad on 30/12/2016.
 */
public class NeuralNetworkEvaluate extends MultiLayerPerceptron
		implements IStateEvaluate<Double> {

	private transient IFeatureExtractor<Double> extractor;

	/*
	 * Create a neural network for a given feature extractor of size n.
	 * Network contains:    Input layer with n neurons
	 *                      ReLu layer with n neurons
	 *                      Sigmoid layer with 2n neurons
	 *                      Tanh layer with n neurons
	 *                      Linear output layer with 1 neuron
	 */
	public NeuralNetworkEvaluate(IFeatureExtractor<Double> extractor) {
		//this(extractor, new MomentumBackpropagation());
		super(extractor.length(), 26, 1);
		this.extractor = extractor;
		/*int size = extractor.length();

		this.setNetworkType(NeuralNetworkType.MULTI_LAYER_PERCEPTRON);
                NeuronProperties inputNeuronProperties = new NeuronProperties(InputNeuron.class, Linear.class);
                Layer inputLayer = LayerFactory.createLayer(size, inputNeuronProperties);
		inputLayer.addNeuron(new BiasNeuron());
                this.addLayer(inputLayer);

		NeuronProperties rampNeuronProperties = new NeuronProperties();
		rampNeuronProperties.setProperty("inputFunction", WeightedSum.class);
		rampNeuronProperties.setProperty("transferFunction", Ramp.class);
		rampNeuronProperties.setProperty("useBias", Boolean.valueOf(true));
		Layer reLuLayer = LayerFactory.createLayer(size, rampNeuronProperties);
		reLuLayer.addNeuron(new BiasNeuron());
		this.addLayer(reLuLayer);
		ConnectionFactory.fullConnect(inputLayer, reLuLayer);

		NeuronProperties sigmoidNeuronProperties = new NeuronProperties();
		sigmoidNeuronProperties.setProperty("inputFunction", WeightedSum.class);
		sigmoidNeuronProperties.setProperty("transferFunction", Sigmoid.class);
		sigmoidNeuronProperties.setProperty("useBias", Boolean.valueOf(true));
		Layer sigmoidLayer = LayerFactory.createLayer(size, sigmoidNeuronProperties);
		sigmoidLayer.addNeuron(new BiasNeuron());
		this.addLayer(sigmoidLayer);
		ConnectionFactory.fullConnect(reLuLayer, sigmoidLayer);

		NeuronProperties tanhNeuronProperties = new NeuronProperties();
		tanhNeuronProperties.setProperty("inputFunction", WeightedSum.class);
		tanhNeuronProperties.setProperty("transferFunction", Tanh.class);
		tanhNeuronProperties.setProperty("useBias", Boolean.valueOf(true));
		Layer tanhLayer = LayerFactory.createLayer(size, tanhNeuronProperties);
		tanhLayer.addNeuron(new BiasNeuron());
		this.addLayer(tanhLayer);
		ConnectionFactory.fullConnect(sigmoidLayer, tanhLayer);

		NeuronProperties outputNeuronProperties = new NeuronProperties();
		outputNeuronProperties.setProperty("inputFunction", WeightedSum.class);
		outputNeuronProperties.setProperty("transferFunction", Sigmoid.class);
		outputNeuronProperties.setProperty("useBias", Boolean.valueOf(true));
		Layer outputLayer = LayerFactory.createLayer(1, outputNeuronProperties);
		//Layer outputLayer = new Layer();
		//outputLayer.addNeuron(new Neuron(new WeightedSum(), new Linear(1)));
		this.addLayer(outputLayer);
		ConnectionFactory.fullConnect(tanhLayer, outputLayer);

                NeuralNetworkFactory.setDefaultIO(this);
                this.setLearningRule(new MomentumBackpropagation());
                this.randomizeWeights(new NguyenWidrowRandomizer(-0.7D, 0.7D));
                */
	}

	/*public NeuralNetworkEvaluate(IFeatureExtractor<Double> extractor, BackPropagation rule) {
		super(
			extractor.length(),
			extractor.length(),
			extractor.length(),
			1
		);
		setExtractor(extractor);
		setLearningRule(rule);
	}*/

	// TODO check if extractor size matches input layer size
	public void setExtractor(IFeatureExtractor<Double> extractor) {
		this.extractor = extractor;
	}

	public void setMomentum(double momentum) {
		((MomentumBackpropagation) getLearningRule()).setMomentum(momentum);
	}

	@Override
	public Double evaluate(GameContext context, int playerId) {
		double[] features = Arrays
			.stream(extractor.extract(context, playerId))
			.mapToDouble(Double::doubleValue)
			.toArray();
		setInput(features);
		calculate();
		return getOutput()[0];
	}

	public static NeuralNetworkEvaluate createFromFile(String filename, IFeatureExtractor<Double> extractor) {
		NeuralNetworkEvaluate network = (NeuralNetworkEvaluate) NeuralNetwork.createFromFile(filename);
		network.setExtractor(extractor);
		return network;
	}
}
