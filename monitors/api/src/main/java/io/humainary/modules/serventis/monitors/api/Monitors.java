/*
 * Copyright (c) 2025 William David Louth
 */

package io.humainary.modules.serventis.monitors.api;

import static io.humainary.substrates.api.Substrates.*;


/// The [Monitors] class is the entry point into the Serventis Monitors API.
///
/// The Monitors API is used to monitor the operational condition of a subject.
/// It is used by an observer to emit an assessment of a subject's operational condition
/// as well as the statistical certainty of that assessment.
///
/// @author autoletics
/// @since 1.0

public interface Monitors
  extends Composer < Monitors.Monitor, Monitors.Status > {


  /// The [Monitor] interface is used to emit an observer's assessment of a subject's
  /// operational condition as well as the statistical certainty of that assessment.

  @Provided
  interface Monitor
    extends Pipe < Status > {

    void emit (
      @NotNull Condition condition,
      @NotNull Confidence confidence
    );

  }

  /// The [Status] interface represents the assessed operational condition of a subject within some context.
  /// It includes the condition classification as well as the statistical certainty of that classification.

  @Provided
  interface Status {

    /// Returns the operational condition classification based on signal pattern analysis.
    @NotNull
    Condition condition ();


    /// Returns the statistical certainty of the operational condition classification.
    @NotNull
    Confidence confidence ();

  }


  /// The [Condition] enum represents the operational condition of a subject.

  enum Condition {

    /// Indicates the subject of observation is stabilizing towards reliable operation,
    /// typically following initialization, scaling, or recovery

    CONVERGING,

    /// Indicates the subject of observation is operating within expected parameters,
    /// characterized by consistent response patterns and acceptable success rates

    STABLE,

    /// Indicates the subject of observation is destabilizing, with increasing variations
    /// in response times, error rates, or other operational metrics

    DIVERGING,

    /// Indicates the subject of observation is exhibiting unpredictable behavior with
    /// irregular transitions between different operational conditions

    ERRATIC,

    /// Indicates the subject of observation is partially operational, with reduced
    /// performance, elevated error rates, or delayed responses

    DEGRADED,

    /// Indicates the subject of observation is unreliable, with predominantly failed
    /// operations and significant inability to meet service level objectives.

    DEFECTIVE,

    /// Indicates the subject of observation is entirely non-operational and unable to process any requests
    DOWN

  }

  /// The [Confidence] enum represents the statistical certainty of an operational condition classification.

  enum Confidence {

    /// Indicates a preliminary assessment based on initial behavioral patterns.
    TENTATIVE,

    /// Indicates an established assessment with strong evidence for the condition classification
    MEASURED,

    /// Indicates a definitive assessment with unambiguous evidence for the condition classification
    CONFIRMED

  }

}
