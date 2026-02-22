package nl.aurorion.blockregen.preset.condition.expression;

import com.google.common.base.Strings;
import nl.aurorion.blockregen.Context;
import lombok.Getter;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.ParseException;
import nl.aurorion.blockregen.configuration.LoadResult;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log
public class Expression {

    public static final Pattern SYMBOL_PATTERN = Pattern.compile("(\\S+)\\s*(>=|<=|==|!=|<|>)\\s*(\\S+)");

    @Getter
    private final Operand left;
    @Getter
    private final Operand right;

    @Getter
    private final OperandRelation relation;

    private boolean staticResult = false;

    private Expression(Operand left, Operand right, OperandRelation relation) {
        this.left = left;
        this.right = right;
        this.relation = relation;
    }

    public boolean isConstant() {
        return left instanceof Constant && right instanceof Constant;
    }

    // Evaluate if the expression is constant
    public void evaluateStatic() {
        this.staticResult = this.relation.evaluate(
                this.left.value(null),
                this.right.value(null)
        );
        log.fine(() -> "Expression " + this + " evaluated statically to " + this.staticResult);
    }

    /**
     * @throws ParseException If the comparator cannot get objects of comparable types.
     * */
    public boolean evaluate(@NotNull Context ctx) {
        if (isConstant()) {
            return this.staticResult;
        }

        Object o1 = this.left.value(ctx);
        Object o2 = this.right.value(ctx);

        log.fine(() -> "Evaluate " + this + " " + o1 + " " + relation + " " + o2);

        return this.relation.evaluate(o1, o2);
    }

    /**
     * @throws ParseException If the static evaluation of the condition fails to compare objects.
     */
    @NotNull
    public static Expression of(@NotNull Operand left, @NotNull Operand right, @NotNull OperandRelation relation) {
        Expression expression = new Expression(left, right, relation);

        if (expression.isConstant()) {
            expression.evaluateStatic();
        }

        return expression;
    }

    /**
     * @throws ParseException If the parsing fails.
     */
    @NotNull
    public static Expression from(@NotNull String input) {
        if (Strings.isNullOrEmpty(input)) {
            throw new ParseException("Expression input cannot be empty or null.");
        }

        Matcher matcher = SYMBOL_PATTERN.matcher(input);

        if (!matcher.find()) {
            throw new ParseException("Invalid expression '" + input + "'");
        }

        // Figure out if it's constant or a variable.
        Operand op1 = Operand.Parser.parse(matcher.group(1));
        Operand op2 = Operand.Parser.parse(matcher.group(3));

        String operator = matcher.group(2);
        OperandRelation relation = OperandRelation.parse(operator);
        if (relation == null) {
            throw new ParseException("Invalid relation operator '" + operator + "'.");
        }

        Expression expression = Expression.of(op1, op2, relation);
        log.fine(() -> "Parsed expression: " + expression);
        return expression;
    }

    /**
     * Attempt to parse operands using a provided parser.
     */
    @NotNull
    public static Expression withCustomOperands(@NotNull Function<String, Operand> parser, @NotNull String input) {
        if (Strings.isNullOrEmpty(input)) {
            throw new ParseException("Expression input cannot be empty or null.");
        }

        Matcher matcher = Expression.SYMBOL_PATTERN.matcher(input);

        if (!matcher.find()) {
            throw new ParseException("Invalid expression " + input);
        }

        OperandRelation relation = OperandRelation.parse(matcher.group(2));
        if (relation == null) {
            throw new ParseException("Invalid relation operator.");
        }

        LoadResult<Operand, Exception> o1 = attemptParse(parser, matcher.group(1));
        LoadResult<Operand, Exception> o2 = attemptParse(parser, matcher.group(3));

        if (o1.isError() && o2.isError()) {
            throw new ParseException("No variable operand in expression '" + input + "'. Operand 1: " + o1.error().getMessage() + " Operand 2: " + o2.error().getMessage());
        }

        o1.ifError(new Constant(Operand.Parser.parseObject(matcher.group(1))));
        o2.ifError(new Constant(Operand.Parser.parseObject(matcher.group(3))));

        log.fine("ops: " + o1.get() + " " + o2.get());

        return Expression.of(o1.get(), o2.get(), relation);
    }

    @NotNull
    private static LoadResult<Operand, Exception> attemptParse(Function<String, Operand> parser, String str) {
        try {
            return LoadResult.of(parser.apply(str));
        } catch (Exception e) {
            return LoadResult.error(e);
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
