import networkx as nx
import random

n = 30 # number of nodes
prob_edge = 0.7 # prob of each edge

labelList = ["A","B","C"]

G_er = nx.erdos_renyi_graph (n, prob_edge)

for i in range(0,n):
	G_er.node[i]['label'] = labelList[random.randint(0,2)]

print 'Nodes: ',G_er.nodes(data=True)
print 'Edges: ',G_er.edges()

# Write erdos_renyi_graph
nx.write_gml(G_er,"graph.gml")

# Read graph
G=nx.read_gml('graph.gml')

print 'Nodes: ',G_er.nodes(data=True)
print 'Edges: ',G_er.edges()