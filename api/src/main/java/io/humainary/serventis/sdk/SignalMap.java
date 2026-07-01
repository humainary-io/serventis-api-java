// Copyright (c) 2025 William David Louth

package io.humainary.serventis.sdk;

import io.humainary.serventis.api.Serventis.Dimension;
import io.humainary.serventis.api.Serventis.Sign;
import io.humainary.serventis.api.Serventis.Signal;
import io.humainary.substrates.api.Substrates.Immutable;
import io.humainary.substrates.api.Substrates.NotNull;

import java.util.function.Function;

/// The [SignalMap] class is a pre-computed, ordinal-indexed projection from a source
/// `Sign × Dimension` signal space to arbitrary values.
///
/// A signal map is produced by mapping a projection function over a [SignalSet]. The function is
/// invoked once per pre-allocated signal in that set; subsequent lookups are a single flat-array
/// load indexed by `sign.ordinal() * dimensions + dimension.ordinal()`.
///
/// `SignalMap` is the signal-level sibling of [SignMap]: where [SignMap] keys on the sign alone,
/// `SignalMap` keys on the full `Sign × Dimension` signal. Reach for it for any value keyed by
/// signal that would otherwise live in a `HashMap` — the ordinal-indexed array makes each lookup a
/// single load with no hashing, boxing, or `equals`. Like `SignMap`, `null` is a valid cached
/// result and conventionally means "no mapping" (untranslated / abstain).
///
/// ## Type Parameters
///
/// @param <S> the source Sign enum type
/// @param <D> the source Dimension enum type
/// @param <E> the source Signal record type
/// @param <T> the projected value type
/// @author William David Louth
/// @since 3.0

@Immutable
public final class SignalMap <
  S extends Enum < S > & Sign,
  D extends Enum < D > & Dimension,
  E extends Record & Signal < S, D >,
  T
  > implements Function < E, T > {

  private final int columns;
  private final T[] map;

  SignalMap (
    final int columns,
    @NotNull final T[] map
  ) {

    this.columns =
      columns;

    this.map =
      map;

  }

  /// Returns the target value mapped from the given signal, or `null` if the signal was mapped to
  /// `null` (untranslated / abstain).
  ///
  /// @param signal the source signal
  /// @return the pre-computed target value, or `null` if untranslated

  @Override
  public T apply (
    @NotNull final E signal
  ) {

    return
      get (
        signal.sign (),
        signal.dimension ()
      );

  }

  /// Returns the target value mapped from the given sign/dimension pair, or `null` if that signal
  /// was mapped to `null` (untranslated / abstain).
  ///
  /// This is an O(1) array load indexed by the pair's ordinals, with no allocation.
  ///
  /// @param sign      the source sign
  /// @param dimension the source dimension
  /// @return the pre-computed target value, or `null` if untranslated

  public T get (
    @NotNull final S sign,
    @NotNull final D dimension
  ) {

    return
      map[sign.ordinal () * columns + dimension.ordinal ()];

  }

}
