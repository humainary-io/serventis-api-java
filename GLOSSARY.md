# Serventis API Glossary

Definitions of core elements in the Humainary Serventis API, organized by functional area.

## Framework Primitives

* **Symbol**: Sealed base interface for all classification markers. Provides `name()` and
  `ordinal()` for enum-like identity. Subtypes are Sign and Dimension.

* **Sign**: Non-sealed marker interface extending Symbol. Represents a primary semantic
  classification. Each domain API defines its own Sign enum (e.g., `Tasks.Sign`, `Locks.Sign`).

* **Dimension**: Sealed marker interface extending Symbol. Adds a secondary qualifier to a sign.
  Two subtypes:
    * **Category**: Discrete, unordered classifications representing perspective or constraint type
    * **Spectrum**: Ordered, ranked progressions representing degree or confidence

* **Signal**: A record composing Sign × Dimension into a single observable event. Each two-
  dimensional API defines its own Signal record type. Pre-allocated via SignalSet to eliminate
  emission-path allocations.

* **SignalSet**: A pre-allocated lookup table storing the full cartesian product of Sign × Dimension
  as Signal instances. Indexed by `sign.ordinal() * columns + dimension.ordinal()` for O(1) access.
  Constructed once per API with a factory function (e.g., `Signal::new`).

## Emission Interfaces

* **Signer**: Base interface for one-dimensional emission. Method: `sign(S)`. Used by APIs where
  a single sign captures the full semantics (Tasks, Locks, Queues, Resources, Counters). Obtained
  from a domain-specific `Pool<Instrument>` derived from a substrates `Conduit`.

* **Signaler**: Base interface for two-dimensional emission. Method: `signal(S, D)`. Used by APIs
  with perspective-based observation (Services, Transactions, Systems, Agents). The signal is
  resolved from a static SignalSet before emission into the underlying pipe.

## Instrument Pattern

Each Serventis API defines a concrete **instrument** class that implements Signer or Signaler:

* **Instrument**: A domain-specific wrapper around a substrates `Pipe`. Constructed via the API's
  `of(Pipe)` factory and obtained through a `Pool<Instrument>` keyed by name. Provides semantic
  convenience methods that delegate to `sign()` or `signal()` (e.g., `task.complete()` instead of
  `task.sign(COMPLETE)`, `service.start(CALLEE)` instead of `service.signal(START, CALLEE)`).

* **Pool factory**: A static `pool(Conduit)` method on each API class (e.g., `Tasks.pool(conduit)`,
  `Statuses.pool(conduit)`) returning a `Pool<Instrument>` derived from the conduit. The pool
  caches one instrument per name on first lookup via the underlying substrates `Conduit.pool(fn)`
  mechanism, with lazy materialization at `pool.get(name)`.

* **Generic Instruments**: Surveys and Cycles are generic over any Sign type. Their pool factories
  take a Sign class parameter alongside the conduit (e.g., `Surveys.pool(Statuses.Sign.class,
  conduit)`) so the SignalSet for the given sign vocabulary is constructed at instrument creation
  time.

## Bracketing Helpers

Some two-dimensional instruments provide operation bracketing methods:

* **Fn**: Functional interface for operations that return a result and may throw. Used by bracketing
  methods like `Service.execute(Dimension, Fn)` which automatically emits BEGIN/END or
  SUCCESS/FAIL signals around the work.

* **Op**: Functional interface for void operations that may throw. Used by bracketing methods like
  `Service.dispatch(Dimension, Op)`.

## API Packages

* **api** (`io.humainary.serventis.api`): Framework primitives — Symbol, Sign, Dimension, Signal,
  SignalSet, Signer, Signaler. The foundational types that all domain APIs build upon.

* **sdk** (`io.humainary.serventis.sdk`): Universal languages — Statuses, Situations, Operations,
  Outcomes, Systems, Surveys, Trends. Domain-independent vocabularies that enable cross-domain
  reasoning. Domain-specific signs translate upward into these universal languages.

* **sdk/meta** (`io.humainary.serventis.sdk.meta`): Meta-observation — Cycles. Vocabulary for
  observing patterns in sign streams themselves (repetition, recurrence).

* **opt/exec** (`io.humainary.serventis.opt.exec`): Execution — Tasks, Services, Transactions,
  Processes, Timers. Work units, service interactions, and transactional coordination.

* **opt/data** (`io.humainary.serventis.opt.data`): Data structures — Queues, Stacks, Caches,
  Pipelines. Bounded container and data flow operations.

* **opt/sync** (`io.humainary.serventis.opt.sync`): Synchronization — Locks, Latches, Atomics.
  Mutual exclusion, barriers, and atomic coordination.

* **opt/pool** (`io.humainary.serventis.opt.pool`): Resource pooling — Resources, Pools, Exchanges,
  Leases. Acquisition, release, and lifecycle of shared resources.

* **opt/flow** (`io.humainary.serventis.opt.flow`): Flow control — Flows, Routers, Breakers, Valves.
  Routing, circuit breaking, and backpressure.

* **opt/tool** (`io.humainary.serventis.opt.tool`): Instrumentation — Counters, Gauges, Probes,
  Sensors, Logs. Metrics, health checks, and structured logging.

* **opt/role** (`io.humainary.serventis.opt.role`): Coordination — Agents, Actors. Promise-based
  and conversational interaction models.

## Semiotic Ascent

The defining architectural pattern. Domain instruments (opt/\*) emit signs that translate upward
through universal languages (sdk) into interpretive assessments:

1. **Domain Signs** (opt/\*): Raw implementer-observable events emitted by instruments
2. **Universal Languages** (sdk): Normalized operational vocabulary — subscribers translate domain
   signs into statuses, outcomes, and system constraints
3. **Interpretive Layer** (Situations): Second-order assessment combining status patterns into
   urgency judgments for response decisions

See `SERVENTIS.md` for the theoretical foundation and `SIGNS.md` for sign classification patterns.
