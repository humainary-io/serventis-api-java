// Copyright (c) 2025 William David Louth

package io.humainary.modules.serventis.reporters.api;

import static io.humainary.substrates.api.Substrates.*;

/// The `Reporters` interface defines the contract for reporting
/// situational assessments within the Serventis framework.
///
/// A `Reporter` emits a `Situation`, representing the assessed
/// operational significance of a subject based on its condition and context.
///
/// This API decouples observational facts (as reported by `Monitors`)
/// from interpretive judgments, enabling adaptive prioritization and
/// layered sense-making in complex systems.
///
/// @author autoletics
/// @since 1.0

public interface Reporters
  extends Composer < Reporters.Reporter, Reporters.Situation > {

  /// A `Reporter` emits a `Situation` to express an observer's
  /// current assessment of a subject’s operational urgency or significance.
  ///
  /// A Reporter interprets input from lower layers (such as Monitors)
  /// and publishes a distilled, context-aware judgment about how the
  /// current situation should be treated by responders or automation layers.

  @Provided
  interface Reporter
    extends Pipe < Situation > {

    /// Emits a `CRITICAL` situation.
    ///
    /// Convenience method that indicates the situation is serious and demands prompt intervention.
    /// Equivalent to calling `emit(Situation.CRITICAL)`.

    default void critical () {
      emit ( Situation.CRITICAL );
    }


    /// Emits a `Situation`.
    ///
    /// @param situation the situational judgment to emit
    /// @throws NullPointerException if the situation param is null

    @Override
    void emit ( @NotNull Situation situation );


    /// Emits a `NORMAL` situation.
    ///
    /// Convenience method that indicates the situation poses no immediate concern.
    /// Equivalent to calling `emit(Situation.NORMAL)`.

    default void normal () {
      emit ( Situation.NORMAL );
    }


    /// Emits a `WARNING` situation.
    ///
    /// Convenience method that indicates the situation requires attention but is not yet critical.
    /// Equivalent to calling `emit(Situation.WARNING)`.

    default void warning () {
      emit ( Situation.WARNING );
    }


  }

  /// A `Situation` represents the situational level assigned to a subject,
  /// capturing the operational significance of its current condition.
  ///
  /// Situations express how the system perceives the current context—
  /// not just what is happening, but how seriously it should be treated.
  ///
  /// These values are used to guide response decisions, escalation,
  /// and visibility within an adaptive observability framework.

  enum Situation {
    NORMAL,   // The situation poses no immediate concern
    WARNING,  // The situation requires attention but is not yet critical
    CRITICAL // The situation is serious and demands prompt intervention
  }

}
