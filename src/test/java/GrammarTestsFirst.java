import org.example.Grammar;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;

public class GrammarTestsFirst {

    String pathG1 = "/g1";
    String pathG2 = "/g3";

    //tests g1
    @Test
    public void testFirstS() {
        Grammar grammar = new Grammar(pathG1);
        Set<String> expected = new HashSet<>(Arrays.asList("a"));
        assertEquals(expected, grammar.getFirstSets("S"));
    }

    @Test
    public void testFirstA() {
        Grammar grammar = new Grammar(pathG1);
        Set<String> expected = new HashSet<>(Arrays.asList("a"));
        assertEquals(expected, grammar.getFirstSet("A"));
    }

    @Test
    public void testFirstB() {
        Grammar grammar = new Grammar(pathG1);
        Set<String> expected = new HashSet<>(Arrays.asList("b"));
        assertEquals(expected, grammar.getFirstSet("B"));
    }

    @Test
    public void testFirstC() {
        Grammar grammar = new Grammar(pathG1);
        Set<String> expected = new HashSet<>(Arrays.asList("a", "b"));
        assertEquals(expected, grammar.getFirstSet("C"));
    }
    

    @Test
    public void testFollowS() {
        Grammar grammar = new Grammar(pathG1);
        Set<String> expected = new HashSet<>(Arrays.asList("$"));
        assertEquals(expected, grammar.getFollowSet("S"));
    }

    @Test
    public void testFollowA() {
        Grammar grammar = new Grammar(pathG1);

        Set<String> expected = new HashSet<>(grammar.getFollowSet("S"));
        assertEquals(expected, grammar.getFollowSet("A"));
    }

    @Test
    public void testFollowB() {
        Grammar grammar = new Grammar(pathG1);

        Set<String> expected = new HashSet<>(grammar.getFollowSet("A"));
        expected.addAll(grammar.getFollowSet("D"));
        assertEquals(expected, grammar.getFollowSet("B"));
    }

    @Test
    public void testFollowC() {
        Grammar grammar = new Grammar(pathG1);
        Set<String> expected = new HashSet<>(Arrays.asList("b"));
        expected.addAll(grammar.getFollowSet("S"));
        assertEquals(expected, grammar.getFollowSet("C"));
    }


    //tests g2

    @Test
    public void testFirstProgram() {
        Grammar grammar = new Grammar(pathG2);
        Set<String> expected = grammar.getFirstSet("stmt");
        assertEquals(expected, grammar.getFirstSet("program"));
    }

    @Test
    public void testFirstStmt() {
        Grammar grammar = new Grammar(pathG2);
        Set<String> expected = new HashSet<>();
        expected.addAll(grammar.getFirstSet("simplstmt"));
        expected.addAll(grammar.getFirstSet("structstmt"));
        assertEquals(expected, grammar.getFirstSet("stmt"));
    }

    @Test
    public void testFirstSimplStmt() {
        Grammar grammar = new Grammar(pathG2);
        Set<String> expected = new HashSet<>();
        expected.addAll(grammar.getFirstSet("assignstmt"));
        expected.addAll(grammar.getFirstSet("iostmt"));
        expected.addAll(grammar.getFirstSet("declaration"));
        assertEquals(expected, grammar.getFirstSet("simplstmt"));
    }

    @Test
    public void testFirstAssignStmt() {
        Grammar grammar = new Grammar(pathG2);
        Set<String> expected = new HashSet<>();
        expected.addAll(grammar.getFirstSet("IDENTIFIER"));
        assertEquals(expected, grammar.getFirstSet("assignstmt"));
    }

    @Test
    public void testFirstIoStmt() {
        Grammar grammar = new Grammar(pathG2);
        Set<String> expected = new HashSet<>(Arrays.asList("pune", "ia"));
        assertEquals(expected, grammar.getFirstSet("iostmt"));
    }

    @Test
    public void testFirstIdentifier() {
        Grammar grammar = new Grammar(pathG2);
        Set<String> expected = new HashSet<>(Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"));
        assertEquals(expected, grammar.getFirstSet("IDENTIFIER"));
    }

    @Test
    public void testFirstExpression() {
        Grammar grammar = new Grammar(pathG2);
        Set<String> expected = new HashSet<>();
        expected.addAll(grammar.getFirstSet("term"));
        assertEquals(expected, grammar.getFirstSet("expression"));
    }

    @Test
    public void testFirstExpression1() {
        Grammar grammar = new Grammar(pathG2);
        Set<String> expected = new HashSet<>();
        expected.addAll(grammar.getFirstSet("first_order_op"));
        assertEquals(expected, grammar.getFirstSet("expression1"));
    }

    @Test
    public void testFirstFirstOrederOp() {
        Grammar grammar = new Grammar(pathG2);
        Set<String> expected = new HashSet<>();
        expected.addAll(Arrays.asList("+", "-"));
        assertEquals(expected, grammar.getFirstSet("first_order_op"));
    }

    @Test
    public void testFirstSecondOrederOp() {
        Grammar grammar = new Grammar(pathG2);
        Set<String> expected = new HashSet<>();
        expected.addAll(Arrays.asList("*", "/"));
        assertEquals(expected, grammar.getFirstSet("second_order_op"));
    }
    @Test
    public void testFirstTerm() {
        Grammar grammar = new Grammar(pathG2);
        Set<String> expected = new HashSet<>();
        expected.addAll(grammar.getFirstSet("factor"));
        assertEquals(expected, grammar.getFirstSet("term"));
    }

    @Test
    public void testFirstTerm1() {
        Grammar grammar = new Grammar(pathG2);
        Set<String> expected = new HashSet<>();
        expected.addAll(grammar.getFirstSet("second_order_op"));
        assertEquals(expected, grammar.getFirstSet("term1"));
    }
    @Test
    public void testFirstFactor() {
        Grammar grammar = new Grammar(pathG2);
        Set<String> expected = new HashSet<>();
        expected.addAll(grammar.getFirstSet("expression"));
        expected.addAll(grammar.getFirstSet("IDENTIFIER"));
        expected.addAll(grammar.getFirstSet("NUMBER"));
        expected.addAll(grammar.getFirstSet("STRING"));expected.addAll(grammar.getFirstSet("CHAR"));
        assertEquals(expected, grammar.getFirstSet("factor"));
    }
}
