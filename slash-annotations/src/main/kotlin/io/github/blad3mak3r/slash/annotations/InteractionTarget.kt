package io.github.blad3mak3r.slash.annotations

/** Where the interaction is allowed to be invoked from. */
enum class InteractionTarget {
    /** Guild channels only. */
    GUILD,
    /** Direct messages only. */
    DM,
    /** Both guilds and DMs. */
    ALL
}
