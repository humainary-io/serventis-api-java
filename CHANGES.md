# Changelog — Humainary Serventis (Public API)

All notable changes to the **Serventis** public API (`io.humainary.serventis`) are recorded here.
The format follows [Keep a Changelog](https://keepachangelog.com/), and the project adheres to
[Semantic Versioning](https://semver.org/).

Tracking begins at `3.0.0-SNAPSHOT-1`. Version `2.10.0` is the baseline; its history predates this
file.

## 3.0.0-SNAPSHOT-5 — 2026-06-16

The Operation/Outcome interlingua and the universal **bracket sequencer**. Each protocol domain now
projects its signs onto two universal SDK vocabularies — `Operations` (episode structure) and
`Outcomes` (verdict) — and a second `Sequencers.flow` overload derives a status trajectory for *any*
domain straight from its `OPERATION` bracket classification and canonical `STATUS` map, with no
hand-written machine. Grows the `Operations.Sign` and `Outcomes.Sign` enums (see *Changed*).

### Added

- **`Operations.Sign.ADVANCE`** (`sdk`) — a third episode position between `BEGIN` and `END`, with a
  matching `Operation.advance()` instrument method. `Operations.Sign` is now the three-phase
  `BEGIN`/`ADVANCE`/`END` *terminality* axis: a sign opens a span, advances it mid-flight, or closes
  it. This is the axis the bracket sequencer walks — the distinction `KIND` (act/result) cannot
  supply, since `Tasks.COMPLETE` is a terminal success while `Locks.GRANT` is a mid-span one.
- **`Outcomes.Sign.UNKNOWN`** (`sdk`) — a third verdict alongside `SUCCESS`/`FAIL`, with a matching
  `Outcome.unknown()` instrument method. It abstains from success/failure tallies for genuinely
  indeterminate results (a write conflict that may retry or abort, an ambiguous disconnect) rather
  than forcing a polarity.
- **Per-domain `OPERATION` and `OUTCOME` maps** — the 10 protocol domains (`Locks`, `Resources`,
  `Leases`, `Latches`, `Atomics`, `Tasks`, `Transactions`, `Processes`, `Probes`, `Agents`) each
  publish a `public static final SignMap<Sign, Operations.Sign> OPERATION` (the episode structure of
  every sign — total, derived from `SIGNS`) and a `SignMap<Sign, Outcomes.Sign> OUTCOME` (the
  success/fail/unknown verdict — partial). Together they are the grammatical interlingua: a domain's
  native signs read in the universal `BEGIN`/`ADVANCE`/`END` × `SUCCESS`/`FAIL`/`UNKNOWN` vocabulary
  without a consumer knowing the source vocabulary. `Services` is **excluded** — it is dimensional
  in a way the single-span bracket cannot model (the opener flips by `CALLER`/`CALLEE`, and outbound
  calls nest within work), so its signs ascend via dimension-aware handling (split, then
  per-perspective machine), not the interlingua. It keeps `STATUS`/`KIND` like every domain.
- **`Sequencers.flow(operation, status)`** (`sdk/meta`) — a second `flow` overload: the universal
  bracket sequencer. It derives the *open → advance → close* status trajectory for any domain
  straight from its `OPERATION` bracket classification and its canonical `STATUS` map — a
  status-derived trajectory reading: `DIVERGING` on open, `CONVERGING`/`DEGRADED`/`DEFECTIVE`
  derived from the step's `STATUS` while advancing, the close's `STATUS` on `END`, and an orphan
  `DEFECTIVE` for a close (or an overlapping open) with no span. One operator, every domain, no
  hand-written machine. See `SEQUENCERS.md` §9.
- **`SEQUENCERS.md`** — new §9 "Deriving a sequencer from `OPERATION` + `STATUS`", plus the
  three-phase `Operations` worked example (§8.2).
- **`SignalMap`** (`sdk`) — the signal-level projection cache: `SignalSet#map(fn)` applies a
  whole-signal projection once per `Sign × Dimension` pair and returns an ordinal-indexed map with
  both `apply(signal)` and direct `get(sign, dimension)` lookup. An efficient `signal → value`
  table — the signal-level sibling of `SignMap` — for any value keyed by the full signal, with no
  hashing or boxing.
- **`SymbolSet`, `DIMENSIONS`, and `SignSet.signals(SymbolSet<D>, BiFunction)`** (`sdk`) — the
  reified-set machinery generalized to the `Symbol` genus shared by `Sign` and `Dimension`.
  `SymbolSet<X extends Enum<X> & Symbol>` captures any symbol enum's constants (`of`, `size`);
  `SignSet<S>` is its sign-axis sibling, adding the sign-only operations — `map` (the ascent into a
  `SignMap`) and `signals` (the `Sign × Dimension` product). Every Signal (Sign × Dimension) API now
  publishes its dimension vocabulary as a `public static final SymbolSet<Dimension> DIMENSIONS`
  beside its `SIGNS`, and a domain builds its signal table with `SIGNS.signals(DIMENSIONS, ...)`.
  Both axes are now captured sets, so `SignalSet` construction has **no reflection at all**
  (`SignalSet`'s package-private constructor takes the two constant arrays directly), and the signal
  space shares one capture and one ordering per axis with every map drawn from those sets. `size()`
  (cardinality) lives on `SymbolSet`; the factory `BiFunction<S,D,T>` still ties the sign,
  dimension, and signal types so the compiler checks their consistency. `SignMap`/`SignalMap` are
  unchanged: translation is a sign operation, so `map` stays on `SignSet` — a dimension set is
  captured and counted, never ascended.

### Changed

- **`Cycles` and `Surveys` factories take a `SignSet<S>`, not a `Class<S>`.** `Cycles.flow/of/pool`
  and `Surveys.of/pool` now consume the sign set a domain already publishes —
  `Cycles.flow(Resources.SIGNS)`, `Surveys.pool(Statuses.SIGNS, conduit)` — rather than a sign
  class. The generic operators no longer reflect internally (`SignSet.of` / `getEnumConstants`); the
  caller passes the reified set, or `SignSet.of(signClass)` for an ad-hoc enum. Update call sites
  from
  `X.Sign.class` to `X.SIGNS`.
- **`Operations.Sign`** and **`Outcomes.Sign`** each gain a constant (`ADVANCE`, `UNKNOWN`).
  Exhaustive
  `switch` statements over either enum that lack a `default` will no longer compile until the new
  case is handled — including any caller-defined `SIGNS.map(...)` over these vocabularies.

### Design notes

- **The bracket sequencer reads `STATUS`, not `OUTCOME`.** `OUTCOME` is the grammatical verdict axis
  (it drives Scorecard success/fail ratios) and is `KIND`-restricted, so a forced operation-only
  close — `Leases.REVOKE`, `Processes.KILL` — carries no verdict; feeding it to the bracket would
  read a forced revocation as a clean `STABLE`. The canonical `STATUS` map is total over closers and
  already distinguishes `DEGRADED` from `DEFECTIVE`, which the three-valued `OUTCOME` cannot.
  `OUTCOME` stays the right input for Scorecards; `STATUS` is the right input for the trajectory.
- **The episode axis is `Operations.Sign` itself**, not a separate classifier type. Like `Outcomes`,
  it is an emittable SDK vocabulary with its own instrument; the per-domain `OPERATION` map projects
  onto it. A sign's episode position is independent of its `KIND` and its `STATUS` — `DENY` is a
  `KIND.OUTCOME` that `OPERATION` closes (`END`); `REVOKE` is a `KIND.OPERATION` that `STATUS` reads
  `DEFECTIVE` and `OPERATION` also closes.

## 3.0.0-SNAPSHOT-4 — 2026-06-13

The `Sequencers` sequencing operator — the second ascent path, the counterpart to the
`Scorecards` translation tally: operation-kind signs ascend by sequence shape rather than by
per-sign verdict. Breaks `SignMap` construction from `3.0.0-SNAPSHOT-3` (see *Changed*);
`3.0.0-SNAPSHOT` builds may break between snapshots.

### Added

- **`Sequencers`** (`sdk/meta`) — the sequencing operator: a `SignMap`-registered finite-state
  recognizer whose per-subject walk speaks `Statuses.Sign` trajectory readings. This makes
  sequencing a status-trajectory operator, not a generic pattern-to-outcome mapper: the normal,
  stable, per-sign design remains with `Scorecards`; `Sequencers` adds readings only structure can
  license (a direction opening, convergence, a timeout after an acquire, an orphaned release).
    - **`SignSet`** (`sdk`) — the source sign set reified as a first-class value: `SignSet.of(type)`
      captures one sign enum's constants, and the set's `map(fn)` method maps an interpretation over
      them, so nested sequencer states never repeat the sign class. It is the source-side sibling of
      `SignalSet` and the single construction path for every `SignMap`.
    - **`SIGNS` constants** — every Serventis API that defines a sign enum (all 33: the 27 domains
      plus the sdk vocabularies `Statuses`, `Situations`, `Systems`, `Operations`, `Outcomes`,
      `Trends`) now publishes its sign set as a `public static final SignSet<Sign> SIGNS`; the 18
      domains with canonical `STATUS`/`KIND` maps derive both from it (`SIGNS.map(...)`). Callers
      derive their own interpretations — scorecard ballots, sequencer states, status→situation
      ballots — from the same constant, with no class literal at all; third-party sign enums are
      captured directly with `SignSet.of(type)`.
    - **`Sequencers.Transition<S>`** — the small carrier behind the authoring helpers: one emitted
      `Statuses.Sign` reading plus an optional next `SignMap` state. Opaque — created only by the
      `emit` factories, with no public accessors or constructor; the operator alone reads it.
    - **`Sequencers.emit(status)`** — convenience factory for a leaf transition that emits `status`
      and resets to the root state after it is admitted.
    - **`Sequencers.emit(status, state)`** — convenience factory for a transition that emits
      `status` and moves into a nested `SignMap` state after it is admitted. `status == null` moves
      silently.
    - **`Sequencers.flow(state)`** — builds a `Flow<S, Statuses.Sign>` from the root
      `SignMap<S, Transition<S>>`. The root state is the initial/idle state; nested
      `states.map(...)`
      calls express the deeper pattern directly.
    - **Explicit silence** — if the current state map returns `null`, the walk stays in that state
      and emits nothing.
    - **Eager recognition** — each transition may speak immediately. Later readings refine earlier
      readings (`DIVERGING` → `CONVERGING` → `STABLE`) rather than waiting for a break to decide
      what the previous evidence meant.
    - **Authored policy** — idle baseline, active suppression, break handling, re-anchor, and reset
      are ordinary state choices. There is no hidden attention rule, no implicit node merge, and no
      special terminal rule.
- **`SERVENTIS.md`** — new normative section "The Sequencing Operator" (finite-state registration by
  nested `SignMap` states; eager transition readings; explicit idle baseline/suppression policy;
  time outside the recognizer; the trace premise; and the transition-pair / bounded-repetition
  idioms), plus the `Sequencers` typing note in "The Java Realization".

### Changed

- **`SignMap` construction** — the public `(Class, Function)` constructor and the `SignMap.of`
  factory have been removed; a map is obtained only by mapping a function over a `SignSet`
  (`SignSet.of(type).map(fn)`, or a sign-bearing API's published `SIGNS.map(fn)`). The sign set owns
  the source constants and the function application; `SignMap` is reduced to the immutable
  array-projection value it always behaved as.

### Design notes

- **Time is not part of the recognizer.** A sequencer answers "what ordered shape did this trace
  take?" and emits from sequence evidence only — it holds no clock. "Open too long" is a
  timeout/liveness policy: time may manufacture additional input signs upstream (domains already
  speak `TIMEOUT`/`EXPIRE`/`ABANDON` as implementer-observable signs; watchdogs and heartbeats do
  the same), but it is never hidden inside the operator. Windowing remains upstream: a window may
  produce signs, and the sequencer reads those signs as ordinary structure.
- **Scorecard boundary.** Stable, positive per-sign readings remain a translation/tally concern.
  Sequencers should be registered for real structural trajectories, especially negative or exception
  patterns. An idle baseline is only an authored sign of no active watched path, not a competing
  normal-case recognizer.
- **Trace premise.** A sequencer presumes its stream is a sequential trace; it does not attempt
  correlation. Concurrent episodes are kept on separate subjects — a circuit or conduit per
  application-level thread of activity.
- **Idioms.** Self-overlapping patterns (breaker flap) register as their shortest transition pairs,
  so the breaking sign opens the next pair; unbounded repetition (CAS retry storms) is collapsed
  upstream with `Fiber.diff()` or registered as a bounded, evidence-enough prefix.
- **Performance.** A `SignSet` holds its enum constants once and applies each mapping function once
  per sign, producing immutable, ordinal-indexed transition tables shared by all attachments;
  mapping functions are never invoked per admission. The per-subject walk is a current-map pointer
  plus the current emission slot, with no per-admission allocation.

### Documentation

- **Annotation coverage** — the Serventis surface now carries the Substrates semantic annotation
  vocabulary in full. The eight protocol interfaces are `@Abstract`, and the six non-sealed
  extension points among them (`Sign`, `Category`, `Spectrum`, `Signal`, `Signaler`, `Signer`)
  are also `@Extension`; the sealed `Dimension` and `Symbol` are abstractions only. The value types
  `SignMap`, `SignSet`, `SignalSet`, and `Sequencers.Transition` are `@Immutable`
  (`Transition` also `@Provided` — factory-only). All 37 holder classes — the domains, the sdk
  vocabularies, and the meta operators — are `@Utility`. Every `Signal` record is now uniformly
  `@Provided @Immutable` (previously inconsistent, and several carried a type-level `@Queued`
  that wrongly claimed their accessors queue work — `@Queued` is reserved for the instruments and
  the `Signer.sign`/`Signaler.signal` methods that actually enqueue emissions). On records,
  `@Provided` reads as *provided vocabulary value* — obtained from instruments in normal use, while
  the public record constructor remains available for assertions and as the signal factory passed to
  `SignSet.signals` (`Signal::new`). `@ReadOnly`, `@Temporal`, and `@Idempotent` have no Serventis
  candidates (no views, no callback-scoped types, no lifecycle methods); `@Tenure` on instruments is
  deferred pending a ruling.

## 3.0.0-SNAPSHOT-3 — 2026-06-08

Per-domain canonical `KIND` classification maps and the `Serventis.Kind` enum — the *act/result*
classification of every outcome-oriented sign set, the structural twin of `STATUS`. Additive and
compatible with `3.0.0-SNAPSHOT-2`.

### Added

- **`Serventis.Kind`** — a new enum `{ OPERATION, OUTCOME }` nested in the core API, classifying a
  `Sign` by its grammatical role: an `OPERATION` is an act the implementer performed, an `OUTCOME` a
  result it observed. The two constants name the two ascent paths — outcomes ascend by tally
  (`Scorecards`), operations by sequencing — and mirror the universal `Operations` (BEGIN/END) and
  `Outcomes` (SUCCESS/FAIL) projections.
- **`Domain.KIND`** — each of the 18 outcome-oriented domains now publishes a
  `public static final SignMap<Sign, Kind> KIND`: the canonical act/result classification. Unlike
  the partial, outcome-reading `STATUS` map, `KIND` is **total** (every sign maps to a kind, none
  abstains) and an **exhaustive switch with no `default`** (a newly added sign is a compile error
  until classified). Published by the same 18 domains as `STATUS`:
    - **pool**: `Resources`, `Leases`
    - **sync**: `Atomics`, `Latches`, `Locks`
    - **exec**: `Tasks`, `Transactions`, `Processes`, `Timers`, `Services`
    - **data**: `Caches`
    - **role**: `Agents`
    - **tool**: `Probes`, `Logs`
    - **flow**: `Flows`, `Breakers`, `Valves`, `Routers`

### Design notes

- **Grammatical, not evaluative.** `KIND` is the *act/result* classification and is deliberately
  independent of the `STATUS` reading. The two coincide for almost every sign, but seven acts are
  themselves the verdict of a prior outcome and so stay `OPERATION` while `STATUS` still reads them:
  `Leases.REVOKE`, `Atomics.PARK`, `Processes.KILL`, `Services.RECOURSE`, `Valves.DROP`,
  `Routers.FORWARD`, `Routers.DROP`. `KindConventionTest` pins this seam set, so any new divergence
  fails the build.
- **Family-relative.** A lexeme's kind is per-domain, not global: `STOP` is an `OUTCOME` in
  `Processes` (clean exit) but an `OPERATION` in `Services` (a bracket); `RELEASE` an `OUTCOME` in
  `Latches` but an `OPERATION` in `Resources`/`Locks`; `DISCONNECT` an `OUTCOME` in `Services` but
  an `OPERATION` in `Probes`.
- **Pure-outcome families.** `Timers`, `Flows`, and `Logs` classify every sign as `OUTCOME` — they
  report verdicts/conditions only, with no operations of their own.
- **Scope.** `KIND` covers the outcome-oriented (act → result) domains. Measurement families
  (`Gauges`, `Sensors`, `Counters`) are excluded by nature — their signs are observations, neither
  acts nor results.

### Documentation

- `SERVENTIS.md` — new **"The Kind of a Sign"** subsection in the Observation Model: the two ascent
  paths and `KIND` as the structural cut that routes between them.
- `SIGNS.md` — note that the per-sign `Operation`/`Outcome` tags are now executable as each domain's
  `KIND` map.

## 3.0.0-SNAPSHOT-2 — 2026-06-07

Per-domain canonical `STATUS` translation maps — the executable *immediate interpretant* of each
outcome-oriented domain's ascent into `Statuses`. Additive and compatible with `3.0.0-SNAPSHOT-1`.

### Added

- **`Domain.STATUS`** — each outcome-oriented domain now publishes a
  `public static final SignMap<Sign, Statuses.Sign> STATUS`: the canonical sign→status reading. The
  map keys on `Sign`, so usage depends on what the domain's instrument emits:
    - **bare-`Sign` (`Signer`) domains** — a direct `Scorecards.flow(Domain.STATUS)` ballot (except
      state domains such as `Breakers`, which are read latest-state via `STATUS.apply(sign)`, not
      through `Scorecards.flow` — see Design notes);
    - **signal-emitting (`Signaler`) domains** (`Leases`, `Transactions`, `Timers`, `Services`,
      `Agents`, `Probes`, `Flows`) emit `Signal`, so project the sign:
      `Scorecards.flow(Domain.STATUS.compose(Domain.Signal::sign))`.

  Either way it composes/overrides per the `SignMap` "Defaults and overrides" recipe. Maps are
  **sign-keyed** (the dimension does not change the reading), **partial** (operation signs abstain
  via `null`), and an **exhaustive switch with no `default`** (a newly added sign is a compile error
  until its reading is decided). Published by 18 domains:
    - **pool**: `Resources`, `Leases`
    - **sync**: `Atomics`, `Latches`, `Locks`
    - **exec**: `Tasks`, `Transactions`, `Processes`, `Timers`, `Services`
    - **data**: `Caches`
    - **role**: `Agents`
    - **tool**: `Probes`, `Logs`
    - **flow**: `Flows`, `Breakers`, `Valves`, `Routers`

### Documentation

- `SignMap` — new **"Defaults and overrides"** section: the two override forms (inline ballot,
  derived `SignMap`) and the `andThen` abstention caveat.

### Design notes

- **Severity calibration**: `STABLE` = nominal; `DEGRADED` = a recoverable/expected failure (deny,
  timeout, conflict, rate-limit); `DEFECTIVE` = an escalated/abnormal/terminal failure (crash,
  abandon, exhaust, forced revoke/abort, open circuit, overload drop, corruption). `DOWN` is **never
  per-sign** — "entirely non-operational" is a sustained/aggregate condition, so it arises only from
  sustained `DEFECTIVE` at the Scorecard's confidence band or from the sequencing operator, not from
  a single sign.
- Domains whose health is **emergent rather than per-sign** deliberately publish no `STATUS`:
  movement/sequencing vocabularies (`Pools`, `Exchanges`, `Queues`, `Stacks`, `Pipelines`,
  `Counters`, `Gauges`), the conversational `Actors`, and the dimension-essential `Sensors` (whose
  verdict requires the `Sign × Dimension` pair). Their ascent is the forthcoming sequencing
  operator.
- `Breakers` publishes a `STATUS` map, but its signs are **state transitions**, not a frequency
  stream — read it as a latest-state translation (`STATUS.apply(sign)`), **not** through
  `Scorecards.flow` (a decayed plurality would lag the current breaker state). A latest-value
  translator is the correct Scorecard-style operator for state domains.

### Compatibility

Non-breaking. All changes are additive — new constants and documentation; no public type, method, or
signature was removed or changed.

## 3.0.0-SNAPSHOT-1 — 2026-06-05

Translation primitives (`Scorecards`, `SignMap`) and a streaming `Cycles` operator. Additive and
compatible with `2.10.0`.

### Added

- **`Scorecards`** — the sign-to-status translation operator.
    - `Scorecards.flow(ballot)` → `Flow<E, Statuses.Signal>` — a stateful running plurality
      assessment with confidence bands (a decayed-vote tally), generic over the input type `E` (a
      bare `Sign`, a `Signal`, or any value the ballot maps to a status).
    - `Scorecards.score(window, ballot)` → `Statuses.Signal` — the windowed, stateless dual: a pure
      plurality assessment of a `Window`'s current contents.
- **`SignMap`** — a pre-computed, ordinal-indexed `Sign → T` projection backed by a `Function`; the
  one-dimensional, function-backed sibling of `SignalSet`. The function is invoked once per source
  constant at construction; thereafter `apply()` is a single array load indexed by the sign's
  ordinal.
- **`Cycles.flow(Class<S>)`** → `Flow<S, Cycles.Signal<S>>` — the streaming form of the qualitative
  repetition vocabulary (first / consecutive / returning), attachable to a conduit pool.

The Serventis flow factories take **no `Cortex`** parameter — `Scorecards.flow(ballot)` and
`Cycles.flow(signClass)` build their recipes from the `Substrates.cortex()` singleton.

### Documentation

- `SERVENTIS.md` — new **"The Observation Model"** section: the sign/signal unification (a signal is
  a qualified sign), sign-sets versus dimension-sets (category versus spectrum), the `S × D` signal
  space, the operator bands (sign-scoped, value-agnostic, translation), and the Java realization.

### Compatibility

Non-breaking. All changes are additive — two new SDK classes and one new method on `Cycles`; no
public type, method, or signature was removed or changed.
