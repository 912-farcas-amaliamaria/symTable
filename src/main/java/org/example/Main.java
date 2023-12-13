package org.example;


import java.io.*;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {

        FiniteAutomata finiteAutomataIdentifier = new FiniteAutomata("/FA_identifier.in");
        FiniteAutomata finiteAutomataInt = new FiniteAutomata("/FA_int.in");
        Parser parser = new Parser(finiteAutomataIdentifier, finiteAutomataInt, "/g4");

        Scanner scanner = new Scanner(System.in);
        int choice;

        InputStream pragramInputStream = Parser.class.getResourceAsStream("/in/p1.txt");

        if (pragramInputStream == null) {
            System.err.println("Resource file not found.");
            return;
        }
        java.util.Scanner fileScanner = new java.util.Scanner(pragramInputStream);
        fileScanner.useDelimiter("\u001a");
        String program = fileScanner.next();

        do {
            System.out.println("Menu:");
            System.out.println("1. Set of terminals");
            System.out.println("2. Set of nonterminals");
            System.out.println("3. Set of productions");
            System.out.println("4. Set of productions for one nonterminal");
            System.out.println("5. Verify if this is a context-free grammar");
            System.out.println("6. Parsing table");
            System.out.println("7. Parse");
            System.out.println("8. Exit");
            System.out.print("Enter your choice: ");

            try {
                choice = scanner.nextInt();
            } catch (java.util.InputMismatchException e) {
                scanner.nextLine(); // Clear the input buffer
                choice = -1; // Set an invalid choice
            }

            switch (choice) {
                case 1 -> System.out.println(parser.getGrammar().getTerminals());
                case 2 -> System.out.println(parser.getGrammar().getNonterminals());
                case 3 -> System.out.println(parser.getGrammar().getProduction());
                case 4 -> {
                    scanner.nextLine(); // Consume the newline character
                    System.out.print("Enter a nonterminal: ");
                    String nonterminal = scanner.nextLine();
                    System.out.println(parser.getGrammar().getProdForOne(nonterminal));
                }
                case 5 -> System.out.println(parser.getGrammar().isCFG());
                case 6 -> {
                    System.out.println(parser.getParseTable().toString());

                }
                case 7 -> {
                    System.out.println(parser.parseSource());
                }
                case 8 -> System.out.println("Goodbye!");
                default -> System.out.println("Invalid choice. Please select a valid option.");
            }
        } while (choice != 8);

        scanner.close();

    }

    static void FAMenu(FiniteAutomata FA, Scanner scanner){
        int choice;

        do {
            System.out.println("FA Menu:");
            System.out.println("1. Get states");
            System.out.println("2. Get alphabet");
            System.out.println("3. Get initial state");
            System.out.println("4. Get final states");
            System.out.println("5. Get transitions");
            System.out.println("6. Check sequence");
            System.out.println("7. Exit");
            System.out.print("Enter your choice: ");

            try {
                choice = scanner.nextInt();
            } catch (java.util.InputMismatchException e) {
                scanner.nextLine(); // Clear the input buffer
                choice = -1; // Set an invalid choice
            }

            switch (choice) {
                case 1 -> System.out.println(FA.getStates());
                case 2 -> System.out.println(FA.getAlphabet());
                case 3 -> System.out.println(FA.getInitialState());
                case 4 -> System.out.println(FA.getFinalStates());
                case 5 -> System.out.println(FA.getTransitions());
                case 6 -> {
                    scanner.nextLine(); // Consume the newline character
                    System.out.print("Enter a string: ");
                    String userInput = scanner.nextLine();
                    System.out.println("Accepted: " + FA.checkItem(userInput));
                }
                default -> System.out.println("Invalid choice. Please select a valid option.");
            }
        } while (choice != 7);

    }

}



/*
        SymbolTable symbolTableIdentifiers = new SymbolTable();

        symbolTableIdentifiers.add("a");
        symbolTableIdentifiers.add("a");
        symbolTableIdentifiers.add("b");
        symbolTableIdentifiers.add("abb");
        symbolTableIdentifiers.add("b");

        System.out.println("identifiers");
        System.out.println(symbolTableIdentifiers);


        SymbolTable symbolTableConstants = new SymbolTable();

        symbolTableConstants.add("'a'");
        symbolTableConstants.add("'a'");
        symbolTableConstants.add("'a'");
        symbolTableConstants.add("5");
        symbolTableConstants.add("5");
        symbolTableConstants.add("\"5\"");
        symbolTableConstants.add("'5'");
        symbolTableConstants.add("\"a\"");
        symbolTableConstants.add("\"a\"");

        System.out.println("constants");
        System.out.println(symbolTableConstants);*/

        /*FiniteAutomata finiteAutomataIdentifier = new FiniteAutomata("/FA_identifier.in");
        FiniteAutomata finiteAutomataInt = new FiniteAutomata("/FA_int.in");
        Parser parser = new Parser(finiteAutomataIdentifier, finiteAutomataInt);
        try {
            parser.scan("/in/p1.txt");
*//*            parser.scan("/in/p2.txt");
            parser.scan("/in/p3.txt");
            parser.scan("/in/p1err.txt");*//*
            System.out.println("Lexically correct!");
        } catch (RuntimeException e) {
            System.out.println(e.getMessage());
        }
        parser.getSymbolTableConstants().writeToFile("sybTblConst.txt");
        parser.getSymbolTableIdentifiers().writeToFile("sybTblIdent.txt");
        parser.writePIF("PIF.out");
        //System.out.println(parser.parseTokensIn(1, 9));



        Scanner scanner = new Scanner(System.in);
        int choice;

        do {
            System.out.println("Main Menu:");
            System.out.println("1. FA identifiers");
            System.out.println("2. FA int");
            System.out.println("3. Exit");
            System.out.print("Enter your choice: ");

            try {
                choice = scanner.nextInt();
            } catch (java.util.InputMismatchException e) {
                scanner.nextLine(); // Clear the input buffer
                choice = -1; // Set an invalid choice
            }

            switch (choice) {
                case 1 -> FAMenu(finiteAutomataIdentifier, scanner);
                case 2 -> FAMenu(finiteAutomataInt, scanner);
                case 3 -> System.out.println("Goodbye!");
                default -> System.out.println("Invalid choice. Please select a valid option.");
            }
        } while (choice != 3);

        scanner.close();*/