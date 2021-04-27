package resources.projects.pluginwizard;

import ca.cgjennings.apps.arkham.plugins.BundleInstaller;
import ca.cgjennings.apps.arkham.plugins.Plugin;
import ca.cgjennings.apps.arkham.project.MetadataSource;
import ca.cgjennings.ui.IconProvider;
import ca.cgjennings.ui.theme.Theme;
import javax.swing.Icon;
import static resources.Language.string;

/**
 * An enumeration of the different kinds of pluggable content, including themes
 * and libraries (which are stored and loaded like true plug-ins).
 */
public enum ContentType implements IconProvider {
	/** An {@link Plugin#ACTIVATED ACTIVATED} plug-in. */
	ACTIVATED,
	/** An {@link Plugin#INJECTED INJECTED} plug-in. */
	INJECTED,
	/** An {@link Plugin#EXTENSION EXTENSION} plug-in. */
	EXTENSION,
	/** A {@linkplain Theme theme}. */
	THEME,
	/** A library bundle. */
	LIBRARY;

	/**
	 * Returns a small icon that represents the plug-in type.
	 *
	 * @return a small plug-in icon
	 */
	@Override
	public Icon getIcon() {
		switch( this ) {
			case EXTENSION:
				return MetadataSource.ICON_EON_EXTENSION;
			case THEME:
				return MetadataSource.ICON_EON_THEME;
			case LIBRARY:
				return MetadataSource.ICON_EON_LIBRARY;
			default:
				return MetadataSource.ICON_EON_PLUGIN;
		}
	}

	/**
	 * Returns the file name extension, including initial dot, used by
	 * plug-in bundles that store plug-ins of this type.
	 *
	 * @return the file name extension for the plug-in type, such as ".seplugin".
	 */
	public String getBundleFileExtension() {
		switch( this ) {
			case EXTENSION:
				return BundleInstaller.EXTENSION_FILE_EXT;
			case THEME:
				return BundleInstaller.THEME_FILE_EXT;
			case LIBRARY:
				return BundleInstaller.LIBRARY_FILE_EXT;
			default:
				return BundleInstaller.PLUGIN_FILE_EXT;
		}
	}
	
	private int type() {
		int t;
		switch( this ) {
			case ACTIVATED: t=0; break;
			case INJECTED: t=1; break;
			case EXTENSION: t=2; break;
			case THEME: t=3; break;
			case LIBRARY: t=4; break;
			default: throw new AssertionError();
		}
		return t;
	}

	/**
	 * Returns a localized description of the role played by this plug-in type.
	 *
	 * @return a description of the plug-in type
	 */
	public String getDescription() {
		return string( "prj-l-plugin-wiz-type-desc-" + type() );
	}

	/**
	 * Returns a localized name for this kind of plug-in.
	 *
	 * @return the name of the plug-in type
	 */
	@Override
	public String toString() {
		return string( "prj-l-plugin-wiz-type-" + type() );
	}
}
