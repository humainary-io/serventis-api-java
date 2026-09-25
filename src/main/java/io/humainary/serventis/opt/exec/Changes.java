// Copyright (c) 2025 William David Louth

package io.humainary.serventis.opt.exec;

import io.humainary.serventis.api.Serventis;
import io.humainary.serventis.sdk.*;
import io.humainary.specs.api.Specs.SpecDoc;
import io.humainary.specs.api.Specs.SpecRef;
import io.humainary.substrates.api.Substrates.Utility;

import static io.humainary.serventis.opt.exec.Changes.Sign.*;
import static io.humainary.serventis.sdk.Statuses.Sign.DEGRADED;
import static io.humainary.serventis.sdk.Statuses.Sign.STABLE;
import static java.util.Objects.requireNonNull;

/// # Changes API
///
/// The `Changes` API reports changes made to a subject: a deployment, a configuration change, a
/// feature flag, a schema migration. Each change is one episode, opened when the change begins and
/// closed when it takes effect, fails, is refused, or is reverted.
///
/// ## Purpose
///
/// When a subject degrades, the first question is usually what changed. Nothing else in the registry
/// can answer it. `Changes` lets the deployer, the configuration system, or the subject itself say
/// that a change began and how it ended, so a consumer correlating a degraded reading with recent
/// activity has the change as evidence.
///
/// ## Important: Reporting vs Implementation
///
/// This API is for **reporting change semantics**, not implementing deployment. Every sign reports a
/// step the change mechanism performed or a result it was given: a change beginning, an apply
/// succeeding or failing, a validation refusing, a rollback being carried out.
///
/// ## Key Concepts
///
/// - **Change**: a named stream of changes to a subject, applied one at a time; what kind of change it
///   is belongs in the name
/// - **Sign**: a step in the change's episode (`START`, `APPLY`, `FAIL`, `REJECT`, `REVERT`,
///   `PROGRESS`)
///
/// ## The Episode
///
/// ```
/// START → APPLY    the change took effect
/// START → FAIL     the change could not be applied
/// START → REJECT   the subject refused the change
/// START → REVERT   the change was rolled back before it took effect
///
/// START → PROGRESS → PROGRESS → APPLY   a staged rollout advanced twice, then took effect
/// ```
///
/// A staged rollout reports `PROGRESS` each time it advances a stage, such as a canary step, before
/// the sign that closes it.
///
/// Validation is part of the change. A change the subject refuses before rolling it out still
/// reports `START` first, so `REJECT` always closes an open episode.
///
/// Undoing a change that already took effect is a new change, reported as `START` then `APPLY`. So
/// no sign ever arrives after its episode has closed.
///
/// ## One Change at a Time
///
/// Each subject carries at most one open change, because the episode structure in [#OPERATION] reads
/// one trace per subject. Changes that can overlap, such as a deployment and a feature-flag change to
/// the same service, are reported under distinct subjects, one per stream of changes applied in
/// order. A producer that abandons an unfinished change for a newer one closes the older one before
/// starting the newer: `REVERT` if it was rolled back, `FAIL` if it could not be applied.
///
/// ## What Changes Cannot Say
///
/// A change near a `DOWN` reading is not evidence that the downtime was planned. Telling planned
/// downtime from an outage needs an observation of intent, which this vocabulary does not carry.
///
/// ## Relationship to Other APIs
///
/// - **Transactions API**: a transaction commits or rolls back data; a change applies or reverts
///   a subject's code or configuration
/// - **Tasks API**: the job carrying out a rollout may itself be reported through Tasks
/// - **Statuses API**: `APPLY` reads stable; `FAIL` and `REVERT` read degraded; `REJECT` abstains,
///   since refusing a bad change is the system working
///
/// ## Performance Considerations
///
/// See [io.humainary.serventis.api.Serventis.Signer] for observation reuse and the
/// limits of performance guarantees. Measure the provider and pipeline for the intended workload.
///
/// @author William David Louth
/// @since 3.6

