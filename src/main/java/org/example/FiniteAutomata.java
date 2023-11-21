package org.example;

import lombok.Getter;

import java.io.InputStream;
import java.util.*;

@Getter
public class FiniteAutomata {
    private final boolean isDeterministic;
    private final String filePath;
    private Set<String> states;
    private Set<String> alphabet;
    private String initialState;
    private Set<String> finalStates;
    private final Map<Pair<String, Object>, Set<String>> transitions;

    public FiniteAutomata(String filePath) {
        this.filePath = filePath;
        this.transitions = new HashMap<>();
        readFile();
/*        System.out.println(this.transitions);
        System.out.println(this.initialState);
        System.out.println(this.states);
        System.out.println(this.finalStates);
        System.out.println(this.alphabet);*/
        this.isDeterministic = checkDeterministic();
        /*System.out.println(isDeterministic);*/
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
                case 1 -> this.states = new HashSet<>(Arrays.asList(tokens));
                case 2 -> this.alphabet = new HashSet<>(Arrays.asList(tokens));
                case 3 -> this.initialState = line.trim();
                case 4 -> this.finalStates = new HashSet<>(Arrays.asList(tokens));
                default -> {
                    Pair<String, Object> pair = new Pair<>(tokens[0], tokens[1]);
                    Set<String> value = this.transitions.computeIfAbsent(pair, k -> new HashSet<>());
                    value.add(tokens[2]);
                }
            }
        }
    }

    private boolean checkDeterministic() {
        if (this.initialState.length() != 1) {
            return false;
        }

        for (var state : this.transitions.values()) {
            if (state.size() > 1) return false;
        }
        return true;
    }

    public boolean checkItem(Object item){
        if(!isDeterministic){
            return false;
        }

        String sequence = item.toString();
        String state = this.initialState;
        for(int i=0; i<sequence.length(); i++){
            Pair pair = new Pair<>(state, String.valueOf(sequence.charAt(i)));
            if(!this.transitions.containsKey(pair)){
                return false;
            }
            Set<String> value = this.transitions.get(pair);
            state = value.stream().findFirst().orElseThrow();
        }
        return this.finalStates.contains(state);
    }
}
