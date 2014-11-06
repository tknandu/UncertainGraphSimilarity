from sys import argv

script, sim, prob = argv

graphToSimMapping=open("graphIdToSimMapping.txt")
graphToProbMapping=open("graphIdToProbMapping.txt")
answerSet=open("answerSet.txt")

simThreshold = float(sim) 
probThreshold= float(prob)

groundPositives=[]
groundNegatives=[]

graphIdToProbMap = {}
for line in graphToProbMapping:
	words=line.split()
	graphId=int(words[1])
	graphProb=float(words[3])
	graphIdToProbMap[graphId] = graphProb

for line in graphToSimMapping:
	words=line.split()
	graphId=int(words[0][1:])
	graphSimilarity=float(words[1])
	if graphSimilarity>=simThreshold and graphIdToProbMap[graphId]>=probThreshold:
		groundPositives.append(graphId)
	else:
		groundNegatives.append(graphId)

truePositives=[]
positives=[]
for lineNumber,line in enumerate(answerSet):
	if lineNumber==0:
		continue
	words=line.split()
	graphId=int(words[1])
	if graphId in groundPositives:
		truePositives.append(graphId)
	else:
		print graphId
	positives.append(graphId)

precision=(len(truePositives)+0.0)/(len(positives)+0.0)
recall=(len(truePositives)+0.0)/(len(groundPositives)+0.0)

print 'No of positives correctly returned: ', len(truePositives)
print 'No of true ground positives: ', len(groundPositives)
print 'No of positives returned: ', len(positives)
print 'Precision: ', precision
print 'Recall: ', recall
