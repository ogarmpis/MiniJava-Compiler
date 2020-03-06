import syntaxtree.*;
import visitor.GJDepthFirst;
import java.util.*;
import java.io.*;


public class LLVMVisitor extends GJDepthFirst<String,String> {

    private Map<String, STClass> SymbolTableMap;
    //private String javaClassName;
    private String outputFilename;
    private List<String> methodArguments;
    private List<String> methodCallArguments;
    private boolean methodCallProcedure;
    private Integer currentIfLabel;
    private Integer currentWhileLabel;
    private Integer currentOobLabel;
    private Integer currentAllocLabel;
    private Integer currentAndClauseLabel;
    private Integer currentRegister;
    //private String callocRegister;
    private String currentMethodName;

    static final String outputsFolder = "./outputs/";

    // Initialize Symbol Table Map on constructor
    public LLVMVisitor(Map<String, STClass> STMap, String className) {
        SymbolTableMap = new LinkedHashMap<String, STClass>(STMap);
        //javaClassName = new String(className);
        outputFilename = new String(outputsFolder + className + ".ll");
        methodArguments = new LinkedList<String>();
        methodCallArguments = new ArrayList<String>();
        methodCallProcedure = false;
        currentIfLabel = -1;
        currentWhileLabel = -1;
        currentOobLabel = -1;
        currentAllocLabel = -1;
        currentAndClauseLabel = -1;
        currentRegister = -1;
        currentMethodName = null;
    }

    // Increase if label number
    private Integer addIfLabel() {
        currentIfLabel++;
        return currentIfLabel;
    }

    // Increase while label number
    private Integer addWhileLabel() {
        currentWhileLabel++;
        return currentWhileLabel;
    }

    // Increase oob label number
    private Integer addOobLabel() {
        currentOobLabel++;
        return currentOobLabel;
    }

    // Increase alloc label number
    private Integer addAllocLabel() {
        currentAllocLabel++;
        return currentAllocLabel;
    }

    // Increase and clause label number
    private Integer addAndClauseLabel() {
        currentAndClauseLabel++;
        return currentAndClauseLabel;
    }

    // Increase register number
    private Integer addRegister() {
        currentRegister++;
        return currentRegister;
    }

    // Generate llvm code
    private String generate(String llvmcode) {
        String code = new String(llvmcode);
        return code;
    }

