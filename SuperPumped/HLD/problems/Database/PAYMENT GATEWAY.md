
## The Core Problem Payment Gateway Solves

On the surface payment processing seems simple — charge a card, transfer money. But consider the real constraints:

```
Payment Gateway scale reality:
────────────────────────────────────────────────
Daily transactions:        500 million+ (Stripe/PayPal scale)
Peak transactions:         50,000+ per second (Black Friday)
Transaction value:         $100 billion+ per day
Fraud rate target:         <0.1% of transactions
Latency requirement:       <3 seconds end-to-end
Uptime requirement:        99.999% (5 minutes downtime/year)

Requirements:
→ ZERO double charges (idempotency critical)
→ ACID transactions (money cannot disappear)
→ Fraud detection in <100ms (real-time ML scoring)
→ PCI-DSS compliance (card data never stored in plain text)
→ Multi-currency support (150+ currencies)
→ Retry handling (network failures must not cause double charges)
→ Audit trail (every state change logged permanently)
→ Reconciliation (bank records must match our records exactly)
→ Chargeback handling (customer disputes)
→ Real-time notifications (webhooks to merchants)
```

This combination of **absolute money correctness + fraud detection + compliance + audit trail + high availability + idempotency** is what forces this specific architecture.

---

## Database Selection Overview

```
PAYMENT GATEWAY STACK:
════════════════════════════════════════════════════════

PostgreSQL:    Core transactions (ACID critical)
               - Payments, accounts, ledger entries
               - Double-entry bookkeeping
               - Idempotency keys

Redis:         Fraud detection + rate limiting + sessions
               - Real-time fraud signals
               - Idempotency key deduplication
               - Payment session state
               - Rate limiting per merchant

Cassandra:     Audit logs + event store
               - Immutable audit trail
               - Transaction event history
               - Compliance logs (7-year retention)

Message Queue: Kafka for async processing
               - Webhook delivery
               - Fraud scoring pipeline
               - Settlement processing
               - Retry logic
```

---

## Why PostgreSQL for Core Transactions?

#### The Money Correctness Problem

```
WHAT HAPPENS WITHOUT ACID:
════════════════════════════════════════════════════════

Customer pays merchant $100:
────────────────────────────────────────────────

WITHOUT TRANSACTIONS (DISASTER):
Step 1: Debit customer account: $1000 → $900  ✓
Step 2: Server crashes HERE
Step 3: Credit merchant account: NEVER HAPPENS

Result:
- Customer lost $100
- Merchant never received it
- $100 disappeared from system
- Both parties angry
- Lawsuit follows


WITH POSTGRESQL ACID TRANSACTIONS:
Step 1: BEGIN TRANSACTION
Step 2: Debit customer: $1000 → $900
Step 3: Credit merchant: $500 → $600
Step 4: Create ledger entries
Step 5: COMMIT

If server crashes at Step 2:
→ PostgreSQL WAL (Write-Ahead Log) detects incomplete transaction
→ On restart: ROLLBACK automatically
→ Customer balance restored to $1000
→ Merchant never credited
→ Both balances consistent
→ No money lost
→ ACID guarantee preserved
```

#### Double-Entry Bookkeeping Schema

```
DOUBLE-ENTRY BOOKKEEPING PRINCIPLE:
════════════════════════════════════════════════════════

Every financial transaction has two sides:
Debit one account + Credit another account

Example: Customer pays $100 to merchant:
Debit:  Customer account    -$100
Credit: Merchant account    +$100

Net effect: $0 (money conserved)

This principle has been used since 1494 (Luca Pacioli)
It's the foundation of all accounting
PostgreSQL enforces it through transactions

ACCOUNTING EQUATION:
Assets = Liabilities + Equity
Must ALWAYS balance
If it doesn't: money created or destroyed (illegal/buggy)
```

#### PostgreSQL Schema

