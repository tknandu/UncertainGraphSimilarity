package ctree.alg;

import java.util.*;
import ctree.graph.Graph;
import ctree.lgraph.*;

/**
 * Neighbor Biased Mapping (NBM).
 *
 * @author Huahai He
 * @version 1.0
 */

public class NeighborBiasedMapping {

    static class Entry implements Comparable {
        int u, v;
        double w; // weight
        public Entry(int _u, int _v, double _w) {
            u = _u;
            v = _v;
            w = _w;
        }

        public int compareTo(Object o) {
            Entry e = (Entry) o;
            if (w < e.w) {
                return 1;
            } else if (w == e.w) {
                return 0;
            } else {
                return -1;
            }
        }
    }
    /**
     * Find a graph mapping between two graphs
     *
     * @param n1 number of vertices in G1
     * @param n2 number of vertices in G2
     * @param W initial weight matrix
     * @param alist1 adjacency list of G1
     * @param alist2 adjacency list of G2
     * @param bilist bi-list of the two graphs
     * @param bonus weights adding to neighboring pairs of a matched pair
     * @return vertex map from G1 to G2
     */
    public static int[] mapGraphs(int n1, int n2, double[][] W, int[][] alist1,
                                  int[][] alist2, int[][] bilist,double bonus) {

        // Insert initial entries into PQ
    	//System.out.println("mapping");
        double[] maxW = new double[n1];
        int[] maxMate = new int[n1];
        PriorityQueue<Entry> PQ = new PriorityQueue(n1 * 2);
        for (int i = 0; i < n1; i++) {
            maxW[i] = 0;
            for (int j = 0; j < n2; j++) {
                if (W[i][j] > maxW[i]) {
                    maxW[i] = W[i][j];
                    maxMate[i] = j;
                }
            }
            //System.out.println("Best mate for node "+i+": "+maxMate[i]+" "+maxW[i]);
            if (maxW[i] > 0) {
                PQ.add(new Entry(i, maxMate[i], maxW[i]));
            }
        }

        int[] map = new int[n1];
        int[] rmap = new int[n2];
        Arrays.fill(map, -1);
        Arrays.fill(rmap, -1);

        //System.out.println("Entries in PQ:");
        
        // iteration of mapping in the order of weighted spanning
        int maxSize=PQ.size();
        while (!PQ.isEmpty()) {
            Entry e = PQ.poll();
            //System.out.println(e.u+" "+e.v+" "+e.w);
            if (map[e.u] >= 0) { // u has been mapped
                continue;
            }
            if (rmap[e.v] >= 0) { // v has been mapped, find another v
                maxW[e.u] = 0;
                for (int v : bilist[e.u]) {
                    if (rmap[v] < 0 && W[e.u][v] > maxW[e.u]) {
                        maxW[e.u] = W[e.u][v];
                        maxMate[e.u] = v;
                    }
                }
                if (maxW[e.u] > 0) {
                    PQ.add(new Entry(e.u, maxMate[e.u], maxW[e.u]));
                }
                continue;
            }

            // map u to v
            map[e.u] = e.v;
            rmap[e.v] = e.u;

            // adjust W[u',v'] where u' and v' are neighbors of u and v
            for (int u1 : alist1[e.u]) { // neighbors of u
                if (map[u1] >= 0) {
                    continue;
                }
                boolean changed = false;
                for (int v1 : alist2[e.v]) { // neighbors of v
                    if (rmap[v1] >= 0 || W[u1][v1] <= 0) {
                        continue; // v1 has been mapped or u1 cannot be mapped to v1
                    }
                    W[u1][v1] += bonus; // add weights to W[u1][v1]
                    if (W[u1][v1] > maxW[u1]) {
                        maxW[u1] = W[u1][v1];
                        maxMate[u1] = v1;
                        changed = true;
                    }
                }
                if (changed) {
                    PQ.add(new Entry(u1, maxMate[u1], maxW[u1]));
                }
            }
            if(PQ.size()>maxSize)
            	maxSize=PQ.size();
        }
        //System.out.println(n1+" "+n2+" "+maxSize);
        return map;
    }
    
