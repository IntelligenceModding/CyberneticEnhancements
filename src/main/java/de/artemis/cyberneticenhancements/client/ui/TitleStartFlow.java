package de.artemis.cyberneticenhancements.client.ui;

/** Once per launch: an optional tutorial offer on the first title screen, then the live title menu. */
public final class TitleStartFlow {
    public enum Stage { TUTORIAL_OFFER, MENU }
    private Stage stage;

    public Stage stage(boolean offer, boolean inWorld) {
        if (inWorld) worldEntered();
        else if (stage == null) stage = offer ? Stage.TUTORIAL_OFFER : Stage.MENU;
        return stage;
    }

    /** Joining any world or server uses up the offer, even when the title was never seen first, such as a direct launch into a world. */
    public void worldEntered() { stage = Stage.MENU; }

    public Stage advance() {
        stage = Stage.MENU;
        return stage;
    }
}
