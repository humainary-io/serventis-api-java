# Serventis Sequencers — Design & Usage Guide

A **Sequencer** is a finite-state recognizer over a stream of domain **signs** that emits
[`Statuses.Sign`] readings from *structural position* — from the order in which signs arrive —
rather than from translating any single sign. Where a **Scorecard** answers *"what does this sign
mean on its own?"*, a Sequencer answers *"what does the **shape** of this sequence prove?"*

This guide is the companion to `SIGNS.md` (sign design) and `SERVENTIS.md` (the semiotic ascent). It
covers when to reach for a Sequencer, its exact semantics, what it deliberately cannot do, and how
to build one for a domain.

> Operator: `io.humainary.serventis.sdk.meta.Sequencers` · since 3.0

---

## 1. Where it sits in the ascent

```text
domain signs ──► STATUS (universal) ──► SITUATION (actionable) ──► actions
```

Three different operators lift signs to status; they are complementary, not competing:

| Operator                     | Question                              | Driven by                                |
|------------------------------|---------------------------------------|------------------------------------------|
| **Scorecard**                | what does each sign say about health? | per-sign translation (decayed plurality) |
| **Sequencer**                | what does the *order* of signs prove? | **structure**                            |
| **(time-windowed assessor)** | how serious is it, over time?         | **dwell + variability** → *Situation*    |

A Sequencer owns the status trajectory that **only ordered structure can prove** — convergence,
divergence, completion, an orphaned closer. It is time-free by design (see §6).

---

## 2. When to use one

Use a Sequencer when a domain has an **ordered protocol** and the meaning lives in the order:
`ACQUIRE` then `GRANT` then `RELEASE` is healthy; a `RELEASE` with no `ACQUIRE` is a defect; an
`ACQUIRE` that never reaches `GRANT` is degradation. None of that is visible one sign at a time.

| ships `STATUS`? | ordered protocol?               | Sequencer role                                                                      |
|-----------------|---------------------------------|-------------------------------------------------------------------------------------|
| yes             | yes                             | **additive** — proves what per-sign translation can't                               |
| no              | yes (single-episode, sign-only) | the **natural** status path — structure is the only source                          |
| no              | no                              | not a Sequencer — use thresholds/windows, or the sign already reports the condition |

A domain **without** a `STATUS` map is often one where single-sign→status was judged meaningless, so
structure is the only way to a status. Those are the strongest candidates.

---

## 3. The operator

A machine is a tree of **states**. Each state is a `SignMap` produced from the domain's published
[`SignSet`] (`SIGNS`) by switching over the admitted sign:

```java
import static io.humainary.serventis.sdk.meta.Sequencers.emit;
import static io.humainary.serventis.sdk.Statuses.Sign.*;

var s = Locks.SIGNS;                       // the published sign set

Flow< Locks.Sign, Statuses.Sign > recognizer =
        Sequencers.flow(
                s.map(sign -> switch (sign) {       // the ROOT (idle) state
                    case ACQUIRE -> emit(DIVERGING, /* ...next state... */ null);
                    default -> emit(STABLE);      // idle baseline
                })
        );
```

- `SIGNS.map(fn)` runs `fn` **once per sign** and stores the result in an ordinal-indexed array; at
  runtime a lookup is a single array load.
- `Sequencers.flow(root)` returns a Substrates `Flow< Sign, Statuses.Sign >`; materialize it against
  a status pipe like any flow: `Pipe<Sign> in = recognizer.pipe( statusSink );`.
- Each flow attachment owns one **`Walk`** with a single current state, touched only on the circuit
  thread.

### Trees, not cycles

States form a **tree rooted at the idle state**. The only back-edge is **reset-to-root**. You cannot
point a state at a sibling or an ancestor — nested `SignMap`s are immutable values built
inner-first, so a true cycle is *not constructible* (this is deliberate). Linear and branching
protocols that run and reset fit naturally; a genuinely **cyclic** state machine — a circuit breaker
holding a persistent CLOSED↔OPEN↔HALF_OPEN state — cannot be modeled faithfully.

