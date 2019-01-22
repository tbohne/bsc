import sys

def get_instance_idx(line):
    return line.split(",")[0].split("_")[3]

def get_value(line):
    return float(line.split(",")[3].strip())

def get_percentage_deviation(otimal_val, curr_val):
    diff = abs(optimal_val - curr_val)
    return round((diff / (optimal_val) * 100), 2)

def get_runtime(line):
    return float(line.split(",")[2])

if __name__ == '__main__':

    time_limit = 5.0

    input = sys.stdin.readlines()
    optimal_costs = []

    optimally_solved_by_bin_packing = 0
    optimally_solved_by_three_index = 0

    for line in input:

        if "BinP," in line or "3Idx," in line:
            runtime = get_runtime(line)

        if "BinP," in line:
            if runtime <= time_limit:
                optimal_costs.append(get_value(line))
                optimally_solved_by_bin_packing += 1
            else:
                optimal_costs.append(-1.0)
                print("Problem: Not all optimal cost values have been determined.")
        elif "3Idx," in line:
            if runtime <= time_limit:
                optimally_solved_by_three_index += 1

    sum_of_deviations_bin_packing = 0.0
    sum_of_deviations_three_idx = 0.0
    sum_of_deviations_three_cap = 0.0

    for line in input:

        if "BinP," in line or "3Idx," in line or "3Cap," in line:
            instance_idx = get_instance_idx(line)
            curr_val = get_value(line)
            optimal_val = optimal_costs[int(instance_idx)]
            percentage_deviation = get_percentage_deviation(optimal_val, curr_val)

        if "BinP," in line:
            sum_of_deviations_bin_packing += percentage_deviation
        elif "3Idx," in line:
            sum_of_deviations_three_idx += percentage_deviation
        elif "3Cap," in line:
            sum_of_deviations_three_cap += percentage_deviation

    num_of_instances = int(instance_idx) + 1
    print("avg percentage deviation BinP: " + str(round((sum_of_deviations_bin_packing / num_of_instances), 2)))
    print("avg percentage deviation 3Idx: " + str(round((sum_of_deviations_three_idx / num_of_instances), 2)))
    print("avg percentage deviation 3Cap: " + str(round((sum_of_deviations_three_cap / num_of_instances), 2)))
    print()
    print("optimally solved by BinP: " + str((optimally_solved_by_bin_packing / num_of_instances) * 100) + " %")
    print("optimally solved by 3Idx: " + str((optimally_solved_by_three_index / num_of_instances) * 100) + " %")
