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

    private static Stack<List<String>> rules = new Stack<>();
    private ParseTable parseTable = new ParseTable();
    private Map<String, Set<String>> firstSets = new HashMap<>();
    private Map<String, Set<String>> followSets = new HashMap<>();

    private Stack<String> alpha = new Stack<>();
    private Stack<String> beta = new Stack<>();
    private Stack<String> pi = new Stack<>();

    private Map<Pair<String, List<String>>, Integer> productionsNumbered = new HashMap<>();
    private final String EPSILON = "ε";

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
        createParseTable();
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
        // Initialize first sets for all non-terminals
        for (String nonTerminal : grammar.getNonterminals()) {
            firstSets.put(nonTerminal, new HashSet<>());
        }

        boolean changed;
        do {
            changed = false;
            for (String nonTerminal : grammar.getNonterminals()) {
                Set<String> currentFirstSet = new HashSet<>(firstSets.get(nonTerminal));

                // Iterate over all productions for a non-terminal
                for (List<String> production : grammar.getProduction().get(nonTerminal)) {
                    // Handle the first symbol of the production
                    String firstSymbol = production.get(0);
                    if (grammar.getTerminals().contains(firstSymbol)) {
                        currentFirstSet.add(firstSymbol);
                    } else if (!firstSymbol.equals("ε")) {
                        Set<String> firstSymbolFirstSet = firstSets.get(firstSymbol);
                        if (firstSymbolFirstSet != null) {
                            currentFirstSet.addAll(firstSymbolFirstSet);
                        }
                        currentFirstSet.remove("ε");
                    }

                    // Check if the production can derive epsilon
                    if (allCanDeriveEpsilon(production)) {
                        currentFirstSet.add("ε");
                    }
                }

                // Check if there's a change in the first set
                if (!currentFirstSet.equals(firstSets.get(nonTerminal))) {
                    changed = true;
                    firstSets.put(nonTerminal, currentFirstSet);
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
        for (String nonTerminal : grammar.getNonterminals()) {
            followSets.put(nonTerminal, new HashSet<>());
        }

        // Add end symbol to follow set of start symbol
        followSets.get(grammar.getInitialState()).add("$");

        boolean changed;
        do {
            changed = false;
            for (String nonTerminal : grammar.getNonterminals()) {
                Set<String> currentFollowSet = new HashSet<>(followSets.get(nonTerminal));

                for (Map.Entry<String, List<List<String>>> entry : grammar.getProduction().entrySet()) {
                    String lhs = entry.getKey();
                    for (List<String> prod : entry.getValue()) {
                        for (int i = 0; i < prod.size(); i++) {
                            String symbol = prod.get(i);
                            if (symbol.equals(nonTerminal)) {
                                // Calculate follow set based on the next symbol in the production
                                if (i + 1 < prod.size()) {
                                    String nextSymbol = prod.get(i + 1);
                                    if (grammar.getTerminals().contains(nextSymbol)) {
                                        currentFollowSet.add(nextSymbol);
                                    } else {
                                        currentFollowSet.addAll(firstSets.get(nextSymbol));
                                        currentFollowSet.remove("ε");
                                        if (firstSets.get(nextSymbol).contains("ε")) {
                                            currentFollowSet.addAll(followSets.get(lhs));
                                        }
                                    }
                                } else {
                                    currentFollowSet.addAll(followSets.get(lhs));
                                }
                            }
                        }
                    }
                }

                if (!currentFollowSet.equals(followSets.get(nonTerminal))) {
                    changed = true;
                    followSets.put(nonTerminal, currentFollowSet);
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

   /* public void createParsingTable() {

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
    }*/

    /*public List<String> getProductionRule(String nonTerminal, String terminal) {
        return parsingTable.get(new Pair<>(nonTerminal, terminal));
    }*/

   /* public void parseAndPrint(String input) {
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
*//*                System.out.println("top----------------------------" + top);
                System.out.println(currentToken);*//*
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

    }*/
    /*private void createParseTable() {
        numberingProductions();

        List<String> columnSymbols = new LinkedList<>(grammar.getTerminals());
        columnSymbols.add("$");

        // M(a, a) = pop
        // M($, $) = acc

        parseTable.put(new Pair<>("$", "$"), new Pair<>(Collections.singletonList("acc"), -1));
        for (String terminal: grammar.getTerminals())
            parseTable.put(new Pair<>(terminal, terminal), new Pair<>(Collections.singletonList("pop"), -1));


//        1) M(A, a) = (α, i), if:
//            a) a ∈ first(α)
//            b) a != ε
//            c) A -> α production with index i
//
//        2) M(A, b) = (α, i), if:
//            a) ε ∈ first(α)
//            b) whichever b ∈ follow(A)
//            c) A -> α production with index i
        productionsNumbered.forEach((key, value) -> {
            String rowSymbol = key.getKey();
            List<String> rule = key.getValue();
            Pair<List<String>, Integer> parseTableValue = new Pair<>(rule, value);

            for (String columnSymbol : columnSymbols) {
                Pair<String, String> parseTableKey = new Pair<>(rowSymbol, columnSymbol);

                // if our column-terminal is exactly first of rule
                if (rule.get(0).equals(columnSymbol) && !columnSymbol.equals("ε"))
                    parseTable.put(parseTableKey, parseTableValue);

                    // if the first symbol is a non-terminal and it's first contain our column-terminal
                else if (grammar.getNonterminals().contains(rule.get(0)) && firstSets.get(rule.get(0)).contains(columnSymbol)) {
                    if (!parseTable.containsKey(parseTableKey)) {
                        parseTable.put(parseTableKey, parseTableValue);
                    }
                }
                else {
                    // if the first symbol is ε then everything if FOLLOW(rowSymbol) will be in parse table
                    if (rule.get(0).equals("ε")) {
                        for (String b : followSets.get(rowSymbol))
                            parseTable.put(new Pair<>(rowSymbol, b), parseTableValue);

                        // if ε is in FIRST(rule)
                    } else {
                        Set<String> firsts = new HashSet<>();
                        for (String symbol : rule)
                            if (grammar.getNonterminals().contains(symbol))
                                firsts.addAll(firstSets.get(symbol));
                        if (firsts.contains("ε")) {
                            for (String b : firstSets.get(rowSymbol)) {
                                if (b.equals("ε"))
                                    b = "$";
                                parseTableKey = new Pair<>(rowSymbol, b);
                                if (!parseTable.containsKey(parseTableKey)) {
                                    parseTable.put(parseTableKey, parseTableValue);
                                }
                            }
                        }
                    }
                }
            }
        });
    }*/


    /*public boolean parse(List<String> w) {
        initializeStacks(w);

        boolean go = true;
        boolean result = true;

        while (go) {
            String betaHead = workingStack.peek();
            String alphaHead = inputStack.peek();

            // Print the current working stack and the remaining input
            System.out.println("Current working stack: " + workingStack);
            System.out.println("Remaining input: " + inputStack);

            if (betaHead.equals("$") && alphaHead.equals("$")) {
                return result;
            }

            Pair<String, String> heads = new Pair<>(betaHead, alphaHead);
            Pair<List<String>, Integer> parseTableEntry = parseTable.get(heads);

            if (parseTableEntry == null) {
                heads = new Pair<>(betaHead, "ε");
                parseTableEntry = parseTable.get(heads);
                if (parseTableEntry != null) {
                    workingStack.pop();
                    continue;
                }
            }

            if (parseTableEntry == null) {
                go = false;
                result = false;
            } else {
                List<String> production = parseTableEntry.getKey();
                Integer productionPos = parseTableEntry.getValue();

                if (productionPos == -1 && production.get(0).equals("acc")) {
                    go = false;
                } else if (productionPos == -1 && production.get(0).equals("pop")) {
                    workingStack.pop();
                    inputStack.pop();
                } else {
                    workingStack.pop();
                    if (!production.get(0).equals("ε")) {
                        pushAsChars(production, workingStack);
                    }
                    pi.push(productionPos.toString());
                }
            }
        }

        return result;
    }*/
    private void generateFirstSet() {
        for (String nonTerminal : grammar.getNonterminals()) {
            firstSets.put(nonTerminal, this.firstOf(nonTerminal));
        }
    }

    private Set<String> firstOf(String nonTerminal) {
        if (firstSets.containsKey(nonTerminal))
            return firstSets.get(nonTerminal);
        Set<String> temp = new HashSet<>();
        Set<String> terminals = grammar.getTerminals();
        for (List<String> production : grammar.getProdForOne(nonTerminal)) {
            String firstSymbol = production.get(0);
            if (firstSymbol.equals("ε"))
                temp.add("ε");
            else if (terminals.contains(firstSymbol))
                temp.add(firstSymbol);
            else
                temp.addAll(firstOf(firstSymbol));
        }
        return temp;
    }

    private void generateFollowSet() {
        for (String nonTerminal : grammar.getNonterminals()) {
            followSets.put(nonTerminal, this.followOf(nonTerminal, nonTerminal));
        }
    }

    private Set<String> followOf(String nonTerminal, String initialNonTerminal) {
        if (followSets.containsKey(nonTerminal))
            return followSets.get(nonTerminal);
        Set<String> temp = new HashSet<>();
        Set<String> terminals = grammar.getTerminals();

        if (nonTerminal.equals(grammar.getInitialState()))
            temp.add("$");

        for (Map.Entry<String, List<List<String>>> production : grammar.getProductionsContainingNonterminal(nonTerminal).entrySet()) {
            String productionStart = production.getKey();
            for (List<String> rule : production.getValue()){
                List<String> ruleConflict = new ArrayList<>();
                ruleConflict.add(nonTerminal);
                ruleConflict.addAll(rule);
                if (rule.contains(nonTerminal) && !rules.contains(ruleConflict)) {
                    rules.push(ruleConflict);
                    int indexNonTerminal = rule.indexOf(nonTerminal);
                    temp.addAll(followOperation(nonTerminal, temp, terminals, productionStart, rule, indexNonTerminal, initialNonTerminal));

//                    // For cases like: N -> E 36 E, when E is the nonTerminal so we have 2 possibilities: 36 goes in follow(E) and also follow(N)
                    List<String> sublist = rule.subList(indexNonTerminal + 1, rule.size());
                    if (sublist.contains(nonTerminal))
                        temp.addAll(followOperation(nonTerminal, temp, terminals, productionStart, rule, indexNonTerminal + 1 + sublist.indexOf(nonTerminal), initialNonTerminal));

                    rules.pop();
                }
            }
        }

        return temp;
    }

    private Set<String> followOperation(String nonTerminal, Set<String> temp, Set<String> terminals, String productionStart, List<String> rule, int indexNonTerminal, String initialNonTerminal) {
        if (indexNonTerminal == rule.size() - 1) {
            if (productionStart.equals(nonTerminal))
                return temp;
            if (!initialNonTerminal.equals(productionStart)){
                temp.addAll(followOf(productionStart, initialNonTerminal));
            }
        }
        else
        {
            String nextSymbol = rule.get(indexNonTerminal + 1);
            if (terminals.contains(nextSymbol))
                temp.add(nextSymbol);
            else{
                if (!initialNonTerminal.equals(nextSymbol)) {
                    Set<String> fists = new HashSet<>(firstSets.get(nextSymbol));
                    if (fists.contains("ε")) {
                        temp.addAll(followOf(nextSymbol, initialNonTerminal));
                        fists.remove("ε");
                    }
                    temp.addAll(fists);
                }
            }
        }
        return temp;
    }

    private void createParseTable() {
        numberingProductions();

        List<String> columnSymbols = new LinkedList<>(grammar.getTerminals());
        columnSymbols.add("$");

        // M(a, a) = pop
        // M($, $) = acc

        parseTable.put(new Pair<>("$", "$"), new Pair<>(Collections.singletonList("acc"), -1));
        for (String terminal: grammar.getTerminals())
            parseTable.put(new Pair<>(terminal, terminal), new Pair<>(Collections.singletonList("pop"), -1));



//        1) M(A, a) = (α, i), if:
//            a) a ∈ first(α)
//            b) a != ε
//            c) A -> α production with index i
//
//        2) M(A, b) = (α, i), if:
//            a) ε ∈ first(α)
//            b) whichever b ∈ follow(A)
//            c) A -> α production with index i
        productionsNumbered.forEach((key, value) -> {
            String rowSymbol = key.getKey();
            List<String> rule = key.getValue();
            Pair<List<String>, Integer> parseTableValue = new Pair<>(rule, value);

            for (String columnSymbol : columnSymbols) {
                Pair<String, String> parseTableKey = new Pair<>(rowSymbol, columnSymbol);

                // if our column-terminal is exactly first of rule
                if (rule.get(0).equals(columnSymbol) && !columnSymbol.equals("ε"))
                    parseTable.put(parseTableKey, parseTableValue);

                    // if the first symbol is a non-terminal and it's first contain our column-terminal
                else if (grammar.getNonterminals().contains(rule.get(0)) && firstSets.get(rule.get(0)).contains(columnSymbol)) {
                    if (!parseTable.containsKey(parseTableKey)) {
                        parseTable.put(parseTableKey, parseTableValue);
                    }
                }
                else {
                    // if the first symbol is ε then everything if FOLLOW(rowSymbol) will be in parse table
                    if (rule.get(0).equals("ε")) {
                        for (String b : followSets.get(rowSymbol))
                            parseTable.put(new Pair<>(rowSymbol, b), parseTableValue);

                        // if ε is in FIRST(rule)
                    } else {
                        Set<String> firsts = new HashSet<>();
                        for (String symbol : rule)
                            if (grammar.getNonterminals().contains(symbol))
                                firsts.addAll(firstSets.get(symbol));
                        if (firsts.contains("ε")) {
                            for (String b : firstSets.get(rowSymbol)) {
                                if (b.equals("ε"))
                                    b = "$";
                                parseTableKey = new Pair<>(rowSymbol, b);
                                if (!parseTable.containsKey(parseTableKey)) {
                                    parseTable.put(parseTableKey, parseTableValue);
                                }
                            }
                        }
                    }
                }
            }
        });
    }

    public boolean parse(List<String> w) {
        initializeStacks(w);

        boolean go = true;
        boolean result = true;

        while (go) {
            String betaHead = beta.peek();
            String alphaHead = alpha.peek();

            if (betaHead.equals("$") && alphaHead.equals("$")) {
                return result;
            }

            Pair<String, String> heads = new Pair<>(betaHead, alphaHead);
            Pair<List<String>, Integer> parseTableEntry = parseTable.get(heads);

            if (parseTableEntry == null) {
                heads = new Pair<>(betaHead, "ε");
                parseTableEntry = parseTable.get(heads);
                if (parseTableEntry != null) {
                    beta.pop();
                    continue;
                }

            }

            if (parseTableEntry == null) {
                go = false;
                result = false;
            } else {
                List<String> production = parseTableEntry.getKey();
                Integer productionPos = parseTableEntry.getValue();

                if (productionPos == -1 && production.get(0).equals("acc")) {
                    go = false;
                } else if (productionPos == -1 && production.get(0).equals("pop")) {
                    beta.pop();
                    alpha.pop();
                } else {
                    beta.pop();
                    if (!production.get(0).equals("ε")) {
                        pushAsChars(production, beta);
                    }
                    pi.push(productionPos.toString());
                }
            }
        }

        return result;
    }

    private void initializeStacks(List<String> w) {
        alpha.clear();
        alpha.push("$");
        pushAsChars(w, alpha);

        beta.clear();
        beta.push("$");
        beta.push(grammar.getInitialState());

        pi.clear();
        pi.push("ε");
    }


    public boolean parseSource() {
        List<String> sequence = new LinkedList<>();
        for (Pair<Integer, Tuple> pifEntry : PIF) {

            Integer code = pifEntry.getKey();

            if (code > 0){
                if(code < separatorsIndexStart){
                    /*System.out.println(operators);
                    System.out.println(code);*/
                    sequence.add(operators.get(code - 1));
                }
                else if (code < reservedWordsIndexStart){
                    /*System.out.println(separators);
                    System.out.println(code);
                    System.out.println(code - separatorsIndexStart);*/
                    sequence.add(separators.get(code - separatorsIndexStart));
                } else {
                    /*System.out.println(reservedWords);
                    System.out.println(code - reservedWordsIndexStart);
                    System.out.println(code);*/
                    sequence.add(reservedWords.get(code - reservedWordsIndexStart));
                }
            } else {
                if (code == -1) {
                    //identifier
                    sequence.add("IDENTIFIER");
                }

                if(code == -2) {
                    //constant
                    sequence.add("CONSTANT");
                }
            }
        }

        return this.parse(sequence);
    }

/*    private void initializeStacks(List<String> w) {
        inputStack.clear();
        inputStack.push("$");
        pushAsChars(w, inputStack);

        workingStack.clear();
        workingStack.push("$");
        workingStack.push(grammar.getInitialState());

        pi.clear();
        pi.push("ε");
    }*/

    private void pushAsChars(List<String> sequence, Stack<String> stack) {
        for (int i = sequence.size() - 1; i >= 0; i--) {
            stack.push(sequence.get(i));
        }
    }

    private void numberingProductions() {
        int index = 1;
        for (Map.Entry<String, List<List<String>>> entry : grammar.getProduction().entrySet()) {
            String nonTerminal = entry.getKey();
            List<List<String>> rules = entry.getValue();

            for (List<String> rule : rules) {
                productionsNumbered.put(new Pair<>(nonTerminal, rule), index++);
            }
        }
    }

    /*public void parse() {
        Stack<String> stack = new Stack<>();
        stack.push("$"); // End symbol
        stack.push(grammar.getInitialState()); // Start symbol of the grammar

        int tokenIndex = 0; // Index to track the current token in PIF

        while (!stack.empty() && tokenIndex < PIF.size()) {
            System.out.println("Current stack: " + stack);
            System.out.println("Remaining input tokens: " + PIF.subList(tokenIndex, PIF.size()));

            String stackTop = stack.peek();
            Pair<Integer, Tuple> currentTokenPair = PIF.get(tokenIndex);
            Pair<Object, String> currentToken = getTokenRepresentation(currentTokenPair);

            if (grammar.getTerminals().contains(stackTop) || stackTop.equals("$")) {
*//*                System.out.println(stackTop);
                System.out.println(currentToken.getKey());
                System.out.println(currentToken.getValue());*//*
                if (stackTop.equals(currentToken.getKey()) || stackTop.equals(currentToken.getValue())) {
                    stack.pop(); // Terminal matches the token, pop from stack
                    tokenIndex++; // Move to next token
                } else {
                    throw new RuntimeException("Parsing error at token " + currentToken + " - unexpected terminal.");
                }
            } else if (grammar.getNonterminals().contains(stackTop)) {
                List<String> productionRule;
                if(Objects.equals(currentToken.getValue(), "IDENTIFIER")){
                    productionRule = parsingTable.get(new Pair<>(stackTop, "IDENTIFIER"));
                } else if(Objects.equals(currentToken.getValue(), "CONSTANT")){
                    productionRule = parsingTable.get(new Pair<>(stackTop, "CONSTANT"));}
                else {
                    String nonterminal = currentToken.getKey().toString();
                    productionRule = parsingTable.get(new Pair<>(stackTop, nonterminal));
                }

                System.out.println(productionRule);

                if (productionRule != null) {
                    stack.pop(); // Pop the nonterminal

                    // Push the symbols of the production rule onto the stack in reverse order
                    // Skip if it's an epsilon production
                    for (int i = productionRule.size() - 1; i >= 0; i--) {
                            if (!productionRule.get(i).equals("ε")) {
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



    private Pair<Object, String> getTokenRepresentation(Pair<Integer, Tuple> tokenPair) {
        // Implement this method to convert a token pair to its string representation.
        // This might involve looking up the token in the symbol tables or converting its type.

        Integer code = tokenPair.getKey();

        if (code > 0){
            if(code < separatorsIndexStart){
                System.out.println(operators);
                System.out.println(code);
                return new Pair<>(operators.get(code - 1), "");
            }
            else if (code < reservedWordsIndexStart){
                System.out.println(separators);
                System.out.println(code);
                System.out.println(code - separatorsIndexStart);
                return new Pair<>(separators.get(code - separatorsIndexStart), "");
            } else {
                System.out.println(reservedWords);
                System.out.println(code - reservedWordsIndexStart);
                System.out.println(code);
                return new Pair<>(reservedWords.get(code - reservedWordsIndexStart), "");
            }
        } else {
            Tuple pos = tokenPair.getValue();
            if (code == -1) {
                //identifier
                return new Pair<>(symbolTableIdentifiers.searchByCode(pos).toString(), "IDENTIFIER");
            }

            if(code == -2) {
                //constant
                return new Pair<>(symbolTableConstants.searchByCode(pos), "CONSTANT");
            }
        }

        return new Pair<>("", ""); // Placeholder return
    }*/
}
