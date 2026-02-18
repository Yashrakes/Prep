
# The Transform Function — The Heart of OT

The transform function `T(op1, op2)` takes two concurrent operations and returns a _new version of op1_ adjusted to account for the fact that op2 has already happened.

Let's write out all the cases for a simple text editor with just two operation types: **Insert** and **Delete**.

### Case 1: Insert vs Insert

```
T(Insert(pos1, char1), Insert(pos2, char2)):

  if pos1 < pos2:
      return Insert(pos1, char1)   // op1 is before op2, no shift needed
  
  if pos1 > pos2:
      return Insert(pos1 + 1, char1)  // op2 inserted before op1's target, shift right
  
  if pos1 == pos2:
      // TIE BREAKER — both want to insert at same spot
      // Use client ID or timestamp to break tie deterministically
      if client1_id < client2_id:
          return Insert(pos1, char1)      // client1 goes first, no shift
      else:
          return Insert(pos1 + 1, char1)  // client2 goes first, client1 shifts right
```

The tie-breaking rule is crucial. It must be deterministic and agreed upon by all clients, because _both_ clients are transforming against each other simultaneously. If they use different tie-breaking logic, they'll diverge.

### Case 2: Delete vs Insert

```
T(Delete(pos1), Insert(pos2, char2)):

  if pos1 < pos2:
      return Delete(pos1)      // delete is before the insertion, unaffected
  
  if pos1 >= pos2:
      return Delete(pos1 + 1)  // insertion happened before delete's target, shift right
```

### Case 3: Insert vs Delete

```
T(Insert(pos1, char1), Delete(pos2)):

  if pos2 < pos1:
      return Insert(pos1 - 1, char1)  // deletion before insert target, shift left
  
  if pos2 >= pos1:
      return Insert(pos1, char1)      // deletion was after insert target, no effect
```

### Case 4: Delete vs Delete — The Tricky One

```
T(Delete(pos1), Delete(pos2)):

  if pos1 < pos2:
      return Delete(pos1)       // deleting before op2's target, unaffected
  
  if pos1 > pos2:
      return Delete(pos1 - 1)   // op2 deleted before pos1, shift left
  
  if pos1 == pos2:
      // BOTH deleted the same character!
      // The character is already gone.
      return NoOp()             // transformed to a no-operation
```

### Where OT sits in HLD

```
┌─────────────────────────────────────────────────────┐
│                    CLIENT                           │
│  ┌──────────┐    ┌──────────┐    ┌──────────────┐  │
│  │  Editor  │───>│ OT Client│───>│ WebSocket    │  │
│  │   (UI)   │<───│  Engine  │<───│  Connection  │  │
│  └──────────┘    └──────────┘    └──────────────┘  │
└─────────────────────────────────────────────────────┘
                         │ WebSocket
┌─────────────────────────────────────────────────────┐
│                COLLABORATION SERVER                  │
│  ┌──────────────────────────────────────────────┐   │
│  │           OT Server Engine                   │   │
│  │  - Maintains Operation History               │   │
│  │  - Transforms concurrent ops                 │   │
│  │  - Broadcasts transformed ops                │   │
│  └──────────────────────────────────────────────┘   │
│                       │                             │
│  ┌────────────┐  ┌─────────────┐  ┌─────────────┐  │
│  │ Operations │  │  Document   │  │  Presence   │  │
│  │    Log     │  │    Store    │  │   Service   │  │
│  │ (Kafka)    │  │  (Storage)  │  │  (Redis)    │  │
│  └────────────┘  └─────────────┘  └─────────────┘  │
```

Let me walk through each scenario one by one, building a concrete picture in your head so the logic feels _obvious_, not memorized.

---

## The Setup: What Are We Actually Transforming?

Before we dive in, burn this mental model into your head:

**`T(op1, op2)` answers this question: "I wrote op1 assuming the document was in state S. But op2 has already been applied to the document, changing it. What should op1 become so it still achieves my original intention?"**

In other words, op2 is the _already happened_ thing. op1 is the _incoming_ thing that needs to be adjusted.

