package org.example;

import org.example.Grammar;

import java.util.Collections;
import java.util.List;
import java.util.Stack;

public class LL1Parser {
    private Grammar grammar;

    public LL1Parser(Grammar grammar) {
        this.grammar = grammar;
    }

    public void parse(String input) {
        Stack<String> stack = new Stack<>();
        stack.push("$");
        stack.push(grammar.getInitialState());

        String[] tokens = input.split(" ");
        int currentIndex = 0;
        String currentToken = tokens[currentIndex] + "$";

        while (!stack.isEmpty()) {
            String top = stack.peek();

            if (grammar.getTerminals().contains(top)) {
                if (top.equals(currentToken)) {
                    stack.pop();
                    currentIndex++;
                    if (currentIndex < tokens.length) {
                        currentToken = tokens[currentIndex];
                    } else {
                        currentToken = "$";
                    }
                } else {
                    // Error: terminal on stack does not match current token
                    throw new ParsingException("Syntax error at token: " + currentToken);
                }
            } else if (grammar.getNonterminals().contains(top)) {
                List<String> production = grammar.getProductionRule(top, currentToken);
                if (!production.isEmpty()) {
                    stack.pop();
                    Collections.reverse(production);
                    for (String symbol : production) {
                        if (!symbol.equals("ε")) {
                            stack.push(symbol);
                        }
                    }
                } else {
                    // Error: no production rule found
                    throw new ParsingException("Syntax error: No rule for non-terminal " + top + " with token " + currentToken);
                }
            } else {
                // Error: invalid symbol on stack
                throw new ParsingException("Syntax error: Invalid symbol on stack " + top);
            }
        }

        if (currentIndex < tokens.length) {
            // Error: input not fully consumed
            throw new ParsingException("Syntax error: Input not fully consumed");
        }
    }
}

class ParsingException extends RuntimeException {
    public ParsingException(String message) {
        super(message);
    }
}
