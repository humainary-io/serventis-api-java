// Copyright (c) 2025 William David Louth

package io.humainary.serventis.opt.tool;

import io.humainary.serventis.api.Serventis;
import io.humainary.serventis.sdk.*;
import io.humainary.specs.api.Specs.SpecDoc;
import io.humainary.specs.api.Specs.SpecRef;
import io.humainary.substrates.api.Substrates.Utility;

import static java.util.Objects.requireNonNull;

/// # Evals API
///
/// The `Evals` API reports qualitative evaluation results about a named subject:
///
/// ```text
/// PASS | FAIL | UNKNOWN | SKIP | ERROR × evaluation criterion
/// ```
///
/// The sign says how the evaluation concluded. The dimension says which property was judged.
/// `UNKNOWN` means the criterion was evaluated but the verdict was indeterminate. `SKIP` records a
/// deliberate decision not to evaluate the criterion. `ERROR` records an attempted evaluation that
/// could not complete. Emit no Evals signal when no evaluation event was observed.
///
/// The subject is the evaluated *unit of work* — the turn, run, or response being judged. Criteria
/// judging what it produced ([Dimension#CORRECTNESS], [Dimension#GROUNDEDNESS], and the rest) and
/// criteria judging how it produced it ([Dimension#TOOLING], [Dimension#ROUTING]) are alike
/// properties of that one subject.
///
/// The subject is never the evaluator. `ERROR` is the one result that says nothing about the unit of
/// work: it reports a defect in the evaluator, so it abstains from [#STATUS] and stays out of the
/// success/failure tallies in [#OUTCOME]. Report evaluator health against the evaluator's own
/// subject.
///
/// Evals reports qualitative results only. Scores, error details, judge provenance, and evaluation
/// orchestration belong to the producer or another instrumentation vocabulary.
///
/// The criterion vocabulary is closed so that an observer can weight the criteria against each
/// other; [#DIMENSIONS] shows how.
///
/// ```java
/// var eval =
///   Evals.pool ( circuit.conduit ( Signal.class ) )
///        .get ( cortex.name ( "support.answer" ) );
///
/// eval.pass ( GROUNDEDNESS );
/// eval.fail ( ROUTING );
/// eval.unknown ( CORRECTNESS );
/// eval.skip ( HELPFULNESS );
/// eval.error ( SAFETY );
/// ```
///
/// @author William David Louth
/// @since 3.0

