package com.ATM.AmountWithdrawl;

import com.ATM.ATM;
import com.ATM.ATMSTATE.CashWithdrawalState;

public abstract class CashWithdrawProcessor {

    CashWithdrawProcessor nextCashWithdrawProcessor;

    public CashWithdrawProcessor(CashWithdrawProcessor cashWithdrawProcessor){
         this.nextCashWithdrawProcessor = cashWithdrawProcessor;

    }

    public void withdraw(ATM atm, int remainingAmount) {

        if (nextCashWithdrawProcessor != null) {
            nextCashWithdrawProcessor.withdraw(atm, remainingAmount);
        }
    }


}
