import syntaxtree.*;
import visitor.GJNoArguDepthFirst;
import java.util.*;


public class TCVisitor extends GJNoArguDepthFirst<String> {

    private Map<String, STClass> SymbolTableMap;
    private String currentClassName;
    private String currentMethodName;
    private List<String> methodArguments;

    static final String identifierRecognitionSymbol = "#";

    // Initialize Symbol Table Map on constructor
    public TCVisitor(Map<String, STClass> STMap) {
        SymbolTableMap = new LinkedHashMap<String, STClass>(STMap);
        methodArguments = new LinkedList<String>();
        currentClassName = null;
        currentMethodName = null;
    }

    // Find the type of an identifier
    public String findIdentifierType(String identName, STClass currentClass, STMethod currentMethod) throws Exception {
        //System.out.println("hiiii4444");
        String type = currentMethod.getLocalVariable().get(identName);
        //System.out.println("hiiii3333");
        if (type == null) {
            type = currentMethod.getArguments().get(identName);
        }
        if (type == null) {
            type = currentClass.getVariablesTypes().get(identName);
        }
        if (type == null) {
            String superClassName = currentClass.getExtendedSuperClass();
            while (superClassName != null) {
                STClass superClass = SymbolTableMap.get(superClassName);
                if (superClass != null) {
                    if (superClass.getVariablesTypes().containsKey(identName)) {
                        type = superClass.getVariablesTypes().get(identName);
                        break;
                    }
                }
                superClassName = superClass.getExtendedSuperClass();
            }
        }
        if (type == null) {
            throw new Exception("Identifier \"" + identName + "\" does not exists");
        }
        else return type;
    }

