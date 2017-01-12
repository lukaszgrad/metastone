package net.demilich.metastone.game.behaviour.advanced;

import org.deeplearning4j.nn.api.Model;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.optimize.api.IterationListener;
import org.nd4j.linalg.dataset.DataSet;

/**
 * Created by lukaszgrad on 02/01/2017.
 */

public class TestIterationListener implements IterationListener {
	private boolean invoked;
	private final DataSet testSet;
	private final int iterations;
	private int current;

	public TestIterationListener(int iterations, DataSet testSet) {
		this.iterations = iterations;
		this.current = 0;
		this.testSet = testSet;
	}

	@Override
	public boolean invoked() {
		boolean ret = invoked;
		if (invoked)
			invoked = false;
		return ret;
	}

	@Override
	public void invoke() {
		current++;
		if (current % iterations == 0) {
			invoked = true;
		}
	}

	@Override
	public void iterationDone(Model model, int iteration) {
		System.out.printf("Test set score: %.12f on iteration %d\n",
			((MultiLayerNetwork) model).score(testSet, false), iteration);
	}
}
