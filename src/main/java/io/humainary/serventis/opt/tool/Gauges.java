// Copyright (c) 2025 William David Louth
package io.humainary.serventis.opt.tool;

import io.humainary.serventis.api.Serventis;
import io.humainary.serventis.sdk.SignMap;
import io.humainary.serventis.sdk.SignSet;
import io.humainary.specs.api.Specs.SpecDoc;
import io.humainary.specs.api.Specs.SpecRef;
import io.humainary.substrates.api.Substrates.Utility;

import static io.humainary.serventis.api.Serventis.Kind.OPERATION;
import static io.humainary.serventis.api.Serventis.Kind.OUTCOME;
import static io.humainary.serventis.opt.tool.Gauges.Sign.*;
import static java.util.Objects.requireNonNull;

/// # Gauges API
///
/// The `Gauges` API provides a structured and minimal interface for observing
/// bidirectional numeric values in systems. It enables systems to emit **semantic signals**
/// representing gauge operations, boundary conditions, and state changes.
///
/// ## Purpose
///
/// This API is designed to support **observability and reasoning** about fluctuating
/// metrics in systems. By modeling gauge interactions as composable signals, it enables
/// introspection of bidirectional flows, capacity utilization, and boundary violations
/// without coupling to specific implementation details.
///
/// ## Important: Reporting vs Implementation
///
/// This API is for **reporting gauge semantics**, not implementing gauges.
/// If you have an actual gauge implementation (AtomicInteger connection pool, resource
/// tracker, etc.), use this API to emit observability signals about operations performed
/// on it. Observer agents can then reason about capacity patterns, utilization trends,
/// and boundary conditions without coupling to your implementation details.
///
/// **Example**: When your connection pool accepts a new connection, you would call
/// `gauge.increment()` to emit a signal. When a connection is released, call
/// `gauge.decrement()`. The signals enable meta-observability: observing the observability
/// instrumentation itself to understand resource utilization and capacity dynamics.
///
/// ## Key Concepts
///
/// - **Gauge**: A named subject that emits signs describing operations performed against it
/// - **Sign**: An enumeration of distinct operation types: `INCREMENT`, `DECREMENT`, `OVERFLOW`, `UNDERFLOW`, `RESET`
///
/// ## Signs and Semantics
///
/// | Sign        | Description                                               |
/// |-------------|-----------------------------------------------------------|
/// | `INCREMENT` | The gauge increased                                       |
/// | `DECREMENT` | The gauge decreased                                       |
/// | `OVERFLOW`  | The gauge exceeded its maximum value                      |
/// | `UNDERFLOW` | The gauge fell below its minimum value                    |
/// | `RESET`     | The gauge was explicitly reset to baseline                |
///
/// ## Semantic Distinctions
///
/// - **INCREMENT/DECREMENT**: Normal operational signs - expected bidirectional changes
/// - **OVERFLOW/UNDERFLOW**: Exceptional boundary signs - capacity limit violations
/// - **RESET**: Intentional operational sign - explicit return to baseline
///
/// ## Use Cases
///
/// - Tracking active connections, queue depth, in-flight requests
/// - Monitoring resource utilization (memory, threads, file handles)
/// - Observing bidirectional flows (entry/exit patterns)
/// - Detecting capacity saturation and starvation conditions
///
/// ## Saturation vs Wrapping
///
/// Gauges can exhibit different boundary behaviors when hitting limits:
/// - **Wrapping**: Overflow/underflow causes value to wrap to opposite bound
/// - **Saturation**: Value sticks at boundary (common for resource limits)
///
/// The gauge emits the boundary signal; observer agents interpret subsequent
/// signals to determine which behavior occurred.
///
/// **Wrapping example**: A numeric gauge at max value wraps to zero on overflow
/// ```
/// gauge at MAX → increment(10) → OVERFLOW(10) → gauge wraps to 9
/// Subsequent INCREMENT signals indicate wrapping occurred
/// ```
///
/// **Saturation example**: A resource gauge at max capacity stays at max
/// ```
/// connections at MAX → increment() → OVERFLOW(1) → connections still at MAX
/// No subsequent INCREMENT signals indicate saturation
/// ```
///
/// Observer agents can detect the difference by monitoring signal patterns after boundary events.
///
/// ## Relationship to Other APIs
///
/// `Gauges` signals can inform higher-level abstractions:
///
/// - **Counters API**: Gauges provide **bidirectional** metric tracking, while Counters
///   enforce **monotonic increase**. Gauges are the superset that allows both INCREMENT
///   and DECREMENT operations. If your metric never legitimately decreases, prefer Counters
///   for semantic clarity and type safety. Key difference: Gauges allow DECREMENT and treat
///   UNDERFLOW as a boundary violation (falling below minimum) rather than an error condition.
/// - **Queues API**: Queue depth can be modeled as a gauge (INCREMENT on PUT, DECREMENT on TAKE)
/// - **Resources API**: Resource availability can be tracked via gauge operations
/// - **Statuses API**: Gauge patterns inform capacity and utilization conditions
/// - Observer agents translate gauge signals into capacity, trend, or threshold signs
///
/// ## Performance Considerations
///
/// See [io.humainary.serventis.api.Serventis.Signer] for observation reuse and the
/// limits of performance guarantees. Measure the provider and pipeline for the intended workload.
///
/// @author William David Louth
/// @since 1.0

