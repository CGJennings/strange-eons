package ca.cgjennings.apps.arkham.diy;

import ca.cgjennings.apps.arkham.component.GameComponent;
import ca.cgjennings.apps.arkham.component.Portrait;
import ca.cgjennings.apps.arkham.component.PortraitProvider;
import ca.cgjennings.apps.arkham.component.ConversionContext;
import ca.cgjennings.io.SEObjectInputStream;
import ca.cgjennings.io.SEObjectOutputStream;
import java.awt.Graphics2D;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * This adapter interface acts as the bridge between a Strange Eons game
 * component and a DIY script. The DIY script is expected to implement these
 * functions.
 *
 * <p>
 * <b>Note:</b> As of Strange Eons 3.0, this interface extends the
 * {@link PortraitProvider} interface. However, scripts do not have to implement
 * functions for that interface unless they perform their own portrait handling.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public interface Handler extends PortraitProvider {

    /**
     * Called when the DIY component is first created. This is used to set up
     * the component's basic features (face style, base key, etc.). This method
     * and
     * {@link #onRead(ca.cgjennings.apps.arkham.diy.DIY, java.io.ObjectInputStream) onRead}
     * are the only places where these values can be changed without throwing an
     * exception.
     *
     * @param diy the component
     */
    public void create(DIY diy);

    /**
     * Called to create a user interface that can edit the component.
     *
     * @param diy the component
     * @param editor the editor that will display the component editing controls
     */
    public void createInterface(DIY diy, DIYEditor editor);

    /**
     * Called to set up any objects needed to paint the component's front
     * face(s).
     *
     * @param diy the component
     * @param sheet the sheet representing the face in question
     */
    public void createFrontPainter(DIY diy, DIYSheet sheet);

    /**
     * Called to set up any objects needed to paint the component's back
     * face(s).
     *
     * @param diy the component
     * @param sheet the sheet representing the face in question
     */
    public void createBackPainter(DIY diy, DIYSheet sheet);

    /**
     * Called to paint content on the component's front face(s).
     *
     * @param g the graphics context to use for painting
     * @param diy the component
     * @param sheet the sheet representing the face in question
     */
    public void paintFront(Graphics2D g, DIY diy, DIYSheet sheet);

    /**
     * Called to paint content on the component's back face(s).
     *
     * @param g the graphics context to use for painting
     * @param diy the component
     * @param sheet the sheet representing the face in question
     */
    public void paintBack(Graphics2D g, DIY diy, DIYSheet sheet);

    /**
     * Called when the user wants to reset the component to a blank state.
     *
     * @param diy the component
     */
    public void onClear(DIY diy);

    /**
     * Called when the component is being read from a file, after all regular
     * content has been loaded.
     *
     * @param diy the component
     * @param objectInputStream the {@link SEObjectInputStream} used to read the
     * component
     */
    public void onRead(DIY diy, ObjectInputStream objectInputStream);

    /**
     * Called when the component is being written to a file, after all regular
     * content has been saved.
     *
     * @param diy the component
     * @param objectOutputStream the {@link SEObjectOutputStream} used to write
     * the component
     */
    public void onWrite(DIY diy, ObjectOutputStream objectOutputStream);

    @Override
    public int getPortraitCount();

    @Override
    public Portrait getPortrait(int index);

    public void onConvertFrom(DIY diy, GameComponent target, ConversionContext context);

    public void onConvertTo(DIY diy, GameComponent source, ConversionContext context);
}
