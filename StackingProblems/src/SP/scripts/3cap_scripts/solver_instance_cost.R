library(ggplot2)

input <- read.csv(file = "../../../../res/solutions/solutions.csv", header = TRUE, sep = ",")
solverEntries <- subset(input, solver == "BinP" | solver == "3Idx" | solver == "3Cap" | solver == "LB")
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

lowerBoundData <- subset(input, solver == "LB")
lowerBoundCosts <- subset(lowerBoundData, select = c(val))
paste("avg LB: ", mean(lowerBoundCosts[["val"]]))
##############################################################################

finalPlot <- plotPointsPre + scale_color_manual(values=c("#fa9f27", "#5428ff", "#f5503b", "#28bd5a"))
ggsave(finalPlot, file="solver_instance_cost.png", width = 6, height = 4)
