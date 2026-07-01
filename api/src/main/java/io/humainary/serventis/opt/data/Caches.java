// Copyright (c) 2025 William David Louth

package io.humainary.serventis.opt.data;

import io.humainary.serventis.api.Serventis;
import io.humainary.serventis.sdk.SignMap;
import io.humainary.serventis.sdk.SignSet;
import io.humainary.serventis.sdk.Statuses;
import io.humainary.substrates.api.Substrates.Utility;

import static io.humainary.serventis.api.Serventis.Kind.OPERATION;
import static io.humainary.serventis.api.Serventis.Kind.OUTCOME;
import static io.humainary.serventis.opt.data.Caches.Sign.*;
import static io.humainary.serventis.sdk.Statuses.Sign.DEGRADED;
import static io.humainary.serventis.sdk.Statuses.Sign.STABLE;

/// # Caches API
///
/// The `Caches` API provides a structured and minimal interface for observing cache
/// interactions within systems. It enables systems to emit **semantic signs** representing
/// lookup operations, hit/miss outcomes, storage operations, and removal conditions.
///
/// ## Purpose
///
/// This API is designed to support **observability and reasoning** about cache behavior
/// in systems. By modeling cache interactions as composable signs, it enables introspection
/// of cache effectiveness, capacity utilization, and operational patterns without coupling
/// to specific implementation details.
///
/// ## Important: Reporting vs Implementation
///
/// This API is for **reporting cache semantics**, not implementing caches.
/// If you have an actual cache implementation (Guava Cache, Caffeine, Redis, etc.),
/// use this API to emit observability signs about operations performed on it.
/// Observer agents can then reason about hit ratios, capacity pressure, and effectiveness
/// patterns without coupling to your implementation details.
///
/// **Example**: When your cache lookup succeeds, call `cache.hit()` to emit a sign.
/// When it misses, call `cache.miss()`. The signs enable meta-observability: observing
/// the observability instrumentation itself to understand cache behavior and effectiveness.
///
/// ## Key Concepts
///
/// - **Cache**: A named subject that emits signs describing operations performed against it
/// - **Sign**: An enumeration of distinct operation types representing cache lifecycle events
///
/// ## Cache Interaction Patterns
///
/// Caches exhibit a lifecycle of lookup, storage, and removal:
///
/// ```
/// Lookup Phase: LOOKUP
///       ↓
/// Outcome Phase: HIT (found) or MISS (not found)
///       ↓
/// Storage Phase: STORE (on miss or update)
///       ↓
/// Removal Phase: EVICT (automatic) or EXPIRE (TTL) or REMOVE (explicit)
/// ```
///
/// ## Signs and Semantics
///
/// | Sign        | Description                                               |
/// |-------------|-----------------------------------------------------------|
/// | `LOOKUP`    | An attempt to retrieve an entry from the cache            |
/// | `HIT`       | A lookup succeeded - entry was found in cache             |
/// | `MISS`      | A lookup failed - entry was not found in cache            |
/// | `STORE`     | An entry was added or updated in the cache                |
/// | `EVICT`     | An entry was automatically removed due to capacity/policy |
/// | `EXPIRE`    | An entry was removed due to TTL/expiration                |
/// | `REMOVE`    | An entry was explicitly invalidated/removed               |
///
/// ## Semantic Distinctions
///
/// - **LOOKUP**: Informational sign - cache access before outcome is determined
/// - **HIT/MISS**: Outcome signs - result of cache lookup operation
/// - **STORE**: Operational sign - cache population or update
/// - **EVICT**: Automatic capacity-driven removal
/// - **EXPIRE**: Automatic time-driven removal
/// - **REMOVE**: Explicit intentional removal
///
/// ## Use Cases
///
/// - Tracking cache effectiveness through hit/miss patterns
/// - Monitoring cache capacity pressure via eviction frequency
/// - Detecting staleness issues through expiration patterns
/// - Understanding cache churn through removal and store frequency
///
/// ## Relationship to Other APIs
///
/// `Caches` signs can inform higher-level abstractions:
///
/// - **Gauges API**: Cache size can be modeled as a gauge (INCREMENT on STORE, DECREMENT on EVICT/EXPIRE/REMOVE)
/// - **Counters API**: Hit/miss totals can be tracked as monotonic counters
/// - **Statuses API**: Cache patterns may indicate DEGRADED or DIVERGING conditions
/// - Observer agents translate cache signs into effectiveness, capacity, or health signs
///
/// ## Performance Considerations
///
/// Cache sign emissions are designed for high-frequency operation (10M-50M Hz).
/// Zero-allocation enum emission with ~10-20ns cost for non-transit emits.
/// Signs flow asynchronously through the circuit's event queue.
///
/// @author William David Louth
/// @since 1.0

