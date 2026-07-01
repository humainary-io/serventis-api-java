// Copyright (c) 2025 William David Louth

package io.humainary.serventis.sdk;

import io.humainary.serventis.api.Serventis.Sign;
import io.humainary.substrates.api.Substrates.Immutable;
import io.humainary.substrates.api.Substrates.NotNull;

import java.util.function.Function;

/// The [SignMap] class is a pre-computed, ordinal-indexed projection from a source sign set to
/// arbitrary values — an immutable array wrapper whose [#apply(Enum)] is a single array load
/// indexed by `source.ordinal()`.
///
/// A sign map is produced by mapping a translation function over a [SignSet] — the captured
/// source sign set is the only construction path. The value is commonly another sign (the
/// headline sign-to-sign translation), but `T` is unbounded, so it may equally be a weighted
/// vote, a transition, or any other per-sign metadata.
///
/// ## Translation primitive
///
/// Sign-to-sign translation is the foundational move of the semiotic ascent (see `SERVENTIS.md`):
/// a domain sign set is interpreted into another sign set. `SignMap` makes that interpretation a
/// first-class, reusable value. Because it implements `Function < S, T >`, it is a drop-in
/// wherever a sign-mapping function is expected — for example as the ballot of a
/// [Scorecards#flow(Function)]:
///
/// ```java
/// var ballot =
///   Resources.SIGNS.map (
///     sign -> switch ( sign ) {
///       case GRANT          -> Statuses.Sign.STABLE;
///       case DENY, TIMEOUT  -> Statuses.Sign.DEGRADED;
///       default             -> null;          // abstain — see below
///     }
///   );
///
/// statuses.pool ( Scorecards.flow ( ballot ) );
/// ```
///
/// ## Abstention
///
/// The mapping function may return `null` for a source sign that has no target. That `null` is
/// stored as the cached result, so [#apply(Enum)] returns `null` for those signs — the convention
/// shared with Flow operators and the Scorecards ballot, where `null` means "no translation".
///
/// ## Defaults and overrides
///
/// A published `SignMap` — such as a domain's `STATUS` map — is a *default* reading: the immediate
/// interpretant of a sign set's upward ascent. Because it is an ordinary `Function`, a caller
/// specializes it for their own context by composing a function around it, in either of two forms:
///
/// ```java
/// // 1. Inline ballot — override a few signs, delegate the rest to the default:
/// Scorecards.flow (
///   s -> s == TIMEOUT ? Statuses.Sign.DEFECTIVE : Resources.STATUS.apply ( s )
/// );
///
/// // 2. Derived SignMap — re-map the sign set once, keeping the O(1) array load:
/// Resources.SIGNS.map (
///   s -> s == TIMEOUT ? Statuses.Sign.DEFECTIVE : Resources.STATUS.apply ( s )
/// );
/// ```
///
/// Re-mapping over the sign set runs the composition once at construction, so the override is
/// itself a single ordinal-indexed array load per emission rather than a per-emission branch.
///
/// Prefer this delegating form over [java.util.function.Function#andThen]: a default that abstains
/// returns `null`, and `andThen(g)` would invoke `g(null)`. Delegating to the default inside the
/// ballot lets abstention flow through untouched.
///
/// ## Immutability and Thread Safety
///
/// All entries are computed before the map is created and never mutated afterward, so a `SignMap`
/// is immutable and may be shared freely across threads and circuits without synchronization. This
/// is deliberately stronger than a lazily-memoized cache: it does not rely on the mapping function
/// being idempotent, nor on the caller confining lookups to a single thread.
///
/// ## Type Parameters
///
/// @param <S> the source Sign enum type being translated from
/// @param <T> the projected value type (commonly a target Sign, but unbounded)
/// @author William David Louth
/// @since 3.0

@Immutable
public final class SignMap <
  S extends Enum < S > & Sign,
  T
  > implements Function < S, T > {

  private final T[] map;

  SignMap (
    @NotNull final T[] map
  ) {

    this.map = map;

  }

  /// Returns the target value mapped from the given source sign, or `null` if the source sign
  /// was mapped to `null` (untranslated / abstain).
  ///
  /// This is an O(1) array load indexed by the source sign's ordinal, with no allocation.
  ///
  /// @param sign the source sign
  /// @return the pre-computed target value, or `null` if untranslated

  @Override
  public T apply (
    @NotNull final S sign
  ) {

    return
      map[sign.ordinal ()];

  }

}
