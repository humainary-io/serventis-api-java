// Copyright (c) 2025 William David Louth

package io.humainary.serventis.sdk;

import io.humainary.serventis.api.Serventis;
import io.humainary.serventis.sdk.meta.Cycles;
import io.humainary.specs.api.Specs.SpecDoc;
import io.humainary.specs.api.Specs.SpecRef;
import io.humainary.substrates.api.Substrates.Utility;

import static java.util.Objects.requireNonNull;

/// # Surveys API
///
/// The `Surveys` API provides a structured framework for expressing collective assessment
/// outcomes when multiple observers or cluster members evaluate a subject's status.
/// It captures the degree of agreement among observers, complementing the individual
/// confidence expressed in [Statuses].
///
/// ## Purpose
///
/// This API provides the **collective agreement vocabulary** in the semiotic ascent architecture.
/// While [Statuses] expresses individual assessment with confidence (how sure am I?),
/// Surveys expresses collective assessment with agreement (how much do we agree?).
///
/// ## Important: Collective Assessment Layer
///
/// This API is for **reporting collective assessment outcomes**, not implementing voting systems.
/// When your consensus mechanism, cluster coordinator, or polling system aggregates assessments
/// from multiple observers and determines the collective judgment, use this API to emit
/// signals about that collective outcome. Observer agents can then reason about collective
/// health patterns and agreement dynamics without coupling to your consensus implementation.
///
/// **Example**: Your cluster has 5 nodes monitoring a shared resource. After polling all nodes,
/// 4 report DEGRADED and 1 reports STABLE. Call `survey.signal(DEGRADED, MAJORITY)` to express
/// that the collective assessment is DEGRADED by majority agreement.
///
/// ## Key Concepts
///
/// - **Survey**: An instrument that emits collective assessment signals for a named subject
/// - **Signal**: A pairing of any sign type with an agreement dimension
/// - **Dimension**: The degree of collective agreement (DIVIDED, MAJORITY, UNANIMOUS)
///
/// ## Generic Over Sign Types
///
/// Like [Cycles], Surveys is **generic over any Sign type**. The Sign comes from the
/// source API being surveyed (typically [Statuses.Sign], but could be any domain sign).
/// Surveys defines only the Dimension, which describes the agreement level.
///
/// ## Dimensions
///
/// | Dimension  | Meaning                           | Observable From                    |
/// |------------|-----------------------------------|------------------------------------|
/// | `DIVIDED`  | No clear majority                 | Highest vote share ≤ threshold     |
/// | `MAJORITY` | Clear majority but not unanimous  | Highest vote share > threshold, < 100% |
/// | `UNANIMOUS`| Complete agreement                | One sign has 100%                  |
///
/// ## Usage Example
///
/// ```java
/// // Create a survey instrument for Status signs
/// var conduit = circuit.conduit(Signal.class);
/// var survey  = Surveys.pool(Statuses.SIGNS, conduit).get(cortex.name("cluster.health"));
///
/// // After polling/aggregating observer status assessments:
/// survey.signal(DEGRADED, MAJORITY);   // Most observers say DEGRADED
/// survey.signal(STABLE, UNANIMOUS);    // All observers agree STABLE
/// survey.signal(DIVERGING, DIVIDED);   // Observers split on DIVERGING
/// ```
///
/// ## Relationship to Other APIs
///
/// Surveys complements Statuses in the observability hierarchy:
///
/// ```
/// Individual: Status (what I assess) × Confidence (how sure I am)
/// Collective: Survey (what we assess) × Agreement (how much we agree)
/// ```
///
/// - **Statuses API**: Individual assessment with confidence dimension
/// - **Surveys API**: Collective assessment with agreement dimension
/// - **Situations API**: Consumes both to produce situational assessments
///
/// ## Semiotic Relationship
///
/// The parallel structure enables rich observability:
///
/// | Layer      | Sign Source    | Dimension        | Question Answered          |
/// |------------|----------------|------------------|----------------------------|
/// | Status     | Status signs   | Confidence       | How sure am I?             |
/// | Survey     | Any signs      | Agreement        | How much do we agree?      |
///
/// A subject might have:
/// - Individual status: DEGRADED × MEASURED (I'm fairly sure it's degraded)
/// - Collective survey: DEGRADED × MAJORITY (most of us agree it's degraded)
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
@SpecRef ( {"7.7", "7.8"} )
public final class Surveys
  implements Serventis {

  /// The pre-captured dimension set of [Dimension] — the agreement axis crossed with any surveyed
  /// sign vocabulary to build a Survey's signal table.

  @SpecRef ( "4.5" )
  public static final SymbolSet < Dimension > DIMENSIONS =
    SymbolSet.of (
      Dimension.class
    );

  private Surveys () { }

  /// Creates a Survey instrument wrapping the specified pipe.
  ///
  /// The sign set is required to enable SignalSet pre-allocation of all
  /// Sign x Dimension combinations.
  ///
  /// @param <S>   the Sign enum type from the source API
  /// @param signs the sign set of the source API (a domain's published `SIGNS`)
  /// @param pipe  the pipe from which to create the survey
  /// @return a new Survey instrument for the specified pipe
  /// @throws NullPointerException if signs or pipe is `null`

  @SpecRef ( "6.4" )
  @New
  @NotNull
  public static < S extends Enum < S > & Sign > Survey < S > of (
    @NotNull final SignSet < S > signs,
    @NotNull final Pipe < ? super Signal < S > > pipe
  ) {

    return
      new Survey <> (
        requireNonNull ( signs ),
        requireNonNull ( pipe )
      );

  }

  /// Returns a pool that creates cached Survey instruments from a conduit.
  ///
  /// Within the returned pool, repeated lookup of the same name returns the same instrument,
  /// created on first lookup. Separate pools have separate identity guarantees.
  ///
  /// @param <S>     the Sign enum type from the source API
  /// @param signs   the sign set of the source API (a domain's published `SIGNS`)
  /// @param conduit the conduit providing signal pipes
  /// @return a pool that creates Survey instruments
  /// @throws NullPointerException if signs or conduit is `null`

  @SpecRef ( {"6.4", "substrates:10.1"} )
  @New
  @NotNull
  public static < S extends Enum < S > & Sign > Pool < Survey < S > > pool (
    @NotNull final SignSet < S > signs,
    @NotNull final Conduit < Signal < S > > conduit
  ) {

    final var checkedSigns =
      requireNonNull ( signs );

    return
      conduit.pool (
        pipe ->
          of (
            checkedSigns,
            pipe
          )
      );

  }


  /// The [Dimension] enum represents the degree of collective agreement
  /// among observers assessing a subject.
  ///
  /// Dimensions describe the level of consensus reached when multiple
  /// observers or cluster members evaluate the same subject.

  @SpecRef ( {"4.3", "7.8"} )
  public enum Dimension
    implements Spectrum {

    /// No clear majority among observers.
    ///
    /// DIVIDED indicates that observers are split with no single sign
    /// achieving majority agreement. The collective assessment is
    /// inconclusive, requiring either more data or resolution mechanisms.

    DIVIDED,

    /// Clear majority but not complete agreement.
    ///
    /// MAJORITY indicates that most observers agree on a particular sign,
    /// but some dissenting assessments exist. The collective judgment is
    /// reasonably confident but not unanimous.

    MAJORITY,

    /// Complete agreement among all observers.
    ///
    /// UNANIMOUS indicates that every observer agrees on the same sign.
    /// The collective assessment is definitive with no dissent.

    UNANIMOUS

  }

  /// The [Signal] record represents a collective assessment for any sign type.
  ///
  /// Unlike domain API signals that pair domain-specific signs with dimensions,
  /// Survey signals are generic over the sign type. The sign comes from whatever
  /// source API is being surveyed (typically Statuses); the dimension comes from
  /// this API representing the agreement level.
  ///
  /// @param <S>       the Sign type from the source API
  /// @param sign      the collective judgment (the sign most observers reported)
  /// @param dimension the degree of agreement among observers

  @SpecRef ( "4.4" )
  @Provided
  @Immutable
  public record Signal < S extends Sign >(
    @NotNull S sign,
    @NotNull Dimension dimension
  ) implements Serventis.Signal < S, Dimension > {

    /// @throws NullPointerException if `sign` or `dimension` is `null`

    public Signal {

      requireNonNull ( sign );
      requireNonNull ( dimension );

    }

  }

  /// The [Survey] class emits collective assessment signals for signs from a source API.
  ///
  /// A Survey instrument is generic over the Sign type of the source API being
  /// surveyed. It provides vocabulary for expressing the degree of agreement
  /// among multiple observers assessing the same subject.
  ///
  /// ## Usage
  ///
  /// ```java
  /// survey.signal(Statuses.Sign.DEGRADED, Dimension.MAJORITY);
  /// survey.signal(Statuses.Sign.STABLE, Dimension.UNANIMOUS);
  /// survey.signal(Statuses.Sign.DIVERGING, Dimension.DIVIDED);
  /// ```
  ///
  /// @param <S> the Sign enum type from the source API being surveyed

  @SpecRef ( {"6.2", "6.5", "substrates:6.1"} )
  @Queued
  @Provided
  public static final class Survey < S extends Enum < S > & Sign >
    implements Signaler < S, Dimension > {

    private final SignalSet < S, Dimension, Signal < S > > signals;
    private final Pipe < ? super Signal < S > >            pipe;

    private Survey (
      final SignSet < S > signs,
      final Pipe < ? super Signal < S > > pipe
    ) {

      this.signals =
        signs.signals (
          DIMENSIONS,
          Signal::new
        );

      this.pipe =
        pipe;

    }

    /// Emits a collective assessment signal for the specified sign and agreement level.
    ///
    /// @param sign      the sign from the source API representing the collective judgment
    /// @param dimension the degree of agreement among observers
    /// @throws NullPointerException if sign or dimension is `null`

    @SpecRef ( "6.2" )
    @Override
    public void signal (
      @NotNull final S sign,
      @NotNull final Dimension dimension
    ) {

      pipe.emit (
        signals.get (
          sign,
          dimension
        )
      );

    }

  }

}
