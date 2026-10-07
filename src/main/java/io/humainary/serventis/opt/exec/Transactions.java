// Copyright (c) 2025 William David Louth

package io.humainary.serventis.opt.exec;

import io.humainary.serventis.api.Serventis;
import io.humainary.serventis.sdk.*;
import io.humainary.specs.api.Specs.SpecDoc;
import io.humainary.specs.api.Specs.SpecRef;
import io.humainary.substrates.api.Substrates.Utility;

import static io.humainary.serventis.sdk.Outcomes.Sign.FAIL;
import static io.humainary.serventis.sdk.Outcomes.Sign.SUCCESS;
import static io.humainary.serventis.sdk.Statuses.Sign.*;
import static java.util.Objects.requireNonNull;

/// # Transactions API
///
/// The `Transactions` API provides a comprehensive framework for observing transactional
/// semantics and distributed coordination patterns. It enables fine-grained instrumentation
/// of transaction lifecycle, state consistency operations, and coordination protocols including
/// two-phase commit (2PC), three-phase commit (3PC), saga patterns, and consensus algorithms.
///
/// ## Purpose
///
/// This API enables systems to emit **rich semantic signals** about transactional operations
/// and coordination states, capturing the complete lifecycle from initiation through resolution
/// (commit or rollback). The dual-dimension model (COORDINATOR/PARTICIPANT) enables observation
/// from both coordinator and cohort perspectives, essential for understanding distributed
/// transaction behavior.
///
/// ## Important: Observability vs Implementation
///
/// This API is for **reporting transaction semantics**, not implementing transaction protocols.
/// When your database, coordinator, or distributed system performs transactional operations,
/// use this API to emit observability signals. Meta-level observers can then reason about
/// transaction patterns, failure modes, and coordination reliability without coupling to
/// your protocol implementation details.
///
/// **Example**: When your coordinator starts a transaction, call `transaction.start(COORDINATOR)`.
/// When a participant prepares in answer to the prepare request, call
/// `transaction.prepare(PARTICIPANT)`. When the coordinator commits, call
/// `transaction.commit(COORDINATOR)`. These signals enable meta-observability of the transaction
/// network.
///
/// ## Key Concepts
///
/// - **Transaction**: A unit of work with ACID properties (Atomicity, Consistency, Isolation, Durability)
/// - **Signal**: A semantic event combining a **Sign** (what happened) and **Dimension** (perspective)
/// - **Sign**: The type of transaction operation (START, PREPARE, COMMIT, ROLLBACK, etc.)
/// - **Dimension**: The coordination perspective (COORDINATOR = transaction manager, PARTICIPANT = client)
/// - **Coordinator**: The initiator managing the transaction protocol (2PC leader, saga orchestrator)
/// - **Cohort**: A participant executing local operations under the transaction
///
/// ## Dual-Dimension Model
///
/// Every sign has two dimensions representing different roles in transaction coordination:
///
/// | Dimension   | Perspective        | Role            | Example Signals                                       |
/// |-------------|--------------------|-----------------|-------------------------------------------------------|
/// | COORDINATOR | Coordinator (self) | Protocol leader | START × COORDINATOR, COMMIT × COORDINATOR             |
/// | PARTICIPANT | Cohort (observed)  | Protocol member | PREPARE × PARTICIPANT (a vote), COMMIT × PARTICIPANT  |
///
/// **COORDINATOR** signals indicate "I am coordinating this transaction operation now" while
/// **PARTICIPANT** signals indicate "I observed/received this transaction operation from the
/// coordinator". This enables distributed transaction systems to observe coordination from
/// both coordinator and participant perspectives.
///
/// ## Transaction Lifecycle
///
/// Each step is reported by whichever party takes part in it; a coordinator's step and a
/// participant's view of the same step carry the same sign under different dimensions.
///
/// ### Standard Transaction Flow (2PC)
/// ```
/// Initiation:    START
///      ↓
/// Voting Phase:  PREPARE (the coordinator asks; each participant's PREPARE is its yes vote)
///      ↓
/// Decision:      COMMIT (if all yes) OR ROLLBACK (if any no)
///      ↓
/// Resolution:    (Transaction complete)
/// ```
///
/// ### Transaction with Expiration
/// ```
/// START → PREPARE → EXPIRE → ROLLBACK
/// ```
///
/// ### Transaction with Conflict
/// ```
/// START → PREPARE → CONFLICT → ABORT
/// ```
///
/// ### Saga Pattern (Compensation)
/// ```
/// START → (a step fails) → COMPENSATE → ROLLBACK
/// ```
///
/// ## Signal Categories
///
/// The API defines signals across transaction lifecycle phases:
///
/// ### Transaction Lifecycle
/// - **START**: Transaction initiation
/// - **PREPARE**: Voting phase (2PC prepare, can you commit?)
/// - **COMMIT**: Final commitment (all voted yes)
/// - **ROLLBACK**: Transaction abort (explicit rollback)
///
/// ### Error Conditions
/// - **ABORT**: Forced termination (e.g., deadlock detected)
/// - **EXPIRE**: Transaction exceeded time budget
/// - **CONFLICT**: Write conflict or constraint violation
///
/// ### Saga Pattern
/// - **COMPENSATE**: Compensating action (saga rollback)
///
/// ## Relationship to Other APIs
///
/// `Transactions` integrates with other Serventis APIs:
///
/// - **Services API**: Transactions often span service boundaries (distributed transactions)
/// - **Resources API**: Transactions may ACQUIRE/RELEASE locks or resources
/// - **Statuses API**: Transaction patterns inform conditions (ABORT → DEFECTIVE, ROLLBACK/EXPIRE/CONFLICT → DEGRADED)
/// - **Locks API**: Transactions coordinate with locking for isolation guarantees
///
/// ## Perspective Usage Patterns
///
/// ### Coordinator Perspective (COORDINATOR)
/// ```java
/// transaction.start ( COORDINATOR );      // I'm starting a transaction
/// transaction.prepare ( COORDINATOR );    // I'm asking participants to prepare
/// transaction.commit ( COORDINATOR );     // I'm committing the transaction
/// transaction.rollback ( COORDINATOR );   // I'm rolling back the transaction
/// ```
///
/// ### Participant Perspective (PARTICIPANT)
/// ```java
/// transaction.start ( PARTICIPANT );      // The coordinator started a transaction I take part in
/// transaction.prepare ( PARTICIPANT );    // I prepared (voted yes)
/// transaction.commit ( PARTICIPANT );     // The coordinator committed; I applied it
/// transaction.rollback ( PARTICIPANT );   // The coordinator rolled back; I discarded my changes
/// ```
///
/// ### Two-Phase Commit (2PC) Example
/// ```java
/// // Coordinator (Node A)
/// coordinatorTx.start ( COORDINATOR );    // Start transaction
/// coordinatorTx.prepare ( COORDINATOR );  // Send prepare to all participants
///
/// // Participant 1 (Node B)
/// participant1Tx.start ( PARTICIPANT );   // Received start
/// participant1Tx.prepare ( PARTICIPANT ); // Voted yes, ready to commit
///
/// // Participant 2 (Node C)
/// participant2Tx.start ( PARTICIPANT );   // Received start
/// participant2Tx.prepare ( PARTICIPANT ); // Voted yes, ready to commit
///
/// // Coordinator (Node A) - all voted yes
/// coordinatorTx.commit ( COORDINATOR );   // Send commit to all
///
/// // Participants apply it
/// participant1Tx.commit ( PARTICIPANT );  // Applied commit
/// participant2Tx.commit ( PARTICIPANT );  // Applied commit
/// ```
///
/// ### Saga Pattern Example (Compensation)
/// ```java
/// // Saga Orchestrator
/// saga.start ( COORDINATOR );             // Start saga
/// // ... steps execute ...
/// saga.compensate ( COORDINATOR );        // Step failed, compensating
/// saga.rollback ( COORDINATOR );          // Rolling back saga
///
/// // Saga Participant
/// sagaStep.start ( PARTICIPANT );         // Saga started
/// sagaStep.compensate ( PARTICIPANT );    // Compensation applied
/// sagaStep.rollback ( PARTICIPANT );      // Rollback complete
/// ```
///
/// ## Protocol Support
///
/// This API supports observation of multiple transaction protocols:
///
/// ### Two-Phase Commit (2PC)
/// - Coordinator: START → PREPARE → (all vote) → COMMIT/ROLLBACK
/// - Participants: START → PREPARE → COMMIT/ROLLBACK, each × PARTICIPANT
///
/// ### Three-Phase Commit (3PC)
/// - Same signals, but semantic interpretation includes pre-commit phase
/// - PREPARE phase is "can commit?", implicit pre-commit before COMMIT
///
/// ### Saga Pattern
/// - Coordinator: START → steps → (on failure) → COMPENSATE → ROLLBACK
/// - Participants: START → (on compensation) → COMPENSATE → ROLLBACK, each × PARTICIPANT
///
/// ### Paxos/Raft Consensus
/// - Leader: START → PREPARE → (quorum) → COMMIT
/// - Followers: START → PREPARE → COMMIT, each × PARTICIPANT
///
/// ## No Episode Map
///
/// Transactions publishes no `OPERATION` map (SPEC.md §B.2). A subject that names a database, a
/// coordinator or a participant sees many transactions at once, so their lifecycles interleave and a
/// bracket reading one trace per subject takes each concurrent transaction for an abandoned episode.
/// Only a subject per transaction would bracket, and nothing in the vocabulary makes a subject that
/// narrow.
///
/// ## Performance Considerations
///
/// See [io.humainary.serventis.api.Serventis.Signaler] for observation reuse and the
/// limits of performance guarantees. Measure the provider and pipeline for the intended workload.
///
/// ## Error Handling and Failure Modes
///
/// Transaction failures can occur at multiple stages:
///
/// - **EXPIRE**: Participant took too long to prepare/commit
/// - **CONFLICT**: Write conflict, constraint violation, or deadlock
/// - **ABORT**: Explicit abort due to business logic or system condition
///
/// The coordinator typically responds to failures by initiating ROLLBACK to restore
/// consistency. Participants report ROLLBACK × PARTICIPANT and undo their prepared changes.
///
/// ## Isolation Levels
///
/// This API is isolation-level agnostic. Whether your transaction uses:
/// - Read Uncommitted
/// - Read Committed
/// - Repeatable Read
/// - Serializable
///
/// ...the signal semantics remain the same. Higher-level analysis can correlate transaction
/// signals with CONFLICT rates to understand isolation behavior, but the API itself doesn't
/// encode isolation levels.
///
/// @author William David Louth
/// @since 1.0

