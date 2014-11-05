package ctree.tool;

import java.io.PrintWriter;
import java.util.*;
import ctree.graph.*;
import ctree.lgraph.LGraph;
import ctree.mapper.*;
import ctree.index.*;




/**
 *
 * @author Huahai He
 * @version 1.0
 */

public class SimRanker {
  public SimRanker() {
  }

  private CTree ctree;
  private Graph query;
  private GraphMapper mapper;
  private GraphSim graphSim;
  private PriorityQueue<RankerEntry> pqueue;
  private boolean strictRanking;
  private int accessCount = 0; // the number of accessed nodes and graphs

  public SimRanker(CTree _ctree, GraphMapper _mapper, GraphSim _graphSim,
                   Graph _query,
                   boolean _strictRanking) {
    ctree = _ctree;
    mapper = _mapper;
    graphSim = _graphSim;
    query = _query;
    strictRanking = _strictRanking;
    //pqueue = new VectorHeap();
    pqueue = new java.util.PriorityQueue();
    RankerEntry element = new RankerEntry(0, ctree.getRoot());
    pqueue.add(element);
  }

  /**
   * Return the next Nearest Neighbor
   * @return Data and its feature distance to the query point
   */
  public RankerEntry nextNN() {
    while (!pqueue.isEmpty()) {
      RankerEntry entry = pqueue.poll();
      Object obj = entry.getObject();
      if (obj instanceof Graph) { // object
        return entry; // if it is a graph, it is returned
      }
      else { // index node or leaf node
        CTreeNode node = (CTreeNode) obj;
        //insert all children into pqueue
        for (int i = 0; i < node.getEntries().size(); i++) {
          Object child = node.childAt(i);
          Graph g = node.childGraphAt(i); // if g is not a leaf node, its closure is returned 
          double sim;
          if (strictRanking && !node.isLeaf()) {
            int sim1 = graphSim.simUpper(query, g); // this seems to be histogram based for closure 
            int[] map = ((NeighborBiasedMapper)mapper).mapRangeQuery(query, g);
            //int[] map = ((NeighborBiasedMapper)mapper).map(query, g);
            sim = graphSim.sim(query, g, map); // this seems to be for database graph
            System.out.println("Upper bound = "+sim1+" Exact sim = "+sim);
          }
          else {
            int[] map = ((NeighborBiasedMapper)mapper).mapRangeQuery(query, g);
          	//int[] map = ((NeighborBiasedMapper)mapper).map(query, g);
          	sim = graphSim.sim(query, g, map); // this seems to be for database graph
            System.out.println("Exact sim to graph "+g.toString()+"= "+sim);
          }
          RankerEntry entry2 = new RankerEntry( -sim, child);
          pqueue.add(entry2);
          accessCount++;
        }
      }
    }
    return null;
  }
  
  /**
   * Return the next nearest neighbour satisfying range threshold with pruning at internal Ctree nodes based on prob threshold
   * @return Data and its feature distance to the query point
   */
  public RankerEntry optimizedRangeQuery(double range) {
    while (!pqueue.isEmpty()) {
      RankerEntry entry = pqueue.poll();
      Object obj = entry.getObject();
      
      if (obj instanceof Graph) { // object
        return entry; // if it is a graph, it is returned
      }
      
      else { // index node or leaf node
        CTreeNode node = (CTreeNode) obj;
        //insert all children into pqueue
        for (int i = 0; i < node.getEntries().size(); i++) {
          Object child = node.childAt(i);
          Graph g = node.childGraphAt(i); // if g is not a leaf node, its closure is returned 
          double sim;
          
          if (strictRanking && !node.isLeaf()) {
            int sim1 = graphSim.simUpper(query, g); // this seems to be histogram based for closure 
            int[] map = ((NeighborBiasedMapper)mapper).mapRangeQuery(query, g);
            //int[] map = ((NeighborBiasedMapper)mapper).map(query, g);
            sim = graphSim.sim(query, g, map); // this seems to be for database graph
            System.out.println("Upper bound of sim = "+sim1+" Exact sim = "+sim);
            if(sim < range){
            	System.out.println("Pruned Ctree internal node since similarity falls below threshold="+ range+" !");
            	accessCount++;
            	continue; // do not add to PQ 
            }
            else {
            	System.out.println("Ctree internal node is not pruned since similarity is above threshold="+ range);
            }
          }
          
          else {
            int[] map = ((NeighborBiasedMapper)mapper).mapRangeQuery(query, g);
          	//int[] map = ((NeighborBiasedMapper)mapper).map(query, g);
          	sim = graphSim.sim(query, g, map); // this seems to be for database graph
            System.out.println("Exact sim to graph "+g.id()+"= "+sim);
            if(sim < range){
            	System.out.println("Graph is not in answer set since similarity falls below threshold="+ range+" !");
            }
            else {
            	System.out.println("Graph is in answer set since similarity is above threshold="+ range);
            }
          }
          
          System.out.println();
          
          RankerEntry entry2 = new RankerEntry( -sim, child);
          pqueue.add(entry2);
          accessCount++;
        }
      }
    }
    return null;
  }

