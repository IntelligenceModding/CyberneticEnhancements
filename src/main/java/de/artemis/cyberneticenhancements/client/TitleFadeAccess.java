package de.artemis.cyberneticenhancements.client;

/** Lets a screen that was opened over the title screen end the title's own first-open fade, so it does not start again on return. */
public interface TitleFadeAccess {
    void cyberneticenhancements$skipFade();
}
