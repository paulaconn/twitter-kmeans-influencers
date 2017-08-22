import java.io.*;
import java.util.*;
import java.text.NumberFormat;
import org.gephi.data.attributes.api.*;
import org.gephi.graph.api.*;
import org.gephi.project.api.*;
import org.gephi.statistics.plugin.*;
import org.openide.util.*;

//for kmeans
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.core.Instance;
import net.sf.javaml.tools.weka.WekaClusterer;
import weka.core.Instances;
import weka.clusterers.Clusterer;
import weka.clusterers.SimpleKMeans;

/**
 * TwitterAnalysis.java
 *
 * Main program for running our analysis of Twitter data
 *
 * Contributors:
 * 
 * @author Paula
 * @author Schuyler
 *
 */
public class TwitterAnalysis {

    // ########## Constants ##########

    // ########## Variables ##########
    private static HierarchicalDirectedGraph gephiGraph = null;
    private static AttributeModel gephiGraphAttributes = null;
    private static ArrayList<Double> allPRVal= new ArrayList <Double>();
    private static ArrayList<Double> allBTVal= new ArrayList <Double>();
    private static ArrayList<Double> allDEGVal= new ArrayList <Double>();
    // ########## Functions ##########

    /**
     * Creates a graph using the Gephi libraries from file information
     *
     * @param _filePath
     *            File path to process
     * @return A Gephi HD Graph object
     */
    public static HierarchicalDirectedGraph createGephiGraph(String _filePath)
            throws FileNotFoundException {

        Scanner scan = new Scanner(new File(_filePath));
        Node n1 = null, n2 = null;

        ProjectController gephiController = Lookup.getDefault()
                .lookup(ProjectController.class);
        gephiController.newProject();

        GraphModel gephiModel = Lookup.getDefault()
                .lookup(GraphController.class).getModel();
        gephiGraph = gephiModel.getHierarchicalDirectedGraph();
        gephiGraphAttributes = Lookup.getDefault()
                .lookup(AttributeController.class).getModel();
        // three columns in the csv files, initialized to the header line
        String[] line = scan.nextLine().split(",");
        String userA = line[0];
        String userB = line[1];
        String weightAB = line[2];

        while (scan.hasNext()) {
            // parse
            line = scan.nextLine().split(",");
            userA = line[0];
            userB = line[1];
            weightAB = line[2];
            // make the new nodes
            n1 = gephiGraph.getNode(userA);
            n2 = gephiGraph.getNode(userB);
            if (n1 == null) {
                n1 = gephiModel.factory().newNode(userA);
                gephiGraph.addNode(n1);
            }
            if (n2 == null) {
                n2 = gephiModel.factory().newNode(userB);
                gephiGraph.addNode(n2);
            }
            // build the edges between nodes
            Edge weightEdge = gephiModel.factory().newEdge(n1, n2,
                    Integer.parseInt(weightAB), true);
            gephiGraph.addEdge(weightEdge);
        }
        scan.close();

        // ensure that the graph has been made correctly w/ formating
        System.out.printf("Total Nodes = %s\n", NumberFormat.getNumberInstance()
                .format(gephiGraph.getNodeCount()));
        System.out.printf("Total Edges = %s\n", NumberFormat.getNumberInstance()
                .format(gephiGraph.getEdgeCount()));

        // run PageRank calculation on graph
        System.out.println("Running PageRank algorithm...");
        PageRank gephiPageRank = new PageRank();
        gephiPageRank.setUseEdgeWeight(true);
        gephiPageRank.setDirected(true);
        gephiPageRank.execute(gephiGraph, gephiGraphAttributes);

        // run degree centrality calculation on graph
        System.out.println("Calculating degree centrality...");
        Degree dCentrality = new Degree();
        dCentrality.execute(gephiGraph, gephiGraphAttributes);
        // run betweenness calculation on graph
        System.out.println("Calculating betweenness...");
        GraphDistance betweenness = new GraphDistance();
        betweenness.setDirected(true);
        betweenness.execute(gephiGraph, gephiGraphAttributes);

        return gephiGraph;
    }

