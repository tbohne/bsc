library(ggplot2)

Input <- read.csv(file="../../res/solutions/solutions.csv", header = TRUE, sep=",")

MipEntries <- subset(Input, mip=="BinP" | mip=="3Idx")

plotPointsPre <- ggplot(data = MipEntries, aes(x=time, y=instance, color=mip, group=mip, xlab="time (s)", ylab="instance")) + geom_point()

ggsave(plotPointsPre, file="test.png", width=10, height=25)
