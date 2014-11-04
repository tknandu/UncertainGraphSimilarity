evaluationFile=open("evaluation.txt")
outputFile=open("output.txt")
simThreshold=106
probThreshold=0.0
groundPositives=[]
groundNegatives=[]
for line in evaluationFile:
	words=line.split()
	graphId=int(words[0][1:])
	graphSimilarity=int(words[1])
	if graphSimilarity>=simThreshold:
		groundPositives.append(graphId)
	else:
		groundNegatives.append(graphId)

truePositives=[]
positives=[]
for lineNumber,line in enumerate(outputFile):
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
print truePositives
print groundPositives
print precision
print recall