You can still recognize the *transitions* of a cyclic protocol by treating each interesting
transition as a short **open→close episode that resets**. A breaker's flap, for instance, reads
`HALF_OPEN` as an opener with two closers (`OPEN` = re-trip, `CLOSE` = recovered), emitting and
resetting on each — what is lost is the persistent across-cycle state, not the ability to score the
probes.

---

## 4. Transition semantics (the exact rules)

Everything the machine does on an admitted sign reduces to four cases. **A `null` map result does
not reset and does not emit — it stays.**

| the state map returns…         | factory              | next state                     | emits                 |
|--------------------------------|----------------------|--------------------------------|-----------------------|
| `null`                         | —                    | **stays** in the current state | nothing               |
| a leaf transition              | `emit(status)`       | **resets to root**             | `status`              |
| a transition with a next state | `emit(status, next)` | moves to `next`                | `status`              |
| …with a null status            | `emit(null, next)`   | moves to `next`                | nothing (silent move) |

Reset-to-root happens **only** through a leaf (`emit(status)`) or a transition whose next state is
null — never from a `null` map result. This "null = stay" rule is what makes idle suppression, the
`activity*` swallow, and wildcards work.

**There is no "emit and stay".** Every emission either resets (leaf) or advances (next state); the
only way to remain in the current state is `null`, which is silent. A state cannot reference
*itself* as the next state — it is the value being constructed. So a reading that should worsen over
several signs must **advance through distinct states**, each emitting its own value (see §8.4).

---

## 5. Wildcards

There is no wildcard *token*. Each state is a **total function** over the sign set, so the wildcard
is simply the `default` arm of the switch — and because these are exhaustive `switch` *expressions*,
the compiler **forces** you to define behavior for every sign. A wildcard can produce any of the
three outcomes:

```java
s.map ( sign -> switch ( sign ) {
  case GRANT -> emit ( CONVERGING, granted ); // explicit step
  case TIMEOUT -> emit ( DEGRADED );          // explicit closer + reset
  case RETRY, REDIRECT -> null;               // grouped: ignore, stay
  default -> null;                            // wildcard: activity* swallow
} )
```

- `default -> null` — match any, **stay, silent** (consume in-between activity).
- `default -> emit(x)` — match any, **emit + reset** ("anything unexpected here ends the episode").
- `default -> emit(x, next)` — match any, **advance**.

Explicit `case`s take precedence over `default`, which is the precedence you want.

> **Sharp edge:** a sign that should behave the same in *every* state (e.g. a global `ABANDON`) must
> be
> repeated in each state's map today — there is no cross-state/global wildcard layer. If this
> becomes
> common, a fallback map is a candidate enhancement; it is not in the current API.

---

## 6. What a Sequencer cannot do

A Sequencer recognizes a **sequence of admitted signs**. Four things sit outside it; design around
them or push them upstream.

1. **Absence is unobservable.** The machine fires only when a sign arrives, never on the
   *non-arrival*
   of one. "Missing closer", "leak", "stalled", "timed out" cannot be detected directly. Express
   them as **explicit signs**: reify a timeout upstream as `TIMEOUT`/`EXPIRE`/`ABANDON` and read it
   like any input; or treat a **fresh opener while still open** as evidence the prior episode ended
   badly (`emit(<reading>, openerState)` to speak and re-anchor); or let an upstream windowed
   assessor handle dwell.

2. **Concurrency / nesting is not tracked.** One `Walk` holds one current state — no stack, counter,
   or correlation table. A subject with concurrent or nested episodes (multiple in-flight
   operations, reentrant locks, N outstanding borrows) cannot be followed by one walk. Use a
   Sequencer only when the subject is already a single episode/resource/conversation, or after
   upstream correlation splits the stream. **Counting** protocols (pool utilization, a latch
   countdown) need a counter, not an FSM.

