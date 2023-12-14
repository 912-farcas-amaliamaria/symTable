package org.example;

import lombok.Getter;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
public class Parser {
    private static final Tuple NULLTUPLE = new Tuple(-1, -1);
    private static final Integer OPERATORS_INDEX_START = 1;
    private static final Integer SEPARATORS_INDEX_START = 14;
    private static final Integer RESERVED_WORDS_INDEX_START = 19;
    Grammar grammar;
    //-2 in PIF
    private final SymbolTable symbolTableConstants;
    //-1 in PIF
    private final SymbolTable symbolTableIdentifiers;
    private final ArrayList<Pair<Integer, Tuple>> pif;
    private final ArrayList<String> separators;
    private final ArrayList<String> operators;
    private final ArrayList<String> reservedWords;
    private final FiniteAutomata finiteAutomataIdentifier;
    private final FiniteAutomata finiteAutomataInt;
    private int currentLine;

    private static final Stack<List<String>> rules = new Stack<>();
    private final ParseTable parseTable = new ParseTable();
    private final Map<String, Set<String>> firstSets = new HashMap<>();
    private final Map<String, Set<String>> followSets = new HashMap<>();

    private final Stack<String> inputStack = new Stack<>();
    private final Stack<String> workingStack = new Stack<>();
    private final Stack<String> productionsOrder = new Stack<>();

    private final Map<Pair<String, List<String>>, Integer> productionsNumbered = new HashMap<>();
    private static final String EPSILON = "ε";

    public Parser(FiniteAutomata finiteAutomataIdentifier, FiniteAutomata finiteAutomataInt, String filePathGrammar) {
        this.currentLine = 1;
        this.operators = parseTokensIn(OPERATORS_INDEX_START, SEPARATORS_INDEX_START - 1);
        this.separators = parseTokensIn(SEPARATORS_INDEX_START, RESERVED_WORDS_INDEX_START - 1);
        this.reservedWords = parseTokensIn(RESERVED_WORDS_INDEX_START, 27);
        this.separators.add(" ");
        this.symbolTableConstants = new SymbolTable();
        this.symbolTableIdentifiers = new SymbolTable();
        this.pif = new ArrayList<>();
        this.finiteAutomataIdentifier = finiteAutomataIdentifier;
        this.finiteAutomataInt = finiteAutomataInt;

        grammar = new Grammar(filePathGrammar);

        scan("/in/p3.txt");
        calculateFirstSets();

        calculateFollowSets();
        createParseTable();
        System.out.println(firstSets);
        System.out.println("\n");
        System.out.println(followSets);
    }

