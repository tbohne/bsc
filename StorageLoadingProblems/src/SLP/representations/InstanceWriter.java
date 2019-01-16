package SLP.representations;

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

            for (int i = 0; i < instance.getStackConstraints().length; i++) {
                for (int j = 0; j < instance.getStackConstraints()[0].length; j++) {
                    bw.write(instance.getStackConstraints()[i][j] + " ");
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
            bw.newLine();

            for (Coordinates coords: instance.getItemPositions()) {
                bw.write(coords + " ");
            }
            bw.newLine();

            for (Coordinates coords: instance.getStackPositions()) {
                bw.write(coords + " ");
            }
            bw.newLine();

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
            float chanceForOneInStackConstraints,
            int costsInclusiveLowerBound,
            int costsExclusiveUpperBound,
            boolean usingStackingConstraintGenerationApproachOne,
            int itemLengthLB,
            int itemLenghUB,
            int itemWidthLB,
            int itemWidthUB
    ) {

        File file = new File(filename);

        try {
            FileOutputStream fos = new FileOutputStream(file);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

            bw.write("numOfInstances,numOfItems,stackCapacity,additionalStackPercentage,chanceForOneInStackingConstraints,"
                    + "chanceForOneInStackConstraints,costsInclusiveLB,costsExclusiveUB,"
                    + "usingStackingConstraintGenerationApproachOne,itemLengthLB,itemLengthUB,itemWidthLB,itemWidthUB\n");
            bw.write(numOfInstances + "," + numOfItems + "," + stackCap + "," + additionalStackPercentage + ","
                + chanceForOneInStackingConstraints + "," + chanceForOneInStackConstraints + "," + costsInclusiveLowerBound + "," + costsExclusiveUpperBound + ","
                + usingStackingConstraintGenerationApproachOne + "," + itemLengthLB + "," + itemLenghUB + "," + itemWidthLB + "," + itemWidthUB);

            bw.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