```
POSTGRESQL SCHEMA:
════════════════════════════════════════════════════════

Accounts table:
────────────────────────────────────────────────────────────────────────────────
account_id │ user_id  │ account_type │ currency │ balance      │ status │ created_at
───────────────────────────────────────────────────────────────────────────────────────────
acc_001    │ user_001 │ customer     │ USD      │ 90000        │ active │ 2023-01-15
acc_002    │ merch_001│ merchant     │ USD      │ 60000        │ active │ 2022-06-20
acc_003    │ sys      │ gateway_fees │ USD      │ 5000         │ active │ 2020-01-01

balance stored in CENTS (avoid floating point errors):
$900.00 = 90000 cents
$600.00 = 60000 cents
$50.00 = 5000 cents

Columns:
  account_type: ENUM('customer', 'merchant', 'gateway', 'escrow', 'refund_pool')
  currency: CHAR(3)  -- ISO 4217: USD, EUR, GBP
  balance: BIGINT NOT NULL  -- CENTS (never FLOAT!)
  available_balance: BIGINT  -- balance minus holds
  hold_amount: BIGINT  -- funds on hold (pending transactions)

Indexes:
  PRIMARY KEY (account_id)
  INDEX (user_id, currency)
  CHECK (balance >= 0)  -- CRITICAL: prevent negative balance


Payments table:
────────────────────────────────────────────────────────────────────────────────
payment_id │ merchant_id │ customer_id │ amount │ currency │ status    │ idempotency_key       │ created_at
───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
pay_abc    │ merch_001   │ user_001    │ 10000  │ USD      │ completed │ idem_xyz_1708945200   │ 2024-02-26 10:00:00
pay_def    │ merch_001   │ user_002    │ 5000   │ USD      │ failed    │ idem_abc_1708945300   │ 2024-02-26 10:05:00
pay_ghi    │ merch_002   │ user_003    │ 25000  │ USD      │ pending   │ idem_def_1708945400   │ 2024-02-26 10:10:00

amount: BIGINT (cents)  -- $100.00 = 10000

status: ENUM(
  'initiated',    -- Payment request received
  'processing',   -- Sent to card network
  'authorized',   -- Card network approved
  'captured',     -- Funds captured
  'completed',    -- Money moved
  'failed',       -- Payment rejected
  'refunded',     -- Full refund issued
  'partial_refund',-- Partial refund issued
  'disputed',     -- Chargeback filed
  'cancelled'     -- Cancelled before processing
)

Indexes:
  PRIMARY KEY (payment_id)
  UNIQUE (idempotency_key)  ← CRITICAL: prevents double charges
  INDEX (merchant_id, created_at DESC)
  INDEX (customer_id, created_at DESC)
  INDEX (status, created_at)  ← "show pending payments"


Ledger_Entries table (double-entry bookkeeping):
────────────────────────────────────────────────────────────────────────────────
entry_id │ payment_id │ account_id │ entry_type │ amount  │ currency │ created_at          │ description
───────────────────────────────────────────────────────────────────────────────────────────────────────────────
ent_001  │ pay_abc    │ acc_001    │ debit      │ -10000  │ USD      │ 2024-02-26 10:00:01 │ Payment to merchant
ent_002  │ pay_abc    │ acc_002    │ credit     │ +9700   │ USD      │ 2024-02-26 10:00:01 │ Payment received
ent_003  │ pay_abc    │ acc_003    │ credit     │ +300    │ USD      │ 2024-02-26 10:00:01 │ Gateway fee (3%)

Every payment creates EXACTLY 3 ledger entries:
1. Debit customer  -$100.00
2. Credit merchant +$97.00  (after 3% fee)
3. Credit gateway  +$3.00   (fee)

Sum must equal ZERO: -10000 + 9700 + 300 = 0  ✓

Indexes:
  PRIMARY KEY (entry_id)
  INDEX (payment_id)
  INDEX (account_id, created_at DESC)


Idempotency_Keys table:
────────────────────────────────────────────────────────────────────────────────
idempotency_key      │ payment_id │ response_body │ created_at          │ expires_at
────────────────────────────────────────────────────────────────────────────────────────────
idem_xyz_1708945200  │ pay_abc    │ {...}         │ 2024-02-26 10:00:00 │ 2024-03-26 10:00:00
idem_abc_1708945300  │ pay_def    │ {...}         │ 2024-02-26 10:05:00 │ 2024-03-26 10:05:00

TTL: 30 days (after that, new payment allowed with same key)


Payment_Methods table:
────────────────────────────────────────────────────────────────────────────────
method_id  │ user_id  │ type        │ last_four │ expiry  │ token_vault_id     │ is_default
───────────────────────────────────────────────────────────────────────────────────────────────
meth_001   │ user_001 │ credit_card │ 4242      │ 12/2027 │ vault_token_abc123 │ true
meth_002   │ user_001 │ bank_account│ 6789      │ NULL    │ vault_token_def456 │ false

CRITICAL: card_number NEVER stored here
Only token_vault_id (reference to PCI-compliant vault)
Vault is separate isolated system (PCI-DSS Zone 1)


Merchants table:
────────────────────────────────────────────────────────────────────────────────
merchant_id │ business_name   │ api_key_hash   │ webhook_url               │ fee_rate │ status
───────────────────────────────────────────────────────────────────────────────────────────────
merch_001   │ Acme Corp       │ hash_abc123    │ https://acme.com/webhooks │ 0.029    │ active
merch_002   │ Beta Shop       │ hash_def456    │ https://beta.com/hooks    │ 0.025    │ active

fee_rate: NUMERIC(5,4)  -- 2.9% = 0.0290
api_key_hash: Hashed API key (never store plain text)


Refunds table:
────────────────────────────────────────────────────────────────────────────────
refund_id  │ payment_id │ amount │ reason          │ status    │ created_at
────────────────────────────────────────────────────────────────────────────────────
ref_001    │ pay_abc    │ 5000   │ customer_request│ completed │ 2024-02-27 10:00:00
ref_002    │ pay_abc    │ 5000   │ duplicate       │ pending   │ 2024-02-28 10:00:00

Partial refunds tracked separately
Total refunds cannot exceed original payment amount
```

#### Why Not NoSQL for Payments?

```
MONGODB ATTEMPT:
════════════════════════════════════════════════════════

{
  payment_id: "pay_abc",
  amount: 10000,
  customer_id: "user_001",
  merchant_id: "merch_001",
  ledger: [
    {account: "customer", change: -10000},
    {account: "merchant", change: +9700},
    {account: "fees", change: +300}
  ]
}

Problems for payments:
────────────────────────────────────────────────
✗ No true ACID multi-document transactions (pre v4.0)
✗ No CHECK constraints (balance >= 0)
✗ No FOREIGN KEY enforcement
✗ Eventual consistency unacceptable for money
✗ Cannot enforce double-entry (sum = 0)
✗ Aggregate queries slow (SUM balances across collections)
✗ No row-level locking for concurrent updates


CASSANDRA ATTEMPT:
════════════════════════════════════════════════════════

Problems:
────────────────────────────────────────────────
✗ No ACID transactions at all
✗ No foreign keys
✗ Eventual consistency (money can disappear in race)
✗ Cannot do "debit + credit atomically"
✗ No aggregations (SUM, COUNT with filters)
✗ Lightweight transactions (Paxos) too slow for payments


POSTGRESQL WINS BECAUSE:
────────────────────────────────────────────────
✓ ACID transactions (debit + credit = atomic)
✓ CHECK constraints (balance >= 0 enforced)
✓ Row-level locking (SELECT FOR UPDATE on accounts)
✓ SERIALIZABLE isolation (prevents phantom reads)
✓ Foreign keys (payment must reference valid account)
✓ Strong consistency (reads always see latest committed data)
✓ Complex queries (reconciliation, reporting, fraud patterns)
✓ Used by Stripe, PayPal, Square, Braintree in production
```

