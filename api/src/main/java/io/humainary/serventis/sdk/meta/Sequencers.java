// Copyright (c) 2025 William David Louth

package io.humainary.serventis.sdk.meta;

import io.humainary.serventis.api.Serventis;
import io.humainary.serventis.sdk.Operations;
import io.humainary.serventis.sdk.SignMap;
import io.humainary.serventis.sdk.Statuses;
import io.humainary.substrates.api.Substrates.Utility;

import static io.humainary.serventis.sdk.Statuses.Sign.*;
import static io.humainary.substrates.api.Substrates.cortex;
import static java.util.Objects.requireNonNull;

/// # Sequencers API
///
/// `Sequencers` provides the sequencing operator: a finite-state recognizer over a sign stream that
/// emits [Statuses.Sign] readings from structural position rather than from single-sign translation.
/// A Scorecard owns normal per-sign status translation; a Sequencer owns the status trajectory that
/// only ordered structure can prove.
///
/// The operator is deliberately small. A machine is registered as nested [SignMap] states. Each
/// state is mapped from the source API's published sign set
/// ([io.humainary.serventis.sdk.SignSet]) as a switch over the admitted sign:
///
/// ```java
/// var states = Locks.SIGNS;
///
/// Sequencers.flow (
///   states.map ( sign -> switch ( sign ) {
///     case ACQUIRE -> emit (
///       DIVERGING,
///       states.map ( next -> switch ( next ) {
///         case GRANT -> emit (
///           CONVERGING,
///           states.map ( end -> switch ( end ) {
///             case RELEASE -> emit ( STABLE );
///             default      -> null;
///           } )
///         );
///         case TIMEOUT -> emit ( DEGRADED );
///         case RELEASE -> emit ( DEFECTIVE );     // orphan closer
///         default      -> null;                   // stay active, speak nothing
///       } )
///     );
///     case RELEASE -> emit ( DEFECTIVE );
///     default      -> emit ( STABLE );            // idle baseline
///   } )
/// )
/// ```
///
/// A `null` map result means "stay in the current state and emit nothing". `emit(status)` speaks
/// and resets to the root. `emit(status, state)` speaks and moves to `state`; `status == null`
/// moves silently. This makes idle baseline, active suppression, break, re-anchor, and terminal
/// reset ordinary state-machine choices rather than hidden operator rules.
///
/// Time is not part of the recognizer. Timeout policy manufactures explicit signs upstream; the
/// machine reads those signs like any other input.
///
/// @author William David Louth
/// @since 3.0

