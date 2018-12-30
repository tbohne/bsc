library(ggplot2)

Input <- read.csv(file="../../../res/solutions/v6/solutions.csv", header = TRUE, sep=",")

MipEntries <- subset(Input, solver=="BinP" | solver=="3Idx" | solver=="3CapRec" | solver=="3CapPerm")

plotPointsPre <- ggplot(data = MipEntries, aes(x=time, y=instance, color=solver, group=solver, xlab="time (s)", ylab="instance")) + geom_point()

ggsave(plotPointsPre, file="test.png", width=10, height=25)
