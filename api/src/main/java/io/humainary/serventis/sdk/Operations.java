// Copyright (c) 2025 William David Louth

package io.humainary.serventis.sdk;

import io.humainary.serventis.api.Serventis;

import static io.humainary.serventis.sdk.Operations.Sign.BEGIN;
import static io.humainary.serventis.sdk.Operations.Sign.END;

/// # Operations API
///
/// The `Operations` API provides a minimal action bracketing vocabulary for expressing
/// when operations start and finish. It enables systems to emit universal BEGIN/END signals
/// that mark action boundaries across diverse domain vocabularies.
///
/// ## Purpose
///
/// This API offers the smallest possible action vocabulary for universal span tracking.
/// When domain APIs (Services, Tasks, Transactions, etc.) have rich operation vocabularies
/// (START, STOP, CALL, SUBMIT, ACQUIRE, RELEASE, etc.), observers can emit simple BEGIN/END
/// brackets using this API to track action duration and nesting without requiring knowledge
/// of source vocabularies.
///
/// ## Key Insight: Universal Bracketing
///
/// Every action has a start and a finish. Operations captures this universal pattern.
/// Combined with Outcomes (SUCCESS/FAIL), you get the complete picture:
/// - BEGIN → END + SUCCESS (action completed successfully)
/// - BEGIN → END + FAIL (action completed with failure)
///
/// ## Key Concepts
///
/// - **Operation**: An instrument that emits BEGIN/END action brackets
/// - **Sign**: The bracket type (BEGIN, END)
///
/// ## Usage Example
///
/// ```java
/// // Create an operation instrument
/// var conduit   = circuit.conduit(Sign.class);
/// var operation = Operations.lookup(conduit).get(cortex.name("db.query"));
///
/// // Bracket an action
/// operation.begin();   // action starting
/// // ... perform work ...
/// operation.end();     // action finished
/// ```
///
/// ## Relationship to Other APIs
///
/// - **Domain APIs**: Provide rich operation vocabularies (START/STOP, CALL/RETURN, etc.)
/// - **Operations**: Aggregates to binary bracket - action started/finished
/// - **Outcomes**: Complements with verdict - did it work?
/// - **Together**: "I began X, it ended, and it succeeded/failed"
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

public final class Operations
  implements Serventis {

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

  /// Returns a lookup that creates cached Operation instruments from a conduit.
  ///
  /// @param conduit the conduit providing sign pipes
  /// @return a lookup that creates Operation instruments
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

    /// Indicates an action is finishing.
    ///
    /// Emitted when an operation ends, regardless of the domain-specific
    /// end vocabulary (STOP, RETURN, COMPLETE, RELEASE, etc.).

    END

  }


  /// The [Operation] class emits action bracket signals.
  ///
  /// ## Usage
  ///
  /// Use the semantic methods: `operation.begin()`, `operation.end()`
  ///
  /// Operations provide the simplest possible action vocabulary for
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
