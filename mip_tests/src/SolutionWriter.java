import java.io.*;

public class SolutionWriter {

    public static String getNameOfMipFormulation(MIPFormulationComparator.Formulation mipFormulation) {
        switch (mipFormulation) {
            case BINPACKING:
                return "BinPacking Formulation";
            case THREEINDEX:
                return "ThreeIndex Formulation";
            default:
                return "";
        }
    }

    public static void writeSolution(String filename, Solution sol, MIPFormulationComparator.Formulation mipFormulation) {

        try {

            File file = new File(filename);

            boolean appendNewLines = true;

            if (!file.exists()) {
                file.createNewFile();
                appendNewLines = false;
            }

            FileWriter fw = new FileWriter(file, true);
            BufferedWriter bw = new BufferedWriter(fw);

            String mip = getNameOfMipFormulation(mipFormulation);

            if (appendNewLines) {
                bw.newLine();
                bw.write("#####################################################\n");
                bw.newLine();
            }

            bw.write("solved with: " + mip + "\n");
            bw.write(sol.toString());

            bw.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