3. **Role / dimension is erased.** `Sequencers.flow(...)` reads a sign enum, not a `Signal`. For a
   dimensional domain a `Signal::sign` projection drops the role (CALLER/CALLEE, PROVIDER/RECEIVER,
   COORDINATOR/PARTICIPANT), so one walk interleaves both sides of the protocol. Scope the machine
   to **sign-only** semantics, or split by role upstream first.

4. **Cycles are not expressible** (see §3) — the model is trees that reset to root.

> The discipline in one line: keep *sequence of observed signs* (what a Sequencer does) separate
> from
> *absence, concurrency, role, and cycles* (which need explicit signs, partitioning, or a different
> operator).

---

## 7. Designing a Sequencer for a domain

1. **Pick the idle root.** What is the baseline when nothing is in flight? Decide whether idle emits
   `STABLE` (a positive baseline) or `null` (silence). Both are valid; silence is quieter.
2. **Walk the happy path** as a chain of `emit(status, next)` steps, ending in a leaf `emit(status)`
   that resets. Choose the status at each step to describe the *trajectory*: `DIVERGING` while
   opening,
   `CONVERGING` once the outcome is assured, `STABLE` on clean completion.
3. **Name the failure closers** explicitly (`emit(DEGRADED)`, `emit(DEFECTIVE)`), each resetting.
4. **Swallow the noise** with `default -> null` inside in-flight states so unrelated signs keep the
   walk alive without speaking.
5. **Catch the orphans** — a closer seen at idle (a `RELEASE` with no `ACQUIRE`) is a structural
   defect:
   `case RELEASE -> emit(DEFECTIVE)` at the root.
6. **Reify time and absence as signs** — never rely on detecting a missing sign.
7. **Keep it single-episode and sign-only** — if the domain is concurrent, counting, cyclic, or
   dimensional, resolve that upstream first (§6).

---

## 8. Worked examples

### 8.1 Locks — a lifecycle (has `STATUS`; Sequencer is *additive*)

The canonical shape: open → outcome → close, with an idle baseline and an orphan-closer defect.

```java
var s = Locks.SIGNS;

Sequencers.flow (
  s.map ( sign -> switch ( sign ) {             // idle
    case ACQUIRE -> emit ( DIVERGING,           // contending until granted
      s.map ( held -> switch ( held ) {         // acquiring
        case GRANT -> emit ( CONVERGING,        // granted, outcome assured
          s.map ( end -> switch ( end ) {       // held
            case RELEASE -> emit ( STABLE );    // clean completion
            default -> null;                    // stay held, silent
          } )
        );
        case TIMEOUT -> emit ( DEGRADED );      // failed to acquire
        case RELEASE -> emit ( DEFECTIVE );     // orphan closer
        default -> null;                        // stay acquiring, silent
      } )
    );
    case RELEASE -> emit ( DEFECTIVE );         // orphan closer at idle
    default -> emit ( STABLE );                 // idle baseline
  } )
);
```

### 8.2 Operations — the minimal open/close bracket (no `STATUS`; Sequencer is *primary*)

`Operations` is the abstract bracket vocabulary — `BEGIN`, `ADVANCE`, and `END` — standing in for
every domain's start verbs (START/CALL/SUBMIT/ACQUIRE/SPAWN…), mid-episode steps, and end verbs
(STOP/RETURN/COMPLETE/RELEASE…). A single sign has no health; only the **pairing** does. This
example sequences the `BEGIN`/`END` bracket and lets `ADVANCE` stay silent. (It is also the target
vocabulary of the per-domain `OPERATION` map in §9 — so most of the time you do not hand-write this
at all.)

