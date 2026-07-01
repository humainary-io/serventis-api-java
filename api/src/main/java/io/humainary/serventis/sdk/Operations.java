// Copyright (c) 2025 William David Louth

package io.humainary.serventis.sdk;

import io.humainary.serventis.api.Serventis;
import io.humainary.substrates.api.Substrates.Utility;

import static io.humainary.serventis.sdk.Operations.Sign.*;

/// # Operations API
///
/// The `Operations` API provides a minimal episode-structure vocabulary for expressing when an
/// operation opens, progresses, and closes. It enables systems to emit universal `BEGIN`/`ADVANCE`/`END`
/// signals that mark the *terminality* of an action across diverse domain vocabularies.
///
/// ## Purpose
///
/// This API offers the smallest possible vocabulary for universal span tracking. When domain APIs
/// (Services, Tasks, Transactions, etc.) have rich operation vocabularies (START, STOP, CALL, SUBMIT,
/// ACQUIRE, RELEASE, etc.), observers can project each sign onto a `BEGIN` (opens a span), `ADVANCE`
/// (a mid-span step that neither opens nor closes), or `END` (closes a span) using this API — tracking
/// action duration and nesting without requiring knowledge of source vocabularies. It is the
/// terminality axis the bracket sequencer walks (see `SEQUENCERS.md`).
///
/// ## Key Insight: Universal Bracketing
///
/// Every episode opens, may progress, and closes. `Operations` captures this universal three-phase
/// shape. Combined with a quality reading (a domain's `STATUS`, or `Outcomes` for a success/fail
/// tally), you get the complete picture:
/// - `BEGIN → END` with a healthy close (episode completed cleanly)
/// - `BEGIN → ADVANCE → END` (episode progressed through mid-span steps before closing)
/// - `BEGIN → END` with a degraded or defective close (episode completed unhealthily)
///
/// ## Key Concepts
///
/// - **Operation**: An instrument that emits `BEGIN`/`ADVANCE`/`END` episode brackets
/// - **Sign**: The bracket position (`BEGIN`, `ADVANCE`, `END`)
///
/// ## Usage Example
///
/// ```java
/// // Create an operation instrument
/// var conduit   = circuit.conduit(Sign.class);
/// var operation = Operations.pool(conduit).get(cortex.name("db.query"));
///
/// // Bracket an episode
/// operation.begin();     // episode opening
/// operation.advance();   // a mid-span step (optional, repeatable)
/// // ... perform work ...
/// operation.end();       // episode closing
/// ```
///
/// ## Relationship to Other APIs
///
/// - **Domain APIs**: Provide rich operation vocabularies (START/STOP, CALL/RETURN, etc.)
/// - **Operations**: Aggregates to a three-phase episode bracket - opened/advanced/closed
/// - **Outcomes**: Complements with verdict - did it work?
/// - **Together**: "I began X, it advanced, it ended, and it succeeded/failed"
///
/// ## When to Use
///
/// Use Operations when you need:
/// - Cross-vocabulary action span tracking
/// - Duration measurement (time between BEGIN and END)
/// - Nesting depth analysis
/// - Universal action counting
///
/// If you need to know *what kind* of action, subscribe to the domain API.
/// Operations answers one question: **when did it start and stop?**
///
/// @author William David Louth
/// @since 1.0

@Utility
public final class Operations
  implements Serventis {

  /// The sign set of this API — the captured [Sign] constants from which caller-derived sign maps
  /// are mapped.

  public static final SignSet < Sign > SIGNS =
    SignSet.of (
      Sign.class
    );

  private Operations () { }

  /// Creates an Operation instrument wrapping the specified pipe.
  ///
  /// @param pipe the pipe from which to create the operation
  /// @return a new Operation instrument for the specified pipe
  /// @throws NullPointerException if the pipe parameter is `null`

  @New
  @NotNull
  public static Operation of (
    @NotNull final Pipe < ? super Sign > pipe
  ) {

    return
      new Operation (
        pipe
      );

  }

  /// Returns a pool that creates cached Operation instruments from a conduit.
  ///
  /// @param conduit the conduit providing sign pipes
  /// @return a pool that creates Operation instruments
  /// @throws NullPointerException if the conduit parameter is `null`

  @New
  @NotNull
  public static Pool < Operation > pool (
    @NotNull final Conduit < Sign > conduit
  ) {

    return
      conduit.pool (
        Operations::of
      );

  }


  /// A [Sign] represents the bracket type of action.
  ///
  /// These signs form the minimal action vocabulary. They enable
  /// universal span tracking across diverse domain APIs without requiring
  /// knowledge of source-specific operation vocabularies.

  public enum Sign
    implements Serventis.Sign {

    /// Indicates an action is starting.
    ///
    /// Emitted when an operation begins, regardless of the domain-specific
    /// start vocabulary (START, CALL, SUBMIT, ACQUIRE, SPAWN, etc.).

    BEGIN,

    /// Indicates an action is progressing within an open episode — neither beginning nor ending it.
    ///
    /// Emitted for a mid-episode step, regardless of the domain-specific vocabulary (a granted hold, a
    /// retry, a suspend/resume, …). Lets the bracket vocabulary express the full
    /// `BEGIN → ADVANCE → END` episode, not just its endpoints.

    ADVANCE,

    /// Indicates an action is finishing.
    ///
    /// Emitted when an operation ends, regardless of the domain-specific
    /// end vocabulary (STOP, RETURN, COMPLETE, RELEASE, etc.).

    END

  }


  /// The [Operation] class emits episode bracket signals.
  ///
  /// ## Usage
  ///
  /// Use the semantic methods: `operation.begin()`, `operation.advance()`, `operation.end()`
  ///
  /// Operations provide the simplest possible episode-structure vocabulary for
  /// cross-domain span tracking and duration measurement.

  @Queued
  @Provided
  public static final class Operation
    implements Signer < Sign > {

    private final Pipe < ? super Sign > pipe;

    private Operation (
      final Pipe < ? super Sign > pipe
    ) {

      this.pipe = pipe;

    }

    /// Emits an ADVANCE step.

    public void advance () {

      pipe.emit (
        ADVANCE
      );

    }

    /// Emits a BEGIN bracket.

    public void begin () {

      pipe.emit (
        BEGIN
      );

    }

    /// Emits an END bracket.

    public void end () {

      pipe.emit (
        END
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

  }

}
