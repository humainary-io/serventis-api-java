// Copyright (c) 2025 William David Louth

package io.humainary.serventis.sdk;

import io.humainary.serventis.api.Serventis;
import io.humainary.substrates.api.Substrates.Utility;

import java.util.function.Function;

import static io.humainary.serventis.sdk.Statuses.Dimension.*;
import static io.humainary.substrates.api.Substrates.cortex;
import static java.util.Objects.requireNonNull;

/// # Scorecards API
///
/// The `Scorecards` API provides a **translation operator** that ascends a stream of
/// domain signs into running [Statuses] assessments by weighted plurality vote. Where the
/// expression vocabularies ([Surveys], [io.humainary.serventis.sdk.meta.Cycles]) report a
/// judgment *something else* computed, a Scorecard **is** the voting machine: it tallies
/// each incoming sign as a vote for a status, continuously, and emits the currently winning
/// status with a confidence band derived from how the votes have accumulated.
///
/// ## Purpose
///
/// This is the first concrete realization of the *translation function* described in
/// `SERVENTIS.md` — the abductive ascent from a domain sign set into the universal status
/// language. The intelligence of the semiotic architecture "lies not in the vocabularies but
/// in the translation paths between them"; a Scorecard is one such path, made executable.
///
/// A Scorecard defines **no signs and no dimensions of its own**. Its output is an ordinary
/// [Statuses.Signal], so a scored assessment is indistinguishable from a hand-emitted status
/// and flows on into [Situations] through the existing ascent without any further wiring.
///
/// ## Shape: a Flow, not an instrument
///
/// Unlike [Statuses.Status] or [Surveys.Survey], a Scorecard is **stateful** — it owns a running
/// tally — so it is delivered as a substrates [Flow] rather than a hand-fed instrument. The flow
/// consumes whatever the upstream conduit emits — a bare [Sign], a [Signal] (whose `dimension()`
/// the ballot may read), or any value — and produces a [Statuses.Signal]. Attach it to the target
/// status conduit; each named percept materializes its own independent tally, mutated on the
/// circuit thread:
///
/// ```java
/// // Translate Resources signs into running status assessments.
/// Conduit < Statuses.Signal > statuses = circuit.conduit ( Statuses.Signal.class );
///
/// Pool < Pipe < Resources.Sign > > scored =
///   statuses.pool (
///     Scorecards.flow (
///       sign -> switch ( sign ) {
///         case GRANT          -> Statuses.Sign.STABLE;
///         case DENY, TIMEOUT  -> Statuses.Sign.DEGRADED;
///         default             -> null;            // abstain — casts no vote
///       }
///     )
///   );
///
/// Pipe < Resources.Sign > in = scored.get ( name );   // emit domain signs here
/// in.emit ( Resources.Sign.GRANT );                   // status signals land in `statuses`
/// ```
///
/// For a [Signaler]-style source that emits `Signal < S, D >`, key the ballot on the whole signal —
/// e.g. `signal -> switch ( signal.sign () ) { ... }`, reading `signal.dimension ()` where the
/// dimension changes the verdict (a `MISS × DEADLINE` may translate differently than a
/// `MISS × THRESHOLD`). No projection step is required.
///
/// ## The ballot
///
/// The `ballot` maps each upstream emission to the status it votes for, with **unit weight**.
/// Returning `null` abstains: the tally is left untouched and nothing is emitted for that input.
/// The emission may be a bare sign, a full `Signal` (so the ballot can weigh its dimension), or any
/// value — the ballot is the sole interpreter.
///
/// Any `Function < ? super E, Statuses.Sign >` works. For a bare-sign ballot that is expensive or
/// reused, back it with a [SignMap], which pre-computes the sign-to-status translation once and
/// resolves it as an ordinal-indexed array load per emission. For a dimension-sensitive signal
/// ballot, use [SignalSet#map(Function)] to create a [SignalMap] backed by the full `Sign ×
/// Dimension` table.
///
/// ## Tally and confidence (fixed policy)
///
/// The tally is a **decayed plurality** vote. On each vote every status bucket decays by `DECAY`
/// and the voted bucket gains `1 - DECAY`, so the buckets sum toward `1` and the winner's share is
/// its normalized dominance. Decay (rather than lifetime accumulation) is what keeps the assessment
/// *continuous* — a cumulative tally asymptotes and stops tracking recent behaviour.
///
/// The winner's confidence is banded into the [Statuses.Dimension] spectrum, honouring its
/// "evidence accumulates" semantics — early observations stay `TENTATIVE` regardless of share:
///
/// | Band         | Condition                                             |
/// |--------------|-------------------------------------------------------|
/// | `TENTATIVE`  | evidence below warm-up, **or** winner share `< 0.50`  |
/// | `MEASURED`   | warmed up and winner share in `[0.50, 0.80)`          |
/// | `CONFIRMED`  | warmed up and winner share `>= 0.80`                  |
///
/// A signal is emitted on **every vote** (abstains excepted) — consistent with other Serventis
/// instruments, which do not self-deduplicate. Collapsing to transitions, or transitions with a
/// max-silence keep-alive, is left to the caller as a composable output fiber rather than built in,
/// since more than one shaping policy is valid.
///
/// ## Windowed scoring (no memory beyond the window)
///
/// [#flow] keeps a soft, exponentially-decayed memory — every past vote fades but never fully
/// leaves. For a **hard, bounded** memory — assess the score over exactly the last *N* (or last
/// *duration*) signs, with nothing carried beyond that — use [#score] as the projection of a
/// windowed flow:
///
/// ```java
/// cortex.flow ( Resources.Sign.class )
///   .window ( 16 )                                // Flow< Sign, Window< Sign > >
///   .map ( w -> Scorecards.score ( w, ballot ) )  // Window< Sign > -> Statuses.Signal
/// ```
///
/// `score` is a pure function of the window's current contents — no decay, no warm-up, equal weight
/// per sign — so a sign that ages out of the window stops counting entirely. The window *is* the
/// only state; there is no carry-over between emissions.
///
/// ## Performance Considerations
///
/// All 21 `Statuses.Sign × Statuses.Dimension` signals are pre-allocated once in a shared
/// [SignalSet]. Each vote decays the buckets in a single pass and updates the winner incrementally,
/// so emission reduces to a banding comparison plus one array lookup — with no allocation on the
/// hot path. The per-subject tally holds a small `double[]` and runs single-threaded on the
/// circuit, so it is safe without synchronization.
///
/// @author William David Louth
/// @since 3.0