---

## Why Redis for Fraud Detection and Rate Limiting?

#### Real-Time Fraud Signals

```
FRAUD DETECTION REQUIREMENTS:
════════════════════════════════════════════════════════

Every payment must check:
- Is this card being used too frequently? (velocity check)
- Is this IP making too many failed attempts? (brute force)
- Is this merchant seeing unusual volume? (account takeover)
- Is the purchase amount anomalous for this user? (ML signal)
- Is this card flagged by bank? (real-time bank signal)

All checks must complete in <100ms
Before charging the card
At 50,000 transactions/second
```

#### Redis Schema for Fraud

```
REDIS SCHEMA:
════════════════════════════════════════════════════════

Velocity checks (sliding window counters):
────────────────────────────────────────────────
Key:   velocity:card:card_hash_abc:1h
Value: 5  (attempts in last hour)
TTL:   3600 seconds

Key:   velocity:card:card_hash_abc:24h
Value: 12  (attempts in last 24 hours)
TTL:   86400 seconds

INCR velocity:card:card_hash_abc:1h

Check:
GET velocity:card:card_hash_abc:1h
→ If value > 10: Flag as suspicious


IP-based fraud signals:
────────────────────────────────────────────────
Key:   fraud:ip:203.0.113.45:failures
Value: 3  (consecutive failures)
TTL:   3600 seconds

Key:   fraud:ip:203.0.113.45:score
Value: 85  (fraud risk score 0-100)
TTL:   3600 seconds

After 3 consecutive failures from same IP:
→ Temporary block


Blocklisted entities:
────────────────────────────────────────────────
Key:   blocklist:cards
Type:  SET
Value: {card_hash_abc, card_hash_xyz, ...}

Key:   blocklist:ips
Type:  SET
Value: {203.0.113.45, 198.51.100.0, ...}

SISMEMBER blocklist:cards "card_hash_abc"
→ Returns 1 (blocked!) or 0 (clean)

Query time: <1ms
Checked on every payment attempt


Payment session state:
────────────────────────────────────────────────
Key:   session:pay_session_abc
Type:  Hash
TTL:   900 seconds (15 minutes)

HSET session:pay_session_abc
  payment_id "pay_pending_123"
  user_id "user_001"
  amount "10000"
  currency "USD"
  step "card_validation"
  started_at "1708945200"

Tracks multi-step checkout flow
Auto-expires if user abandons


Rate limiting per merchant:
────────────────────────────────────────────────
Key:   ratelimit:merchant:merch_001:1min
Value: 523  (API calls in last minute)
TTL:   60 seconds

Key:   ratelimit:merchant:merch_001:1hour
Value: 28431  (API calls in last hour)
TTL:   3600 seconds

Lua script (atomic check + increment):
────────────────────────────────────────────────
local key = KEYS[1]
local limit = tonumber(ARGV[1])

local count = redis.call('INCR', key)
if count == 1 then
  redis.call('EXPIRE', key, 60)
end

if count > limit then
  return 0  -- Rate limited
else
  return 1  -- Allowed
end

Merchant rate limit: 1000 payments/minute
Prevents API abuse and DDoS


Idempotency key cache (fast duplicate check):
────────────────────────────────────────────────
Key:   idem:idem_xyz_1708945200
Value: "pay_abc"  (payment_id)
TTL:   86400 seconds (24 hours hot cache)

Before processing payment:
result = GET idem:idem_xyz_1708945200

If exists: Return cached payment_id (duplicate detected!)
If not: Process payment, then SET key


Real-time fraud score cache:
────────────────────────────────────────────────
Key:   fraud_score:user_001:card_hash
Value: '{"score": 23, "signals": ["new_device", "large_amount"]}'
TTL:   300 seconds (5 minutes)

ML model computed score cached
Avoids re-running expensive ML on every API call
```

---

## Why Cassandra for Audit Logs?

#### The Compliance Requirement

```
AUDIT LOG REQUIREMENTS:
════════════════════════════════════════════════════════

PCI-DSS Requirement 10:
"Track and monitor all access to network resources
 and cardholder data"

Every payment state change must be logged:
- Who made the change
- What changed
- When it changed
- What the previous state was
- What IP/device made the request

Retention: 7+ years (regulatory requirement)
Volume: 500M payments/day × 10 events each = 5B events/day
Writes: 57,870 events/second sustained
Query: "Show all events for payment pay_abc" (investigations)
Access pattern: Write heavy, occasional read (audits)

This is IMMUTABLE, append-only, time-series data
Perfect for Cassandra
```

#### Cassandra Schema

