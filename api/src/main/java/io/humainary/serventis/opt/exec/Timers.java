// Copyright (c) 2025 William David Louth

package io.humainary.serventis.opt.exec;

import io.humainary.serventis.api.Serventis;
import io.humainary.serventis.sdk.*;
import io.humainary.substrates.api.Substrates.Utility;

import static io.humainary.serventis.api.Serventis.Kind.OUTCOME;
import static io.humainary.serventis.sdk.Statuses.Sign.DEGRADED;
import static io.humainary.serventis.sdk.Statuses.Sign.STABLE;

/// # Timers API
///
/// The `Timers` API provides a structured framework for observing time constraint outcomes
/// in software systems. It enables emission of semantic signals about whether execution
/// met or missed time constraints, supporting performance analysis and SLA monitoring
/// without encoding quantitative time values in the signal vocabulary.
///
/// ## Purpose
///
/// This API enables systems to emit **qualitative signals** about time constraint outcomes,
/// capturing the binary result of time-bounded operations: did the execution satisfy
/// the constraint or violate it? The implementer determines the constraint; the API
/// provides the vocabulary for reporting the outcome.
///
/// **Design Principle**: The API follows the Apdex-inspired model of qualitative time
/// classification. Rather than emitting durations or timestamps, implementers emit
/// simple MEET/MISS signals. Pattern recognition and threshold interpretation happen
/// in subscribers, not at the emission point.
///
/// ## Important: Reporting vs Enforcement
///
/// This API is for **reporting time constraint outcomes**, not implementing timers or
/// enforcing deadlines. When your system completes time-bounded work (requests with SLAs,
/// tasks with deadlines, operations with performance targets), use this API to emit
/// observability signals about whether constraints were satisfied. Meta-level observers
/// can then aggregate patterns to assess system health without coupling to your
/// implementation details.
///
/// **Example**: When a request completes, check if it met the performance threshold.
/// Call `timer.meet(THRESHOLD)` if within target, `timer.miss(THRESHOLD)` if exceeded.
/// Subscribers aggregate MISS rates to determine if the system is degraded.
///
/// ## Key Concepts
///
/// - **Timer**: An observable instrument for reporting time constraint outcomes
/// - **Signal**: A composition of Sign (outcome) and Dimension (constraint type)
/// - **Sign**: The outcome of the time constraint (MEET or MISS)
/// - **Dimension**: The type of time constraint (DEADLINE or THRESHOLD)
/// - **Deadline**: An absolute point in time by which something must complete
/// - **Threshold**: A relative duration or performance target
///
/// ## Two-Dimension Model
///
/// Every signal combines an outcome (Sign) with a constraint type (Dimension):
///
/// | Dimension   | Semantic                                    | Example                        |
/// |-------------|---------------------------------------------|--------------------------------|
/// | DEADLINE    | Absolute time constraint                    | "Complete by 3pm"              |
/// | THRESHOLD   | Relative duration/performance target        | "Complete within 500ms"        |
///
/// | Sign        | Semantic                                    | Outcome                        |
/// |-------------|---------------------------------------------|--------------------------------|
/// | MEET        | Constraint satisfied                        | Completed within bounds        |
/// | MISS        | Constraint violated                         | Exceeded time bounds           |
///
/// ## Signal Matrix
///
/// The complete signal space:
///
/// | Signal                | Meaning                                        |
/// |-----------------------|------------------------------------------------|
/// | MEET × DEADLINE       | Completed before the absolute deadline         |
/// | MISS × DEADLINE       | Completed after the deadline passed            |
/// | MEET × THRESHOLD      | Completed within performance target            |
/// | MISS × THRESHOLD      | Exceeded performance threshold                 |
///
/// ## Usage Patterns
///
/// ### Performance Threshold (Apdex-style)
/// ```java
/// // Request processing with latency SLA
/// long start = System.nanoTime();
/// processRequest();
/// long elapsed = System.nanoTime() - start;
///
/// if (elapsed <= targetNanos) {
///   timer.meet(THRESHOLD);    // Within performance target
/// } else {
///   timer.miss(THRESHOLD);    // Exceeded performance target
/// }
/// ```
///
/// ### Absolute Deadline
/// ```java
/// // Task with deadline
/// Instant deadline = Instant.now().plusSeconds(30);
/// processTask();
///
/// if (Instant.now().isBefore(deadline)) {
///   timer.meet(DEADLINE);     // Completed before deadline
/// } else {
///   timer.miss(DEADLINE);     // Missed the deadline
/// }
/// ```
///
/// ### Batch Processing with Deadline
/// ```java
/// // Batch job with nightly deadline
/// timer.meet(DEADLINE);  // 95% of batches completed on time
/// timer.miss(DEADLINE);  // This batch ran over
/// ```
///
/// ## Subscriber-Side Interpretation
///
/// The power of this API comes from subscriber-side pattern recognition:
///
/// ```
/// MISS rate on THRESHOLD > 15%  → Translate to DEGRADED status
/// MISS rate on DEADLINE > 5%    → Translate to CRITICAL situation
/// Sudden spike in MISS signals  → Trigger capacity alert
/// ```
///
/// Quantitative judgment (what MISS rate is "too high") lives in subscriber policy,
/// not in the sign vocabulary. This enables different observers to apply different
/// thresholds based on their requirements.
///
/// ## Relationship to Other APIs
///
/// `Timers` complements other Serventis APIs:
///
/// - **Tasks API**: Task execution may have time budgets (task completes → timer MEET/MISS)
/// - **Services API**: Service calls have SLAs (call completes → timer MEET/MISS)
/// - **Transactions API**: Transactions may have time limits (commit → timer MEET/MISS)
/// - **Resources API**: Resource acquisition may be time-bounded (acquire → timer MEET/MISS)
/// - **Statuses API**: Timer patterns inform system status (many MISS → DEGRADED)
/// - **Situations API**: Deadline patterns inform situation assessment
///
/// ## Relationship to TIMEOUT and EXPIRE
///
/// Timers API focuses on **completion outcomes** relative to constraints:
///
/// - **TIMEOUT** (other APIs): Operation never completed (gave up waiting)
/// - **EXPIRE** (other APIs): Time budget exhausted during execution
/// - **MEET/MISS** (Timers): Operation completed, classifying whether it met the constraint
///
/// TIMEOUT and EXPIRE indicate failure to complete; MEET/MISS classify completions.
///
/// ## Performance Considerations
///
/// Timer sign emissions operate at completion timescales, which vary by use case:
/// - Request/response: microseconds to milliseconds
/// - Batch processing: seconds to minutes
/// - Scheduled tasks: minutes to hours
///
/// Zero-allocation enum emission with ~10-20ns cost ensures timer observability
/// adds negligible overhead to completion paths. Signs flow asynchronously through
/// the circuit's event queue, decoupling observation from execution.
///
/// ## Aggregation Patterns
///
/// Timer signals enable simple, powerful aggregations:
///
/// - **Hit rate**: MEET count / (MEET + MISS count) × 100%
/// - **Miss rate**: MISS count / (MEET + MISS count) × 100%
/// - **By constraint type**: Separate rates for DEADLINE vs THRESHOLD
/// - **Time windows**: Rates over last minute, hour, day
///
/// These aggregations map directly to Apdex-style scoring and SLA compliance metrics.
///
/// ## Semiotic Ascent: Timer Signs → Status → Situation
///
/// Timer signs translate upward through the semiotic hierarchy:
///
/// ### Timer → Status Translation
///
/// The canonical [Timers#STATUS] map reads each sign at the **level** axis: `MEET → STABLE`,
/// `MISS → DEGRADED`. *Chronic* misses are not a different status sign — they surface as rising
/// **confidence** on the same sign (`DEGRADED × CONFIRMED`) as a Scorecard accumulates the meet/miss
/// plurality. The sharper `DEADLINE`-miss reading (`→ DEFECTIVE`) is **dimension-aware** and so
/// cannot come from the sign-keyed default; a consumer that wants it composes a ballot on the
/// `Signal` (e.g. `MISS × DEADLINE → DEFECTIVE`). A `DIVERGING` "constraints worsening" reading is a
/// *trend*, the province of the sequencing operator rather than this map.
///
/// ### Status → Situation Assessment
/// - Sustained `DEGRADED` from THRESHOLD misses → WARNING situation (need more resources)
/// - `DEFECTIVE` from a deadline-aware ballot → CRITICAL situation (immediate intervention)
/// - Pattern of morning MISS spikes → WARNING situation (load balancing needed)
///
/// This hierarchical meaning-making enables cross-domain reasoning: timer outcomes
/// translate to status assessments, which inform situational awareness. Observers
/// understand time performance impact on service quality without needing implementation details.
///
/// @author William David Louth
/// @since 1.0