With that framing, every rule below will feel like common sense.

---

## Scenario 1: `pos1 < pos2` → Return `Insert(pos1, char1)` unchanged

### The Situation

op1 wants to insert _before_ where op2 inserted. op2's insertion is somewhere to the _right_ of op1's target.

### Concrete Example

```
Document: "ABCD"
           0123

op1 (Priya):  Insert 'X' at position 1   (between A and B)
op2 (Rahul):  Insert 'Y' at position 3   (between C and D)
```

Rahul's op2 has already been applied to the document. So the document now looks like this:

```
"ABCD" → Insert 'Y' at 3 → "ABCYD"
           01234
```

Now Priya's op1 arrives. She wants to insert 'X' at position 1. Think about it visually — she wants to go between 'A' and 'B'. Rahul inserted 'Y' over at position 3, which is _to the right_ of Priya's target. **Rahul's insertion doesn't affect anything to the left of position 3.** Position 1 is still exactly between 'A' and 'B'.

```
"ABCYD" → Insert 'X' at 1 → "AXBCYD"
```

So the transform returns op1 completely unchanged. **No adjustment needed because the already-applied op2 is to the right and cannot affect positions to its left.**

### The Mental Rule

Think of it like a row of seats. If someone sits down to your _right_, your seat number doesn't change. You're unaffected.

---

## Scenario 2: `pos1 > pos2` → Return `Insert(pos1 + 1, char1)`

### The Situation

op1 wants to insert _after_ where op2 inserted. op2's insertion is somewhere to the _left_ of op1's target. This means op2's insertion has already pushed everything to the right — including op1's intended target position.

### Concrete Example

```
Document: "ABCD"
           0123

op1 (Priya):  Insert 'X' at position 3   (between C and D)
op2 (Rahul):  Insert 'Y' at position 1   (between A and B)
```

Rahul's op2 has already been applied:

```
"ABCD" → Insert 'Y' at 1 → "AYBC D"
           0 1 2 3 4
```

Now look carefully. The character 'C' that Priya wanted to insert _after_ was originally at position 2. After Rahul inserted 'Y' at position 1, **every character from position 1 onwards shifted one step to the right.** So 'C' is now at position 3, and 'D' is now at position 4.

If Priya's op1 is applied naively at position 3, she inserts after 'C' — which is exactly what she intended! Wait... but that's correct. Let me use a cleaner example where the problem is more visible.

```
Document: "ABCD"
           0123

op1 (Priya):  Insert 'X' at position 2   (she wants to go between B and C)
op2 (Rahul):  Insert 'Y' at position 1   (between A and B)
```

Rahul's op2 applied first:

```
"ABCD" → Insert 'Y' at 1 → "AYBCD"
           0 1 2 3 4
```

Now Priya arrives wanting to insert 'X' at position 2. But look at the document now — position 2 is where 'B' sits, not the gap between 'B' and 'C'. Priya's original intention was the gap _between B and C_, which has now shifted to position 3.

If we apply naively at position 2: `"AYBCD"` → insert X at 2 → `"AYXBCD"` — X lands between Y and B, which is **not what Priya intended.** She wanted to be between B and C.

So we transform: since Rahul inserted at position 1, which is _before_ Priya's target of 2, we shift Priya's target right by 1:

```
Transformed op1: Insert 'X' at position 3

"AYBCD" → Insert 'X' at 3 → "AYBXCD"  ✓
```

Now 'X' is correctly between 'B' and 'C'. Priya's intention is preserved.

### The Mental Rule

Think of it like a queue. If someone cuts in line _in front of you_, your position number increases by 1. You get pushed back. Every insertion to your _left_ shifts your target one step to the right.

And if multiple insertions happen to your left? You shift by the count of those insertions. If 3 people cut in front of you, your position goes up by 3. The +1 in the formula represents exactly one such insertion.

---

## Scenario 3: `pos1 == pos2` → The Tie-Breaker

### The Situation

This is the genuinely tricky one. Both users want to insert at the **exact same position**. The document doesn't have a "natural" winner here — we have to make an arbitrary but _consistent_ choice.

