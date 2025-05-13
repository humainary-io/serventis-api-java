// Copyright (c) 2025 William David Louth
package io.humainary.modules.serventis.queues.api;

import static io.humainary.substrates.api.Substrates.*;

/// # Queues API
///
/// The `Queues` API provides a structured and minimal interface for observing interactions
/// with queue-like systems. It enables systems to emit **semantic signals** representing
/// key queue operations such as enqueue (put), dequeue (take), and boundary violations.
/// ## Purpose
/// This API is designed to support **observability and reasoning** in systems that use
/// queues as flow-control or communication mechanisms. By modeling queue interactions
/// as composable signals, it enables introspection of system dynamics, pressure points,
/// and resource utilization without coupling to specific implementation details.
/// ## Key Concepts
/// - **Queue**: A named subject that emits signals describing operations performed against it.
/// - **Signal**: A structured payload that communicates what interaction occurred and the quantity involved.
/// - **Sign**: An enumeration of distinct interaction types: `PUT`, `TAKE`, `OVERFLOW`, `UNDERFLOW`.
/// ## Signals and Semantics
/// | Sign        | Description                                               |
/// |-------------|-----------------------------------------------------------|
/// | `PUT`       | An item or unit was added to the queue                    |
/// | `TAKE`      | An item or unit was removed from the queue                |
/// | `OVERFLOW`  | A `PUT` failed partially or completely due to capacity    |
/// | `UNDERFLOW` | A `TAKE` failed partially or completely due to emptiness  |
/// Each signal includes a scalar value (`units`) representing the number of queue units
/// involved in the operation. For example, a signal may indicate an overflow of 3 units
/// when attempting to add 10 to a nearly full queue.
/// ## Use Cases
/// - Modeling traffic flow in bounded queues or backpressure systems
/// - Instrumenting messaging systems, task queues, or pipelines
/// - Diagnosing latency and load in producer-consumer patterns
/// - Building higher-level abstractions such as buffers, schedulers, or mailboxes

public interface Queues
  extends Composer < Queues.Queue, Queues.Signal > {

  /// The `Queue` interface represents a named, observable queue from which signals are emitted.
  @Provided
  interface Queue
    extends Pipe < Signal > {

    /// Emits a signal from this queue.
    ///
    /// @param signal The signal to emit
    /// @throws NullPointerException if the signal param is `null``

    @Override
    void emit (
      @NotNull final Signal signal
    );


    /// Emits an overflow signal with a default of 1 unit from this queue.

    default void overflow () {

      signal (
        Sign.OVERFLOW
      );

    }


    /// Emits an overflow signal with the specified number of units from this queue.
    ///
    /// @param units The number of units involved in the overflow condition

    default void overflow (
      final long units
    ) {

      signal (
        Sign.OVERFLOW
      );

    }


    /// Emits a put signal with a default of 1 unit from this queue.

    default void put () {

      signal (
        Sign.PUT
      );

    }


    /// Emits a put signal with the specified number of units from this queue.
    ///
    /// @param units The number of units being put into the queue

    default void put (
      final long units
    ) {

      signal (
        Sign.PUT
      );

    }


    /// Emits a signal with the specified `sign` and `units` from this queue.
    ///
    /// @param sign  The sign representing the type of queue interaction
    /// @param units The number of queue units involved in the interaction
    /// @throws NullPointerException if the sign param is `null`

    void signal (
      @NotNull final Sign sign,
      final long units
    );


    /// Emits a signal with the specified sign and a default of 1 unit from this queue.
    ///
    /// This is a convenience method for interactions involving a single queue unit.
    ///
    /// @param sign The sign representing the type of queue interaction
    /// @throws NullPointerException if the sign param is `null`

    default void signal (
      @NotNull final Sign sign
    ) {

      signal (
        sign,
        1L
      );

    }


    /// Emits a take signal with a default of 1 unit from this queue.

    default void take () {

      signal (
        Sign.TAKE
      );

    }


    /// Emits a take signal with the specified number of units from this queue.
    ///
    /// @param units The number of units being taken from the queue

    default void take (
      final long units
    ) {

      signal (
        Sign.TAKE
      );

    }


    /// Emits an underflow signal with a default of 1 unit from this queue.

    default void underflow () {

      signal (
        Sign.UNDERFLOW
      );

    }


    /// Emits an underflow signal with the specified number of units from this queue.
    ///
    /// @param units The number of units involved in the underflow condition

    default void underflow (
      final long units
    ) {

      signal (
        Sign.UNDERFLOW
      );

    }

  }

  /// A `Signal` represents the payload emitted from a queue interaction.

  @Provided
  interface Signal {

    /// Returns the sign representing the kind of queue action observed.
    ///
    /// @return The sign enum value associated with this signal

    @NotNull
    Sign sign ();


    /// Returns the number of queue units involved in this interaction.
    ///
    /// @return The number of units involved in this interaction.

    long units ();

  }

  /// A `Sign` represents the kind of action being observed in a queue interaction.

  enum Sign {

    /// Indicates an item was put into the queue
    PUT,

    /// Indicates an item was taken from the queue
    TAKE,

    /// Indicates the queue reached an overflow condition
    OVERFLOW,

    /// Indicates the queue reached an underflow condition
    UNDERFLOW

  }

}
