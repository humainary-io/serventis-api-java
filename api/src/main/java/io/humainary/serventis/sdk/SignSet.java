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

/// The [SignSet] class reifies a source sign set — the captured constants of one sign enum — as a
/// first-class value from which interpretations are mapped.
///
/// A sign set is the central concept of the semiotic ascent (see `SERVENTIS.md`): a
/// translation-capable language whose power lies in its capacity to project into other sign sets.
/// `SignSet` is that capacity made executable: [#map(Function)] applies a translation function
/// exactly once per sign constant — never per lookup — and materializes the result as an
/// ordinal-indexed [SignMap]. It is the construction path for every sign map; `SignMap` itself is
/// the immutable projection value a sign set produces.
///
/// Every Serventis API that defines a sign enum publishes its sign set as a `SIGNS` constant —
/// alongside the canonical interpretations (`STATUS`, `KIND`) where the domain ships them. A
/// third-party sign enum is captured directly with [#of(Class)]:
///
/// ```java
/// public static final SignSet < Sign > SIGNS =
///   SignSet.of ( Sign.class );
///
/// public static final SignMap < Sign, Statuses.Sign > STATUS =
///   SIGNS.map ( sign -> switch ( sign ) { ... } );
///
/// public static final SignMap < Sign, Kind > KIND =
///   SIGNS.map ( sign -> switch ( sign ) { ... } );
/// ```
///
/// Callers derive their own interpretations from the same published set — a scorecard ballot, a
/// sequencer state — without repeating the sign class:
///
/// ```java
/// var ballot =
///   Resources.SIGNS.map (
///     sign -> switch ( sign ) {
///       case GRANT          -> Statuses.Sign.STABLE;
///       case DENY, TIMEOUT  -> Statuses.Sign.DEGRADED;
///       default             -> null;          // abstain
///     }
///   );
/// ```
///
/// It is the source-side sibling of [SignalSet]: where `SignalSet` pre-allocates the composite
/// `Sign × Dimension` signal space, `SignSet` captures the atomic sign space and maps it.
///
/// ## Immutability and Thread Safety
///
/// A sign set holds only the enum constants captured at construction and is immutable; it may be
/// shared freely across threads and circuits without synchronization. The maps it produces carry
/// the same guarantee.
///
/// @param <S> the source Sign enum type
/// @author William David Louth
/// @since 3.0

@Immutable
public final class SignSet <
  S extends Enum < S > & Sign
  > {

  private final SymbolSet < S > signs;

  private SignSet (
    final Class < S > source
  ) {

    this.signs =
      SymbolSet.of ( source );

  }

  /// Captures the sign set of the given sign enum.
  ///
  /// @param <S>    the source Sign enum type
  /// @param source the class object for the source sign enum
  /// @return the sign set of `source`
  /// @throws NullPointerException if `source` is `null`

  @New
  @NotNull
  public static < S extends Enum < S > & Sign > SignSet < S > of (
    @NotNull final Class < S > source
  ) {

    return
      new SignSet <> (
        requireNonNull (
          source
        )
      );

  }

  /// Creates a sign map by applying `fn` to every constant of this sign set.
  ///
  /// The function is invoked exactly once per constant here, at map creation; thereafter the
  /// returned map's lookup is a single ordinal-indexed array load.
  ///
  /// @param <T> the projected value type (commonly a target Sign, but unbounded)
  /// @param fn  the translation function; may return `null` to mark a sign as untranslated
  /// @return a new immutable sign map
  /// @throws NullPointerException if `fn` is `null`

  @New
  @NotNull
  @SuppressWarnings ( "unchecked" )
  public < T > SignMap < S, T > map (
    @NotNull final Function < ? super S, ? extends T > fn
  ) {

    requireNonNull ( fn );

    final var constants =
      signs.symbols;

    final var values =
      new Object[constants.length];

    for ( final var sign : constants ) {

      values[sign.ordinal ()] =
        fn.apply ( sign );

    }

    return
      new SignMap <> (
        (T[]) values
      );

  }

  /// Creates a signal set: the `Sign × Dimension` product of this sign set with `dims`, each cell
  /// pre-allocated by `factory`. The composite counterpart to [#map(Function)] — where `map`
  /// projects the sign axis alone into a [SignMap], `signals` crosses it with a dimension axis into
  /// the full [SignalSet] signal space. Both axes are captured [SymbolSet]s, so the signal space
  /// shares the exact ordering the sign maps from this set use — and the product is built with no
  /// reflection.
  ///
  /// @param <D>     the Dimension enum type
  /// @param <T>     the Signal record type, bound to this set's sign type and `D`
  /// @param dims    the dimension set (a domain's published `DIMENSIONS`)
  /// @param factory creates a signal instance from a sign and a dimension
  /// @return a new pre-allocated signal set over this set's signs and the given dimensions
  /// @throws NullPointerException if `dims` or `factory` is `null`

  @New
  @NotNull
  public < D extends Enum < D > & Dimension, T extends Record & Signal < S, D > >
  SignalSet < S, D, T > signals (
    @NotNull final SymbolSet < D > dims,
    @NotNull final BiFunction < ? super S, ? super D, ? extends T > factory
  ) {

    return
      new SignalSet <> (
        signs.symbols,
        requireNonNull ( dims ).symbols,
        factory
      );

  }

  /// The number of signs in this set — its cardinality.
  ///
  /// @return the count of sign constants captured in this set

  public int size () {

    return
      signs.size ();

  }

}
