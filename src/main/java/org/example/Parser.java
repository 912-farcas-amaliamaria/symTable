package org.example;

import lombok.Getter;

import java.io.*;
import java.util.ArrayList;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
public class Parser {
    //-2 in PIF
    private SymbolTable symbolTableConstants;
    //-1 in PIF
    private SymbolTable symbolTableIdentifiers;

    private final Integer operatorsIndexStart = 1;
    private final Integer separatorsIndexStart = 14;
    private final Integer reservedWordsIndexStart = 19;
    private ArrayList<Pair<Integer, Tuple>> PIF;
    private ArrayList<String> separators;
    private ArrayList<String> operators;
    private ArrayList<String> reservedWords;
    private FiniteAutomata finiteAutomataIdentifier;
    private FiniteAutomata finiteAutomataInt;
    private int currentLine;


    private static final Tuple NULLTUPLE = new Tuple(-1, -1);

    public Parser(FiniteAutomata finiteAutomataIdentifier, FiniteAutomata finiteAutomataInt) {
        this.currentLine = 1;
        this.operators = parseTokensIn(operatorsIndexStart, separatorsIndexStart-1);
        this.separators = parseTokensIn(separatorsIndexStart, reservedWordsIndexStart-1);
        this.reservedWords = parseTokensIn(reservedWordsIndexStart, 27);
        this.separators.add(" ");
        this.symbolTableConstants = new SymbolTable();
        this.symbolTableIdentifiers = new SymbolTable();
        this.PIF = new ArrayList<>();
        this.finiteAutomataIdentifier = finiteAutomataIdentifier;
        this.finiteAutomataInt = finiteAutomataInt;
    }

    public void scan(String filePath){
        this.currentLine = 1;
        // Create a Scanner to read from the file
        InputStream inputStream = Parser.class.getResourceAsStream(filePath);

        if (inputStream == null) {
            System.err.println("Resource file not found.");
            return;
        }
        java.util.Scanner scanner = new java.util.Scanner(inputStream);

        // Use whitespace as the default delimiter (can be customized)
        scanner.useDelimiter("");
        StringBuilder input = new StringBuilder();

        // Read and print tokens one by one
        while (scanner.hasNext()) {
            String token = scanner.next();
            if (token.equals("\r") || token.equals("\n") || token.equals("\t")) {
                if (token.equals("\r"))
                    this.currentLine++;
            }
            else if (isChar(token) || token.equals("_")) {
                input.append(token);
            }
            else{
                if(!input.isEmpty()) {
                    try{
                        this.searchForIdentConstReserved(String.valueOf(input));
                        input = new StringBuilder();
                    }
                    catch (RuntimeException e){
                        System.out.println(e.getMessage());
                        throw new RuntimeException("Lexical error on line "+ currentLine + " in "+filePath + " token " + input);
                    }
                }
                //input = new StringBuilder();
                //check if String
                if(token.equals( "\"")){
                    input.append(token);
                    token = scanner.next();
                    while (!token.equals("\"")) {
                        input.append(token);
                        if(!scanner.hasNext()) {
                            throw new RuntimeException("Lexical error on line "+ currentLine + " in "+filePath + " token " + input);
                        }
                        token = scanner.next();
                    }
                    input.append(token);
                    Tuple pos = symbolTableConstants.add(String.valueOf(input));
                    PIF.add(new Pair<>(-2, pos));
                    input = new StringBuilder();
                }
                //check if Char
                else if(token.equals("'")){
                    input.append(token);
                    token = scanner.next();
                    input.append(token);
                    token=scanner.next();
                    input.append(token);

                    if(!Objects.equals(token, "'")){
                        throw new RuntimeException("Lexical error on line "+ currentLine + " in "+filePath + " token " + input);
                    }

                    Tuple pos = symbolTableConstants.add(String.valueOf(input));
                    PIF.add(new Pair<>(-2, pos));
                    input = new StringBuilder();
                }
                else if(operators.contains(token)){
                    input.append(token);
                    token = scanner.next();
                    if(!((String.valueOf(input).equals("<") && token.equals("-"))
                            || (String.valueOf(input).equals("-") && token.equals(">"))
                            || (String.valueOf(input).equals("!") && token.equals("="))
                            || (String.valueOf(input).equals("<") && token.equals("="))
                            || (String.valueOf(input).equals("=") && token.equals(">")))) {
                        int pos = this.operators.indexOf(String.valueOf(input)) + operatorsIndexStart;
                        PIF.add(new Pair<>(pos, NULLTUPLE));
                        input = new StringBuilder();
                        if(!token.equals(" "))
                            input.append(token);
                    }
                    else {
                        input.append(token);
                        int pos = this.operators.indexOf(String.valueOf(input)) + operatorsIndexStart;
                        PIF.add(new Pair<>(pos, NULLTUPLE));
                        input = new StringBuilder();
                    }
                }
                else if(separators.contains(token)){
                    if(!token.equals(" ")) {
                        int pos = this.separators.indexOf(token) + separatorsIndexStart;
                        PIF.add(new Pair<>(pos, NULLTUPLE));
                    }
                    input = new StringBuilder();
                }
                else{
                    throw new RuntimeException("Lexical error on line "+ currentLine + " in "+filePath + " token " + token);
                }
            }
        }

        // Close the scanner when done
        scanner.close();
    }

