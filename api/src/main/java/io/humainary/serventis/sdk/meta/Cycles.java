// Copyright (c) 2025 William David Louth

package io.humainary.serventis.sdk.meta;

import io.humainary.serventis.api.Serventis;
import io.humainary.serventis.sdk.SignSet;
import io.humainary.serventis.sdk.SignalSet;
import io.humainary.serventis.sdk.SymbolSet;
import io.humainary.substrates.api.Substrates.Utility;

import static io.humainary.substrates.api.Substrates.cortex;
import static java.util.Objects.requireNonNull;

/// # Cycles API
///
/// The `Cycles` API provides a structured framework for expressing observations about
/// the repetition patterns of signs within a signal stream. It enables systems to emit
/// semantic signals that describe whether a sign is appearing for the first time,
/// repeating consecutively, or returning after an absence.
///
/// ## Purpose
///
/// This API provides vocabulary for **meta-level observation** of sign streams.
/// Unlike domain APIs that emit raw operations/outcomes, Cycles expresses analytical
/// observations about patterns in those streams — and it ships its **canonical detector** for
/// computing them.
///
/// ## Two ways to use it
///
/// - **Detect** — attach [#flow] to a sign stream, and it emits the SINGLE/REPEAT/RETURN observation
///   per admission, tracking the per-subject history for you. Cycle detection is fully determined,
///   so the flow needs no policy beyond the sign class.
/// - **Express** — if a bespoke analyzer already computed the pattern, use the [Cycle] instrument
///   ([#of] / [#pool]) to emit observations directly. The API is then just the vocabulary.
///
/// ## Key Concepts
///
/// - **Cycle**: An instrument that emits cycle observations for signs from a source API
/// - **Signal**: A pairing of any sign type with a cycle dimension
/// - **Dimension**: The repetition characteristic (SINGLE, REPEAT, RETURN)
///
/// ## Generic Over Sign Types
///
/// Unlike domain APIs that define their own signs, Cycles is **generic over any Sign type**.
/// The Sign comes from the source API being observed (Resources, Tasks, Gauges, etc.).
/// Cycles defines only the Dimension, which describes the repetition pattern.
///
/// ## Dimensions
///
/// | Dimension | Meaning                                      |
/// |-----------|----------------------------------------------|
/// | `SINGLE`  | First occurrence of this sign in the stream  |
/// | `REPEAT`  | Same sign as immediately previous emission   |
/// | `RETURN`  | Seen before, but not immediately previous    |
///
/// ## Usage Example
///
/// ```java
/// // Detect: attach the flow to a Resources.Sign stream; it emits the cycle observation per sign.
/// // Signal.class is a raw literal (no Signal<Resources.Sign>.class in Java), so the parameterised
/// // conduit type requires an unchecked cast — the same one the TCK uses.
/// @SuppressWarnings ( "unchecked" )
/// Conduit < Signal < Resources.Sign > > cycles =
///   (Conduit < Signal < Resources.Sign > >) (Conduit < ? >) circuit.conduit ( Signal.class );
///
/// Pipe < Resources.Sign > in =
///   cycles.pool ( Cycles.flow ( Resources.SIGNS ) )
///         .get ( cortex.name ( "db.pool.cycles" ) );
///
/// // Stream: GRANT, GRANT, DENY, GRANT
/// in.emit ( GRANT );   // (GRANT, SINGLE)
/// in.emit ( GRANT );   // (GRANT, REPEAT)
/// in.emit ( DENY );    // (DENY,  SINGLE)
/// in.emit ( GRANT );   // (GRANT, RETURN)
/// ```
///
/// ## Relationship to Other APIs
///
/// Cycles is a **meta-level** API in the `sdk/meta` package:
///
/// - **Domain APIs** (opt/*): Emit raw signs (GRANT, DENY, INCREMENT, etc.)
/// - **Meta APIs** (sdk/meta): Express observations about sign patterns
/// - **Universal APIs** (sdk): Translation targets (Statuses, Situations, Systems)
///
/// The semiotic flow:
/// ```
/// Domain signs → Meta observations (Cycles) → Universal vocabularies → Actions
/// ```
///
/// ## Performance Considerations
///
/// Cycle signals are pre-allocated using SignalSet for zero-allocation emission. The SignalSet is
/// constructed with the source Sign class, pre-computing all Sign × Dimension combinations. [#flow]
/// adds only a fixed-size per-subject cursor (a `seen` flag per sign ordinal plus the previous
/// ordinal), so the detector contributes no per-emission allocation of its own; each emission still
/// carries the substrate's standard per-dispatch cost (≈32 B/op, common to every emitting flow).
///
/// @author William David Louth
/// @since 1.0

