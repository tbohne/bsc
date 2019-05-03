library(ggplot2)
library(plyr)

input <- read.csv(file = "../../../../res/solutions/solutions.csv", header = TRUE, sep = ",")
solverEntries <- subset(input, solver == "BinP" | solver == "3Idx" | solver == "2Cap")
plotPointsPre <- ggplot(data = solverEntries, aes(x = time, y = instance, color = solver, group = solver)) + geom_point() + xlab("time (s)") + ylab("instances")

##############################################################################
binpData <- subset(input, solver == "BinP")
binpRuntime <- subset(binpData, select = c(time))
paste("avg runtime of BinP: ", mean(as.numeric(as.character(binpRuntime[["time"]]))))

threeidxData <- subset(input, solver == "3Idx")
threeidxRuntime <- subset(threeidxData, select = c(time))
paste("avg runtime of 3Idx: ", mean(as.numeric(as.character(threeidxRuntime[["time"]]))))

twoCapData <- subset(input, solver == "2Cap")
twoCapRuntime <- subset(twoCapData, select = c(time))
paste("avg runtime of 2Cap: ", mean(as.numeric(as.character(twoCapRuntime[["time"]]))))
##############################################################################

ggsave(plotPointsPre, file="solver_instance_time.png", width = 8, height = 4)
