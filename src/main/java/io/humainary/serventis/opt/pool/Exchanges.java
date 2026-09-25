// Copyright (c) 2025 William David Louth

package io.humainary.serventis.opt.pool;

import io.humainary.serventis.api.Serventis;
import io.humainary.serventis.sdk.SignMap;
import io.humainary.serventis.sdk.SignSet;
import io.humainary.serventis.sdk.SignalSet;
import io.humainary.serventis.sdk.SymbolSet;
import io.humainary.specs.api.Specs.SpecDoc;
import io.humainary.specs.api.Specs.SpecRef;
import io.humainary.substrates.api.Substrates.Utility;

import static io.humainary.serventis.api.Serventis.Kind.OPERATION;
import static java.util.Objects.requireNonNull;

/// # Exchanges API
///
/// The `Exchanges` API provides a structured framework for observing resource exchanges
/// between parties. It enables emission of semantic signals that describe bilateral
/// transfers of resources, supporting both economic exchange patterns (REA model) and
/// synchronization rendezvous patterns (Java Exchanger).
///
/// ## Purpose
///
/// This API enables systems to **observe** exchange interactions where resources move
/// between a provider and receiver. The dual-perspective model captures both sides of
/// the exchange, enabling complete observability of bilateral transfers.
///
/// ## Important: Reporting vs Implementation
///
/// This API is for **reporting exchange semantics**, not implementing exchange mechanisms.
/// When your system performs exchanges (trades, swaps, transfers between parties), use
/// this API to emit observability signals. Observer agents can then reason about exchange
/// patterns, completion rates, and transfer dynamics.
///
/// ## Theoretical Foundation
///
/// The API is grounded in the **REA (Resource-Event-Agent)** accounting model:
///
/// - **Resource**: Economic things of value being exchanged
/// - **Event**: The exchange event (CONTRACT, TRANSFER)
/// - **Agent**: Parties participating (PROVIDER, RECEIVER)
///
/// The fundamental insight is that economic activity consists of **dual exchanges**:
/// every give implies a take (conservation), creating reciprocal flows between agents.
///
/// ## Key Concepts
///
/// - **Exchange**: A named bilateral transfer of resources between parties
/// - **Sign**: The phase of exchange (CONTRACT, TRANSFER)
/// - **Dimension**: The perspective (PROVIDER giving, RECEIVER taking)
///
/// ## Exchange Patterns
///
/// ### REA Economic Exchange
///
/// ```
/// CONTRACT × PROVIDER  →  CONTRACT × RECEIVER    (commit to exchange)
/// TRANSFER × PROVIDER  →  TRANSFER × RECEIVER    (fulfill exchange)
/// ```
///
/// ### Java Exchanger Rendezvous
///
/// Both threads arrive at the exchange point and swap data:
///
/// ```
/// Thread 1: CONTRACT × PROVIDER  (arrive with data)
/// Thread 2: CONTRACT × PROVIDER  (arrive with data)
/// Thread 1: TRANSFER × PROVIDER, TRANSFER × RECEIVER  (swap)
/// Thread 2: TRANSFER × PROVIDER, TRANSFER × RECEIVER  (swap)
/// ```
///
/// ## Signal Matrix
///
/// | Sign       | PROVIDER              | RECEIVER                |
/// |------------|-----------------------|-------------------------|
/// | CONTRACT   | I contract to provide | I contract to receive   |
/// | TRANSFER   | I transfer out        | I receive transfer      |
///
/// ## Relationship to Other APIs
///
/// - **Resources API**: Exchanges transfer resources; Resources tracks acquisition/release
/// - **Agents API**: Agents make promises; Exchanges fulfill transfers
/// - **Leases API**: Leases grant time-bounded access; Exchanges transfer ownership
/// - **Statuses API**: Exchange failure patterns may indicate DEGRADED conditions
///
/// ## Performance Considerations
///
/// See [io.humainary.serventis.api.Serventis.Signaler] for observation reuse and the
/// limits of performance guarantees. Measure the provider and pipeline for the intended workload.
///
/// @author William David Louth
/// @since 1.0