### Why Consistency Is Everything

Here's the key insight. On Priya's machine, she's computing `T(op1, op2)` — transforming her op against Rahul's. On Rahul's machine, he's computing `T(op2, op1)` — transforming his op against Priya's. Both machines are doing this simultaneously, completely independently, with no communication. They must both arrive at the **same final document state.** If they use different rules, the documents diverge forever.

This means the tie-breaking rule must be a _pure function_ of information both sides already have. The most common choice is client ID.

### Concrete Example

```
Document: "ABCD"
           0123

op1 (Priya,  client_id = "P"):  Insert 'X' at position 2
op2 (Rahul, client_id = "R"):  Insert 'Y' at position 2
```

Both want to insert at position 2, right between 'B' and 'C'. One of them must go first. We use alphabetical order of client IDs to decide — "P" comes before "R" alphabetically, so Priya goes first (lower ID = higher priority).

**On the server**, Rahul's op2 is applied first (let's say it arrived first). The server then needs to transform Priya's op1 against Rahul's op2:

```
T(op1, op2):
  pos1 == pos2 (both are 2)
  client1_id = "P", client2_id = "R"
  "P" < "R" → Priya wins → return Insert(pos1, char1) = Insert(2, 'X')
  (no shift, Priya goes first)
```

The document after applying op2 (Rahul's Y at 2) is `"ABYDCD"` wait let me redo this cleanly:

```
"ABCD" → Apply op2 first: Insert 'Y' at 2 → "ABYCD"
           0 1 2 3 4
```

Now apply transformed op1 (Insert 'X' at 2, unchanged):

```
"ABYCD" → Insert 'X' at 2 → "ABXYCD"
```

**On Rahul's machine**, Priya's op1 is applied first (locally). Then the server sends back op2 transformed against op1:

```
T(op2, op1):
  pos2 == pos1 (both are 2)
  client2_id = "R", client1_id = "P"
  "R" > "P" → Rahul loses → return Insert(pos2 + 1, char2) = Insert(3, 'Y')
  (shift right, Rahul goes second)
```

```
"ABCD" → Apply op1 first (Priya's X at 2): "ABXCD"
        → Apply transformed op2 (Rahul's Y at 3): "ABXYCD"  ✓
```

Both machines converge to `"ABXYCD"`. Priya's 'X' is before Rahul's 'Y', consistently decided by their client IDs.

### What If You Used Timestamps Instead?

Timestamps are tempting but dangerous in distributed systems. Two machines' clocks are never perfectly synchronized — a phenomenon called **clock skew**. Priya's machine might say 10:00:00.500 and Rahul's might say 10:00:00.499 even though they typed at the same instant. If timestamps are equal (which can happen), you still need a secondary tiebreaker. So in practice, systems use a **Lamport clock** (a logical counter) combined with client ID, which gives you a globally consistent ordering without relying on wall-clock time.

---

## Putting All Three Together: A Single Document, All Scenarios

Let's watch all three scenarios play out in one example flow.

```
Document: "ACD"
           012

Three concurrent operations:

op1 (User1, id="U1"):  Insert 'B' at position 1  (wants "ABCD")
op2 (User2, id="U2"):  Insert 'X' at position 1  (wants "AXCD")  ← same pos as op1!
op3 (User3, id="U3"):  Insert 'Z' at position 2  (wants "ACZD")
```

Server receives them in order: op1, op2, op3.

**Applying op1 first:** `"ACD"` → `"ABCD"`

**Now transform op2 against op1:**

```
T(op2, op1):  both want position 1
  pos1==pos2, id="U2" vs id="U1"
  "U2" > "U1" → U2 loses, shift right
  → Insert('X', position 2)
```

Apply transformed op2: `"ABCD"` → `"ABXCD"`

**Now transform op3 against op1, then against transformed op2:**

First, transform op3 against op1:

```
T(op3, op1):  op3 wants pos 2, op1 inserted at pos 1
  pos_op1 (1) < pos_op3 (2) → op1 is to the LEFT of op3's target
  → shift op3 right by 1 → Insert('Z', position 3)
```

Then transform that result against op2 (which inserted at position 2 after transformation):

```
T(Insert('Z', 3), Insert('X', 2)):
  op_x inserted at pos 2, op_z targets pos 3
  pos 2 < pos 3 → shift op_z right by 1 → Insert('Z', position 4)
```

Apply transformed op3: `"ABXCD"` → `"ABXCZD"`

The final document is `"ABXCZD"`, and every user's intention is honored — B after A, X also near the front (with U1 winning the tie), and Z between C and D. Clean, consistent, no conflicts. 🎯








# 📌 OT vs CRDT — The Big Comparison

```
┌─────────────────┬──────────────────────────┬──────────────────────────┐
│   Property      │          OT              │          CRDT            │
├─────────────────┼──────────────────────────┼──────────────────────────┤
│ Central Server  │ REQUIRED                 │ NOT required (P2P works) │
│ Complexity      │ High (hard to implement) │ Medium (math is hard)    │
│ Memory          │ Low (just ops + doc)     │ Higher (metadata/IDs)    │
│ Network         │ Needs ordering           │ Works with any order     │
│ Offline Support │ Hard                     │ Excellent (designed for) │
│ Used By         │ Google Docs              │ Figma, Notion, CouchDB   │
│ History         │ Need to store all ops    │ Built into structure     │
└─────────────────┴──────────────────────────┴──────────────────────────┘
```

Google Docs historically utilized [Operational Transformation (OT)](https://en.wikipedia.org/wiki/Operational_transformation) rather than [CRDTs (Conflict-free Replicated Data Types)](https://en.wikipedia.org/wiki/Conflict-free_replicated_data_type) primarily because ==OT is optimized for a central-server model, providing faster performance, lower overhead, and more intuitive text merging for collaborative document editing==. While CRDTs are excellent for distributed systems, they require more metadata storage and higher memory usage. 

**Key Reasons for Choosing OT over CRDTs:**

- **Centralized Architecture:** Google Docs relies on a central server to manage document state, user access, and permission control. OT excels here because the server can validate and transform every incoming operation.
- **Performance and Efficiency:** OT processes changes quickly with minimal latency, keeping the document data structure compact because it does not need to store massive amounts of metadata for every character.
- **Intent Preservation:** OT is designed to handle user intent better during complex, simultaneous edits (e.g., one user deletes a paragraph while another edits it).
- **Reduced Metadata Overhead:** CRDTs ensure consistency by storing the entire editing history and unique identifiers for every character, which can lead to high storage overhead, whereas OT keeps the document structure lighter.
- The Trade-offs

Although CRDTs allow better offline functionality, they are not universally superior. 

- **Memory Overhead:** CRDTs require significantly more memory and metadata (sometimes 2-3x the size of the actual data) because every single character needs a unique identifier.
- **Server Burden vs. Client Burden:** OT is heavy on the server; CRDTs are heavy on the client (browser/mobile app).






# CRDTs for Text Documents — Deep Dive with Examples

Let me build this from the ground up, because CRDTs have a fundamentally different _philosophy_ from OT. Once you feel that philosophical difference in your bones, everything else follows naturally.

---

## The Philosophical Shift: Don't Resolve Conflicts, Make Them Impossible

Remember how OT works — two users edit, a conflict happens, and the server _resolves_ it by transforming operations. OT is essentially a conflict _resolution_ strategy.

CRDTs take a completely different stance. They say: **"What if we designed the data structure itself so that merging any two versions always produces the same result, regardless of order?"**

The key mathematical property they rely on is this: if your merge operation is **commutative** (A merge B = B merge A), **associative** ((A merge B) merge C = A merge (B merge C)), and **idempotent** (A merge A = A), then it doesn't matter what order operations arrive in, or even if they arrive twice — the result is always identical.

This is why CRDTs don't need a central server. There's no "authoritative order" to establish. Every replica just merges freely with every other replica and they all converge to the same state. Think of it like addition — it doesn't matter if you add 3+5 or 5+3, you always get 8.

But here's the hard question: **how do you design a text document, where order and position matter deeply, to have this property?**

---

## The Core Problem with Naive Text + CRDTs

A plain array of characters is a terrible CRDT. Consider this:

```
Document: "ACD"
           012

Priya inserts 'B' at index 1  →  "ABCD"
Rahul deletes 'C' at index 1  →  "AD"
```

If Priya's insert arrives first and then Rahul's delete is applied, Rahul accidentally deletes 'B' (which is now at index 1) instead of 'C'. The same problem OT faces — positions are _relative_ and _unstable_. As soon as anyone edits the document, every position after that edit becomes meaningless.

The fundamental insight of text CRDTs is: **stop using positions. Give every character a permanent, unique, immutable identity.**

---

## The Big Idea: Characters as Nodes in a Linked List

Instead of thinking of a document as an array where characters live at index 0, 1, 2... think of it as a **linked list where every character has a unique ID that never changes, no matter what insertions or deletions happen around it.**

```
Instead of this (unstable positions):
  "ACD"
   012

Think of this (stable identities):
  [id:1, val:'A'] → [id:2, val:'C'] → [id:3, val:'D']
```

Now when Priya wants to insert 'B' between 'A' and 'C', she doesn't say "insert at position 1." She says **"insert after the node with id:1."** This reference is stable forever — it doesn't matter how many characters get inserted or deleted around it.

This is the core of every text CRDT. The specific algorithms (RGA, LSEQ, Logoot, YJS/YATA) differ in the details of how they assign IDs and break ties, but they all share this fundamental idea.

Let's go deep on **RGA (Replicated Growable Array)**, which is the most widely used and conceptually clean algorithm.

---

## RGA: Replicated Growable Array

### How RGA Identifies Every Character

In RGA, every character gets an ID that is a pair: `(timestamp, client_id)`. The timestamp here is a **Lamport clock** — a logical counter that increments every time a client performs an operation. This pair is globally unique because even if two clients have the same timestamp, their client IDs will differ.

```
Character node in RGA:
{
  id:        (timestamp, client_id),  // unique identity, never changes
  value:     'A',                     // the actual character
  left_id:   (ts, cid) or NULL,       // id of the character to my left when I was inserted
  tombstone: false                    // true if deleted (we'll get to this)
}
```

The `left_id` is the crucial field. When you insert a character, you record _what was to your left at the moment of insertion_. This is your permanent address in the document — not a number that shifts, but a pointer to a specific other character that will always be identifiable.

### Building a Document from Scratch

Let's say User A (client_id = "A") types "HI" into an empty document. Their Lamport clock starts at 0.

```
Step 1: Type 'H'
  Clock becomes 1
  Node: { id:(1,"A"), value:'H', left_id: NULL }
  Document: [H:1A]

Step 2: Type 'I' after H
  Clock becomes 2
  Node: { id:(2,"A"), value:'I', left_id: (1,"A") }
  Document: [H:1A] → [I:2A]
```

Now User B (client_id = "B") joins and they share the document state. User B's Lamport clock syncs to 2 (the max they've seen). Now both users edit simultaneously.

---

## Scenario 1: Insert at Different Positions (No Conflict)

This is the easy case. Let's watch it play out.

```
Shared document state: [H:1A] → [I:2A]
Visible text: "HI"

User A (clock=2): Types 'X' after 'H'
  Clock becomes 3
  Node: { id:(3,"A"), value:'X', left_id: (1,"A") }
  "I want to go after H"

User B (clock=2): Types 'Y' after 'I'  
  Clock becomes 3
  Node: { id:(3,"B"), value:'Y', left_id: (2,"A") }
  "I want to go after I"
```

Both operations are sent to each other (or to the server, which broadcasts). Now each client applies both operations. Let's see what User A's client does when it receives B's operation, and what User B's client does when it receives A's operation.

**User A receives B's insert ('Y' after node 2A):**

```
Current list: [H:1A] → [X:3A] → [I:2A]
              (A already applied their own op)

B says: insert 'Y' after node (2,"A") which is 'I'
Find node 2A in the list... it's there, it's 'I'
Insert Y after it:

Result: [H:1A] → [X:3A] → [I:2A] → [Y:3B]
Visible: "HXIY"
```

**User B receives A's insert ('X' after node 1A):**

```
Current list: [H:1A] → [I:2A] → [Y:3B]
              (B already applied their own op)

A says: insert 'X' after node (1,"A") which is 'H'
Find node 1A in the list... it's there, it's 'H'
Insert X after it:

Result: [H:1A] → [X:3A] → [I:2A] → [Y:3B]
Visible: "HXIY"
```

Both converge to **"HXIY"** without any transformation logic at all. The operations just _naturally_ don't interfere because they reference different anchor nodes. No server coordination needed.

---

## Scenario 2: Insert at the Same Position (The Interesting Case)

This is where RGA's real elegance shows up. Two users both want to insert after the same character.

```
Shared document: [H:1A] → [I:2A]
Visible text: "HI"

User A (clock=2): Types 'X' after 'H'
  Node: { id:(3,"A"), value:'X', left_id: (1,"A") }

User B (clock=2): Types 'Y' after 'H'  ← same anchor!
  Node: { id:(3,"B"), value:'Y', left_id: (1,"A") }
```

Both claim "I want to go after 'H'". Both have timestamp 3. This is a genuine conflict. How does RGA resolve it without a server?

**The RGA tie-breaking rule:** When two nodes have the same `left_id`, you sort them by their own IDs in _descending_ order. The node with the higher ID goes closer to the left (gets inserted first, appearing before the other).

Why descending? Think of it intuitively: the higher timestamp means "more recent" — the more recent insert pushes itself to the left of the same-position conflict. And if timestamps are equal (as here), we sort by client_id. Let's say "B" > "A" alphabetically.

```
Conflict: both want to go after [H:1A]
  Node (3,"A") has id (3,"A")
  Node (3,"B") has id (3,"B")
  
  Compare: (3,"B") > (3,"A")  because "B" > "A"
  Higher ID goes first (closer to left anchor)
  
  So: [H:1A] → [Y:3B] → [X:3A] → [I:2A]
  Visible: "HYXI"
```

Now here's the beautiful part. **Both clients apply this same rule independently, with no communication.** User A, when inserting their own 'X' and seeing that 'Y' also wants to go after 'H', runs the same comparison. User B does the same. They both arrive at "HYXI" without any server coordination.

Let's verify:

**On User A's machine** (they applied X first, then receive Y):

```
After applying own op:  [H:1A] → [X:3A] → [I:2A]

Receive Y with left_id = (1,"A"):
  Find node (1,"A") = 'H'
  Try to insert Y right after H
  But wait — there's already X at position after H, and X has left_id = (1,"A") too
  
  Check: should Y go before or after X?
  Y's id = (3,"B"), X's id = (3,"A")
  (3,"B") > (3,"A") → Y wins, goes first
  
  Result: [H:1A] → [Y:3B] → [X:3A] → [I:2A]  ✓
```

**On User B's machine** (they applied Y first, then receive X):

```
After applying own op:  [H:1A] → [Y:3B] → [I:2A]

Receive X with left_id = (1,"A"):
  Find node (1,"A") = 'H'
  Try to insert X right after H
  But Y is already there with left_id = (1,"A") too
  
  Check: should X go before or after Y?
  X's id = (3,"A"), Y's id = (3,"B")
  (3,"A") < (3,"B") → X loses, goes after Y
  
  Result: [H:1A] → [Y:3B] → [X:3A] → [I:2A]  ✓
```

Both machines arrive at **"HYXI"** using pure local logic. No server needed. This is the magic of CRDTs.

---

## Scenario 3: Deletion — The Tombstone Problem

Now here's something subtle. What happens when a user deletes a character?

Remember that in RGA, characters are _anchor points_ that other characters use to identify their position. If User A inserts 'X' saying "I go after node (2,'A')", and then someone _deletes_ node (2,'A') from the list entirely, User A's reference becomes a dangling pointer — you can't find the anchor anymore.

This is why CRDTs use **tombstones** instead of actual deletion. When a character is deleted, it is _marked as deleted_ but kept in the list as a ghost node. It becomes invisible to the user but remains available as an anchor.

```
Document: [H:1A] → [I:2A] → [!:3A]
Visible:   "HI!"

User A deletes '!'
  Don't remove node (3,"A") from the list
  Just set its tombstone flag: { id:(3,"A"), value:'!', tombstone: TRUE }

List:  [H:1A] → [I:2A] → [!:3A✝]
Visible: "HI"   (tombstoned nodes are invisible)
```

Now watch why this matters:

```
Concurrent with A's deletion, User B inserts '?' after '!'
  Node: { id:(4,"B"), value:'?', left_id: (3,"A") }

User B hasn't received A's deletion yet.
Their document still shows "HI!" and they type after it.
```

When B's insert arrives at A (who has already deleted '!'):

```
A's list: [H:1A] → [I:2A] → [!:3A✝]

Receive B's insert: "insert '?' after node (3,'A')"
Find node (3,"A")... it's there! (tombstoned but still in the list)
Insert '?' after it:

A's list: [H:1A] → [I:2A] → [!:3A✝] → [?:4B]
Visible:  "HI?"   (tombstoned '!' is invisible, but '?' anchored to it is visible)
```

And when A's delete arrives at B:

```
B's list: [H:1A] → [I:2A] → [!:3A] → [?:4B]

Receive A's delete: "tombstone node (3,'A')"
Find it, mark tombstone = true:

B's list: [H:1A] → [I:2A] → [!:3A✝] → [?:4B]
Visible:  "HI?"   ✓
```

Both converge to **"HI?"**. The tombstone saved the reference. Without it, B's insert would have been orphaned and you wouldn't know where to place '?' in the document.

This is an important interview point: **tombstones are why CRDTs have higher memory usage than OT.** In a long-lived document with lots of edits, you accumulate a graveyard of tombstoned characters that can never be fully purged (unless you run a separate garbage collection protocol with coordination — which is complex).

---

## Putting It All Together: The Full RGA Node and Merge Logic

Now that you've seen all the cases, here's the complete picture of how RGA works as a system:

```
Each character node stores:
{
  id:        (lamport_clock, client_id),  // permanent unique identity
  value:     char,                        // the character itself
  left_id:   id or NULL,                  // anchor: what was to my left at insertion time
  tombstone: boolean                      // true = deleted but preserved as anchor
}

Insert algorithm:
  1. Create new node with current (clock, client_id) as ID
  2. Set left_id to the ID of the character currently to your left
  3. Broadcast this node to all replicas
  4. On receive: find the left_id anchor, scan right past any nodes
     with higher IDs (tie-breaking), insert there

Delete algorithm:
  1. Find the node by its ID
  2. Set tombstone = true
  3. Broadcast the tombstone operation
  4. On receive: find the node by ID, set tombstone = true

Merge (the CRDT guarantee):
  Because every node has a permanent ID and a stable left_id anchor,
  applying operations in ANY order produces the same linked list.
  This is the convergence guarantee — no coordination needed.
```

---

## OT vs CRDT: The Deep Intuitive Difference

Now that you understand both, here's how to articulate the difference in an interview in a way that shows real understanding.

OT treats a document as a **mutable array with shifting positions**, and resolves conflicts by transforming the position numbers when they clash. It needs a server to establish a canonical operation order, because the transformation math depends on knowing the exact sequence of past operations.

CRDT treats a document as an **immutable set of uniquely-identified nodes** that grow over time (deletions just add tombstones). There are no position numbers to transform. Every operation says "relative to this specific node (by ID), do this." Since IDs are permanent, operations are _self-describing_ — they carry enough context to be applied in any order.

The tradeoff is: OT is leaner in memory (no IDs, no tombstones) but requires central coordination. CRDT is heavier in memory but works in fully peer-to-peer, offline-first, eventually-consistent environments. That's exactly why Figma and Notion chose CRDTs — they need offline support and real-time sync across distributed clients without strict server dependency.