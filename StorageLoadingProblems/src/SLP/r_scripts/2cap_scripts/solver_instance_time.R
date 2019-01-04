library(ggplot2)

input <- read.csv(file = "../../../../res/solutions/solutions.csv", header = TRUE, sep = ",")
solverEntries <- subset(input, solver == "BinP" | solver == "3Idx" | solver == "2Cap")
plotPointsPre <- ggplot(data = solverEntries, aes(x = time, y = instance, color = solver, group = solver)) + geom_point() + xlab("time (s)") + ylab("instance")

ggsave(plotPointsPre, file="solver_instance_time.png", width=8, height=4)
