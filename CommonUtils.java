import java.io.*;
import java.util.*;
import java.lang.Math;

/**
 * CommonUtils.java
 *
 * Abstract Java class that contains common utility functions and global
 * constants used in our programs
 *
 * Contributors:
 * 
 * @author Paula 
 * @author Schuyler 
 *
 */
public abstract class CommonUtils {

    // ########## Constants ##########
    // CSV file pathings
    public static final String PATH_IN_CSV = "csv_in_nike/Nike_";
    public static final String PATH_OUT_CSV = "csv_out_nike/Nike_";
    // shorts/ directory is used for testing
    public static final String PATH_IN_TEST_CSV = "csv_in/shorts/";
    public static final String PATH_OUT_TEST_CSV = "csv_out/testing/";

    // File that links UIDs to user names
    public static final String UID_LOOKUP_FILE = "UID_Name.csv";

    // ########## Variables ##########

    // ########## Functions ##########

    /**
     * Calculate the time between two timestamps
     *
     * @param start
     *            Start time of the operation in milliseconds
     * @param end
     *            End time of the operation in milliseconds
     * @return The difference between time operations, in milliseconds
     */
    public static long calcTimestamp(long start, long end) {
        return end - start;
    }

    /**
     * Calculate the time between two timestamps given an array of timestamps
     * and an index of the ending time
     *
     * @param timestamps
     *            Array of sequential timestamps in milliseconds
     * @param end_index
     *            Index of the ending timestamp. Must be 0 < n <= len - 1
     * @return The difference between time operations, in milliseconds
     */
    public static long calcTimestamp(long[] timestamps, int end_index) {
        return calcTimestamp(timestamps[end_index - 1], timestamps[end_index]);
    }

    /**
     * Print the formatted time between two timestamps
     *
     * @param msg
     *            String message to report with timestamp "msg: time_stamp"
     * @param start
     *            Start time of the operation in milliseconds
     * @param end
     *            End time of the operation in milliseconds
     */
    public static void printTimestamp(String msg, long start, long end) {
        long diff = calcTimestamp(start, end);
        // format the time (hour:minute:sec:millisec)
        long tm = diff % 1000;
        long tS = (diff / 1000) % 60;
        long tM = (diff / (60 * 1000) % 60);
        long tH = diff / (3600 * 1000);
        // print the whole message
        System.out.printf("%-32s %02d:%02d:%02d:%03d\n", msg + ":", tH, tM, tS,
                tm);
    }

    /**
     * Print the formatted time between two timestamps given an array of
     * timestamps and an index of the ending time
     *
     * @param msg
     *            String message to report with timestamp "msg: time_stamp"
     * @param timestamps
     *            Array of sequential timestamps in milliseconds
     * @param end_index
     *            Index of the ending timestamp. Must be 0 < n <= len - 1
     */
    public static void printTimestamp(String msg, long[] timestamps,
            int end_index) {
        printTimestamp(msg, timestamps[end_index - 1], timestamps[end_index]);
    }

    /**
     * Convert from Double to double
     *
     * @param Double
     *            ArrayList
     * @return converted double array
     */
    public static double[] convertDouble(ArrayList<Double> dList) {
        double[] dConverted = new double[dList.size()];
        for (int i = 0; i < dConverted.length; i++) {
            dConverted[i] = dList.get(i).doubleValue();
        }
        return dConverted;
    }

    /**
     * Determines the Eudclidean distance between data points
     *
     * @param sx1
     *              Source's x1 value
     * @param sx2
     *              Source's x2 value
     * @param dx1
     *              Destination's x1 value
     * @param dx2
     *              Destination's x2 value
     *
     * @return Euclidean distance between values
     */
    public static double euclDist(double sx1, double sx2, double dx1,
            double dx2) {
        return Math.sqrt(Math.pow((dx1 - sx1), 2) + Math.pow((dx2 - dx1), 2));
    }

    /**
     * Reads a file that maps UIDs to user names
     *
     * @return Mapping of UIDs to user names
     */
    private static HashMap<String, String> readUIDLookup() {
        HashMap<String, String> tbl = new HashMap<String, String>();
        try {
            Scanner scan = new Scanner(new File(PATH_IN_CSV + UID_LOOKUP_FILE));
            // skip header line
            String[] line = scan.nextLine().split(",");
            while (scan.hasNext()) {
                line = scan.nextLine().split(",");
                tbl.put(line[0], line[1]);
            }
            scan.close();
        } catch (FileNotFoundException e) {
            System.err.println("Warning: No UID lookup file found!");
        }
        return tbl;
    }

    /**
     * Prints the top N results from a list of influencers
     *
     * @param lst
     *              Sorted listing of influencers by UID
     * @param n
     *              Number of results to print
     */
    public static void printTopN(ArrayList<String> lst, int n) {
        HashMap<String, String> tbl = readUIDLookup();
        System.out.println("================================================");
        System.out.println("Top " + n + " influencers:");
        System.out.println("================================================");
        for (int cntr=0; cntr<n; cntr++) {
            // default to UID if no name is found
            String id = "UID: " + lst.get(cntr);
            if (tbl.containsKey(lst.get(cntr))) {
                id += " - " + tbl.get(lst.get(cntr));
            }
            System.out.println((cntr + 1) + ". " + id);
        }
    }
}
