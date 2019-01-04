library(ggplot2)

input <- read.csv(file = "../../../../res/solutions/v8/solutions.csv", header = TRUE, sep = ",")
solverEntries <- subset(input, solver == "BinP" | solver == "3Idx" | solver == "3CapRec" | solver == "3CapPerm")
plotPointsPre <- ggplot(data = solverEntries, aes(x = val, y = instance, color = solver, group = solver)) + geom_point() + xlab("costs") + ylab("instance")

ggsave(plotPointsPre, file="solver_instance_cost.png", width = 8, height = 8)
