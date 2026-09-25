// Copyright (c) 2025 William David Louth

package io.humainary.serventis.sdk;

import io.humainary.serventis.api.Serventis;
import io.humainary.specs.api.Specs.SpecDoc;
import io.humainary.specs.api.Specs.SpecRef;
import io.humainary.substrates.api.Substrates.Utility;

import static java.util.Objects.requireNonNull;

/// # Systems API
///
/// The `Systems` API provides a structured framework for observing fundamental system constraints
/// using process control vocabulary. It enables emission of semantic signals that describe the
/// operational state of core system resources: space, flow, link, and time.
///
/// ## Purpose
///
/// This API provides the **universal constraint language** in the semiotic ascent architecture.
/// Domain-specific signs (Resources, Queues, Services, etc.) translate upward into constraint
/// state assessments, enabling cross-domain reasoning about fundamental system health.
///
/// ## Design Philosophy
///
/// The API models four fundamental constraints that apply universally to any system:
///
/// - **SPACE**: Container constraint — capacity, volume, room
/// - **FLOW**: Movement constraint — throughput, bandwidth, rate
/// - **LINK**: Structural constraint — connectivity, reachability, edges
/// - **TIME**: Temporal constraint — latency, responsiveness, duration
///
/// Signs use process control vocabulary to describe constraint state:
///
/// - **NORMAL**: Operating within parameters
/// - **LIMIT**: At boundary
/// - **ALARM**: Beyond boundary, attention required
/// - **FAULT**: Failed state
///
/// ## Signal Matrix
///
/// | | SPACE | FLOW | LINK | TIME |
/// |---|---|---|---|---|
/// | **NORMAL** | Adequate room | Steady throughput | Connections stable | Response times normal |
/// | **LIMIT** | Running tight | Flow restricted | Links saturated | Timing tight |
/// | **ALARM** | Space critical | Flow blocked | Links failing | Latency critical |
/// | **FAULT** | Space exhausted | Flow stopped | Links down | Timeouts total |
///
/// ## Relationship to Other APIs
///
/// `Systems` complements [Statuses] and [Situations] in the universal SDK layer:
///
/// - **Systems**: Constraint state (NORMAL, LIMIT, ALARM, FAULT) × Constraint type (SPACE, FLOW, LINK, TIME)
/// - **Statuses**: Behavioral condition (STABLE, DEGRADED, etc.) × Confidence (TENTATIVE, MEASURED, CONFIRMED)
/// - **Situations**: Urgency (NORMAL, WARNING, CRITICAL) × Variability (CONSTANT, VARIABLE, VOLATILE)
///
/// ```
/// Domain signs → Systems (what constraint?) → Statuses (how behaving?) → Situations (how urgent?)
/// ```
///
/// ## Translation Examples
///
/// | Domain Pattern | Systems Signal | Statuses | Situations |
/// |----------------|----------------|----------|------------|
/// | Pool 90% utilized | LIMIT × SPACE | STABLE × MEASURED | NORMAL × CONSTANT |
/// | High DENY rate | ALARM × SPACE | DEGRADED × MEASURED | WARNING × CONSTANT |
/// | Services.DISCONNECT | ALARM × LINK | DEFECTIVE × CONFIRMED | CRITICAL × CONSTANT |
/// | High TIMEOUT rate | LIMIT × TIME | DIVERGING × TENTATIVE | WARNING × VARIABLE |
///
/// ## Performance Considerations
///
/// See [io.humainary.serventis.api.Serventis.Signaler] for observation reuse and the
/// limits of performance guarantees. Measure the provider and pipeline for the intended workload.
///
/// @author William David Louth
/// @since 1.0

