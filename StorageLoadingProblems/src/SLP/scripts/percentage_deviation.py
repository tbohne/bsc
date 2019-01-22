import sys
import os

def get_instance_idx(line):
    return line.split(",")[0].split("_")[3]

def get_value(line):
    return float(line.split(",")[3].strip())

def get_percentage_deviation(otimal_val, curr_val):
    diff = abs(optimal_val - curr_val)
    return round((diff / (optimal_val) * 100), 2)

def get_runtime(line):
    return float(line.split(",")[2])

def read_optimal_costs():
    costs_file = open("optimal_costs.txt", "r")
    lines = costs_file.readlines()
    costs_file.close()

    optimal_costs = []
    for line in lines:
        if not "time limit" in line:
            optimal_costs.append(float(line.strip()))
    return optimal_costs

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

if __name__ == '__main__':

    solutions_file = open("../../../res/solutions/solutions.csv", "r")
    lines = solutions_file.readlines()
    optimal_costs = read_optimal_costs()
    time_limit = get_time_limit()

    optimally_solved_by_bin_packing = 0
    optimally_solved_by_three_index = 0

    for line in lines:

        if "BinP," in line or "3Idx," in line:
            runtime = get_runtime(line)

        if "BinP," in line:
            if runtime <= time_limit:
                optimally_solved_by_bin_packing += 1
        elif "3Idx," in line:
            if runtime <= time_limit:
                optimally_solved_by_three_index += 1

    sum_of_deviations_bin_packing = 0.0
    sum_of_deviations_three_idx = 0.0
    sum_of_deviations_three_cap = 0.0

    bin_packing_solutions = 0
    three_index_solutions = 0
    three_cap_solutions = 0

    for line in lines:

        if "BinP," in line or "3Idx," in line or "3Cap," in line:
            instance_idx = get_instance_idx(line)
            curr_val = get_value(line)
            optimal_val = optimal_costs[int(instance_idx)]
            percentage_deviation = get_percentage_deviation(float(optimal_val), curr_val)

        if "BinP," in line:
            sum_of_deviations_bin_packing += percentage_deviation
            bin_packing_solutions += 1
        elif "3Idx," in line:
            sum_of_deviations_three_idx += percentage_deviation
            three_index_solutions += 1
        elif "3Cap," in line:
            sum_of_deviations_three_cap += percentage_deviation
            three_cap_solutions += 1

    num_of_instances = int(instance_idx) + 1
    print("avg percentage deviation BinP: " + str(round((sum_of_deviations_bin_packing / bin_packing_solutions), 2)))
    print("avg percentage deviation 3Idx: " + str(round((sum_of_deviations_three_idx / three_index_solutions), 2)))
    print("avg percentage deviation 3Cap: " + str(round((sum_of_deviations_three_cap / three_cap_solutions), 2)))
    print()
    print("optimally solved by BinP: " + str((optimally_solved_by_bin_packing / num_of_instances) * 100) + " %")
    print("optimally solved by 3Idx: " + str((optimally_solved_by_three_index / num_of_instances) * 100) + " %")
