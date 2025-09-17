
# 📄 Sample Design
---

## 1. Problem Statement & Goals
- **Brief:**  
  _One or two sentences describing what you need to build._
- **Operations:**  
  - `operation1(args): returnType`  
  - `operation2(args): returnType`
- **SLAs / Scale:**  
  _e.g. 1k QPS, latency < 10 ms, capacity = n items._

---

## 2. Assumptions & Scope
- _Clarification 1 (e.g. single-node vs. distributed)_  
- _Clarification 2 (e.g. in-memory vs. persistent storage)_  
- _Clarification 3 (e.g. nulls allowed? max key size?)_

---

## 3. Data Model & APIs

| Entity / Class   | Fields / Methods                     | Responsibility                  |
|------------------|--------------------------------------|---------------------------------|
| `ClassA`         | `field1`, `method1(param):Return`    | Public interface / operations   |
| `ClassB`         | `fieldX`, `methodY(params)`          | Core data structure / helper    |
| `map: K → V`     | —                                    | O(1) lookup                     |

---

## 4. Core Algorithm / Pseudocode

    operation1(args):
      if <condition>:
        handle edge case
      else:
        perform main steps
      return result

    // Helper routines should each be O(1) or O(log n), as needed.

---

## 5. Patterns & Trade-Offs
- **Pattern:** _Name of design pattern used_  
- **Trade-Off:** _Describe any trade-offs (e.g. time vs. space)_  
- **Alternative:** _Possible alternative approach & its trade-offs_

---

## 6. Concurrency & Edge Cases
- **Thread Safety:**  
  _Locking strategy, concurrency primitives, immutability_
- **Edge Cases:**  
  - Case 1 description  
  - Case 2 description  
  - Case 3 description

---

## 7. Extension Points
- **Feature 1:** _How to plug in TTL eviction, plugins, etc._  
- **Feature 2:** _Distributed version, sharding, scaling out_

---

## 8. Complexity & Review Notes
- **Time Complexity:** _e.g. O(1) / O(log n)_  
- **Space Complexity:** _e.g. O(n)_  
- **Review Focus:**  
  - _What to double-check before an interview (e.g. pointer logic, lock granularity)_
