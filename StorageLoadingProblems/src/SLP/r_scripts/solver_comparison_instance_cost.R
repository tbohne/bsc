library(ggplot2)

Input <- read.csv(file="../../../res/solutions/v6/solutions.csv", header = TRUE, sep=",")

MipEntries <- subset(Input, solver=="BinP" | solver=="3Idx" | solver=="3CapRec" | solver=="3CapPerm")

plotPointsPre <- ggplot(data = MipEntries, aes(x=val, y=instance, color=solver, group=solver, xlab="val", ylab="instance")) + geom_point()

ggsave(plotPointsPre, file="test.png", width=10, height=25)