    // Check if the right identifier is a derived type of the left one's type
    public boolean checkBaseDerivedRelated(String leftType, String rightType) {
        STClass rightClassItem = SymbolTableMap.get(rightType);
        String superClassName = rightClassItem.getExtendedSuperClass();
        while (superClassName != null) {
            STClass superClass = SymbolTableMap.get(superClassName);
            if (superClass != null) {
                if (superClass.getClassName().equals(leftType)) {
                    return true;
                }
            }
            superClassName = superClass.getExtendedSuperClass();
        }
        return false;
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
        currentClassName = n.f1.accept(this).substring(1);
        currentMethodName = "main";
        n.f15.accept(this);
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
    public String visit(ClassDeclaration n) throws Exception {
        currentClassName = n.f1.accept(this).substring(1);
        n.f4.accept(this);
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
        currentClassName = n.f1.accept(this).substring(1);
        n.f6.accept(this);
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
        currentMethodName = n.f2.accept(this).substring(1);
        String methodType = n.f1.accept(this);
        if (methodType.substring(0, 1).equals("#")) {
            methodType = methodType.substring(1);
        }
        n.f8.accept(this);
        // Return type
        String returnType = n.f10.accept(this);
        if (returnType.substring(0, 1).equals("#")) {
            String expName = returnType.substring(1);
            STClass currentClass = SymbolTableMap.get(currentClassName);
            STMethod currentMethod = currentClass.getMethodsTypes().get(currentMethodName);
            returnType = findIdentifierType(expName, currentClass, currentMethod);
        }
        //System.out.println(methodType + " " + returnType);
        if (!returnType.equals(methodType)) {
            throw new Exception("Method \"" + currentMethodName + "\" has wrong return type");
        }
        return null;
    }

    /**
     * f0 -> "int"
     * f1 -> "["
     * f2 -> "]"
     */
    public String visit(ArrayType n) throws Exception {
       n.f0.accept(this);
       n.f1.accept(this);
       n.f2.accept(this);
       return "int[]";
    }

    /**
     * f0 -> "boolean"
     */
    public String visit(BooleanType n) throws Exception {
       n.f0.accept(this);
       return "boolean";
    }

    /**
     * f0 -> "int"
     */
    public String visit(IntegerType n) throws Exception {
       n.f0.accept(this);
       return "int";
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Expression()
     * f3 -> ";"
     */
    public String visit(AssignmentStatement n) throws Exception {
        // Find the name and type of identifier (left)
        String identName = n.f0.accept(this).substring(1);
        STClass currentClass = SymbolTableMap.get(currentClassName);
        STMethod currentMethod = currentClass.getMethodsTypes().get(currentMethodName);
        String leftType = findIdentifierType(identName, currentClass, currentMethod);
        // Find expression's type (right), if this is an identifier, then find its type from maps (Identifier returns name, not type)
        String rightType = n.f2.accept(this);
        if (rightType != null) {
            if (rightType.substring(0, 1).equals("#")) {
                String expName = rightType.substring(1);
                if (expName.equals("this")) {
                    rightType = currentClassName;
                }
                else rightType = findIdentifierType(expName, currentClass, currentMethod);
            }
            // Compare the two types
            if (!leftType.equals(rightType)) {
                if (!rightType.equals("int") && !rightType.equals("int[]") && !rightType.equals("boolean") && !rightType.equals("String[]")) {
                    if (checkBaseDerivedRelated(leftType, rightType)) {
                        return null;
                    }
                }
                throw new Exception("Identifier \"" + identName + "\" is type of " + leftType + " and the term which is assigned has type " + rightType);
            }
        }
        return null;
    }

    /**
     * f0 -> Clause()
     * f1 -> "&&"
     * f2 -> Clause()
     */
    public String visit(AndExpression n) throws Exception {
        String clauseLeft = n.f0.accept(this);
        String clauseRight = n.f2.accept(this);
        //System.out.println(clauseLeft + " " + clauseRight);
        return "boolean";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "<"
     * f2 -> PrimaryExpression()
     */
    public String visit(CompareExpression n) throws Exception {
        n.f0.accept(this);
        n.f2.accept(this);
        return "boolean";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "+"
     * f2 -> PrimaryExpression()
     */
    public String visit(PlusExpression n) throws Exception {
        n.f0.accept(this);
        n.f2.accept(this);
        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "-"
     * f2 -> PrimaryExpression()
     */
    public String visit(MinusExpression n) throws Exception {
        n.f0.accept(this);
        n.f2.accept(this);
        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "*"
     * f2 -> PrimaryExpression()
     */
    public String visit(TimesExpression n) throws Exception {
        n.f0.accept(this);
        n.f2.accept(this);
        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "["
     * f2 -> PrimaryExpression()
     * f3 -> "]"
     */
    public String visit(ArrayLookup n) throws Exception {
        n.f0.accept(this);
        String insidePrimExp = n.f2.accept(this);
        /*if (!insidePrimExp.equals("int") && insidePrimExp != null) {
            throw new Exception("helooooooooooooooo");
        }*/
        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> "length"
     */
    public String visit(ArrayLength n) throws Exception {
        n.f0.accept(this);
        n.f2.accept(this);
        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( ExpressionList() )?
     * f5 -> ")"
     */
    public String visit(MessageSend n) throws Exception {
        // Check the PrimaryExpression
        String primExpName = n.f0.accept(this);
        //System.out.println(primExpName);
        if (n.f0.accept(this).substring(0, 1).equals("#")) {
            primExpName = n.f0.accept(this).substring(1);
        }
        STClass currentClass = SymbolTableMap.get(currentClassName);
        STMethod currentMethod = currentClass.getMethodsTypes().get(currentMethodName);
        String type = primExpName;
        if (type.equals("this")) {
            type = currentClassName;
        }
        if (!SymbolTableMap.containsKey(type)) {
            type = findIdentifierType(primExpName, currentClass, currentMethod);
        }
        // Check the Identifier type
        String identName = n.f2.accept(this).substring(1);
        currentClass = SymbolTableMap.get(type);
        currentMethod = currentClass.getMethodsTypes().get(identName);
        if (currentMethod == null) {
            String superClassName = currentClass.getExtendedSuperClass();
            while (superClassName != null) {
                STClass superClass = SymbolTableMap.get(superClassName);
                if (superClass != null) {
                    currentMethod = superClass.getMethodsTypes().get(identName);
                    if (currentMethod != null) {
                        break;
                    }
                }
                superClassName = superClass.getExtendedSuperClass();
            }
        }
        if (currentMethod == null) {
            throw new Exception("There is no method \"" + identName + "\" in class \"" + type + "\"");
        }
        type = currentMethod.getType();
        // Finally check the argument list
        /*methodArguments = new LinkedList<String>();
        n.f4.accept(this);
        //System.out.println(methodArguments + " " + currentMethod.getArguments());
        if (methodArguments.size() != currentMethod.getArguments().size()) {
            throw new Exception("Method \"" + identName + "\" has " + methodArguments.size() + " arguments while expected " + currentMethod.getArguments().size());
        }
        STClass currentClassItem = SymbolTableMap.get(currentClassName);
        STMethod currentMethodItem = currentClassItem.getMethodsTypes().get(currentMethodName);
        Integer i = 0;
        for (Map.Entry m: currentMethod.getArguments().entrySet()) {
        //for (int i = 0; i < methodArguments.size(); i++) {
            String methArg = methodArguments.get(i);
            String typeItem = methArg;
            if (methArg.substring(0, 1).equals("#")) {
                methArg = methArg.substring(1);
                //System.out.println("hiiii1111 " + methArg + currentClassItem + currentMethodItem);
                typeItem = findIdentifierType(methArg, currentClassItem, currentMethodItem);
                //System.out.println("hiiii2222");
            }
            String currArg = currentMethod.getArguments().get(m.getKey());
            //System.out.println(typeItem + " " + currArg);
            if (!typeItem.equals(currArg)) {
                throw new Exception("Method \"" + identName + "\" has different types of arguments");
            }
            i += 1;
        }
        //System.out.println(methodArguments);*/
        return type;
    }

    /**
     * f0 -> Expression()
     * f1 -> ExpressionTail()
     */
    public String visit(ExpressionList n) throws Exception {
        methodArguments.add(n.f0.accept(this));
        n.f1.accept(this);
        return null;
    }

    /**
     * f0 -> ","
     * f1 -> Expression()
     */
    public String visit(ExpressionTerm n) throws Exception {
        methodArguments.add(n.f1.accept(this));
        return null;
    }

    /**
     * f0 -> <INTEGER_LITERAL>
     */
    public String visit(IntegerLiteral n) throws Exception {
        return "int";
    }

    /**
     * f0 -> "true"
     */
    public String visit(TrueLiteral n) throws Exception {
        return "boolean";
    }

    /**
     * f0 -> "false"
     */
    public String visit(FalseLiteral n) throws Exception {
        return "boolean";
    }

    /**
     * f0 -> <IDENTIFIER>
     */
    public String visit(Identifier n) throws Exception {
        String ident = identifierRecognitionSymbol + n.f0.toString();
        return ident;
    }

    /**
     * f0 -> "this"
     */
    public String visit(ThisExpression n) throws Exception {
        //return identifierRecognitionSymbol + "this";
        return currentClassName;
    }

    /**
     * f0 -> "new"
     * f1 -> "int"
     * f2 -> "["
     * f3 -> Expression()
     * f4 -> "]"
     */
    public String visit(ArrayAllocationExpression n) throws Exception {
       n.f3.accept(this);
       return "int[]";
    }

    /**
     * f0 -> "new"
     * f1 -> Identifier()
     * f2 -> "("
     * f3 -> ")"
     */
    public String visit(AllocationExpression n) throws Exception {
        return n.f1.accept(this).substring(1);
    }

    /**
     * f0 -> "!"
     * f1 -> Clause()
     */
    public String visit(NotExpression n) throws Exception {
       n.f1.accept(this);
       return "boolean";
    }

    /**
     * f0 -> "("
     * f1 -> Expression()
     * f2 -> ")"
     */
    public String visit(BracketExpression n) throws Exception {
        return n.f1.accept(this);
    }

}
