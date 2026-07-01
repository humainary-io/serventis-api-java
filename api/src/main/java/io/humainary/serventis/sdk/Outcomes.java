// Copyright (c) 2025 William David Louth

package io.humainary.serventis.sdk;

import io.humainary.serventis.api.Serventis;
import io.humainary.substrates.api.Substrates.Utility;

import static io.humainary.serventis.sdk.Outcomes.Sign.*;

/// # Outcomes API
///
/// The `Outcomes` API provides a minimal verdict vocabulary for expressing success, failure, or an
/// indeterminate result. It enables systems to emit universal `SUCCESS`/`FAIL`/`UNKNOWN` signals that
/// aggregate across diverse domain vocabularies.
///
/// ## Purpose
///
/// This API offers the smallest possible outcome vocabulary for universal rollup and translations.
/// When domain APIs (Services, Tasks, Transactions, etc.) have rich outcome vocabularies (COMPLETE,
/// EXPIRE, TIMEOUT, REJECT, etc.), observers can emit a simple verdict using this API to drive
/// higher-level translations (Statuses, Situations) without requiring knowledge of source
/// vocabularies. `UNKNOWN` abstains from success/failure tallies for genuinely indeterminate results
/// (a write conflict that may retry or abort, an ambiguous disconnect) rather than forcing a verdict.
///
/// ## Key Insight: Semiotic Ascent
///
/// As signals ascend the semiotic hierarchy, they trade specificity for universality.
/// Outcomes represents this abstraction: the cause is lost, the verdict remains.
/// If you need cause-level detail, subscribe to the domain API directly.
///
/// ## Key Concepts
///
/// - **Outcome**: An instrument that emits `SUCCESS`/`FAIL`/`UNKNOWN` verdicts
/// - **Sign**: The verdict classification (`SUCCESS`, `FAIL`, `UNKNOWN`)
///
/// ## Usage Example
///
/// ```java
/// // Create an outcome instrument
/// var conduit = circuit.conduit(Sign.class);
/// var outcome = Outcomes.pool(conduit).get(cortex.name("payment.outcomes"));
///
/// // Emit verdicts
/// outcome.success();  // operation succeeded
/// outcome.fail();     // operation failed
/// outcome.unknown();  // indeterminate — abstains from success/failure tallies
/// ```
///
/// ## Relationship to Other APIs
///
/// - **Domain APIs**: Provide rich outcome vocabularies (SUCCESS/FAIL/EXPIRE/TIMEOUT/etc.)
/// - **Outcomes**: Aggregates to a verdict - did it work, fail, or remain indeterminate?
/// - **Statuses/Situations**: Consume outcome streams for condition/urgency translation
///
/// ## When to Use
///
/// Use Outcomes when you need:
/// - Cross-vocabulary success/failure aggregation
/// - Simple success rate metrics
/// - Binary health signals for Status translation
///
/// If you need to know *why* something failed, subscribe to the domain API.
/// Outcomes answers one question: **did it work?**
///
/// @author William David Louth
/// @since 1.0

@Utility
public final class Outcomes
  implements Serventis {

  /// The sign set of this API — the captured [Sign] constants from which caller-derived sign maps
  /// are mapped.

  public static final SignSet < Sign > SIGNS =
    SignSet.of (
      Sign.class
    );

  private Outcomes () { }

  /// Creates an Outcome instrument wrapping the specified pipe.
  ///
  /// @param pipe the pipe from which to create the outcome
  /// @return a new Outcome instrument for the specified pipe
  /// @throws NullPointerException if the pipe parameter is `null`

  @New
  @NotNull
  public static Outcome of (
    @NotNull final Pipe < ? super Sign > pipe
  ) {

    return
      new Outcome (
        pipe
      );

  }

  /// Returns a pool that creates cached Outcome instruments from a conduit.
  ///
  /// @param conduit the conduit providing sign pipes
  /// @return a pool that creates Outcome instruments
  /// @throws NullPointerException if the conduit parameter is `null`

  @New
  @NotNull
  public static Pool < Outcome > pool (
    @NotNull final Conduit < Sign > conduit
  ) {

    return
      conduit.pool (
        Outcomes::of
      );

  }


  /// A [Sign] represents the verdict of an operation — success, failure, or indeterminate.
  ///
  /// These signs form the minimal success/failure/unknown vocabulary. They enable
  /// universal aggregation across diverse domain APIs without requiring
  /// knowledge of source-specific outcome vocabularies.

  public enum Sign
    implements Serventis.Sign {

    /// Indicates the operation succeeded.
    ///
    /// Emitted when an operation completes successfully, regardless of
    /// the domain-specific success vocabulary (COMPLETE, COMMIT, GRANT, etc.).

    SUCCESS,

    /// Indicates the operation failed.
    ///
    /// Emitted when an operation fails, regardless of the domain-specific
    /// failure vocabulary (FAIL, TIMEOUT, EXPIRE, REJECT, DENY, ABORT, etc.).

    FAIL,

    /// Indicates the verdict is indeterminate — neither a clean success nor a definite failure. The
    /// sign **is** verdict-bearing; its polarity is merely unsettled (contention that will be retried, a
    /// detected conflict, an ambiguous disconnect, …). `UNKNOWN` abstains from success/failure tallies
    /// rather than forcing a polarity.
    ///
    /// Distinct, in a domain `OUTCOME` map, from a `null` result: `null` means the sign does **not**
    /// project onto the verdict axis at all (a non-verdict operation/lifecycle sign such as
    /// `Tasks.START` or `Locks.RELEASE`), whereas `UNKNOWN` means the sign *is* a verdict whose polarity
    /// is unsettled (`Transactions.CONFLICT`, `Locks.CONTEST`).

    UNKNOWN

  }


  /// The [Outcome] class emits verdict signals.
  ///
  /// ## Usage
  ///
  /// Use the semantic methods: `outcome.success()`, `outcome.fail()`, `outcome.unknown()`
  ///
  /// Outcomes provide the simplest possible verdict vocabulary for
  /// cross-domain aggregation and status translation.

  @Queued
  @Provided
  public static final class Outcome
    implements Signer < Sign > {

    private final Pipe < ? super Sign > pipe;

    private Outcome (
      final Pipe < ? super Sign > pipe
    ) {

      this.pipe = pipe;

    }

    /// Emits a FAIL verdict.

    public void fail () {

      pipe.emit (
        FAIL
      );

    }

    /// Emits the specified sign.
    ///
    /// @param sign the sign to emit

    @Override
    public void sign (
      @NotNull final Sign sign
    ) {

      pipe.emit (
        sign
      );

    }

    /// Emits a SUCCESS verdict.

    public void success () {

      pipe.emit (
        SUCCESS
      );

    }

    /// Emits an UNKNOWN (indeterminate) verdict.

    public void unknown () {

      pipe.emit (
        UNKNOWN
      );

    }

  }

}
