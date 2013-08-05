package org.disk.collection.test;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Random;
import java.util.logging.Logger;

import org.disk.collection.MMapIntArrayException;
import org.disk.collection.MemoryMappedIntArray;

public class TestMemoryMappedIntArray {
	static int ARRAY_ROWS = 10 * 1000 * 1000;
	static int ARRAY_COLUMNS = 8;
	static int MAX_PRINT_LINES = 0;
	MemoryMappedIntArray memoryMappedIntArray = new MemoryMappedIntArray(
			ARRAY_ROWS);
	static Logger logger = Logger.getLogger(TestMemoryMappedIntArray.class
			.getCanonicalName());

	public void testAppend() {
		long startTime = System.nanoTime();
		for (int i = 0; i < ARRAY_ROWS; i++) {
			// Random array sizes 0 to 999
			int arraySize = (int) Math.round(Math.random() * 100);
			int[] array = new int[arraySize];
			for (int j = 0; j < array.length; j++) {
				array[j] = (int) Math.round(Math.random() * 100);
			}
			if (i < MAX_PRINT_LINES) {
				logger.info(arraySize + "==>" + Arrays.toString(array));
			}
			try {
				memoryMappedIntArray.append(array);
			} catch (MMapIntArrayException e) {
				e.printStackTrace();
			}
		}
		logger.info("Append in " + (System.nanoTime() - startTime) / 1E9
				+ " seconds");
	}

	public void testGet() {
		long startTime = System.nanoTime();
		for (int i = 0; i < ARRAY_ROWS; i++) {
			try {
				int[] array = memoryMappedIntArray.get(i);
				if (i < MAX_PRINT_LINES) {
					logger.info(array.length + "==>" + Arrays.toString(array));
				}
			} catch (MMapIntArrayException e) {
				e.printStackTrace();
			}
		}
		logger.info("Retrieve in " + (System.nanoTime() - startTime) / 1E9
				+ " seconds");
	}

	public void testRandomGet() {
		long startTime = System.nanoTime();
		int edgeValue = 0;
		Random random = new Random();
		for (int i = 0; i < ARRAY_ROWS; i++) {
			int index = (int) Math.round(random.nextDouble() * ARRAY_ROWS);
			try {
				if (index < ARRAY_ROWS) {
					int[] array = memoryMappedIntArray.get(index);
					if (i < MAX_PRINT_LINES) {
						System.out
								.println("At index" + index + " ::: "
										+ array.length + "==>"
										+ Arrays.toString(array));
					}
				} else {
					edgeValue++;
				}
			} catch (MMapIntArrayException e) {
				e.printStackTrace();
			}

		}
		logger.info("Random Retrieve in " + (System.nanoTime() - startTime)
				/ 1E9 + " seconds");
		logger.warning("Randomizer generated the edgevalue this many times: "
				+ edgeValue);
	}

	public static void main(String[] args) {

		DecimalFormat formatter = new DecimalFormat("#,###");

		logger.info("2D array: " + formatter.format(ARRAY_ROWS) + " * "
				+ formatter.format(ARRAY_COLUMNS));
		long startTime = System.nanoTime();
		TestMemoryMappedIntArray testMemoryMappedIntArray = new TestMemoryMappedIntArray();
		logger.info("Initialization in " + (System.nanoTime() - startTime)
				/ 1E9 + " seconds");
		testMemoryMappedIntArray.testAppend();
		testMemoryMappedIntArray.testGet();
		testMemoryMappedIntArray.testRandomGet();

	}
}