```java
var o = Operations.SIGNS;

Sequencers.flow (
  o.map ( sign -> switch ( sign ) {         // idle
    case BEGIN -> emit ( DIVERGING,         // opened
      o.map ( inner -> switch ( inner ) {   // in flight
        case END     -> emit ( STABLE );    // closed cleanly → reset
        case BEGIN   -> emit ( DEFECTIVE ); // overlapping open: prior bracket broke → reset
        case ADVANCE -> null;               // mid-span step → stay, silent
      } ) );
    case END     -> emit ( DEFECTIVE );     // close with nothing open (orphan)
    case ADVANCE -> null;                   // idle, silent
  } )
);
```

An overlapping `BEGIN` emits `DEFECTIVE` and **resets to idle** — it cannot re-anchor straight into
a fresh in-flight bracket, because that would mean *advancing to a copy of the state being built* (a
self-reference, §3). A genuinely new bracket opens on the next `BEGIN`.

### 8.3 Actors — a conversation (no `STATUS`; structure is the *only* status)

A speech act has no health on its own; only the completed exchange does. Note the **absence**
handling:
a broken promise is surfaced by an explicit re-request that resets the walk — never by detecting a
missing `DELIVER`.

```java
var a = Actors.SIGNS;

Sequencers.flow (
  a.map ( sign -> switch ( sign ) {             // idle
    case REQUEST -> emit ( DIVERGING,           // awaiting commitment
      a.map ( msg -> switch ( msg ) {           // awaiting
        case PROMISE -> emit ( CONVERGING,      // committed
          a.map ( done -> switch ( done ) {     // promised
            case DELIVER -> emit ( STABLE );    // fulfilled
            case REQUEST -> emit ( DEFECTIVE ); // new ask before delivery
            default -> null;                    // stay promised
          } )
        );
        case DENY -> emit ( DEGRADED );         // refused
        default -> null;                        // stay awaiting
      } )
    );
    default -> null;                            // idle, silent
  } )
);
```

A `REQUEST` arriving mid-promise emits `DEFECTIVE` and **resets to idle** — it flags the broken
prior promise but is itself consumed; the genuinely-new exchange opens on the next `REQUEST`. You
cannot
"re-anchor" straight into the awaiting state, because that is a back-edge to a sibling state — a
cycle, which the model does not express (§3). Resetting to root is the only backward move available.

### 8.4 Atomics — a deepening contention trajectory (has `STATUS`; *additive*)

There is **no "emit and stay"** (§4): a step either emits-and-resets or emits-and-advances. So a
trajectory that worsens over several signs must *advance through distinct states*, each emitting a
worse reading. Here contention deepens `CONVERGING → DIVERGING → DEGRADED`/`DEFECTIVE`.

```java
var a = Atomics.SIGNS;

Sequencers.flow (
  a.map ( sign -> switch ( sign ) {             // idle
    case ATTEMPT -> emit ( CONVERGING,          // attempting
      a.map ( light -> switch ( light ) {       // contended, light
        case SUCCESS -> emit ( STABLE );        // won
        case EXHAUST -> emit ( DEFECTIVE );     // gave up
        case FAIL -> emit ( DIVERGING,          // first failure deepens
          a.map ( heavy -> switch ( heavy ) {   // contended, heavy
            case SUCCESS -> emit ( CONVERGING );      // recovered
            case EXHAUST -> emit ( DEFECTIVE );       // gave up
            case BACKOFF, PARK -> emit ( DEGRADED );  // deep wait
            default -> null;                          // stay heavy
          } )
        );
        default -> null;                        // stay light
      } )
    );
    default -> null;                            // idle, silent
  } )
);
```

`ATTEMPT, FAIL, BACKOFF` reads `CONVERGING, DIVERGING, DEGRADED`; `ATTEMPT, FAIL, SUCCESS` recovers
as
`CONVERGING, DIVERGING, CONVERGING`.

> All four machines above are exercised in `SequencerExamplesTest` (Operations/Actors/Atomics) and
> `SequencersTest` (Locks) in the serventis TCK — the guide stays honest because the examples must
> compile against the real sign sets and produce the documented trajectories.

### Good fits / poor fits

