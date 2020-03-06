import syntaxtree.*;
import visitor.*;
import java.io.*;


class Main {
    public static void main (String [] args) {
      	if (args.length == 0) {
      	    System.err.println("Usage: java Driver <inputFile>");
      	    System.exit(1);
      	}
      	FileInputStream fis = null;
        for (String argument: args) {
            System.out.println("-------------------------------------------------------------------------------------");
            System.out.println(argument);
            // Firstly, create symbol table and find offsets
          	try {
          	    fis = new FileInputStream(argument);
          	    MiniJavaParser parser = new MiniJavaParser(fis);
          	    System.err.println("Program parsed successfully.");
          	    STVisitor symbolTable = new STVisitor();
          	    Goal root = parser.Goal();
                try {
          	        String className = root.accept(symbolTable);
                    //symbolTable.printTypes();
                    System.out.println("Symbol Table for class \"" + className + "\" created successfully\n");
                    // Now type checking
                    fis = new FileInputStream(argument);
              	    parser = new MiniJavaParser(fis);
              	    //System.err.println("Program parsed successfully again.");
              	    TCVisitor typeChecker = new TCVisitor(symbolTable.getSymbolTableMap());
              	    root = parser.Goal();
                    try {
              	        root.accept(typeChecker);
                        symbolTable.printOffsets();
                        System.out.println("Offsets and Type Checking for class \"" + className + "\" were successful");
                    }
                    catch (Exception ex) {
                        symbolTable.getSymbolTableMap().clear();
                        System.out.println(ex.getMessage());
                    }
                }
                catch (Exception ex) {
                    System.out.println(ex.getMessage());
                }
          	}
          	catch (ParseException ex) {
          	    System.out.println(ex.getMessage());
          	}
          	catch (FileNotFoundException ex) {
          	    System.err.println(ex.getMessage());
          	}
          	finally {
          	    try {
              		  if (fis != null) fis.close();
          	    }
          	    catch (IOException ex) {
          		      System.err.println(ex.getMessage());
          	    }
            }
        }
        System.out.println("-------------------------------------------------------------------------------------");
    }
}
