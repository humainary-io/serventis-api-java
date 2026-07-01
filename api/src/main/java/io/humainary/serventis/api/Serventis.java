// Copyright (c) 2025 William David Louth

package io.humainary.serventis.api;

import io.humainary.substrates.api.Substrates;

/// # Serventis API
///
/// The `Serventis` API provides common abstractions for all signal-based observability
/// interfaces in the Serventis framework. It defines the structural pattern that all
/// signal types follow: a composition of Sign × Dimension.
///
/// ## Purpose
///
/// This API establishes a uniform protocol for signal-based communication across all
/// Serventis observability instruments. By providing common interfaces, it enables:
///
/// - Polymorphic handling of signals from different APIs
/// - Generic utilities that work across all signal types
/// - Consistent structural patterns across the framework
/// - Type-safe composition of signs and dimensions
///
/// ## Core Abstractions
///
/// - **Signal**: A composition of Sign and Dimension representing an observable event
/// - **Sign**: The primary semantic classification of what is being observed
/// - **Dimension**: The secondary qualifier providing perspective, confidence, or directionality
///
/// ## Design Pattern
///
/// All Serventis APIs that emit signals follow this structural pattern:
///
/// ```
/// Signal = Sign × Dimension
/// ```
///
/// Where each API defines domain-specific enums for Sign and Dimension that implement
/// the common interfaces defined here.
///
/// ## Example Implementations
///
/// - **Probes**: Sign (CONNECT, TRANSFER, etc.) × Dimension (OUTBOUND, INBOUND)
/// - **Services**: Sign (START, CALL, SUCCESS, etc.) × Dimension (CALLER, CALLEE)
/// - **Agents**: Sign (OFFER, PROMISE, FULFILL, etc.) × Dimension (PROMISER, PROMISEE)
/// - **Statuses**: Sign (STABLE, DEGRADED, etc.) × Dimension (TENTATIVE, MEASURED, CONFIRMED)
///
/// ## Benefits
///
/// 1. **Architectural Consistency**: All signal-based APIs share the same structure
/// 2. **Code Reuse**: Generic utilities can process any signal type
/// 3. **Type Safety**: Each API maintains its own strongly-typed enums
/// 4. **Extensibility**: New signal-based APIs can easily adopt this pattern
/// 5. **Clarity**: The structural pattern is explicit and self-documenting
///
/// @author William David Louth
/// @since 1.0

