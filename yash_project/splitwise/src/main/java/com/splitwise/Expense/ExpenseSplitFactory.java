package com.splitwise.Expense;

import com.splitwise.Expense.Split.EqualExpenseSplit;
import com.splitwise.Expense.Split.ExpenseSplit;
import com.splitwise.Expense.Split.PercentageExpenseSplit;
import com.splitwise.Expense.Split.UnequalExpenseSplit;

public class ExpenseSplitFactory {

    public static ExpenseSplit getSplitObject(ExpenseSplitType splitType){
        switch (splitType){
            case EQUAL:
                return new EqualExpenseSplit();
            case UNEQUAL:
                return new UnequalExpenseSplit();
            case PERCENTAGE:
                return new PercentageExpenseSplit();
            default:
                return null;

        }
    }
}
