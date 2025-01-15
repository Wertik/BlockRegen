package nl.aurorion.blockregen.preset.condition.expression;

import com.linecorp.conditional.ConditionContext;
import lombok.Getter;

public class Constant implements Operand {
    @Getter
    private final Object value;

    public Constant(Object value) {
        this.value = value;
    }

    @Override
    public Object value(ConditionContext ctx) {
        return this.value;
    }

    @Override
    public String toString() {
        return "Constant{" +
                "value=" + value +
                '}';
    }
}
