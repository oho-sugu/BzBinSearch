/*
 * Created on 2011/09/19
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package net.ohoooo.BZBinSearch;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import org.apache.commons.cli.*;

/**
 * @author Suguru Oho
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class BZBinSearch{
	public static final long MARGINREADTIME = 5 * 1000; // Set Up mill second
	private static boolean debug = false;

	/**
	 * @param args
	 * @return 
	 */
	public static void main(String[] args) {
		// needed parameters
		int strStart = 6,strEnd = 25;
		final Locale locale = Locale.ENGLISH;
		DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", locale),df2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", locale);
		long fromTime = 0,toTime = Long.MAX_VALUE;
		String inputFileName = "";
		
		// Option parser  =====================================================================================
		// parsing option value and validate and set parameters
		Options options = new Options();
		
		options.addOption("h", "help", false, "Help and Usage");
		options.addOption("d", "debug", false, "Enable Debug print");
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

			if(commandLine.hasOption("d")){
				debug = true;
				debugLog("Debug ON");
			}

			if(commandLine.hasOption("s")){
				strStart = Integer.parseInt(commandLine.getOptionValue("s"));
				debugLog("strStart:"+strStart);
			} else {
				errorLog("Need start position to cut time string in row");
				usage(options);
				System.exit(1);
			}
			if(commandLine.hasOption("e")){
				strEnd = Integer.parseInt(commandLine.getOptionValue("e"));
				debugLog("strEnd:"+strEnd);
			} else {
				errorLog("Need end position to cut time string in row");
				usage(options);
				System.exit(1);
			}
			
			if(commandLine.hasOption("i")){
				inputFileName = commandLine.getOptionValue("i");
				debugLog("inputFileName:"+inputFileName);
			} else {
				errorLog("Need input file name");
				usage(options);
				System.exit(1);
			}

			if(commandLine.hasOption("p")){
				df1 = new SimpleDateFormat(commandLine.getOptionValue("p"), locale);
				debugLog("df1:"+commandLine.getOptionValue("p"));
			} else {
				errorLog("Need Parameter Date Format");
				usage(options);
				System.exit(1);
			}
			if(commandLine.hasOption("F")){
				df2 = new SimpleDateFormat(commandLine.getOptionValue("F"), locale);
				debugLog("df2:"+commandLine.getOptionValue("F"));
			} else {
				errorLog("Need File Date Format");
				usage(options);
				System.exit(1);
			}

			if(commandLine.hasOption("f")){
				try {
					fromTime = df1.parse(commandLine.getOptionValue("f")).getTime();
					debugLog("fromTime:"+commandLine.getOptionValue("f")+" : "+fromTime);
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
					toTime = df1.parse(commandLine.getOptionValue("t")).getTime();
					debugLog("toTime:"+commandLine.getOptionValue("t")+" : "+toTime);
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
		final DateFormat _df2 = df2;
		final long _fromTime = fromTime, _toTime = toTime;
		final boolean _debug = debug;
		
		IndexSearcher searcher = new IndexSearcher() {
			@Override
			public long search(String str) throws ParseException {
				String substr = str.substring(_strStart, _strEnd);
				if(_debug) debugLog("substr:"+substr); // not eval when not debug for performance reason
				long ret = _df2.parse(substr).getTime();
				if(_debug) debugLog("timestamp:"+ret); // not eval when not debug for performance reason
				return ret;
			}
		};
		
		BinComparator comparator = new BinComparator() {
			@Override
			public int compare(long time, boolean margin) {
				long _margin = 0;
				if(margin){
					_margin = MARGINREADTIME;
				}
				if(time < _fromTime - _margin){
					return BinComparator.BEFORE;
				} else if(_toTime + _margin < time){
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
		
		RandomAccessFile inputFile = null;
		try {
			long startTimestamp = System.currentTimeMillis();
			inputFile = new RandomAccessFile(new File(inputFileName),"r");
			BinSearch.binSearch(inputFile, searcher, comparator, outputter);
			debugLog("Elapsed Time:"+((System.currentTimeMillis()-startTimestamp)/1000));

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
		} finally {
			if(inputFile != null){
				try {
					inputFile.close();
				} catch (IOException e) {
					errorLog("File close error.",e);
				}
			}
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
	private static void debugLog(String log){
		if(debug){
			System.out.println("Debug:"+log);
		}
	}
}
