

==INTRODUCTION==

An ATM (Automated Teller Machine) system is designed to efficiently handle customer banking operations, authenticate users, process transactions, and manage cash inventory. The system needs to support multiple transaction types, maintain security, handle various states of operation, and provide a seamless user experience. The system should be reliable and capable of handling different machine states and transaction processing.



1. user will perform different operation on a single ATM machine or system will perform actions on a single atm machine
2. Users can insert cards, authenticate, and perform various banking operations.
3. The system authenticates users via PIN verification before allowing transactions.
4. The ATM maintains cash inventory and prevents dispensing when insufficient.
5.  The system transitions through various states during the transaction cycle.



==clarification questions== 

1. what all kind of operations  do we need to perform for banking transactions 
2.  How should the system handle user authentication?

Answers 

  1. Supports basic operations like cash withdrawal and balance checking
  2. Handles card insertion and PIN-based authentication.

==KEY COMPONENTS==

Card : 
Account
User
Atm



==OPERATIONS==

insertcard();
authenticate pin()
select operation()
perform operation()




==DESIGN PATTERNS==

*STATE*

Atm State
CashWithdrawl State
Checkbalance State
HasCard State
selectOperation State




 