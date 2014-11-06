from sys import argv

script, withoutProb, withProb = argv

withoutProbResults = []
withProbResults = []

for lineNumber,line in enumerate(open(withoutProb)):
	if lineNumber==0:
		continue
	words=line.split()
	graphId=int(words[1])
	withoutProbResults.append(graphId)

for lineNumber,line in enumerate(open(withProb)):
	if lineNumber==0:
		continue
	words=line.split()
	graphId=int(words[1])
	withProbResults.append(graphId)

print 'No of common graphs: ',len(set(withProbResults).intersection(set(withoutProbResults)))