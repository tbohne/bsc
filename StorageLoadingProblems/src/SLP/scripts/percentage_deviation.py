import sys

if __name__ == '__main__':

    time_limit = 5.0

    input = sys.stdin.readlines()
    optimal_costs = []

    for line in input:
        if "BinP," in line:
            runtime = float(line.split(",")[2])
            if runtime <= time_limit:
                optimal_costs.append(float(line.split(",")[3]))
            else:
                optimal_costs.append(-1.0)
                print("Problem: Not all optimal cost values have been determined.")

    sum_of_deviations = 0.0

    for line in input:
        if "3Cap," in line:
            instance_idx = line.split(",")[0].split("_")[3]
            three_cap_val = float(line.split(",")[3].strip())
            optimal_val = optimal_costs[int(instance_idx)]

            diff = abs(optimal_val - three_cap_val)
            percentage_deviation = round((diff / (optimal_val) * 100), 2)
            print("percentage deviation for instance " + instance_idx + ": " + str(percentage_deviation))
            sum_of_deviations += percentage_deviation

    num_of_instances = int(instance_idx) + 1
    print(num_of_instances)
    print("avg percentage deviation: " + str(round((sum_of_deviations / num_of_instances), 2)))
