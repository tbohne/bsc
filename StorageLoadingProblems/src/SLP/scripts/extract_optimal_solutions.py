import sys
import os

def get_time_limit():
    for filename in os.listdir("../../../res/solutions/"):
        if "00.txt" in filename:
            instance_file = open("../../../res/solutions/" + filename, "r")
            break

    lines = instance_file.readlines()
    for line in lines:
        if "time limit:" in line:
            time_limit = int(line.split(" ")[2].strip())
            break
    instance_file.close()
    return time_limit

def get_runtime(line):
    return float(line.split(",")[2])

def get_value(line):
    return float(line.split(",")[3].strip())

def write_results(optimal_costs):
    f = open("optimal_costs.txt", "a")
    for costs in optimal_costs:
        f.write(str(costs) + "\n")
    f.close()

if __name__ == '__main__':

    time_limit = get_time_limit()

    solutions_file = open("../../../res/solutions/solutions.csv", "r")
    lines = solutions_file.readlines()
    solutions_file.close()

    optimal_costs = []

    for line in lines:
        if "BinP," in line:
            runtime = get_runtime(line)

            if runtime <= time_limit:
                optimal_costs.append(get_value(line))
            else:
                print("Problem: Not all optimal cost values have been determined.")
                exit()

    write_results(optimal_costs)
