package ca.cgjennings.ui.textedit.completion;

/**
 * This is the superclass of all API nodes that cannot have children, such as
 * named constants.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public abstract class APILeaf extends APINode {

    public APILeaf(String name) {
        super(name);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * <b>The leaf implementation throws an unsupported operation exception.</b>
     *
     * @throws UnsupportedOperationException
     */
    @Override
    public boolean add(APINode child) {
        throw new UnsupportedOperationException("cannot add children to " + getClass().getSimpleName());
    }
}
