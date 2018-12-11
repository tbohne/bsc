library(ggplot2)

Input <- read.csv(file="../../../res/solutions/v6/solutions.csv", header = TRUE, sep=",")

MipEntries <- subset(Input, solver=="BinP" | solver=="3Idx" | solver=="ConstHeu")

plotPointsPre <- ggplot(data = MipEntries, aes(x=time, y=val, color=solver, group=solver, xlab="time (s)", ylab="costs")) + geom_point()

ggsave(plotPointsPre, file="test.png", width=10, height=25)
