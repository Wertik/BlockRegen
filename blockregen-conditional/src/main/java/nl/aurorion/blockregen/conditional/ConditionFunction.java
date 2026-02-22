package nl.aurorion.blockregen.conditional;

import nl.aurorion.blockregen.Context;

public interface ConditionFunction {
    boolean match(Context context);
}