```
CASSANDRA SCHEMA:
════════════════════════════════════════════════════════

Payment_Events table (immutable audit log):
────────────────────────────────────────────────────────────────────────────────
payment_id │ event_time          │ event_id  │ event_type      │ actor_id  │ old_status │ new_status │ metadata
───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
pay_abc    │ 2024-02-26 10:00:00 │ evt_001   │ payment_init    │ user_001  │ NULL       │ initiated  │ {"ip": "..."}
pay_abc    │ 2024-02-26 10:00:01 │ evt_002   │ fraud_check     │ system    │ initiated  │ initiated  │ {"score": 23}
pay_abc    │ 2024-02-26 10:00:02 │ evt_003   │ card_auth       │ system    │ initiated  │ authorized │ {"auth_code": "..."}
pay_abc    │ 2024-02-26 10:00:03 │ evt_004   │ funds_captured  │ system    │ authorized │ captured   │ {"network": "visa"}
pay_abc    │ 2024-02-26 10:00:04 │ evt_005   │ payment_complete│ system    │ captured   │ completed  │ {"settled": true}

PRIMARY KEY ((payment_id), event_time, event_id)
CLUSTERING ORDER BY (event_time ASC, event_id ASC)

Partition by payment_id:
→ All events for one payment in same partition
→ Fast query: "Show history of payment pay_abc"
→ Forensics and investigations
→ Returns complete audit trail instantly


Transaction_Logs_By_Merchant table:
────────────────────────────────────────────────────────────────────────────────
merchant_id │ created_at          │ payment_id │ amount │ status    │ customer_id
───────────────────────────────────────────────────────────────────────────────────────
merch_001   │ 2024-02-26 10:00:00 │ pay_abc    │ 10000  │ completed │ user_001
merch_001   │ 2024-02-26 10:05:00 │ pay_def    │ 5000   │ failed    │ user_002

PRIMARY KEY ((merchant_id), created_at DESC, payment_id)

Query: "Show merchant's recent transactions"
Used for merchant dashboard and settlement


Settlement_Records table:
────────────────────────────────────────────────────────────────────────────────
merchant_id │ settlement_date │ settlement_id │ total_amount │ fee_amount │ net_amount │ status
───────────────────────────────────────────────────────────────────────────────────────────────────
merch_001   │ 2024-02-26      │ sett_001      │ 150000       │ 4350       │ 145650     │ completed
merch_001   │ 2024-02-25      │ sett_002      │ 120000       │ 3480       │ 116520     │ completed

Daily settlement records
7-year retention required


Compliance_Logs table:
────────────────────────────────────────────────────────────────────────────────
log_id    │ timestamp           │ log_type         │ actor     │ resource    │ action   │ ip_address
──────────────────────────────────────────────────────────────────────────────────────────────────────────
log_001   │ 2024-02-26 10:00:00 │ data_access      │ admin_001 │ payment_data│ read     │ 10.0.0.1
log_002   │ 2024-02-26 10:01:00 │ config_change    │ admin_001 │ fee_rate    │ update   │ 10.0.0.1

PCI-DSS requirement: log ALL data access
Even admin reads of sensitive data
TTL: 7 years (regulatory minimum)
```

---

## Idempotency: The Double Charge Problem

```
THE DOUBLE CHARGE NIGHTMARE:
════════════════════════════════════════════════════════

Without idempotency:
────────────────────────────────────────────────
T+0ms:    Merchant sends: "Charge user $100"
T+500ms:  Payment processing starts
T+2000ms: Network timeout (merchant never gets response!)
T+2001ms: Merchant retries: "Charge user $100"
T+2500ms: Second payment processing starts
T+3000ms: BOTH payments complete successfully!

Result: User charged $200 instead of $100
User files chargeback
Merchant refunds one charge
Gateway loses $3 fee on refund
Everyone unhappy


IDEMPOTENCY KEY SOLUTION:
════════════════════════════════════════════════════════

Merchant generates unique key before charging:
idempotency_key = UUID()  →  "idem_xyz_1708945200"

Merchant stores: {key: "idem_xyz", amount: 100, time: ...}

First request:
────────────────────────────────────────────────
POST /api/payments
Headers:
  Idempotency-Key: idem_xyz_1708945200

Body: {amount: 10000, currency: "USD", ...}

Backend:
1. Check Redis:
   GET idem:idem_xyz_1708945200
   → Returns NULL (not seen before)

2. Check PostgreSQL:
   SELECT payment_id, response_body
   FROM idempotency_keys
   WHERE idempotency_key = 'idem_xyz_1708945200';
   → Returns 0 rows (not processed yet)

3. Process payment → pay_abc created

4. Store in BOTH Redis AND PostgreSQL:
   -- Redis (fast cache, 24h TTL):
   SET idem:idem_xyz_1708945200 
       '{"payment_id": "pay_abc", "status": "completed"}' 
       EX 86400

   -- PostgreSQL (permanent storage, 30 days):
   INSERT INTO idempotency_keys
   (idempotency_key, payment_id, response_body, created_at, expires_at)
   VALUES
   ('idem_xyz_1708945200', 'pay_abc', '{"status": "completed"}', 
    NOW(), NOW() + INTERVAL '30 days');

5. Return response to merchant


Retry request (network timeout recovery):
────────────────────────────────────────────────
POST /api/payments
Headers:
  Idempotency-Key: idem_xyz_1708945200  ← SAME KEY

Backend:
1. Check Redis:
   GET idem:idem_xyz_1708945200
   → Returns '{"payment_id": "pay_abc", "status": "completed"}'
   → DUPLICATE DETECTED!

2. Return SAME response as before
   (do NOT charge card again)

3. Merchant receives: {payment_id: "pay_abc", status: "completed"}
   Merchant knows: Payment already processed, not double-charged


Result: Zero double charges
Even with network failures and retries
CRITICAL for payment systems
```

---

## PCI-DSS Compliance Architecture

