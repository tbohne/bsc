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
}