| Fits (sign-only, single-episode, resets)                                   | Poor fits — and why                                                                                                         |
|----------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------|
| Locks, Resources, Leases, Tasks, Processes, Probes, Agents                 | **Breakers** — persistent cycle (recognize its *flap* as resetting pairs, §3)                                               |
| Operations (generic open/close), Actors, Pipelines (escalation trajectory) | **Services, Exchanges** — dimensional, opener varies by role (§6.3); Services is **excluded** from the generic bracket (§9) |
| Atomics (`SPIN→YIELD→BACKOFF→PARK` deepening trajectory)                   | **Pools** — counting, not pairing (§6.2)                                                                                    |

> This table judges hand-writing a **bespoke** machine (§8). Several domains awkward to hand-write
> still
> work with the generic `OPERATION`/`STATUS` bracket (§9): **Transactions** (its `START → COMMIT`
> span is
> dimension- *invariant* — the COORDINATOR/PARTICIPANT role does not move the bracket) and
> **Latches** (its
> `AWAIT → RELEASE` is a clean pair; only the *arrival counting* is not a bracket). The genuinely
> unbracketable cases are **Breakers** (cyclic), **Pools** (pure counting), and **Services**
> (dimension- *variant* opener — excluded outright, §9).

---

## 9. Deriving a sequencer from `OPERATION` + `STATUS`

For the common *open → progress → close* shape you do not hand-write a machine. The sequencer reads
two classifications each bracketable protocol domain publishes alongside `SIGNS`/`KIND` (see
`SIGNS.md`). (`Services` is **excluded**: it is dimension-variant — the opener flips by `CALLER`/
`CALLEE` and outbound calls nest within work — so a single-span bracket cannot model it; split by
dimension first, §6.3, then hand-write a per-perspective machine.)

- **`OPERATION`** (sign → [`Operations.Sign`] `BEGIN`/`ADVANCE`/`END`) — the **episode structure**
  of each sign: which signs open, advance, and close a span. *Total* and *cross-kind*: a terminal
  outcome is `END`, a mid-span outcome is `ADVANCE`. This is the span-terminality axis a sequencer
  needs — the one `KIND` (act/result) plus verdict polarity cannot supply, because `Tasks.COMPLETE`
  is a terminal success while `Locks.GRANT` is a mid-span one. (So `OPERATION` maps a *result* like
  `DENY` to `END`:
  it closes the episode even though its `KIND` is `OUTCOME`.)
- **`STATUS`** (sign → [`Statuses.Sign`] `STABLE`/`DEGRADED`/`DEFECTIVE`, or `null`) — the canonical
  per-sign **quality**, the same map the Scorecard reads. It supplies the close's severity directly:
  `STABLE` (or abstaining) is a healthy close, `DEGRADED` an unhealthy one, `DEFECTIVE` a violation.

> **Why `STATUS`, not `OUTCOME`.** `OUTCOME` (`SUCCESS`/`FAIL`/`UNKNOWN`) is the grammatical
> *verdict*
> axis — it drives success/fail tallies in a Scorecard, and it is `KIND`-restricted, so a forced
> operation-only close like `Leases.REVOKE` or `Processes.KILL` carries *no* verdict (`null`).
> Feeding
> that to a bracket would read a forced revocation as a clean `STABLE`. `STATUS` has no such gap: it
> is
> total over closers and already distinguishes `DEGRADED` from `DEFECTIVE`, which `OUTCOME` cannot.
> `OUTCOME` remains the right input for Scorecards (ratios); `STATUS` is the right input for the
> trajectory.

One operator turns them into a sequencer for *any* domain:

```java
Flow< Locks.Sign, Statuses.Sign > recognizer =
        Sequencers.flow(Locks.OPERATION, Locks.STATUS);
```

The same call works everywhere — `Sequencers.flow ( Transactions.OPERATION, Transactions.STATUS )`,
`Sequencers.flow ( Probes.OPERATION, Probes.STATUS )` — reading only the universal bracket +
canonical status, never a domain's native signs.

The trajectory:

