library(ggplot2)

input <- read.csv(file = "../../../../res/solutions/solutions.csv", header = TRUE, sep = ",")
solverEntries <- subset(input, solver == "BinP" | solver == "3Idx" | solver == "2Cap")
plotPointsPre <- ggplot(data = solverEntries, aes(x = val, y = instance, color = solver, group = solver)) + geom_point() + xlab("costs") + ylab("instance")

##############################################################################
binpData <- subset(input, solver == "BinP")
binpCosts <- subset(binpData, select = c(val))
paste("avg costs of BinP: ", mean(binpCosts[["val"]]))

threeidxData <- subset(input, solver == "3Idx")
threeidxCosts <- subset(threeidxData, select = c(val))
paste("avg costs of 3Idx: ", mean(threeidxCosts[["val"]]))

twoCapData <- subset(input, solver == "2Cap")
twoCapCosts <- subset(twoCapData, select = c(val))
paste("avg costs of 2Cap: ", mean(twoCapCosts[["val"]]))
##############################################################################

finalPlot <- plotPointsPre + scale_x_continuous(limits = c(6545.0, 6635.0))
ggsave(finalPlot, file = "solver_instance_cost.png", width = 6, height = 4)
