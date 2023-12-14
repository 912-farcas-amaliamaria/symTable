import org.example.FiniteAutomata;
import org.example.Grammar;
import org.example.Parser;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;

public class GrammarTestsFirst {

    String pathG1 = "/g1";
    String pathG2 = "/g3";

    FiniteAutomata finiteAutomataIdentifier = new FiniteAutomata("/FA_identifier.in");
    FiniteAutomata finiteAutomataInt = new FiniteAutomata("/FA_int.in");
    Parser parserg1 = new Parser(finiteAutomataIdentifier, finiteAutomataInt, "/g4");
    Parser parserg2 = new Parser(finiteAutomataIdentifier, finiteAutomataInt, "/g1");

    //tests g1
    @Test
    public void testFirstS() {
        Set<String> expected = new HashSet<>(Arrays.asList("a"));
        assertEquals(expected, parserg1.getFirstSet("S"));
    }

    @Test
    public void testFirstA() {
        Set<String> expected = new HashSet<>(Arrays.asList("a"));
        assertEquals(expected, parserg1.getFirstSet("A"));
    }

    @Test
    public void testFirstB() {
        Set<String> expected = new HashSet<>(Arrays.asList("b"));
        assertEquals(expected, parserg1.getFirstSet("B"));
    }

    @Test
    public void testFirstC() {
        Set<String> expected = new HashSet<>(Arrays.asList("a", "b"));
        assertEquals(expected, parserg1.getFirstSet("C"));
    }
    

    @Test
    public void testFollowS() {
        Set<String> expected = new HashSet<>(Arrays.asList("$"));
        assertEquals(expected, parserg1.getFollowSet("S"));
    }

    @Test
    public void testFollowA() {
        Set<String> expected = new HashSet<>(parserg1.getFollowSet("S"));
        assertEquals(expected, parserg1.getFollowSet("A"));
    }

    @Test
    public void testFollowB() {
        Set<String> expected = new HashSet<>(parserg1.getFollowSet("A"));
        expected.addAll(parserg1.getFollowSet("D"));
        assertEquals(expected, parserg1.getFollowSet("B"));
    }

    @Test
    public void testFollowC() {
        Set<String> expected = new HashSet<>(Arrays.asList("b"));
        expected.addAll(parserg1.getFollowSet("S"));
        assertEquals(expected, parserg1.getFollowSet("C"));
    }


    //tests g2

    @Test
    public void testFirstProgram() {
        Set<String> expected = parserg2.getFirstSet("stmt");
        assertEquals(expected, parserg2.getFirstSet("program"));
    }

    @Test
    public void testFirstStmt() {
        Set<String> expected = new HashSet<>();
        expected.addAll(parserg2.getFirstSet("simplstmt"));
        expected.addAll(parserg2.getFirstSet("structstmt"));
        assertEquals(expected, parserg2.getFirstSet("stmt"));
    }

    @Test
    public void testFirstSimplStmt() {
        Set<String> expected = new HashSet<>();
        expected.addAll(parserg2.getFirstSet("assignstmt"));
        expected.addAll(parserg2.getFirstSet("iostmt"));
        expected.addAll(parserg2.getFirstSet("declaration"));
        assertEquals(expected, parserg2.getFirstSet("simplstmt"));
    }

    @Test
    public void testFirstAssignStmt() {
        Set<String> expected = new HashSet<>();
        expected.addAll(parserg2.getFirstSet("IDENTIFIER"));
        assertEquals(expected, parserg2.getFirstSet("assignstmt"));
    }

    @Test
    public void testFirstIoStmt() {
        Set<String> expected = new HashSet<>(Arrays.asList("pune", "ia"));
        assertEquals(expected, parserg2.getFirstSet("iostmt"));
    }

    @Test
    public void testFirstIdentifier() {
        Set<String> expected = new HashSet<>(Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"));
        assertEquals(expected, parserg2.getFirstSet("IDENTIFIER"));
    }

    @Test
    public void testFirstExpression() {
        Set<String> expected = new HashSet<>();
        expected.addAll(parserg2.getFirstSet("term"));
        assertEquals(expected, parserg2.getFirstSet("expression"));
    }

    @Test
    public void testFirstExpression1() {
        Set<String> expected = new HashSet<>();
        expected.addAll(parserg2.getFirstSet("first_order_op"));
        assertEquals(expected, parserg2.getFirstSet("expression1"));
    }

    @Test
    public void testFirstFirstOrederOp() {
        Set<String> expected = new HashSet<>();
        expected.addAll(Arrays.asList("+", "-"));
        assertEquals(expected, parserg2.getFirstSet("first_order_op"));
    }

    @Test
    public void testFirstSecondOrederOp() {
        Set<String> expected = new HashSet<>();
        expected.addAll(Arrays.asList("*", "/"));
        assertEquals(expected, parserg2.getFirstSet("second_order_op"));
    }
    @Test
    public void testFirstTerm() {
        Set<String> expected = new HashSet<>();
        expected.addAll(parserg2.getFirstSet("factor"));
        assertEquals(expected, parserg2.getFirstSet("term"));
    }

    @Test
    public void testFirstTerm1() {
        Set<String> expected = new HashSet<>();
        expected.addAll(parserg2.getFirstSet("second_order_op"));
        assertEquals(expected, parserg2.getFirstSet("term1"));
    }
    @Test
    public void testFirstFactor() {
        Set<String> expected = new HashSet<>();
        expected.addAll(parserg2.getFirstSet("expression"));
        expected.addAll(parserg2.getFirstSet("IDENTIFIER"));
        expected.addAll(parserg2.getFirstSet("NUMBER"));
        expected.addAll(parserg2.getFirstSet("STRING"));expected.addAll(parserg2.getFirstSet("CHAR"));
        assertEquals(expected, parserg2.getFirstSet("factor"));
    }
}
