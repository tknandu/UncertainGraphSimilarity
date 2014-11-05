import networkx as nx
from networkx.algorithms import isomorphism
import networkx.algorithms.isomorphism as iso
import random
import sys

def printGraph(G):
	#Print number of nodes
	print nx.number_of_nodes(G)
	#Print the node labels
	for vertex in G:
		print G.node[vertex]['label']
	print len(G.edges())
	for n in G.edges_iter():
		print str(n[0])+" "+str(n[1])+" "+str(G.edge[n[0]][n[1]]['prob'])

def writeGraphToFile(G,fileptr,graphId):
	#Print number of nodes
	fileptr.write("#"+str(graphId)+"\n")
	fileptr.write(str(nx.number_of_nodes(G))+"\n")
	#Print the node labels
	for vertex in G:
		fileptr.write(str(G.node[vertex]['label'])+"\n")
	fileptr.write(str(len(G.edges()))+"\n")
	for edge in G.edges():
		fileptr.write(str(edge[0])+" "+str(edge[1])+" "+str(1.0)+"\n")

def addProbabilities(G):
	for n in G.edges_iter():
		G.edge[n[0]][n[1]]['prob'] = random.random()

def addNodes(G,alpha,labelList):
	#For now alpha is even
	sampleSet=[]
	n=nx.number_of_nodes(G)
	for k in range(1,alpha/2+1):
		E=alpha-2*k
		if E>k*(n-1):
			continue
		#create a copy of the graph
		newGraph=G.copy()
		for i in range(k):		
			newNodeLabel=labelList[random.randint(0,len(labelList)-1)]
			newNodeMate=random.randint(0,n-1)
			newGraph.add_node(n+i)
			newGraph.node[n+i]['label']=newNodeLabel
			newGraph.add_edge(n+i,newNodeMate)
		for e in range(E):
			while True:
				randomNewNode=n+random.randint(0,k-1)
				randomOldNode=random.randint(0,n-1)
				if randomNewNode in newGraph[randomOldNode]:
					continue
				else:
					newGraph.add_edge(randomNewNode,randomOldNode)
					break
		sampleSet.append(newGraph)
	return sampleSet

def deleteEdges(G,alpha):
	sampleGraph=G.copy()
	n=nx.number_of_nodes(G)	
	for edgeDeletion in range(alpha):
		if nx.number_of_edges(sampleGraph)==0:
			print "Insufficient edges"
			break
		while True:
			i=random.randint(0,n-1)
			j=random.randint(0,n-1)
			if j not in sampleGraph[i]:
				continue
			else:
				sampleGraph.remove_edge(i,j)
				break
	addProbabilities(sampleGraph)
	return sampleGraph

def relabelNodes(G,alpha,labelList):
	sampleGraph=G.copy()
	n=nx.number_of_nodes(G)
	relabelled={}
	for relabelling in range(alpha):
		while True:
			randomNode=random.randint(0,n-1)
			if randomNode in relabelled:
				continue
			else:
				relabelled[randomNode]=1
				originalLabel=G.node[randomNode]['label']
				while True:
					randomLabel=labelList[random.randint(0,len(labelList)-1)]
					if randomLabel==originalLabel:
						continue
					else:
						sampleGraph.node[randomNode]['label']=randomLabel
						break
				break
	addProbabilities(sampleGraph)
	return sampleGraph

labelList=["A","B","C"]

Q=nx.read_gml(sys.argv[1])
writeGraphToFile(Q,open("queryGraph.txt","w"),10000)

evaluationFile=open("graphIdToSimMapping.txt","w")
sampleSet=[]
for editDistance in range(2,16):
	for sampleSize in range(5):
		sampleSet.append((deleteEdges(Q,editDistance),editDistance))
		sampleSet.append((deleteEdges(Q,editDistance),editDistance))
		sampleSet.append((relabelNodes(Q,editDistance,labelList),editDistance))
		sampleSet.append((relabelNodes(Q,editDistance,labelList),editDistance))
#for sampleCount in range(5):
#	sampleSet+=[(graphSample,2) for graphSample in addNodes(Q,2,labelList)]
#	sampleSet+=[(graphSample,3) for graphSample in addNodes(Q,3,labelList)]
#	sampleSet+=[(graphSample,4) for graphSample in addNodes(Q,4,labelList)]
#	sampleSet+=[(graphSample,5) for graphSample in addNodes(Q,5,labelList)]
#	sampleSet+=[(graphSample,6) for graphSample in addNodes(Q,6,labelList)]

for (sampleIndex,graphSample) in enumerate(sampleSet):
	if sampleIndex!=0:
		print ""
	print "#"+str(sampleIndex)
	evaluationFile.write("#"+str(sampleIndex)+" ")
	editDistance=graphSample[1]
	queryNodes=nx.number_of_nodes(Q)
	queryEdges=nx.number_of_edges(Q)
	similarity=queryNodes+queryEdges-editDistance
	evaluationFile.write(str(similarity)+"\n")
	printGraph(graphSample[0])
