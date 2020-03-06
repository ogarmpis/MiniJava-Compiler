import java.util.*;


public class STMethod {
    private String methodName;
    private String type;
    private Map<String, String> arguments;
    private Map<String, String> localVariables;

    public STMethod(String name, String t) {
        methodName = name;
        type = t;
        arguments = new LinkedHashMap<String, String>();
        localVariables = new LinkedHashMap<String, String>();
    }

    public String getMethodName() {
        return methodName;
    }
    public String getType() {
        return type;
    }
    public Map<String, String> getArguments() {
        return arguments;
    }
    public Map<String, String> getLocalVariable() {
        return localVariables;
    }

    public void setArgument(String type, String name) throws Exception {
        if (arguments.containsKey(name)) {
            throw new Exception("Argument \"" + name + "\" has already been declared");
        }
        else arguments.put(name, type);
    }

    public void setLocalVariable(String type, String name) throws Exception {
        if (arguments.containsKey(name)) {
            throw new Exception("Variable name \"" + name + "\" has been used as argument name");
        }
        else if (localVariables.containsKey(name)) {
            throw new Exception("Argument \"" + name + "\" has already been declared");
        }
        else localVariables.put(name, type);
    }
}
