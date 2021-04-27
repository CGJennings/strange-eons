package resources.projects.pluginwizard;

import ca.cgjennings.apps.arkham.project.MakeBundle;
import ca.cgjennings.apps.arkham.project.ProjectUtilities;
import ca.cgjennings.apps.arkham.project.Task;
import ca.cgjennings.text.SETemplateProcessor;
import java.io.File;
import java.io.IOException;

/**
 * Utility functions for creating new wizard kits.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 * @see ca.cgjennings.apps.arkham.project.PluginWizardDialog.WizardKit
 */
public class KitUtilities {
	private KitUtilities() {}

	/**
	 * Creates one or more subfolders in a task folder. If path is
	 * <code>null</code>, then this will create the plug-in's base location,
	 * the location identified by the "Location" field of {@link NamePage}.
	 * Typically, this is something like resources/myplugin. If path is non-null,
	 * then it will create a subfolder (using path as the name) within the base location.
	 * If path contains one or more slashes (/), then the entire path will be
	 * created. For example, "fonts/body" would create a fonts folder in the base
	 * location, and a body folder within the fonts folder.
	 *
	 * <p>If the name page np is  <code>null</code>, then the task folder is used
	 * as the base location.
	 *
	 * <p>If a folder that would have been created by this method already exists,
	 * the creation of that folder is skipped without causing an exception.
	 *
	 * @param t the task that is being created
	 * @param np a name page that allowed the user to describe basic data about the plug-in task
	 * @param path the path to create within the base location
	 * @throws IOException if an error occurs while creating the folders
	 */
	public static void makeTaskFolders( Task t, NamePage np, String path ) throws IOException {
		File parent = t.getFile();
		if( np != null ) parent = np.getPathAsFile( parent );
		makeFolderImpl( parent );
		if( path != null ) {
			String[] folders = path.split( "\\/" );
			for( String folder : folders ) {
				File child = new File( parent, folder );
				makeFolderImpl( child );
				parent = child;
			}
		}
	}

	private static void makeFolderImpl( File child ) throws IOException {
		if( child.exists() && !child.isDirectory() ) {
			throw new IOException( "Expected folder instead of file: " + child );
		}
		if( !child.exists() && !child.mkdirs() ) {
			throw new IOException( "Unable to create folder: " + child );
		}
	}

	/**
	 * Returns the text of a template file stored in resources/projects/pluginwizard.
	 *
	 * @param templateFileName the name of the template file
	 * @return the text of the template
	 * @throws IOException if an error occurs while reading the template
	 */
	public static String getTemplateText( String templateFileName ) throws IOException {
		return ProjectUtilities.getResourceText( "projects/pluginwizard/" + templateFileName );
	}

	/**
	 * Sets conditional variables in a template processor based on the plug-in
	 * type. The conditional variables are isActivated, isInjected, isExtension,
	 * isLibrary, and isTheme.
	 *
	 * @param proc the processor to set conditional variables in
	 * @param type the plug-in type that will determine the values of the conditionals
	 */
	public static void setPluginTypeConditionals( SETemplateProcessor proc, ContentType type ) {
		proc.setCondition( "isActivated", type == ContentType.ACTIVATED );
		proc.setCondition( "isInjected", type == ContentType.INJECTED );
		proc.setCondition( "isExtension", type == ContentType.EXTENSION );
		proc.setCondition( "isTheme", type == ContentType.THEME );
		proc.setCondition( "isLibrary", type == ContentType.LIBRARY );
	}

	/**
	 * Sets the file name used for a task's plug-in bundle when the Make Bundle
	 * action is applied to it.
	 *
	 * @param t the task whose bundle file should be set
	 * @param bundleFileName the file name for the bundle
	 */
	public static void setBundleName( Task t, String bundleFileName ) {
		t.getSettings().set( MakeBundle.KEY_BUNDLE_FILE, bundleFileName );
	}

	/**
	 * Sets the file name used for a task's plug-in bundle when the Make Bundle
	 * action is applied to it. This method uses the file name supplied by
	 * the user on a {@link NamePage}.
	 *
	 * @param t the task whose bundle file should be set
	 * @param np the name page containing the bundle name
	 */
	public static void setBundleName( Task t, NamePage np ) {
		setBundleName( t, np.getBundleFileName() + np.getBundleFileNameExtension() );
	}

	/**
	 * Returns a File object for a file within a plug-in within the given task folder.
	 * If the name page is non-<code>null</code>, then the base location given
	 * on that page will be the parent of the file. Otherwise, the task folder
	 * will be the parent of the file. The path argument specifies a path
	 * relative to this parent and ending in the file of interest.
	 *
	 * @param t the task folder that contains the plug-in files
	 * @param np if non-<code>null</code>, used to determine the base location within the task
	 * @param path a path indicating the desired file location, using / to separate folders
	 * @return the specified file within the task folder
	 */
	public static File getFile( Task t, NamePage np, String path ) {
		File parent = t.getFile();
		if( np != null ) parent = np.getPathAsFile( parent );
		return new File( parent, path.replace( '/', File.separatorChar ) );
	}

	/**
	 * Returns the path to a resource file in the plug-in, taking into account
	 * the base location set on a {@link NamePage}.
	 *
	 * @param np the name page where the user set a base location
	 * @param path the path relative to the base location, e.g., icons/up.png
	 * @return the complete resource path, e.g., myplugin/icons/up.png
	 */
	public static String getResourcePath( NamePage np, String path ) {
		String base = np.getPathAsResource();
		if( path != null ) base += path;
		return base;
	}
}
