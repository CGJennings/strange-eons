package resources;

import java.io.IOException;
import java.net.URL;

/**
 * A base class for resources that are cached by the {@link ResourceKit}.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
abstract class CacheBase<R> extends AbstractResourceCache<String, R> {

    public CacheBase(Class<? extends R> contentType, String name) {
        super(contentType, name);
    }

    @Override
    protected R loadResource(String canonicalIdentifier) {
        URL url = ResourceKit.composeResourceURL(canonicalIdentifier);
        if (url == null) {

        }
        try {
            return loadResource(url);
        } catch (Exception e) {

        }
        return null;
    }

    protected abstract R loadResource(URL url) throws IOException;

    @Override
    protected String canonicalizeIdentifier(String identifier) {
        return ResourceKit.normalizeResourceIdentifier(identifier);
    }

    @Override
    protected boolean allowCaching(String canonicalIdentifier, R loadedResource) {
        return ResourceKit.isResourceStatic(ResourceKit.composeResourceURL(canonicalIdentifier));
    }
}
