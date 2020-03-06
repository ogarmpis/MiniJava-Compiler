import syntaxtree.*;
import visitor.GJNoArguDepthFirst;
import java.util.*;


public class STVisitor extends GJNoArguDepthFirst<String> {

    private Map<String, STClass> SymbolTableMap;
    private STClass symbolTableClassItem;
    private STMethod methodClassItem;
    private boolean methodLocalVariables;

    // Initialize Symbol Table Map on constructor
    public STVisitor() {
        SymbolTableMap = new LinkedHashMap<String, STClass>();
        methodLocalVariables = false;
    }

    // Return the symbol table, in order to use it on type checking visitor
    public Map<String, STClass> getSymbolTableMap() {
        return SymbolTableMap;
    }

    // Print Full Variables and Methods Types
    public void printTypes() {
        for (Map.Entry stm: SymbolTableMap.entrySet()) {
            STClass currentClass = SymbolTableMap.get(stm.getKey());
            System.out.println("-----------Class " + stm.getKey() + "-----------");
            System.out.println("---Variables---");
            if (currentClass.getVariablesTypes() != null) {
                for (Map.Entry m: currentClass.getVariablesTypes().entrySet()) {
                    System.out.println(stm.getKey() + "." + m.getKey() + " : " + m.getValue());
                }
            }
            System.out.println("---Methods---");
            if (currentClass.getMethodsTypes() != null) {
                for (Map.Entry m: currentClass.getMethodsTypes().entrySet()) {
                    STMethod currentMethod = currentClass.getMethodsTypes().get(m.getKey());
                    System.out.println(stm.getKey() + "." + m.getKey() + " : " + currentMethod.getType());
                    System.out.print("   -  Arguments: ");
                    for (Map.Entry arg: currentMethod.getArguments().entrySet()) {
                        System.out.print(arg.getValue() + " " + arg.getKey() + " - ");
                    }
                    System.out.print("\n   -  Local Variables: ");
                    for (Map.Entry lv: currentMethod.getLocalVariable().entrySet()) {
                        System.out.print(lv.getValue() + " " + lv.getKey() + " - ");
                    }
                    System.out.println();
                }
            }
            System.out.println();
        }
    }

    // Print the Symbol Table
    public void printOffsets() {
        for (Map.Entry stm: SymbolTableMap.entrySet()) {
            STClass currentClass = SymbolTableMap.get(stm.getKey());
            if (!currentClass.getIsMainClass()) {
                System.out.println("-----------Class " + stm.getKey() + "-----------");
                System.out.println("--Variables---");
                if (currentClass.getVariablesOffsets() != null) {
                    for (Map.Entry m: currentClass.getVariablesOffsets().entrySet()) {
                        System.out.println(stm.getKey() + "." + m.getKey() + " : " + m.getValue());
                    }
                }
                System.out.println("---Methods---");
                if (currentClass.getMethodsOffsets() != null) {
                    for (Map.Entry m: currentClass.getMethodsOffsets().entrySet()) {
                        System.out.println(stm.getKey() + "." + m.getKey() + " : " + m.getValue());
                    }
                }
                System.out.println();
            }
        }
    }