@Utility
@SpecDoc ( "https://github.com/humainary-io/serventis-api-spec/blob/3.6.0/SPEC.md" )
@SpecRef ( {"8.2", "registry:evals"} )
public final class Evals
  implements Serventis {

  /// The signs emitted by Evals.

  @SpecRef ( "4.5" )
  public static final SignSet < Sign > SIGNS =
    SignSet.of (
      Sign.class
    );

  /// The evaluation criteria that qualify Evals signs.
  ///
  /// The set is closed so that an observer can weight the criteria against each other: a safety
  /// failure and a completeness failure are not the same event, and a vocabulary any producer could
  /// extend would hand the observer criteria it has no weighting for. That weighting is why this
  /// constant is published — crossed with [#SIGNS] it builds a [SignalMap] keyed on the whole
  /// signal, where the canonical [#STATUS] is keyed on the sign alone and so reads every criterion
  /// alike:
  ///
  /// ```java
  /// static final SignalMap < Sign, Dimension, Signal, Statuses.Sign > WEIGHTED =
  ///   SIGNS.signals ( DIMENSIONS, Signal::new )
  ///        .map (
  ///          signal -> switch ( signal.sign () ) {
  ///            case PASS -> Statuses.Sign.STABLE;
  ///            case FAIL -> switch ( signal.dimension () ) {
  ///              case SAFETY, CORRECTNESS -> Statuses.Sign.DEFECTIVE;
  ///              default                  -> Statuses.Sign.DEGRADED;
  ///            };
  ///            case UNKNOWN, SKIP, ERROR -> null;
  ///          }
  ///        );
  ///
  /// var reading = WEIGHTED.apply ( signal );
  /// ```
  ///
  /// The projection runs once per signal when the map is built; each later lookup is one array load
  /// indexed by the two ordinals. Weightings belong here and not in the canonical maps — [#STATUS],
  /// [#KIND], and [#OUTCOME] key on the sign alone by design.

  @SpecRef ( "4.5" )
  public static final SymbolSet < Dimension > DIMENSIONS =
    SymbolSet.of (
      Dimension.class
    );

  /// Canonical sign-to-status translation. A pass reads stable and a failed criterion reads
  /// degraded. Everything that renders no verdict abstains — an indeterminate result, a deliberate
  /// skip, and a judge error alike.
  ///
  /// The three abstentions are alike in the only respect this map reads: none of them says anything
  /// about the unit of work. A fail-closed gate that wants an unevaluable criterion to block is
  /// policy rather than reading, and belongs in the observer's weighted map — see [#DIMENSIONS].
  ///
  /// As Evals emits [Signal], compose [Signal#sign()] before using this sign-keyed map as a ballot.
  ///
  /// Exhaustive without a `default`: a new [Sign] is a compile error here until its reading is
  /// decided.

  public static final SignMap < Sign, Statuses.Sign > STATUS =
    SIGNS.map (
      sign -> switch ( sign ) {
        case PASS -> Statuses.Sign.STABLE;
        case FAIL -> Statuses.Sign.DEGRADED;
        case UNKNOWN, SKIP, ERROR -> null;
      }
    );

  /// Canonical sign-to-kind classification. Every Evals sign is a terminal evaluation outcome,
  /// including a deliberate skip or judge error.
  ///
  /// Exhaustive without a `default`: a new [Sign] is a compile error here until its kind is decided.

  public static final SignMap < Sign, Kind > KIND =
    SIGNS.map (
      sign -> switch ( sign ) {
        case PASS, FAIL, UNKNOWN, SKIP, ERROR -> Kind.OUTCOME;
      }
    );

  /// Canonical translation into the universal [Outcomes] vocabulary. A judge error leaves a verdict
  /// that never settled, so it joins `UNKNOWN` on the indeterminate reading and stays out of the
  /// success/failure tallies — a flaky evaluator must not depress the evaluated subject's ratios.
  /// A deliberate skip renders no verdict at all and therefore abstains from the verdict axis
  /// entirely, the [Outcomes.Sign#UNKNOWN] `null`-versus-`UNKNOWN` distinction.
  ///
  /// Exhaustive without a `default`: a new [Sign] is a compile error here until its reading is
  /// decided.

  public static final SignMap < Sign, Outcomes.Sign > OUTCOME =
    SIGNS.map (
      sign -> switch ( sign ) {
        case PASS -> Outcomes.Sign.SUCCESS;
        case FAIL -> Outcomes.Sign.FAIL;
        case UNKNOWN, ERROR -> Outcomes.Sign.UNKNOWN;
        case SKIP -> null;
      }
    );

  private Evals () { }

  /// Creates an Eval instrument wrapping the specified pipe.
  ///
  /// @param pipe the pipe used to emit evaluation signals
  /// @return a new Eval instrument
  /// @throws NullPointerException if `pipe` is `null`

  @SpecRef ( "6.4" )
  @New
  @NotNull
  public static Eval of (
    @NotNull final Pipe < ? super Signal > pipe
  ) {

    return
      new Eval (
        requireNonNull ( pipe )
      );

  }

  /// Returns a pool that creates cached Eval instruments from a conduit.
  ///
  /// Within the returned pool, repeated lookup of the same name returns the same instrument,
  /// created on first lookup. Separate pools have separate identity guarantees.
  ///
  /// @param conduit the conduit providing evaluation signal pipes
  /// @return a pool that creates Eval instruments
  /// @throws NullPointerException if `conduit` is `null`

  @SpecRef ( {"6.4", "substrates:10.1"} )
  @New
  @NotNull
  public static Pool < Eval > pool (
    @NotNull final Conduit < Signal > conduit
  ) {

    return
      conduit.pool (
        Evals::of
      );

  }


  /// The result recorded for one evaluation criterion.

  @SpecRef ( {"4.2", "registry:evals"} )
  public enum Sign
    implements Serventis.Sign {

    /// The subject satisfied the criterion.

    PASS,

    /// The subject violated the criterion.

    FAIL,

    /// The criterion was evaluated but the verdict was indeterminate.

    UNKNOWN,

    /// The criterion was deliberately not evaluated.

    SKIP,

    /// Evaluation of the criterion could not complete because the judge errored or timed out.

    ERROR

  }


  /// The property judged by an evaluation.
  ///
  /// The criteria divide into those judging what the unit of work produced — [#CORRECTNESS],
  /// [#COMPLETENESS], [#GROUNDEDNESS], [#HELPFULNESS], [#ADHERENCE], [#SAFETY] — and those judging
  /// how it went about producing it — [#TOOLING], [#ROUTING]. Both are properties of the same
  /// subject, which is the unit of work rather than the artifact alone.
  ///
  /// The criteria are deliberately non-overlapping, so that a single defect fires exactly one of
  /// them and an observer weighting the set never counts that defect twice. A criterion is admitted
  /// only when an observer would weight it differently from every criterion already present.

  @SpecRef ( {"4.3", "registry:evals"} )
  public enum Dimension
    implements Category {

    /// Factual or functional correctness.

    CORRECTNESS,

    /// Coverage of the information or work required.

    COMPLETENESS,

    /// Support in authoritative evidence or supplied context.

    GROUNDEDNESS,

    /// Usefulness in accomplishing the request or objective. Subsumes mere relevance: an answer that
    /// does not address the request cannot be useful for it, so an off-topic result fails here and
    /// is not counted a second time under a separate relevance criterion.

    HELPFULNESS,

    /// Compliance with instructions, formats, and schemas. Excludes safety policy, which [#SAFETY]
    /// judges — a safety violation is not also an adherence violation.

    ADHERENCE,

    /// Quality of tool selection, arguments, and use.

    TOOLING,

    /// Quality of delegation, handoff, and context transfer.

    ROUTING,

    /// Compliance with safety requirements.

    SAFETY

  }


  /// An instrument that emits evaluation results about a named subject.

  @SpecRef ( {"6.2", "6.3", "6.5", "substrates:6.1"} )
  @Queued
  @Provided
  public static final class Eval
    implements Signaler < Sign, Dimension > {

    private static final SignalSet < Sign, Dimension, Signal > SIGNALS =
      SIGNS.signals (
        DIMENSIONS,
        Signal::new
      );

    private final Pipe < ? super Signal > pipe;

    private Eval (
      final Pipe < ? super Signal > pipe
    ) {

      this.pipe =
        pipe;

    }

    /// Emits an `ERROR` result for the specified criterion.
    ///
    /// @param dimension the criterion whose evaluation could not complete

    public void error (
      @NotNull final Dimension dimension
    ) {

      pipe.emit (
        SIGNALS.get (
          Sign.ERROR,
          dimension
        )
      );

    }

    /// Emits a `FAIL` verdict for the specified criterion.
    ///
    /// @param dimension the criterion that was violated

    public void fail (
      @NotNull final Dimension dimension
    ) {

      pipe.emit (
        SIGNALS.get (
          Sign.FAIL,
          dimension
        )
      );

    }

    /// Emits a `PASS` verdict for the specified criterion.
    ///
    /// @param dimension the criterion that was satisfied

    public void pass (
      @NotNull final Dimension dimension
    ) {

      pipe.emit (
        SIGNALS.get (
          Sign.PASS,
          dimension
        )
      );

    }

    /// Emits the specified evaluation result and criterion.
    ///
    /// @param sign      the evaluation result
    /// @param dimension the criterion associated with the result

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

    /// Emits a `SKIP` result for the specified criterion.
    ///
    /// @param dimension the criterion deliberately not evaluated

    public void skip (
      @NotNull final Dimension dimension
    ) {

      pipe.emit (
        SIGNALS.get (
          Sign.SKIP,
          dimension
        )
      );

    }

    /// Emits an `UNKNOWN` verdict for the specified criterion.
    ///
    /// @param dimension the criterion whose verdict was indeterminate

    public void unknown (
      @NotNull final Dimension dimension
    ) {

      pipe.emit (
        SIGNALS.get (
          Sign.UNKNOWN,
          dimension
        )
      );

    }

  }

  /// An evaluation result qualified by the property judged.
  ///
  /// @param sign      the evaluation result
  /// @param dimension the criterion associated with the result

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

}