  /**
   * Return the next nearest neighbour satisfying range threshold without pruning at internal Ctree nodes based on prob threshold
   * @return Data and its feature distance to the query point
   */
  public RankerEntry naiveRangeQuery(double range, double probThresh) {
    while (!pqueue.isEmpty()) {
      RankerEntry entry = pqueue.poll();
      Object obj = entry.getObject();
      
      if (obj instanceof Graph) { // object
        return entry; // if it is a graph, it is returned
      }
      
      else { // index node or leaf node
        CTreeNode node = (CTreeNode) obj;
        //insert all children into pqueue
        for (int i = 0; i < node.getEntries().size(); i++) {
          Object child = node.childAt(i);
          Graph g = node.childGraphAt(i); // if g is not a leaf node, its closure is returned 
          double sim;
          
          if (strictRanking && !node.isLeaf()) {
            int sim1 = graphSim.simUpper(query, g); 
            int[] map = ((NeighborBiasedMapper)mapper).map(query, g); // probability of mapped Ctree node not considered for pruning here
            sim = graphSim.sim(query, g, map); 
            System.out.println("Upper bound of sim = "+sim1+" Exact sim = "+sim);
            if(sim < range){
            	System.out.println("Pruned Ctree internal node since similarity falls below threshold="+ range+" !");
            	accessCount++;
            	continue; // do not add to PQ 
            }
            else {
            	System.out.println("Ctree internal node is not pruned since similarity is above threshold="+ range);
            }
            RankerEntry entry2 = new RankerEntry( -sim, child);
            pqueue.add(entry2);
          }
          
          else {
            int[] map = ((NeighborBiasedMapper)mapper).mapAndReturnProb(query, g, entry); // for a database graph, we calculate probability of mapped G2
          	//int[] map = ((NeighborBiasedMapper)mapper).map(query, g);
          	sim = graphSim.sim(query, g, map); // this seems to be for database graph
            System.out.println("Exact sim to graph "+g.id()+"= "+sim);
            System.out.println("Probability of mapped version of graph "+g.id()+"= "+entry.prob);
            
            if(sim < range){
            	System.out.println("Graph is not in answer set since similarity falls below threshold="+ range+" !");
            }
            else {
            	if(entry.prob >= probThresh)
            		System.out.println("Graph is in answer set since similarity > threshold="+ range+" and prob > threshold="+probThresh);
            	else
            		System.out.println("Graph is not in answer set since probablity of mapped version falls below threshold="+ probThresh+" !");
            }
            
            RankerEntry entry2 = new RankerEntry( -sim, child);
            entry2.prob = entry.prob;
            pqueue.add(entry2);
          }
          
          System.out.println();
         
          accessCount++;
        }
      }
    }
    return null;
  }  