@Utility
public final class Sequencers
  implements Serventis {

  private Sequencers () { }

  /// Creates one leaf transition.
  ///
  /// @param <S>    the sign enum type the machine reads
  /// @param status the status reading to emit, or `null` for silence
  /// @return a leaf transition that resets to the root after it is admitted

  @New
  @NotNull
  public static < S extends Enum < S > & Sign > Transition < S > emit (
    final Statuses.Sign status
  ) {

    return
      emit (
        status,
        null
      );

  }

  /// Creates one transition.
  ///
  /// @param <S>    the sign enum type the machine reads
  /// @param status the status reading to emit, or `null` for silence
  /// @param state  the next state map, or `null` to reset to the root
  /// @return a transition to `state`

  @New
  @NotNull
  public static < S extends Enum < S > & Sign > Transition < S > emit (
    final Statuses.Sign status,
    final SignMap < S, Transition < S > > state
  ) {

    return
      new Transition <> (
        status,
        state
      );

  }

  /// Returns a flow that applies the finite-state machine to a sign stream.
  ///
  /// `state` is the root state map — the initial, usually idle, state. If the current state returns
  /// `null` for a sign, the walk stays in the current state and emits nothing. If a transition has
  /// no next state, the walk resets to the root after that transition.
  ///
  /// @param <S>   the sign enum type the machine reads
  /// @param state the root state map
  /// @return a flow translating sign sequences into status trajectory readings
  /// @throws NullPointerException if `state` is `null`

  @New
  @NotNull
  public static < S extends Enum < S > & Sign > Flow < S, Statuses.Sign > flow (
    @NotNull final SignMap < S, Transition < S > > state
  ) {

    return
      flow (
        new Machine <> (
          requireNonNull ( state )
        )
      );

  }

  /// Returns a flow that recognizes the episode structure of a sign stream **directly from a domain's
  /// [Operations] bracket classification and its canonical [Statuses] map** — its `OPERATION`
  /// (`BEGIN`/`ADVANCE`/`END`) and `STATUS` maps (see `SEQUENCERS.md`). One operator produces a status
  /// trajectory for *any* domain with no hand-written machine:
  /// `Sequencers.flow ( Locks.OPERATION, Locks.STATUS )` works as well as
  /// `Sequencers.flow ( Transactions.OPERATION, Transactions.STATUS )`.
  ///
  /// `OPERATION` supplies the episode *structure* — which signs open, advance, and close a span;
  /// `STATUS` supplies the per-sign *quality*, the canonical health reading every domain already
  /// publishes. The trajectory:
  ///
  /// - a [Operations.Sign#BEGIN] opens a span and reads [Statuses.Sign#DIVERGING]; opening one while a
  ///   span is already open reads [Statuses.Sign#DEFECTIVE] (the prior episode never closed) and
  ///   re-opens;
  /// - an [Operations.Sign#ADVANCE] of an open span mirrors the sign's `STATUS` —
  ///   [Statuses.Sign#CONVERGING] for a healthy step, [Statuses.Sign#DEGRADED] or
  ///   [Statuses.Sign#DEFECTIVE] for an unhealthy one, silent for an abstaining one — and leaves the
  ///   span open; an `ADVANCE` with nothing open is silent (no positive reading without a span);
  /// - an [Operations.Sign#END] of an open span reads the closing sign's `STATUS` —
  ///   [Statuses.Sign#STABLE] for a healthy or abstaining close, [Statuses.Sign#DEGRADED] or
  ///   [Statuses.Sign#DEFECTIVE] for an unhealthy one — and pops the span; an `END` with nothing open
  ///   is an orphan [Statuses.Sign#DEFECTIVE].
  ///
  /// Reading the *canonical* `STATUS` of the closing sign (rather than a coarser success/fail verdict)
  /// lets a forced close — a lease `REVOKE` (`DEFECTIVE`), a process `KILL` (`DEGRADED`), a transaction
  /// `CONFLICT` (`DEGRADED`) — surface its true severity instead of collapsing to a false `STABLE`.
  ///
  /// **Contract on `status`.** Pass a canonical per-sign `STATUS` map: it reads the health *lattice*
  /// subset — [Statuses.Sign#STABLE], [Statuses.Sign#DEGRADED], [Statuses.Sign#DEFECTIVE], or `null`
  /// (abstains). The operator never echoes the input verbatim — it emits a *trajectory* reading. The
  /// remaining [Statuses.Sign] values are not per-sign health readings and are not expected here, but a
  /// non-canonical map that supplies one is folded by **severity** and never masked as healthy:
  /// [Statuses.Sign#ERRATIC] and [Statuses.Sign#DIVERGING] read like `DEGRADED`, [Statuses.Sign#DOWN]
  /// like `DEFECTIVE`, [Statuses.Sign#CONVERGING] like `STABLE`. (A severe `DOWN` close therefore reads
  /// `DEFECTIVE`, not a masked `STABLE`.)
  ///
  /// @param <S>       the sign enum type the machine reads
  /// @param operation the domain's `OPERATION` bracket classification (sign → [Operations.Sign], total)
  /// @param status    the domain's canonical `STATUS` map (sign → [Statuses.Sign] health lattice, partial)
  /// @return a flow translating the stream's episode structure into status trajectory readings
  /// @throws NullPointerException if either map is `null`

  @New
  @NotNull
  public static < S extends Enum < S > & Sign > Flow < S, Statuses.Sign > flow (
    @NotNull final SignMap < S, Operations.Sign > operation,
    @NotNull final SignMap < S, Statuses.Sign > status
  ) {

    requireNonNull ( operation );
    requireNonNull ( status );

    return
      cortex (). < S > flow ()
        .scan (
          Bracket::new,
          ( bracket, sign ) -> {
            bracket.admit (
              operation.apply ( sign ),
              status.apply ( sign )
            );
            return bracket;
          },
          bracket -> bracket.out
        );

  }

  private static < S extends Enum < S > & Sign > Flow < S, Statuses.Sign > flow (
    final Machine < S > machine
  ) {

    return
      cortex (). < S > flow ()
        .scan (
          () -> new Walk <> ( machine ),
          ( walk, sign ) -> {
            walk.admit ( sign );
            return walk;
          },
          walk -> walk.out
        );

  }

  /// The per-subject bracket walk for [#flow(SignMap, SignMap)]. Holds only whether a span is open; one
  /// instance per flow attachment, touched only on the circuit thread. Each sign arrives already
  /// resolved into its [Operations] bracket position and its canonical [Statuses] reading, so the walk
  /// is domain-agnostic. Only `BEGIN` and `END` move the span; an `ADVANCE` mirrors the step's status
  /// but never closes — the `OPERATION` map already drew the line between mid-span and terminal signs.
  ///
  /// `status` is the closing/advancing sign's *own* health reading (`STABLE`/`DEGRADED`/`DEFECTIVE`, or
  /// `null` when it abstains); `out` is the *trajectory* reading the sequencer speaks downstream.

  private static final class Bracket {

    private boolean       inSpan;
    private Statuses.Sign out;

    /// The mid-span reading for an advancing step's canonical `STATUS`: `null` (abstaining) is silent;
    /// otherwise by **severity**, so a severe status (`DOWN`/`DEFECTIVE`) is surfaced, never masked as
    /// healthy. Exhaustive over [Statuses.Sign] — a new status forces a decision here.

    private static Statuses.Sign advancing (
      final Statuses.Sign status
    ) {

      return
        status == null
        ? null
        : switch ( status ) {
          case STABLE, CONVERGING -> CONVERGING;
          case DEGRADED, ERRATIC, DIVERGING -> DEGRADED;
          case DEFECTIVE, DOWN -> DEFECTIVE;
        };

    }

    /// The closing reading for a closing sign's canonical `STATUS`: `null` (abstaining) or a healthy
    /// status is a clean `STABLE` close; otherwise by **severity**, so a severe status is surfaced,
    /// never masked. Exhaustive over [Statuses.Sign].

    private static Statuses.Sign closing (
      final Statuses.Sign status
    ) {

      return
        status == null
        ? STABLE
        : switch ( status ) {
          case STABLE, CONVERGING -> STABLE;
          case DEGRADED, ERRATIC, DIVERGING -> DEGRADED;
          case DEFECTIVE, DOWN -> DEFECTIVE;
        };

    }

    private void admit (
      final Operations.Sign operation,
      final Statuses.Sign status
    ) {

      switch ( operation ) {

        case BEGIN -> {
          out = inSpan ? DEFECTIVE : DIVERGING;   // overlapping open ⇒ prior span never closed
          inSpan = true;
        }

        case ADVANCE -> out =
          inSpan ? advancing ( status ) : null;      // no reading without an open span

        case END -> {
          if ( inSpan ) {
            inSpan = false;
            out = closing ( status );
          } else {
            out = DEFECTIVE;                         // orphan close
          }
        }

      }

    }

  }

  /// The finite-state machine. The root map is the initial/idle state.

  private record Machine < S extends Enum < S > & Sign >( SignMap < S, Transition < S > > root ) {

  }

  /// One sequencer transition — the optional status reading to speak plus the optional next state
  /// to move to. Opaque: created only by the [#emit(Statuses.Sign)] and
  /// [#emit(Statuses.Sign, SignMap)] factories; its components are read only by the operator.
  ///
  /// A `null` status means the transition moves silently. A `null` state means the transition is
  /// a leaf and the walk resets to the root after the transition.
  ///
  /// @param <S> the sign enum type the machine reads

  @Provided
  @Immutable
  public static final class Transition <
    S extends Enum < S > & Sign
    > {

    private final Statuses.Sign                   status;
    private final SignMap < S, Transition < S > > state;

    private Transition (
      final Statuses.Sign status,
      final SignMap < S, Transition < S > > state
    ) {

      this.status = status;
      this.state = state;

    }

  }

  /// The per-subject walk over the shared machine. One instance is created per flow attachment and
  /// is only ever touched on the circuit thread.

  private static final class Walk < S extends Enum < S > & Sign > {

    private final Machine < S > machine;

    private SignMap < S, Transition < S > > state;
    private Statuses.Sign                   out;

    private Walk (
      final Machine < S > machine
    ) {

      this.machine = machine;
      state = machine.root;

    }

    /// Admits one sign by applying the current state map.

    private void admit (
      final S sign
    ) {

      final var transition =
        state.apply ( sign );

      if ( transition == null ) {
        out = null;
        return;
      }

      state =
        transition.state == null
        ? machine.root
        : transition.state;

      out = transition.status;

    }

  }

}