@Utility
@SpecDoc ( "https://github.com/humainary-io/serventis-api-spec/blob/3.6.0/SPEC.md" )
@SpecRef ( {"8.2", "registry:changes"} )
public final class Changes
  implements Serventis {

  /// The sign set of this API — the captured [Sign] constants from which the canonical
  /// [#STATUS] and [#KIND] interpretations, and any caller-derived sign maps, are mapped.

  @SpecRef ( "4.5" )
  public static final SignSet < Sign > SIGNS =
    SignSet.of (
      Sign.class
    );

  /// Canonical sign-to-status translation for changes — the default *immediate interpretant* of the
  /// upward ascent (compose to override; see [SignMap]). A change that took effect reads healthy; a
  /// failed change and a reverted one read degraded. `REJECT` abstains: the subject refusing a bad
  /// change is the system working. `START` and `PROGRESS` abstain.
  ///
  /// Exhaustive without a `default`: a new [Sign] is a compile error here until its reading is decided.

  public static final SignMap < Sign, Statuses.Sign > STATUS =
    SIGNS.map (
      sign -> switch ( sign ) {
        case APPLY -> STABLE;
        case FAIL, REVERT -> DEGRADED;
        case START, REJECT, PROGRESS -> null;
      }
    );

  /// Canonical sign-to-kind classification for changes — each [Sign] tagged [Kind#OPERATION] or
  /// [Kind#OUTCOME]: starting and advancing a stage are acts; applying, failing, being refused, and
  /// being reverted are results, as a transaction's rollback is. Exhaustive without a `default`.

  public static final SignMap < Sign, Kind > KIND =
    SIGNS.map (
      sign -> switch ( sign ) {
        case APPLY, FAIL, REJECT, REVERT -> Kind.OUTCOME;
        case START, PROGRESS -> Kind.OPERATION;
      }
    );

  /// Sign-to-[Operations] classification — the **episode structure** of each sign (see
  /// `SEQUENCERS.md`): the start opens the change ([Operations.Sign#BEGIN]); a stage of a staged
  /// rollout advances it ([Operations.Sign#ADVANCE]); applying, failing, being refused, and reverting
  /// each close it ([Operations.Sign#END]). Total and exhaustive without a `default`.

  public static final SignMap < Sign, Operations.Sign > OPERATION =
    SIGNS.map (
      sign -> switch ( sign ) {
        case START -> Operations.Sign.BEGIN;
        case PROGRESS -> Operations.Sign.ADVANCE;
        case APPLY, FAIL, REJECT, REVERT -> Operations.Sign.END;
      }
    );

  /// Sign-to-[Outcomes] translation — the *outcome* half of the grammatical interlingua: a change
  /// that took effect reads [Outcomes.Sign#SUCCESS]; a failed, refused, or reverted change reads
  /// [Outcomes.Sign#FAIL]. The operations abstain. Exhaustive without a `default`, consistent with
  /// [#KIND].

  public static final SignMap < Sign, Outcomes.Sign > OUTCOME =
    SIGNS.map (
      sign -> switch ( sign ) {
        case APPLY -> Outcomes.Sign.SUCCESS;
        case FAIL, REJECT, REVERT -> Outcomes.Sign.FAIL;
        case START, PROGRESS -> null;
      }
    );

  private Changes () { }

  /// Creates a Change instrument wrapping the specified pipe.
  ///
  /// @param pipe the pipe from which to create the change
  /// @return a new Change instrument for the specified pipe
  /// @throws NullPointerException if the pipe parameter is `null`

  @SpecRef ( "6.4" )
  @New
  @NotNull
  public static Change of (
    @NotNull final Pipe < ? super Sign > pipe
  ) {

    return
      new Change (
        requireNonNull ( pipe )
      );

  }

  /// Returns a pool that creates cached Change instruments from a conduit.
  ///
  /// Within the returned pool, repeated lookup of the same name returns the same instrument,
  /// created on first lookup. Separate pools have separate identity guarantees.
  ///
  /// @param conduit the conduit providing sign pipes
  /// @return a pool that creates Change instruments
  /// @throws NullPointerException if the conduit parameter is `null`

  @SpecRef ( {"6.4", "substrates:10.1"} )
  @New
  @NotNull
  public static Pool < Change > pool (
    @NotNull final Conduit < Sign > conduit
  ) {

    return
      conduit.pool (
        Changes::of
      );

  }


  /// The [Sign] enum represents a step in a change's episode.
  ///
  /// `START` opens the episode, `PROGRESS` marks each stage a staged rollout advances, and exactly one
  /// of `APPLY`, `FAIL`, `REJECT`, or `REVERT` closes it.

  @SpecRef ( {"4.2", "registry:changes"} )
  public enum Sign
    implements Serventis.Sign {

    /// A change to the subject began.
    ///
    /// Validation, where there is any, is part of the change, so a change refused before it rolls
    /// out still begins here.

    START,

    /// The change took effect.

    APPLY,

    /// The change could not be applied.

    FAIL,

    /// The subject refused the change, by validation or policy.

    REJECT,

    /// A change still rolling out was rolled back before it took effect.

    REVERT,

    /// The change advanced a stage, such as a canary step.

    PROGRESS

  }

  /// The [Change] class emits change observations for one named subject.
  ///
  /// ## Usage
  ///
  /// ```java
  /// deployment.start();
  /// deployment.progress();   // canary
  /// deployment.apply();
  ///
  /// config.start();
  /// config.reject();
  /// ```

  @SpecRef ( {"6.1", "6.3", "6.5", "substrates:6.1"} )
  @Queued
  @Provided
  public static final class Change
    implements Signer < Sign > {

    private final Pipe < ? super Sign > pipe;

    private Change (
      final Pipe < ? super Sign > pipe
    ) {

      this.pipe =
        pipe;

    }

    /// Emits an `APPLY` sign: the change took effect.

    public void apply () {

      pipe.emit (
        APPLY
      );

    }

    /// Emits a `FAIL` sign: the change could not be applied.

    public void fail () {

      pipe.emit (
        FAIL
      );

    }

    /// Emits a `PROGRESS` sign: the change advanced a stage.

    public void progress () {

      pipe.emit (
        PROGRESS
      );

    }

    /// Emits a `REJECT` sign: the subject refused the change.

    public void reject () {

      pipe.emit (
        REJECT
      );

    }

    /// Emits a `REVERT` sign: the change was rolled back before it took effect.

    public void revert () {

      pipe.emit (
        REVERT
      );

    }

    /// Signs a change event.
    ///
    /// @param sign the sign to make

    @SpecRef ( "6.1" )
    @Override
    public void sign (
      @NotNull final Sign sign
    ) {

      pipe.emit (
        sign
      );

    }

    /// Emits a `START` sign: the change began.

    public void start () {

      pipe.emit (
        START
      );

    }

  }

}