@Utility
public final class Caches
  implements Serventis {

  /// The sign set of this API — the captured [Sign] constants from which the canonical
  /// [#STATUS] and [#KIND] interpretations, and any caller-derived sign maps, are mapped.

  public static final SignSet < Sign > SIGNS =
    SignSet.of (
      Sign.class
    );

  /// Canonical sign-to-status translation for caches — the default *immediate interpretant* of the
  /// upward ascent (compose to override; see [SignMap]). A lookup hit reads healthy, a miss degraded;
  /// the lookup/store operations and the evict/expire/remove removals abstain (their health is a rate,
  /// not a per-sign verdict). The hit/miss *ratio* — cache effectiveness — emerges from the
  /// Scorecard's plurality, exactly as Atomics' success/fail ratio does.
  ///
  /// Exhaustive without a `default`: a new [Sign] is a compile error here until its reading is decided.

  public static final SignMap < Sign, Statuses.Sign > STATUS =
    SIGNS.map (
      sign -> switch ( sign ) {
        case HIT -> STABLE;
        case MISS -> DEGRADED;
        case LOOKUP, STORE, EVICT,
             EXPIRE, REMOVE -> null;
      }
    );

  /// Canonical sign-to-kind classification for caches — each [Sign] tagged [Kind#OPERATION] or
  /// [Kind#OUTCOME]: lookup/store/remove are operations; the hit/miss verdicts and the evict/expire
  /// removals are outcomes. Exhaustive without a `default`. See [Kind].

  public static final SignMap < Sign, Kind > KIND =
    SIGNS.map (
      sign -> switch ( sign ) {
        case HIT, MISS, EVICT, EXPIRE -> OUTCOME;
        case LOOKUP, STORE, REMOVE -> OPERATION;
      }
    );

  private Caches () { }

  /// Creates a Cache instrument wrapping the specified pipe.
  ///
  /// @param pipe the pipe from which to create the cache
  /// @return a new Cache instrument for the specified pipe
  /// @throws NullPointerException if the pipe parameter is `null`

  @New
  @NotNull
  public static Cache of (
    @NotNull final Pipe < ? super Sign > pipe
  ) {

    return
      new Cache (
        pipe
      );

  }

  /// Returns a pool that creates cached Cache instruments from a conduit.
  ///
  /// @param conduit the conduit providing sign pipes
  /// @return a pool that creates Cache instruments
  /// @throws NullPointerException if the conduit parameter is `null`

  @New
  @NotNull
  public static Pool < Cache > pool (
    @NotNull final Conduit < Sign > conduit
  ) {

    return
      conduit.pool (
        Caches::of
      );

  }

  /// A [Sign] represents the kind of action being observed in a cache interaction.
  ///
  /// These signs distinguish between lookup operations (LOOKUP), their outcomes
  /// (HIT/MISS), storage operations (STORE), and different removal conditions
  /// (EVICT/EXPIRE/REMOVE).

  public enum Sign
    implements Serventis.Sign {

    /// Indicates an attempt to retrieve an entry from the cache.
    ///
    /// This sign represents a cache access operation before the outcome is known.

    LOOKUP,

    /// Indicates a cache lookup succeeded - the requested entry was found.
    ///
    /// This sign represents successful cache access, avoiding more expensive fallback
    /// operations like database queries or computation.

    HIT,

    /// Indicates a cache lookup failed - the requested entry was not found.
    ///
    /// This sign represents cache miss, requiring fallback to the authoritative source.

    MISS,

    /// Indicates an entry was added to or updated in the cache.
    ///
    /// This sign represents cache population or refresh operations, typically following
    /// MISS events when loading from authoritative sources.

    STORE,

    /// Indicates an entry was automatically removed due to capacity or policy.
    ///
    /// Eviction reveals capacity boundaries where the cache reached limits (capacity,
    /// LRU/LFU policy) and removed entries to make space.

    EVICT,

    /// Indicates an entry was removed because it reached its time-to-live (TTL).
    ///
    /// Expiration represents time-based invalidation, distinct from capacity-driven
    /// eviction.

    EXPIRE,

    /// Indicates an entry was explicitly removed or invalidated.
    ///
    /// This sign represents intentional cache invalidation, distinct from automatic
    /// EVICT or EXPIRE.

    REMOVE

  }

  /// The [Cache] class represents a named, observable cache from which signs are emitted.
  ///
  /// ## Usage
  ///
  /// Use domain-specific methods: `cache.lookup()`, `cache.hit()`, `cache.miss()`,
  /// `cache.store()`, `cache.evict()`, `cache.expire()`, `cache.remove()`
  ///
  /// Caches provide semantic methods for reporting cache lifecycle events.

  @Queued
  @Provided
  public static final class Cache
    implements Signer < Sign > {

    private final Pipe < ? super Sign > pipe;

    private Cache (
      final Pipe < ? super Sign > pipe
    ) {

      this.pipe = pipe;

    }

    /// Emits an evict sign from this cache.

    public void evict () {

      pipe.emit (
        EVICT
      );

    }

    /// Emits an expire sign from this cache.

    public void expire () {

      pipe.emit (
        EXPIRE
      );

    }

    /// Emits a hit sign from this cache.

    public void hit () {

      pipe.emit (
        HIT
      );

    }

    /// Emits a lookup sign from this cache.

    public void lookup () {

      pipe.emit (
        LOOKUP
      );

    }

    /// Emits a miss sign from this cache.

    public void miss () {

      pipe.emit (
        MISS
      );

    }

    /// Emits a remove sign from this cache.

    public void remove () {

      pipe.emit (
        REMOVE
      );

    }

    /// Signs a cache event.
    ///
    /// @param sign the sign to make

    @Override
    public void sign (
      @NotNull final Sign sign
    ) {

      pipe.emit (
        sign
      );

    }

    /// Emits a store sign from this cache.

    public void store () {

      pipe.emit (
        STORE
      );

    }

  }

}