    /**
     * f0 -> MainClass()
     * f1 -> ( TypeDeclaration() )*
     * f2 -> <EOF>
     */
    public String visit(Goal n) throws Exception {
        String mainClassName = n.f0.accept(this);
        String retValue = n.f1.accept(this);
        return mainClassName;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> "public"
     * f4 -> "static"
     * f5 -> "void"
     * f6 -> "main"
     * f7 -> "("
     * f8 -> "String"
     * f9 -> "["
     * f10 -> "]"
     * f11 -> Identifier()
     * f12 -> ")"
     * f13 -> "{"
     * f14 -> ( VarDeclaration() )*
     * f15 -> ( Statement() )*
     * f16 -> "}"
     * f17 -> "}"
     */
    public String visit(MainClass n) throws Exception {
        // Find the name of current class and initialize the STClass structure
        String className = n.f1.accept(this);
        symbolTableClassItem = new STClass(className);
        symbolTableClassItem.setIsMainClass();
        // Insert the Main information
        methodClassItem = new STMethod("main", "void");
        methodClassItem.setArgument("String[]", n.f11.accept(this));
        methodLocalVariables = true;
        String retValue = n.f14.accept(this);
        methodLocalVariables = false;
        symbolTableClassItem.insertMethodType("main", methodClassItem);
        // Add the structure of current class elements in the Symbol Table list
        SymbolTableMap.put(className, symbolTableClassItem);
        return className;
    }

    /**
     * f0 -> ClassDeclaration()
     *       | ClassExtendsDeclaration()
     */
    public String visit(TypeDeclaration n) throws Exception {
        return n.f0.accept(this);
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> ( VarDeclaration() )*
     * f4 -> ( MethodDeclaration() )*
     * f5 -> "}"
     */
    public String visit(ClassDeclaration n) throws Exception {
        // Find the name of current class and check if we have already declared a class with same name
        String className = n.f1.accept(this);
        if (SymbolTableMap.containsKey(className)) {
            throw new Exception("Class \"" + className + "\" has already been declared (duplicate)");
        }
        // Initialize the STClass structure
        symbolTableClassItem = new STClass(className);
        String retValue = n.f3.accept(this);
        retValue = n.f4.accept(this);
        // Add the structure of current class elements in the Symbol Table list
        SymbolTableMap.put(className, symbolTableClassItem);
        return null;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "extends"
     * f3 -> Identifier()
     * f4 -> "{"
     * f5 -> ( VarDeclaration() )*
     * f6 -> ( MethodDeclaration() )*
     * f7 -> "}"
     */
    public String visit(ClassExtendsDeclaration n) throws Exception {
        // Find the name of current class and check if we have already declared a class with same name
        String className = n.f1.accept(this);
        if (SymbolTableMap.containsKey(className)) {
            throw new Exception("Class \"" + className + "\" has already been declared (duplicate)");
        }
        // Initialize the STClass structure
        symbolTableClassItem = new STClass(className);
        // Check if the class that extends exists
        String extendedClassName = n.f3.accept(this);
        if (!SymbolTableMap.containsKey(extendedClassName)) {
            throw new Exception("Class \"" + className + "\" extends undefined class: " + extendedClassName);
        }
        // Variables and Methods
        symbolTableClassItem.setExtendedSuperClass(extendedClassName);
        String retValue = n.f5.accept(this);
        retValue = n.f6.accept(this);
        // Add the structure of current class elements in the Symbol Table list
        SymbolTableMap.put(className, symbolTableClassItem);
        return null;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     * f2 -> ";"
     */
    public String visit(VarDeclaration n) throws Exception {
        // Store the method's local variables into symbol table
        if (methodLocalVariables) {
            methodClassItem.setLocalVariable(n.f0.accept(this), n.f1.accept(this));
        }
        else {    // For offsets, check if this is an extended class, for same variables with the super class
            String superClassName = symbolTableClassItem.getExtendedSuperClass();
            if (superClassName != null) {
                STClass superClass = SymbolTableMap.get(superClassName);
                if (symbolTableClassItem.getCurrentVariableOffset() == 0) {
                    symbolTableClassItem.setCurrentVariableOffset(superClass.getCurrentVariableOffset());
                }
            }    // Now time for type checking
            String type = n.f0.accept(this);
            // If everything is alright, insert this variable into symbol table (both its type and offset)
            symbolTableClassItem.insertVariableOffset(n.f0.accept(this), n.f1.accept(this));
            symbolTableClassItem.insertVariableType(n.f0.accept(this), n.f1.accept(this));
        }
        return null;
    }

    /**
     * f0 -> "public"
     * f1 -> Type()
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( FormalParameterList() )?
     * f5 -> ")"
     * f6 -> "{"
     * f7 -> ( VarDeclaration() )*
     * f8 -> ( Statement() )*
     * f9 -> "return"
     * f10 -> Expression()
     * f11 -> ";"
     * f12 -> "}"
     */
    public String visit(MethodDeclaration n) throws Exception {
        // Insert method's arguments and type
        String type = n.f1.accept(this);
        String methodName = n.f2.accept(this);
        methodClassItem = new STMethod(methodName, type);
        // Calculate method's arguments
        String retValue = n.f4.accept(this);
        methodLocalVariables = true;
        retValue = n.f7.accept(this);
        methodLocalVariables = false;
        symbolTableClassItem.insertMethodType(methodName, methodClassItem);
        // Check, if this is an extended class, for same variables with the super class
        String superClassName = symbolTableClassItem.getExtendedSuperClass();
        Integer firstParent = 0;
        while (superClassName != null) {
            STClass superClass = SymbolTableMap.get(superClassName);
            if (superClass != null) {
                firstParent += 1;
                if (superClass.getMethodsOffsets().containsKey(n.f2.accept(this))) {
                    STMethod currentMethod = superClass.getMethodsTypes().get(methodName);
                    if (!currentMethod.getType().equals(type)) {
                        throw new Exception("Method \"" + methodName + "\" is different type than the overloaded one");
                    }
                    List<String> currentArgumentsTypes = new ArrayList<String>(methodClassItem.getArguments().values());
                    List<String> parentArgumentsTypes = new ArrayList<String>(currentMethod.getArguments().values());
                    if (!currentArgumentsTypes.equals(parentArgumentsTypes)) {
                        throw new Exception("Method \"" + methodName + "\" has different arguments from base method");
                    }
                    return null;
                }
                if (symbolTableClassItem.getCurrentMethodOffset() == 0 && firstParent == 1) {
                    symbolTableClassItem.setCurrentMethodOffset(superClass.getCurrentMethodOffset());
                }
                superClassName = superClass.getExtendedSuperClass();
            }
        }
        // If everything is alright, insert this variable offset into symbol table
        symbolTableClassItem.insertMethodOffset(n.f2.accept(this));
        return null;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     */
    public String visit(FormalParameter n) throws Exception {
        methodClassItem.setArgument(n.f0.accept(this), n.f1.accept(this));
        return null;
    }

    /**
     * f0 -> ArrayType()
     *       | BooleanType()
     *       | IntegerType()
     *       | Identifier()
     */
    public String visit(Type n) throws Exception {
        return n.f0.accept(this);
    }

    /**
     * f0 -> "int"
     * f1 -> "["
     * f2 -> "]"
     */
    public String visit(ArrayType n) throws Exception {
        String arrType = n.f0.toString() + n.f1.toString() + n.f2.toString();
        return arrType;
    }

    /**
     * f0 -> "boolean"
     */
    public String visit(BooleanType n) throws Exception {
        return n.f0.toString();
    }

    /**
     * f0 -> "int"
     */
    public String visit(IntegerType n) throws Exception {
        return n.f0.toString();
    }

    /**
     * f0 -> <IDENTIFIER>
     */
    public String visit(Identifier n) throws Exception {
        return n.f0.toString();
    }

}