@Utility
@SpecDoc ( "https://github.com/humainary-io/serventis-api-spec/blob/3.6.0/SPEC.md" )
@SpecRef ( {"8.2", "registry:exchanges"} )
public final class Exchanges
  implements Serventis {

  /// The sign set of this API — the captured [Sign] constants from which the canonical [#KIND]
  /// interpretation, and any caller-derived sign maps, are mapped.

  @SpecRef ( "4.5" )
  public static final SignSet < Sign > SIGNS =
    SignSet.of (
      Sign.class
    );

  /// The dimension set of this API — the captured [Dimension] constants used to build
  /// exchange signal instances and caller-derived signal maps.

  @SpecRef ( "4.5" )
  public static final SymbolSet < Dimension > DIMENSIONS =
    SymbolSet.of (
      Dimension.class
    );

  /// Canonical sign-to-kind classification for exchanges — both signs are [Kind#OPERATION]:
  /// contracting and transferring are acts a party performs, and the PROVIDER/RECEIVER perspective
  /// does not change the kind. Exchanges has no outcomes of its own. Exhaustive without a `default`.
  /// See [Kind].

  public static final SignMap < Sign, Kind > KIND =
    SIGNS.map (
      sign -> switch ( sign ) {
        case CONTRACT, TRANSFER -> OPERATION;
      }
    );

  private Exchanges () { }

  /// Creates an Exchange instrument wrapping the specified pipe.
  ///
  /// @param pipe the pipe from which to create the exchange
  /// @return a new Exchange instrument for the specified pipe
  /// @throws NullPointerException if the pipe parameter is `null`

  @SpecRef ( "6.4" )
  @New
  @NotNull
  public static Exchange of (
    @NotNull final Pipe < ? super Signal > pipe
  ) {

    return
      new Exchange (
        requireNonNull ( pipe )
      );

  }

  /// Returns a pool that creates cached Exchange instruments from a conduit.
  ///
  /// Within the returned pool, repeated lookup of the same name returns the same instrument,
  /// created on first lookup. Separate pools have separate identity guarantees.
  ///
  /// @param conduit the conduit providing signal pipes
  /// @return a pool that creates Exchange instruments
  /// @throws NullPointerException if the conduit parameter is `null`

  @SpecRef ( {"6.4", "substrates:10.1"} )
  @New
  @NotNull
  public static Pool < Exchange > pool (
    @NotNull final Conduit < Signal > conduit
  ) {

    return
      conduit.pool (
        Exchanges::of
      );

  }


  /// The [Dimension] enum represents the perspective in an exchange.
  ///
  /// Every exchange has two sides: the provider (giving) and the receiver (taking).
  /// This dual-perspective model enables complete observability of bilateral transfers.

  @SpecRef ( {"4.3", "registry:exchanges"} )
  public enum Dimension
    implements Category {

    /// The giving perspective in an exchange.
    ///
    /// PROVIDER signals indicate actions from the party transferring resources out.
    /// Use PROVIDER when reporting "I am providing/giving/transferring out."

    PROVIDER,

    /// The receiving perspective in an exchange.
    ///
    /// RECEIVER signals indicate actions from the party receiving resources in.
    /// Use RECEIVER when reporting "I am receiving/taking/accepting transfer."

    RECEIVER

  }


  /// The [Sign] enum represents the phases of an exchange.
  ///
  /// Exchanges proceed through phases: parties first CONTRACT (commit to exchange),
  /// then TRANSFER (resources change hands). This minimal vocabulary captures the
  /// essential exchange lifecycle.

  @SpecRef ( {"4.2", "registry:exchanges"} )
  public enum Sign
    implements Serventis.Sign {

    /// Commit to participate in an exchange.
    ///
    /// CONTRACT signals indicate a party has committed to the exchange.
    /// For REA: agreeing to terms. For Exchanger: arriving at rendezvous.
    ///
    /// - CONTRACT × PROVIDER: "I contract to provide"
    /// - CONTRACT × RECEIVER: "I contract to receive"

    CONTRACT,

    /// Resource changes hands.
    ///
    /// TRANSFER signals indicate resources are moving between parties.
    /// For REA: fulfilling the exchange. For Exchanger: the swap occurs.
    ///
    /// - TRANSFER × PROVIDER: "I transfer out"
    /// - TRANSFER × RECEIVER: "I receive transfer"

    TRANSFER

  }

  /// The [Exchange] class emits exchange observations.
  ///
  /// An Exchange instrument provides methods for signaling exchange phases
  /// from both provider and receiver perspectives.
  ///
  /// ## Usage
  ///
  /// ```java
  /// // Provider commits to exchange
  /// exchange.contract(PROVIDER);
  ///
  /// // Receiver commits to exchange
  /// exchange.contract(RECEIVER);
  ///
  /// // Provider transfers out
  /// exchange.transfer(PROVIDER);
  ///
  /// // Receiver receives transfer
  /// exchange.transfer(RECEIVER);
  /// ```

  @SpecRef ( {"6.2", "6.3", "6.5", "substrates:6.1"} )
  @Queued
  @Provided
  public static final class Exchange
    implements Signaler < Sign, Dimension > {

    private static final SignalSet < Sign, Dimension, Signal > SIGNALS =
      SIGNS.signals (
        DIMENSIONS,
        Signal::new
      );

    private final Pipe < ? super Signal > pipe;

    private Exchange (
      final Pipe < ? super Signal > pipe
    ) {

      this.pipe =
        pipe;

    }

    /// Signals a CONTRACT phase from the specified perspective.
    ///
    /// @param dimension the party perspective (PROVIDER or RECEIVER)

    public void contract (
      @NotNull final Dimension dimension
    ) {

      signal (
        Sign.CONTRACT,
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

    /// Signals a TRANSFER phase from the specified perspective.
    ///
    /// @param dimension the party perspective (PROVIDER or RECEIVER)

    public void transfer (
      @NotNull final Dimension dimension
    ) {

      signal (
        Sign.TRANSFER,
        dimension
      );

    }

  }

  /// The [Signal] record represents an exchange observation.
  ///
  /// Each signal combines a sign (CONTRACT, TRANSFER) with a dimension
  /// (PROVIDER, RECEIVER) to capture the complete exchange event.
  ///
  /// @param sign      the exchange phase
  /// @param dimension the party perspective

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
