package org.example;

import lombok.Getter;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
public class Parser {
    private static final Tuple NULLTUPLE = new Tuple(-1, -1);
    private final Integer operatorsIndexStart = 1;
    private final Integer separatorsIndexStart = 14;
    private final Integer reservedWordsIndexStart = 19;
    Grammar grammar;
    //-2 in PIF
    private SymbolTable symbolTableConstants;
    //-1 in PIF
    private SymbolTable symbolTableIdentifiers;
    private ArrayList<Pair<Integer, Tuple>> PIF;
    private ArrayList<String> separators;
    private ArrayList<String> operators;
    private ArrayList<String> reservedWords;
    private FiniteAutomata finiteAutomataIdentifier;
    private FiniteAutomata finiteAutomataInt;
    private int currentLine;
    private ParseTable parsingTable = new ParseTable();
    private Map<String, Set<String>> firstSets;
    private Map<String, Set<String>> followSets;

    public Parser(FiniteAutomata finiteAutomataIdentifier, FiniteAutomata finiteAutomataInt, String filePathGrammar) {
        this.currentLine = 1;
        this.operators = parseTokensIn(operatorsIndexStart, separatorsIndexStart - 1);
        this.separators = parseTokensIn(separatorsIndexStart, reservedWordsIndexStart - 1);
        this.reservedWords = parseTokensIn(reservedWordsIndexStart, 27);
        this.separators.add(" ");
        this.symbolTableConstants = new SymbolTable();
        this.symbolTableIdentifiers = new SymbolTable();
        this.PIF = new ArrayList<>();
        this.finiteAutomataIdentifier = finiteAutomataIdentifier;
        this.finiteAutomataInt = finiteAutomataInt;

        grammar = new Grammar(filePathGrammar);

        scan("/in/p1.txt");
        calculateFirstSets();
        calculateFollowSets();
        createParsingTable();

    }

