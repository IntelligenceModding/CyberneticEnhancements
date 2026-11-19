package de.artemis.cyberneticenhancements.common.tutorial;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * What a tutorial player may currently see and use. Tokens are interface elements ({@link TutorialScript#ELEMENTS}) or
 * {@code key:<mapping name>} for a single key action. Steps change it declaratively, and code can change it at any time:
 * <ul>
 *   <li>{@code reveal}: allowed from now on;</li>
 *   <li>{@code hide}: taken away again, including any temporary allowance;</li>
 *   <li>{@code allow}: allowed only until the current step ends, such as opening chat for one step.</li>
 * </ul>
 */
public final class TutorialAccess {
    public static final String KEY_PREFIX = "key:";
    private final Set<String> unlocked = new LinkedHashSet<>(), temporary = new LinkedHashSet<>();

    public static boolean valid(String token) {
        return TutorialScript.ELEMENTS.contains(token) || token.startsWith(KEY_PREFIX) && token.length() > KEY_PREFIX.length() && token.length() <= 128;
    }

    public TutorialAccess reveal(Collection<String> tokens) {
        unlocked.addAll(tokens);
        return this;
    }

    public TutorialAccess hide(Collection<String> tokens) {
        unlocked.removeAll(tokens);
        temporary.removeAll(tokens);
        return this;
    }

    public TutorialAccess allow(Collection<String> tokens) {
        temporary.addAll(tokens);
        return this;
    }

    /** Temporary allowances end with the step that granted them. */
    public TutorialAccess endStep() {
        temporary.clear();
        return this;
    }

    /** Entering a step ends the previous step's allowances, then applies this step's changes in a fixed order. */
    public TutorialAccess enter(TutorialScript.Step step) {
        return endStep().reveal(step.reveal()).hide(step.hide()).allow(step.allow());
    }

    public boolean allows(String token) { return unlocked.contains(token) || temporary.contains(token); }

    public List<String> snapshot() {
        var all = new LinkedHashSet<>(unlocked);
        all.addAll(temporary);
        return List.copyOf(all);
    }
}
