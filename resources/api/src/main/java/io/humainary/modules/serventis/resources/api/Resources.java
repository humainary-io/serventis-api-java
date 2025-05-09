// Copyright (c) 2025 William David Louth
package io.humainary.modules.serventis.resources.api;

import static io.humainary.substrates.api.Substrates.*;

/// The `Resources` API provides a clean, consistent way to emit signals describing resource interactions
/// for observability and reasoning. This API enables systems to emit structured signals that represent
/// interactions with named resources. These signals do not control resources themselves but report what
/// is happening to a resource or around a resource from either the requester's or provider's perspective.

public interface Resources
  extends Composer < Resources.Resource, Resources.Signal > {

  /// A `Resource` represents an observable entity whose state can be influenced through acquisition
  /// and release patterns. A resource emits `Signal` objects describing those patterns.

  @Provided
  interface Resource
    extends Pipe < Signal > {

    /// Emits an `ACQUIRE` signal with a default of 1 unit from this resource.
    ///
    /// Represents a blocking or wait-based request for a single unit from the resource.

    default void acquire () {

      signal (
        Sign.ACQUIRE
      );

    }


    /// Emits an `ACQUIRE` signal with the specified number of units from this resource.
    ///
    /// Represents a blocking or wait-based request for multiple units from the resource.
    ///
    /// @param units The number of resource units being requested

    default void acquire (
      final long units
    ) {

      signal (
        Sign.ACQUIRE,
        units
      );

    }


    /// Emits an `ATTEMPT` signal with a default of 1 unit from this resource.
    ///
    /// Represents a non-blocking request for a single unit from the resource.

    default void attempt () {

      signal (
        Sign.ATTEMPT
      );

    }


    /// Emits an `ATTEMPT` signal with the specified number of units from this resource.
    ///
    /// Represents a non-blocking request for multiple units from the resource.
    ///
    /// @param units The number of resource units being requested

    default void attempt (
      final long units
    ) {

      signal (
        Sign.ATTEMPT,
        units
      );

    }


    /// Emits a `DENY` signal with a default of 1 unit from this resource.
    ///
    /// Represents the denial of a request for a single unit from the resource (e.g., due to lack of capacity).

    default void deny () {

      signal (
        Sign.DENY
      );

    }


    /// Emits a `DENY` signal with the specified number of units from this resource.
    ///
    /// Represents the denial of a request for multiple units from the resource (e.g., due to lack of capacity).
    ///
    /// @param units The number of resource units being denied

    default void deny (
      final long units
    ) {

      signal (
        Sign.DENY,
        units
      );

    }


    /// Emits a signal from this resource.
    ///
    /// @param signal The signal to emit

    @Override
    void emit (
      @NotNull final Signal signal
    );


    /// Emits a `GRANT` signal with a default of 1 unit from this resource.
    ///
    /// Represents the granting of a request for a single unit from this resource.

    default void grant () {

      signal (
        Sign.GRANT
      );

    }


    /// Emits a `GRANT` signal with the specified number of units from this resource.
    ///
    /// Represents the granting of a request for multiple units from this resource.
    ///
    /// @param units The number of resource units being granted by this resource

    default void grant (
      final long units
    ) {

      signal (
        Sign.GRANT,
        units
      );

    }


    /// Emits a `RELEASE` signal with a default of 1 unit from this resource.
    ///
    /// Represents the returning of a single unit previously granted by this resource.

    default void release () {

      signal (
        Sign.RELEASE
      );

    }


    /// Emits a `RELEASE` signal with the specified number of units from this resource.
    ///
    /// Represents the returning of a number of units previously granted by this resource.
    ///
    /// @param units The number of resource units being returned to this resource

    default void release (
      final long units
    ) {

      signal (
        Sign.RELEASE,
        units
      );

    }


    /// Emits a signal with the specified `sign` and `units` from this resource.
    ///
    /// @param sign  The sign representing the type of resource interaction
    /// @param units The number of resource units involved in the interaction
    /// @throws NullPointerException if the sign param is `null`

    void signal (
      @NotNull final Sign sign,
      final long units
    );


    /// Emits a signal with the specified sign and a default of 1 unit from this resource.
    ///
    /// This is a convenience method for interactions involving a single resource unit.
    ///
    /// @param sign The sign representing the type of resource interaction
    /// @throws NullPointerException if the sign param is `null`

    default void signal (
      @NotNull final Sign sign
    ) {

      signal (
        sign,
        1L
      );

    }


    /// Emits a `TIMEOUT` signal with a default of 1 unit from this resource.
    ///
    /// Represents the timing out of a request to this resource.

    default void timeout () {

      signal (
        Sign.TIMEOUT
      );

    }


    /// Emits a `TIMEOUT` signal with the specified number of units requested from this resource.
    ///
    /// Represents the timing out of a request to the resource.
    ///
    /// @param units The number of resource units that were requested from this resource

    default void timeout (
      final long units
    ) {

      signal (
        Sign.TIMEOUT,
        units
      );

    }

  }

  /// A `Signal` represents the payload emitted from a resource interaction.

  @Provided
  interface Signal {

    /// Returns the sign representing the kind of resource action observed.
    ///
    /// @return The sign enum value associated with this signal

    @NotNull
    Sign sign ();


    /// Returns the number of resource units involved in this interaction.
    ///
    /// @return The number of units being requested, granted, denied, or released

    long units ();

  }

  /// A `Sign` represents the kind of action being observed in a resource interaction.

  enum Sign {

    /// A non-blocking request for units from a resource
    ATTEMPT,

    /// A blocking or wait-based request for units from a resource
    ACQUIRE,

    /// The granting of a request for units from a resource
    GRANT,

    /// The denial of a request (e.g., due to lack of capacity)
    DENY,

    /// The timing out of a request to the resource
    TIMEOUT,

    /// The releasing and returning of units previously granted by the resource
    RELEASE

  }

}
