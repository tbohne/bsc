library(ggplot2)

input <- read.csv(file = "../../../../res/solutions/solutions.csv", header = TRUE, sep = ",")
solverEntries <- subset(input, solver == "BinP" | solver == "3Idx" | solver == "3Cap" | solver == "3CapRec" | solver == "3CapPerm")
plotPointsPre <- ggplot(data = solverEntries, aes(x = val, y = instance, color = solver, group = solver)) + geom_point() + xlab("costs") + ylab("instance")

##############################################################################
binpData <- subset(input, solver == "BinP")
binpCosts <- subset(binpData, select = c(val))
paste("avg costs of BinP: ", mean(binpCosts[["val"]]))

threeidxData <- subset(input, solver == "3Idx")
threeidxCosts <- subset(threeidxData, select = c(val))
paste("avg costs of 3Idx: ", mean(threeidxCosts[["val"]]))

threeCapData <- subset(input, solver == "3Cap")
threeCapCosts <- subset(threeCapData, select = c(val))
paste("avg costs of 3Cap: ", mean(threeCapCosts[["val"]]))

threeCapPermData <- subset(input, solver == "3CapPerm")
threeCapPermCosts <- subset(threeCapPermData, select = c(val))
paste("avg costs of 3CapPerm: ", mean(threeCapPermCosts[["val"]]))

threeCapRecData <- subset(input, solver == "3CapRec")
threeCapRecCosts <- subset(threeCapRecData, select = c(val))
paste("avg costs of 3CapRec: ", mean(threeCapRecCosts[["val"]]))
##############################################################################

ggsave(plotPointsPre, file="solver_instance_cost.png", width = 8, height = 5)
