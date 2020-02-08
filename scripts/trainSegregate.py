import csv

trainno = []
trainname = []

with open("../datasets/trains.csv","r") as readFile:
	file = csv.reader(readFile)
	for row in file:
		trainno.append(row[0])
		trainname.append(row[1])

for i in range(0,len(trainno)):
	with open("../datasets/combinedtrains.csv","a") as writeFile:
		file = csv.writer(writeFile)
		file.writerow([trainno[i]]+[trainname[i]])