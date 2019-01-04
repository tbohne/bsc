library(ggplot2)
library(plyr)

input <- read.csv(file = "../../../../res/solutions/v8/solutions.csv", header = TRUE, sep = ",")
configFile <- read.csv(file = "../../../../res/instances/v8/instance_set_config.csv", header = TRUE, sep = ",")
numOfInstances <- configFile["numOfInstances"]

print("########## THE ACTUAL RESULTS ############")
print(count(input, vars = "solver"))
print("##########################################")

paste("BinP: ", sum(input == "BinP"), " solutions")
paste("3Idx: ", sum(input == "3Idx"), " solutions")
paste("3CapPerm: ", sum(input == "3CapPerm"), " solutions")
paste("3CapRec: ", sum(input == "3CapRec"), " solutions")

SOLUTIONS <- c(
    sum(input == "BinP") / as.integer(numOfInstances) * 100,
    sum(input == "3Idx") / as.integer(numOfInstances) * 100,
    sum(input == "3CapPerm") / as.integer(numOfInstances) * 100,
    sum(input == "3CapRec") / as.integer(numOfInstances) * 100
)
SOLVER <- c("BinP", "3Idx", "3CapPerm", "3CapRec")

png(file = "solver_instance_coverage.png")

# plot the bar chart
barplot(SOLUTIONS, names.arg = SOLVER, xlab = "solver", ylab = "instace coverage (%)", col = "red", border = "black")

# save the file
dev.off()
