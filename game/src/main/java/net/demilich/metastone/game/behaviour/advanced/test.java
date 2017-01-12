package net.demilich.metastone.game.behaviour.advanced;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.cpu.nativecpu.NDArray;
import org.nd4j.linalg.factory.Nd4j;

/**
 * Created by lukaszgrad on 06/01/2017.
 */
public class test {
	public static void main(String[] args) {
		int[] shape = {2, 3};
		double[] data = {1,2,3,4,5,6};
		INDArray array = Nd4j.create(data, shape);

		double[][] data2 = {{1,2,3},{4,5,6}};
		INDArray array2 = Nd4j.create(data2);
		System.out.println(array2.toString());
	}
}