    // Emit function - write llvm instructions to outputFilename
    private void emit(String llvmInstruction) throws Exception {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new BufferedWriter(new FileWriter(outputFilename, true)));
            writer.println(llvmInstruction);
        } catch (IOException ex) {
            throw new Exception("Error on writing to file \"" + outputFilename + "\"");
        } finally {
            try { writer.close(); } catch (Exception ex) {}
        }
    }

    // Find the type of a variable or method
    private String convertType(String actualType) {
        String llvmType = "i8*";
        if (actualType.equals("int")) {
            llvmType = "i32";
        }
        else if (actualType.equals("boolean")) {
            llvmType = "i1";
        }
        return llvmType;
    }

    // Check for different kind of expressions
    private String checkExpressionTypes(String expr, String currClass) throws Exception {
        String llvmInstruction = "";
        String llvmType = "";
        if (expr.substring(0, 1).equals("$")) {
            expr = expr.substring(1);
            String[] splited = expr.split("\\s+");
            expr = "%_" + splited[1];
        }
        else if (!expr.matches("-?\\d+")) {
            STClass currentClass = SymbolTableMap.get(currClass);
            STMethod currentMethod = currentClass.getMethodsTypes().get(currentMethodName);
            String objectName = expr;
            String objectType = currentClass.getVariablesTypes().get(objectName);
            objectType = currentMethod.getArguments().get(objectName);
            if (objectType == null) {
                objectType = currentMethod.getLocalVariable().get(objectName);
            }
            if (objectType == null) {
                objectType = currentClass.getVariablesTypes().get(objectName);
                if (objectType != null) {
                    Integer offset = currentClass.getVariablesOffsets().get(objectName) + 8;
                    llvmInstruction = generate("\t%_" + addRegister() + " = getelementptr i8, i8* %this, i32 " + offset);
                    emit(llvmInstruction);
                    if (objectType.equals("int")) llvmType = "i32*";
                    else if (objectType.equals("int[]")) llvmType = "i32**";
                    else if (objectType.equals("boolean")) llvmType = "i1*";
                    else llvmType = "i8**";
                    llvmInstruction = generate("\t%_" + addRegister() + " = bitcast i8* %_" + (currentRegister-1) + " to " + llvmType);
                    emit(llvmInstruction);
                    expr = "_" + currentRegister.toString();
                }
            }
            if (objectType == null) {
                String superClassName = currentClass.getExtendedSuperClass();
                while (superClassName != null) {
                    STClass superClass = SymbolTableMap.get(superClassName);
                    if (superClass != null) {
                        objectType = superClass.getVariablesTypes().get(objectName);
                        if (objectType != null) {
                            break;
                        }
                    }
                    superClassName = superClass.getExtendedSuperClass();
                }
            }
            if (objectType.equals("int")) llvmType = "i32*";
            else if (objectType.equals("int[]")) llvmType = "i32**";
            else if (objectType.equals("boolean")) llvmType = "i1*";
            else llvmType = "i8**";
            String llvmType2 = "";
            if (objectType.equals("int")) llvmType2 = "i32";
            else if (objectType.equals("int[]")) llvmType2 = "i32*";
            else if (objectType.equals("boolean")) llvmType2 = "i1";
            else llvmType2 = "i8*";
            llvmInstruction = generate("\t%_" + addRegister() + " = load " + llvmType2 + ", " + llvmType + " %" + expr);
            emit(llvmInstruction);
            expr = "%_" + currentRegister;
        }
        return expr;
    }


    /**
     * f0 -> MainClass()
     * f1 -> ( TypeDeclaration() )*
     * f2 -> <EOF>
     */
    public String visit(Goal n, String argu) throws Exception {
        // Now create classes vtables
        String llvmInstruction = "";
        // Generate a vtable for each class
        for (Map.Entry stm: SymbolTableMap.entrySet()) {
            STClass currentClass = SymbolTableMap.get(stm.getKey());
            Integer numOfMethods = 0;
            String methods = new String("");
            // If this is the first main class, no methods, else, get the information of inner classes methods
            if (!currentClass.getIsMainClass()) {
                numOfMethods = currentClass.getMethodsTypes().size();
                // Find all methods in order to add them into vtable
                if (currentClass.getMethodsTypes() != null) {
                    for (Map.Entry m: currentClass.getMethodsTypes().entrySet()) {
                        STMethod currentMethod = currentClass.getMethodsTypes().get(m.getKey());
                        // Find methodName, return type and arguments types
                        String methodName = "@" + stm.getKey() + "." + m.getKey();
                        String returnType = convertType(currentMethod.getType());
                        String argumentsTypes = "";
                        for (Map.Entry arg: currentMethod.getArguments().entrySet()) {
                            String argType = currentMethod.getArguments().get(arg.getKey());
                            argumentsTypes = argumentsTypes + ", " + convertType(argType);
                        }
                        methods = methods + "\t\t\ti8* bitcast (" + returnType + " (i8*" + argumentsTypes + ")* " + methodName + " to i8*),\n";
                    }
                    methods = methods.substring(0, methods.length()-2);
                }
            }
            else numOfMethods = 0;
            // Generate the vtables code
            if (numOfMethods == 0) {
                llvmInstruction = generate("@." + stm.getKey() + "_vtable = global [" + numOfMethods + " x i8*] [" + methods + "]");
            } else {
                llvmInstruction = generate("@." + stm.getKey() + "_vtable = global [" + numOfMethods + " x i8*] \n\t\t[\n" + methods + "\n\t\t]");
            }
            emit(llvmInstruction);
        }
        // Write standard llvm instructions
        llvmInstruction = generate("\n\ndeclare i8* @calloc(i32, i32)\ndeclare i32 @printf(i8*, ...)\ndeclare void @exit(i32)\n");
        emit(llvmInstruction);
        llvmInstruction = generate("@_cint = constant [4 x i8] c\"%d\\0a\\00\"\n@_cOOB = constant [15 x i8] c\"Out of bounds\\0a\\00\"");
        emit(llvmInstruction);
        llvmInstruction = generate("define void @print_int(i32 %i) {\n\t%_str = bitcast [4 x i8]* @_cint to i8*\n\tcall i32 (i8*, ...) @printf(i8* %_str, i32 %i)\n\tret void\n}\n");
        emit(llvmInstruction);
        llvmInstruction = generate("define void @throw_oob() {\n\t%_str = bitcast [15 x i8]* @_cOOB to i8*\n\tcall i32 (i8*, ...) @printf(i8* %_str)\n\tcall void @exit(i32 1)\n\tret void\n}\n");
        emit(llvmInstruction);
        // Continue deep into the tree
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        return null;
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
    public String visit(MainClass n, String argu) throws Exception {
        // Call VarDeclaration and Statement only
        String llvmInstruction = generate("define i32 @main() {");
        emit(llvmInstruction);
        n.f14.accept(this, argu);
        n.f15.accept(this, argu);
        llvmInstruction = generate("\tret i32 0\n}\n");
        emit(llvmInstruction);
        return null;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> ( VarDeclaration() )*
     * f4 -> ( MethodDeclaration() )*
     * f5 -> "}"
     */
    public String visit(ClassDeclaration n, String argu) throws Exception {
        String className = n.f1.accept(this, argu);
        //n.f3.accept(this, argu);
        // Pass the class' name as argument into methodDeclaration
        n.f4.accept(this, className);
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
    public String visit(ClassExtendsDeclaration n, String argu) throws Exception {
        String className = n.f1.accept(this, argu);
        //n.f5.accept(this, argu);
        n.f6.accept(this, className);
        return null;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     * f2 -> ";"
     */
    public String visit(VarDeclaration n, String argu) throws Exception {
        String varType = convertType(n.f0.accept(this, argu));
        String varName = n.f1.accept(this, argu);
        String llvmInstruction = generate("\t%" + varName + " = alloca " + varType + "\n");
        emit(llvmInstruction);
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
    public String visit(MethodDeclaration n, String argu) throws Exception {
        String methodType = convertType(n.f1.accept(this, argu));
        String methodName = n.f2.accept(this, argu);
        currentMethodName = methodName;
        // Find method's arguments types and names
        methodArguments = new LinkedList<String>();
        n.f4.accept(this, argu);
        String arguments = "";
        for (String arg : methodArguments) {
            arguments = arguments + ", " + arg;
        }
        String llvmInstruction = generate("define " + methodType + " @" + argu + "." + methodName + "(i8* %this" + arguments + ") {");
        emit(llvmInstruction);
        // Assign arguments into local registers in llvm
        for (String arg : methodArguments) {
            String[] splited = arg.split("\\s+");
            String argName = splited[1].substring(2);
            llvmInstruction = generate("\t%" + argName + " = alloca " + splited[0]);
            emit(llvmInstruction);
            llvmInstruction = generate("\tstore " + splited[0] + " " + splited[1] + ", " + splited[0] + "* %" + argName);
            emit(llvmInstruction);
        }
        emit("");
        n.f7.accept(this, argu);
        n.f8.accept(this, argu);
        // Return statement (different for integers, booleans and identifiers)
        String retValue = n.f10.accept(this, argu);
        if (retValue.matches("-?\\d+")) {
            llvmInstruction = generate("\tret i32 " + retValue);
            emit(llvmInstruction);
        } else if (retValue.equals("true") || retValue.equals("false")) {
            if (retValue.equals("true")) retValue = "1";
            else if (retValue.equals("false")) retValue = "0";
            llvmInstruction = generate("\tret " + methodType + " " + retValue);
            emit(llvmInstruction);
        } else {
            retValue = checkExpressionTypes(retValue, argu);
            //llvmInstruction = generate("\t%_" + addRegister() + " = load " + methodType + ", " + methodType + "* " + retValue);
            //emit(llvmInstruction);
            llvmInstruction = generate("\tret " + methodType + " %_" + currentRegister);
            emit(llvmInstruction);
        }
        emit("}\n");
        return null;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     */
    public String visit(FormalParameter n, String argu) throws Exception {
        String fullParameter = convertType(n.f0.accept(this, argu)) + " %." + n.f1.accept(this, argu);
        methodArguments.add(fullParameter);
        return null;
    }

    /**
     * f0 -> "if"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     * f5 -> "else"
     * f6 -> Statement()
     */
    public String visit(IfStatement n, String argu) throws Exception {
        // if expression
        String className = n.f2.accept(this, argu);
        String expressionRegister = className;
        if (className.substring(0, 1).equals("$")) {
            className = className.substring(1);
            String[] splited = className.split("\\s+");
            className = splited[0];
            expressionRegister = "%_" + splited[1];
        }
        else if (!className.matches("-?\\d+")) {
            expressionRegister = checkExpressionTypes(className, argu);
            //String llvmInstruction = generate("\t%_" + addRegister() + " = load i1, i1* " + expressionRegister);
            //emit(llvmInstruction);
            //expressionRegister = "%_" + currentRegister;
        }
        Integer ifLabel = addIfLabel();
        Integer elseLabel = addIfLabel();
        Integer exitLabel = addIfLabel();
        String llvmInstruction = generate("\tbr i1 " + expressionRegister + ", label %if" + ifLabel + ", label %if" + elseLabel + "\n");
        emit(llvmInstruction);
        // If
        llvmInstruction = generate("if" + ifLabel + ":");
        emit(llvmInstruction);
        n.f4.accept(this, argu);
        llvmInstruction = generate("\tbr label %if" + exitLabel + "\n");
        emit(llvmInstruction);
        // Else
        llvmInstruction = generate("if" + elseLabel + ":");
        emit(llvmInstruction);
        n.f6.accept(this, argu);
        llvmInstruction = generate("\tbr label %if" + exitLabel + "\n");
        emit(llvmInstruction);
        // Finally exit if statement
        llvmInstruction = generate("if" + exitLabel + ":");
        emit(llvmInstruction);
        return null;
    }

    /**
     * f0 -> "while"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     */
    public String visit(WhileStatement n, String argu) throws Exception {
        // While initial statements
        Integer exprLabel = addWhileLabel();
        Integer bodyLabel = addWhileLabel();
        Integer exitLabel = addWhileLabel();
        String llvmInstruction = generate("\tbr label %loop" + exprLabel + "\n");
        emit(llvmInstruction);
        llvmInstruction = generate("loop" + exprLabel + ":");
        emit(llvmInstruction);
        // While expression
        String className = n.f2.accept(this, argu);
        String expressionRegister = className;
        if (className.substring(0, 1).equals("$")) {
            className = className.substring(1);
            String[] splited = className.split("\\s+");
            className = splited[0];
            expressionRegister = "%_" + splited[1];
        }
        else if (!className.matches("-?\\d+")) {
            expressionRegister = checkExpressionTypes(className, argu);
            //llvmInstruction = generate("\t%_" + addRegister() + " = load i1, i1* %" + expressionRegister);
            //emit(llvmInstruction);
            //expressionRegister = "%_" + currentRegister;
        }
        llvmInstruction = generate("\tbr i1 " + expressionRegister + ", label %loop" + bodyLabel + ", label %loop" + exitLabel + "\n");
        emit(llvmInstruction);
        // Body of While loop
        llvmInstruction = generate("loop" + bodyLabel + ":");
        emit(llvmInstruction);
        n.f4.accept(this, argu);
        llvmInstruction = generate("\tbr label %loop" + exprLabel + "\n");
        emit(llvmInstruction);
        // Exit loop
        llvmInstruction = generate("loop" + exitLabel + ":");
        emit(llvmInstruction);
        return null;
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Expression()
     * f3 -> ";"
     */
    public String visit(AssignmentStatement n, String argu) throws Exception {
        String ident = n.f0.accept(this, argu);
        String identName = ident;
        String llvmType = "";
        boolean foundIntArray = false;
        if (ident.substring(0, 1).equals("$")) {
            ident = ident.substring(1);
            String[] splited = ident.split("\\s+");
            ident = "%_" + splited[1];
        }
        else if (!ident.matches("-?\\d+")) {
            STClass currentClass = SymbolTableMap.get(argu);
            STMethod currentMethod = currentClass.getMethodsTypes().get(currentMethodName);
            String objectName = ident;
            String objectType = currentClass.getVariablesTypes().get(objectName);
            objectType = currentMethod.getArguments().get(objectName);
            if (objectType == null) {
                objectType = currentMethod.getLocalVariable().get(objectName);
            }
            if (objectType == null) {
                objectType = currentClass.getVariablesTypes().get(objectName);
                if (objectType != null) {
                    if (!objectType.equals("int[]")) {
                        Integer offset = currentClass.getVariablesOffsets().get(objectName) + 8;
                        String llvmInstruction = generate("\t%_" + addRegister() + " = getelementptr i8, i8* %this, i32 " + offset);
                        emit(llvmInstruction);
                        if (objectType.equals("int")) llvmType = "i32*";
                        else if (objectType.equals("int[]")) llvmType = "i32**";
                        else if (objectType.equals("boolean")) llvmType = "i1*";
                        else llvmType = "i8**";
                        llvmInstruction = generate("\t%_" + addRegister() + " = bitcast i8* %_" + (currentRegister-1) + " to " + llvmType);
                        emit(llvmInstruction);
                        ident = "%_" + currentRegister.toString();
                    }
                    else foundIntArray = true;
                }
            } else ident = "%" + ident;
        }
        //ident = checkExpressionTypes(ident, argu);
        String expr = n.f2.accept(this, argu);
        String exprName = expr;
        //expr = checkExpressionTypes(expr, argu);
        String expressionRegister = expr;
        String[] splited = null;
        //System.out.println(className);
        if (expr.equals(argu)) {
            expressionRegister = "%this";
        }
        else if (expr.substring(0, 1).equals("$")) {
            expr = expr.substring(1);
            splited = expr.split("\\s+");
            expr = splited[0];
            expressionRegister = "%_" + splited[1];
        }
        else if (!expr.matches("-?\\d+") && !expr.matches("true") && !expr.matches("false")) {
            STClass currentClass = SymbolTableMap.get(argu);
            STMethod currentMethod = currentClass.getMethodsTypes().get(currentMethodName);
            String objectName = expr;
            String objectType = currentClass.getVariablesTypes().get(objectName);
            objectType = currentMethod.getArguments().get(objectName);
            if (objectType == null) {
                objectType = currentMethod.getLocalVariable().get(objectName);
            }
            if (objectType == null) {
                objectType = currentClass.getVariablesTypes().get(objectName);
                if (objectType != null) {
                    //if (!objectType.equals("int[]")) {
                        Integer offset = currentClass.getVariablesOffsets().get(objectName) + 8;
                        String llvmInstruction = generate("\t%_" + addRegister() + " = getelementptr i8, i8* %this, i32 " + offset);
                        emit(llvmInstruction);
                        if (objectType.equals("int")) llvmType = "i32*";
                        else if (objectType.equals("int[]")) llvmType = "i32**";
                        else if (objectType.equals("boolean")) llvmType = "i1*";
                        else llvmType = "i8**";
                        llvmInstruction = generate("\t%_" + addRegister() + " = bitcast i8* %_" + (currentRegister-1) + " to " + llvmType);
                        emit(llvmInstruction);
                        expressionRegister = "%_" + currentRegister.toString();
                    //}
                }
            } else expressionRegister = "%" + expressionRegister;
            // Find type of call objects
            String callType = "i8*";
            if (objectType == null) objectType = expr;
            if (objectType != null) expr = objectType;
            if (objectType.equals("int")) callType = "i32";
            else if (objectType.equals("boolean")) callType = "i1";
            String llvmInstruction = generate("\t%_" + addRegister() + " = load " + callType + ", " + callType + "* " + expressionRegister);
            emit(llvmInstruction);
            expressionRegister = "%_" + currentRegister;
            //expressionRegister = "%" + expressionRegister;
        }
        if (!foundIntArray) {
            String storeReg = "i32 " + expressionRegister + ", i32* ";
            // Check ident and expr type
            STClass currentClass = SymbolTableMap.get(expr);
            if (currentClass != null) storeReg = "i8* " + expressionRegister + ", i8** ";
            currentClass = SymbolTableMap.get(argu);
            STMethod currentMethod = currentClass.getMethodsTypes().get(currentMethodName);
            String identType = currentMethod.getArguments().get(identName);
            if (identType == null) {
                identType = currentMethod.getLocalVariable().get(identName);
            }
            if (identType == null) {
                identType = currentClass.getVariablesTypes().get(identName);
            }
            if (identType != null) {
                //System.out.println(identName + " " + identType);
                if (identType.equals("boolean")) storeReg = "i1 " + expressionRegister + ", i1* ";
            }
            // Special if expr = true or false
            if (expr.matches("true")) storeReg = "i1 1, i1* ";
            if (expr.matches("false")) storeReg = "i1 0, i1* ";
            String llvmInstruction = generate("\tstore " + storeReg + ident + "\n");
            emit(llvmInstruction);
        }
        return null;
    }

    /**
     * f0 -> Identifier()
     * f1 -> "["
     * f2 -> Expression()
     * f3 -> "]"
     * f4 -> "="
     * f5 -> Expression()
     * f6 -> ";"
     */
    public String visit(ArrayAssignmentStatement n, String argu) throws Exception {
        // Check for different kind of expressions
        String llvmInstruction = "";
        String first = n.f0.accept(this, argu);
        first = checkExpressionTypes(first, argu);
        String second = n.f2.accept(this, argu);
        second = checkExpressionTypes(second, argu);
        // LLVM instruction for array lookup
        llvmInstruction = generate("\t%_" + addRegister() + " = load i32, i32 *" + first);
        emit(llvmInstruction);
        llvmInstruction = generate("\t%_" + addRegister() + " = icmp ult i32 " + second + ", %_" + (currentRegister-1));
        emit(llvmInstruction);
        // Oob labels initialization
        Integer bodyLabel = addOobLabel();
        Integer throwLabel = addOobLabel();
        Integer exitLabel = addOobLabel();
        llvmInstruction = generate("\tbr i1 %_" + currentRegister + ", label %oob" + bodyLabel + ", label %oob" + throwLabel + "\n");
        emit(llvmInstruction);
        // Oob body
        llvmInstruction = generate("oob" + bodyLabel + ":");
        emit(llvmInstruction);
        llvmInstruction = generate("\t%_" + addRegister() + " = add i32 " + second + ", 1");
        emit(llvmInstruction);
        llvmInstruction = generate("\t%_" + addRegister() + " = getelementptr i32, i32* " + first + ", i32 %_" + (currentRegister-1));
        emit(llvmInstruction);
        first = "%_" + currentRegister.toString();
        //llvmInstruction = generate("\t%_" + addRegister() + " = load i32, i32* %_" + (currentRegister-1));
        //emit(llvmInstruction);
        llvmInstruction = generate("\tbr label %oob" + exitLabel + "\n");
        emit(llvmInstruction);
        // Oob throw
        llvmInstruction = generate("oob" + throwLabel + ":");
        emit(llvmInstruction);
        llvmInstruction = generate("\tcall void @throw_oob()");
        emit(llvmInstruction);
        llvmInstruction = generate("\tbr label %oob" + exitLabel + "\n");
        emit(llvmInstruction);
        // Exit oob
        llvmInstruction = generate("oob" + exitLabel + ":");
        emit(llvmInstruction);
        // Right part, value assignment
        String className = n.f5.accept(this, argu);
        className = checkExpressionTypes(className, argu);
        String expressionRegister = className;
        /*String[] splited = null;
        //System.out.println(className);
        if (className.substring(0, 1).equals("$")) {
            className = className.substring(1);
            splited = className.split("\\s+");
            className = splited[0];
            expressionRegister = "%_" + splited[1];
        }
        else if (!className.matches("-?\\d+")) {
            llvmInstruction = generate("\t%_" + addRegister() + " = load i32, i32* %" + className);
            emit(llvmInstruction);
            expressionRegister = "%_" + currentRegister;
        }*/
        llvmInstruction = generate("\tstore i32 " + expressionRegister + ", i32* " + first + "\n");
        emit(llvmInstruction);
        return null;
    }

    /**
     * f0 -> "System.out.println"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> ";"
     */
    public String visit(PrintStatement n, String argu) throws Exception {
        // Generate code for System.out.println
        String className = n.f2.accept(this, argu);
        String expressionRegister = className;
        String[] splited = null;
        if (className.substring(0, 1).equals("$")) {
            className = className.substring(1);
            splited = className.split("\\s+");
            className = splited[0];
            expressionRegister = "%_" + splited[1];
        }
        if (splited == null && !className.matches("-?\\d+")) {
            String llvmInstruction = generate("\t%_" + addRegister() + " = load i32, i32* %" + className);
            emit(llvmInstruction);
            expressionRegister = "%_" + currentRegister.toString();
        }
        String llvmInstruction = generate("\tcall void (i32) @print_int(i32 " + expressionRegister + ")\n");
        emit(llvmInstruction);
        return null;
    }

    /**
     * f0 -> Clause()
     * f1 -> "&&"
     * f2 -> Clause()
     */
    public String visit(AndExpression n, String argu) throws Exception {
        // And Clause initial statements
        Integer exprLabel = addAndClauseLabel();
        Integer firstLabel = addAndClauseLabel();
        Integer secondLabel = addAndClauseLabel();
        Integer exitLabel = addAndClauseLabel();
        // First clause
        String first = n.f0.accept(this, argu);
        first = checkExpressionTypes(first, argu);
        String llvmInstruction = generate("\tbr label %andclause" + exprLabel + "\n");
        emit(llvmInstruction);
        llvmInstruction = generate("andclause" + exprLabel + ":");
        emit(llvmInstruction);
        llvmInstruction = generate("\tbr i1 " + first + ", label %andclause" + firstLabel + ", label %andclause" + exitLabel + "\n");
        emit(llvmInstruction);
        llvmInstruction = generate("andclause" + firstLabel + ":");
        emit(llvmInstruction);
        // Second clause
        String second = n.f2.accept(this, argu);
        second = checkExpressionTypes(second, argu);
        llvmInstruction = generate("\tbr label %andclause" + secondLabel + "\n");
        emit(llvmInstruction);
        llvmInstruction = generate("andclause" + secondLabel + ":");
        emit(llvmInstruction);
        llvmInstruction = generate("\tbr label %andclause" + exitLabel + "\n");
        emit(llvmInstruction);
        llvmInstruction = generate("andclause" + exitLabel + ":");
        emit(llvmInstruction);
        llvmInstruction = generate("\t%_" + addRegister() + " = phi i1 [ 0, %andclause" + exprLabel + " ], [ " + second + ", %andclause" + secondLabel +" ]\n");
        emit(llvmInstruction);
        // Return the class type of currentMethod and the last register
        String retValue = "$boolean " + currentRegister;
        return retValue;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "<"
     * f2 -> PrimaryExpression()
     */
    public String visit(CompareExpression n, String argu) throws Exception {
        // Check for different kind of expressions
        String first = n.f0.accept(this, argu);
        first = checkExpressionTypes(first, argu);
        String second = n.f2.accept(this, argu);
        second = checkExpressionTypes(second, argu);
        // LLVM instruction for compare
        String llvmInstruction = generate("\t%_" + addRegister() + " = icmp slt i32 " + first + ", " + second);
        emit(llvmInstruction);
        // Return the class type of currentMethod and the last register
        String retValue = "$boolean " + currentRegister;
        return retValue;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "+"
     * f2 -> PrimaryExpression()
     */
    public String visit(PlusExpression n, String argu) throws Exception {
        boolean mcProcedure = false;
        if (methodCallProcedure) {
            methodCallProcedure = false;
            mcProcedure = true;
        }
        // Check for different kind of expressions
        String first = n.f0.accept(this, argu);
        first = checkExpressionTypes(first, argu);
        String second = n.f2.accept(this, argu);
        second = checkExpressionTypes(second, argu);
        // LLVM instruction for plus
        String llvmInstruction = generate("\t%_" + addRegister() + " = add i32 " + first + ", " + second);
        emit(llvmInstruction);
        if (mcProcedure) {
            methodCallArguments.add("%_" + currentRegister.toString());
            methodCallProcedure = true;
        }
        // Return the class type of currentMethod and the last register
        String retValue = "$int " + currentRegister;
        return retValue;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "-"
     * f2 -> PrimaryExpression()
     */
    public String visit(MinusExpression n, String argu) throws Exception {
        boolean mcProcedure = false;
        if (methodCallProcedure) {
            methodCallProcedure = false;
            mcProcedure = true;
        }
        // Check for different kind of expressions
        String first = n.f0.accept(this, argu);
        first = checkExpressionTypes(first, argu);
        String second = n.f2.accept(this, argu);
        second = checkExpressionTypes(second, argu);
        // LLVM instruction for minus
        String llvmInstruction = generate("\t%_" + addRegister() + " = sub i32 " + first + ", " + second);
        emit(llvmInstruction);
        if (mcProcedure) {
            methodCallArguments.add("%_" + currentRegister.toString());
            methodCallProcedure = true;
        }
        // Return the class type of currentMethod and the last register
        String retValue = "$int " + currentRegister;
        return retValue;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "*"
     * f2 -> PrimaryExpression()
     */
    public String visit(TimesExpression n, String argu) throws Exception {
        boolean mcProcedure = false;
        if (methodCallProcedure) {
            methodCallProcedure = false;
            mcProcedure = true;
        }
        // Check for different kind of expressions
        String first = n.f0.accept(this, argu);
        first = checkExpressionTypes(first, argu);
        String second = n.f2.accept(this, argu);
        second = checkExpressionTypes(second, argu);
        // LLVM instruction for multiply
        String llvmInstruction = generate("\t%_" + addRegister() + " = mul i32 " + first + ", " + second);
        emit(llvmInstruction);
        if (mcProcedure) {
            methodCallArguments.add("%_" + currentRegister.toString());
            methodCallProcedure = true;
        }
        // Return the class type of currentMethod and the last register
        String retValue = "$int " + currentRegister;
        return retValue;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "["
     * f2 -> PrimaryExpression()
     * f3 -> "]"
     */
    public String visit(ArrayLookup n, String argu) throws Exception {
        boolean mcProcedure = false;
        if (methodCallProcedure) {
            methodCallProcedure = false;
            mcProcedure = true;
        }
        // Check for different kind of expressions
        String first = n.f0.accept(this, argu);
        first = checkExpressionTypes(first, argu);
        String second = n.f2.accept(this, argu);
        second = checkExpressionTypes(second, argu);
        // LLVM instruction for array lookup
        String llvmInstruction = generate("\t%_" + addRegister() + " = load i32, i32 *" + first);
        emit(llvmInstruction);
        llvmInstruction = generate("\t%_" + addRegister() + " = icmp ult i32 " + second + ", %_" + (currentRegister-1));
        emit(llvmInstruction);
        // Oob labels initialization
        Integer bodyLabel = addOobLabel();
        Integer throwLabel = addOobLabel();
        Integer exitLabel = addOobLabel();
        llvmInstruction = generate("\tbr i1 %_" + currentRegister + ", label %oob" + bodyLabel + ", label %oob" + throwLabel + "\n");
        emit(llvmInstruction);
        // Oob body
        llvmInstruction = generate("oob" + bodyLabel + ":");
        emit(llvmInstruction);
        llvmInstruction = generate("\t%_" + addRegister() + " = add i32 " + second + ", 1");
        emit(llvmInstruction);
        llvmInstruction = generate("\t%_" + addRegister() + " = getelementptr i32, i32* " + first + ", i32 %_" + (currentRegister-1));
        emit(llvmInstruction);
        llvmInstruction = generate("\t%_" + addRegister() + " = load i32, i32* %_" + (currentRegister-1));
        emit(llvmInstruction);
        llvmInstruction = generate("\tbr label %oob" + exitLabel + "\n");
        emit(llvmInstruction);
        // Oob throw
        llvmInstruction = generate("oob" + throwLabel + ":");
        emit(llvmInstruction);
        llvmInstruction = generate("\tcall void @throw_oob()");
        emit(llvmInstruction);
        llvmInstruction = generate("\tbr label %oob" + exitLabel + "\n");
        emit(llvmInstruction);
        // Exit oob
        llvmInstruction = generate("oob" + exitLabel + ":");
        emit(llvmInstruction);
        if (mcProcedure) {
            methodCallArguments.add("%_" + currentRegister.toString());
            methodCallProcedure = true;
        }
        // Return the class type of currentMethod and the last register
        String retValue = "$int[] " + currentRegister;
        return retValue;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> "length"
     */
    public String visit(ArrayLength n, String argu) throws Exception {
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        // Return the class type of currentMethod and the last register
        String retValue = "$int " + currentRegister;
        return retValue;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( ExpressionList() )?
     * f5 -> ")"
     */
    public String visit(MessageSend n, String argu) throws Exception {
        // Check the return value (maybe both class name and calloc register)
        String className = n.f0.accept(this, argu);
        String callocRegister = "this";
        if (className.substring(0, 1).equals("$")) {
            className = className.substring(1);
            String[] splited = className.split("\\s+");
            className = splited[0];
            callocRegister = splited[1];
        }
        // Check if className is an object and, if so, find what class is
        if (SymbolTableMap.get(className) == null) {
            STClass currentClass = SymbolTableMap.get(argu);
            STMethod currentMethod = currentClass.getMethodsTypes().get(currentMethodName);
            String objectName = className;
            String regObject = className;
            className = currentMethod.getArguments().get(objectName);
            if (className == null) {
                className = currentMethod.getLocalVariable().get(objectName);
            }
            if (className == null) {
                className = currentClass.getVariablesTypes().get(objectName);
                if (className != null) {
                    Integer offset = currentClass.getVariablesOffsets().get(objectName) + 8;
                    String llvmInstruction = generate("\t%_" + addRegister() + " = getelementptr i8, i8* %this, i32 " + offset);
                    emit(llvmInstruction);
                    llvmInstruction = generate("\t%_" + addRegister() + " = bitcast i8* %_" + (currentRegister-1) + " to i32*");
                    emit(llvmInstruction);
                    regObject = "_" + currentRegister.toString();
                }
            }
            if (className == null) {
                String superClassName = currentClass.getExtendedSuperClass();
                while (superClassName != null) {
                    STClass superClass = SymbolTableMap.get(superClassName);
                    if (superClass != null) {
                        className = superClass.getVariablesTypes().get(objectName);
                        if (className != null) {
                            break;
                        }
                    }
                    superClassName = superClass.getExtendedSuperClass();
                }
            }
            String llvmInstruction = generate("\t%_" + addRegister() + " = load i8*, i8** %" + regObject);
            emit(llvmInstruction);
            //callocRegister = checkExpressionTypes(className, argu);
            callocRegister = currentRegister.toString();
        }
        // Continue with method call
        String methodName = n.f2.accept(this, argu);
        String llvmInstruction = "";
        if (callocRegister.matches("-?\\d+")) {
            llvmInstruction = generate("\t%_" + addRegister() + " = bitcast i8* %_" + callocRegister + " to i8***");
        } else {
            llvmInstruction = generate("\t%_" + addRegister() + " = bitcast i8* %" + callocRegister + " to i8***");
        }
        emit(llvmInstruction);
        llvmInstruction = generate("\t%_" + addRegister() + " = load i8**, i8*** %_" + (currentRegister-1));
        emit(llvmInstruction);
        // Find the position of method in vtable
        Integer methodIndex = 0;
        STClass currClass = SymbolTableMap.get(className);
        for (Map.Entry meth: currClass.getMethodsTypes().entrySet()) {
            if (meth.getKey().equals(methodName)) {
                break;
            }
            methodIndex++;
        }
        llvmInstruction = generate("\t%_" + addRegister() + " = getelementptr i8*, i8** %_" + (currentRegister-1) + ", i32 " + methodIndex);
        emit(llvmInstruction);
        llvmInstruction = generate("\t%_" + addRegister() + " = load i8*, i8** %_" + (currentRegister-1));
        emit(llvmInstruction);
        // Find method's arguments types
        String argumentsTypes = "";
        STClass currentClass = SymbolTableMap.get(className);
        STMethod currentMethod = currentClass.getMethodsTypes().get(methodName);
        for (Map.Entry arg: currentMethod.getArguments().entrySet()) {
            String argType = currentMethod.getArguments().get(arg.getKey());
            argumentsTypes = argumentsTypes + ", " + convertType(argType);
        }
        // Find type of call objects
        String callType = "i8*";
        if (currentMethod.getType().equals("int")) callType = "i32";
        else if (currentMethod.getType().equals("boolean")) callType = "i1";
        // Write llvm code
        llvmInstruction = generate("\t%_" + addRegister() + " = bitcast i8* %_" + (currentRegister-1) + " to " + callType + " (i8*" + argumentsTypes + ")*");
        Integer callReg = currentRegister;
        emit(llvmInstruction);
        // Find method's arguments and store their values into a list
        String argumentsTypes2 = "";
        /*ArrayList<String> temp = new ArrayList<String>(methodCallArguments);
        if (methodCallProcedure) {
            methodCallProcedure = false;
        }*/
        if (!methodCallProcedure) {
            methodCallProcedure = true;
            methodCallArguments = new ArrayList<String>();
            n.f4.accept(this, argu);
            methodCallProcedure = false;
        }
        else methodCallArguments.add(callocRegister);
        Integer argIndex = 0;
        STMethod currentMethod2 = currentClass.getMethodsTypes().get(methodName);
        for (Map.Entry arg: currentMethod2.getArguments().entrySet()) {
            String argType = currentMethod2.getArguments().get(arg.getKey());
            argumentsTypes2 = argumentsTypes2 + ", " + convertType(argType) + " " + methodCallArguments.get(argIndex);
            argIndex++;
        }
        //methodCallArguments = new ArrayList<String>(temp);
        if (callocRegister.matches("-?\\d+")) {
            llvmInstruction = generate("\t%_" + addRegister() + " = call " + callType + " %_" + callReg + "(i8* %_" + callocRegister + argumentsTypes2 + ")\n");
        } else {
            llvmInstruction = generate("\t%_" + addRegister() + " = call " + callType + " %_" + callReg + "(i8* %" + callocRegister + argumentsTypes2 + ")\n");
        }
        emit(llvmInstruction);
        // Return the class type of currentMethod and the last register
        String retValue = "$" + currentMethod.getType() + " " + currentRegister;
        return retValue;
    }

    /**
     * f0 -> "new"
     * f1 -> "int"
     * f2 -> "["
     * f3 -> Expression()
     * f4 -> "]"
     */
    public String visit(ArrayAllocationExpression n, String argu) throws Exception {
        // Check if out of bounds
        String size = n.f3.accept(this, argu);
        //size = checkExpressionTypes(size, argu);
        Integer throwLabel = addAllocLabel();
        Integer exitLabel = addAllocLabel();
        String llvmInstruction = generate("\t%_" + addRegister() + " = load i32, i32* %" + size);
        emit(llvmInstruction);
        Integer callocRegister = currentRegister;
        llvmInstruction = generate("\t%_" + addRegister() + " = icmp slt i32 %_" + (currentRegister-1) + ", 0");
        emit(llvmInstruction);
        llvmInstruction = generate("\tbr i1 %_" + currentRegister + ", label %arr_alloc" + throwLabel + ", label %arr_alloc" + exitLabel + "\n");
        emit(llvmInstruction);
        llvmInstruction = generate("arr_alloc" + throwLabel + ":");
        emit(llvmInstruction);
        llvmInstruction = generate("\tcall void @throw_oob()");
        emit(llvmInstruction);
        llvmInstruction = generate("\tbr label %arr_alloc" + exitLabel + "\n");
        emit(llvmInstruction);
        llvmInstruction = generate("arr_alloc" + exitLabel + ":");
        emit(llvmInstruction);
        // Code for new int[] array allocation
        llvmInstruction = generate("\t%_" + addRegister() + " = add i32 %_" + callocRegister + ", 1");
        emit(llvmInstruction);
        llvmInstruction = generate("\t%_" + addRegister() + " = call i8* @calloc(i32 4, i32 %_" + (currentRegister-1) + ")");
        emit(llvmInstruction);
        llvmInstruction = generate("\t%_" + addRegister() + " = bitcast i8* %_" + (currentRegister-1) + " to i32*");
        emit(llvmInstruction);
        llvmInstruction = generate("\tstore i32 %_" + callocRegister + ", i32* %_" + currentRegister);
        emit(llvmInstruction);
        llvmInstruction = generate("\t%_" + addRegister() + " = getelementptr i8, i8* %this, i32 8");
        emit(llvmInstruction);
        llvmInstruction = generate("\t%_" + addRegister() + " = bitcast i8* %_" + (currentRegister-1) + " to i32**");
        emit(llvmInstruction);
        llvmInstruction = generate("\tstore i32* %_" + (currentRegister-2) + ", i32** %_" + currentRegister + "\n");
        emit(llvmInstruction);
        // Return the class type of currentMethod and the last register
        String retValue = "$int " + currentRegister;
        return retValue;
    }

    /**
     * f0 -> "new"
     * f1 -> Identifier()
     * f2 -> "("
     * f3 -> ")"
     */
    public String visit(AllocationExpression n, String argu) throws Exception {
        // Code for new allocation (identifier -> classes)
        String objectType = n.f1.accept(this, argu);
        STClass currentClass = SymbolTableMap.get(objectType);
        Integer numOfMethods = currentClass.getMethodsTypes().size();
        String llvmInstruction = generate("\t%_" + addRegister() + " = call i8* @calloc(i32 1, i32 8)");
        emit(llvmInstruction);
        String callocRegister = currentRegister.toString();
        llvmInstruction = generate("\t%_" + addRegister() + " = bitcast i8* %_" + (currentRegister-1) + " to i8***");
        emit(llvmInstruction);
        llvmInstruction = generate("\t%_" + addRegister() + " = getelementptr [" + numOfMethods + " x i8*], [" + numOfMethods + " x i8*]* @." + objectType + "_vtable, i32 0, i32 0");
        emit(llvmInstruction);
        llvmInstruction = generate("\tstore i8** %_" + currentRegister + ", i8*** %_" + (currentRegister-1) + "\n");
        emit(llvmInstruction);
        // Return both className and calloc register
        String retValue = "$" + objectType + " " + callocRegister;
        return retValue;
    }

    /**
     * f0 -> "!"
     * f1 -> Clause()
     */
    public String visit(NotExpression n, String argu) throws Exception {
        String clause = n.f1.accept(this, argu);
        clause = checkExpressionTypes(clause, argu);
        String llvmInstruction = generate("\t%_" + addRegister() + " = xor i1 1, " + clause);
        emit(llvmInstruction);
        return "$boolean " + currentRegister.toString();
    }

    /**
     * f0 -> "("
     * f1 -> Expression()
     * f2 -> ")"
     */
    public String visit(BracketExpression n, String argu) throws Exception {
        String retValue = "";
        if (methodCallProcedure) {
            methodCallProcedure = false;
            retValue = n.f1.accept(this, argu);
            methodCallProcedure = true;
        } else {
            retValue = n.f1.accept(this, argu);
        }
        return retValue;
    }

    /**
     * f0 -> "this"
     */
    public String visit(ThisExpression n, String argu) throws Exception {
        if (methodCallProcedure) methodCallArguments.add("this");
        return argu;
    }

    /**
     * f0 -> "int"
     * f1 -> "["
     * f2 -> "]"
     */
    public String visit(ArrayType n, String argu) throws Exception {
        return "int[]";
    }

    /**
     * f0 -> "boolean"
     */
    public String visit(BooleanType n, String argu) throws Exception {
        return "boolean";
    }

    /**
     * f0 -> "int"
     */
    public String visit(IntegerType n, String argu) throws Exception {
        return "int";
    }

    /**
     * f0 -> <INTEGER_LITERAL>
     */
    public String visit(IntegerLiteral n, String argu) throws Exception {
        String retValue = n.f0.toString();
        if (methodCallProcedure) methodCallArguments.add(retValue);
        return retValue;
    }

    /**
     * f0 -> "true"
     */
    public String visit(TrueLiteral n, String argu) throws Exception {
        String retValue = n.f0.toString();
        if (methodCallProcedure) methodCallArguments.add("1");
        return retValue;
    }

    /**
     * f0 -> "false"
     */
    public String visit(FalseLiteral n, String argu) throws Exception {
        String retValue = n.f0.toString();
        if (methodCallProcedure) methodCallArguments.add("0");
        return retValue;
    }

    /**
     * f0 -> <IDENTIFIER>
     */
    public String visit(Identifier n, String argu) throws Exception {
        String retValue = n.f0.toString();
        String expressionRegister = retValue;
        if (methodCallProcedure) {
            STClass currentClass = SymbolTableMap.get(argu);
            STMethod currentMethod = currentClass.getMethodsTypes().get(currentMethodName);
            String objectName = retValue;
            //String objectType = currentClass.getVariablesTypes().get(objectName);
            String objectType = currentMethod.getArguments().get(objectName);
            if (objectType == null) {
                objectType = currentMethod.getLocalVariable().get(objectName);
            }
            if (objectType == null) {
                objectType = currentClass.getVariablesTypes().get(objectName);
                if (objectType != null) {
                    //if (!objectType.equals("int[]")) {
                        Integer offset = currentClass.getVariablesOffsets().get(objectName) + 8;
                        String llvmInstruction = generate("\t%_" + addRegister() + " = getelementptr i8, i8* %this, i32 " + offset);
                        emit(llvmInstruction);
                        String llvmType = "";
                        if (objectType.equals("int")) llvmType = "i32*";
                        else if (objectType.equals("int[]")) llvmType = "i32**";
                        else if (objectType.equals("boolean")) llvmType = "i1*";
                        else llvmType = "i8**";
                        llvmInstruction = generate("\t%_" + addRegister() + " = bitcast i8* %_" + (currentRegister-1) + " to " + llvmType);
                        emit(llvmInstruction);
                        expressionRegister = "%_" + currentRegister.toString();
                    //}
                }
            } else expressionRegister = "%" + expressionRegister;
            // Find type of call objects
            String callType = "i8*";
            //if (objectType == null) objectType = retValue;
            if (objectType != null) {
                retValue = objectType;
                if (objectType.equals("int")) callType = "i32";
                else if (objectType.equals("boolean")) callType = "i1";
                String llvmInstruction = generate("\t%_" + addRegister() + " = load " + callType + ", " + callType + "* " + expressionRegister);
                emit(llvmInstruction);
                methodCallArguments.add("%_" + currentRegister.toString());
            }
        }
        return retValue;
    }

}