    /**
     * Find a graph mapping between two graphs for range query
     *
     * @param n1 number of vertices in G1
     * @param n2 number of vertices in G2
     * @param W initial weight matrix
     * @param alist1 adjacency list of G1
     * @param alist2 adjacency list of G2
     * @param bilist bi-list of the two graphs
     * @param bonus weights adding to neighboring pairs of a matched pair
     * @return vertex map from G1 to G2
     */
    public static int[] mapGraphsRangeQuery(LGraph g1, LGraph g2, int n1, int n2, double[][] W, int[][] alist1,
                                  int[][] alist2, int[][] bilist,double bonus, double userProbThreshold) {
    	
    	  // added
    		double probOfMappedG2 = 1.0;
    		
        // Insert initial entries into PQ
        double[] maxW = new double[n1];
        int[] maxMate = new int[n1];
        PriorityQueue<Entry> PQ = new PriorityQueue(n1 * 2);
        for (int i = 0; i < n1; i++) {
            maxW[i] = 0;
            for (int j = 0; j < n2; j++) {
                if (W[i][j] > maxW[i]) {
                    maxW[i] = W[i][j];
                    maxMate[i] = j;
                }
            }
            if (maxW[i] > 0) {
                PQ.add(new Entry(i, maxMate[i], maxW[i]));
            }
        }

        int[] map = new int[n1];
        int[] rmap = new int[n2];
        Arrays.fill(map, -1);
        Arrays.fill(rmap, -1);

        Map<String,Integer> discaredBasedOnProb = new HashMap<String,Integer>();
        
        // iteration of mapping in the order of weighted spanning
        int maxSize=PQ.size();
        while (!PQ.isEmpty()) {
            Entry e = PQ.poll();
            if (map[e.u] >= 0) { // u has been mapped
                continue;
            }
            if (rmap[e.v] >= 0) { // v has been mapped, find another v
            	//System.out.println("here");
                maxW[e.u] = 0;
                for (int v : bilist[e.u]) {
                    if (rmap[v] < 0 && W[e.u][v] > maxW[e.u]) {
                        maxW[e.u] = W[e.u][v];
                        maxMate[e.u] = v;
                    }
                }
                if (maxW[e.u] > 0) {
                    PQ.add(new Entry(e.u, maxMate[e.u], maxW[e.u]));
                }
                continue;
            }
            
            // check if by adding v, probability of mapped g2 falls below user threshold
            double tempProbOfMappedG2 = probOfMappedG2;
            for(int v1:alist2[e.v]){ // neighbours of v
            	if(rmap[v1]>=0){ // if neighbour of v is already mapped
          			for(int neigh : alist1[e.u]){
          				if(neigh==rmap[v1]) {
                		if(e.v<v1){
                			//System.out.println(e.v+","+v1+" "+g2.probMap.get(e.v+","+v1));
                			tempProbOfMappedG2 = tempProbOfMappedG2 * g2.probMap.get(e.v+","+v1);
                		}
                		else {
                			//System.out.println(v1+","+e.v+" "+g2.probMap.get(v1+","+e.v));
                			tempProbOfMappedG2 = tempProbOfMappedG2 * g2.probMap.get(v1+","+e.v);
                		}          					
          					break;
          				}
          			}
            	}
            }
            
            if(tempProbOfMappedG2 > userProbThreshold) { 	// map u to v
            	probOfMappedG2 = tempProbOfMappedG2;
            	System.out.println(e.u +" is mapped to "+e.v);
	            map[e.u] = e.v;
	            rmap[e.v] = e.u;
	
	            // adjust W[u',v'] where u' and v' are neighbors of u and v
	            for (int u1 : alist1[e.u]) { // neighbors of u
	                if (map[u1] >= 0) {
	                    continue;
	                }
	                boolean changed = false;
	                for (int v1 : alist2[e.v]) { // neighbors of v
	                    if (rmap[v1] >= 0 || W[u1][v1] <= 0) {
	                        continue; // v1 has been mapped or u1 cannot be mapped to v1
	                    }
	                    W[u1][v1] += bonus; // add weights to W[u1][v1]
	                    if (W[u1][v1] > maxW[u1]) {
	                        maxW[u1] = W[u1][v1];
	                        maxMate[u1] = v1;
	                        changed = true;
	                    }
	                }
	                if (changed) {
	                    PQ.add(new Entry(u1, maxMate[u1], maxW[u1]));
	                }
	            }
            }
            else { // current mapping would lead to probility of mapped G2 to fall below threshold, hence find better v
            	discaredBasedOnProb.put(e.u+","+e.v,1);
              maxW[e.u] = 0;
              for (int v : bilist[e.u]) {
                  if (rmap[v] < 0 && !discaredBasedOnProb.containsKey(e.u+","+v) && W[e.u][v] > maxW[e.u]) {
                      maxW[e.u] = W[e.u][v];
                      maxMate[e.u] = v;
                  }
              }
              if (maxW[e.u] > 0) {
                  PQ.add(new Entry(e.u, maxMate[e.u], maxW[e.u]));
              }
              continue;
            }
            
            if(PQ.size()>maxSize)
            	maxSize=PQ.size();
        }
        //System.out.println(n1+" "+n2+" "+maxSize);
        

        System.out.println("Probability of G2 after mapping = "+probOfMappedG2);
        System.out.println("G2's id:"+g2.getId());

        /*
        if(g2.id()==null){
          System.out.println("NBM Results for mapping query graph to internal Ctree node");
        }
        else {
          System.out.println("NBM Results for mapping query graph to graph #"+g2.id());
        }
        for (int i = 0; i < map.length; i++) {
            System.out.println(i+" "+map[i]);
        }
        */
        System.out.println("Probability of g2 after mapping (considering only mapped edges) = "+probOfMappedG2);
        

        return map;
    }

}
