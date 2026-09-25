// Copyright (c) 2025 William David Louth

package io.humainary.serventis.opt.role;

import io.humainary.serventis.api.Serventis;
import io.humainary.serventis.sdk.SignMap;
import io.humainary.serventis.sdk.SignSet;
import io.humainary.serventis.sdk.SignalSet;
import io.humainary.serventis.sdk.Statuses;
import io.humainary.serventis.sdk.SymbolSet;
import io.humainary.specs.api.Specs.SpecDoc;
import io.humainary.specs.api.Specs.SpecRef;
import io.humainary.substrates.api.Substrates.Utility;

import static io.humainary.serventis.api.Serventis.Kind.OPERATION;
import static io.humainary.serventis.api.Serventis.Kind.OUTCOME;
import static io.humainary.serventis.sdk.Statuses.Sign.DEGRADED;
import static io.humainary.serventis.sdk.Statuses.Sign.STABLE;
import static java.util.Objects.requireNonNull;

/// # Members API
///
/// The `Members` API reports group membership and leadership as a membership protocol decides them:
/// who joined, who left, who is suspected of failure, who was removed, and who leads. It covers
/// cluster membership services, gossip-based failure detectors, and consensus groups.
///
/// ## Purpose
///
/// Nothing else in the registry can say who belongs to a group. `Leases` can model holding a
/// leadership lease, but not the membership around it, and the Systems `LINK` constraint says only
/// that connectivity failed. `Members` reports the membership events themselves, so a consumer can
/// see a node being suspected, refuting the suspicion, or being evicted.
///
/// ## Important: The Protocol Decides
///
/// This vocabulary is **primary**. Every sign reports a result the membership protocol itself
/// presents: a join or leave it processed, a suspicion its failure detector raised, a refutation it
/// accepted, an eviction it carried out, an election it concluded. A producer MUST NOT choose
/// `SUSPECT` or `EVICT` by comparing heartbeat gaps or timeouts itself; that comparison is the
/// failure detector's work, and a producer doing it is emitting a derived observation this
/// vocabulary does not carry.
///
/// ## Key Concepts
///
/// - **Member**: a named group member whose membership events are observed
/// - **Sign**: a membership or leadership event (`JOIN`, `LEAVE`, `SUSPECT`, `REFUTE`, `EVICT`,
///   `ELECT`, `RESIGN`)
/// - **Dimension**: whether the member reporting is the one the event concerns (`SELF`) or another
///   member (`PEER`)
///
/// ## Signal Matrix
///
/// | Sign      | SELF                                | PEER                                   |
/// |-----------|-------------------------------------|----------------------------------------|
/// | `JOIN`    | I joined the group                  | This member joined, as I learned       |
/// | `LEAVE`   | I left the group                    | This member left, as I learned         |
/// | `SUSPECT` | I am suspected of failure           | This member is suspected of failure    |
/// | `REFUTE`  | I refuted a suspicion of me         | This member refuted its suspicion      |
/// | `EVICT`   | I was removed while still running   | This member was declared failed        |
/// | `ELECT`   | I became leader                     | This member became leader              |
/// | `RESIGN`  | I stepped down as leader            | This member stepped down as leader     |
///
/// `EVICT` about a peer is routine. `EVICT` about oneself means this member was removed while still
/// running, which is how a split brain first shows.
///
/// ## Relationship to Other APIs
///
/// - **Leases API**: a leader holding a lease reports it through Leases; who is in the group is
///   reported here
/// - **Systems API**: repeated suspicion may indicate a `LINK` constraint
/// - **Statuses API**: `REFUTE` and `ELECT` read stable; `SUSPECT` reads degraded
///
/// ## Performance Considerations
///
/// See [io.humainary.serventis.api.Serventis.Signaler] for observation reuse and the
/// limits of performance guarantees. Measure the provider and pipeline for the intended workload.
///
/// @author William David Louth
/// @since 3.6

