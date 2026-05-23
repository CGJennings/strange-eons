package ca.cgjennings.graphics.cloudfonts;

/**
 * A set of descriptive categories that a cloud font can belong to.
 * These categories are useful to help narrow down the search for a font.
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
public final class CategorySet {
    CategorySet(int bits) {
        this.set = bits;
    }

    public CategorySet(boolean display, boolean handwriting, boolean monospace, boolean sansSerif, boolean serif, boolean symbols, boolean other) {
        set = 0;
        if (display) {
            set |= DISPLAY;
        }
        if (handwriting) {
            set |= HANDWRITING;
        }
        if (monospace) {
            set |= MONOSPACE;
        }
        if (sansSerif) {
            set |= SANS_SERIF;
        }
        if (serif) {
            set |= SERIF;
        }
        if (symbols) {
            set |= SYMBOLS;
        }
        if (other) {
            set |= OTHER;
        }
    }

    private int set;

    public boolean equals(CategorySet other) {
        return set == other.set;
    }

    public boolean equals(Object other) {
        return other instanceof CategorySet && equals((CategorySet) other);
    }

    public boolean isSubsetOf(CategorySet other) {
        return (set & other.set) == set;
    }

    public boolean isSupersetOf(CategorySet other) {
        return (set & other.set) == other.set;
    }

    public CategorySet union(CategorySet other) {
        return new CategorySet(set | other.set);
    }

    public CategorySet intersection(CategorySet other) {
        return new CategorySet(set & other.set);
    }

    public boolean hasEmptyIntersectionWith(CategorySet other) {
        return (set & other.set) == 0;
    }

    public boolean isEmpty() {
        return set == 0;
    }

    public boolean isDisplay() {
        return (set & DISPLAY) != 0;
    }

    public boolean isHandwriting() {
        return (set & HANDWRITING) != 0;
    }   

    public boolean isMonospace() {
        return (set & MONOSPACE) != 0;
    }

    public boolean isSansSerif() {
        return (set & SANS_SERIF) != 0;
    }

    public boolean isSerif() {
        return (set & SERIF) != 0;
    }

    public boolean isSymbols() {
        return (set & SYMBOLS) != 0;
    }

    public boolean isOther() {
        return (set & OTHER) != 0;
    }

    static final int DISPLAY = 1<<0;
    static final int HANDWRITING = 1<<1;
    static final int MONOSPACE = 1<<2;
    static final int SANS_SERIF = 1<<3;
    static final int SERIF = 1<<4;
    static final int SYMBOLS = 1<<5;

    static final int OTHER = 1<<30;

    static int toBits(String[] categories) {
        int bits = 0;
        for (String cat : categories) {
            switch (cat) {
                case "DISPLAY":
                    bits |= DISPLAY;
                    break;
                case "HANDWRITING":
                    bits |= HANDWRITING;
                    break;
                case "MONOSPACE":
                    bits |= MONOSPACE;
                    break;
                case "SANS_SERIF":
                    bits |= SANS_SERIF;
                    break;
                case "SERIF":
                    bits |= SERIF;
                    break;
                case "SYMBOLS":
                    bits |= SYMBOLS;
                    break;
                default:
                    bits |= OTHER;
                    break;
            }
        }
        return bits;
    }

    /**
     * Returns a string description of the categories in this set,
     * consisting of a comma-separated list of category names.
     */
    public String toString() {
        StringBuilder b = new StringBuilder();
        if (isDisplay()) {
            b.append("DISPLAY, ");
        }
        if (isHandwriting()) {
            b.append("HANDWRITING, ");
        }
        if (isMonospace()) {
            b.append("MONOSPACE, ");
        }
        if (isSansSerif()) {
            b.append("SANS_SERIF, ");
        }
        if (isSerif()) {
            b.append("SERIF, ");
        }
        if (isSymbols()) {
            b.append("SYMBOLS, ");
        }
        if (isOther()) {
            b.append("OTHER, ");
        }
        if (b.length() > 0) {
            b.setLength(b.length() - 2);
        }
        return b.toString();
    }
}