```
CARD DATA SECURITY:
════════════════════════════════════════════════════════

PCI-DSS REQUIREMENT: Never store raw card numbers
────────────────────────────────────────────────

What users submit:
{
  card_number: "4242 4242 4242 4242",
  cvv: "123",
  expiry: "12/2027",
  cardholder: "John Doe"
}


What we NEVER store in our database:
────────────────────────────────────────────────
✗ Full card number (PAN)
✗ CVV/CVC code
✗ Magnetic stripe data
✗ PIN


Tokenization Flow:
────────────────────────────────────────────────

STEP 1: Card data collected in PCI-DSS Zone
────────────────────────────────────────────────
Frontend uses Stripe.js/Braintree SDK
Card data NEVER touches our servers directly
Encrypted in browser → sent directly to card vault

Our server only receives: token_vault_id = "tok_abc123"


STEP 2: Store token reference only
────────────────────────────────────────────────
INSERT INTO payment_methods
(method_id, user_id, type, last_four, expiry, token_vault_id)
VALUES
('meth_001', 'user_001', 'credit_card', '4242', '12/2027', 'tok_abc123');

last_four: Display purposes only (shows user "ending in 4242")
expiry: For display and pre-expiry warnings
token_vault_id: Pointer to actual card in vault
card_number: NEVER stored


STEP 3: Process payment using token
────────────────────────────────────────────────
To charge card:
gateway_client.charge(
  token: "tok_abc123",
  amount: 10000,
  currency: "USD"
)

Vault retrieves actual card number internally
Sends to card network (Visa/Mastercard)
Returns authorization code
We store only: auth_code, not card details


Database isolation (PCI Zones):
────────────────────────────────────────────────
Zone 1 (Most Restricted): Card vault
  - Stores actual card numbers (encrypted)
  - Hardware Security Module (HSM) for encryption keys
  - Only vault application can access
  - Network isolated from everything else
  - Separate audit logs

Zone 2 (Restricted): Payment processing
  - PostgreSQL with payment data
  - Token references only
  - Encrypted at rest (AES-256)
  - Encrypted in transit (TLS 1.3)
  - Access logged

Zone 3 (Regular): Analytics, reporting
  - Cassandra with audit logs
  - No card data whatsoever
  - Aggregated/anonymized data only
```

---

## Complete Payment Flow

```
FLOW 1: Customer Makes Payment
════════════════════════════════════════════════════════

Customer checks out on merchant website: $100 purchase
        │
        ▼
STEP 1: Merchant initiates payment via API
────────────────────────────────────────────────
POST /api/v1/payments
Headers:
  Authorization: Bearer sk_live_merchant_api_key
  Idempotency-Key: idem_xyz_1708945200

Body:
{
  amount: 10000,  // cents
  currency: "USD",
  payment_method_id: "meth_001",
  merchant_id: "merch_001",
  description: "Order #12345",
  metadata: {
    order_id: "order_12345",
    customer_email: "john@example.com"
  }
}
        │
        ▼
STEP 2: Authenticate merchant (Redis + PostgreSQL)
────────────────────────────────────────────────
Extract API key from header
Hash it: SHA256("sk_live_merchant_api_key")

Check Redis cache first:
GET merchant:api_key_hash_abc123
→ Returns "merch_001" (cache hit, <1ms)

If cache miss:
SELECT merchant_id, status, fee_rate
FROM merchants
WHERE api_key_hash = 'hash_abc123';

Validate:
- status = 'active' ✓
- Not suspended or banned ✓

Cache merchant: SET merchant:api_key_hash_abc123 "merch_001" EX 3600


STEP 3: Rate limit check (Redis Lua script)
────────────────────────────────────────────────
Check merchant rate limit:
EVAL rate_limit_script 1 
  ratelimit:merchant:merch_001:1min 
  1000  -- limit per minute

Returns: 1 (allowed)

Check global rate limit:
EVAL rate_limit_script 1
  ratelimit:global:1sec
  50000  -- 50K transactions/second max

Returns: 1 (allowed)


STEP 4: Idempotency check (Redis + PostgreSQL)
────────────────────────────────────────────────
Check Redis (fast path):
GET idem:idem_xyz_1708945200
→ Returns NULL (not in cache)

Check PostgreSQL (reliable path):
SELECT payment_id, response_body
FROM idempotency_keys
WHERE idempotency_key = 'idem_xyz_1708945200';

Returns: 0 rows → NEW PAYMENT (proceed)


STEP 5: Fraud detection (Redis ML signals)
────────────────────────────────────────────────
Parallel checks (all <100ms combined):

A. Blocklist check:
   SISMEMBER blocklist:cards "card_hash_abc" → 0 (clean)
   SISMEMBER blocklist:ips "203.0.113.45" → 0 (clean)

B. Velocity check:
   GET velocity:card:card_hash_abc:1h → 2 (below threshold)
   GET velocity:user:user_001:1h → 3 (below threshold)
   GET velocity:merchant:merch_001:1min → 52 (normal)

C. Cached fraud score:
   GET fraud_score:user_001:card_hash_abc
   → '{"score": 23, "signals": ["trusted_device"]}'
   Score: 23/100 (LOW RISK - proceed)

D. If no cached score:
   → Call ML fraud scoring service
   → Score = f(user_history, amount, time, location, ...)
   → Cache result: SET fraud_score:user_001:card_hash EX 300

Fraud decision:
Score 0-30: Low risk → ALLOW
Score 31-70: Medium risk → 3D Secure required
Score 71-100: High risk → DECLINE

Score 23 → ALLOW


STEP 6: Call card network (Visa/Mastercard)
────────────────────────────────────────────────
Request payment authorization:

gateway_client.authorize(
  token: "tok_abc123",  // from vault
  amount: 10000,
  currency: "USD",
  merchant: "ACME_CORP",
  descriptor: "ACME* ORDER 12345"
)

Card network checks:
- Is card valid? ✓
- Sufficient funds? ✓
- Not reported stolen? ✓
- Not over limit? ✓
- CVV matches? ✓

Response (200ms):
{
  authorized: true,
  auth_code: "AUTH_789",
  network_transaction_id: "visa_txn_abc"
}


STEP 7: Create payment record (PostgreSQL ACID)
────────────────────────────────────────────────
BEGIN TRANSACTION;

-- Create payment record
INSERT INTO payments
(payment_id, merchant_id, customer_id, amount, currency, status,
 idempotency_key, auth_code, network_txn_id, created_at)
VALUES
('pay_abc', 'merch_001', 'user_001', 10000, 'USD', 'authorized',
 'idem_xyz_1708945200', 'AUTH_789', 'visa_txn_abc', NOW());


-- Update account balances
-- Customer: debit $100 (put on hold)
UPDATE accounts
SET available_balance = available_balance - 10000,
    hold_amount = hold_amount + 10000
WHERE account_id = 'acc_001'
AND available_balance >= 10000;  -- Prevents overdraft

If rows_affected = 0:
  ROLLBACK;
  Return error: "Insufficient funds"


-- Create ledger entries (double-entry)
INSERT INTO ledger_entries (entry_id, payment_id, account_id, entry_type, amount)
VALUES
  ('ent_001', 'pay_abc', 'acc_001', 'debit', -10000),  -- customer
  ('ent_002', 'pay_abc', 'acc_002', 'credit', +9700),   -- merchant (97%)
  ('ent_003', 'pay_abc', 'acc_003', 'credit', +300);    -- gateway fee (3%)

-- Verify double-entry balance (SUM must = 0)
SELECT SUM(amount) FROM ledger_entries WHERE payment_id = 'pay_abc';
-- Must return: 0

-- Store idempotency key
INSERT INTO idempotency_keys
(idempotency_key, payment_id, response_body, created_at, expires_at)
VALUES
('idem_xyz_1708945200', 'pay_abc', '{"status": "authorized"}',
 NOW(), NOW() + INTERVAL '30 days');

COMMIT;
-- All operations atomic: all succeed or all fail


STEP 8: Capture funds
────────────────────────────────────────────────
(Sometimes same as auth, sometimes separate for pre-auth)

gateway_client.capture(
  auth_code: "AUTH_789",
  amount: 10000
)

Bank actually transfers money now


STEP 9: Update payment status (PostgreSQL)
────────────────────────────────────────────────
BEGIN TRANSACTION;

UPDATE payments
SET status = 'completed',
    completed_at = NOW()
WHERE payment_id = 'pay_abc';

-- Release customer hold, finalize
UPDATE accounts
SET hold_amount = hold_amount - 10000,
    balance = balance - 10000  -- Actually deduct now
WHERE account_id = 'acc_001';

-- Credit merchant available balance
UPDATE accounts
SET balance = balance + 9700,
    available_balance = available_balance + 9700
WHERE account_id = 'acc_002';

COMMIT;


STEP 10: Update caches and audit logs
────────────────────────────────────────────────
Redis updates:
-- Cache idempotency result
SET idem:idem_xyz_1708945200
    '{"payment_id": "pay_abc", "status": "completed"}'
    EX 86400

-- Update fraud velocity counters
INCR velocity:card:card_hash_abc:1h
INCR velocity:user:user_001:1h

-- Update merchant counter
INCR velocity:merchant:merch_001:1min


Cassandra audit log (async via Kafka):
INSERT INTO payment_events
(payment_id, event_time, event_id, event_type, actor_id, old_status, new_status, metadata)
VALUES
('pay_abc', NOW(), 'evt_005', 'payment_complete', 'system',
 'captured', 'completed', '{"settled": true, "net_amount": 9700}');


STEP 11: Notify merchant (async)
────────────────────────────────────────────────
Publish to Kafka: "payment_completed"

Webhook service consumes:
POST https://acme.com/webhooks
{
  "event": "payment.completed",
  "payment_id": "pay_abc",
  "amount": 10000,
  "currency": "USD",
  "status": "completed",
  "created_at": "2024-02-26T10:00:00Z"
}

Retry logic (exponential backoff):
- Attempt 1: immediate
- Attempt 2: 1 minute later
- Attempt 3: 5 minutes later
- Attempt 4: 30 minutes later
- Attempt 5: 2 hours later
Total: 5 attempts over 2 hours


STEP 12: Return response to merchant
────────────────────────────────────────────────
Response:
{
  payment_id: "pay_abc",
  status: "completed",
  amount: 10000,
  currency: "USD",
  created_at: "2024-02-26T10:00:00Z",
  merchant_net: 9700,
  gateway_fee: 300
}

Total end-to-end latency: ~2 seconds
Primarily determined by card network (1-2 seconds)
Our processing adds <100ms
```

