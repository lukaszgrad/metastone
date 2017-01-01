package net.demilich.metastone.game.behaviour.training;

import org.neuroph.core.data.DataSet;

import java.io.*;

/**
 * Created by Lukasz Grad on 30/12/2016.
 */
public class TrainingSet extends DataSet {

	public TrainingSet(int inputSize, int outputSize) {
		super(inputSize, outputSize);
	}

	/*
	 * Normalize given Training Set
	 */
	public void normalize() {
		// TODO IMPLEMENT
	}

	public static TrainingSet load(String filename) {
		return (TrainingSet) DataSet.load(filename);
	}

	/*
	@Override
	public void save(String filename) {
		try (Writer writer = new BufferedWriter(new OutputStreamWriter(
			new FileOutputStream(filename), "utf-8"))) {
			writer.write(toCSV());
			writer.flush();
			writer.close();
		} catch (Exception e) {
			System.out.println("Error: " + e.getMessage());
		}
	}
	*/
}
