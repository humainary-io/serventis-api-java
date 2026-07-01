// Copyright (c) 2025 William David Louth

package io.humainary.serventis.sdk;

import io.humainary.serventis.api.Serventis.Dimension;
import io.humainary.serventis.api.Serventis.Sign;
import io.humainary.serventis.api.Serventis.Signal;
import io.humainary.substrates.api.Substrates.Immutable;
import io.humainary.substrates.api.Substrates.New;
import io.humainary.substrates.api.Substrates.NotNull;

import java.util.function.BiFunction;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/// The [SignalSet] class provides a pre-allocated lookup table for all Signal combinations
/// in a Sign × Dimension Cartesian product.
///
/// This internal utility eliminates boilerplate initialization code by providing a generic,
/// reusable pattern for creating and accessing the complete signal set. All possible
/// signal combinations are constructed once during initialization, ensuring zero runtime
/// allocation and optimal lookup performance.
///
/// ## Usage Example
///
/// A signal set is built from two captured [SymbolSet]s — a domain's `SIGNS` (a [SignSet]) crossed
/// with its `DIMENSIONS` — via [SignSet#signals(SymbolSet, BiFunction)]:
///
/// ```java
/// private static final SignalSet<Sign, Dimension, Signal> SIGNALS =
///   SIGNS.signals ( DIMENSIONS, Signal::new );
///
/// public void connect() {
///   pipe.emit ( SIGNALS.get ( Sign.CONNECT, OUTBOUND ) );
/// }
/// ```
///
/// Routing construction through [SignSet]/[SymbolSet] keeps the signal space's axes identical to
/// the ones every [SignMap] (and any dimension lookup) from those sets use — one capture per axis,
/// no drift, no reflection. The factory `BiFunction<S,D,T>` ties the sign, dimension, and signal
/// types, so the compiler checks their consistency. A sign type known only at runtime (a generic
/// signal record) is captured first with [SignSet#of(Class)]:
/// `SignSet.of(signClass).signals(DIMENSIONS, Signal::new)`.
///
/// ## Type Parameters
///
/// @param <S> the Sign enum type implementing Serventis.Sign
/// @param <D> the Dimension enum type implementing Serventis.Dimension
/// @param <T> the Signal record type implementing Serventis.Signal
/// @author William David Louth
/// @since 1.0

@Immutable
public final class SignalSet <
  S extends Enum < S > & Sign,
  D extends Enum < D > & Dimension,
  T extends Record & Signal < S, D >
  > {

  private final int columns;
  private final T[] signals;

  /// Constructs a signal set over the given sign and dimension constants, each `Sign × Dimension`
  /// cell pre-allocated by `factory`. Package-private: the construction path is
  /// [SignSet#signals(SymbolSet, BiFunction)], so both axes are captured [SymbolSet]s — the same
  /// reified constants a domain's `SIGNS` and `DIMENSIONS` publish — and no reflection is needed.
  ///
  /// @param signs      the captured sign constants (the sign axis)
  /// @param dimensions the captured dimension constants (the dimension axis)
  /// @param factory    a function that creates signal instances from sign and dimension

  @SuppressWarnings ( {"unchecked"} )
  SignalSet (
    @NotNull final S[] signs,
    @NotNull final D[] dimensions,
    @NotNull final BiFunction < ? super S, ? super D, ? extends T > factory
  ) {

    requireNonNull ( factory );

    this.columns =
      dimensions.length;

    this.signals =
      (T[]) ( new Record[signs.length * columns] );

    for ( final var sign : signs ) {

      final var offset =
        sign.ordinal () * columns;

      for ( final var dimension : dimensions ) {

        signals[offset + dimension.ordinal ()] =
          factory.apply (
            sign,
            dimension
          );

      }

    }

  }

  /// Retrieves the pre-allocated signal for the given sign and dimension.
  ///
  /// This method provides O(1) lookup with no allocation overhead, as all signals
  /// are constructed during set initialization.
  ///
  /// @param sign      the sign component
  /// @param dimension the dimension component
  /// @return the pre-allocated signal for this sign/dimension combination

  public T get (
    @NotNull final S sign,
    @NotNull final D dimension
  ) {

    return
      signals[sign.ordinal () * columns + dimension.ordinal ()];

  }

  /// Creates a signal map by applying `fn` to every pre-allocated signal in this set.
  ///
  /// The function is invoked exactly once per `Sign × Dimension` pair here, at map creation;
  /// thereafter the returned map resolves either a full signal or a sign/dimension pair as a
  /// single ordinal-indexed array load.
  ///
  /// @param <R> the projected value type
  /// @param fn  the projection function; may return `null` to mark a signal as untranslated
  /// @return a new immutable signal map
  /// @throws NullPointerException if `fn` is `null`

  @New
  @NotNull
  @SuppressWarnings ( "unchecked" )
  public < R > SignalMap < S, D, T, R > map (
    @NotNull final Function < ? super T, ? extends R > fn
  ) {

    requireNonNull ( fn );

    final var values =
      new Object[signals.length];

    for (
      var i = 0;
      i < signals.length;
      i++
    ) {

      values[i] =
        fn.apply (
          signals[i]
        );

    }

    return
      new SignalMap <> (
        columns,
        (R[]) values
      );

  }

}
