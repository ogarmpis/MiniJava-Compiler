import java.util.*;


public class STClass {
    private String className;
    private Map<String, Integer> variablesOffsets;
    private Map<String, Integer> methodsOffsets;
    private Integer currentVariableOffset;
    private Integer currentMethodOffset;
    private String extendedSuperClass;
    private boolean isMainClass;

    private Map<String, String> variablesTypes;
    private Map<String, STMethod> methodsTypes;

    static final Integer intOffset = 4;
    static final Integer intArrayOffset = 8;
    static final Integer booleanOffset = 1;
    static final Integer pointerOffset = 8;

    public STClass(String name) {
        // Initialise arguments for offsets
        className = name;
        variablesOffsets = new LinkedHashMap<String, Integer>();
        methodsOffsets = new LinkedHashMap<String, Integer>();
        currentVariableOffset = 0;
        currentMethodOffset = 0;
        extendedSuperClass = null;
        isMainClass = false;
        // Initialize arguments for types
        variablesTypes = new LinkedHashMap<String, String>();
        methodsTypes = new LinkedHashMap<String, STMethod>();
    }

    public String getClassName() {
        return className;
    }
    public Map<String, Integer> getVariablesOffsets() {
        return variablesOffsets;
    }
    public Map<String, Integer> getMethodsOffsets() {
        return methodsOffsets;
    }
    public void setExtendedSuperClass(String className) {
        extendedSuperClass = className;
    }
    public String getExtendedSuperClass() {
        return extendedSuperClass;
    }
    public void setCurrentVariableOffset(Integer initOffset) {
        currentVariableOffset = initOffset;
    }
    public Integer getCurrentVariableOffset() {
        return currentVariableOffset;
    }
    public void setCurrentMethodOffset(Integer initOffset) {
        currentMethodOffset = initOffset;
    }
    public Integer getCurrentMethodOffset() {
        return currentMethodOffset;
    }
    public void setIsMainClass() {
        isMainClass = true;
    }
    public boolean getIsMainClass() {
        return isMainClass;
    }

    public Map<String, String> getVariablesTypes() {
        return variablesTypes;
    }
    public Map<String, STMethod> getMethodsTypes() {
        return methodsTypes;
    }

    // Insert variables offsets into map (linked hashmap so as to keep the initial order)
    public void insertVariableOffset(String type, String varName) {
        Integer offset = pointerOffset;
        if (type.equals("int")) {
            offset = intOffset;
        }
        else if (type.equals("int[]")) {
            offset = intArrayOffset;
        }
        else if (type.equals("boolean")) {
            offset = booleanOffset;
        }
        variablesOffsets.put(varName, currentVariableOffset);
        currentVariableOffset += offset;
    }

    // Insert variables types into map
    public void insertVariableType(String type, String varName) throws Exception {
        if (!variablesTypes.containsKey(varName)) {
            variablesTypes.put(varName, type);
        }
        else throw new Exception("Variable \"" + varName + "\" has already been declared");
    }

    // Same as above, now for methods offsets
    public void insertMethodOffset(String methodName) {
        methodsOffsets.put(methodName, currentMethodOffset);
        currentMethodOffset += pointerOffset;
    }

    // Same as above, now for methods types, arguments and more..
    public void insertMethodType(String methodName, STMethod methodClass) throws Exception {
        if (!methodsTypes.containsKey(methodName)) {
            methodsTypes.put(methodName, methodClass);
        }
        else throw new Exception("Method \"" + methodName + "\" has already been declared in this class");
    }
}
