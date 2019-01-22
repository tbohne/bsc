library(ggplot2)

input <- read.csv(file = "../../../../res/solutions/solutions.csv", header = TRUE, sep = ",")
solverEntries <- subset(input, solver == "BinP" | solver == "3Idx" | solver == "2Cap")
plotPointsPre <- ggplot(data = solverEntries, aes(x = time, y = val, color = solver, group = solver, xlab = "time (s)", ylab = "costs")) + geom_point()

ggsave(plotPointsPre, file="test.png", width=10, height=25)
