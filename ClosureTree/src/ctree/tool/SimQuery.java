package ctree.tool;

import java.util.*;

import ctree.index.*;
import ctree.graph.*;
import ctree.mapper.*;

import ctree.util.*;

import ctree.lgraph.*;
import java.io.PrintWriter;

import ctree.tool.BuildCTree;;

/**
 *
 * @author Huahai He
 * @version 1.0
 */

public class SimQuery {
    /*
     static private double confidence;
     static private double validity;
     static private double precision;
         static private double confidence_L;
     */

    private static void usage() {
        System.err.println(
                "Usage: ... [options] ctree_file query_file");
        System.err.println("  -knn=INT \t\t K-NN query");
        System.err.println("  -range=DOUBLE \t range query");
        System.err.println(
                "  -nQ=INT \t\t number of queries, default=queries in query_file");
        System.err.println(
                "  -strict=[yes|no] \t strict ranking, default=yes");
        System.err.println(
                "  -output=FILE \t\t if this option is present, then output the answers");
    }

    public static void main(String[] args) throws Exception {
    	
    	  //String[] customArgs = {"-range=0", "-probThresh=0.0", "-output=output.txt", "toyDatabaseG.txt", "toyQueryG.txt"};
    		String[] customArgs = {"-range=153", "-probThresh=1.0E-24", "-output=answerSet.txt", "graphDatabase.txt", "queryGraph.txt"};

    	
        Opt opt = new Opt(customArgs);
        if (opt.args() < 2) {
            usage();
            return;
        }
        
        //System.err.println("Load ctree " + opt.getArg(0));
        //CTree ctree = CTree.load(opt.getArg(0));

        // Added
        System.err.println("Building ctree from " + opt.getArg(0)+"\n");
        LGraph[] graphs = LGraphFile.loadLGraphs(opt.getArg(0));
        
        double probThresh=0.0;
        if (opt.hasOpt("probThresh")) {
        	probThresh = opt.getDouble("probThresh");
        }
        
        GraphMapper mapper = new NeighborBiasedMapper(new LGraphWeightMatrix(), 10, probThresh); // bonus = 10
        GraphSim graphSim = new LGraphSim();
        LabelMap labelMap = new LabelMap(graphs);
        int L = labelMap.size();
        int dim1 = Math.min(97, L);
        int dim2 = Math.min(97, L * L);
        GraphFactory factory = new LGraphFactory(labelMap, dim1, dim2);
        
        // Decide m, M
        int m = 50;
        int M = 99;
        CTree ctree = BuildCTree.buildCTree(graphs, m, M, mapper, graphSim, labelMap, factory); // build ctree using hierarchical clustering
        
        Graph[] queries = LGraphFile.loadLGraphs(opt.getArg(1));

        boolean knn;
        int k = 0;
        double range = 0;
        if (opt.hasOpt("knn")) {
            k = opt.getInt("knn");
            knn = true;
        } else if (opt.hasOpt("range")) {
            range = opt.getDouble("range");
            knn = false;
        } else {
            usage();
            return;
        }

        int nQ = opt.getInt("nQ", queries.length);
        boolean strict = opt.getString("strict", "yes").equals("yes");

        String output = opt.getString("output");
        PrintWriter out = null;
        if (output != null) {
            out = new PrintWriter(output);
        }

        
        
        
        // if you want to generate probability for database graphs
        /*
        File file = new File("graphIdToProbMapping.txt");
        PrintWriter probOutput = new PrintWriter(file);
        SimRanker ranker = new SimRanker(ctree, mapper, graphSim, queries[0],strict);
        ranker.generateProbs(graphs, queries, probOutput);
        probOutput.close();
        */
        
        
        
        // We always compute exact sim even for internal node, so next comment is invalid
        // By strict ranking, the similarity between a ctree node and the query 
        // is computed by upper bound.

        //GraphMapper mapper = new NeighborBiasedMapper(new LGraphWeightMatrix());
        //GraphSim graphSim = new LGraphSim();
        DataSum stat = new DataSum();

        System.err.println("Range Query:\n");
        for (int i = 0; i < nQ; i++) {
        	  System.out.println("For query graph:");
        	  System.out.println(queries[i].toString());
        	  
            long query_time = System.currentTimeMillis();

            Vector<RankerEntry> ans;
            if (knn) {
                ans = kNNQuery(ctree, mapper, graphSim, queries[i], k, strict);
            } else {
            	
            		/* in optimizedRangeQuery, you prune internal Ctree nodes which fail probability test */
                //ans = optimizedRangeQuery(ctree, mapper, graphSim, queries[i], -range, strict);
            	
            		/* in naiveRangeQuery, you don't prune internal Ctree nodes which fail probability test, instead you prune
                	 at the end */
                //ans = naiveRangeQuery(ctree, mapper, graphSim, queries[i], -range, strict, probThresh);
                
            		/* in samplingRangeQuery, at each Ctree node, you generate PossibleWorld deterministic versions of 
            		   the uncertain graph whose prob > probThreshold and set sim = weighted similarity of those possible 
            		   worlds */
                //ans = samplingRangeQuery(ctree, mapper, graphSim, queries[i], -range, strict, probThresh);

                /* Greedy Representative graph approach */
                //ans = GPRepresentativeRangeQuery(ctree, mapper, graphSim, queries[i], -range, strict, probThresh);
                
                /* ADR Representative graph approach */
                ans = ADRRepresentativeRangeQuery(ctree, mapper, graphSim, queries[i], -range, strict, probThresh);

            }
            query_time = System.currentTimeMillis() - query_time;

            // Output answers to this query
            if (output != null) {
                out.println("Answer set size: "+ans.size());
                for (RankerEntry e : ans) {
                    Graph g = e.getGraph();
                    out.println("GraphId: "+((LGraph) g).getId()+" Similarity: "+-e.getDist());
                }
            }

            // statistics
            stat.add("query_time", query_time);
            stat.add("ans_size", ans.size());
            stat.add("access_ratio", (double) accessCount / ctree.size());
            //stat.append("confidence", confidence);
            //stat.append("validity", validity);
            //stat.append("precision", precision);
            //stat.append("confidence_L", confidence_L);

            double sim = 0, simUp = 0, rate = 0;
            int size = ans.size();
            for (int j = 0; j < size; j++) {
                RankerEntry entry = (RankerEntry) ans.elementAt(j);
                double temp1, temp2;
                sim += temp1 = -entry.getDist();
                simUp += temp2 = graphSim.simUpper(queries[i], entry.getGraph());
                //ratio += (double) accessCount / ctree.size();
                rate += temp1 / temp2;
            }
            stat.add("sim", sim / size);
            stat.add("simUp", simUp / size);
            stat.add("rate", rate / size); //sim/simUp
            stat.add("norm", graphSim.norm(queries[i]));

            if ((i + 1) % 10 == 0) {
                System.err.println("Query at " + (i + 1));
            }
        } // for queries

        if (output != null) {
            out.close();
        }

        //format: query_time(ms) ans_size access_ratio confidence validity precision confidence_L
        System.err.println("Result: Query_time(ms) Ans_size Access_ratio");
        System.out.printf("%f %f %f\n",
                          stat.mean("query_time"), stat.mean("ans_size"),
                          stat.mean("access_ratio"));

        /*
         double[][] M = stat.report("norm", "sim", "simUp", "access_ratio", "rate");
             for (double[] row : M) {
         System.err.printf("%d %f %f %f %f\n", (int) row[0], row[1], row[2], row[3],
                            row[4]);
             }*/

        /*
         // Report by merging rows with identical graph norms
         double[][] H = stat.reportOnKey("norm", "sim", "simUp", "access_ratio",
                                        "rate");
             int cols = H[0].length;
             for (double[] row : H) {
          if (row[cols - 1] == 0) {
            continue;
          }
          // format: norm sim simUp access_ratio rate count
         System.out.printf("%d %f %f %f %f %d\n", (int) row[0], row[1], row[2],
                            row[3], row[4], (int) row[5]);
             }*/
    }

