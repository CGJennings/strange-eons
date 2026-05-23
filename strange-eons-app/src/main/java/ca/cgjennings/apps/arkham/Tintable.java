package ca.cgjennings.apps.arkham;

/**
 * Interface implemented by objects which can be tinted by an HSBPanel.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public interface Tintable {

    public abstract void setTint(float hueShift, float saturation, float brightness);

    public abstract float[] getTint();
}
