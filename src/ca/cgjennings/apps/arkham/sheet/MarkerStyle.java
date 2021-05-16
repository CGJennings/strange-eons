package ca.cgjennings.apps.arkham.sheet;

/**
 * An enumeration of the possible types of embedded markers. An embedded marker
 * is a marker that is represented by one of the sheets for a game component. An
 * embedded marker is closely related to the sheets for the rest of the
 * component, but is not the primary content represented by those sheets. A
 * typical example is a game component for a player character which includes
 * sheets for the character statistics along with a separate sheet for a
 * character marker used to indicate the character's location on a board. It is
 * easier for the user to design both together since the same character portrait
 * will probably be used on both the character sheet and the marker.
 *
 * <p>
 * Embedded tokens are unique in that they consist of a single sheet, but when
 * printed (or when decks are generated that include them) that single sheet may
 * be replicated automatically to create a double-sided token. When a sheet has
 * a non-{@code null} {@code MarkerStyle}, this both declares that the
 * sheet represents an embedded marker and determines how it is copied for print
 * layouts.
 *
 * @see Sheet#getMarkerStyle()
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public enum MarkerStyle {
    /**
     * No second side will be generated for this embedded marker.
     */
    ONE_SIDED,
    /**
     * A back side that is identical to the front side will be generated.
     */
    COPIED,
    /**
     * A back side that is a mirror image of the front side will be generated.
     */
    MIRRORED
}