@Utility
@SpecDoc ( "https://github.com/humainary-io/serventis-api-spec/blob/3.6.0/SPEC.md" )
@SpecRef ( "7.5" )
public final class Systems
  implements Serventis {

  /// The sign set of this API — the captured [Sign] constants from which caller-derived sign maps
  /// are mapped.

  @SpecRef ( "4.5" )
  public static final SignSet < Sign > SIGNS =
    SignSet.of (
      Sign.class
    );

  /// The dimension set of this API — the captured [Dimension] constants used to build
  /// system signal instances and caller-derived signal maps.

  @SpecRef ( "4.5" )
  public static final SymbolSet < Dimension > DIMENSIONS =
    SymbolSet.of (
      Dimension.class
    );

  private Systems () { }

  /// Creates a System instrument wrapping the specified pipe.
  ///
  /// @param pipe the pipe from which to create the system
  /// @return a new System instrument for the specified pipe
  /// @throws NullPointerException if the pipe parameter is `null`

  @SpecRef ( "6.4" )
  @New
  @NotNull
  public static System of (
    @NotNull final Pipe < ? super Signal > pipe
  ) {

    return
      new System (
        requireNonNull ( pipe )
      );

  }

  /// Returns a pool that creates cached System instruments from a conduit.
  ///
  /// Within the returned pool, repeated lookup of the same name returns the same instrument,
  /// created on first lookup. Separate pools have separate identity guarantees.
  ///
  /// @param conduit the conduit providing signal pipes
  /// @return a pool that creates System instruments
  /// @throws NullPointerException if the conduit parameter is `null`

  @SpecRef ( {"6.4", "substrates:10.1"} )
  @New
  @NotNull
  public static Pool < System > pool (
    @NotNull final Conduit < Signal > conduit
  ) {

    return
      conduit.pool (
        Systems::of
      );

  }


  /// The [Sign] enum represents the operational state of a system constraint
  /// using process control vocabulary.
  ///
  /// Signs form a severity progression describing constraint health:
  ///
  /// ```
  /// NORMAL → LIMIT → ALARM → FAULT
  /// (healthy)  (boundary)  (violated)  (failed)
  /// ```

  @SpecRef ( {"4.2", "7.5"} )
  public enum Sign
    implements Serventis.Sign {

    /// Operating within standard parameters.
    ///
    /// The constraint is comfortably satisfied with adequate headroom.
    /// No action required.

    NORMAL,

    /// At constraint boundary.
    ///
    /// The constraint is being met but margins are thin. Prepare to act.

    LIMIT,

    /// Beyond constraint boundary.
    ///
    /// The constraint is violated. Attention required.

    ALARM,

    /// Constraint failed.
    ///
    /// The constraint cannot be met. Failover or recovery required.

    FAULT

  }


  /// The [Dimension] enum represents the fundamental constraint types
  /// that apply universally to any system.

  @SpecRef ( {"4.3", "7.5"} )
  public enum Dimension
    implements Category {

    /// Container constraint — capacity, volume, room.
    ///
    /// "Running out of space."

    SPACE,

    /// Movement constraint — throughput, bandwidth, rate.
    ///
    /// "Flow is restricted."

    FLOW,

    /// Structural constraint — connectivity, reachability, edges.
    ///
    /// "The link is down."

    LINK,

    /// Temporal constraint — latency, responsiveness, duration.
    ///
    /// "Time is critical."

    TIME

  }


  /// The [Signal] record represents a system constraint assessment.
  ///
  /// @param sign      the operational state of the constraint
  /// @param dimension the type of constraint being assessed

  @Provided
  @Immutable
  @SpecRef ( "4.4" )
  public record Signal(
    @NotNull Sign sign,
    @NotNull Dimension dimension
  ) implements Serventis.Signal < Sign, Dimension > {

    /// @throws NullPointerException if `sign` or `dimension` is `null`

    public Signal {

      requireNonNull ( sign );
      requireNonNull ( dimension );

    }

  }


  /// The [System] class emits constraint state assessments for a named subject.
  ///
  /// ## Usage
  ///
  /// ```java
  /// system.normal(SPACE);    // Space constraint normal
  /// system.limit(TIME);      // Time constraint at limit
  /// system.alarm(FLOW);      // Flow constraint alarm
  /// system.fault(LINK);      // Link constraint fault
  /// ```

  @SpecRef ( {"6.2", "6.3", "6.5", "substrates:6.1"} )
  @Queued
  @Provided
  public static final class System
    implements Signaler < Sign, Dimension > {

    private static final SignalSet < Sign, Dimension, Signal > SIGNALS =
      SIGNS.signals (
        DIMENSIONS,
        Signal::new
      );

    private final Pipe < ? super Signal > pipe;

    private System (
      final Pipe < ? super Signal > pipe
    ) {

      this.pipe =
        pipe;

    }

    /// Emits an `ALARM` signal for the specified constraint dimension.
    ///
    /// @param dimension the constraint type
    /// @throws NullPointerException if the dimension is `null`

    public void alarm (
      @NotNull final Dimension dimension
    ) {

      pipe.emit (
        SIGNALS.get (
          Sign.ALARM,
          dimension
        )
      );

    }

    /// Emits a `FAULT` signal for the specified constraint dimension.
    ///
    /// @param dimension the constraint type
    /// @throws NullPointerException if the dimension is `null`

    public void fault (
      @NotNull final Dimension dimension
    ) {

      pipe.emit (
        SIGNALS.get (
          Sign.FAULT,
          dimension
        )
      );

    }

    /// Emits a `LIMIT` signal for the specified constraint dimension.
    ///
    /// @param dimension the constraint type
    /// @throws NullPointerException if the dimension is `null`

    public void limit (
      @NotNull final Dimension dimension
    ) {

      pipe.emit (
        SIGNALS.get (
          Sign.LIMIT,
          dimension
        )
      );

    }

    /// Emits a `NORMAL` signal for the specified constraint dimension.
    ///
    /// @param dimension the constraint type
    /// @throws NullPointerException if the dimension is `null`

    public void normal (
      @NotNull final Dimension dimension
    ) {

      pipe.emit (
        SIGNALS.get (
          Sign.NORMAL,
          dimension
        )
      );

    }

    /// Signals a system constraint assessment.
    ///
    /// @param sign      the operational state
    /// @param dimension the constraint type

    @SpecRef ( "6.2" )
    @Override
    public void signal (
      @NotNull final Sign sign,
      @NotNull final Dimension dimension
    ) {

      pipe.emit (
        SIGNALS.get (
          sign,
          dimension
        )
      );

    }

  }

}
