package resources.projects.pluginwizard;

import ca.cgjennings.apps.arkham.plugins.catalog.CatalogID;
import ca.cgjennings.apps.arkham.project.PluginWizardDialog;
import ca.cgjennings.apps.arkham.project.ProjectUtilities;
import ca.cgjennings.apps.arkham.project.Task;
import ca.cgjennings.text.SETemplateProcessor;
import ca.cgjennings.ui.wizard.DefaultWizardModel;
import ca.cgjennings.ui.wizard.WizardModel;
import java.io.File;
import java.io.IOException;
import javax.swing.JComponent;
import static resources.Language.string;
import static resources.projects.pluginwizard.KitUtilities.*;

/**
 * A plug-in wizard kit that creates basic skeleton code for a plug-in.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class SkeletonKit implements PluginWizardDialog.WizardKit {
	private boolean compiledMode;

	/**
	 * Creates a new plug-in wizard kit for creating basic plug-ins.
	 */
	public SkeletonKit() {
	}

	@Override
	public String getName() {
		return string( "plugin-wiz-kit-skel" );
	}

	@Override
	public String getDescription() {
		return string( "plugin-wiz-kit-skel-desc" );
	}



	@Override
	public WizardModel createModel( Task task ) {
		return new SkeletonModel( task );
	}

	private class SkeletonModel extends DefaultWizardModel {
		private final ContentTypePage tp;
		private final CodeFormatPage cp;
		private final NamePage np;

		public SkeletonModel( Task task ) {
			tp = new ContentTypePage( true, true, this );
			cp = new CodeFormatPage();
			np = new NamePage( task, this );
			np.setCompiledClassMode( compiledMode );
			setPages( new JComponent[] {
				tp, cp, np
			});
		}

		@Override
		public int backward() {
			// skip code page for themes and libraries
			if( getCurrentPageComponent() == np ) {
				ContentType type = tp.getSelectedContentType();
				if( type == ContentType.THEME || type == ContentType.LIBRARY ) {
					setCurrentPage( 0 );
					return 0;
				}
			}
			return super.backward();
		}

		@Override
		public int forward() {
			// skip code page for themes and libraries
			if( getCurrentPageComponent() == tp ) {
				ContentType type = tp.getSelectedContentType();
				if( type == ContentType.THEME || type == ContentType.LIBRARY ) {
					setCurrentPage( 2 );
					return 2;
				}
			}
			return super.forward();
		}

		@Override
		public void aboutToShow( int index, JComponent page ) {
			if( page == np ) {
				ContentType type = tp.getSelectedContentType();

				if( type == ContentType.THEME ) {
					np.setCompiledClassMode( true );
				} else if( type == ContentType.LIBRARY ) {
					np.setCompiledClassMode( false );
				} else {
					np.setCompiledClassMode( cp.isCompiledFormatSelected() );
				}
			}
		}

		@Override
		public void aboutToHide( int index, JComponent page ) {
			if( page == tp ) {
				// update the file extension for the selected plug-in type
				// on the name page
				ContentType type = tp.getSelectedContentType();
				np.setPluginType( type );
			}
			super.aboutToHide( index, page );
		}
	}

	@Override
	public void createTask( WizardModel model, Task task ) throws Exception {
		SkeletonModel skelModel = (SkeletonModel) model;
		ContentType type = skelModel.tp.getSelectedContentType();
		boolean compiledModeSelected = skelModel.cp.isCompiledFormatSelected();
		NamePage np = skelModel.np;

		// set the bundle name make target
		setBundleName( task, np );

		// create the folder for the plug-in script
		makeTaskFolders( task, np, null );

		// create and write the plugin script or class
		String fileName, fileNameNoExtension, code;

		if( type == ContentType.THEME ) {
			compiledMode = true;
		} else {
			compiledMode = compiledModeSelected;
		}

		if( compiledMode ) {
			fileName = np.getClassFileName();
			fileNameNoExtension = np.getClassName();
		} else {
			fileName = np.getScriptFileName();
			fileNameNoExtension = fileName.substring( 0, fileName.lastIndexOf('.') );
		}

		// generate appropriate code
		switch( type ) {
			case LIBRARY:
				code = null;
				// we need to set this to a suitable name for the library icon
				// since there is no script or class to use
				fileNameNoExtension = "library-image";
				np.setCompiledClassMode( false );
				np.setScriptFileName( fileNameNoExtension );
				break;
			case THEME:
				code = fillInThemeTemplate( np );
				break;
			default:
				if( compiledMode ) {
					code = fillInClassPluginTemplate( type, np );
				} else {
					code = fillInScriptPluginTemplate( type, np );
				}
				break;
		}


		// if there is code to write, write it now
		if( code != null ) {
			ProjectUtilities.writeTextFile( getFile( task, np, fileName ), code );
		}

		// make the root file
		String root = fillInRootTemplate( type, np );
		ProjectUtilities.writeTextFile( getFile( task, null, "eons-plugin" ), root );

		// write the icon image
		File iconFile = getFile( task, np, fileNameNoExtension + ".png" );
		np.writePluginIcon( iconFile );
	}

	private String fillInClassPluginTemplate( ContentType pt, NamePage np ) throws IOException {
		String template = getTemplateText( "plugin-template.java_" );
		SETemplateProcessor pr = new SETemplateProcessor();
		setPluginTypeConditionals( pr, pt );
		final String desc = np.getPluginDescription();
		pr.setAll(
				"filename", np.getClassFileName(),
				"plainname", np.getPluginName(),
				"plaindescription", desc,
				"name", SETemplateProcessor.escapeJavaString( np.getPluginName() ),
				"description", desc.isEmpty() ? "null" : SETemplateProcessor.escapeJavaString( desc ),
				"package", np.getPath().replace( '/', '.' ),
				"classname", np.getClassName()
		);

		return pr.process( template );
	}

	private String fillInScriptPluginTemplate( ContentType pt, NamePage np ) throws IOException {
		String template = getTemplateText( "plugin-template.js_" );
		SETemplateProcessor pr = new SETemplateProcessor();
		setPluginTypeConditionals( pr, pt );
		final String desc = np.getPluginDescription();
		pr.setAll(
				"filename", np.getScriptFileName(),
				"plainname", np.getPluginName(),
				"plaindescription", desc,
				"name", SETemplateProcessor.escapeScriptString( np.getPluginName() ),
				"description", desc.isEmpty() ? "null" : SETemplateProcessor.escapeScriptString( desc )
		);
		return pr.process( template );
	}

	private String fillInThemeTemplate( NamePage np ) throws IOException {
		String template = getTemplateText( "theme-template.java_" );
		SETemplateProcessor pr = new SETemplateProcessor();
		final String desc = np.getPluginDescription();
		pr.setAll(
				"filename", np.getClassFileName(),
				"plainname", np.getPluginName(),
				"plaindescription", desc,
				"name", SETemplateProcessor.escapeJavaString( np.getPluginName() ),
				"description", desc.isEmpty() ? "null" : SETemplateProcessor.escapeJavaString( desc ),
				"package", np.getPath().replace( '/', '.' ),
				"classname", np.getClassName()
		);
		// used by theme template to exclude getThemeDescription()
		pr.setCondition( "isDescribed", !desc.isEmpty() );
		return pr.process( template );
	}

	private String fillInRootTemplate( ContentType pt, NamePage np ) throws IOException {
		String template = getTemplateText( "root-template" );
		SETemplateProcessor pr = new SETemplateProcessor();
		setPluginTypeConditionals( pr, pt );
		pr.setCondition( "hasIcon", np.getPluginIconImage() != null );

		String klass;
		if( compiledMode ) {
			klass = np.getPath().replace( '/', '.' ) + '.' + np.getClassName();
		} else {
			klass = "res://" + np.getPathAsResource() + '/' + np.getScriptFileName();
		}
		pr.setAll(
				"name", np.getPluginName(),
				"description", np.getPluginDescription(),
				"catid", new CatalogID().toString(),
				"pluginclass", klass,
				// the klass field of the page was stuffed with the icon name
				"iconres", klass.substring( 0, klass.length() - "js".length() ) + "png"
		);
		return pr.process( template );
	}
}