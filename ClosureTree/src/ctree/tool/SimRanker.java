package ctree.tool;

import java.util.*;
import ctree.graph.*;
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
            		System.out.println("Graph is in answer set since similarity is above threshold="+ range+" and prob > threshold="+probThresh);
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

  public void clear() {
    pqueue.clear();
    accessCount = 0;
  }

  public int getAccessCount() {
    return accessCount;
  }

}
