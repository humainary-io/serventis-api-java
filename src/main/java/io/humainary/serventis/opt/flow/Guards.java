// Copyright (c) 2025 William David Louth

package io.humainary.serventis.opt.flow;

import io.humainary.serventis.api.Serventis;
import io.humainary.serventis.sdk.Outcomes;
import io.humainary.serventis.sdk.SignMap;
import io.humainary.serventis.sdk.SignSet;
import io.humainary.serventis.sdk.SignalSet;
import io.humainary.serventis.sdk.Statuses;
import io.humainary.serventis.sdk.SymbolSet;
import io.humainary.specs.api.Specs.SpecDoc;
import io.humainary.specs.api.Specs.SpecRef;
import io.humainary.substrates.api.Substrates.Utility;

import static io.humainary.serventis.sdk.Statuses.Sign.DEGRADED;
import static io.humainary.serventis.sdk.Statuses.Sign.STABLE;
import static java.util.Objects.requireNonNull;

/// # Guards API
///
/// The `Guards` API reports access decisions: a credential or request presented to a guard, further
/// proof demanded, and access granted or denied. The subject is the guard, such as an
/// authenticator or a policy decision point.
///
/// ## Purpose
///
/// An access refusal borrowed from another vocabulary is misread. `Resources.DENY` means a shortage
/// of capacity, and `Valves.DENY` a gate shedding load. Neither can say whether identity or
/// permission failed. `Guards` reports the decision itself and which check made it.
///
/// ## Important: Reporting vs Implementation
///
/// This API is for **reporting access decisions**, not making them. The guard emits each sign as it
/// reaches the corresponding step; every sign is its own decision or the request it was given.
///
/// ## Key Concepts
///
/// - **Guard**: a named access decision point
/// - **Sign**: a step in an access decision (`ATTEMPT`, `CHALLENGE`, `GRANT`, `DENY`)
/// - **Dimension**: which check the step belongs to (`IDENTITY`, `PERMISSION`)
///
/// ## Signal Matrix
///
/// | Sign        | IDENTITY                               | PERMISSION                              |
/// |-------------|----------------------------------------|-----------------------------------------|
/// | `ATTEMPT`   | A credential was presented             | A request was presented for a decision  |
/// | `CHALLENGE` | Further proof, such as a second factor | Step-up proof for this request          |
/// | `GRANT`     | The caller was authenticated           | The caller was authorized               |
/// | `DENY`      | The credential was refused             | A known caller lacked the right         |
///
/// ## Scope
///
/// Guards covers the decision only. A session or token the guard issues has a lifetime of its own,
/// and that lifetime is a lease: report it through `Leases`, which has `EXPIRE`, `REVOKE`, `RENEW`,
/// and `EXTEND`.
///
/// Guards publishes no `OPERATION` map: a guard serves many requests at once, so two `ATTEMPT`s in a
/// row are normal, and a bracket reading one trace per subject would take the second for an
/// abandoned episode.
///
/// ## When the Identity Store Fails
///
/// A guard that cannot reach its identity store or policy store still reports what it decided:
/// `DENY` when it fails closed, `GRANT` when it fails open. The outage itself is reported on the
/// store's own subject, through `Services` or `Probes`. A consumer that sees a guard's refusals climb
/// reads the store's subject to tell an outage from a wave of bad credentials.
///
/// ## Relationship to Other APIs
///
/// - **Valves API**: a valve admits or refuses load; a guard admits or refuses a caller
/// - **Leases API**: the session or token a grant produces
/// - **Statuses API**: `GRANT` reads stable and `DENY` degraded
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
@SpecRef ( {"8.2", "registry:guards"} )
public final class Guards
  implements Serventis {

  /// The sign set of this API — the captured [Sign] constants from which the canonical [#STATUS],
  /// [#KIND], and [#OUTCOME] interpretations, and any caller-derived sign maps, are mapped.

  @SpecRef ( "4.5" )
  public static final SignSet < Sign > SIGNS =
    SignSet.of (
      Sign.class
    );

  /// The dimension set of this API — the captured [Dimension] constants used to build
  /// guard signal instances and caller-derived signal maps.

  @SpecRef ( "4.5" )
  public static final SymbolSet < Dimension > DIMENSIONS =
    SymbolSet.of (
      Dimension.class
    );

  /// Canonical sign-to-status translation for guards — the default *immediate interpretant* of the
  /// upward ascent (sign-keyed; the check does not change the reading; compose to override, see
  /// [SignMap]). A grant reads healthy and a denial degraded. A Scorecard weighs recent votes most,
  /// so the guard reads degraded once refusals outweigh grants among its recent decisions, as in an
  /// attack, an identity store outage, or after a bad policy change. With little history the reading
  /// is `TENTATIVE`, and a single refusal can tip it. Presenting a request and demanding further
  /// proof abstain. The instrument
  /// emits `Signal`, so as a Scorecards ballot project the sign first —
  /// `Scorecards.flow ( STATUS.compose ( Signal::sign ) )`.
  ///
  /// Exhaustive without a `default`: a new [Sign] is a compile error here until its reading is decided.

  public static final SignMap < Sign, Statuses.Sign > STATUS =
    SIGNS.map (
      sign -> switch ( sign ) {
        case GRANT -> STABLE;
        case DENY -> DEGRADED;
        case ATTEMPT, CHALLENGE -> null;
      }
    );

  /// Canonical sign-to-kind classification for guards — each [Sign] tagged [Kind#OPERATION] or
  /// [Kind#OUTCOME]: presenting a request and demanding further proof are operations; granting and
  /// denying are outcomes. The dimension does not change the kind. Exhaustive without a `default`.
  /// See [Kind].

  public static final SignMap < Sign, Kind > KIND =
    SIGNS.map (
      sign -> switch ( sign ) {
        case GRANT, DENY -> Kind.OUTCOME;
        case ATTEMPT, CHALLENGE -> Kind.OPERATION;
      }
    );

  /// Sign-to-[Outcomes] translation — the *outcome* half of the grammatical interlingua: a grant reads
  /// [Outcomes.Sign#SUCCESS] and a denial [Outcomes.Sign#FAIL]. The operations abstain. Exhaustive
  /// without a `default`, consistent with [#KIND].

  public static final SignMap < Sign, Outcomes.Sign > OUTCOME =
    SIGNS.map (
      sign -> switch ( sign ) {
        case GRANT -> Outcomes.Sign.SUCCESS;
        case DENY -> Outcomes.Sign.FAIL;
        case ATTEMPT, CHALLENGE -> null;
      }
    );

  private Guards () { }

  /// Creates a Guard instrument wrapping the specified pipe.
  ///
  /// @param pipe the pipe from which to create the guard
  /// @return a new Guard instrument for the specified pipe
  /// @throws NullPointerException if the pipe parameter is `null`

  @SpecRef ( "6.4" )
  @New
  @NotNull
  public static Guard of (
    @NotNull final Pipe < ? super Signal > pipe
  ) {

    return
      new Guard (
        requireNonNull ( pipe )
      );

  }

  /// Returns a pool that creates cached Guard instruments from a conduit.
  ///
  /// Within the returned pool, repeated lookup of the same name returns the same instrument,
  /// created on first lookup. Separate pools have separate identity guarantees.
  ///
  /// @param conduit the conduit providing signal pipes
  /// @return a pool that creates Guard instruments
  /// @throws NullPointerException if the conduit parameter is `null`

  @SpecRef ( {"6.4", "substrates:10.1"} )
  @New
  @NotNull
  public static Pool < Guard > pool (
    @NotNull final Conduit < Signal > conduit
  ) {

    return
      conduit.pool (
        Guards::of
      );

  }


  /// The [Dimension] enum names the check an access decision belongs to.
  ///
  /// `DENY` on identity is a bad credential; `DENY` on permission is a known caller without the
  /// right. The two call for different responses, so the dimension keeps them apart.

  @SpecRef ( {"4.3", "registry:guards"} )
  public enum Dimension
    implements Category {

    /// The check of who the caller is: authentication.

    IDENTITY,

    /// The check of what the caller may do: authorization.

    PERMISSION

  }


  /// The [Sign] enum represents a step in an access decision.
  ///
  /// A request is presented, further proof may be demanded, and access is granted or denied.

  @SpecRef ( {"4.2", "registry:guards"} )
  public enum Sign
    implements Serventis.Sign {

    /// A credential or request was presented.

    ATTEMPT,

    /// Further proof was required, such as a second factor.

    CHALLENGE,

    /// Access was allowed.

    GRANT,

    /// Access was refused.

    DENY

  }

  /// The [Guard] class emits access decision observations for one named guard.
  ///
  /// ## Usage
  ///
  /// ```java
  /// // A login with a second factor
  /// guard.attempt(IDENTITY);
  /// guard.challenge(IDENTITY);
  /// guard.grant(IDENTITY);
  ///
  /// // An authenticated caller without the right
  /// guard.attempt(PERMISSION);
  /// guard.deny(PERMISSION);
  /// ```

  @SpecRef ( {"6.2", "6.3", "6.5", "substrates:6.1"} )
  @Queued
  @Provided
  public static final class Guard
    implements Signaler < Sign, Dimension > {

    private static final SignalSet < Sign, Dimension, Signal > SIGNALS =
      SIGNS.signals (
        DIMENSIONS,
        Signal::new
      );

    private final Pipe < ? super Signal > pipe;

    private Guard (
      final Pipe < ? super Signal > pipe
    ) {

      this.pipe =
        pipe;

    }

    /// Emits an `ATTEMPT` sign for the specified check.
    ///
    /// @param dimension the check the step belongs to
    /// @throws NullPointerException if the dimension is `null`

    public void attempt (
      @NotNull final Dimension dimension
    ) {

      signal (
        Sign.ATTEMPT,
        dimension
      );

    }

    /// Emits a `CHALLENGE` sign for the specified check.
    ///
    /// @param dimension the check the step belongs to
    /// @throws NullPointerException if the dimension is `null`

    public void challenge (
      @NotNull final Dimension dimension
    ) {

      signal (
        Sign.CHALLENGE,
        dimension
      );

    }

    /// Emits a `DENY` sign for the specified check.
    ///
    /// @param dimension the check the step belongs to
    /// @throws NullPointerException if the dimension is `null`

    public void deny (
      @NotNull final Dimension dimension
    ) {

      signal (
        Sign.DENY,
        dimension
      );

    }

    /// Emits a `GRANT` sign for the specified check.
    ///
    /// @param dimension the check the step belongs to
    /// @throws NullPointerException if the dimension is `null`

    public void grant (
      @NotNull final Dimension dimension
    ) {

      signal (
        Sign.GRANT,
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

  }

  /// The [Signal] record represents an access decision observation.
  ///
  /// Each signal combines a sign with the check it belongs to.
  ///
  /// @param sign      the step in the access decision
  /// @param dimension the check the step belongs to

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
