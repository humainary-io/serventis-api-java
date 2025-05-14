// Copyright (c) 2025 William David Louth

package io.humainary.modules.serventis.probes.api;

import static io.humainary.substrates.api.Substrates.*;

/// The `Probes` API provides a structured framework for monitoring and reporting
/// communication outcomes in distributed systems. It enables precise observation
/// of operations across client-server boundaries.
///
/// ## Key Concepts
///
/// This API is built around three core dimensions:
/// - **Outcome**: What happened (success or failure)
/// - **Origin**: Where it happened (client or server)
/// - **Operation**: What activity was occurring (connect, send, receive, process)
///
/// By combining these dimensions, the API enables detailed diagnostics and monitoring
/// of distributed communication patterns, allowing systems to detect, report, and
/// respond to various conditions that may arise during network operations.
///
/// @since 1.0

public interface Probes
  extends Composer < Probes.Probe, Probes.Observation > {

  /// An `Observation` is a record of a communication event, capturing:
  /// - What happened (outcome)
  /// - Where it happened (origin)
  /// - What was happening (operation)
  ///
  /// Observations provide a structured way to describe and analyze the behavior
  /// of distributed systems, enabling detailed monitoring and diagnostics.

  @Provided
  interface Observation {

    /// Returns the type of operation being observed.
    ///
    /// The operation identifies what activity was occurring when the observation
    /// was made, providing context about the communication phase.
    ///
    /// @return the type of operation (CONNECT, SEND, RECEIVE, or PROCESS) being observed

    @NotNull
    Operation operation ();


    /// Returns the origin where the observation was made.
    ///
    /// The origin identifies the location within the distributed system
    /// where the observation occurred, helping to pinpoint responsibility
    /// and understand the communication flow.
    ///
    /// @return the origin (CLIENT or SERVER) where the observation was made

    @NotNull
    Origin origin ();


    /// Returns the outcome of the observed operation.
    ///
    /// The outcome indicates whether the operation succeeded or failed,
    /// providing the most fundamental assessment of the observation.
    ///
    /// @return the outcome (SUCCESS or FAILURE) of the observed operation

    @NotNull
    Outcome outcome ();

  }


  /// A `Probe` is an instrument that emits observations about communication operations.
  /// It serves as the primary reporting mechanism within the Probes API.
  ///
  /// Probes can be attached to various components within a distributed system to monitor
  /// and report on communication, health, and behavior.
  /// Each probe emits observations that capture the outcome, origin, and type of operation being performed.

  @Provided
  interface Probe
    extends Pipe < Observation > {

    /// Emits a CLIENT observation with the specified outcome and operation.
    ///
    /// This is a convenience method that automatically sets the origin to CLIENT.
    ///
    /// @param outcome   the outcome of the operation (SUCCESS or FAILURE)
    /// @param operation the type of operation being observed (CONNECT, SEND, RECEIVE, or PROCESS)
    /// @throws NullPointerException if any parameter is null

    default void client (
      @NotNull final Operation operation,
      @NotNull final Outcome outcome
    ) {

      observation (
        Origin.CLIENT,
        operation,
        outcome
      );

    }


    /// Emits a CLOSE operation observation with the specified outcome and origin.
    ///
    /// This is a convenience method that automatically sets the operation to CLOSE.
    ///
    /// @param outcome the outcome of the operation (SUCCESS or FAILURE)
    /// @param origin  the origin where the observation was made (CLIENT or SERVER)
    /// @throws NullPointerException if any parameter is null

    default void close (
      @NotNull final Origin origin,
      @NotNull final Outcome outcome
    ) {

      observation (
        origin,
        Operation.CLOSE,
        outcome
      );

    }


    /// Emits a CONNECT operation observation with the specified outcome and origin.
    ///
    /// This is a convenience method that automatically sets the operation to CONNECT.
    ///
    /// @param outcome the outcome of the operation (SUCCESS or FAILURE)
    /// @param origin  the origin where the observation was made (CLIENT or SERVER)
    /// @throws NullPointerException if any parameter is null

    default void connect (
      @NotNull final Origin origin,
      @NotNull final Outcome outcome
    ) {

      observation (
        origin,
        Operation.CONNECT,
        outcome
      );

    }


    /// Emits an observation.
    ///
    /// This method is useful when the observation has been created elsewhere.
    ///
    /// @param observation the complete observation to emit
    /// @throws NullPointerException if observation is null

    @Override
    void emit (
      @NotNull Observation observation
    );


    /// Emits a FAILURE observation for the specified origin and operation.
    ///
    /// This is a convenience method that automatically sets the outcome to FAILURE.
    ///
    /// @param origin    the origin where the observation was made (CLIENT or SERVER)
    /// @param operation the type of operation being observed (CONNECT, SEND, RECEIVE, or PROCESS)
    /// @throws NullPointerException if any parameter is null

    default void failure (
      @NotNull final Origin origin,
      @NotNull final Operation operation
    ) {

      observation (
        origin,
        operation,
        Outcome.FAILURE
      );

    }


    /// Emits an observation constructed from individual components.
    ///
    /// This method constructs an observation from the specified origin, operation,
    /// and outcome, then emits it.
    ///
    /// @param origin    the origin where the observation was made (CLIENT or SERVER)
    /// @param operation the type of operation being observed (CONNECT, SEND, RECEIVE, or PROCESS)
    /// @param outcome   the outcome of the operation (SUCCESS or FAILURE)
    /// @throws NullPointerException if any parameter is null

    void observation (
      @NotNull Origin origin,
      @NotNull Operation operation,
      @NotNull Outcome outcome
    );


    /// Emits a PROCESS operation observation with the specified outcome and origin.
    ///
    /// This is a convenience method that automatically sets the operation to PROCESS.
    ///
    /// @param outcome the outcome of the operation (SUCCESS or FAILURE)
    /// @param origin  the origin where the observation was made (CLIENT or SERVER)
    /// @throws NullPointerException if any parameter is null

    default void process (
      @NotNull final Origin origin,
      @NotNull final Outcome outcome
    ) {

      observation (
        origin,
        Operation.PROCESS,
        outcome
      );

    }


    /// Emits a RECEIVE operation observation with the specified outcome and origin.
    ///
    /// This is a convenience method that automatically sets the operation to RECEIVE.
    ///
    /// @param outcome the outcome of the operation (SUCCESS or FAILURE)
    /// @param origin  the origin where the observation was made (CLIENT or SERVER)
    /// @throws NullPointerException if any parameter is null

    default void receive (
      @NotNull final Origin origin,
      @NotNull final Outcome outcome
    ) {

      observation (
        origin,
        Operation.RECEIVE,
        outcome
      );

    }


    /// Emits a SEND operation observation with the specified outcome and origin.
    ///
    /// This is a convenience method that automatically sets the operation to SEND.
    ///
    /// @param outcome the outcome of the operation (SUCCESS or FAILURE)
    /// @param origin  the origin where the observation was made (CLIENT or SERVER)
    /// @throws NullPointerException if any parameter is null

    default void send (
      @NotNull final Origin origin,
      @NotNull final Outcome outcome
    ) {

      observation (
        origin,
        Operation.SEND,
        outcome
      );

    }


    /// Emits a SERVER observation with the specified outcome and operation.
    ///
    /// This is a convenience method that automatically sets the origin to SERVER.
    ///
    /// @param outcome   the outcome of the operation (SUCCESS or FAILURE)
    /// @param operation the type of operation being observed (CONNECT, SEND, RECEIVE, or PROCESS)
    /// @throws NullPointerException if any parameter is null

    default void server (
      @NotNull final Operation operation,
      @NotNull final Outcome outcome
    ) {

      observation (
        Origin.SERVER,
        operation,
        outcome
      );

    }


    /// Emits a SUCCESS observation for the specified origin and operation.
    ///
    /// This is a convenience method that automatically sets the outcome to SUCCESS.
    ///
    /// @param origin    the origin where the observation was made (CLIENT or SERVER)
    /// @param operation the type of operation being observed (CONNECT, SEND, RECEIVE, or PROCESS)
    /// @throws NullPointerException if any parameter is null

    default void success (
      @NotNull final Origin origin,
      @NotNull final Operation operation
    ) {

      observation (
        origin,
        operation,
        Outcome.SUCCESS
      );

    }

  }


  /// The `Outcome` enum represents the result of an observed operation.
  ///
  /// Each operation can either succeed as expected or fail in some manner.
  /// This classification provides the foundation for more detailed analysis
  /// when combined with origin and operation information.

  enum Outcome {

    /// The operation completed successfully as expected.
    SUCCESS,

    /// The operation failed to complete as expected.
    FAILURE

  }


  /// The `Origin` enum identifies where in the distributed system an observation was made.
  ///
  /// This spatial information helps locate the source of observations and,
  /// in the case of failures, can help attribute responsibility appropriately.

  enum Origin {
    /// The observation was made at the client side of the communication.
    ///
    /// Client-side observations typically relate to connection initiation,
    /// request sending, response receiving, and client-side processing.
    CLIENT,

    /// The observation was made at the server side of the communication.
    ///
    /// Server-side observations typically relate to connection acceptance,
    /// request receiving, processing, and response sending.
    SERVER
  }


  /// The `Operation` enum identifies the type of activity that was being
  /// performed when the observation was made.
  ///
  /// This provides context about what phase of communication was occurring,
  /// helping to construct a complete picture of the communication lifecycle.

  enum Operation {

    /// Establishing a connection or session between communicating parties.
    ///
    /// This operation represents the initial setup phase of communication,
    /// such as TCP handshakes, authentication, or session establishment.
    CONNECT,

    /// Sending data across the communication channel.
    ///
    /// This operation represents the transmission of data from one system
    /// to another, such as sending requests or commands.
    SEND,

    /// Receiving data from the communication channel.
    ///
    /// This operation represents the reception of data from another system,
    /// such as receiving responses or events.
    RECEIVE,

    /// Processing data after receipt.
    ///
    /// This operation represents post-communication activities such as
    /// parsing, validation, or application-level processing of received data.
    PROCESS,

    /// Closing the connection or session between communicating parties.
    ///
    /// This operation represents final shutdown operations, flushes, or protocol-level
    /// close signals.
    CLOSE

  }

}