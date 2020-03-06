import java_cup.runtime.*;
import java.io.*;

class Main {
    public static void main(String[] argv) throws Exception {
        // Set output stream to a .java file inside a new outputs directory
        new File("./outputs").mkdirs();
        PrintStream outputFile = new PrintStream(new File("./outputs/Main.java"));
        System.setOut(outputFile);
        // Parsing
        Parser p = new Parser(new Scanner(new InputStreamReader(System.in)));
        p.parse();
    }
}
