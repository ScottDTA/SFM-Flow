package dta.sfmflow.api.component;

import java.util.UUID;

/**
 * Public API base class representing a trigger component in the flowchart logic workspace [3].
 * Configures common single-output visual and logic behavior by default [3].
 */
public abstract class AbstractTriggerComponent extends AbstractFlowComponent
 {
  public AbstractTriggerComponent(UUID uuid)
   {
    super(uuid);
    this.hasOutputNodes = true;
    this.numOutputs = 1;
   }
 }