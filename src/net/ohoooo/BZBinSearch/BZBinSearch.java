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

import org.apache.commons.cli.*;
import org.apache.commons.compress.compressors.bzip2.BZip2PartedCompressorInputStream;

/**
 * @author Suguru Oho
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class BZBinSearch{
	public static final long MARGINREADTIME = 5 * 1000; // Set Up mill second

	/**
	 * @param args
	 * @return 
	 */
	public static void main(String[] args) {
		
		// needed parameters
		int strStart = 6,strEnd = 25;
		DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss"),df2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		long fromTime = 0,toTime = Long.MAX_VALUE;
		String inputFileName = "";
		
		// Option parser  =====================================================================================
		// parsing option value and validate and set parameters
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

			if(commandLine.hasOption("h")){
				usage(options);
				System.exit(1);
			}
			
			if(commandLine.hasOption("s")){
				strStart = Integer.parseInt(commandLine.getOptionValue("s"));
			} else {
				errorLog("Need start position to cut time string in row");
				usage(options);
				System.exit(1);
			}
			if(commandLine.hasOption("e")){
				strEnd = Integer.parseInt(commandLine.getOptionValue("e"));
			} else {
				errorLog("Need end position to cut time string in row");
				usage(options);
				System.exit(1);
			}
			
			if(commandLine.hasOption("i")){
				inputFileName = commandLine.getOptionValue("i");
			} else {
				errorLog("Need input file name");
				usage(options);
				System.exit(1);
			}

			if(commandLine.hasOption("p")){
				df1 = new SimpleDateFormat(commandLine.getOptionValue("p"));
			} else {
				errorLog("Need Parameter Date Format");
				usage(options);
				System.exit(1);
			}
			if(commandLine.hasOption("F")){
				df2 = new SimpleDateFormat(commandLine.getOptionValue("F"));
			} else {
				errorLog("Need File Date Format");
				usage(options);
				System.exit(1);
			}

			if(commandLine.hasOption("f")){
				try {
					fromTime = df1.parse(commandLine.getOptionValue("f")).getTime() - MARGINREADTIME;
				} catch (ParseException e) {
					errorLog("Need 'from' time",e);
					usage(options);
					System.exit(1);
				}
			} else {
				errorLog("Need 'from' time");
				usage(options);
				System.exit(1);
			}
			if(commandLine.hasOption("t")){
				try {
					toTime = df1.parse(commandLine.getOptionValue("t")).getTime() + MARGINREADTIME;
				} catch (ParseException e) {
					errorLog("Need 'to' time",e);
					usage(options);
					System.exit(1);
				}
			} else {
				errorLog("Need 'to' time");
				usage(options);
				System.exit(1);
			}

			if( fromTime > toTime) {
				errorLog("start time > end time");
				System.exit(1);
			}

		} catch (org.apache.commons.cli.ParseException e) {
			errorLog("Parameter Parse Error",e);
			usage(options);
			System.exit(1);
		}

		// Prepare Parameter included classes ===================================================================
		final int _strStart = strStart, _strEnd = strEnd;
		final DateFormat _df1 = df1, _df2 = df2;
		final long _fromTime = fromTime, _toTime = toTime;
		
		IndexSearcher searcher = new IndexSearcher() {
			@Override
			public long search(String str) throws ParseException {
				return _df2.parse((str.substring(_strStart, _strEnd))).getTime();
			}
		};
		
		BinComparator comparator = new BinComparator() {
			@Override
			public int compare(long time) {
				if(time < _fromTime){
					return BinComparator.BEFORE;
				} else if(_toTime < time){
					return BinComparator.AFTER;
				}
				return BinComparator.IN;
			}
		};
		
		Outputter outputter = new Outputter() {
			@Override
			public void output(String line) {
				System.out.println(line);
			}
		};
		
		// End Preparation =======================================================================================
		
		try (RandomAccessFile inputFile = new RandomAccessFile(new File(inputFileName),"r")) {
			
			BinSearch.binSearch(inputFile, searcher, comparator, outputter);

			inputFile.close();
		} catch (FileNotFoundException e) {
			errorLog("File Not Found.",e);
			System.exit(1);
		} catch (IOException e) {
			errorLog("File IO Error.",e);
			System.exit(1);
		} catch (CantSearchIndexException e){
			errorLog("Can't search bin-search index (such as Timestamp). Check file contents/log format.",e);
			System.exit(1);
		} catch (FileTooSmallException e) {
			errorLog("File is too small. Use bunzip and grep.");
			System.exit(1);
		} catch (PatternNotExistException e) {
			errorLog("BZip2 block header pattern not exist. Is it really bzip2 type file?",e);
			System.exit(1);
		}
		
		System.exit(0);
	}

	private static void usage(Options options){
		HelpFormatter help = new HelpFormatter();
		help.printHelp("BzBinSearch", options,true);
	}
	
	private static void errorLog(String err){
		System.err.println("BzBinSearch Error: "+err);
	}
	private static void errorLog(String err,Exception e){
		e.printStackTrace();
		errorLog(err);
	}
}
