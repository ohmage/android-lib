
package org.ohmage.prompt;

import org.ohmage.Utilities.KVLTriplet;

import java.util.List;

public interface ChoicePrompt {
    public void setChoices(List<KVLTriplet> choices);

    public List<KVLTriplet> getChoices();
}
