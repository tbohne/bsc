library(ggplot2)
library(plyr)

input <- read.csv(file = "../../../res/solutions/solutions.csv", header = TRUE, sep = ",")

config_file <- read.csv(file = "../../../res/instances/instance_set_config.csv", header = TRUE, sep = ",")

numOfInstances <- config_file["numOfInstances"]

# gg <- ggplot(data=input, aes(y=0, yend=100))
# gg <- gg + geom_segment(aes(x=solver, xend=0, color="3Idx"), size=9)
# gg <- gg + xlab("solver")+ylab("instance coverage (%)")+labs(color='Presolver')
# ggsave(gg, file="test.png", width=10, height=17)

# PERCENTAGE <- c(
#     count(input, vars = "solver")[1, "freq"],
#     count(input, vars = "solver")[2, "freq"],
#     count(input, vars = "solver")[3, "freq"],
#     count(input, vars = "solver")[4, "freq"]
# )

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

png(file = "test.png")

# # plot the bar chart
barplot(SOLUTIONS, names.arg = SOLVER, xlab = "Solver", ylab = "Instace Coverage (%)", col = "blue", border = "red")

# save the file
dev.off()
