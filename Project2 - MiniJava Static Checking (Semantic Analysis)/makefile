all: compile

compile:
	java -jar ./lib/jtb132di.jar -te minijava.jj
	java -jar ./lib/javacc5.jar minijava-jtb.jj
	javac Main.java

clean:
	rm -f *.class *~

cleanAll:
	rm -f *.class *~ minijava-jtb.jj
	rm -f JavaCharStream.java ParseException.java Token.java TokenMgrError.java
	rm -f MiniJavaParser.java MiniJavaParserConstants.java MiniJavaParserTokenManager.java
	rm -rf visitor
	rm -rf syntaxtree
