/*
 * CS61C Spring 2014 Project2
 * Reminders:
 *
 * DO NOT SHARE CODE IN ANY WAY SHAPE OR FORM, NEITHER IN PUBLIC REPOS OR FOR DEBUGGING.
 *
 * This is one of the two files that you should be modifying and submitting for this project.
 */
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.lang.Math;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

public class SolveMoves {
    public static class Map extends Mapper<IntWritable, MovesWritable, IntWritable, ByteWritable> {
        /**
         * Configuration and setup that occurs before map gets called for the first time.
         *
         **/
        @Override
        public void setup(Context context) {
        }

        /**
         * The map function for the second mapreduce that you should be filling out.
         */
        @Override
        public void map(IntWritable key, MovesWritable val, Context context) throws IOException, InterruptedException {
            /* YOUR CODE HERE */
        	for (int value : val.getMoves()) {
        		context.write(new IntWritable(value), new ByteWritable(val.getValue()));
        	}
        }
    }

    public static class Reduce extends Reducer<IntWritable, ByteWritable, IntWritable, MovesWritable> {
        int boardWidth;
        int boardHeight;
        int connectWin;
        boolean OTurn;
        /**
         * Configuration and setup that occurs before map gets called for the first time.
         *
         **/
        @Override
        public void setup(Context context) {
            // load up the config vars specified in Proj2.java#main()
            boardWidth = context.getConfiguration().getInt("boardWidth", 0);
            boardHeight = context.getConfiguration().getInt("boardHeight", 0);
            connectWin = context.getConfiguration().getInt("connectWin", 0);
            OTurn = context.getConfiguration().getBoolean("OTurn", true);
        }

        /**
         * The reduce function for the first mapreduce that you should be filling out.
         */
        @Override
        public void reduce(IntWritable key, Iterable<ByteWritable> values, Context context) throws IOException, InterruptedException {
            /* YOUR CODE HERE */
        	MovesWritable moves = new MovesWritable((byte)0, null);
        	byte statusMask = 3;
        	char currentPlayer;
        	char otherPlayer;
        	byte currentWin;
        	if (OTurn) {
        		currentPlayer = 'O';
        		otherPlayer = 'X';
        		currentWin = 1;
        	} else {
        		currentPlayer = 'X';
        		otherPlayer = 'O';
        		currentWin = 2;
        	}
        	boolean hasWin = false;
        	byte leastMoves = Byte.MAX_VALUE;
        	ArrayList<Byte> vals = new ArrayList<Byte>();
        	boolean validParent = false;
        	for (ByteWritable val : values) {
        		if ((val.get() >>> 2) == 0) {
        			validParent = true;
        		}
        		vals.add(val.get());
        	}
        	if (!validParent) {
        		return;
        	}
        	for (byte val : vals) {
        		if ((val & statusMask) == currentWin) {
        			hasWin = true;
        			if ((val >>> 2) < leastMoves) {
        				leastMoves = (byte) (val >>> 2);
        			}
        		}
        	}
        	if (hasWin) {
        		moves.setMovesToEnd(leastMoves + 1);
        		moves.setStatus(currentWin);
        	} else {
        		byte mostMoves = 0;
            	boolean hasDraw = false;
            	for (byte val : vals) {
            		if ((val & statusMask) == 3) {
            			hasDraw = true;
            			if ((val >>> 2) > mostMoves) {
            				mostMoves = (byte) (val >>> 2);
            			}
            		}
            	}
            	if (hasDraw) {
            		moves.setMovesToEnd(mostMoves + 1);
            		moves.setStatus(3);
            	} else {
                	for (byte val : vals) {
                		if ((val >>> 2) > mostMoves) {
                			mostMoves = (byte) (val >>> 2);
                		}
                	}
            		moves.setMovesToEnd(mostMoves + 1);
            		moves.setStatus(currentWin ^ statusMask);
            	}
        	}
        	String board = Proj2Util.gameUnhasher(key.get(), boardWidth, boardHeight);
        	ArrayList<Integer> tempParents = new ArrayList<Integer>();
        	for (int col = 0; col < boardWidth; col++) {
        		for (int i = col * boardHeight + boardHeight - 1; i > col * boardHeight - 1; i--) {
        			if (board.charAt(i) == currentPlayer) {
        				break;
        			}
        			if (board.charAt(i) == otherPlayer) {
        				String parentBoard = board.substring(0, i) + ' ' + board.substring(i + 1);
        				tempParents.add(Proj2Util.gameHasher(parentBoard, boardWidth, boardHeight));
        				break;
        			}
        		}
        	}
        	int[] parents = new int[tempParents.size()];
        	for (int i = 0; i < tempParents.size(); i++) {
        		parents[i] = tempParents.get(i);
        	}
        	moves.setMoves(parents);
        	context.write(key, moves);
        }
    }
}