@Utility
public final class Timers
  implements Serventis {

  /// The sign set of this API — the captured [Sign] constants from which the canonical
  /// [#STATUS] and [#KIND] interpretations, and any caller-derived sign maps, are mapped.

  public static final SignSet < Sign > SIGNS =
    SignSet.of (
      Sign.class
    );

  public static final SymbolSet < Dimension > DIMENSIONS =
    SymbolSet.of (
      Dimension.class
    );

  /// Canonical sign-to-status translation for timers — the default *immediate interpretant* of the
  /// upward ascent (sign-keyed; `DEADLINE` vs `THRESHOLD` does not change the reading, though a
  /// consumer may weight deadline misses more severely by composing a ballot on the `Signal` — see
  /// [SignMap]). Meeting the constraint reads healthy; missing it reads degraded. The Apdex-style hit
  /// rate emerges from the Scorecard's plurality. The instrument emits `Signal`, so as a Scorecards
  /// ballot project the sign first — `Scorecards.flow ( STATUS.compose ( Signal::sign ) )`.
  ///
  /// Exhaustive without a `default`: a new [Sign] is a compile error here until its reading is decided.

  public static final SignMap < Sign, Statuses.Sign > STATUS =
    SIGNS.map (
      sign -> switch ( sign ) {
        case MEET -> STABLE;
        case MISS -> DEGRADED;
      }
    );

  /// Canonical sign-to-kind classification for timers — both signs are [Kind#OUTCOME]: `MEET`/`MISS`
  /// are verdicts on whether a completion satisfied its time constraint (Timers reports no operations
  /// of its own). The DEADLINE/THRESHOLD perspective does not change the kind. Exhaustive without a
  /// `default`. See [Kind].

  public static final SignMap < Sign, Kind > KIND =
    SIGNS.map (
      sign -> switch ( sign ) {
        case MEET, MISS -> OUTCOME;
      }
    );

  private Timers () { }

  /// Creates a Timer instrument wrapping the specified pipe.
  ///
  /// @param pipe the pipe from which to create the timer
  /// @return a new Timer instrument for the specified pipe
  /// @throws NullPointerException if the pipe parameter is `null`

  @New
  @NotNull
  public static Timer of (
    @NotNull final Pipe < ? super Signal > pipe
  ) {

    return
      new Timer (
        pipe
      );

  }

  /// Returns a pool that creates cached Timer instruments from a conduit.
  ///
  /// @param conduit the conduit providing signal pipes
  /// @return a pool that creates Timer instruments
  /// @throws NullPointerException if the conduit parameter is `null`

  @New
  @NotNull
  public static Pool < Timer > pool (
    @NotNull final Conduit < Signal > conduit
  ) {

    return
      conduit.pool (
        Timers::of
      );

  }

  /// A [Dimension] represents the type of time constraint being evaluated.
  ///
  /// These dimensions distinguish between absolute time constraints (DEADLINE)
  /// and relative duration constraints (THRESHOLD).

  public enum Dimension
    implements Category {

    /// An absolute time constraint.
    ///
    /// DEADLINE represents a specific point in time by which something must complete.
    /// Examples: "Complete by 3pm", "Finish before midnight", "Deploy by Friday".
    /// The constraint is anchored to wall-clock time, not relative to when work started.

    DEADLINE,

    /// A relative duration constraint.
    ///
    /// THRESHOLD represents a performance target expressed as a duration from start.
    /// Examples: "Complete within 500ms", "Respond in under 100ms", "Process within 5 seconds".
    /// The constraint is relative to when the operation began, not to wall-clock time.
    /// This aligns with Apdex-style performance classification.

    THRESHOLD

  }

  /// A [Sign] represents the outcome of evaluating a time constraint.
  ///
  /// These signs provide a binary classification: did the execution satisfy
  /// the time constraint (MEET) or violate it (MISS)?

  public enum Sign
    implements Serventis.Sign {

    /// Indicates the time constraint was satisfied.
    ///
    /// MEET represents successful completion within the time bounds.
    /// For DEADLINE: completed before the absolute deadline.
    /// For THRESHOLD: completed within the performance target duration.
    ///
    /// High MEET rates indicate healthy time performance.

    MEET,

    /// Indicates the time constraint was violated.
    ///
    /// MISS represents completion that exceeded the time bounds.
    /// For DEADLINE: completed after the deadline passed.
    /// For THRESHOLD: exceeded the performance target duration.
    ///
    /// Rising MISS rates indicate degrading time performance and may
    /// require capacity adjustments or architectural review.

    MISS

  }

  /// Represents a signal combining a [Sign] and [Dimension].
  ///
  /// Signal instances are created by combining signs (MEET, MISS) with dimensions
  /// (DEADLINE, THRESHOLD) to represent specific time constraint outcomes.

  @Provided
  @Immutable
  public record Signal(
    Sign sign,
    Dimension dimension
  ) implements Serventis.Signal < Sign, Dimension > { }

  /// The [Timer] class represents a named, observable timer from which signals are emitted.
  ///
  /// A timer is an instrument for reporting time constraint outcomes. It does not
  /// measure time itself; rather, it receives signals about whether time constraints
  /// were met or missed, enabling observability of time performance across the system.
  ///
  /// ## Usage
  ///
  /// Use domain-specific methods with dimensions:
  ///
  /// ```java
  /// // Performance threshold (Apdex-style)
  /// if (elapsed <= target) {
  ///   timer.meet(THRESHOLD);
  /// } else {
  ///   timer.miss(THRESHOLD);
  /// }
  ///
  /// // Absolute deadline
  /// if (completedBeforeDeadline) {
  ///   timer.meet(DEADLINE);
  /// } else {
  ///   timer.miss(DEADLINE);
  /// }
  /// ```

  @Queued
  @Provided
  public static final class Timer
    implements Signaler < Sign, Dimension > {

    private static final SignalSet < Sign, Dimension, Signal > SIGNALS =
      SIGNS.signals (
        DIMENSIONS,
        Signal::new
      );

    private final Pipe < ? super Signal > pipe;

    private Timer (
      final Pipe < ? super Signal > pipe
    ) {

      this.pipe = pipe;

    }

    /// Emits a MEET signal for the specified dimension.
    ///
    /// Indicates that the time constraint was satisfied.
    ///
    /// @param dimension the type of constraint (DEADLINE or THRESHOLD)

    public void meet (
      @NotNull final Dimension dimension
    ) {

      pipe.emit (
        SIGNALS.get (
          Sign.MEET,
          dimension
        )
      );

    }

    /// Emits a MISS signal for the specified dimension.
    ///
    /// Indicates that the time constraint was violated.
    ///
    /// @param dimension the type of constraint (DEADLINE or THRESHOLD)

    public void miss (
      @NotNull final Dimension dimension
    ) {

      pipe.emit (
        SIGNALS.get (
          Sign.MISS,
          dimension
        )
      );

    }

    /// Signals a timer event.
    ///
    /// @param sign      the outcome (MEET or MISS)
    /// @param dimension the constraint type (DEADLINE or THRESHOLD)

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