    public static void writeToFile(StringBuilder stringBuilder) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("parserOut.txt"))) {
            writer.write(stringBuilder.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void scan(String filePath) {
        this.currentLine = 1;
        InputStream inputStream = Parser.class.getResourceAsStream(filePath);

        if (inputStream == null) {
            System.err.println("Resource file not found.");
            return;
        }
        java.util.Scanner scanner = new java.util.Scanner(inputStream);

        scanner.useDelimiter("");
        StringBuilder input = new StringBuilder();

        while (scanner.hasNext()) {
            String token = scanner.next();
            if (token.equals("\r") || token.equals("\n") || token.equals("\t")) {
                if (token.equals("\r"))
                    this.currentLine++;
            } else if (isChar(token) || token.equals("_")) {
                input.append(token);
            } else {
                if (!input.isEmpty()) {
                    try {
                        this.searchForIdentConstReserved(String.valueOf(input));
                        input = new StringBuilder();
                    } catch (RuntimeException e) {
                        System.out.println(e.getMessage());
                        throw new RuntimeException("Lexical error on line " + currentLine + " in " + filePath + " token " + input);
                    }
                }

                if (token.equals("\"")) {
                    input.append(token);
                    token = scanner.next();
                    while (!token.equals("\"")) {
                        input.append(token);
                        if (!scanner.hasNext()) {
                            throw new RuntimeException("Lexical error on line " + currentLine + " in " + filePath + " token " + input);
                        }
                        token = scanner.next();
                    }
                    input.append(token);
                    Tuple pos = symbolTableConstants.add(String.valueOf(input));
                    PIF.add(new Pair<>(-2, pos));
                    input = new StringBuilder();
                }

                else if (token.equals("'")) {
                    input.append(token);
                    token = scanner.next();
                    input.append(token);
                    token = scanner.next();
                    input.append(token);

                    if (!Objects.equals(token, "'")) {
                        throw new RuntimeException("Lexical error on line " + currentLine + " in " + filePath + " token " + input);
                    }

                    Tuple pos = symbolTableConstants.add(String.valueOf(input));
                    PIF.add(new Pair<>(-2, pos));
                    input = new StringBuilder();
                } else if (operators.contains(token)) {
                    input.append(token);
                    token = scanner.next();
                    if (!((String.valueOf(input).equals("<") && token.equals("-"))
                            || (String.valueOf(input).equals("-") && token.equals(">"))
                            || (String.valueOf(input).equals("!") && token.equals("="))
                            || (String.valueOf(input).equals("<") && token.equals("="))
                            || (String.valueOf(input).equals("=") && token.equals(">")))) {
                        int pos = this.operators.indexOf(String.valueOf(input)) + operatorsIndexStart;
                        PIF.add(new Pair<>(pos, NULLTUPLE));
                        input = new StringBuilder();
                        if (!token.equals(" "))
                            input.append(token);
                    } else {
                        input.append(token);
                        int pos = this.operators.indexOf(String.valueOf(input)) + operatorsIndexStart;
                        PIF.add(new Pair<>(pos, NULLTUPLE));
                        input = new StringBuilder();
                    }
                } else if (separators.contains(token)) {
                    if (!token.equals(" ")) {
                        int pos = this.separators.indexOf(token) + separatorsIndexStart;
                        PIF.add(new Pair<>(pos, NULLTUPLE));
                    }
                    input = new StringBuilder();
                } else {
                    throw new RuntimeException("Lexical error on line " + currentLine + " in " + filePath + " token " + token);
                }
            }
        }

        scanner.close();
    }

    void searchForIdentConstReserved(String input) {
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

    boolean isIdentifier(String input) {
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

    public ArrayList<String> parseTokensIn(int start, int end) {

        ArrayList<String> lines = new ArrayList<>();

        InputStream inputStream = Parser.class.getResourceAsStream("/token.in");

        if (inputStream == null) {
            System.err.println("Resource file not found.");
            return lines;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (lineNumber >= start && lineNumber <= end) {
                    lines.add(line);
                }
                if (lineNumber > end) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return lines;
    }

    public void writePIF(String filePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (Pair<Integer, Tuple> pair : PIF) {
                writer.write(pair.toStringPIF());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void calculateFirstSets() {
        firstSets = new HashMap<>();
        for (String nonterminal : grammar.getNonterminals()) {
            firstSets.put(nonterminal, new HashSet<>());
        }

        boolean changed;
        do {
            changed = false;
            for (String nonterminal : grammar.getNonterminals()) {
                Set<String> firstSet = firstSets.get(nonterminal);
                int initialSize = firstSet.size();
                for (List<String> production : grammar.getProduction().get(nonterminal)) {
                    if (production.isEmpty()) {
                        // Handle epsilon
                        firstSet.add("ε");
                        continue;
                    }

                    String firstSymbol = production.get(0);
                    if (grammar.getTerminals().contains(firstSymbol)) {
                        firstSet.add(firstSymbol);
                    } else if (grammar.getNonterminals().contains(firstSymbol)) {

                        firstSet.addAll(firstSets.get(firstSymbol));
                        firstSet.remove("ε");
                    }

                    if (allCanDeriveEpsilon(production)) {
                        firstSet.add("ε");
                    }
                }
                if (firstSet.size() != initialSize) {
                    changed = true;
                }
            }
        } while (changed);
    }

    private boolean allCanDeriveEpsilon(List<String> symbols) {
        for (String symbol : symbols) {
            if (!firstSets.getOrDefault(symbol, Collections.emptySet()).contains("ε")) {
                return false;
            }
        }
        return true;
    }

    private void calculateFollowSets() {
        followSets = new HashMap<>();
        for (String nonterminal : grammar.getNonterminals()) {
            followSets.put(nonterminal, new HashSet<>());
        }

        followSets.get(grammar.getInitialState()).add("$");

        boolean changed;
        do {
            changed = false;

            for (Map.Entry<String, Set<List<String>>> entry : grammar.getProduction().entrySet()) {
                String lhs = entry.getKey();

                for (List<String> prod : entry.getValue()) {
                    for (int i = 0; i < prod.size(); i++) {
                        String symbol = prod.get(i);
                        if (grammar.getNonterminals().contains(symbol)) {

                            Set<String> followSet = followSets.get(symbol);
                            int initialSize = followSet.size();

                            if (i + 1 < prod.size()) {
                                String nextSymbol = prod.get(i + 1);

                                if (grammar.getNonterminals().contains(nextSymbol)) {

                                    followSet.addAll(firstSets.get(nextSymbol));
                                    followSet.remove("ε");

                                    if (firstSets.get(nextSymbol).contains("ε")) {
                                        followSet.addAll(followSets.get(lhs));
                                    }
                                } else {
                                    followSet.add(nextSymbol);
                                }
                            } else {
                                followSet.addAll(followSets.get(lhs));
                            }

                            if (followSet.size() > initialSize) {
                                changed = true;
                            }
                        }
                    }
                }
            }
        } while (changed);
    }

    public Set<String> getFirstSet(String nonTerminal) {
        return firstSets.getOrDefault(nonTerminal, Collections.emptySet());
    }

    public Set<String> getFollowSet(String nonTerminal) {
        return followSets.getOrDefault(nonTerminal, Collections.emptySet());
    }

    public void createParsingTable() {

        for (String nonTerminal : grammar.getNonterminals()) {
            for (List<String> production : grammar.getProduction().get(nonTerminal)) {
                String firstSymbol = production.isEmpty() ? "ε" : production.get(0);

                if (grammar.getTerminals().contains(firstSymbol)) {
                    parsingTable.put(new Pair<>(nonTerminal, firstSymbol), production);
                } else if (nonTerminal.equals(firstSymbol)) {
                    for (String followSymbol : followSets.get(nonTerminal)) {
                        parsingTable.put(new Pair<>(nonTerminal, followSymbol), production);
                    }
                } else {
                    Set<String> firstSet = firstSets.getOrDefault(firstSymbol, Collections.emptySet());
                    for (String symbol : firstSet) {
                        if (!symbol.equals("ε")) {
                            parsingTable.put(new Pair<>(nonTerminal, symbol), production);
                        }
                    }

                    if (firstSet.contains("ε")) {
                        Set<String> followSet = followSets.get(nonTerminal);
                        for (String followSymbol : followSet) {
                            parsingTable.put(new Pair<>(nonTerminal, followSymbol), production);
                        }
                    }
                }
            }
        }
    }

    public List<String> getProductionRule(String nonTerminal, String terminal) {
        return parsingTable.get(new Pair<>(nonTerminal, terminal));
    }

    public void parseAndPrint(String input) {
        StringBuilder outputFileContent = new StringBuilder();
        Stack<String> stack = new Stack<>();
        stack.push("$");
        stack.push(grammar.getInitialState());

        String[] tokens = (input + " $").split(" "); // Appending the end marker to the input
        int currentIndex = 0;
        String currentToken = tokens[currentIndex];
        String string;
        while (!stack.isEmpty()) {
            string = "Stack: " + stack;
            outputFileContent.append(string);
            System.out.println(string);
            string = "Remaining Input: " + Arrays.toString(Arrays.copyOfRange(tokens, currentIndex, tokens.length));
            System.out.println(string);
            outputFileContent.append(string);

            String top = stack.peek();

            if (grammar.getTerminals().contains(top) || top.equals("$")) {
/*                System.out.println("top----------------------------" + top);
                System.out.println(currentToken);*/
                if (top.equals(currentToken)) {
                    string = "Action: Match " + currentToken;
                    System.out.println(string);
                    outputFileContent.append(string);
                    stack.pop();
                    currentIndex++;
                    if (currentIndex < tokens.length) {
                        currentToken = tokens[currentIndex];
                    }
                } else {
                    string = "Error: terminal on stack does not match current token";
                    System.out.println(string);
                    outputFileContent.append(string);
                    return; // Stop parsing on error
                }
            } else if (grammar.getNonterminals().contains(top)) {
                List<String> production = getProductionRule(top, currentToken);
                if (!production.isEmpty()) {

                    string = "Action: Apply " + top + " -> " + production;
                    System.out.println(string);
                    outputFileContent.append(string);

                    stack.pop();
                    for (int i = production.size() - 1; i >= 0; i--) {
                        String symbol = production.get(i);
                        if (!symbol.equals("ε")) {
                            stack.push(symbol);
                        }
                    }
                } else {
                    string = "Error: no production rule found for " + top + " with token " + currentToken;
                    System.out.println(string);
                    outputFileContent.append(string);
                    writeToFile(outputFileContent);
                    return; // Stop parsing on error
                }
            } else {
                string = "Error: invalid symbol on stack " + top;
                System.out.println(string);
                outputFileContent.append(string);
                writeToFile(outputFileContent);
                return; // Stop parsing on error
            }
            string = "--------------------------------------------------";
            System.out.println(string);
            outputFileContent.append(string);

        }

        if (currentIndex < tokens.length) {
            string = "Error: input not fully consumed";
            System.out.println(string);
            outputFileContent.append(string);

        } else {
            string = "Parsing successful!";
            System.out.println(string);
            outputFileContent.append(string);
        }
        writeToFile(outputFileContent);

    }

    public void parse() {
        Stack<String> stack = new Stack<>();
        stack.push("$"); // End symbol
        stack.push(grammar.getInitialState()); // Start symbol of the grammar

        int tokenIndex = 0; // Index to track the current token in PIF

        while (!stack.empty() && tokenIndex < PIF.size()) {

            System.out.println("Current stack: " + stack);
            System.out.println("Remaining input tokens: " + PIF.subList(tokenIndex, PIF.size()));

            String stackTop = stack.peek();
            Pair<Integer, Tuple> currentTokenPair = PIF.get(tokenIndex);
            Object currentToken = getTokenRepresentation(currentTokenPair);

            if (grammar.getTerminals().contains(stackTop)) {
                if (stackTop.equals(currentToken)) {
                    stack.pop(); // Terminal matches the token, pop from stack
                    tokenIndex++; // Move to next token
                } else {
                    throw new RuntimeException("Parsing error at token " + currentToken + " - unexpected terminal.");
                }
            } else if (grammar.getNonterminals().contains(stackTop)) {
                List<String> productionRule = getProductionRule(stackTop, currentToken.toString());

                if (productionRule != null) {
                    stack.pop(); // Pop the nonterminal

                    // Check if the production rule is an epsilon production
                    if (!productionRule.isEmpty() && productionRule.get(0).equals("ε")) {
                        // If it's an epsilon production, we don't push anything onto the stack
                    } else {
                        // Push the symbols of the production rule onto the stack in reverse order
                        for (int i = productionRule.size() - 1; i >= 0; i--) {
                            stack.push(productionRule.get(i));
                        }
                    }
                } else {
                    throw new RuntimeException("Parsing error at token " + currentToken + " - no production rule found.");
                }
            } else {
                throw new RuntimeException("Parsing error - invalid symbol on stack.");
            }
        }

        if (!stack.empty()) {
            throw new RuntimeException("Parsing error - stack not empty after processing input.");
        }

        // Print final state (if the parsing is successful)
        System.out.println("Parsing completed successfully!");
        System.out.println("Final stack: " + stack);
        System.out.println("No remaining input tokens.");
    }

    private Object getTokenRepresentation(Pair<Integer, Tuple> tokenPair) {
        // Implement this method to convert a token pair to its string representation.
        // This might involve looking up the token in the symbol tables or converting its type.

        Integer code = tokenPair.getKey();

        if (code > 0){
            if(code < separatorsIndexStart){
                return operators.get(code);
            }
            else if (code < reservedWordsIndexStart){
                return separators.get(code - separatorsIndexStart);
            } else {
                return reservedWords.get(code - reservedWordsIndexStart);
            }
        } else {
            Tuple pos = tokenPair.getValue();
            if (code == -1) {
                //identifier
                return symbolTableIdentifiers.searchByCode(pos).toString();
            }

            if(code == -2) {
                //constant
                return symbolTableConstants.searchByCode(pos);
            }
        }

        return ""; // Placeholder return
    }
}
