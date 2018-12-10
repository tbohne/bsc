package SLP;

import java.io.*;

public class InstanceWriter {

    public static void writeInstance(String filename, Instance instance) {

        File file = new File(filename);

        try {
            FileOutputStream fos = new FileOutputStream(file);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

            bw.write(instance.getItems().length + "\n");
            bw.write(instance.getStacks().length + "\n");
            bw.write(instance.getStackCapacity() + "\n");
            bw.newLine();
            for (int i = 0; i < instance.getStackingConstraints().length; i++) {
                for (int j = 0; j < instance.getStackingConstraints()[0].length; j++) {
                    bw.write(instance.getStackingConstraints()[i][j] + " ");
                }
                bw.newLine();
            }
            bw.newLine();

            for (int i = 0; i < instance.getCosts().length; i++) {
                for (int j = 0; j < instance.getCosts()[0].length; j++) {
                    bw.write(instance.getCosts()[i][j] + " ");
                }
                bw.newLine();
            }

            bw.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeConfig(
            String filename,
            int numOfInstances,
            int numOfItems,
            int stackCap,
            int additionalStackPercentage,
            float chanceForOneInStackingConstraints,
            int costsInclusiveLowerBound,
            int costsExclusiveUpperBound
    ) {

        File file = new File(filename);

        try {
            FileOutputStream fos = new FileOutputStream(file);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

            bw.write("---------- INSTANCE CONFIGURATION ----------\n");

            bw.write("NUMBER_OF_INSTANCES: " + numOfInstances + "\n");
            bw.write("NUMBER_OF_ITEMS: " + numOfItems + "\n");
            bw.write("STACK_CAPACITY: " + stackCap + "\n");
            bw.write("ADDITIONAL_STACK_PERCENTAGE: " + additionalStackPercentage + "\n");
            bw.write("CHANCE_FOR_ONE_IN_STACKING_CONSTRAINTS: " + chanceForOneInStackingConstraints + "\n");
            bw.write("COSTS_INCLUSIVE_LOWER_BOUND: " + costsInclusiveLowerBound + "\n");
            bw.write("COSTS_EXCLUSIVE_UPPER_BOUND: " + costsExclusiveUpperBound + "\n");

            bw.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
