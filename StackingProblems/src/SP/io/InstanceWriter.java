package SP.io;

import SP.representations.Instance;
import SP.representations.Position;

import java.io.*;

/**
 * Provides functionalities to write instances of stacking problems to the file system.
 *
 * @author Tim Bohne
 */
public class InstanceWriter {

    /**
     * Writes the stacking constraints to the file using the specified buffered writer.
     *
     * @param instance - the instance that is written to the file
     * @param bw       - the buffered writer pointing to the part of the file to write to
     * @throws IOException
     */
    private static void writeStackingConstraints(Instance instance, BufferedWriter bw) throws IOException {
        for (int i = 0; i < instance.getStackingConstraints().length; i++) {
            for (int j = 0; j < instance.getStackingConstraints()[0].length; j++) {
                bw.write(instance.getStackingConstraints()[i][j] + " ");
            }
            bw.newLine();
        }
        bw.newLine();
    }

    /**
     * Writes the costs to the file using the specified buffered writer.
     *
     * @param instance - the instance that is written to the file
     * @param bw       - the buffered writer pointing to the part of the file to write to
     * @throws IOException
     */
    private static void writeCosts(Instance instance, BufferedWriter bw) throws IOException {
        for (int i = 0; i < instance.getCosts().length; i++) {
            for (int j = 0; j < instance.getCosts()[0].length; j++) {
                bw.write(String.format("%.2f", instance.getCosts()[i][j]) + " ");
            }
            bw.newLine();
        }
        bw.newLine();
    }

    /**
     * Writes the item positions to the file using the specified buffered writer.
     *
     * @param instance - the instance that is written to the file
     * @param bw       - the buffered writer pointing to the part of the file to write to
     * @throws IOException
     */
    private static void writeItemPositions(Instance instance, BufferedWriter bw) throws IOException {
        for (Position itemPos : instance.getItemPositions()) {
            bw.write(itemPos + " ");
        }
        bw.newLine();
    }

    /**
     * Writes the stack positions to the file using the specified buffered writer.
     *
     * @param instance - the instance that is written to the file
     * @param bw       - the buffered writer pointing to the part of the file to write to
     * @throws IOException
     */
    private static void writeStackPositions(Instance instance, BufferedWriter bw) throws IOException {
        for (Position stackPos : instance.getStackPositions()) {
            bw.write(stackPos + " ");
        }
        bw.newLine();
    }

    /**
     * Writes the specified instance to the file with the specified name.
     *
     * @param filename - the file the instance gets written to
     * @param instance - the instance to be written to the file
     */
    public static void writeInstance(String filename, Instance instance) {

        File file = new File(filename);

        try {
            FileOutputStream fos = new FileOutputStream(file);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

            bw.write(instance.getItems().length + "\n");
            bw.write(instance.getStacks().length + "\n");
            bw.write(instance.getStackCapacity() + "\n");
            bw.newLine();

            writeStackingConstraints(instance, bw);
            writeCosts(instance, bw);
            writeItemPositions(instance, bw);
            writeStackPositions(instance, bw);

            bw.close();
            fos.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes the configuration of an instance set to the specified file.
     *
     * @param filename                                     - the name of the file to be written to
     * @param numOfInstances                               - the number of instances of the set
     * @param numOfItems                                   - the number of items that are part of the instances
     * @param stackCapacity                                - the capacity of the instance's stacks
     * @param additionalStackPercentage                    - the percentage to be added upon the minimum number of stacks (n/b)
     * @param chanceForOneInStackingConstraints            - the chance for a 1-entry in the stacking constraint matrix (not used atm)
     * @param chanceForOneInPlacementConstraints           - the chance for a 1-entry in the placement constraint matrix
     * @param usingStackingConstraintGenerationApproachOne - determines whether the 1st approach of stacking constraint generation is used
     * @param itemLengthLB                                 - the lower bound of the item lengths
     * @param itemLengthUB                                 - the upper bound of the item lengths
     * @param itemWidthLB                                  - the lower bound of the item widths
     * @param itemWidthUB                                  - the upper bound of the item widths
     */
    public static void writeConfig(String filename, int numOfInstances, int numOfItems, int stackCapacity,
        int additionalStackPercentage, float chanceForOneInStackingConstraints, float chanceForOneInPlacementConstraints,
        boolean usingStackingConstraintGenerationApproachOne, float itemLengthLB, float itemLengthUB, float itemWidthLB, float itemWidthUB
    ) {

        File file = new File(filename);

        try {
            FileOutputStream fos = new FileOutputStream(file);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

            bw.write(
                "numOfInstances,numOfItems,stackCapacity,additionalStackPercentage,"
                + "chanceForOneInStackingConstraints,chanceForOneInStackConstraints,"
                + "usingStackingConstraintGenerationApproachOne,itemLengthLB,itemLengthUB,"
                + "itemWidthLB,itemWidthUB\n"
            );
            bw.write(
                numOfInstances + "," + numOfItems + "," + stackCapacity + "," + additionalStackPercentage + ","
                + chanceForOneInStackingConstraints + "," + chanceForOneInPlacementConstraints + ","
                + usingStackingConstraintGenerationApproachOne + "," + itemLengthLB + "," + itemLengthUB + ","
                + itemWidthLB + "," + itemWidthUB
            );

            bw.close();
            fos.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