@Utility
@SpecDoc ( "https://github.com/humainary-io/serventis-api-spec/blob/3.7.0/SPEC.md" )
@SpecRef ( {"8.2", "registry:transactions"} )
public final class Transactions
  implements Serventis {

  /// The sign set of this API — the captured [Sign] constants from which the canonical
  /// [#STATUS] and [#KIND] interpretations, and any caller-derived sign maps, are mapped.

  @SpecRef ( "4.5" )
  public static final SignSet < Sign > SIGNS =
    SignSet.of (
      Sign.class
    );

  /// The dimension set of this API — the captured [Dimension] constants used to build
  /// transaction signal instances and caller-derived signal maps.

  @SpecRef ( "4.5" )
  public static final SymbolSet < Dimension > DIMENSIONS =
    SymbolSet.of (
      Dimension.class
    );

  /// Canonical sign-to-status translation for transactions — the default *immediate interpretant* of
  /// the upward ascent (sign-keyed; the COORDINATOR/PARTICIPANT perspective does not change the
  /// reading; compose to override, see [SignMap]). A commit reads healthy; rollback, expiry, and
  /// conflict read degraded, while a forced `ABORT` (deadlock or constraint violation — an abnormal
  /// termination, distinct from a normal no-vote rollback) reads defective; the start/prepare
  /// lifecycle and saga compensation abstain. The
  /// commit/rollback *ratio* the prose calls for emerges from the Scorecard's plurality. The
  /// instrument emits `Signal`, so as a Scorecards ballot project the sign first —
  /// `Scorecards.flow ( STATUS.compose ( Signal::sign ) )`.
  ///
  /// Exhaustive without a `default`: a new [Sign] is a compile error here until its reading is decided.

  public static final SignMap < Sign, Statuses.Sign > STATUS =
    SIGNS.map (
      sign -> switch ( sign ) {
        case COMMIT -> STABLE;
        case ROLLBACK, EXPIRE, CONFLICT -> DEGRADED;
        case ABORT -> DEFECTIVE;
        case START, PREPARE, COMPENSATE -> null;
      }
    );

  /// Canonical sign-to-kind classification for transactions — each [Sign] tagged [Kind#OPERATION] or
  /// [Kind#OUTCOME]: the start/prepare lifecycle and the saga `COMPENSATE` are operations; commit,
  /// rollback, forced abort, expiry, and write-conflict are outcomes. The COORDINATOR/PARTICIPANT
  /// perspective does not change the kind. Exhaustive without a `default`. See [Kind].

  public static final SignMap < Sign, Kind > KIND =
    SIGNS.map (
      sign -> switch ( sign ) {
        case COMMIT, ROLLBACK, ABORT,
             EXPIRE, CONFLICT -> Kind.OUTCOME;
        case START, PREPARE, COMPENSATE -> Kind.OPERATION;
      }
    );


  /// Sign-to-[Outcomes] translation — the *outcome* half of the grammatical interlingua: a commit reads
  /// [Outcomes.Sign#SUCCESS]; a rollback, abort, or expiry reads [Outcomes.Sign#FAIL]; a conflict reads
  /// [Outcomes.Sign#UNKNOWN] (it may retry or abort). The operations abstain. Exhaustive without a
  /// `default`, consistent with [#KIND].

  public static final SignMap < Sign, Outcomes.Sign > OUTCOME =
    SIGNS.map (
      sign -> switch ( sign ) {
        case COMMIT -> SUCCESS;
        case ROLLBACK, ABORT, EXPIRE -> FAIL;
        case CONFLICT -> Outcomes.Sign.UNKNOWN;
        case START, PREPARE, COMPENSATE -> null;
      }
    );

  private Transactions () { }

  /// Creates a Transaction instrument wrapping the specified pipe.
  ///
  /// @param pipe the pipe from which to create the transaction
  /// @return a new Transaction instrument for the specified pipe
  /// @throws NullPointerException if the pipe parameter is `null`

  @SpecRef ( "6.4" )
  @New
  @NotNull
  public static Transaction of (
    @NotNull final Pipe < ? super Signal > pipe
  ) {

    return
      new Transaction (
        requireNonNull ( pipe )
      );

  }

  /// Returns a pool that creates cached Transaction instruments from a conduit.
  ///
  /// Within the returned pool, repeated lookup of the same name returns the same instrument,
  /// created on first lookup. Separate pools have separate identity guarantees.
  ///
  /// @param conduit the conduit providing signal pipes
  /// @return a pool that creates Transaction instruments
  /// @throws NullPointerException if the conduit parameter is `null`

  @SpecRef ( {"6.4", "substrates:10.1"} )
  @New
  @NotNull
  public static Pool < Transaction > pool (
    @NotNull final Conduit < Signal > conduit
  ) {

    return
      conduit.pool (
        Transactions::of
      );

  }

  /// A [Sign] classifies transaction operations that occur during distributed coordination.
  /// These classifications enable analysis of transaction patterns, failure modes, and
  /// coordination protocol behavior in distributed systems.
  ///
  /// ## Sign Categories
  ///
  /// Signs are organized into functional categories representing different aspects
  /// of transaction coordination:
  ///
  /// - **Lifecycle**: START, PREPARE, COMMIT, ROLLBACK
  /// - **Error Conditions**: ABORT, EXPIRE, CONFLICT
  /// - **Compensation**: COMPENSATE

  @SpecRef ( {"4.2", "registry:transactions"} )
  public enum Sign
    implements Serventis.Sign {

    /// Indicates transaction initiation.
    ///
    /// START marks the start of a transactional unit of work. In distributed systems,
    /// the coordinator starts the transaction and participants observe the start.
    /// The transaction is now in-flight and will eventually reach a terminal state
    /// (COMMIT or ROLLBACK).
    ///
    /// **Typical usage**: Starting a database transaction, beginning a saga, initiating 2PC
    ///
    /// **Protocols**: All transaction protocols (2PC, 3PC, Saga, Paxos, Raft)

    START,

    /// Indicates the voting/prepare phase of two-phase commit.
    ///
    /// PREPARE represents the coordinator asking participants "Can you commit?" and
    /// participants responding with their vote (yes = prepared, no = abort). In 2PC,
    /// this is the critical synchronization point where participants durably record
    /// their intent to commit but have not yet committed.
    ///
    /// **Typical usage**: 2PC prepare phase, Paxos propose, Raft log replication
    ///
    /// **Protocols**: 2PC, 3PC, Paxos, Raft (voting/consensus building)

    PREPARE,

    /// Indicates transaction commitment.
    ///
    /// COMMIT represents the coordinator deciding to commit (all participants voted yes)
    /// and participants durably applying the transaction. After COMMIT, the transaction's
    /// effects are permanent and visible. This is the positive terminal state.
    ///
    /// **Typical usage**: 2PC commit phase, saga completion, Paxos/Raft commit
    ///
    /// **Protocols**: All transaction protocols (positive resolution)

    COMMIT,

    /// Indicates transaction rollback.
    ///
    /// ROLLBACK represents the coordinator deciding to abort (at least one participant
    /// voted no or expiration occurred) and participants undoing their prepared changes.
    /// After ROLLBACK, the transaction's effects are erased and the system returns to
    /// the state before START. This is the negative terminal state.
    ///
    /// **Typical usage**: 2PC abort phase, saga rollback, explicit transaction abort
    ///
    /// **Protocols**: All transaction protocols (negative resolution)

    ROLLBACK,

    /// Indicates forced transaction termination.
    ///
    /// ABORT represents an explicit abort condition detected by the coordinator or
    /// participant, such as deadlock detection, business logic rejection, or system
    /// constraint violation. Unlike ROLLBACK (which can be a normal response to a no vote),
    /// ABORT indicates an abnormal condition requiring immediate termination.
    ///
    /// **Typical usage**: Deadlock abort, constraint violation, business rule rejection
    ///
    /// **Protocols**: Database transactions, distributed deadlock detection

    ABORT,

    /// Indicates transaction expiration.
    ///
    /// EXPIRE represents the transaction exceeding its time budget. In distributed
    /// systems, expiration prevents indefinite blocking when participants fail or become
    /// unreachable. Coordinator typically responds to EXPIRE by initiating ROLLBACK.
    ///
    /// **Typical usage**: Participant prepare expiry, commit expiry, network partition
    ///
    /// **Protocols**: 2PC, 3PC (failure detection), distributed systems with time budgets

    EXPIRE,

    /// Indicates write conflict or constraint violation.
    ///
    /// CONFLICT represents a conflict detected during transaction execution, such as
    /// write-write conflict (optimistic locking), serialization failure, or constraint
    /// violation. Coordinator typically responds to CONFLICT by initiating ROLLBACK or
    /// ABORT. High CONFLICT rates may indicate contention or isolation level issues.
    ///
    /// **Typical usage**: Optimistic locking conflict, serialization failure, unique constraint
    ///
    /// **Protocols**: Database transactions, optimistic concurrency control, MVCC

    CONFLICT,

    /// Indicates compensating action in saga pattern.
    ///
    /// COMPENSATE represents a compensating transaction that undoes the effects of a
    /// previously committed local transaction within a saga. Unlike ROLLBACK (which undoes
    /// uncommitted changes), COMPENSATE semantically reverses committed changes through
    /// explicit compensation logic.
    ///
    /// **Typical usage**: Saga rollback, compensating transaction, semantic undo
    ///
    /// **Protocols**: Saga pattern, choreography-based sagas, orchestration-based sagas

    COMPENSATE

  }


  /// Dimension of transaction observation representing the role in distributed coordination.
  ///
  /// In distributed transaction protocols (2PC, 3PC, Paxos, Raft, Saga), there are two
  /// fundamental roles: the coordinator/initiator and the participants/cohorts. The dimension
  /// classifies whether signals represent operations initiated by the coordinator or observations
  /// by participants. This dual-perspective model enables complete observability of transaction
  /// coordination from both sides of the protocol.
  ///
  /// ## The Two Perspectives
  ///
  /// | Dimension   | Perspective        | Role           | Example                          |
  /// |-------------|--------------------|----------------|----------------------------------|
  /// | COORDINATOR | Coordinator (self) | Protocol leader| "I am committing"                |
  /// | PARTICIPANT | Cohort (observed)  | Protocol member| "Coordinator committed"          |
  ///
  /// ## COORDINATOR vs PARTICIPANT
  ///
  /// **COORDINATOR** signals represent **operations the coordinator is performing**:
  /// - Generated by the transaction coordinator/leader
  /// - Present-tense semantics ("I start", "I prepare", "I commit")
  /// - Used for protocol orchestration and decision reporting
  /// - Forms the basis for understanding coordinator behavior
  ///
  /// **PARTICIPANT** signals represent **operations participants observe/perform**:
  /// - Generated by transaction participants/cohorts
  /// - Observed or self-reported semantics ("Coordinator started", "I prepared", "Coordinator committed")
  /// - Used for participant state tracking and vote reporting
  /// - Forms the basis for understanding participant behavior
  ///
  /// ## Protocol Flow Example: Two-Phase Commit
  ///
  /// ### Coordinator (Node A) - COORDINATOR perspective
  /// ```java
  /// transaction.start ( COORDINATOR );      // I'm starting transaction T1
  /// transaction.prepare ( COORDINATOR );    // I'm sending prepare to all participants
  /// // ... wait for votes ...
  /// transaction.commit ( COORDINATOR );     // All voted yes, I'm committing
  /// ```
  ///
  /// ### Participant (Node B) - PARTICIPANT perspective
  /// ```java
  /// transaction.start ( PARTICIPANT );      // Coordinator started transaction T1
  /// transaction.prepare ( PARTICIPANT );    // I prepared and voted yes
  /// transaction.commit ( PARTICIPANT );     // Coordinator committed, I applied changes
  /// ```
  ///
  /// ### Participant (Node C) - PARTICIPANT perspective with failure
  /// ```java
  /// transaction.start ( PARTICIPANT );      // Coordinator started transaction T1
  /// transaction.conflict ( PARTICIPANT );   // I detected a conflict, voting no
  /// transaction.rollback ( PARTICIPANT );   // Coordinator rolled back, I discarded changes
  /// ```
  ///
  /// ## Temporal Semantics
  ///
  /// - **COORDINATOR**: Present tense, happening **now**, coordinator decision/action
  /// - **PARTICIPANT**: Mixed tense, either observed (coordinator action) or self-reported (vote)
  ///
  /// The temporal distinction is crucial for understanding protocol causality. START × PARTICIPANT
  /// reports that the coordinator initiated something earlier, while PREPARE × PARTICIPANT is the
  /// participant reporting its own vote.
  ///
  /// ## Use in Distributed Protocols
  ///
  /// The dual-dimension model enables:
  /// - **Protocol observability**: Tracking coordinator decisions and participant responses
  /// - **Failure analysis**: Identifying which participants expire, conflict, or fail
  /// - **Latency analysis**: Measuring time between PREPARE × COORDINATOR and PREPARE × PARTICIPANT
  /// - **Consistency verification**: Ensuring all participants observe coordinator decisions
  /// - **Deadlock detection**: Observing ABORT patterns across participants

  @SpecRef ( {"4.3", "registry:transactions"} )
  public enum Dimension
    implements Category {

    /// The emission of a transaction signal from the coordinator's perspective.
    ///
    /// COORDINATOR represents **operations the transaction coordinator is performing right now**.
    /// Use COORDINATOR when the local node is the transaction coordinator/leader orchestrating
    /// the protocol (2PC coordinator, Paxos leader, Raft leader, saga orchestrator, database engine).
    ///
    /// In transaction protocol terms, this node is the source of coordination decisions,
    /// even when those decisions are to ROLLBACK or ABORT.
    ///
    /// **Mental model**: "I am the transaction manager coordinating this operation now"
    /// **Examples**: START, PREPARE, COMMIT, ROLLBACK, ABORT
    /// **Usage**: Protocol orchestration, decision reporting, coordinator telemetry
    /// **Note**: Application code using JDBC would typically use PARTICIPANT, not COORDINATOR

    COORDINATOR,

    /// The reception of a transaction signal from a participant's perspective.
    ///
    /// PARTICIPANT represents **observations or actions from the participant/cohort perspective**.
    /// Use PARTICIPANT when observing coordinator operations or reporting participant actions
    /// within a transaction. This is the dimension most application code will use.
    ///
    /// In transaction protocol terms, this node is a cohort receiving coordinator messages
    /// or reporting its own vote/state within the distributed protocol.
    ///
    /// **Mental model**: "I am a client/participant in this transaction"
    /// **Examples**: START, PREPARE (a vote), COMMIT, ROLLBACK, CONFLICT — each × PARTICIPANT
    /// **Usage**: Participant state tracking, vote reporting, cohort telemetry, application code
    /// **Note**: JDBC applications, REST clients, and most app code should use PARTICIPANT

    PARTICIPANT

  }

  /// The [Signal] record represents one transaction step as reported from one perspective.
  ///
  /// @param sign      the transaction step
  /// @param dimension the perspective it is reported from, coordinator or participant

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

  /// The `Transaction` class represents a transactional unit of work in a distributed system.
  /// A transaction is an observable entity that emits signals about its lifecycle, coordination
  /// state, and resolution (commit or rollback).
  ///
  /// ## Usage
  ///
  /// Use domain-specific methods for all transaction lifecycle events, specifying the dimension
  /// (role) as a parameter:
  ///
  /// ```java
  /// // A coordinator (e.g., database engine, saga orchestrator) emits:
  /// transaction.start(Transactions.Dimension.COORDINATOR);
  /// transaction.prepare(Transactions.Dimension.COORDINATOR);
  /// transaction.commit(Transactions.Dimension.COORDINATOR);
  ///
  /// // A participant (e.g., JDBC application, REST client) emits:
  /// transaction.start(Transactions.Dimension.PARTICIPANT);
  /// transaction.prepare(Transactions.Dimension.PARTICIPANT);
  /// transaction.commit(Transactions.Dimension.PARTICIPANT);
  /// ```

  @SpecRef ( {"6.2", "6.3", "6.5", "substrates:6.1"} )
  @Queued
  @Provided
  public static final class Transaction
    implements Signaler < Sign, Dimension > {

    private static final SignalSet < Sign, Dimension, Signal > SIGNALS =
      SIGNS.signals (
        DIMENSIONS,
        Signal::new
      );

    private final Pipe < ? super Signal > pipe;

    private Transaction (
      final Pipe < ? super Signal > pipe
    ) {

      this.pipe =
        pipe;

    }

    /// Emits an `ABORT` sign with the specified dimension.
    ///
    /// @param dimension the role perspective of the signal emission
    /// @throws NullPointerException if the dimension is `null`

    public void abort (
      @NotNull final Dimension dimension
    ) {

      pipe.emit (
        SIGNALS.get (
          Sign.ABORT,
          dimension
        )
      );

    }

    /// Emits a `COMMIT` sign with the specified dimension.
    ///
    /// @param dimension the role perspective of the signal emission
    /// @throws NullPointerException if the dimension is `null`

    public void commit (
      @NotNull final Dimension dimension
    ) {

      pipe.emit (
        SIGNALS.get (
          Sign.COMMIT,
          dimension
        )
      );

    }

    /// Emits a `COMPENSATE` sign with the specified dimension.
    ///
    /// @param dimension the role perspective of the signal emission
    /// @throws NullPointerException if the dimension is `null`

    public void compensate (
      @NotNull final Dimension dimension
    ) {

      pipe.emit (
        SIGNALS.get (
          Sign.COMPENSATE,
          dimension
        )
      );

    }

    /// Emits a `CONFLICT` sign with the specified dimension.
    ///
    /// @param dimension the role perspective of the signal emission
    /// @throws NullPointerException if the dimension is `null`

    public void conflict (
      @NotNull final Dimension dimension
    ) {

      pipe.emit (
        SIGNALS.get (
          Sign.CONFLICT,
          dimension
        )
      );


    }

    /// Emits an `EXPIRE` sign with the specified dimension.
    ///
    /// @param dimension the role perspective of the signal emission
    /// @throws NullPointerException if the dimension is `null`

    public void expire (
      @NotNull final Dimension dimension
    ) {

      pipe.emit (
        SIGNALS.get (
          Sign.EXPIRE,
          dimension
        )
      );


    }

    /// Emits a `PREPARE` sign with the specified dimension.
    ///
    /// @param dimension the role perspective of the signal emission
    /// @throws NullPointerException if the dimension is `null`

    public void prepare (
      @NotNull final Dimension dimension
    ) {

      pipe.emit (
        SIGNALS.get (
          Sign.PREPARE,
          dimension
        )
      );


    }

    /// Emits a `ROLLBACK` sign with the specified dimension.
    ///
    /// @param dimension the role perspective of the signal emission
    /// @throws NullPointerException if the dimension is `null`

    public void rollback (
      @NotNull final Dimension dimension
    ) {

      pipe.emit (
        SIGNALS.get (
          Sign.ROLLBACK,
          dimension
        )
      );


    }

    /// Signals a transaction event by composing sign and dimension.
    ///
    /// @param sign      the sign component
    /// @param dimension the dimension component

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

    /// Emits a `START` sign with the specified dimension.
    ///
    /// @param dimension the role perspective of the signal emission
    /// @throws NullPointerException if the dimension is `null`

    public void start (
      @NotNull final Dimension dimension
    ) {

      pipe.emit (
        SIGNALS.get (
          Sign.START,
          dimension
        )
      );


    }

  }

}