    /**
     * Calculates the k-means for the output csv file
     *
     * @param Dataset
     *            inputDataset
     * @return k-means values in .csv file
     */
    public static void calculateKmeans(Dataset _inputDataset) {
        System.out.println("CALCULATING KMEANS....");
        System.out.println("Number in dataset= "+_inputDataset.noAttributes());
        System.out.println("");

        SimpleKMeans skm = new SimpleKMeans();

        WekaClusterer KMeansObj = new WekaClusterer(skm);
        Dataset[] kMeansDataset = ((WekaClusterer) KMeansObj)
            .cluster(_inputDataset);
        // write out clustering information
        System.out.println("KMEANS COMPLETE");
        System.out.println(
                "Number of sets in k-Means Output: " + kMeansDataset.length);
        System.out.println("K Means sets writing to output file...");
        System.out.println("");

        BufferedWriter br = null;
        try {
            br = new BufferedWriter(new FileWriter(
                    CommonUtils.PATH_OUT_CSV + "Kmeans_Output.txt"));
            StringBuilder sb = new StringBuilder();

            for (Dataset kmeansVal : kMeansDataset) {
                sb.append(kmeansVal);
                sb.append("\n");
                br.write(sb.toString());
                br.flush();
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // calculate the centroid
        Instances centroids = skm.getClusterCentroids();

        // extract the last centroid value calculated
        double c0 = centroids.instance(0).toDoubleArray()
            [centroids.instance(0).numValues()-1];
        double c1 = centroids.instance(1).toDoubleArray()
            [centroids.instance(1).numValues()-1];
        System.out.println("Centroid 0: " + c0);
        System.out.println("Centroid 1: " + c1);
    }

    // ########## Main Execution ##########

    /**
     * Main execution point of the program
     */
    public static void main(String[] args) {
        // set debug macro based on command line args; this changes
        // the data set used
        boolean isDebug = false;
        if ((args.length > 0) && (args[0].equals("DEBUG"))) {
            isDebug = true;
        }

        String csvFiles[] = { "MentionNetwork", "FollowerNetwork",
                "RetweetNetwork" };
        String filePath = null;
        // Recording starting times of certain operations
        // timestamping for processing
        long[] proc_time = new long[csvFiles.length + 1];
        // timestamping for file processings
        long[] file_time = new long[csvFiles.length + 1];
        long start_time = System.currentTimeMillis();

        // Open each file and generate a weighted graph for gephi to calculate:
        // PR, BT, C
        for (int i = 0; i < csvFiles.length; i++) {
            File csvFile;
            if (isDebug) {
                csvFile = new File(
                        CommonUtils.PATH_IN_TEST_CSV + csvFiles[i] + ".csv");
            } else {
                csvFile = new File(
                        CommonUtils.PATH_IN_CSV + csvFiles[i] + ".csv");
            }
            filePath = csvFile.getAbsolutePath();
            boolean fileExists = csvFile.exists();
            if (!fileExists) {
                System.out.printf(
                        "%s could not be found, please"
                                + " locate and add it to the project folder",
                        csvFiles[i]);
                System.exit(0);
            }
            System.out.println("Referencing: " + csvFiles[i] + ".csv");

            try {
                // run calculations
                proc_time[i] = System.currentTimeMillis();
                HierarchicalDirectedGraph _gephiGraph = createGephiGraph(
                        filePath);
                CommonUtils.printTimestamp("Time processing " + csvFiles[i],
                        proc_time[i], System.currentTimeMillis());

                // write the results to a file
                file_time[i] = System.currentTimeMillis();
                File outFile;
                if (isDebug) {
                    outFile = new File(CommonUtils.PATH_OUT_TEST_CSV
                            + csvFiles[i] + "_" + i + "PR.csv");
                } else {
                    outFile = new File(CommonUtils.PATH_OUT_CSV + csvFiles[i]
                            + "_" + i + "PR.csv");
                }

                FileWriter writer = new FileWriter(outFile);
                writer.write(
                        "id,ename,etype,freq,pagerank,degree,betweenness\n");
                writer.flush();

                for (Node n : _gephiGraph.getNodes()) {
                    Attributes id = n.getNodeData().getAttributes();
                    AttributeColumn pRank = gephiGraphAttributes.getNodeTable()
                            .getColumn(PageRank.PAGERANK);
                    AttributeColumn dCentrality = gephiGraphAttributes
                            .getNodeTable().getColumn(Degree.DEGREE);
                    AttributeColumn betweeness = gephiGraphAttributes
                            .getNodeTable()
                            .getColumn(GraphDistance.BETWEENNESS);
                    // get page rank value
                    double prVal = (double) n.getNodeData().getAttributes()
                            .getValue(pRank.getIndex());
                    // get degree centrality value **changed degree to double
                    double degreeVal = ((double)(Integer) n.getNodeData()
                            .getAttributes().getValue(dCentrality.getIndex()));
                    // get betweenness value
                    double betweenVal = (double) n.getNodeData().getAttributes()
                            .getValue(betweeness.getIndex());
                    // build the file output formating
                    String str = id.getValue(0) + "," + id.getValue(3) + ","
                            + id.getValue(2) + "," + id.getValue(4) + ","
                            + prVal + "," + degreeVal + "," + betweenVal
                            + " \n ";
                    writer.write(str);
                    writer.flush();

                    // store information for instance & dataset (kmeans)
                    allPRVal.add(prVal);
                    allDEGVal.add(degreeVal);
                    allBTVal.add(betweenVal);
                }
                writer.close();
                // store and print the time stamp for the operation
                CommonUtils.printTimestamp("Time writing    " + csvFiles[i],
                        file_time[i], System.currentTimeMillis());
                // prep for the file
                System.out.println("");
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }

        // create a dataset of the information to be used for kmeans
        double[] allPRArr = CommonUtils.convertDouble(allPRVal);
        double[] allDEGArr = CommonUtils.convertDouble(allDEGVal);
        double[] allBTArr = CommonUtils.convertDouble(allBTVal);

        Instance PRinstance = new DenseInstance(allPRArr, "PageRank");
        Instance DEGinstance = new DenseInstance(allDEGArr, "DegreeCentrality");
        Instance BTinstance = new DenseInstance(allBTArr, "Betweeness");

        Dataset inputDataset = new DefaultDataset();
        inputDataset.add(PRinstance);
        inputDataset.add(DEGinstance);
        inputDataset.add(BTinstance);

        calculateKmeans(inputDataset);

        // report the total time spent on this program
        CommonUtils.printTimestamp("Total time spent", start_time,
                System.currentTimeMillis());
    }
}
