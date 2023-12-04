package org.example;

import lombok.Getter;

import java.io.InputStream;
import java.util.*;

@Getter
public class Grammar {

    private final String filePath;
    private Set<String> nonterminals;
    private Set<String> terminals;
    private String initialState;
    private final Map<String, Set<List<String>>> production;

    private Map<String, Set<String>> firstSets;
    private Map<String, Set<String>> followSets;



    public Grammar(String filePath) {
        this.filePath = filePath;
        this.production = new HashMap<>();
        readFile();
        calculateFirstSets();
        calculateFollowSets();
    }

    private void readFile() {
        InputStream inputStream = Parser.class.getResourceAsStream(filePath);

        if (inputStream == null) {
            System.err.println("Resource file not found.");
            return;
        }
        Scanner scanner = new Scanner(inputStream);
        scanner.useDelimiter("\r\n");
        int currentLine = 0;
        String line;
        while (scanner.hasNext()) {
            currentLine++;
            line = scanner.next();
            String[] tokens = line.split(" ");
            for (int i = 0; i < tokens.length; i++) {
                tokens[i] = tokens[i].trim();
            }
            switch (currentLine) {
                case 1 -> this.nonterminals = new HashSet<>(Arrays.asList(tokens));
                case 2 -> this.terminals = new HashSet<>(Arrays.asList(tokens));
                case 3 -> this.initialState = line.trim();
                default -> {
                    tokens = line.split(" \\| ");
                    String key = tokens[0];
                    Set<List<String>> value = this.production.computeIfAbsent(key, k -> new HashSet<>());
                    for (int i = 1; i < tokens.length; i++) {
                        String[] prod = tokens[i].split(" ");
                        value.add(Arrays.asList(prod));
                    }
                }
            }
        }
    }

    public Set<List<String>> getProdForOne(String terminal){
        return this.production.get(terminal);
    }

    boolean isCFG() {
        for (String s : this.production.keySet())
            if (!this.nonterminals.contains(s))
                return false;
        return true;
    }

    private void calculateFirstSets() {
        firstSets = new HashMap<>();
        // Initialize FIRST sets for non-terminals
        for (String nonterminal : nonterminals) {
            firstSets.put(nonterminal, new HashSet<>());
        }

        boolean changed;
        do {
            changed = false;
            for (String nonterminal : nonterminals) {
                Set<String> firstSet = firstSets.get(nonterminal);
                int initialSize = firstSet.size();
                for (List<String> production : this.production.get(nonterminal)) {
                    if (production.isEmpty()) {
                        // Handle epsilon
                        firstSet.add("ε");
                        continue;
                    }

                    String firstSymbol = production.get(0);
                    if (terminals.contains(firstSymbol)) {
                        // Add terminal to the FIRST set
                        firstSet.add(firstSymbol);
                    } else if (nonterminals.contains(firstSymbol)) {
                        // Add all non-ε elements of FIRST(firstSymbol) to the FIRST set
                        firstSet.addAll(firstSets.get(firstSymbol));
                        firstSet.remove("ε");
                    }

                    // Add ε if all symbols in production can derive ε
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
        for (String nonterminal : nonterminals) {
            followSets.put(nonterminal, new HashSet<>());
        }

        // Adding end-of-input marker to the FOLLOW set of the start symbol
        followSets.get(initialState).add("$");

        boolean changed;
        do {
            changed = false;
            for (Map.Entry<String, Set<List<String>>> entry : production.entrySet()) {
                String lhs = entry.getKey(); // Left-hand side of the production
                for (List<String> prod : entry.getValue()) {
                    for (int i = 0; i < prod.size(); i++) {
                        String symbol = prod.get(i);
                        if (nonterminals.contains(symbol)) {
                            Set<String> followSet = followSets.get(symbol);
                            int initialSize = followSet.size();

                            // Handling the symbol that follows the current non-terminal
                            if (i + 1 < prod.size()) {
                                String nextSymbol = prod.get(i + 1);
                                if (nonterminals.contains(nextSymbol)) {
                                    followSet.addAll(firstSets.get(nextSymbol));
                                    followSet.remove("ε"); // Remove ε if present
                                    if (firstSets.get(nextSymbol).contains("ε")) {
                                        followSet.addAll(followSets.get(lhs));
                                    }
                                } else {
                                    followSet.add(nextSymbol);
                                }
                            } else {
                                // If it's the last symbol in the production, add FOLLOW(lhs)
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

}

