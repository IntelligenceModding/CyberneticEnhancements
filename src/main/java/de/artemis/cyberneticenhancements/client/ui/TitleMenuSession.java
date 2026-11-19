package de.artemis.cyberneticenhancements.client.ui;

/** A title visit includes its child menus and connection/loading screens, until a world opens. */
public final class TitleMenuSession {
    private boolean active;

    public boolean update(boolean inWorld, boolean titleReady) {
        if (inWorld) active = false;
        else if (titleReady) active = true;
        return active;
    }

    public boolean active() { return active; }
}
