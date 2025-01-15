package nl.aurorion.blockregen.preset.condition.expression;

import com.google.common.base.Strings;
import com.linecorp.conditional.ConditionContext;
import lombok.Getter;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.Pair;
import nl.aurorion.blockregen.ParseException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log
public class Expression {

    public static final Pattern SYMBOL_PATTERN = Pattern.compile("(.+)\\s*(<|>|>=|<=|==|!=)\\s*(.+)");

    @Getter
    private final Operand left;
    @Getter
    private final Operand right;

    @Getter
    private final OperandRelation relation;

    private Expression(Operand left, Operand right, OperandRelation relation) {
        this.left = left;
        this.right = right;
        this.relation = relation;
    }

    public boolean evaluate(@NotNull ConditionContext ctx) {
        Object o1 = this.left.value(ctx);
        Object o2 = this.right.value(ctx);

        log.fine(() -> "Evaluate " + this + " " + o1 + " " + relation + " " + o2);

        return this.relation.evaluate(o1, o2);
    }

    @NotNull
    public static Expression of(Operand left, Operand right, OperandRelation relation) {
        return new Expression(left, right, relation);
    }

    /**
     * @throws IllegalArgumentException if the input is null or empty.
     * @throws ParseException           If the parsing fails.
     */
    @NotNull
    public static Expression from(String input) {
        if (Strings.isNullOrEmpty(input)) {
            throw new IllegalArgumentException("Expression#from input cannot be empty or null.");
        }

        Matcher matcher = SYMBOL_PATTERN.matcher(input);

        if (!matcher.matches()) {
            throw new ParseException("Invalid expression '" + input + "'");
        }

        // Figure out if it's constant or a variable.
        Operand op1 = Operand.Parser.parse(matcher.group(1));
        Operand op2 = Operand.Parser.parse(matcher.group(3));

        String operator = matcher.group(2);
        OperandRelation relation = OperandRelation.parse(operator);
        if (relation == null) {
            throw new ParseException("Invalid relation operator.");
        }

        Expression expression = new Expression(op1, op2, relation);
        log.fine(() -> "Parsed expression: " + expression);
        return expression;
    }

    /**
     * Attempt to parse operands using a provided parser.
     * */
    public static Expression withCustomOperands(Function<String, Operand> parser, String input) {
        Matcher matcher = Expression.SYMBOL_PATTERN.matcher(input);

        if (!matcher.find()) {
            throw new ParseException("Invalid expression " + input);
        }

        OperandRelation relation = OperandRelation.parse(matcher.group(2));
        if (relation == null) {
            throw new ParseException("Invalid relation operator.");
        }

        Operand o1 = attemptParse(parser, matcher.group(1));
        Operand o2 = attemptParse(parser, matcher.group(3));

        if (o1 == null && o2 == null) {
            throw new ParseException("No variable operand in expression.");
        }

        if (o1 == null) {
            o1 = new Constant(Operand.Parser.parseObject(matcher.group(1)));
        } else {
            o2 = new Constant(Operand.Parser.parseObject(matcher.group(3)));
        }

        return Expression.of(o1, o2, relation);
    }

    public static Pair<OperandRelation, String[]> splitExpression(String input) {
        Matcher matcher = Expression.SYMBOL_PATTERN.matcher(input);

        if (!matcher.find()) {
            throw new ParseException("Invalid expression " + input);
        }

        OperandRelation relation = OperandRelation.parse(matcher.group(2));
        if (relation == null) {
            throw new ParseException("Invalid relation operator.");
        }

        return new Pair<>(relation, new String[]{matcher.group(1), matcher.group(3)});
    }

    @Nullable
    private static Operand attemptParse(Function<String, Operand> parser, String str) {
        try {
            return parser.apply(str);
        } catch (Exception e) {
            return null;
        }
    }

    @NotNull
    public String pretty() {
        return left + " " + relation.getSymbol() + " " + right;
    }

    @Override
    public String toString() {
        return "Expression{" +
                "left=" + left +
                ", right=" + right +
                ", relation=" + relation +
                '}';
    }
}