@Utility
@SpecDoc ( "https://github.com/humainary-io/serventis-api-spec/blob/3.6.0/SPEC.md" )
@SpecRef ( {"8.2", "registry:members"} )
public final class Members
  implements Serventis {

  /// The sign set of this API — the captured [Sign] constants from which the canonical
  /// [#STATUS] and [#KIND] interpretations, and any caller-derived sign maps, are mapped.

  @SpecRef ( "4.5" )
  public static final SignSet < Sign > SIGNS =
    SignSet.of (
      Sign.class
    );

  /// The dimension set of this API — the captured [Dimension] constants used to build
  /// member signal instances and caller-derived signal maps.

  @SpecRef ( "4.5" )
  public static final SymbolSet < Dimension > DIMENSIONS =
    SymbolSet.of (
      Dimension.class
    );

  /// Canonical sign-to-status translation for members — the default *immediate interpretant* of the
  /// upward ascent (sign-keyed; compose to override, see [SignMap]). A refuted suspicion and a
  /// concluded election read healthy; a suspicion reads degraded from either end, since either the
  /// peer or this member's own connectivity is in doubt. `EVICT` abstains: a map keyed on the sign
  /// alone cannot tell a routine eviction of a peer from this member's own removal, and the two
  /// deserve different readings, so a consumer that needs one keys on the full signal. Joining,
  /// leaving, and resigning abstain. The instrument emits `Signal`, so as a Scorecards ballot project
  /// the sign first — `Scorecards.flow ( STATUS.compose ( Signal::sign ) )`.
  ///
  /// Exhaustive without a `default`: a new [Sign] is a compile error here until its reading is decided.

  public static final SignMap < Sign, Statuses.Sign > STATUS =
    SIGNS.map (
      sign -> switch ( sign ) {
        case REFUTE, ELECT -> STABLE;
        case SUSPECT -> DEGRADED;
        case JOIN, LEAVE, EVICT, RESIGN -> null;
      }
    );

  /// Canonical sign-to-kind classification for members — each [Sign] tagged [Kind#OPERATION] or
  /// [Kind#OUTCOME]: joining, leaving, and resigning are acts a member performs; suspicion,
  /// refutation, eviction, and election are results the protocol reaches. The dimension does not
  /// change the kind. Exhaustive without a `default`. See [Kind].

  public static final SignMap < Sign, Kind > KIND =
    SIGNS.map (
      sign -> switch ( sign ) {
        case SUSPECT, REFUTE, EVICT, ELECT -> OUTCOME;
        case JOIN, LEAVE, RESIGN -> OPERATION;
      }
    );

  private Members () { }

  /// Creates a Member instrument wrapping the specified pipe.
  ///
  /// @param pipe the pipe from which to create the member
  /// @return a new Member instrument for the specified pipe
  /// @throws NullPointerException if the pipe parameter is `null`

  @SpecRef ( "6.4" )
  @New
  @NotNull
  public static Member of (
    @NotNull final Pipe < ? super Signal > pipe
  ) {

    return
      new Member (
        requireNonNull ( pipe )
      );

  }

  /// Returns a pool that creates cached Member instruments from a conduit.
  ///
  /// Within the returned pool, repeated lookup of the same name returns the same instrument,
  /// created on first lookup. Separate pools have separate identity guarantees.
  ///
  /// @param conduit the conduit providing signal pipes
  /// @return a pool that creates Member instruments
  /// @throws NullPointerException if the conduit parameter is `null`

  @SpecRef ( {"6.4", "substrates:10.1"} )
  @New
  @NotNull
  public static Pool < Member > pool (
    @NotNull final Conduit < Signal > conduit
  ) {

    return
      conduit.pool (
        Members::of
      );

  }


  /// The [Dimension] enum says whether the reporting member is the one an event concerns.
  ///
  /// A member's subject names the member the event is about. The member itself reports with
  /// `SELF`; any other member reporting what it learned about that member uses `PEER`. The same
  /// event reported from both ends produces two signals that differ only in dimension, and a
  /// disagreement between them is how a partition shows.

  @SpecRef ( {"4.3", "registry:members"} )
  public enum Dimension
    implements Category {

    /// Reported by the member the event concerns.
    ///
    /// **Mental model**: "this happened to me"

    SELF,

    /// Reported by another member of the group.
    ///
    /// **Mental model**: "this happened to that member"

    PEER

  }


  /// The [Sign] enum represents a membership or leadership event.
  ///
  /// A member joins, may be suspected and either refute the suspicion or be evicted, and leaves.
  /// Leadership is reported alongside: a member is elected and may resign.

  @SpecRef ( {"4.2", "registry:members"} )
  public enum Sign
    implements Serventis.Sign {

    /// A member joined the group.

    JOIN,

    /// A member left the group gracefully.

    LEAVE,

    /// The failure detector marked a member suspect.

    SUSPECT,

    /// A suspected member proved it was alive.

    REFUTE,

    /// A member was declared failed and removed.

    EVICT,

    /// A member became leader.

    ELECT,

    /// A leader stepped down.

    RESIGN

  }

  /// The [Member] class emits membership observations about one named member.
  ///
  /// ## Usage
  ///
  /// ```java
  /// // Node 1 reports about itself
  /// self.join(SELF);
  ///
  /// // Node 1 reports what its failure detector concluded about node 3
  /// node3.suspect(PEER);
  /// node3.evict(PEER);
  ///
  /// // Node 3, still running, learns it was removed
  /// self.evict(SELF);
  /// ```

  @SpecRef ( {"6.2", "6.3", "6.5", "substrates:6.1"} )
  @Queued
  @Provided
  public static final class Member
    implements Signaler < Sign, Dimension > {

    private static final SignalSet < Sign, Dimension, Signal > SIGNALS =
      SIGNS.signals (
        DIMENSIONS,
        Signal::new
      );

    private final Pipe < ? super Signal > pipe;

    private Member (
      final Pipe < ? super Signal > pipe
    ) {

      this.pipe =
        pipe;

    }

    /// Emits an `ELECT` sign from the specified perspective.
    ///
    /// @param dimension whether the reporter is the member concerned
    /// @throws NullPointerException if the dimension is `null`

    public void elect (
      @NotNull final Dimension dimension
    ) {

      signal (
        Sign.ELECT,
        dimension
      );

    }

    /// Emits an `EVICT` sign from the specified perspective.
    ///
    /// @param dimension whether the reporter is the member concerned
    /// @throws NullPointerException if the dimension is `null`

    public void evict (
      @NotNull final Dimension dimension
    ) {

      signal (
        Sign.EVICT,
        dimension
      );

    }

    /// Emits a `JOIN` sign from the specified perspective.
    ///
    /// @param dimension whether the reporter is the member concerned
    /// @throws NullPointerException if the dimension is `null`

    public void join (
      @NotNull final Dimension dimension
    ) {

      signal (
        Sign.JOIN,
        dimension
      );

    }

    /// Emits a `LEAVE` sign from the specified perspective.
    ///
    /// @param dimension whether the reporter is the member concerned
    /// @throws NullPointerException if the dimension is `null`

    public void leave (
      @NotNull final Dimension dimension
    ) {

      signal (
        Sign.LEAVE,
        dimension
      );

    }

    /// Emits a `REFUTE` sign from the specified perspective.
    ///
    /// @param dimension whether the reporter is the member concerned
    /// @throws NullPointerException if the dimension is `null`

    public void refute (
      @NotNull final Dimension dimension
    ) {

      signal (
        Sign.REFUTE,
        dimension
      );

    }

    /// Emits a `RESIGN` sign from the specified perspective.
    ///
    /// @param dimension whether the reporter is the member concerned
    /// @throws NullPointerException if the dimension is `null`

    public void resign (
      @NotNull final Dimension dimension
    ) {

      signal (
        Sign.RESIGN,
        dimension
      );

    }

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

    /// Emits a `SUSPECT` sign from the specified perspective.
    ///
    /// @param dimension whether the reporter is the member concerned
    /// @throws NullPointerException if the dimension is `null`

    public void suspect (
      @NotNull final Dimension dimension
    ) {

      signal (
        Sign.SUSPECT,
        dimension
      );

    }

  }

  /// The [Signal] record represents a membership observation.
  ///
  /// Each signal combines a sign with whether the reporter is the member concerned.
  ///
  /// @param sign      the membership or leadership event
  /// @param dimension whether the reporter is the member concerned

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
