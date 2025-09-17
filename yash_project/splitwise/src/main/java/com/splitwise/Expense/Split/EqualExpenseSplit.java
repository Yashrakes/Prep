package com.splitwise.Expense.Split;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class EqualExpenseSplit implements ExpenseSplit{
    @Override
    public void validateSplitRequest(List<Split> splitList, double totalAmount)
    {
        double amountShouldBePresent = totalAmount/splitList.size();
        Predicate<Split> amountEqualsX = x->x.getAmountOwe() == amountShouldBePresent;

        Boolean temp = splitList.stream().allMatch(amountEqualsX);

//        Function<Split,Double> getAmountFn = Split::getAmountOwe;
//        Boolean temp1 = splitList.stream().allMatch(x-> getAmountFn.apply(x) == amountShouldBePresent);
        if(!temp){
            //exception
        }

    }
}