```
FLOW 2: Fraud Detection (Declined Payment)
════════════════════════════════════════════════════════

Suspicious payment: $5000 from new device, unusual location
        │
        ▼
STEP 1: Check fraud signals (Redis)
────────────────────────────────────────────────
velocity:card:card_hash_abc:1h → 8 (high, but below 10)
velocity:card:card_hash_abc:24h → 23 (very high)
velocity:ip:198.51.100.1:1h → 15 (suspicious)
fraud_score:user_001:card_hash → NULL (no cache)


STEP 2: Run ML fraud scoring
────────────────────────────────────────────────
Features passed to model:
- Transaction amount: $5000 (10x user average)
- Location: Nigeria (unusual for this user)
- Device: New device fingerprint
- Time: 3 AM local time
- Velocity: 23 attempts in 24h
- Merchant category: Electronics (high-fraud category)

Model output:
{
  "score": 87,
  "signals": [
    "amount_anomaly",
    "new_device",
    "unusual_location",
    "high_velocity",
    "risky_merchant_category",
    "off_hours"
  ]
}

Score 87 → HIGH RISK → DECLINE


STEP 3: Record fraud event
────────────────────────────────────────────────
INSERT INTO payment_events (via Kafka → Cassandra)
(payment_id, event_type, metadata)
VALUES
('pay_xyz', 'fraud_decline', 
 '{"score": 87, "signals": ["amount_anomaly", "new_device", ...]}');

INCR velocity:ip:198.51.100.1:1h  -- Track this IP
INCR fraud:ip:198.51.100.1:failures


STEP 4: Return decline to merchant
────────────────────────────────────────────────
Response:
{
  status: "declined",
  decline_code: "fraud_suspected",
  message: "Transaction declined for security reasons"
}

Note: NEVER reveal fraud score to merchant
(fraudsters would optimize against it)


STEP 5: Alert user (if legitimate)
────────────────────────────────────────────────
Send email to user_001:
"Unusual activity detected on your account.
 Was this you? [Yes, it was me] [No, block this]"

If user says "Yes, it was me":
- Whitelist this device
- Lower fraud score
- User retries payment → succeeds

If user says "No":
- Block card
- Initiate fraud investigation
- Alert card network
```

