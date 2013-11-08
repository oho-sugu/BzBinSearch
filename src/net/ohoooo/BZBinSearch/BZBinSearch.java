/*
 * Created on 2011/09/19
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package net.ohoooo.BZBinSearch;

import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.compress.compressors.bzip2.BZip2PartedCompressorInputStream;

/**
 * @author Suguru Oho
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class BZBinSearch{
	private static int[][] search = {
		{0x31, 0x41, 0x59, 0x26, 0x53},
	  {0x62, 0x82, 0xB2, 0x4C, 0xA6},
	  {0xC5, 0x05, 0x64, 0x99, 0x4D},
	  {0x8A, 0x0A, 0xC9, 0x32, 0x9A},
	  {0x14, 0x15, 0x92, 0x65, 0x35},
	  {0x28, 0x2B, 0x24, 0xCA, 0x6B},
	  {0x50, 0x56, 0x49, 0x94, 0xD6},
	  {0xA0, 0xAC, 0x93, 0x29, 0xAC}};
	private static int[] preChar = 
		{0x00,0x00,0x00,0x01,0x03,0x06,0x0C,0x18};

	public static final long MARGINREADTIME = 5 * 1000; // Set Up mill second
	public static final int MINIMUMSIZE = 2 * 1000 * 1000;
	private static String paramFormatString = "yyyy-MM-dd_HH:mm:ss";
	private static String fileFormatString = "yyyy-MM-dd HH:mm:ss";
	private static DateFormat df1;
	private static DateFormat df2;
	
	private static int strStart,strEnd;

	private static final int TIMEERRORLIMIT = 40;
	/**
	 * @param args
	 * @return 
	 */
	public static int main(String[] args) {
		
		// Option parser
		Options options = new Options();
		
		options.addOption("h", "help", false, "Help and Usage");
		options.addOption("s", "start", true, "Start Position to cut time string");
		options.addOption("e", "end", true, "End Position to cut time string");
		options.addOption("f", "from", true, "From time for log reading");
		options.addOption("t", "to", true, "To time for log reading");
		options.addOption("i", "input", true, "Input File Name");
		options.addOption("p", "paramformat", true, "Parameter date format");
		options.addOption("F", "timeformat", true, "Date format in log file");

		CommandLineParser parser = new BasicParser();
		CommandLine commandLine;
		try{
			commandLine = parser.parse(options, args);
		} catch (org.apache.commons.cli.ParseException e) {
			System.err.println("Parameter Parse Error");
			return 1;
		}

		if(commandLine.hasOption("h")){
			usage();
		}
		
		if(commandLine.hasOption("s")){
			strStart = Integer.parseInt(commandLine.getOptionValue("s"));
		} else {
			System.err.println("Error:Need start position to cut time string in row");
			return 1;
		}
		if(commandLine.hasOption("e")){
			strEnd = Integer.parseInt(commandLine.getOptionValue("e"));
		} else {
			System.err.println("Error:Need end position to cut time string in row");
			return 1;
		}
		
		df1 = new SimpleDateFormat(paramFormatString);
		df2 = new SimpleDateFormat(fileFormatString);
		
		
		if(args.length < 1){
			usage();
		}
		
		if(args[0].equals("help") || args[0].equals("-h") || args[0].equals("--help")){
			usage();
		}
		
		try {
			RandomAccessFile inputFile = null;
			try {
				inputFile = new RandomAccessFile(new File(args[2]),"r");
			} catch (FileNotFoundException e2) {
				System.out.println("ERROR File Not Found.");
				System.exit(1);
			}
			long inputFileSize = inputFile.length();
			
			if(inputFileSize < MINIMUMSIZE){
				System.out.println("ERROR File is too small. Use bunzip and grep.");
				System.exit(1);
			}
			
			long startTime = 0;
			long endTime = Long.MAX_VALUE;
			try {
				startTime = df1.parse(args[0]).getTime() - MARGINREADTIME;
				endTime = df1.parse(args[1]).getTime() + MARGINREADTIME;
			} catch (ParseException e1) {
				System.out.println("ERROR Irreguler Timestamp Format. Timestamp Format must be 'yyyy-MM-dd_HH:mm:ss'.");
				System.exit(1);
			}
			
			if( startTime > endTime) {
				System.out.println("ERROR start time > end time");
				System.exit(1);
			}
			
			// BinSearch Variable Initialize
			long startPosition = 0;
			long partSize = inputFileSize / 2;
			long halfPosition = partSize;
			
			// Pattern Search Variable
			int[] buffer = new int[5];
			int pointer = -1;
			int series = -1;

			// Variables needed by BZip2PartedCompressInputStream
			long readStartIndex = 0;
			int offset = 0;
			int prebuffer = preChar[0];
			
			do{
				inputFile.seek(halfPosition);
				int data;
				
				// First Byte Searching
				while((data = inputFile.read()) != -1){
					if(series == -1){
						// Pattern Matching
						for(int i=0;i < 8;i++){
							if(data == search[i][0]){
								series = i;
								pointer = 0;
								buffer[0] = data;
								break;
							}
						}
					} else {
						if(data != search[series][++pointer]){
							series = -1;
							pointer = -1;
						} else{
							buffer[pointer] = data;
							if(pointer >= 4){
								readStartIndex = inputFile.getFilePointer() - 5;
								offset = series;
								prebuffer = preChar[series];
								break;
							}
						}
					}
				}
				
				// Next Iteration
				inputFile.seek(readStartIndex);
				BufferedReader br = new BufferedReader(new InputStreamReader(new BZip2PartedCompressorInputStream(new FileInputStream(inputFile.getFD()), prebuffer, offset, 9, false)));
				
				// assumed this log format
				// w0815 2011-12-01 23:00:00 V=3 127.0.0.1 - - - - - ...

				String line = br.readLine();
				long time1 = -1;
				int timeErrorCount = 0;
				while(time1 == -1){
					line = br.readLine();
					try {
						time1 = df2.parse((line.substring(6, 25))).getTime();
					} catch (ParseException e) {
						// Can't Convert String to Time. Maybe irregular format Log.
						timeErrorCount++;
						if(timeErrorCount > TIMEERRORLIMIT){
							System.out.println("ERROR Time Format Error. Error Count is "+timeErrorCount+".");
							System.exit(1);
						}
						time1 = -1;
					}
				}
				
				// Binary Search
				if(startTime < time1){
					//startPosition = startPosition;
				} else {
					startPosition = halfPosition;
				}

				// Initialize for Next Iteration.
				partSize = partSize/2;
				halfPosition = startPosition + partSize;
				inputFile.seek(halfPosition);

				// Reinitialize Pattern Matching Variable
				buffer = new int[5];
				pointer = -1;
				series = -1;

			} while(partSize > MINIMUMSIZE);
			
			if(halfPosition == partSize){
				// All Backwarded Searched. StartTime < File Start or StartTime in First PartSize
				System.out.println("Branch!");
				readStartIndex = 4;
				prebuffer = 0;
				offset = 0;
			}
			
			// Begin Output Logs
			
			inputFile.seek(readStartIndex);
			
			BufferedReader br = new BufferedReader(
					new InputStreamReader(
							new BZip2PartedCompressorInputStream(
									new FileInputStream(inputFile.getFD())
									, prebuffer, offset, 9, true)
							));
			
			String line2;
			long time;
			while((line2 = br.readLine())!=null){
				try {
					time = df2.parse((line2.substring(6, 25))).getTime();
					if(endTime < time){
						break;
					} else if(time < startTime){
						continue;
					}
					System.out.println(line2);
				} catch (ParseException e) {
					// Can't Convert String to Time. Maybe irregular format Log.
					System.out.println(line2);
				}
			}
			
			br.close();
			inputFile.close();
		} catch (IOException e) {
			System.out.println("ERROR IOException.");
			e.printStackTrace();
			System.exit(1);
		}
		return 0;
	}

	private static void usage(){
		System.out.println("Usage : BZBinSearch STARTTIME ENDTIME FILE");
		System.out.println("        Timestamp format must be 'yyyy-MM-dd_HH:mm:ss'");
		System.exit(0);
	}
}
