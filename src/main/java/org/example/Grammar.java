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
    private final Map<String, List<List<String>>> production;


    public Grammar(String filePath) {
        this.filePath = filePath;
        this.production = new HashMap<>();
        readFile();

    }

    private void readFile() {
        InputStream inputStream = Parser.class.getResourceAsStream(filePath);

        if (inputStream == null) {
            System.err.println("Resource file not found.");
            return;
        }
        java.util.Scanner scanner = new java.util.Scanner(inputStream);
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
                    List<List<String>> value = this.production.computeIfAbsent(key, k -> new ArrayList<>());
                    for (int i = 1; i < tokens.length; i++) {
                        String[] prod = tokens[i].split(" ");
                        value.add(Arrays.asList(prod));
                    }
                }
            }
        }
    }

    public List<List<String>> getProdForOne(String terminal){
        return this.production.get(terminal);
    }

    boolean isCFG() {
        for (String s : this.production.keySet())
            if (!this.nonterminals.contains(s))
                return false;
        return true;
    }

    public Map<String, List<List<String>>> getProductionsContainingNonterminal(String nonTerminal) {
        Map<String, List<List<String>>> prod = new HashMap<>();
        for (Map.Entry<String, List<List<String>>> p : production.entrySet()) {
            for(List<String> list : p.getValue()){
                if(list.contains(nonTerminal)){
                    prod.put(p.getKey(), p.getValue());
                }
            }
        }
        return prod;
    }
}