```
FLOW 3: Refund Processing
════════════════════════════════════════════════════════

Customer requests refund for pay_abc
        │
        ▼
POST /api/v1/refunds
{
  payment_id: "pay_abc",
  amount: 10000,  // full refund
  reason: "customer_request"
}
        │
        ▼
STEP 1: Validate refund (PostgreSQL)
────────────────────────────────────────────────
SELECT p.payment_id, p.amount, p.status,
       COALESCE(SUM(r.amount), 0) as already_refunded
FROM payments p
LEFT JOIN refunds r ON p.payment_id = r.payment_id
WHERE p.payment_id = 'pay_abc'
GROUP BY p.payment_id, p.amount, p.status;

Returns:
- payment amount: 10000
- status: completed
- already_refunded: 0

Check:
- status = 'completed' ✓
- refund amount (10000) <= payment amount - already_refunded (10000 - 0 = 10000) ✓
- within refund window (within 90 days) ✓


STEP 2: Call card network for refund
────────────────────────────────────────────────
gateway_client.refund(
  transaction_id: "visa_txn_abc",
  amount: 10000
)

Card network initiates return:
Timeline: 3-5 business days for customer to see credit


STEP 3: Update database (PostgreSQL ACID)
────────────────────────────────────────────────
BEGIN TRANSACTION;

-- Record refund
INSERT INTO refunds (refund_id, payment_id, amount, reason, status)
VALUES ('ref_001', 'pay_abc', 10000, 'customer_request', 'processing');

-- Reverse ledger entries
INSERT INTO ledger_entries (entry_id, payment_id, account_id, entry_type, amount)
VALUES
  ('ent_010', 'pay_abc', 'acc_002', 'debit', -9700),  -- merchant loses net
  ('ent_011', 'pay_abc', 'acc_003', 'debit', -300),   -- gateway loses fee
  ('ent_012', 'pay_abc', 'acc_001', 'credit', +10000); -- customer gets back

-- Update payment status
UPDATE payments
SET status = 'refunded'
WHERE payment_id = 'pay_abc';

-- Update balances
UPDATE accounts SET balance = balance - 9700
WHERE account_id = 'acc_002';  -- Merchant pays back

UPDATE accounts SET balance = balance - 300
WHERE account_id = 'acc_003';  -- Gateway absorbs fee

UPDATE accounts SET balance = balance + 10000
WHERE account_id = 'acc_001';  -- Customer restored

COMMIT;


STEP 4: Audit log (Cassandra via Kafka)
────────────────────────────────────────────────
INSERT INTO payment_events
(payment_id, event_type, old_status, new_status, metadata)
VALUES
('pay_abc', 'refund_initiated', 'completed', 'refunded',
 '{"refund_id": "ref_001", "amount": 10000, "reason": "customer_request"}');


STEP 5: Notify merchant webhook
────────────────────────────────────────────────
POST https://acme.com/webhooks
{
  "event": "payment.refunded",
  "payment_id": "pay_abc",
  "refund_id": "ref_001",
  "amount": 10000,
  "status": "refunded"
}
```

```
FLOW 4: Daily Settlement
════════════════════════════════════════════════════════

End of day: Transfer funds to merchant's bank account
        │
        ▼
STEP 1: Calculate settlement (PostgreSQL)
────────────────────────────────────────────────
SELECT 
  merchant_id,
  SUM(CASE WHEN status = 'completed' THEN amount ELSE 0 END) as gross_amount,
  SUM(CASE WHEN status = 'completed' THEN gateway_fee ELSE 0 END) as total_fees,
  SUM(CASE WHEN status = 'refunded' THEN amount ELSE 0 END) as total_refunds,
  SUM(CASE WHEN status = 'completed' THEN merchant_net ELSE 0 END)
  - SUM(CASE WHEN status = 'refunded' THEN amount ELSE 0 END) as net_settlement
FROM payments
WHERE merchant_id = 'merch_001'
AND DATE(created_at) = '2024-02-26'
AND status IN ('completed', 'refunded');

Returns:
- gross: $150,000
- fees: $4,350
- refunds: $5,000
- net settlement: $140,650


STEP 2: Initiate bank transfer (ACH/wire)
────────────────────────────────────────────────
bank_client.transfer(
  from: gateway_bank_account,
  to: "merch_001_bank_account_routing_xxxx",
  amount: 14065000,  // cents
  description: "Settlement 2024-02-26"
)


STEP 3: Record settlement
────────────────────────────────────────────────
INSERT INTO settlement_records (via Cassandra)
(merchant_id, settlement_date, settlement_id,
 total_amount, fee_amount, net_amount, status)
VALUES
('merch_001', '2024-02-26', 'sett_001',
 15000000, 435000, 14065000, 'completed');
```

---

## System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                    CLIENT APPLICATIONS                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │  Web Browser │  │ Merchant SDK │  │  Mobile App  │         │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘         │
└─────────┼─────────────────┼─────────────────┼──────────────────┘
          │                 │                 │
          └─────────────────┼─────────────────┘
                            │ HTTPS/TLS 1.3
                    ┌───────▼────────┐
                    │  API Gateway   │
                    │ (Auth+Rate Lmt)│
                    └───────┬────────┘
                            │
         ┌──────────────────┼──────────────────┐
         │                  │                  │
         ▼                  ▼                  ▼
  ┌────────────┐    ┌────────────┐    ┌────────────┐
  │  Payment   │    │   Fraud    │    │  Merchant  │
  │  Service   │    │  Service   │    │  Service   │
  └─────┬──────┘    └─────┬──────┘    └─────┬──────┘
        │                 │                  │
   ┌────┴────┐       ┌────┴────┐       ┌────┴────┐
   │         │       │         │       │         │
   ▼         ▼       ▼         ▼       ▼         ▼
┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐
│Postgr│ │Redis │ │Redis │ │ML    │ │Postgr│ │Cassan│
│SQL   │ │(idem)│ │(fraud│ │Model │ │SQL   │ │dra   │
│(core)│ │      │ │score)│ │      │ │(merch│ │(audit│
└──────┘ └──────┘ └──────┘ └──────┘ └──────┘ └──────┘
        │
        ▼
  ┌──────────┐      ┌──────────────┐
  │  Kafka   │ ───→ │  Card Network│
  │(async)   │      │ (Visa/Mcard) │
  └──────────┘      └──────────────┘
        │
   ┌────┴────┐
   ▼         ▼
┌──────┐ ┌──────────┐
│Webhk │ │Settlement│
│Svc   │ │Service   │
└──────┘ └──────────┘
```

---

## Tradeoffs vs Other Databases

```
┌───────────────────────────────┬──────────────┬──────────────┬──────────────┬──────────────┐
│                               │ THIS ARCH    │ MONGO ALL    │ CASSANDRA ALL│ DYNAMODB ALL │
├───────────────────────────────┼──────────────┼──────────────┼──────────────┼──────────────┤
│ ACID transactions (critical)  │ PostgreSQL✓  │ Limited ✗    │ NO ✗         │ Limited ✗    │
│ Double-entry enforcement      │ PostgreSQL✓  │ Manual ✗     │ Impossible✗  │ Manual ✗     │
│ Idempotency (unique key)      │ PostgreSQL✓  │ Unique idx✓  │ LWT (slow)✗  │ Conditional✓ │
│ Fraud signals (<1ms)          │ Redis ✓      │ Slow ✗       │ Slow ✗       │ DAX ✓        │
│ Rate limiting (atomic)        │ Redis Lua✓   │ Manual ✗     │ LWT (slow)✗  │ Manual ✗     │
│ Audit logs (immutable)        │ Cassandra✓   │ MongoDB ✓    │ Cassandra ✓  │ Streams ✓    │
│ Compliance retention (7yr)    │ Cassandra✓   │ Storage ✓    │ Cassandra ✓  │ Expensive ✗  │
│ Balance queries (SUM, GROUP)  │ PostgreSQL✓  │ Pipeline     │ NO ✗         │ GSI limited  │
│ Overdraft prevention          │ CHECK+Lock✓  │ Impossible✗  │ Impossible✗  │ Conditional✓ │
│ Settlement queries            │ PostgreSQL✓  │ Pipeline     │ Limited      │ GSI scan ✗   │
│ Operational complexity        │ HIGH         │ MEDIUM       │ HIGH         │ LOW (managed)│
│ PCI-DSS compliance            │ Proven ✓     │ Possible     │ Possible     │ AWS certify✓ │
└───────────────────────────────┴──────────────┴──────────────┴──────────────┴──────────────┘
```

---

## One Line Summary

> **PostgreSQL stores all financial transactions using ACID guarantees because payment processing requires atomic operations where debiting customer (-$100), crediting merchant (+$97), and recording gateway fee (+$3) must ALL succeed or ALL fail — no partial state is acceptable since money cannot disappear or be created, enforced through BEGIN TRANSACTION/COMMIT blocks with row-level locking (SELECT FOR UPDATE on accounts preventing overdraft) and CHECK constraints (balance >= 0) that MongoDB's eventual consistency and Cassandra's lack of multi-row transactions fundamentally cannot provide — double-entry bookkeeping ledger entries enforce that SUM(all entries per payment) = 0 always, making financial inconsistencies mathematically impossible at the database level — UNIQUE constraint on idempotency_key prevents double charges when merchants retry failed requests by returning the cached previous response instead of creating new payments, stored in both Redis (24-hour fast cache) and PostgreSQL (30-day durable store) for the two-layer deduplication that handles both hot retries and cold recovery — Redis handles real-time fraud detection through sub-millisecond velocity checks (INCR velocity:card:hash:1h with TTL), blocklist lookups (SISMEMBER on card/IP sets), cached ML fraud scores (SET fraud_score:user:card EX 300), and rate limiting with Lua scripts providing atomic check-and-increment that prevents race conditions — all returning fraud decisions in <100ms before charging any card — Cassandra stores the immutable audit trail partitioned by payment_id with clustering on timestamp ASC enabling complete forensic reconstruction "show all events for payment pay_abc" in <50ms through single partition reads, while 7-year TTL-based retention satisfies PCI-DSS compliance without manual deletion jobs — card numbers are NEVER stored anywhere in our database (only vault tokens), satisfying PCI-DSS through tokenization where the browser-side SDK sends raw card data directly to the isolated card vault returning only tok_abc123 which our servers use to charge without ever seeing actual card numbers — Kafka decouples the critical path (payment processing < 2 seconds) from slow operations (webhook delivery, settlement, ML model retraining) through async event publishing where payment_completed events trigger webhook delivery with 5-attempt exponential backoff, daily settlement calculations aggregating completed transactions, and fraud model retraining on new patterns without blocking any payment from completing — the entire system achieves 99.999% uptime through PostgreSQL streaming replication, Redis Cluster with automatic failover, and Cassandra's tunable replication (RF=3) providing zero-downtime database failures.**

Would you like me to explain how 3D Secure authentication works adding an extra verification layer for high-fraud-risk transactions, or how the reconciliation system automatically detects and resolves mismatches between our internal ledger and bank statements to ensure every dollar is accounted for?

You are out of free [messages](https://support.claude.com/en/articles/11647753-understanding-usage-and-length-limits) until 1:50 AM