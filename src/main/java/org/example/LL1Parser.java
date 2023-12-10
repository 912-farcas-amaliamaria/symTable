package org.example;

import org.example.Grammar;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

public class LL1Parser {
    private final Grammar grammar;

    public LL1Parser(Grammar grammar) {
        this.grammar = grammar;
    }

    public void parseAndPrint(String input) {
        Stack<String> stack = new Stack<>();
        stack.push("$");
        stack.push(grammar.getInitialState());

        String[] tokens = (input + " $").split(" "); // Appending the end marker to the input
        int currentIndex = 0;
        String currentToken = tokens[currentIndex];

        while (!stack.isEmpty()) {
            System.out.println("Stack: " + stack);
            System.out.println("Remaining Input: " + Arrays.toString(Arrays.copyOfRange(tokens, currentIndex, tokens.length)));

            String top = stack.peek();

            if (grammar.getTerminals().contains(top) || top.equals("$")) {
                if (top.equals(currentToken)) {
                    System.out.println("Action: Match " + currentToken);
                    stack.pop();
                    currentIndex++;
                    if (currentIndex < tokens.length) {
                        currentToken = tokens[currentIndex];
                    }
                } else {
                    System.out.println("Error: terminal on stack does not match current token");
                    return; // Stop parsing on error
                }
            } else if (grammar.getNonterminals().contains(top)) {
                List<String> production = grammar.getProductionRule(top, currentToken);
                if (!production.isEmpty()) {
                    System.out.println("Action: Apply " + top + " -> " + production);
                    stack.pop();
                    for (int i = production.size() - 1; i >= 0; i--) {
                        String symbol = production.get(i);
                        if (!symbol.equals("ε")) {
                            stack.push(symbol);
                        }
                    }
                } else {
                    System.out.println("Error: no production rule found for " + top + " with token " + currentToken);
                    return; // Stop parsing on error
                }
            } else {
                System.out.println("Error: invalid symbol on stack " + top);
                return; // Stop parsing on error
            }

            System.out.println("--------------------------------------------------");
        }

        if (currentIndex < tokens.length) {
            System.out.println("Error: input not fully consumed");
        } else {
            System.out.println("Parsing successful!");
        }
    }
}


class ParsingException extends RuntimeException {
    public ParsingException(String message) {
        super(message);
    }
}
