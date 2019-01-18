library(ggplot2)

input <- read.csv(file = "../../../../res/solutions/solutions.csv", header = TRUE, sep = ",")
solverEntries <- subset(input, solver == "BinP" | solver == "3Idx" | solver == "3Cap" | solver == "3CapPerm" | solver == "3CapRec")
plotPointsPre <- ggplot(data = solverEntries, aes(x = time, y = instance, color = solver, group = solver)) + geom_point() + xlab("time (s)") + ylab("instance")

##############################################################################
binpData <- subset(input, solver == "BinP")
binpRuntime <- subset(binpData, select = c(time))
paste("avg runtime of BinP: ", mean(binpRuntime[["time"]]))

threeidxData <- subset(input, solver == "3Idx")
threeidxRuntime <- subset(threeidxData, select = c(time))
paste("avg runtime of 3Idx: ", mean(threeidxRuntime[["time"]]))

threeCapData <- subset(input, solver == "3Cap")
threeCapRuntime <- subset(threeCapData, select = c(time))
paste("avg runtime of 3Cap: ", mean(threeCapRuntime[["time"]]))

threeCapPermData <- subset(input, solver == "3CapPerm")
threeCapPermRuntime <- subset(threeCapPermData, select = c(time))
paste("avg runtime of 3CapPerm: ", mean(threeCapPermRuntime[["time"]]))

threeCapRecData <- subset(input, solver == "3CapRec")
threeCapRecRuntime <- subset(threeCapRecData, select = c(time))
paste("avg runtime of 3CapRec: ", mean(threeCapRecRuntime[["time"]]))
##############################################################################

ggsave(plotPointsPre, file="solver_instance_time.png", width=8, height=5)