    public static void writeToFile(StringBuilder stringBuilder) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("parserOutStacks.txt"))) {
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
                    pif.add(new Pair<>(-2, pos));
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
                    pif.add(new Pair<>(-2, pos));
                    input = new StringBuilder();
                } else if (operators.contains(token)) {
                    input.append(token);
                    token = scanner.next();
                    if (!((String.valueOf(input).equals("<") && token.equals("-"))
                            || (String.valueOf(input).equals("-") && token.equals(">"))
                            || (String.valueOf(input).equals("!") && token.equals("="))
                            || (String.valueOf(input).equals("<") && token.equals("="))
                            || (String.valueOf(input).equals("=") && token.equals(">")))) {
                        int pos = this.operators.indexOf(String.valueOf(input)) + OPERATORS_INDEX_START;
                        pif.add(new Pair<>(pos, NULLTUPLE));
                        input = new StringBuilder();
                        if (!token.equals(" "))
                            input.append(token);
                    } else {
                        input.append(token);
                        int pos = this.operators.indexOf(String.valueOf(input)) + OPERATORS_INDEX_START;
                        pif.add(new Pair<>(pos, NULLTUPLE));
                        input = new StringBuilder();
                    }
                } else if (separators.contains(token)) {
                    if (!token.equals(" ")) {
                        int pos = this.separators.indexOf(token) + SEPARATORS_INDEX_START;
                        pif.add(new Pair<>(pos, NULLTUPLE));
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
            int pos = this.reservedWords.indexOf(input) + RESERVED_WORDS_INDEX_START;
            pif.add(new Pair<>(pos, NULLTUPLE));
        } else if (isIdentifier(input)) {
            Tuple pos = symbolTableIdentifiers.add(input);
            pif.add(new Pair<>(-1, pos));
        } else if (isInteger(input)) {
            Tuple pos = symbolTableConstants.add(input);
            pif.add(new Pair<>(-2, pos));
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
            for (Pair<Integer, Tuple> pair : pif) {
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
                    boolean allCanDeriveEpsilon = true;

                    for (String symbol : production) {
                        if (grammar.getTerminals().contains(symbol)) {
                            currentFirstSet.add(symbol);
                            allCanDeriveEpsilon = false;
                            break; // Stop at the first terminal
                        } else if (!symbol.equals("ε")) {
                            Set<String> firstSymbolFirstSet = firstSets.get(symbol);
                            if (firstSymbolFirstSet != null) {
                                currentFirstSet.addAll(firstSymbolFirstSet);
                                currentFirstSet.remove("ε"); // Remove ε because we're not at the end of the production
                            }
                            if (firstSymbolFirstSet == null || !firstSymbolFirstSet.contains("ε")) {
                                allCanDeriveEpsilon = false;
                                break; // Stop at the first non-terminal that doesn't derive ε or is not defined
                            }
                        }
                    }

                    // Check if the entire production can derive epsilon
                    if (allCanDeriveEpsilon) {
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
                                if (i + 1 < prod.size()) {
                                    String nextSymbol = prod.get(i + 1);
                                    if (grammar.getTerminals().contains(nextSymbol)) {
                                        currentFollowSet.add(nextSymbol);
                                    } else {
                                        Set<String> nextSymbolFirstSet = firstSets.get(nextSymbol);
                                        if (nextSymbolFirstSet != null) {
                                            currentFollowSet.addAll(nextSymbolFirstSet);
                                            currentFollowSet.remove("ε");
                                            if (nextSymbolFirstSet.contains("ε")) {
                                                Set<String> lhsFollowSet = followSets.get(lhs);
                                                if (lhsFollowSet != null) {
                                                    currentFollowSet.addAll(lhsFollowSet);
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Set<String> lhsFollowSet = followSets.get(lhs);
                                    if (lhsFollowSet != null) {
                                        currentFollowSet.addAll(lhsFollowSet);
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
            }
        } while (changed);
    }


    public Set<String> getFirstSet(String nonTerminal) {
        return firstSets.getOrDefault(nonTerminal, Collections.emptySet());
    }

    public Set<String> getFollowSet(String nonTerminal) {
        return followSets.getOrDefault(nonTerminal, Collections.emptySet());
    }

    private void createParseTable() {
        numberingProductions();


        // Initialize the table with pop and acc actions for terminals and end symbol
        parseTable.put(new Pair<>("$", "$"), new Pair<>(Collections.singletonList("acc"), -1));
        for (String terminal : grammar.getTerminals()) {
            parseTable.put(new Pair<>(terminal, terminal), new Pair<>(Collections.singletonList("pop"), -1));
        }

        // Iterate through each production
        productionsNumbered.forEach((key, value) -> {
            String nonTerminal = key.getKey();
            List<String> production = key.getValue();
            Pair<List<String>, Integer> tableEntry = new Pair<>(production, value);

            // Case 1: First symbol of production is a terminal (including ε)
            if (grammar.getTerminals().contains(production.get(0)) || production.get(0).equals("ε")) {
                addEntryToParseTable(nonTerminal, production.get(0), tableEntry);
            }
            // Case 2: First symbol of production is a non-terminal
            else if (grammar.getNonterminals().contains(production.get(0))) {
                Set<String> firstSet = firstSets.get(production.get(0));

                // Add entries for symbols in FIRST set
                firstSet.forEach(symbol -> {
                    if (!symbol.equals("ε")) {
                        addEntryToParseTable(nonTerminal, symbol, tableEntry);
                    }
                });

                // If ε is in FIRST set, add entries for symbols in FOLLOW set
                if (firstSet.contains("ε")) {
                    followSets.get(nonTerminal).forEach(followSymbol -> addEntryToParseTable(nonTerminal, followSymbol, new Pair<>(Collections.singletonList("ε"), -1)));
                }
            }
        });
    }

    private void addEntryToParseTable(String nonTerminal, String symbol, Pair<List<String>, Integer> tableEntry) {
        Pair<String, String> tableKey = new Pair<>(nonTerminal, symbol);
        if (!parseTable.containsKey(tableKey)) {
            parseTable.put(tableKey, tableEntry);
        }
    }


    public boolean parse(List<String> w) {
        initializeStacks(w);

        boolean go = true;
        boolean result = true;

        while (go) {
            String betaHead = workingStack.peek();
            String alphaHead = inputStack.peek();

            // Print the current working stack and the remaining input
            System.out.println("Current working stack: " + workingStack);
            System.out.println("Remaining input: " + inputStack);
            System.out.println("\n");

            if (betaHead.equals("$") && alphaHead.equals("$")) {
                return true;
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
                    productionsOrder.push(productionPos.toString());
                }
            }
        }

        return result;
    }

    private void initializeStacks(List<String> w) {
        inputStack.clear();
        inputStack.push("$");
        pushAsChars(w, inputStack);

        workingStack.clear();
        workingStack.push("$");
        workingStack.push(grammar.getInitialState());

        productionsOrder.clear();
        productionsOrder.push("ε");
    }


    public boolean parseSource() {
        List<String> sequence = new LinkedList<>();
        for (Pair<Integer, Tuple> pifEntry : pif) {

            Integer code = pifEntry.getKey();

            if (code > 0){
                if(code < SEPARATORS_INDEX_START){
                    /*System.out.println(operators);
                    System.out.println(code);*/
                    sequence.add(operators.get(code - 1));
                }
                else if (code < RESERVED_WORDS_INDEX_START){
                    /*System.out.println(separators);
                    System.out.println(code);
                    System.out.println(code - separatorsIndexStart);*/
                    sequence.add(separators.get(code - SEPARATORS_INDEX_START));
                } else {
                    /*System.out.println(reservedWords);
                    System.out.println(code - reservedWordsIndexStart);
                    System.out.println(code);*/
                    sequence.add(reservedWords.get(code - RESERVED_WORDS_INDEX_START));
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

    private void pushAsChars(List<String> sequence, Stack<String> stack) {
        for (int i = sequence.size() - 1; i >= 0; i--) {
            stack.push(sequence.get(i));
        }
    }

    private void numberingProductions() {
        int index = 1;
        for (Map.Entry<String, List<List<String>>> entry : grammar.getProduction().entrySet()) {
            String nonTerminal = entry.getKey();
            List<List<String>> prods = entry.getValue();

            for (List<String> rule : prods) {
                productionsNumbered.put(new Pair<>(nonTerminal, rule), index++);
            }
        }
    }

}
