package org.example;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.InputStream;
import java.util.*;

@Getter
public class Grammar {

    private String filePath;
    private Set<String> nonterminals;
    private Set<String> terminals;
    private String initialState;
    private Map<String, Set<List<String>>> production;

    public Grammar(String filePath) {
        this.filePath = filePath;
        this.production = new HashMap<>();
        readFile();
        System.out.println(this.production.toString());
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

    Set<List<String>> getProdForOne(String terminal){
        return this.production.get(terminal);
    }
}

