package org.disk.collection;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.BufferUnderflowException;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.Vector;

/**
 * 2D array stored as IntBuffers. int[a][b] is a 2D array. The int[b] is
 * retrieved based on the index "a".
 * 
 * The 2D array is append only. Cannot modify values once set.
 * 
 * This is primarily for read-only access, after the 2D array initialization
 * process.
 * 
 * The data that this array holds is probably in some other data store, and it
 * is expensive to access this data store each time int[b] is required, and also
 * it is expensive to hold this 2D array in memory (requires contiguous
 * allocation of space)
 */

public class MemoryMappedIntArray {

	// ------------------ PRIVATE FIELDS ---------------

	private int capacity = 0;
	private String bufferFilePath;
	private IntBuffer currentBlockBuffer;

	/* Each block buffer holds 128MB of data */
	private static int MAXIMUM_BYTES_PER_BLOCK = 128 * 1024 * 1024;

	private static final int MAPPED_BYTES_PER_INT = 4;

	private static final int INDEX_LENGTH = 4;

	private static final int MAX_ELEMENTS_PER_BLOCK = MAXIMUM_BYTES_PER_BLOCK
			/ MAPPED_BYTES_PER_INT;

	private static final String MAPPEDBUFFER_DIRECTORY = "mappedIntBuffer";
	private static final String BUFFER_FILENAME_PREFIX = "MAPPED_INTBUFFER";

	/*
	 * original index, block index, start position, length
	 * 
	 * [int,int, int, int]
	 */
	private IntBuffer mainIndex;
	private Vector<MemoryMap> memoryMaps;
	private int blockIndex = 0;
	private int currentIndex;

	// ------------------ CONSTRUCTORS ---------------
	public MemoryMappedIntArray(int i) {
		this(null, i);
	}

	public MemoryMappedIntArray(String bufferFilePath, int capacity) {
		this.bufferFilePath = bufferFilePath;
		this.capacity = capacity;
		initialize();

	}

	// ------------------ PUBLIC METHODS ---------------

	public void append(int[] array) throws MMapIntArrayException {

		if (array.length > MAX_ELEMENTS_PER_BLOCK) {
			String message = "Array cannot be fit into the memory map block because the array length "
					+ array.length
					+ " exceeds the capacity of the block: "
					+ MAX_ELEMENTS_PER_BLOCK;
			throw new MMapIntArrayException(message);
		}

		if (false == bufferHasSpaceFor(array)) {
			/* Create a new buffer */
			renewCurrentBuffer();
		}
		int insertPosition = currentBlockBuffer.position();
		currentBlockBuffer.put(array);

		int[] idx = { currentIndex, blockIndex, insertPosition, array.length };
		mainIndex.put(idx);
		/* Increment total elements count */
		currentIndex++;
	}

	public int[] get(int index) throws MMapIntArrayException {
		int[] array = null;

		/* Move to the index position */
		mainIndex.position(index * INDEX_LENGTH);

		// original index, block index, start position, length
		int[] idxDetails = new int[INDEX_LENGTH];
		try {
			mainIndex.get(idxDetails);
		} catch (BufferUnderflowException bux) {
			if (index * INDEX_LENGTH >= mainIndex.capacity()) {
				String message = "Index: " + index
						+ " exceeds the capacity of the index buffer: "
						+ mainIndex.capacity() / INDEX_LENGTH;
				throw new MMapIntArrayException(message, bux);
			} else {
				bux.printStackTrace();
			}
		}
		int mainIdx = idxDetails[0];
		int blockIdx = idxDetails[1];
		int startPos = idxDetails[2];
		int length = idxDetails[3];

		if (mainIdx == index) {
			IntBuffer buffer = memoryMaps.get(blockIdx).getIntBuffer();
			buffer.position(startPos);
			array = new int[length];
			buffer.get(array);
		}
		return array;
	}

	// ------------------ PRIVATE METHODS ---------------

	private void initialize() {
		currentIndex = 0;
		blockIndex = 0;
		memoryMaps = new Vector<MemoryMap>();
		long mainIndexSize = capacity * INDEX_LENGTH * MAPPED_BYTES_PER_INT
				* 1L;
		mainIndex = new MemoryMap(-1).getIntBuffer(mainIndexSize);
		initCurrentBuffer();
	}

	private void initCurrentBuffer() {
		memoryMaps.add(blockIndex, new MemoryMap(blockIndex));
		currentBlockBuffer = memoryMaps.get(blockIndex).getIntBuffer();
		currentBlockBuffer.rewind();
	}

	private void renewCurrentBuffer() {
		memoryMaps.get(blockIndex).close();
		blockIndex++;
		initCurrentBuffer();
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		for (MemoryMap memMap : memoryMaps) {
			memMap.close();
		}
	}

	private class MemoryMap {
		private int serialNumber;
		private IntBuffer intBuffer = null;
		private RandomAccessFile randomAccessFile = null;
		private FileChannel outChan = null;
		private String readWriteMode = "rw";

		private MemoryMap(int number) {
			this.serialNumber = number;
		}

		private IntBuffer getIntBuffer() {
			return getIntBuffer(-1);
		}

		private IntBuffer getIntBuffer(long sizeOfBuffer) {
			if (intBuffer == null) {
				createIntBuffer(sizeOfBuffer);
			}
			return intBuffer;
		}

		private boolean close() {

			boolean status = true;
			try {
				if (randomAccessFile != null) {
					randomAccessFile.close();
				}
				if (outChan != null) {
					outChan.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
				status = false;
			}
			return status;
		}

		private void createIntBuffer(long sizeOfBuffer) {
			/* Create a mappedByteBuffer */
			File bufferFile = createMappedBufferFile();
			try {
				randomAccessFile = new RandomAccessFile(bufferFile,
						readWriteMode);
				outChan = randomAccessFile.getChannel();
				long size = (sizeOfBuffer > 0) ? sizeOfBuffer
						: MAXIMUM_BYTES_PER_BLOCK;
				intBuffer = outChan.map(MapMode.READ_WRITE, 0, size)
						.asIntBuffer();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private File createMappedBufferFile() {
			File arrayDirectory = null;
			if (bufferFilePath == null) {
				arrayDirectory = new File(MAPPEDBUFFER_DIRECTORY);
			} else {
				arrayDirectory = new File(bufferFilePath,
						MAPPEDBUFFER_DIRECTORY);
			}

			if (!arrayDirectory.exists()) {
				arrayDirectory.mkdirs();
			}

			/* Create a mappedByteBuffer */
			File bufferFile = new File(arrayDirectory, BUFFER_FILENAME_PREFIX
					+ serialNumber);
			return bufferFile;
		}
	}

	private boolean bufferHasSpaceFor(int[] array) {
		return ((currentBlockBuffer.position() + array.length) <= currentBlockBuffer
				.capacity());
	}
}