    private static int accessCount;

    /**
     * Query using NNRanker
     * @param ctree CTree
     * @param mapper GraphMapper
     * @param graphSim GraphSim
     * @param query Graph
     * @param k int
     * @param preciseRanking boolean
     * @return Vector
     */
    public static Vector<RankerEntry> kNNQuery(CTree ctree, GraphMapper mapper,
                                               GraphSim graphSim,
                                               Graph query, int k,
                                               boolean strictRanking) {
        /*SimRanker ranker = new SimRanker(ctree, mapper, query, preciseRanking);
             RankerEntry entry;
             Vector ans = new Vector(k); // answer set
             while ( (entry = ranker.nextNN()) != null && ans.size() < k) {
          ans.addElement(entry);
             }
             accessCount = ranker.getAccessCount();
             ranker.clear();
             return ans;
         */

        SimRanker ranker = new SimRanker(ctree, mapper, graphSim, query,
                                         strictRanking);
        Vector<RankerEntry> ans = ranker.optimizedKNNQuery(k);
        accessCount = ranker.getAccessCount();

        /*
                 // compute confidence, validity and precision
                 double simAtK = -ans.elementAt(k - 1).getDist();
                 Vector<RankerEntry> ansUp = ranker.upperRangeQuery(simAtK);

                 if (ansUp.size() <= k) {
            confidence = 1;
            validity = 1;
            precision = 1;
            confidence_L = k - 1;
                 } else {
            confidence = (double) (k - 1) / (ansUp.size() - 1); // omit the answer which is the query
            double simUpAtK = -ansUp.elementAt(k - 1).getDist();
            int j;
            for (j = 0; j < k; j++) {
                if ( -ans.elementAt(j).getDist() < simUpAtK) {
                    break;
                }
            }
            validity = (double) (j - 1) / (k - 1);
            precision = simAtK / simUpAtK;
            confidence_L = ansUp.size() - 1;
                 }*/
        return ans;
    }