@Utility
public final class Scorecards
  implements Serventis {

  /// Per-vote retention: every bucket decays by this factor on each vote, giving an
  /// effective memory of roughly `1 / ( 1 - DECAY )` votes.

  private static final double DECAY = 0.95d;

  /// The per-vote increment added to the voted bucket and to accumulated evidence (`1 - DECAY`).

  private static final double GAIN = 1.0d - DECAY;

  /// Accumulated evidence (`1 - DECAY^votes`) must reach this before a winner can be
  /// reported above `TENTATIVE` — the warm-up gate that keeps early calls tentative.

  private static final double WARMUP = 0.25d;

  /// Winner share at or above this reports `CONFIRMED`.

  private static final double STRONG = 0.80d;

  /// Winner share below this reports `TENTATIVE` (once warmed up, `[WEAK, STRONG)` is `MEASURED`).

  private static final double WEAK = 0.50d;

  /// Status signs by ordinal, cached to avoid the defensive copy of `values()` per assessment.

  private static final Statuses.Sign[] SIGNS =
    Statuses.Sign.values ();

  /// Shared, pre-allocated table of every `Statuses.Sign × Statuses.Dimension` signal.

  private static final SignalSet < Statuses.Sign, Statuses.Dimension, Statuses.Signal > SIGNALS =
    Statuses.SIGNS.signals (
      Statuses.DIMENSIONS,
      Statuses.Signal::new
    );

  private Scorecards () { }

  /// Bands a winner's score against the running `total` into the [Statuses.Dimension] confidence
  /// spectrum, comparing the share `max / total` to `WEAK` and `STRONG` — expressed as
  /// multiplications by `total` to avoid a division. A `total` below `warmup` is held at
  /// `TENTATIVE` (the streaming warm-up gate); pass `0` to disable the gate (the windowed path,
  /// which has no warm-up).

  private static Statuses.Dimension band (
    final double max,
    final double total,
    final double warmup
  ) {

    return
      total < warmup || max < WEAK * total
      ? TENTATIVE
      : max >= STRONG * total
        ? CONFIRMED
        : MEASURED;

  }

  /// Returns a flow that tallies each upstream emission as a weighted vote for a status and emits
  /// the running winner with a confidence band.
  ///
  /// The flow's input type `E` is whatever the upstream conduit emits — a bare [Sign], a [Signal]
  /// (whose `dimension()` the ballot may read), or any value — and its output is a [Statuses.Signal].
  /// Attach it to a status [Conduit] via [Conduit#pool(Flow)] — each named percept materializes its
  /// own tally.
  ///
  /// The recipe is built from the singleton [io.humainary.substrates.api.Substrates#cortex()]; a
  /// flow is a standalone, circuit-agnostic value (materializable against any pipe of that cortex),
  /// so no cortex need be threaded in.
  ///
  /// @param <E>    the upstream emission type being translated
  /// @param ballot maps each emission to the status it votes for; `null` abstains
  /// @return a flow translating emissions into running status assessments
  /// @throws NullPointerException if `ballot` is `null`

  @New
  @NotNull
  public static < E > Flow < E, Statuses.Signal > flow (
    @NotNull final Function < ? super E, Statuses.Sign > ballot
  ) {

    requireNonNull ( ballot );

    return
      cortex (). < E > flow ()
        .scan (
          Tally::new,
          ( tally, value ) -> {
            tally.cast (
              ballot.apply ( value )
            );
            return tally;
          },
          Tally::assess
        );

  }

  /// Scores a window of upstream emissions in one pass and returns the plurality-winning status
  /// with a confidence band — a pure, stateless assessment of the window's current contents.
  ///
  /// This is the bounded-memory dual of [#flow]: where the flow keeps a decayed running tally,
  /// `score` counts the votes present in `window` with equal weight, so an emission that has aged
  /// out of the window no longer counts. Compose it as the projection of a windowed flow:
  /// `flow(type).window(n).map(w -> Scorecards.score(w, ballot))`.
  ///
  /// There is **no warm-up gate** — the band is purely the winner's share over the votes in the
  /// window (the window size is the caller's chosen evidence horizon). This is a sharp difference
  /// from [#flow], whose warm-up gate holds sparse early evidence at `TENTATIVE`: here a window with
  /// a *single* non-abstaining vote reports that status at `CONFIRMED` (100% share). If that is
  /// undesirable, gate it yourself — e.g. only score once the window has filled.
  ///
  /// A `null` ballot result abstains and is not counted; the method returns `null` when the window
  /// holds no votes (empty, or every sign abstained), which filters the emission when used in `map`.
  ///
  /// This is **not** an allocation-free path. The per-call bucket accumulator is passed through
  /// `Window.fold` — an opaque cross-module call — so escape analysis does not eliminate it
  /// (measured at ~120 B/op for windowed scoring, the bulk of which is the substrate's per-emission
  /// window view rather than this accumulator). Negligible at typical status rates, but not zero.
  ///
  /// @param <E>    the upstream emission type being scored
  /// @param window the window of upstream emissions to assess
  /// @param ballot maps each emission to the status it votes for; `null` abstains
  /// @return the winning status with a confidence band, or `null` if the window holds no votes
  /// @throws NullPointerException if `window` or `ballot` is `null`

  public static < E > Statuses.Signal score (
    @NotNull final Window < E > window,
    @NotNull final Function < ? super E, Statuses.Sign > ballot
  ) {

    requireNonNull ( window );
    requireNonNull ( ballot );

    final var counts =
      window.fold (
        new int[SIGNS.length],
        ( acc, value ) -> {
          final var target = ballot.apply ( value );
          if ( target != null ) {
            acc[target.ordinal ()]++;
          }
          return acc;
        }
      );

    var win = 0;
    var max = counts[0];
    var total = counts[0];

    for ( var i = 1; i < counts.length; i++ ) {
      final var c = counts[i];
      total += c;
      if ( c > max ) {
        max = c;
        win = i;
      }
    }

    if ( total == 0 ) {
      return null;
    }

    return
      SIGNALS.get (
        SIGNS[win],
        band ( max, total, 0d )   // windowed: no warm-up gate
      );

  }

  /// The mutable, per-subject vote accumulator. One instance is created per flow attachment
  /// (per named percept) and is only ever touched on the circuit thread.

  private static final class Tally {

    private final double[] scores = new double[SIGNS.length];
    private       double   evidence;
    private       int      winner;   // ordinal of the current argmax (lowest ordinal on ties)
    private       boolean  voted;

    /// Projects the current tally into a status signal, or `null` to filter the emission
    /// (the most recent input abstained).

    private Statuses.Signal assess () {

      if ( !voted ) {
        return null;
      }

      final var w = winner;
      return
        SIGNALS.get (
          SIGNS[w],
          band ( scores[w], evidence, WARMUP )
        );

    }

    /// Casts one vote. A `null` target abstains: state is left untouched and the next
    /// assessment is suppressed.

    private void cast (
      final Statuses.Sign target
    ) {

      if ( target == null ) {
        voted = false;
        return;
      }

      voted = true;

      final var s = scores;

      for ( var i = 0; i < s.length; i++ ) {
        s[i] *= DECAY;
      }

      final var t =
        target.ordinal ();

      s[t] += GAIN;

      evidence =
        evidence * DECAY + GAIN;

      // Uniform decay preserves order and only bucket `t` gains, so the new winner is either the
      // old winner or `t` — tracked incrementally to avoid an O(buckets) argmax scan per vote.
      // The `t < w` tie rule preserves the lowest-ordinal choice a full scan would make.
      final var w = winner;

      if ( s[t] > s[w] || ( s[t] == s[w] && t < w ) ) {
        winner = t;
      }

    }

  }

}