    void searchForIdentConstReserved(String input){
        if (this.reservedWords.contains(input)) {
            int pos = this.reservedWords.indexOf(input) + reservedWordsIndexStart;
            PIF.add(new Pair<>(pos, NULLTUPLE));
        } else if (isIdentifier(input)) {
            Tuple pos = symbolTableIdentifiers.add(input);
            PIF.add(new Pair<>(-1, pos));
        } else if (isInteger(input)) {
            Tuple pos = symbolTableConstants.add(input);
            PIF.add(new Pair<>(-2, pos));
        } else {
            throw new RuntimeException();
        }
    }

    boolean isIdentifier(String input){
        /*String pattern = "^[a-z]+";  // Matches a string composed of one or more lowercase letters
        Pattern compiledPattern = Pattern.compile(pattern);
        Matcher matcher = compiledPattern.matcher(input);
        return matcher.matches();*/
        return finiteAutomataIdentifier.checkItem(input);
    }

    boolean isChar(String token) {
        String patterLetter = "[a-zA-Z]";
        String patternDigit = "\\d";
        Pattern compiledPatternLetter = Pattern.compile(patterLetter);
        Matcher matcherLetter = compiledPatternLetter.matcher(token);

        Pattern compiledPatternDigit = Pattern.compile(patternDigit);
        Matcher matcherDigit = compiledPatternDigit.matcher(token);

        return matcherDigit.matches() || matcherLetter.matches();
    }

    boolean isInteger(String token) {
        /*String patternDigit = "\\d+";
        Pattern compiledPatternDigit = Pattern.compile(patternDigit);
        Matcher matcherDigit = compiledPatternDigit.matcher(token);

        return matcherDigit.matches();*/
        return finiteAutomataInt.checkItem(token);
    }

    public ArrayList<String> parseTokensIn(int start, int end){

        ArrayList<String> lines = new ArrayList<>();

        // Use the class loader to get the input stream for the resource file
        InputStream inputStream = Parser.class.getResourceAsStream("/token.in");

        if (inputStream == null) {
            System.err.println("Resource file not found.");
            return lines;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream)) ) {
            String line;
            int lineNumber = 0;

            // Read lines from the file and add them to the list
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (lineNumber >= start && lineNumber <= end) {
                    lines.add(line);
                }
                if (lineNumber > end) {
                    break; // Stop reading after line 20
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return lines;
    }

    public void writePIF(String filePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (Pair<Integer, Tuple> pair: PIF) {
                writer.write(pair.toStringPIF());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