    /**
     * Optimized Range query
     * @param ctree CTree
     * @param mapper GraphMapper
     * @param graphSim GraphSim
     * @param query Graph
     * @param range double
     * @param preciseRanking boolean
     * @return Vector
     */
    public static Vector<RankerEntry> optimizedRangeQuery(CTree ctree,
                                                 GraphMapper mapper,
                                                 GraphSim graphSim,
                                                 Graph query, double range,
                                                 boolean preciseRanking) {
        SimRanker ranker = new SimRanker(ctree, mapper, graphSim, query,
                                         preciseRanking);
        System.out.println("Entered here");
        RankerEntry entry;
        Vector<RankerEntry> ans = new Vector(); // answer set
        while ((entry = ranker.optimizedRangeQuery(-range)) != null && entry.getDist() <= range) {
            ans.addElement(entry);
        }
        accessCount = ranker.getAccessCount();
        ranker.clear();        
        return ans;
    }
    
    /**
     * Naive Range query
     * @param ctree CTree
     * @param mapper GraphMapper
     * @param graphSim GraphSim
     * @param query Graph
     * @param range double
     * @param preciseRanking boolean
     * @return Vector
     */
    public static Vector<RankerEntry> naiveRangeQuery(CTree ctree,
                                                 GraphMapper mapper,
                                                 GraphSim graphSim,
                                                 Graph query, double range,
                                                 boolean preciseRanking,
                                                 double probThresh) {
        SimRanker ranker = new SimRanker(ctree, mapper, graphSim, query,
                                         preciseRanking);
        RankerEntry entry;
        Vector<RankerEntry> ans = new Vector(); // answer set
        while ((entry = ranker.naiveRangeQuery(-range, probThresh)) != null && entry.getDist() <= range && entry.prob >= probThresh) {
            ans.addElement(entry);
        }
        accessCount = ranker.getAccessCount();
        ranker.clear();        
        return ans;
    }
    
    /**
     * Sampling Range query
     * @param ctree CTree
     * @param mapper GraphMapper
     * @param graphSim GraphSim
     * @param query Graph
     * @param range double
     * @param preciseRanking boolean
     * @return Vector
     */
    public static Vector<RankerEntry> samplingRangeQuery(CTree ctree,
                                                 GraphMapper mapper,
                                                 GraphSim graphSim,
                                                 Graph query, double range,
                                                 boolean preciseRanking,
                                                 double probThresh) {
        SimRanker ranker = new SimRanker(ctree, mapper, graphSim, query,
                                         preciseRanking);
        RankerEntry entry;
        Vector<RankerEntry> ans = new Vector(); // answer set
        while ((entry = ranker.samplingRangeQuery(-range, probThresh)) != null && entry.getDist() <= range) {
            ans.addElement(entry);
        }
        accessCount = ranker.getAccessCount();
        ranker.clear();        
        return ans;
    }

    
    public static Vector<RankerEntry> GPRepresentativeRangeQuery(CTree ctree,GraphMapper mapper,GraphSim graphSim,Graph query, double range,boolean preciseRanking,double probThresh) 
    {
    	SimRanker ranker = new SimRanker(ctree, mapper, graphSim, query,
    			preciseRanking);
    	RankerEntry entry;
    	Vector<RankerEntry> ans = new Vector(); // answer set
    	while ((entry = ranker.GPRepresentativeRangeQuery(-range, probThresh)) != null && entry.getDist() <= range) {
    		ans.addElement(entry);
    	}
    	accessCount = ranker.getAccessCount();
    	ranker.clear();        
    	return ans;
    }

    public static Vector<RankerEntry> ADRRepresentativeRangeQuery(CTree ctree,GraphMapper mapper,GraphSim graphSim,Graph query, double range,boolean preciseRanking,double probThresh) 
    {
    	int numberOfSteps=100;
    	SimRanker ranker = new SimRanker(ctree, mapper, graphSim, query,
    			preciseRanking);
    	RankerEntry entry;
    	Vector<RankerEntry> ans = new Vector(); // answer set
    	while ((entry = ranker.ADRRepresentativeRangeQuery(-range, probThresh,numberOfSteps)) != null && entry.getDist() <= range) {
    		ans.addElement(entry);
    	}
    	accessCount = ranker.getAccessCount();
    	ranker.clear();        
    	return ans;
    }
}
