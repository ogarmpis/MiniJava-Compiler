all: compile

compile:
	jflex scanner.flex
	java -jar ./lib/java-cup-11b.jar -interface -parser Parser parser.cup
	javac -cp ./lib/java-cup-11b-runtime.jar *.java

execute:
	@java -cp ./lib/java-cup-11b-runtime.jar:. Main

clean:
	rm -f *.class *~

cleanAll:
	rm -f *.class *~
	rm -f Parser.java Scanner.java sym.java
	rm -rf ./outputs
