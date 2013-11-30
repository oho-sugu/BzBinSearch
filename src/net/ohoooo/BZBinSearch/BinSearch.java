/*
 * Created on 2013/11/28
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package net.ohoooo.BZBinSearch;

import java.io.*;
import java.text.ParseException;

import org.apache.commons.compress.compressors.bzip2.BZip2PartedCompressorInputStream;

/**
 * @author Suguru Oho
 * 
 *         To change the template for this generated type comment go to
 *         Window>Preferences>Java>Code Generation>Code and Comments
 */
public class BinSearch {

	private static final int[][]		search					= {
			{ 0x31, 0x41, 0x59, 0x26, 0x53 }, { 0x62, 0x82, 0xB2, 0x4C, 0xA6 },
			{ 0xC5, 0x05, 0x64, 0x99, 0x4D }, { 0x8A, 0x0A, 0xC9, 0x32, 0x9A },
			{ 0x14, 0x15, 0x92, 0x65, 0x35 }, { 0x28, 0x2B, 0x24, 0xCA, 0x6B },
			{ 0x50, 0x56, 0x49, 0x94, 0xD6 }, { 0xA0, 0xAC, 0x93, 0x29, 0xAC } };

	private static final int[]			preChar					= { 0x00, 0x00, 0x00, 0x01, 0x03,
			0x06, 0x0C, 0x18											};

	private static final int	TIMEERRORLIMIT	= 40;

	public static final int		MINIMUMSIZE			= 2 * 1000 * 1000;

	public static void binSearch(RandomAccessFile inputFile,
			IndexSearcher searcher, BinComparator comparator, Outputter outputter)
			throws FileTooSmallException, CantSearchIndexException, IOException, PatternNotExistException {

		// File is too small to use this program.
		// n : log n
		if (inputFile.length() < MINIMUMSIZE * 3) {
			throw new FileTooSmallException();
		}

		// BinSearch Variable Initialize
		long startPosition = 0;
		long partSize = inputFile.length() / 2;
		long halfPosition = partSize;


		// Variables needed by BZip2PartedCompressInputStream
		BzHeaderParam bzHeaderParam = null,oldHeaderParam,resultHeaderParam;

		do {
			oldHeaderParam = bzHeaderParam;
			bzHeaderParam = patternSearch(inputFile, halfPosition);

			// Search index value such as timestamp
			inputFile.seek(bzHeaderParam.readStartIndex);
			BufferedReader br = new BufferedReader(new InputStreamReader(
					new BZip2PartedCompressorInputStream(new FileInputStream(
							inputFile.getFD()), bzHeaderParam.prebuffer, bzHeaderParam.offset, 9, false)));

			String line = br.readLine();
			long time1 = -1;
			int timeErrorCount = 0;
			while (time1 == -1) {
				line = br.readLine();
				try {
					time1 = searcher.search(line);
				} catch (ParseException e) {
					// Can't Convert String to Time. Maybe irregular format Log.
					timeErrorCount++;
					if (timeErrorCount > TIMEERRORLIMIT) {
						br.close();
						throw new CantSearchIndexException();
					}
					time1 = -1;
				}
			}

			// Binary Search
			if (comparator.compare(time1) >= BinComparator.IN) {
				// startPosition = startPosition;
				resultHeaderParam = oldHeaderParam;
			} else {
				startPosition = halfPosition;
				resultHeaderParam = bzHeaderParam;
			}

			// Initialize for Next Iteration.
			partSize = partSize / 2;
			halfPosition = startPosition + partSize;
			inputFile.seek(halfPosition);

		} while (partSize > MINIMUMSIZE);

		if (halfPosition == partSize) {
			// All Backwarded Searched. StartTime < File Start or StartTime in First
			// PartSize
			resultHeaderParam.readStartIndex = 4;
			resultHeaderParam.prebuffer = 0;
			resultHeaderParam.offset = 0;
		}

		// Begin Output Logs

		inputFile.seek(resultHeaderParam.readStartIndex);

		BufferedReader br = new BufferedReader(new InputStreamReader(
				new BZip2PartedCompressorInputStream(new FileInputStream(
						inputFile.getFD()), resultHeaderParam.prebuffer, resultHeaderParam.offset, 9, true)));

		String line;
		long time;
		while ((line = br.readLine()) != null) {
			try {
				time = searcher.search(line);
				if (comparator.compare(time) == BinComparator.AFTER) {
					break;
				} else if (comparator.compare(time) == BinComparator.BEFORE) {
					continue;
				} else {
					outputter.output(line);
				}
			} catch (ParseException e) {
				// Can't Convert String to Time. Maybe irregular format Log.
				// System.out.println(line);
			}
		}

		br.close();
	}
	
	private static BzHeaderParam patternSearch(RandomAccessFile inputFile,long halfPosition) throws IOException,PatternNotExistException{
		// Pattern Search Variable
		int[] buffer = new int[5];
		int series = -1;
		int pointer = -1;
		
		inputFile.seek(halfPosition);
		int data;

		// First Byte Searching
		while ((data = inputFile.read()) != -1) {
			if (series == -1) {
				// Pattern Matching
				for (int i = 0; i < 8; i++) {
					if (data == search[i][0]) {
						series = i;
						pointer = 0;
						buffer[0] = data;
						break;
					}
				}
			} else {
				if (data != search[series][++pointer]) {
					series = -1;
					pointer = -1;
				} else {
					buffer[pointer] = data;
					if (pointer >= 4) {
						BzHeaderParam headerParam = new BzHeaderParam();
						headerParam.readStartIndex = inputFile.getFilePointer() - 5;
						headerParam.offset = series;
						headerParam.prebuffer = preChar[series];
						return headerParam;
					}
				}
			}
		}
		throw new PatternNotExistException();
	}
}

class BzHeaderParam{
	long readStartIndex;
	int offset;
	int prebuffer;
}

class PatternNotExistException extends Exception{
}
