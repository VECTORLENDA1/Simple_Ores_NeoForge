// Copy of a small part of the Just Enough Resources API, only used to COMPILE the JER plugin of Simple Ores.
// It is NOT inside the Simple Ores jar: in the game the real classes of JER are used.
package jeresources.api;

public interface IJERPlugin {
    void receive(IJERAPI api);
}
