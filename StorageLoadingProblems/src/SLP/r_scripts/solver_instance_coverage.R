library(ggplot2)
library(plyr)

input <- read.csv(file="../../../res/solutions/solutions.csv", header = TRUE, sep=",")

# gg <- ggplot(data=input, aes(y=0, yend=100))
# gg <- gg + geom_segment(aes(x=solver, xend=0, color="3Idx"), size=9)
# gg <- gg + xlab("solver")+ylab("instance coverage (%)")+labs(color='Presolver')
# ggsave(gg, file="test.png", width=10, height=17)

PERCENTAGE <- c(
    count(input, vars = "solver")[1, "freq"],
    count(input, vars = "solver")[2, "freq"],
    count(input, vars = "solver")[3, "freq"],
    count(input, vars = "solver")[4, "freq"]
)

SOLVER <- c("3CapPerm","3CapRec","3Idx","BinP")

png(file = "test.png")

# plot the bar chart
barplot(PERCENTAGE,names.arg=SOLVER,xlab="Solver",ylab="Instace Coverage (%)",col="blue",border="red")

# save the file
dev.off()
