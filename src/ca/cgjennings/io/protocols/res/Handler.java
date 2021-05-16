package ca.cgjennings.io.protocols.res;

import ca.cgjennings.io.protocols.MappedURLHandler;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import resources.ResourceKit;

/**
 * A URL protocol handler for the {@code res:} protocol, which accesses
 * application resources.
 *
 * <p>
 * URLs consist of these segments (where [...] indicates an optional
 * segment):<br>
 * {@code res: [//] [/] [path/]* file}<br>
 * Where:<br>
 * <dl>
 * <dt>[//]<dd> is optional, but makes it easier to distinguish URLs from files
 * in user-supplied strings.
 * <dt>[/]<dd>indicates that the path is not relative to /resources/, but to the
 * default package (/)
 * <dt>[path/]*<dd> is zero or more path entries (subfolders)
 * <dt>[file]<dd> is the name of the resource file
 * </dl>
 *
 * <p>
 * <b>Note:</b> the unusual class name is required for this class to be used by
 * the default protocol handler factory.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class Handler extends MappedURLHandler {

    @Override
    protected URL mapURL(URL sourceURL) throws IOException {
        String path = MappedURLHandler.getComposedPath(sourceURL);
        if (sourceURL.getRef() != null) {
            path += '#' + sourceURL.getRef();
        }
        if (path != null) {
            return ResourceKit.composeResourceURL(path);
        }
        throw new FileNotFoundException(sourceURL.toExternalForm());
    }

//	public static void main( String[] args ) {
//		install();
//		//InputStream in = null;
//		try {
//		URL url = new URL( "res://the/standard/format.png" );
//			System.err.println( url.getPath() );
//		url = new URL( "res:///the/absolute/format.png" );
//			System.err.println( url.getPath() );
//		url = new URL( "res:the/nonstandard/format.png" );
//			System.err.println( url.getPath() );
//		url = new URL( "res:/the/nonstandard/format.png" );
//			System.err.println( url.getPath() );
//		url = new URL( url, "./test" );
//			System.err.println( url.getPath() );
//		//in = url.openStream();
//		//System.err.println( in ); // verify that a stream object was created
//		} catch( Exception e ) {
//			e.printStackTrace();
//		}
////		finally {
////			if( in != null ) try { in.close(); } catch( IOException e ) {}
////		}
//	}
}

//package ca.cgjennings.io.protocols.res;
//
//import java.io.FileNotFoundException;
//import java.io.IOException;
//import java.net.URL;
//import java.net.URLConnection;
//import java.net.URLStreamHandler;
//import java.util.LinkedList;
//import resources.ResourceKit;
//
///**
// * A URL protocol handler for the {@code res://} protocol, which access
// * application resources.
// *
// * Note: the unusual class name is required for this class to be used
// * by the default protocol handler factory.
// *
// * Valid URLs consist of these segments:<br>
// * {@code res: [//] [/] [path/]* file}<br>
// * Where:<br>
// * <dl>
// *  <dt>[//]<dd> is optional, but makes it easier to distinguish URLs from files
// *          in user-supplied strings.
// *  <dt>[/]<dd>indicates that the path is not relative to /resources/, but to the empty package
// *  <dt>[path/]*<dd> is zero or more path entries (subfolders)
// *  <dt>[file]<dd> is the name of the resource file
// * </dl>
// *
// * @author Chris Jennings <https://cgjennings.ca/contact>
// * @since 3.0
// */
//public class Handler extends URLStreamHandler {
//
//	@Override
//	protected void parseURL( URL u, String spec, int start, int limit ) {
//        // This field has already been parsed
//        String ref = u.getRef();
//        // These fields may receive context content if this was relative URL
//        String protocol = u.getProtocol();
//        String authority = u.getAuthority();
//        String userInfo = u.getUserInfo();
//        String host = u.getHost();
//        int port = u.getPort();
//        String path = u.getPath();
//		String query = u.getQuery();
//
//		if( start < limit ) {
//			int slashCount = 0;
//			final int slashLimit = Math.min( start+3, limit );
//			for( int i=start; i<slashLimit; ++i ) {
//				if( spec.charAt( i ) == '/' ) {
//					++slashCount;
//				} else {
//					break;
//				}
//			}
//
//			boolean isRelative = spec.charAt( start ) == '.';
//			if( isRelative && path != null && !path.isEmpty() ) {
//				if( !path.endsWith( "/" ) ) {
//					int lastSlash = path.lastIndexOf( '/' );
//					if( lastSlash >= 0 ) {
//						path = path.substring( 0, lastSlash+1 );
//					} else {
//						path = "";
//					}
//				}
//				path += spec.substring( start, limit );
//			} else {
//				if( slashCount < 2 )
//					path = "//" + spec.substring( start, limit );
//				else
//					path = spec.substring( start, limit );
//			}
//			path = makeAbsolute( path );
//		}
//
//		setURL( u, protocol, host, port, authority, userInfo, path, query, ref );
//	}
//
//	public static String makeAbsolute( String relpath ) {
//		LinkedList<String> out = new LinkedList<String>();
//		String path = removeInitialSlashesFromPath( relpath );
//		if( !path.isEmpty() && path.charAt(0) == '/' ) return path;
//		for( String segment : path.split( "\\/" ) ) {
//			if( segment.equals( "." ) ) {
//				continue;
//			} else if( segment.equals( ".." ) ) {
//				if( out.size() > 0 ) out.removeLast();
//			} else {
//				out.add( segment );
//			}
//		}
//		StringBuilder b = new StringBuilder();
//		for( String segment : out ) {
//			if( b.length() > 0 ) b.append( '/' ); else b.append( "//" );
//			b.append( segment );
//		}
//		return b.toString();
//	}
//
//
//
//	@Override
//	protected URLConnection openConnection( URL u ) throws IOException {
//		String path = u.getPath();
//		if( path != null ) {
//			URL url = ResourceKit.composeResourceURL( removeInitialSlashesFromPath( path ) );
//			if( url != null ) {
//				return url.openConnection();
//			}
//		}
//		throw new FileNotFoundException( u.toExternalForm() );
//	}
//
//	private static String removeInitialSlashesFromPath( String path ) {
//		int initialSlashes = 0;
//		for( ; initialSlashes < path.length() && path.charAt( initialSlashes ) == '/'; ++initialSlashes );
//		if( (initialSlashes & 1) == 1 ) --initialSlashes;
//		if( initialSlashes > 0 ) path = path.substring( initialSlashes );
//		return path;
//	}
//
//	/**
//	 * Installs all custom protocol handlers. There is normally no need to call this as
//	 * Strange Eons will install the handler during initialization.
//	 */
//	public static void install() {
//		String property = System.getProperty( "java.protocol.handler.pkgs", null );
//		if( property == null || !property.contains( "ca.cgjennings.io.protocols" ) ) {
//			if( property == null ) property = "ca.cgjennings.io.protocols";
//			else property = property + '|' + "ca.cgjennings.io.protocols";
//			System.setProperty( "java.protocol.handler.pkgs", property );
//		}
//	}
//
////	public static void main( String[] args ) {
////		install();
////		//InputStream in = null;
////		try {
////		URL url = new URL( "res://the/standard/format.png" );
////			System.err.println( url.getPath() );
////		url = new URL( "res:///the/absolute/format.png" );
////			System.err.println( url.getPath() );
////		url = new URL( "res:the/nonstandard/format.png" );
////			System.err.println( url.getPath() );
////		url = new URL( "res:/the/nonstandard/format.png" );
////			System.err.println( url.getPath() );
////		url = new URL( url, "./test" );
////			System.err.println( url.getPath() );
////		//in = url.openStream();
////		//System.err.println( in ); // verify that a stream object was created
////		} catch( Exception e ) {
////			e.printStackTrace();
////		}
//////		finally {
//////			if( in != null ) try { in.close(); } catch( IOException e ) {}
//////		}
////	}
//}