  /**
   * Sampling range query
   * @return Data and its feature distance to the query point
   */
  public RankerEntry samplingRangeQuery(double range, double probThresh) {
  	
  	int noOfSampledGraphs = 1000;
  	
    while (!pqueue.isEmpty()) {
      RankerEntry entry = pqueue.poll();
      Object obj = entry.getObject();
      
      if (obj instanceof Graph) { // object
        return entry; // if it is a graph, it is returned
      }
      
      else { // index node or leaf node
        CTreeNode node = (CTreeNode) obj;
        //insert all children into pqueue
        for (int i = 0; i < node.getEntries().size(); i++) {
          Object child = node.childAt(i);
          Graph g = node.childGraphAt(i); // if g is not a leaf node, its closure is returned 
          double weightedSim;
          
          if (strictRanking && !node.isLeaf()) { 
          	
            LGraph[] sampledGraphs = ((LGraph)g).sampledGraphs(noOfSampledGraphs, probThresh);
            double weightedSim_Num = 0.0, weightedSim_Den = 0.0;
            System.out.println("Number of sampled deterministic graphs with prob>probThresh= "+sampledGraphs.length);
            
           for(int j=0; j<sampledGraphs.length; j++){
          	 int[] map = ((NeighborBiasedMapper)mapper).map(query, sampledGraphs[j]); // probability of mapped Ctree node not considered for pruning here
          	 int sim = graphSim.sim(query, sampledGraphs[j], map); 
          	 weightedSim_Num += sampledGraphs[j].probOfGraph * sim;
          	 weightedSim_Den += sampledGraphs[j].probOfGraph;
           }
            
           weightedSim = weightedSim_Num/weightedSim_Den;
           
            System.out.println("Weighted sim = "+weightedSim);
            
            if(weightedSim < range){
            	System.out.println("Pruned Ctree internal node since weighted similarity falls below threshold="+ range+" !");
            	accessCount++;
            	continue; // do not add to PQ 
            }
            else {
            	System.out.println("Ctree internal node is not pruned since weighted similarity is above threshold="+ range);
            }
          }
          
          else {
          	
            LGraph[] sampledGraphs = ((LGraph)g).sampledGraphs(noOfSampledGraphs, probThresh);
            double weightedSim_Num = 0.0, weightedSim_Den = 0.0;
            System.out.println("Number of sampled deterministic graphs with prob>probThresh= "+sampledGraphs.length);
            
           for(int j=0; j<sampledGraphs.length; j++){
          	 int[] map = ((NeighborBiasedMapper)mapper).map(query, sampledGraphs[j]); // probability of mapped Ctree node not considered for pruning here
          	 int sim = graphSim.sim(query, sampledGraphs[j], map); 
          	 weightedSim_Num += sampledGraphs[j].probOfGraph * sim;
          	 weightedSim_Den += sampledGraphs[j].probOfGraph;
           }
            
           weightedSim = weightedSim_Num/weightedSim_Den;
           
           System.out.println("Weighted sim of graph "+g.id()+"= "+weightedSim);
            
            if(weightedSim < range){
            	System.out.println("Graph is not in answer set since since weighted similarity falls below threshold="+ range+" !");
            }
            else {
            	System.out.println("Graph is in answer set since weighted similarity is above threshold="+ range);
            }
          }
          
          RankerEntry entry2 = new RankerEntry( -weightedSim, child);
          pqueue.add(entry2);
          System.out.println();
         
          accessCount++;
        }
      }
    }
    return null;
  }  
  
  
  public Vector<RankerEntry> optimizedKNNQuery(int k) {
    PriorityQueue<RankerEntry> knnPQ = new PriorityQueue(k); ;
    double lowerBound = -1;
    Hist queryHist = ctree.factory.toHist(query);
    Vector<RankerEntry> ans = new Vector<RankerEntry> (k);

    while (!pqueue.isEmpty()) {
      RankerEntry entry = pqueue.poll();
      Object obj = entry.getObject();
      if (obj instanceof Graph) { // object
        ans.addElement(entry);
        if (ans.size() >= k) {
          break;
        }
      }
      else { // index node or leaf node
        CTreeNode node = (CTreeNode) obj;
        //insert all children into PQ
        for (int i = 0; i < node.getEntries().size(); i++) {
          double simUp = Double.POSITIVE_INFINITY;
          if (lowerBound >= 0) {
            simUp = Hist.commonCounts(queryHist,node.histAt(i));
            if (simUp <= lowerBound) {
              continue;
            }
          }
          Object child = node.childAt(i);
          Graph g = node.childGraphAt(i);
          double sim;
          if (strictRanking && !node.isLeaf()) {
            simUp = Math.min(simUp, graphSim.simUpper(query, g));
            sim = simUp;
          }
          else {
            int[] map = mapper.map(query, g);
            sim = graphSim.sim(query, g, map);
          }
          accessCount++;
          if (sim <= lowerBound) {
            continue;
          }

          // update lowerbound
          if (child instanceof Graph) {
            knnPQ.add(new RankerEntry(sim, child));
            if (knnPQ.size() >= k) {
              if (knnPQ.size() > k) {
                knnPQ.poll();
              }
              lowerBound = knnPQ.peek().getDist();
            }
          }

          // insert child into PQ
          RankerEntry entry2 = new RankerEntry( -sim, child);
          pqueue.add(entry2);

        }
      }
    }

    return ans;
  }