| admitted sign                                  | reading                                                | span      |
|------------------------------------------------|--------------------------------------------------------|-----------|
| `OPERATION` = `BEGIN`, nothing open            | `DIVERGING`                                            | opens     |
| `OPERATION` = `BEGIN`, a span is open          | `DEFECTIVE` (overlap)                                  | re-opens  |
| `OPERATION` = `ADVANCE`, `STATUS` `STABLE`     | `CONVERGING`                                           | unchanged |
| `OPERATION` = `ADVANCE`, `STATUS` degraded/def | `DEGRADED` / `DEFECTIVE`                               | unchanged |
| `OPERATION` = `ADVANCE`, abstains, or no span  | *(silent)*                                             | unchanged |
| `OPERATION` = `END`, a span is open            | the close's `STATUS` (`STABLE`/`DEGRADED`/`DEFECTIVE`) | closes    |
| `OPERATION` = `END`, nothing open              | `DEFECTIVE` (orphan)                                   | —         |

**`END` — not polarity — closes the span**, which is exactly what the earlier two-value (`BEGIN`/
`END` only) model got wrong. `ATTEMPT, DENY, RELEASE` reads `DIVERGING, DEGRADED, DEFECTIVE`
(the denial closes; the release is an orphan), and a completed task followed by a cancel reads
`DIVERGING, STABLE, DEFECTIVE`.

**The close carries its own severity.** Because the bracket reads the closing sign's canonical
`STATUS`, a forced or indeterminate close surfaces honestly: `Leases.REVOKE` closes `DEFECTIVE`,
`Processes.KILL` and `Transactions.CONFLICT` close `DEGRADED`, a clean `Leases.RELEASE` closes
`STABLE`. What stays out of the bracket is *pattern* severity — whether a *run* of degraded closes
is actually defective is the aggregation layer's call (a Scorecard), not a single-episode structural
fact.

**When to still hand-write a machine (§3):** the `OPERATION`/`STATUS` sequencer gives the *default*
open → progress → close trajectory. Reach for `flow(SignMap)` when you need a richer shape — a
deepening ladder (§8.4), a two-phase gate, distinct readings per intermediate state, or anything
beyond a single span. Both factories return the same `Flow< S, Statuses.Sign >`.

---

## 10. From status to situation — keep time out of the Sequencer

A Sequencer emits **status**, not a *Situation*. Elevating status → situation is **time-based**: a
`Situation`'s dimension is *variability* (e.g. `CONSTANT`), and "elevated errors for three hours,
unchanging → CRITICAL/CONSTANT" is definitionally about duration. A time-free recognizer cannot
produce it.

```text
domain signs ──Sequencer (structure)──► Statuses.Sign ──time-windowed assessor──► Situations.Sign
```

The Sequencer's contribution to situation is **indirect**: clean, structurally-grounded status in
makes the downstream time-based elevation trustworthy. Reach for a windowed assessor — not the
Sequencer — for dwell, persistence, and SLO windows.

---

## 11. Design checklist

- [ ] Does the domain have an **ordered protocol** (not just independent levels/outcomes)?
- [ ] Is it **single-episode** per subject (else correlate upstream)?
- [ ] Is it **sign-only** (else split by dimension upstream)?
- [ ] Is it **acyclic** — does each run reach a natural reset (else it is not a Sequencer)?
- [ ] Are timeouts/absence reified as **explicit signs**?
- [ ] Does idle emit a deliberate baseline (`STABLE` or silent `null`)?
- [ ] Do in-flight states **swallow noise** with `default -> null`?
- [ ] Are **orphan closers** caught as defects?
- [ ] Is the status at each step chosen to describe the **trajectory** (`DIVERGING`/`CONVERGING`/…)?

---

*See also: `SIGNS.md` (sign design and the `KIND`/`STATUS` maps), `SERVENTIS.md` (the semiotic
ascent and the observation model), and the [`Statuses`] / [`Scorecards`] operators.*
