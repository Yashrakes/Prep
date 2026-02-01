
## 0️⃣ What a Vector Clock Really Is (mental model)

Think of a **vector clock** as:

> “A history log that says **which servers have modified this data, and how many times each one has done so**.”

Formally:

`D([S1, v1], [S2, v2], …)`

- `Si` → server ID
    
- `vi` → how many times that server has written this data
    
- Each server **only increments its own counter**
    

---

## 1️⃣ First write: D1 on server Sx

**What happens**

- Data item `D1` is written for the very first time.
    
- Server handling the write = `Sx`.
    

**Vector clock rule applied**

- `[Sx, v]` does not exist → create `[Sx, 1]`
    

**Result**

`D1([Sx, 1])`

**Meaning**

> “This data was written once, by server Sx.”

No ambiguity. No conflict.

---

## 2️⃣ Second write: D2 overwrites D1 (same server)

**What happens**

- Client reads `D1`
    
- Modifies it → `D2`
    
- Writes back to **the same server Sx**
    

**Vector clock rule**

- `[Sx, v]` exists → increment it
    

**Result**

`D2([Sx, 2])`

**Meaning**

> “Same data lineage, same server, second update.”

Since `D2` **descends from** `D1`, this is a clean overwrite.

✅ No conflict  
✅ Linear history so far

---

## 3️⃣ Third write: D3 written on server Sy

**What happens**

- Client reads **latest D2**
    
- Modifies it → `D3`
    
- Write handled by **different server Sy**
    

**Vector clock rule**

- `[Sy, v]` does not exist → create `[Sy, 1]`
    
- Keep existing entries
    

**Result**

`D3([Sx, 2], [Sy, 1])`

**Meaning**

> “This version knows about:
> 
> - 2 writes by Sx
>     
> - 1 write by Sy”
>     

Still no conflict, because:

- D3 clearly includes _all history_ of D2
    

---

## 4️⃣ Parallel write: D4 written on server Sz (conflict created)

**What happens**

- Another client **also reads D2** (important!)
    
- Modifies it → `D4`
    
- Write handled by **server Sz**
    

⚠️ Key point:  
D3 and D4 are both based on **D2**, but neither knows about the other.

**Vector clock**

`D4([Sx, 2], [Sz, 1])`

---

## 5️⃣ Why D3 and D4 are in conflict

Let’s compare the clocks:

### D3

`[Sx, 2], [Sy, 1]`

### D4

`[Sx, 2], [Sz, 1]`

### Comparison logic

To say **X is ancestor of Y**:

- For **every server**, `counter(Y) ≥ counter(X)`
    

Check D3 vs D4:

- Sy: D3 has `1`, D4 has `0` ❌
    
- Sz: D4 has `1`, D3 has `0` ❌
    

👉 Neither dominates the other  
👉 They are **siblings**  
👉 **Conflict detected**

**Root cause**

> Same base version (D2) modified independently on different servers.

---

## 6️⃣ Conflict resolution → D5

**What happens**

- Client reads **both D3 and D4**
    
- Detects conflict using vector clocks
    
- Resolves it (merge, pick one, custom logic)
    
- Writes resolved version `D5`
    
- Write handled by server `Sx`
    

**Vector clock rule**

- Increment Sx
    
- Preserve history from both branches
    

**Result**

`D5([Sx, 3], [Sy, 1], [Sz, 1])`

**Meaning**

> “This version incorporates:
> 
> - all Sx updates
>     
> - the update from Sy
>     
> - the update from Sz”
>     

Now **D5 dominates both D3 and D4**, so conflict is resolved.

---

## 7️⃣ How conflict detection works (core rule)

### ✅ No conflict (ancestor relationship)

Version X is ancestor of Y if:

`∀ server i : Yi ≥ Xi`

Example:

`X: [s0,1], [s1,1] Y: [s0,1], [s1,2]`

Y includes all of X → no conflict

---

### ❌ Conflict (siblings)

Conflict exists if:

`∃ server i where Yi < Xi AND ∃ server j where Xj < Yj`

Example:

`X: [s0,1], [s1,2] Y: [s0,2], [s1,1]`

Each is missing something from the other → conflict

---

## 8️⃣ Big picture summary (one-liner per step)

1. **D1** – first write → `[Sx,1]`
    
2. **D2** – same server → `[Sx,2]`
    
3. **D3** – new server, same lineage → `[Sx,2],[Sy,1]`
    
4. **D4** – parallel update → `[Sx,2],[Sz,1]`
    
5. **Conflict** – neither dominates
    
6. **D5** – merged version → `[Sx,3],[Sy,1],[Sz,1]`
    

---

## 9️⃣ Why systems like Dynamo use vector clocks

✔ Detect concurrent updates  
✔ Preserve causality  
✔ Allow eventual consistency  
❌ Clocks grow large with many servers  
❌ Clients may need to resolve conflicts