  /**
   * Range query where Sim_Up > range
   * @param range double
   * @return Vector
   */
  public Vector<RankerEntry> upperRangeQuery(double range) {
    pqueue = new java.util.PriorityQueue();
    RankerEntry element = new RankerEntry(0, ctree.getRoot());
    pqueue.add(element);
    //GraphFeature queryFeature = new GraphFeature(query, ctree);
    Vector<RankerEntry> ans = new Vector<RankerEntry> ();
    while (!pqueue.isEmpty()) {
      RankerEntry entry = pqueue.poll();
      Object obj = entry.getObject();
      if (obj instanceof Graph) { // object
        if ( -entry.getDist() <= range) {
          break;
        }
        else {
          ans.addElement(entry);
        }
      }
      else { // index node or leaf node
        CTreeNode node = (CTreeNode) obj;
        //insert all children into pqueue
        for (int i = 0; i < node.getEntries().size(); i++) {
          Object child = node.childAt(i);
          Graph g = node.childGraphAt(i);
          double simUp = graphSim.simUpper(query, g);
          RankerEntry entry2 = new RankerEntry( -simUp, child);
          pqueue.add(entry2);
          accessCount++;
        }
      }
    }
    return ans;
  }

  public void generateProbs(LGraph[] graphs, Graph[] queries, PrintWriter output) {
  	
  	int noOfSampledGraphs = 1000;
  	double probThresh = 0.0;
  	RankerEntry entry = new RankerEntry();
  	
	  for(int i=0; i<queries.length; i++){	
	  	for(int j=0; j<graphs.length; j++){
	  		LGraph[] sampledGraphs = ((LGraph)graphs[j]).sampledGraphs(noOfSampledGraphs, probThresh);
	  		double prob_Num = 0.0, prob_Den = 0.0;
	  		int size = 0;
	  		
	  		for(int k=0; k<sampledGraphs.length; k++){
	  			int[] map = ((NeighborBiasedMapper)mapper).mapAndReturnProb((LGraph)queries[i], sampledGraphs[k], entry);
	  			size+= sampledGraphs[k].numE();
	  			prob_Num += sampledGraphs[k].probOfGraph * entry.prob;
	  			prob_Den += sampledGraphs[k].probOfGraph;
	  		}
	  		
	  		System.out.println("Id: "+graphs[j].id()+" Prob: "+(prob_Num/prob_Den)+" Avg no edges: "+(double)size/sampledGraphs.length);
	  	}
	  }
  	
  }
  
  public void clear() {
    pqueue.clear();
    accessCount = 0;
  }

  public int getAccessCount() {
    return accessCount;
  }

}
