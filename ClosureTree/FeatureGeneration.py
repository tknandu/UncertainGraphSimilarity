import networkx as nx
from networkx.algorithms import isomorphism
import networkx.algorithms.isomorphism as iso
import random

#Checks if the sets of vertex labels are same, for two edges
def checkTupleEquality(tuple_a,tuple_b):
	if tuple_a[0]==tuple_b[0] and tuple_a[1]==tuple_b[1]:
		return True
	elif tuple_a[0]==tuple_b[1] and tuple_a[1]==tuple_b[0]:
		return True
	else:
		return False

def nodeMatchWithVertexLabels(dictA,dictB):
	#Debug Statement to see if function is entered
	#print "blah"
	return checkTupleEquality(dictA['label'],dictB['label'])

def findCommonNode(tuple_a,tuple_b):
	for i in tuple_a:
		if i in tuple_b:
			return i

def generateLabeledLineGraph(G):
	lineGraph=nx.line_graph(G)
	for vertexIndex in lineGraph:
		lineGraph.node[vertexIndex]['label']=(G.node[vertexIndex[0]]['label'],G.node[vertexIndex[1]]['label'])
	for n,nbrsdict in lineGraph.adjacency_iter():
		for nbr,eattr in nbrsdict.items():
			lineGraph.edge[n][nbr]['label']=G.node[findCommonNode(n,nbr)]['label']
	return lineGraph

em = iso.categorical_edge_match('label', 'miss')

def checkSubGraphIsomorphismWithLabels(G1,G2):
	if len(G1.nodes())==2 and len(G1.edges())==1:
		loneEdge=G1.edges()[0]
		loneEdgetuple=(G1.node[loneEdge[0]]['label'],G1.node[loneEdge[1]]['label'])
		foundMatch=False
		for edge in G2.edges():
			edgeLabel=(G2.node[edge[0]]['label'],G2.node[edge[1]]['label'])
			if not foundMatch:
				foundMatch=checkTupleEquality(edgeLabel,loneEdgetuple)
		print foundMatch
		return foundMatch
	#Returns true if G1 is a subGraph of G2
	lineGraphG1=generateLabeledLineGraph(G1)
	lineGraphG2=generateLabeledLineGraph(G2)
	GM=isomorphism.GraphMatcher(lineGraphG2,lineGraphG1,node_match=nodeMatchWithVertexLabels,edge_match=em)
	#GM=isomorphism.GraphMatcher(lineGraphG2,lineGraphG1,node_match=nodeMatchWithVertexLabels)
	return GM.subgraph_is_isomorphic()

def generateLabelledVariants(G,labelList):
	seedSet=[G,]
	for vertex in G:
		newSeedSet=[]
		for labelName in labelList:
			for seedGraph in seedSet:
				#print seedGraph.nodes()
				#print "Entered"
				newGraph=seedGraph.copy()
				#print newGraph.nodes()
				newGraph.node[vertex]['label']=labelName
				#print newGraph.node[vertex]
				newSeedSet.append(newGraph)
		#print newSeedSet
		seedSet=newSeedSet
	for seedGraph in seedSet:
		representation=[]
		for vertex in seedGraph:
			representation.append(seedGraph.node[vertex])
	uniqueSeedSet=[]
	for seedGraph in seedSet:
		alreadyPresent=False
		for uniqueSeedGraph in uniqueSeedSet:
			if checkSubGraphIsomorphismWithLabels(seedGraph,uniqueSeedGraph):
				alreadyPresent=True
				break
		if not alreadyPresent:
			uniqueSeedSet.append(seedGraph)
	return uniqueSeedSet
def getK3():
	f1=nx.Graph()
	f1.add_node(1)
	f1.add_node(2)
	f1.add_node(3)
	f1.add_edge(1,2)
	f1.add_edge(2,3)
	f1.add_edge(1,3)
	return f1

def generatePLength(l):
	f=nx.Graph()
	for i in range(1,l+1):
		f.add_node(i)
	for i in range(1,l):
		f.add_edge(i,i+1)
	return f

def printGraph(G):
	#Print number of nodes
	print nx.number_of_nodes(G)
	#Print the node labels
	for vertex in G:
		print G.node[vertex]['label']
	print len(G.edges())
	for edge in G.edges():
		print str(edge[0])+" "+str(edge[1])+" "+str(1.0)

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

#features=[]
#features=features+generateLabelledVariants(getK3(),labelList)
#features+=generateLabelledVariants(generatePLength(3),labelList)
#print len(features)

labelList=["A","B","C"]
"""
Q=nx.Graph()
Q.add_node(0)
Q.add_node(1)
Q.add_node(2)
Q.add_node(3)
Q.add_node(4)
Q.add_edge(1,2)
Q.add_edge(2,4)
Q.add_edge(4,3)
Q.add_edge(3,1)
Q.add_edge(0,1)
Q.add_edge(0,2)
Q.add_edge(0,3)
Q.add_edge(0,4)
Q.node[0]['label']="A"
Q.node[1]['label']="B"
Q.node[2]['label']="B"
Q.node[3]['label']="C"
Q.node[4]['label']="C"
"""
import sys
Q=nx.read_gml(sys.argv[1])
writeGraphToFile(Q,open("evaluationQueryGraph.txt","w"),10000)
#G1=addNodeAndEdge(Q)
#G2=addEdge(Q)
#G3=deleteEdge(Q)
#G4=deleteEdge(Q)
#G5=relabelNode(Q)
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
	return sampleGraph

evaluationFile=open("evaluation.txt","w")
sampleSet=[]
for editDistance in range(2,16):
	for sampleSize in range(5):
		sampleSet.append((deleteEdges(Q,editDistance),editDistance))
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
