package resources.projects.pluginwizard;

import ca.cgjennings.apps.arkham.plugins.BundleInstaller;
import ca.cgjennings.apps.arkham.project.NewProjectDialog;
import ca.cgjennings.apps.arkham.project.PluginWizardDialog.WizardKit;
import ca.cgjennings.apps.arkham.project.Project;
import ca.cgjennings.apps.arkham.project.Task;
import ca.cgjennings.graphics.ImageUtilities;
import ca.cgjennings.spelling.ui.JSpellingTextField;
import ca.cgjennings.ui.DocumentEventAdapter;
import ca.cgjennings.ui.FilteredDocument;
import ca.cgjennings.ui.wizard.WizardAdapter;
import ca.cgjennings.ui.wizard.WizardEvent;
import ca.cgjennings.ui.wizard.WizardModel;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.event.DocumentEvent;
import static resources.Language.string;
import resources.ResourceKit;

/**
 * A standard page for gathering a plug-in's name, description, icon, file
 * names, and location in a plug-in wizard.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
public final class NamePage extends javax.swing.JPanel {
	private Task task;
	private boolean compiled;
	private WizardModel model;

	/**
	 * Creates new wizard dialog page for gathering basic information about
	 * a plug-in.
	 *
	 * @param task the task that was passed to the {@link WizardKit}, or <code>null</code>
	 * @param model the model that the page will be used in
	 */
	public NamePage( Task task, WizardModel model ) {
		initComponents();
		setPluginType( ContentType.ACTIVATED  );

		pluginNameField.getDocument().addDocumentListener( new DocumentEventAdapter() {
			@Override
			public void changedUpdate( DocumentEvent e ) {
				pluginNameChanged();
			}
		});

		DocumentEventAdapter blockStateUpdater = new DocumentEventAdapter() {
			@Override
			public void changedUpdate( DocumentEvent e ) {
				updateBlockState();
			}
		};
		bundleNameField.getDocument().addDocumentListener( blockStateUpdater );
		scriptNameField.getDocument().addDocumentListener( blockStateUpdater );

		String loc = null ;
		if( task != null ) {
			loc = task.getProject().getSettings().get( Project.KEY_RESOURCE_ID );
		}
		if( loc == null ) {
			loc = NewProjectDialog.getDefaultResourceID();
		}
		locationField.setText( "resources/" + loc );

		if( model != null ) {
			model.addWizardListener( new WizardAdapter() {
				@Override
				public void wizardShowingPage( WizardEvent e ) {
					if( e.getPage() == NamePage.this ) {
						updateBlockState();
					}
				}
			});
		}

	}

	private void pluginNameChanged() {
		String compactName = pluginNameField.getText().replaceAll( "\\s+", "" );
		bundleNameField.setText( compactName );
		if( type == ContentType.THEME && !compactName.endsWith("Theme") ) {
			scriptNameField.setText( compactName + "Theme" );
		} else {
			scriptNameField.setText( compactName );
		}
		updateBlockState();
	}

	private void updateBlockState() {
		if( model == null ) return;
		boolean block = false;

		if( pluginNameField.getText().isEmpty() ) {
			block = true;
		}

		if( bundleNameField.getText().isEmpty() ) {
			block = true;
		}

		if( scriptNameField.getText().isEmpty() && type != ContentType.LIBRARY ) {
			block = true;
		}

		model.setProgressBlocked( block );
	}

	private void setHiddenLibraryFields( boolean library ) {
		library = !library;
		scriptNameLabel.setVisible( library );
		scriptNameField.setVisible( library );
		scriptExt.setVisible( library );
//		locationLabel.setVisible( library );
//		locationField.setVisible( library );
//		locationTip.setVisible( library );
	}

	/**
	 * Sets whether the plug-in described by the page will be compiled instead
	 * of script-based.
	 *
	 * @param compiledCodeMode if <code>true</code>, the dialog will be set up
	 *     to support compiled code
	 */
	public void setCompiledClassMode( boolean compiledCodeMode ) {
		if( compiledCodeMode != compiled ) {
			compiled = compiledCodeMode;
			if( compiled ) {
				scriptExt.setText( ".java" );
				scriptNameLabel.setText( string("prj-l-plugin-wiz-plug-class") );

			} else {
				scriptExt.setText( ".js" );
				scriptNameLabel.setText( string("prj-l-plugin-wiz-plug-script") );
			}
		}
	}

	/**
	 * Returns <code>true</code> if the dialog is set up to create a compiled
	 * class plug-in instead of a script-based plug-in.
	 *
	 * @return <code>true</code> if the plug-in is based on compiled Java code
	 */
	public boolean isCompiledClassMode() {
		return compiled;
	}

	/**
	 * Updates the controls to represent a particular type of plug-in.
	 *
	 * <p>Setting the type to  <code>LIBRARY</code> will hide the
	 * script/class name and location fields since libraries do not include
	 * plug-in code. You can still query the dialog for these values,
	 * but they will be useless.
	 *
	 * @param type the type of plug-in to format the dialog for
	 * @throws NullPointerException if the type is <code>null</code>
	 * @throws IllegalStateException if the type is <code>THEME</code> and
	 *     compiled class mode is not set
	 */
	public void setPluginType( ContentType type ) {
		if( type == null ) throw new NullPointerException( "type" );

		if( this.type == type ) return;
		this.type = type;

		switch( type ) {
			case ACTIVATED:
				setBundleFileNameExtension( BundleInstaller.PLUGIN_FILE_EXT );
				setHiddenLibraryFields( false );
				pluginNameLabel.setText( string("prj-l-plugin-wiz-plug-name") );
				iconTip.setTipText( string("prj-l-plugin-wiz-plug-icon-tip") );
				break;
			case INJECTED:
				setBundleFileNameExtension( BundleInstaller.PLUGIN_FILE_EXT );
				setHiddenLibraryFields( false );
				pluginNameLabel.setText( string("prj-l-plugin-wiz-plug-name") );
				iconTip.setTipText( string("prj-l-plugin-wiz-plug-icon-tip") );
				break;
			case EXTENSION:
				setBundleFileNameExtension( BundleInstaller.EXTENSION_FILE_EXT );
				setHiddenLibraryFields( false );
				pluginNameLabel.setText( string("prj-l-plugin-wiz-plug-name") );
				iconTip.setTipText( string("prj-l-plugin-wiz-plug-icon-tip") );
				break;
			case LIBRARY:
				setBundleFileNameExtension( BundleInstaller.LIBRARY_FILE_EXT );
				setHiddenLibraryFields( true );
				pluginNameLabel.setText( string("prj-l-plugin-wiz-lib-name") );
				iconTip.setTipText( string("prj-l-plugin-wiz-plug-icon-tip") );
				break;
			case THEME:
				if( !compiled ) {
					setCompiledClassMode( true );
				}
				setBundleFileNameExtension( BundleInstaller.THEME_FILE_EXT );
				setHiddenLibraryFields( false );
				pluginNameLabel.setText( string("prj-l-plugin-wiz-theme-name") );
				iconTip.setTipText( string("prj-l-plugin-wiz-theme-icon-tip") );
				break;
			default:
				throw new AssertionError( "unknown type" );
		}
	}

	/**
	 * Returns the type of plug-in that the dialog is formatted for.
	 * @return the type of plug-in being created
	 */
	public ContentType getPluginType() {
		return type;
	}

	private ContentType type;

	/**
	 * Sets the name of the plug-in, theme, or library. Changing the
	 * plug-in name will set default values for the script/class name and
	 * bundle file name, so if you wish to set these also you should do so
	 * after setting name.
	 *
	 * @param name the value to set in the name field
	 */
	public void setPluginName( String name ) {
		pluginNameField.setText( name );
	}

	/**
	 * Returns the name of the plug-in, theme, or library.
	 * @return the value of the name field
	 */
	public String getPluginName() {
		return pluginNameField.getText();
	}

	/**
	 * Sets the description of the plug-in, theme, or library.
	 * @param description the value to set in the description field
	 */
	public void setPluginDescription( String description ) {
		descField.setText( description );
	}

	/**
	 * Returns the description of the plug-in, theme, or library.
	 * @return the value of the description field
	 */
	public String getPluginDescription() {
		return descField.getText();
	}

	/**
	 * Sets the image to use as the representative image for the plug-in,
	 * theme, or library.
	 *
	 * @param image an image to use, or <code>null</code>
	 */
	public void setPluginIconImage( BufferedImage image ) {
		pluginIconDrop.setImage( image );
	}

	/**
	 * Returns the selected representative image, or
	 * <code>null</code> if no image is set.
	 *
	 * @return the selected representative image
	 */
	public BufferedImage getPluginIconImage() {
		BufferedImage icon;
		if( type == ContentType.THEME ) {
			icon = pluginIconDrop.getImageAtSize( 48 );
			if( icon != null ) {
				icon = ImageUtilities.center( icon, 48, 48 );
				icon = ImageUtilities.copy( icon );
				Graphics2D g = icon.createGraphics();
				try {
					g.drawImage( ResourceKit.getImageQuietly( "projects/theme-frame.png" ), 0, 0, null );
				} finally {
					g.dispose();
				}
			}
		} else {
			icon = pluginIconDrop.getImageAtSize( 24 );
		}
		if( icon != null ) {
			icon = ImageUtilities.trim( icon );
		}
		return icon;
	}

	/**
	 * Writes the selected representative image as a PNG image
	 * file to the specified destination. If no image is selected,
	 * the method has no effect.
	 *
	 * @param dest the location where the image should be written
	 * @throws IOException if an error occurs while writing
	 */
	public void writePluginIcon( File dest ) throws IOException {
		BufferedImage icon = getPluginIconImage();
		if( icon != null ) {
			ImageIO.write( icon, "png", dest );
		}
	}

	/**
	 * Sets the name of the plug-in bundle file. The file name should not
	 * include an extension.
	 *
	 * @param bundle the name of the bundle file
	 */
	public void setBundleFileName( String bundle ) {
		bundleNameField.setText( bundle );
		bundleNameFieldFocusLost( null );
	}

	/**
	 * Returns the file name for the plug-in bundle, without an extension.
	 * @return the bundle file name
	 * @see #getBundleFileNameExtension()
	 */
	public String getBundleFileName() {
		return bundleNameField.getText();
	}

	private void setBundleFileNameExtension( String extension ) {
		if( extension == null ) extension = BundleInstaller.PLUGIN_FILE_EXT;
		if( !extension.isEmpty() && extension.charAt(0) != '.' ) {
			extension = '.' + extension;
		}
		bundleExtLabel.setText( extension );
	}

	/**
	 * Returns the file name extension for the plug-in bundle, including the
	 * leading dot. The extension returned is the appropriate extension
	 * for the current plug-in type.
	 *
	 * @return the bundle file name extension
	 * @see #getBundleFileName
	 * @see #setPluginType
	 */
	public String getBundleFileNameExtension() {
		return bundleExtLabel.getText();
	}

	/**
	 * Sets the script file name for a script-based plug-in. If the name
	 * includes the .js extension, this is removed before copying the name
	 * into the script name field.
	 *
	 * @param name the script file name, without extension
	 * @throws IllegalStateException if the dialog is set to compiled code mode
	 */
	public void setScriptFileName( String name ) {
		if( compiled ) {
			throw new IllegalStateException( "compiledClassMode=true" );
		}
		if( name == null ) name = "";
		if( name.endsWith( ".js" ) ) name = name.substring( 0, name.length() - ".js".length() );
		scriptNameField.setText( name );
	}

	/**
	 * Returns the script file name, including the .js extension.
	 *
	 * @return the script file name
	 * @throws IllegalStateException if the dialog is set to compiled code mode
	 */
	public String getScriptFileName() {
		if( compiled ) {
			throw new IllegalStateException( "compiledClassMode=true" );
		}
		String name = scriptNameField.getText();
		if( !name.endsWith( ".js" ) ) name += ".js";
		return name;
	}

	/**
	 * Sets the class name for a compiled plug-in. If the name
	 * includes the .java or .class extension, this is removed before copying the name
	 * into the class name field.
	 *
	 * @param name the class file name, without extension
	 * @throws IllegalStateException if the dialog is not set to compiled code mode
	 */
	public void setClassName( String name ) {
		if( !compiled ) {
			throw new IllegalStateException( "compiledClassMode=false" );
		}
		if( name == null ) name = "";
		if( name.endsWith( ".java" ) ) name = name.substring( 0, name.length() - ".java".length() );
		else if( name.endsWith( ".class" ) ) name = name.substring( 0, name.length() - ".class".length() );
		scriptNameField.setText( name );
	}

	/**
	 * Returns the compiled class name, without any .java or .class extension.
	 *
	 * @return the class file name
	 * @throws IllegalStateException if the dialog is not set to compiled code mode
	 */
	public String getClassName() {
		if( !compiled ) {
			throw new IllegalStateException( "compiledClassMode=false" );
		}
		String name = scriptNameField.getText();
		if( name.endsWith( ".java" ) ) name = name.substring( 0, name.length() - ".java".length() );
		if( name.endsWith( ".class" ) ) name = name.substring( 0, name.length() - ".class".length() );
		return name;
	}

	/**
	 * Returns the compiled class name, with the .java.
	 *
	 * @return the class file name
	 * @throws IllegalStateException if the dialog is not set to compiled code mode
	 */
	public String getClassFileName() {
		if( !compiled ) {
			throw new IllegalStateException( "compiledClassMode=false" );
		}
		String name = scriptNameField.getText();
		if( !name.endsWith( ".java" ) ) name += ".java";
		return name;
	}

	/**
	 * Returns a default path to use as the plug-in location.
	 * The path will use the value of the project's
	 * <code>Project.KEY_RESOURCE_ID</code> settings, if available,
	 * to create the name of a default resources subfolder.
	 * (If this setting is undefined, a default value is used that
	 * may be platform and/or user specific.)
	 *
	 * @return a default location
	 */
	public String getDefaultPath() {
		String folder = null;
		if( task != null ) {
			Project p = task.getProject();
			if( p != null ) {
				folder = p.getSettings().get( Project.KEY_RESOURCE_ID );
			}
		}
		if( folder == null ) {
			folder = NewProjectDialog.getDefaultResourceID();
		}
		return "resources/" + folder;
	}

	/**
	 * Sets the path or package name in the bundle where the plug-in files
	 * will be stored. The path may be slash-separated or dot-separated;
	 * it will be converted to slash-separated.
	 *
	 * @param location a package name or bundle file path
	 */
	public void setPath( String location ) {
		if( location == null ) location = "";
		if( compiled ) {
			location = location.replace( '/', '.' );
		} else {
			location = location.replace( '.', '/' );
		}
		location = location.replace( '\\', '/' );
		locationField.setText( location );
	}

	/**
	 * Returns the path where plug-in files should be stored.
	 * If you require a Java package name, replace slashes in the returned
	 * path with dots.
	 *
	 * @return the location for plug-in files
	 */
	public String getPath() {
		String location = locationField.getText();
		location = location.replace( '.', '/' );
		location = location.replace( '\\', '/' );
		while( location.startsWith( "/" ) ) location = location.substring(1);
		while( location.endsWith( "/" ) ) location = location.substring(0, location.length()-1);
		return location;
	}

	/**
	 * Returns the location where plug-in files should be stored as a file.
	 * The returned file will represent the current plug-in path relative to
	 * the specified parent file. If the parent file is
	 * <code>null</code>, then the task folder will be used if a non-
	 * <code>null</code> {@link Task} was provided to the constructor.
	 *
	 * @param parent the parent file that the path is relative to, or <code>null</code>
	 *     to use the task folder
	 * @return the path as a child of the parent file
	 * @throws IllegalStateException if the parent is <code>null</code> and
	 *   a <code>null</code> task was set when the page was constructed
	 */
	public File getPathAsFile( File parent ) {
		if( parent == null ) {
			if( task == null ) throw new IllegalStateException( "cannot use parent==null if null task set in constructor" );
			parent = task.getFile();
		}
		String folder = getPath().replace( '/', File.separatorChar );
		return new File( parent, folder );
	}

	/**
	 * Returns the path value as a resource identifier. (If you need this value
	 * as a URL, prepend res:// to the start of the returned string.)
	 * <p>Example:<br>
	 * <code>String imgCode = "ResourceKit.getImage( \"" + page.getPathAsResource() + "/myimage.png\" );"</code>
	 *
	 * @return the base resource name for resources stored in the plug-in's location
	 */
	public String getPathAsResource() {
		String folder = getPath();
		if( folder.startsWith( "resources/" ) ) {
			folder = folder.substring( "resources/".length() );
		} else {
			folder = '/' + folder;
		}
		return folder;
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	@SuppressWarnings( "unchecked" )
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        pluginNameField = new JSpellingTextField();
        pluginNameLabel = new javax.swing.JLabel();
        descField = new JSpellingTextField();
        scriptNameField = new javax.swing.JTextField();
        scriptNameLabel = new javax.swing.JLabel();
        descLabel = new javax.swing.JLabel();
        bundleNameLabel = new javax.swing.JLabel();
        scriptExt = new javax.swing.JLabel();
        bundleExtLabel = new javax.swing.JLabel();
        bundleNameField = new javax.swing.JTextField();
        ca.cgjennings.ui.JTip bundleNameTip = new ca.cgjennings.ui.JTip();
        locationLabel = new javax.swing.JLabel();
        locationField = new javax.swing.JTextField();
        pluginIconDrop = new ca.cgjennings.ui.JIconDrop();
        iconLabel = new javax.swing.JLabel();
        iconTip = new ca.cgjennings.ui.JTip();
        locationTip = new ca.cgjennings.ui.JTip();

        pluginNameField.setColumns(25);

        pluginNameLabel.setText(string( "prj-l-plugin-wiz-plug-name" )); // NOI18N

        descField.setColumns(40);
        descField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                descFieldFocusLost(evt);
            }
        });

        scriptNameField.setColumns(25);
        scriptNameField.setDocument( FilteredDocument.createJavaNameDocument() );

        scriptNameLabel.setText(string( "prj-l-plugin-wiz-plug-script" )); // NOI18N

        descLabel.setText(string( "prj-l-plugin-wiz-plug-desc" )); // NOI18N

        bundleNameLabel.setText(string( "prj-l-plugin-wiz-bundle" )); // NOI18N

        scriptExt.setText(".js");

        bundleExtLabel.setText(".seext");

        bundleNameField.setColumns(25);
        bundleNameField.setDocument( FilteredDocument.createFileNameDocument() );
        bundleNameField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                bundleNameFieldFocusLost(evt);
            }
        });

        bundleNameTip.setTipText(string("prj-l-plugin-wiz-bundle-tip")); // NOI18N

        locationLabel.setText(string("prj-l-plugin-wiz-location")); // NOI18N

        locationField.setDocument( FilteredDocument.createFilePathDocument() );

        iconLabel.setText(string("prj-l-plugin-wiz-plug-icon")); // NOI18N

        iconTip.setTipText(string("prj-l-plugin-wiz-plug-icon-tip")); // NOI18N

        locationTip.setTipText(string("prj-l-plugin-wiz-location-tip")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pluginNameLabel)
                    .addComponent(descLabel)
                    .addComponent(scriptNameLabel)
                    .addComponent(bundleNameLabel)
                    .addComponent(locationLabel)
                    .addComponent(iconLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(descField, javax.swing.GroupLayout.PREFERRED_SIZE, 1, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(pluginIconDrop, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(iconTip, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                    .addComponent(locationField)
                                    .addComponent(pluginNameField)
                                    .addComponent(scriptNameField)
                                    .addComponent(bundleNameField))
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addGap(1, 1, 1)
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(scriptExt)
                                            .addGroup(layout.createSequentialGroup()
                                                .addComponent(bundleExtLabel)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(bundleNameTip, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                                    .addGroup(layout.createSequentialGroup()
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(locationTip, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                        .addGap(0, 24, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(pluginNameLabel)
                    .addComponent(pluginNameField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(descLabel)
                    .addComponent(descField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pluginIconDrop, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(iconLabel)
                    .addComponent(iconTip, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(bundleNameLabel)
                    .addComponent(bundleNameField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(bundleExtLabel)
                    .addComponent(bundleNameTip, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(scriptNameLabel)
                    .addComponent(scriptNameField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(scriptExt))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(locationLabel)
                    .addComponent(locationField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(locationTip, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(46, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void bundleNameFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_bundleNameFieldFocusLost
        // check if the user accidentally eneterd an extension, and if
		// so remove it
		String name = bundleNameField.getText();
		int dot = name.lastIndexOf( '.' );
		if( dot >= 0 ) {
			String ext = name.substring( dot );
			name = name.substring( 0, dot );
			if(
					ext.equals( BundleInstaller.PLUGIN_FILE_EXT )
				|| ext.equals( BundleInstaller.EXTENSION_FILE_EXT )
				|| ext.equals( BundleInstaller.THEME_FILE_EXT )
				|| ext.equals( BundleInstaller.LIBRARY_FILE_EXT )
			) {
				bundleNameField.setText( name );
			}
		}
    }//GEN-LAST:event_bundleNameFieldFocusLost

    private void descFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_descFieldFocusLost
        String text = descField.getText();
		if( !text.isEmpty() && text.charAt( text.length()-1 ) == '.' ) {
			descField.setText( text.substring( 0, text.length()-1 ) );
		}
    }//GEN-LAST:event_descFieldFocusLost

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel bundleExtLabel;
    private javax.swing.JTextField bundleNameField;
    private javax.swing.JLabel bundleNameLabel;
    private javax.swing.JTextField descField;
    private javax.swing.JLabel descLabel;
    private javax.swing.JLabel iconLabel;
    private ca.cgjennings.ui.JTip iconTip;
    private javax.swing.JTextField locationField;
    private javax.swing.JLabel locationLabel;
    private ca.cgjennings.ui.JTip locationTip;
    private ca.cgjennings.ui.JIconDrop pluginIconDrop;
    private javax.swing.JTextField pluginNameField;
    private javax.swing.JLabel pluginNameLabel;
    private javax.swing.JLabel scriptExt;
    private javax.swing.JTextField scriptNameField;
    private javax.swing.JLabel scriptNameLabel;
    // End of variables declaration//GEN-END:variables
}