@Utility
public final class Cycles
  implements Serventis {

  /// The pre-captured dimension set of [Dimension] — the repetition axis (SINGLE/REPEAT/RETURN)
  /// crossed with any source sign vocabulary to build a Cycle's signal table.

  public static final SymbolSet < Dimension > DIMENSIONS =
    SymbolSet.of (
      Dimension.class
    );

  private Cycles () { }

  /// Returns a flow that performs canonical cycle detection over a sign stream, emitting one
  /// [Signal] per admission tagged with its repetition [Dimension] (SINGLE, REPEAT, or RETURN).
  ///
  /// This is the **detector** counterpart to the [Cycle] instrument: rather than expressing an
  /// observation a bespoke analyzer computed, the flow tracks the per-subject history and computes
  /// the pattern itself. Detection is fully determined, so no policy is required beyond the sign
  /// class. Attach it to a [Signal] conduit via [Conduit#pool(Flow)]; each named percept maintains
  /// its own cursor (a `seen` flag per sign plus the previous sign), and the emitted signals are
  /// drawn from a shared, pre-allocated SignalSet, so the detector adds no allocation of its own.
  ///
  /// ```java
  /// cycles.pool ( Cycles.flow ( Resources.SIGNS ) );
  /// ```
  ///
  /// The recipe is built from the singleton [io.humainary.substrates.api.Substrates#cortex()]; a
  /// flow is a standalone, circuit-agnostic value (materializable against any pipe of that cortex),
  /// so no cortex need be threaded in.
  ///
  /// @param <S>   the Sign enum type from the source API
  /// @param signs the sign set of the source API (a domain's published `SIGNS`)
  /// @return a flow translating signs into per-admission cycle observations
  /// @throws NullPointerException if `signs` is `null`

  @SuppressWarnings ( "unchecked" )
  @New
  @NotNull
  public static < S extends Enum < S > & Sign > Flow < S, Signal < S > > flow (
    @NotNull final SignSet < S > signs
  ) {

    requireNonNull ( signs );

    final var signals =
      signs.signals (
        DIMENSIONS,
        Signal::new
      );

    final var count =
      signs.size ();

    return
      cortex (). < S > flow ()
        .scan (
          () -> new Cursor ( count ),
          ( cursor, sign ) -> {
            cursor.observe ( sign.ordinal () );
            return cursor;
          },
          ( cursor, sign ) ->
            signals.get (
              sign,
              cursor.dimension
            )
        );

  }

  /// Creates a Cycle instrument wrapping the specified pipe.
  ///
  /// The sign set is required to enable SignalSet pre-allocation of all
  /// Sign x Dimension combinations.
  ///
  /// @param <S>   the Sign enum type from the source API
  /// @param signs the sign set of the source API (a domain's published `SIGNS`)
  /// @param pipe  the pipe from which to create the cycle
  /// @return a new Cycle instrument for the specified pipe
  /// @throws NullPointerException if signs or pipe is `null`

  @New
  @NotNull
  public static < S extends Enum < S > & Sign > Cycle < S > of (
    @NotNull final SignSet < S > signs,
    @NotNull final Pipe < ? super Signal < S > > pipe
  ) {

    return
      new Cycle <> (
        signs,
        pipe
      );

  }

  /// Returns a pool that creates cached Cycle instruments from a conduit.
  ///
  /// @param <S>     the Sign enum type from the source API
  /// @param signs   the sign set of the source API (a domain's published `SIGNS`)
  /// @param conduit the conduit providing signal pipes
  /// @return a pool that creates Cycle instruments
  /// @throws NullPointerException if signs or conduit is `null`

  @New
  @NotNull
  public static < S extends Enum < S > & Sign > Pool < Cycle < S > > pool (
    @NotNull final SignSet < S > signs,
    @NotNull final Conduit < Signal < S > > conduit
  ) {

    return
      conduit.pool (
        pipe ->
          new Cycle <> (
            signs,
            pipe
          )
      );

  }

  /// The [Dimension] enum represents the repetition characteristic of a sign
  /// within a signal stream.
  ///
  /// Dimensions describe the temporal relationship of a sign occurrence to
  /// previous occurrences of the same sign in the stream.

  public enum Dimension
    implements Serventis.Category {

    /// First occurrence of this sign in the stream.
    ///
    /// SINGLE indicates this sign has not been seen before in the observation
    /// window. It represents a novel appearance of this particular sign value.

    SINGLE,

    /// Same sign as immediately previous emission.
    ///
    /// REPEAT indicates consecutive occurrence - this sign was also the most
    /// recent emission. Back-to-back repetition of the same sign.

    REPEAT,

    /// Seen before, but not immediately previous.
    ///
    /// RETURN indicates the sign has appeared previously in the stream, but
    /// other signs occurred between then and now. The sign has cycled back
    /// after an absence.

    RETURN

  }

  /// Per-subject cycle-detection state for [#flow]: a `seen` flag per sign ordinal and the ordinal
  /// of the immediately previous sign. Created fresh per flow attachment and only ever touched on
  /// the circuit thread.

  private static final class Cursor {

    private final boolean[] seen;
    private       int       prev = -1;
    private       Dimension dimension;

    private Cursor (
      final int count
    ) {

      this.seen = new boolean[count];

    }

    /// Classifies the sign at `ordinal` against the history, advances the history, and records the
    /// resulting dimension for the projection to read.

    private void observe (
      final int ordinal
    ) {

      if ( !seen[ordinal] ) {
        seen[ordinal] = true;
        dimension = Dimension.SINGLE;
      } else if ( ordinal == prev ) {
        dimension = Dimension.REPEAT;
      } else {
        dimension = Dimension.RETURN;
      }

      prev = ordinal;

    }

  }

  /// The [Cycle] class emits cycle observations for signs from a source API.
  ///
  /// A Cycle instrument is generic over the Sign type of the source API being
  /// observed. It provides vocabulary for expressing whether signs are appearing
  /// for the first time, repeating consecutively, or returning after absence.
  ///
  /// ## Usage
  ///
  /// ```java
  /// cycle.signal(Resources.Sign.GRANT, Dimension.SINGLE);
  /// cycle.signal(Resources.Sign.GRANT, Dimension.REPEAT);
  /// cycle.signal(Resources.Sign.DENY, Dimension.SINGLE);
  /// cycle.signal(Resources.Sign.GRANT, Dimension.RETURN);
  /// ```

  @Queued
  @Provided
  public static final class Cycle < S extends Enum < S > & Sign >
    implements Signaler < S, Dimension > {

    private final SignalSet < S, Dimension, Signal < S > > signals;
    private final Pipe < ? super Signal < S > >            pipe;

    private Cycle (
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

    /// Emits a cycle observation for the specified sign and dimension.
    ///
    /// @param sign      the sign from the source API
    /// @param dimension the repetition characteristic
    /// @throws NullPointerException if sign or dimension is `null`

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

  /// The [Signal] record represents a cycle observation for any sign type.
  ///
  /// Unlike domain API signals that pair domain-specific signs with dimensions,
  /// Cycle signals are generic over the sign type. The sign comes from whatever
  /// source API is being observed; the dimension comes from this API.
  ///
  /// @param <S>       the Sign type from the source API
  /// @param sign      the observed sign from the source API
  /// @param dimension the repetition characteristic

  @Provided
  @Immutable
  public record Signal < S extends Sign >(
    @NotNull S sign,
    @NotNull Dimension dimension
  ) implements Serventis.Signal < S, Dimension > { }

}
