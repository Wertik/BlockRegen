package nl.aurorion.blockregen.preset.condition.expression;

import com.google.common.base.Strings;
import com.linecorp.conditional.ConditionContext;
import lombok.Getter;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.configuration.ParseException;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log
public class Expression {
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

    public boolean evaluate(ConditionContext ctx) {
        Object o1 = this.left.value(ctx);
        Object o2 = this.right.value(ctx);

        log.fine(() -> "Evaluate " + this + " " + o1 + " " + relation + " " + o2);

        return this.relation.evaluate(o1, o2);
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

        Pattern p = Pattern.compile("(.+)\\s*(<|>|>=|<=|==|!=)\\s*(.+)");

        Matcher matcher = p.matcher(input);

        if (!matcher.matches()) {
            throw new ParseException("Invalid expression '" + input + "'");
        }

        // Figure out if it's constant or a variable.
        Operand op1 = Operand.parse(matcher.group(1));
        Operand op2 = Operand.parse(matcher.group(3));

        String operator = matcher.group(2);
        OperandRelation relation = OperandRelation.parse(operator);

        if (relation == null) {
            throw new ParseException("Invalid relation operator.");
        }

        Expression expression = new Expression(op1, op2, relation);
        log.fine(() -> "Parsed expression: " + expression);
        return expression;
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
