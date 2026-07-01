// Copyright (c) 2025 William David Louth

package io.humainary.serventis.sdk;

import io.humainary.serventis.api.Serventis.Symbol;
import io.humainary.substrates.api.Substrates.Immutable;
import io.humainary.substrates.api.Substrates.New;
import io.humainary.substrates.api.Substrates.NotNull;

import static java.util.Objects.requireNonNull;

/// The [SymbolSet] class reifies a source symbol set — the captured constants of one symbol enum,
/// either a [io.humainary.serventis.api.Serventis.Sign] or a
/// [io.humainary.serventis.api.Serventis.Dimension] — as a first-class, ordinal-indexed value.
///
/// It is the genus shared by the two axes of a signal: a domain publishes its sign vocabulary as a
/// [SignSet] (`SIGNS`) and, where it is two-dimensional, its dimension vocabulary as a `SymbolSet`
/// (`DIMENSIONS`). The set captures the enum constants once at construction and exposes only their
/// cardinality. The sign-specific operations — translation ([SignSet#map]) and `Sign × Dimension`
/// construction ([SignSet#signals]) — live on [SignSet], the sign-axis sibling, since the ascent
/// and the signal product are operations of the sign axis, not the dimension axis.
///
/// ## Immutability and Thread Safety
///
/// A symbol set holds only the enum constants captured at construction and is immutable; it may be
/// shared freely across threads and circuits without synchronization.
///
/// @param <X> the source Symbol enum type (a Sign or a Dimension)
/// @author William David Louth
/// @since 3.0

@Immutable
public final class SymbolSet <
  X extends Enum < X > & Symbol
  > {

  final X[] symbols;

  SymbolSet (
    final Class < X > source
  ) {

    this.symbols =
      source.getEnumConstants ();

  }

  /// Captures the symbol set of the given symbol enum — a sign or a dimension.
  ///
  /// @param <X>    the source Symbol enum type
  /// @param source the class object for the source symbol enum
  /// @return the symbol set of `source`
  /// @throws NullPointerException if `source` is `null`

  @New
  @NotNull
  public static < X extends Enum < X > & Symbol > SymbolSet < X > of (
    @NotNull final Class < X > source
  ) {

    return
      new SymbolSet <> (
        requireNonNull (
          source
        )
      );

  }

  /// The number of symbols in this set — its cardinality.
  ///
  /// @return the count of symbol constants captured in this set

  public int size () {

    return
      symbols.length;

  }

}
