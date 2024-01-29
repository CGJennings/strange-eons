package ca.cgjennings.imageio;

import resources.Language;

/**
 * A writable format for a standard ImageIO format that is directly supported by
 * {@link SimpleImageWriter}.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
class DefaultImageFormat implements WritableImageFormat {

    String name;
    private String ext;
    private String fullName;
    private String desc;

    public DefaultImageFormat(String keyBase, String extension) {
        this("exf-" + keyBase + "-ext", extension, "exf-" + keyBase, "exf-" + keyBase + "-detail", true);
    }

    public DefaultImageFormat(String name, String extension, String fullName, String description, boolean lookupStrings) {
        this.name = lookupStrings ? resources.Language.string(name) : name;
        ext = extension;
        if (fullName != null) {
            this.fullName = lookupStrings ? resources.Language.string(fullName) : fullName;
        }
        if (description != null) {
            desc = lookupStrings ? resources.Language.string(description) : description;
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getExtension() {
        return ext;
    }

    @Override
    public String getFullName() {
        return fullName;
    }

    @Override
    public String getDescription() {
        return desc;
    }

    @Override
    public SimpleImageWriter createImageWriter() {
        return new SimpleImageWriter(ext, Language.getGameLocale());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DefaultImageFormat other = (DefaultImageFormat) obj;
        return !((this.ext == null) ? (other.ext != null) : !this.ext.equals(other.ext));
    }

    @Override
    public int hashCode() {
        return ext.hashCode();
    }
}