public interface Serventis
  extends Substrates {

  /// The [Category] interface represents discrete, unordered dimensional classifications.
  ///
  /// Category dimensions classify signals into distinct kinds or aspects without implying
  /// any ordering, ranking, or progression between values. Each category value represents
  /// a qualitatively different classification.
  ///
  /// ## Examples
  ///
  /// - **Systems.Dimension**: SPACE, FLOW, LINK, TIME (what kind of constraint?)
  /// - **Services.Dimension**: CALLER, CALLEE (whose perspective?)
  /// - **Transactions.Dimension**: COORDINATOR, PARTICIPANT (what role?)
  /// - **Cycles.Dimension**: SINGLE, REPEAT, RETURN (what recurrence pattern?)
  ///
  /// ## Contrast with Spectrum
  ///
  /// Unlike [Spectrum] dimensions where values form an ordered progression (e.g., low→medium→high),
  /// Category values are peers with no inherent ordering. SPACE is not "more than" FLOW;
  /// CALLER is not "greater than" CALLEE.

  @Abstract
  @Extension
  non-sealed interface Category
    extends Dimension { }

  /// The [Dimension] interface represents the secondary qualifier for an observable event.
  ///
  /// Dimension enums in each Serventis API implement this interface to provide domain-specific
  /// qualifiers while maintaining a common protocol. Dimensions add context to signs:
  ///
  /// - **Perspective**: CALLER vs CALLEE (calling vs serving perspective)
  /// - **Promise Perspective**: PROMISER vs PROMISEE (agent role in coordination)
  /// - **Confidence**: TENTATIVE vs MEASURED vs CONFIRMED (statistical certainty)
  ///
  /// ## Dimension Subtypes
  ///
  /// Dimensions fall into two categories:
  ///
  /// - **[Category]**: Discrete, unordered classifications (e.g., SPACE/FLOW/LINK/TIME,
  ///   CALLER/CALLEE, SINGLE/REPEAT/RETURN). Values represent different kinds or aspects
  ///   without implying any ordering or progression.
  ///
  /// - **[Spectrum]**: Ordered, ranked progressions (e.g., TENTATIVE→MEASURED→CONFIRMED,
  ///   CONSTANT→VARIABLE→VOLATILE). Values represent points along a scale where relative
  ///   position is meaningful.

  @Abstract
  sealed interface Dimension
    extends Symbol
    permits Category, Spectrum { }

  /// The [Sign] interface represents the primary semantic classification of an observable event.
  ///
  /// Sign enums in each Serventis API implement this interface to provide domain-specific
  /// classifications (e.g., CONNECT, START, OFFER, STABLE) while maintaining a common protocol.

  @Abstract
  @Extension
  non-sealed interface Sign
    extends Symbol { }

  /// The [Signal] interface represents an observable event composed of a sign and dimension.
  ///
  /// This interface provides the common protocol for all signal types across Serventis APIs.
  /// Each signal combines a semantic sign (what is being observed) with a qualifying dimension
  /// (perspective, confidence, directionality, or other contextual qualifier).
  ///
  /// Implementations are typically enums or records that provide domain-specific sign and
  /// dimension values while maintaining this common structural pattern.
  ///
  /// @param <S> the Sign type implementing Serventis.Sign
  /// @param <D> the Dimension type implementing Serventis.Dimension

  @Abstract
  @Extension
  interface Signal < S extends Sign, D extends Dimension > {

    /// Returns the dimension component of this signal.
    ///
    /// The dimension provides the secondary qualifier that gives context to the sign,
    /// such as perspective (self vs observed), confidence level, or directionality.
    ///
    /// @return the dimension of this signal

    @NotNull
    D dimension ();


    /// Returns the sign component of this signal.
    ///
    /// The sign represents the primary semantic classification of the observable event.
    ///
    /// @return the sign of this signal

    @NotNull
    S sign ();

  }

  /// Marker interface for percepts that signal two-dimensional events.
  ///
  /// Signalers make signals composed of Sign × Dimension, combining semantic signs
  /// with qualifying dimensions. The dimension adds essential context such as perspective
  /// (self vs observed), confidence level (tentative vs confirmed), or role (promiser vs promisee).
  ///
  /// Examples of Signalers include:
  /// - **Probes**: (CONNECT, TRANSFER, etc.) × (OUTBOUND, INBOUND)
  /// - **Services**: (START, CALL, SUCCESS, etc.) × (CALLER, CALLEE)
  /// - **Statuses**: (STABLE, DEGRADED, etc.) × (TENTATIVE, MEASURED, CONFIRMED)
  /// - **Agents**: (OFFER, PROMISE, FULFILL, etc.) × (PROMISER, PROMISEE)
  ///
  /// @param <S> the Sign enum type implementing Serventis.Sign
  /// @param <D> the Dimension enum type implementing Serventis.Dimension

  @Abstract
  @Extension
  interface Signaler <
    S extends Enum < S > & Sign,
    D extends Enum < D > & Dimension
    > {

    /// Signals a two-dimensional event by composing sign and dimension.
    ///
    /// This method makes a signal where the sign provides the primary
    /// semantic classification and the dimension provides qualifying context.
    ///
    /// @param sign      the sign component
    /// @param dimension the dimension component

    @Queued
    void signal (
      @NotNull S sign,
      @NotNull D dimension
    );

  }

  /// Marker interface for percepts that sign single-dimensional events.
  ///
  /// Signers make signs without additional qualifiers such as perspective,
  /// confidence, or directionality. The sign itself carries the complete semantic meaning.
  ///
  /// Examples of Signers include:
  /// - **Counters**: INCREMENT, OVERFLOW, RESET
  /// - **Gauges**: INCREMENT, DECREMENT, OVERFLOW, UNDERFLOW, RESET
  /// - **Resources**: ACQUIRE, GRANT, DENY, RELEASE
  /// - **Queues**: ENQUEUE, DEQUEUE, OVERFLOW, UNDERFLOW
  ///
  /// @param <S> the Sign enum type implementing Serventis.Sign

  @Abstract
  @Extension
  interface Signer < S extends Enum < S > & Sign > {

    /// Signs a single-dimensional event.
    ///
    /// This method makes a sign representing an observable occurrence
    /// without additional qualifying dimensions.
    ///
    /// @param sign the sign to make

    @Queued
    void sign (
      @NotNull S sign
    );

  }

  /// The [Spectrum] interface represents ordered, ranked dimensional progressions.
  ///
  /// Spectrum dimensions classify signals along a scale where the relative position
  /// of values is meaningful. Values form a progression from one end to another,
  /// enabling comparison and directional reasoning.
  ///
  /// ## Examples
  ///
  /// - **Statuses.Dimension**: TENTATIVE → MEASURED → CONFIRMED (increasing confidence)
  /// - **Situations.Dimension**: CONSTANT → VARIABLE → VOLATILE (increasing variability)
  ///
  /// ## Contrast with Category
  ///
  /// Unlike [Category] dimensions where values are discrete peers, Spectrum values
  /// have meaningful ordering. CONFIRMED represents more confidence than TENTATIVE;
  /// VOLATILE represents more variability than CONSTANT.
  ///
  /// ## Ordering Convention
  ///
  /// Spectrum enums should be declared in ascending order, where the first value
  /// represents the "low" end and the last value represents the "high" end.
  /// The enum ordinal() reflects this ordering.

  @Abstract
  @Extension
  non-sealed interface Spectrum
    extends Dimension { }

  /// Base interface for enum-based classification markers in the Serventis semiotic framework.
  ///
  /// Both [Sign] and [Dimension] extend this interface to provide the fundamental enum
  /// protocol (name and ordinal) that enables generic handling across different classification
  /// types without requiring explicit generics.
  ///
  /// In semiotic terms, a symbol is a sign whose relationship to its referent is established
  /// by convention rather than resemblance or causation. The enum constants that implement
  /// this interface are symbols in exactly this sense: their meaning derives from their
  /// defined role within the Serventis vocabulary, not from any intrinsic property.
  ///
  /// This interface leverages the methods already provided by Java enums to enable
  /// polymorphic handling of classification markers across different Serventis APIs.

  @Abstract
  sealed interface Symbol
    permits Dimension,
            Sign {

    /// Returns the name of this symbol.
    ///
    /// @return the name of this enum constant, exactly as declared

    String name ();


    /// Returns the ordinal of this symbol.
    ///
    /// @return the ordinal of this enum constant (its position in the enum declaration)

    int ordinal ();

  }

  /// The [Kind] enum classifies a [Sign] within its sign set as either an [#OPERATION] — an act the
  /// producer performed — or an [#OUTCOME] — a result it observed. Operations lead to outcomes: a
  /// request, a lifecycle transition, or a flow-control move is an operation; the verdict, terminal
  /// condition, or boundary result it leads to is an outcome.
  ///
  /// The kind is a property of a sign *within its set*, as intrinsic and enumerable as the sign
  /// itself. It is **grammatical** — it asks only "act or result?" of what the sign reports — and so
  /// is decided from the sign's own meaning, independent of how any consumer later reads or aggregates
  /// it. The same lexeme may therefore take different kinds in different sets: a clean `STOP` exit is
  /// an outcome where a bracketing `STOP` is an operation, because the kind belongs to the sign in its
  /// set, not to the word.
  ///
  /// `Kind` is a structural property of the *source* sign language — not an SDK translation target
  /// like `Statuses`, nor an emitted vocabulary like `Operations`/`Outcomes` — which is why it lives
  /// in the protocol rather than the SDK. The ownership splits cleanly: the API owns the **concept**
  /// (`OPERATION` | `OUTCOME`), each domain owns its **classification** (its `KIND` map), and the SDK
  /// owns only the **lookup mechanism** ([io.humainary.serventis.sdk.SignMap]) and the operators
  /// that consume it. The SDK reads the property; it does not define it.
  ///
  /// ## Secondary: how the kind is used
  ///
  /// The kind is the structural cut that routes a sign toward the universal status language along two
  /// ascent paths. An outcome is verdict-bearing and ascends by tally — a plurality over verdicts. An
  /// operation carries no reading in isolation and ascends by the sequence it participates in (a
  /// `CALL` that never reaches an outcome is hung; a `SUSPEND` with no `RESUME` is stalled). The two
  /// constants mirror the universal `Operations` (`BEGIN`/`END`) and `Outcomes` (`SUCCESS`/`FAIL`)
  /// projections.
  ///
  /// Because the kind is grammatical rather than evaluative, it is independent of the per-domain
  /// `STATUS` reading and not derivable from it: `STATUS` abstains on most operations, yet a few acts
  /// that are themselves the verdict of a prior outcome — a forced revoke, a load-shedding drop, a
  /// degraded-mode recourse — stay [#OPERATION] here while `STATUS` still reads them.
  ///
  /// Each outcome-oriented domain materializes the classification as a `public static final
  /// SignMap<Sign, Kind> KIND` — total (none abstains) and exhaustive (a new sign is a compile error
  /// until classified), the structural twin of the partial, outcome-reading `STATUS` map. `Kind` is
  /// not (yet) carried on [Sign] itself; the per-domain map is its home until a generic, Sign-agnostic
  /// operator needs to read it off an arbitrary sign.

  enum Kind {

    /// An act the producer performed — a request, a lifecycle transition, a flow-control move, or a
    /// recovery action (`CALL`, `START`, `SUSPEND`, `RETRY`, `RECOURSE`, …). Operations lead to the
    /// outcomes they produce.

    OPERATION,

    /// A result the producer observed — a verdict, a terminal condition, or a boundary result
    /// (`SUCCESS`, `FAIL`, `GRANT`, `DENY`, `TIMEOUT`, `OVERFLOW`, …). An outcome is what an operation
    /// leads to.

    OUTCOME

  }

}
