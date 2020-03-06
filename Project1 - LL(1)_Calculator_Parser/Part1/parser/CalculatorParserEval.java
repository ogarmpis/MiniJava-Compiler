import java.io.InputStream;
import java.io.IOException;


class CalculatorParserEval {

    private int lookaheadToken;

    private InputStream in;

    public CalculatorParserEval(InputStream in) throws IOException {
    	this.in = in;
    	lookaheadToken = in.read();
    }

    private void consume(int symbol) throws IOException, ParseError {
    	if (lookaheadToken != symbol) {
    	    throw new ParseError();
        }
    	lookaheadToken = in.read();
    }

    private int evalDigit(int digit){
	    return digit - '0';
    }

    private int expr() throws IOException, ParseError {
        int value = term();
        return expr2(value);
    }

    private int expr2(int leftValue) throws IOException, ParseError {
        if (lookaheadToken == ')' || lookaheadToken == '\n' || lookaheadToken == -1) {
            return leftValue;
        }
        if (lookaheadToken != '^') {
            throw new ParseError();
        }
        consume('^');
        int rightValue = term();
        int result = leftValue ^ expr2(rightValue);
        return result;
    }

    private int term() throws IOException, ParseError {
        int value = factor();
        return term2(value);
    }

    private int term2(int leftValue) throws IOException, ParseError {
        if (lookaheadToken == '^' || lookaheadToken == ')' || lookaheadToken == '\n' || lookaheadToken == -1) {
            return leftValue;
        }
        if (lookaheadToken != '&') {
            throw new ParseError();
        }
        consume('&');
        int rightValue = factor();
        int result = leftValue & term2(rightValue);
        return result;
    }

    private int factor() throws IOException, ParseError {
        if (lookaheadToken == '(') {
            consume(lookaheadToken);
            int expression = expr();
            if (lookaheadToken == ')') {
                consume(lookaheadToken);
                return expression;
            }
            else throw new ParseError();
        }
        if (lookaheadToken < '0' || lookaheadToken > '9') {
            throw new ParseError();
        }
        int number = evalDigit(lookaheadToken);
        consume(lookaheadToken);
        return number;
    }

    public int goal() throws IOException, ParseError {
    	int rv = expr();
    	if (lookaheadToken != '\n' && lookaheadToken != -1) {
    	    throw new ParseError();
        }
    	return rv;
    }

    public static void main(String[] args) {
    	try {
    	    CalculatorParserEval evaluate = new CalculatorParserEval(System.in);
    	    System.out.println(evaluate.goal());
    	}
    	catch (IOException e) {
    	    System.err.println(e.getMessage());
    	}
    	catch(ParseError err){
    	    System.err.println(err.getMessage());
    	}
    }
}