@Utility
@SpecDoc ( "https://github.com/humainary-io/serventis-api-spec/blob/3.6.0/SPEC.md" )
@SpecRef ( {"8.2", "registry:gauges"} )
public final class Gauges
  implements Serventis {

  /// The sign set of this API — the captured [Sign] constants from which the canonical [#KIND]
  /// interpretation, and any caller-derived sign maps, are mapped.

  @SpecRef ( "4.5" )
  public static final SignSet < Sign > SIGNS =
    SignSet.of (
      Sign.class
    );

  /// Canonical sign-to-kind classification for gauges — each [Sign] tagged [Kind#OPERATION] or
  /// [Kind#OUTCOME]: increment, decrement, and reset are operations; the overflow/underflow boundary
  /// results are outcomes. Exhaustive without a `default`. See [Kind].

  public static final SignMap < Sign, Kind > KIND =
    SIGNS.map (
      sign -> switch ( sign ) {
        case OVERFLOW, UNDERFLOW -> OUTCOME;
        case INCREMENT, DECREMENT, RESET -> OPERATION;
      }
    );

  private Gauges () { }

  /// Creates a Gauge instrument wrapping the specified pipe.
  ///
  /// @param pipe the pipe from which to create the gauge
  /// @return a new Gauge instrument for the specified pipe
  /// @throws NullPointerException if the pipe parameter is `null`

  @SpecRef ( "6.4" )
  @New
  @NotNull
  public static Gauge of (
    @NotNull final Pipe < ? super Sign > pipe
  ) {

    return
      new Gauge (
        requireNonNull ( pipe )
      );

  }

  /// Returns a pool that creates cached Gauge instruments from a conduit.
  ///
  /// Within the returned pool, repeated lookup of the same name returns the same instrument,
  /// created on first lookup. Separate pools have separate identity guarantees.
  ///
  /// @param conduit the conduit providing sign pipes
  /// @return a pool that creates Gauge instruments
  /// @throws NullPointerException if the conduit parameter is `null`

  @SpecRef ( {"6.4", "substrates:10.1"} )
  @New
  @NotNull
  public static Pool < Gauge > pool (
    @NotNull final Conduit < Sign > conduit
  ) {

    return
      conduit.pool (
        Gauges::of
      );

  }

  /// A [Sign] represents the kind of action being observed in a gauge interaction.
  ///
  /// These signs distinguish between bidirectional operations (INCREMENT/DECREMENT),
  /// boundary violations (OVERFLOW/UNDERFLOW), and intentional control (RESET).

  @SpecRef ( {"4.2", "registry:gauges"} )
  public enum Sign
    implements Serventis.Sign {

    /// Indicates the gauge was incremented.
    ///
    /// This sign represents normal gauge increase operations. Common in scenarios
    /// like connection establishment, resource acquisition, or queue entry.

    INCREMENT,

    /// Indicates the gauge was decremented.
    ///
    /// This sign represents normal gauge decrease operations. Common in scenarios
    /// like connection closure, resource release, or queue exit.

    DECREMENT,

    /// Indicates the gauge exceeded its maximum value.
    ///
    /// Overflow reveals capacity violations where the gauge hit its upper bound.
    /// The subsequent behavior (wrap or saturate) is determined by implementation and
    /// can be inferred by observer agents from subsequent signs.

    OVERFLOW,

    /// Indicates the gauge fell below its minimum value.
    ///
    /// Underflow reveals capacity violations where the gauge hit its lower bound.
    /// This often indicates attempted operations on empty or depleted resources.

    UNDERFLOW,

    /// Indicates the gauge was explicitly reset to baseline.
    ///
    /// This sign represents intentional reset operations, distinct from boundary
    /// conditions.

    RESET

  }

  /// The [Gauge] class represents a named, observable gauge from which signs are emitted.
  ///
  /// ## Usage
  ///
  /// Use domain-specific methods: `gauge.increment()`, `gauge.decrement()`, `gauge.overflow()`,
  /// `gauge.underflow()`, `gauge.reset()`
  ///
  /// Gauges provide semantic methods for reporting gauge operation events.

  @SpecRef ( {"6.1", "6.3", "6.5", "substrates:6.1"} )
  @Queued
  @Provided
  public static final class Gauge
    implements Signer < Sign > {

    private final Pipe < ? super Sign > pipe;

    private Gauge (
      final Pipe < ? super Sign > pipe
    ) {

      this.pipe =
        pipe;

    }

    /// Emits a decrement sign from this gauge.

    public void decrement () {

      pipe.emit (
        DECREMENT
      );

    }

    /// Emits an increment sign from this gauge.

    public void increment () {

      pipe.emit (
        INCREMENT
      );

    }

    /// Emits an overflow sign from this gauge.

    public void overflow () {

      pipe.emit (
        OVERFLOW
      );

    }

    /// Emits a reset sign from this gauge.

    public void reset () {

      pipe.emit (
        RESET
      );

    }

    /// Signs a gauge event.
    ///
    /// @param sign the sign to make

    @SpecRef ( "6.1" )
    @Override
    public void sign (
      @NotNull final Sign sign
    ) {

      pipe.emit (
        sign
      );

    }

    /// Emits an underflow sign from this gauge.

    public void underflow () {

      pipe.emit (
        UNDERFLOW
      );

    }

  }

}
