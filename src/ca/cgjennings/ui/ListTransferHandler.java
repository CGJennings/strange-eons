package ca.cgjennings.ui;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.TransferHandler;

/**
 * A transfer handler for moving items between lists.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
public class ListTransferHandler extends TransferHandler {

    private Class<?> contentClass;

    /**
     * This can be used as the transfer handler for a "rubbish can". Objects can
     * be moved to it but not dragged from it.
     */
    public static class RubbishTransferHandler extends ListTransferHandler {

        public RubbishTransferHandler() {
        }

        @Override
        public int getSourceActions(JComponent c) {
            return TransferHandler.NONE;
        }

        @Override
        public boolean canImport(TransferSupport support) {
            if (super.canImport(support)) {
                if (support.isDrop()) {
                    if ((support.getSourceDropActions() & MOVE) == MOVE) {
                        support.setDropAction(MOVE);
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public boolean importData(JComponent c, Transferable t) {
            return true;
        }

        @Override
        protected void exportDone(JComponent c, Transferable data, int action) {
        }
    }

    protected DataFlavor localArrayListFlavor, serialArrayListFlavor;
    protected String localArrayListType = DataFlavor.javaJVMLocalObjectMimeType + ";class=java.util.ArrayList";
    protected int[] indices = null;
    protected int addIndex = -1; //Location where items were added
    protected int addCount = 0;  //Number of items added

    public ListTransferHandler() {
        this(null);
    }

    public ListTransferHandler(Class<?> contentClass) {
        try {
            localArrayListFlavor = new DataFlavor(localArrayListType);
        } catch (ClassNotFoundException e) {
            System.out.println("failed to create data flavor");
        }
        serialArrayListFlavor = new DataFlavor(ArrayList.class, "ArrayList");
        this.contentClass = contentClass;
    }

    @Override
    public Icon getVisualRepresentation(Transferable t) {
        ArrayList<?> alist = getTransferData(t);
        for (int i = 0; i < alist.size(); ++i) {
            Object o = alist.get(i);
            if (o instanceof IconProvider) {
                return ((IconProvider) o).getIcon();
            }
            if (o instanceof Icon) {
                return (Icon) o;
            }
        }
        return null;
    }

    protected ArrayList<?> getTransferData(Transferable t) {
        ArrayList<?> alist = null;
        try {
            if (hasLocalArrayListFlavor(t.getTransferDataFlavors())) {
                alist = (ArrayList<?>) t.getTransferData(localArrayListFlavor);
            } else if (hasSerialArrayListFlavor(t.getTransferDataFlavors())) {
                alist = (ArrayList<?>) t.getTransferData(serialArrayListFlavor);
            } else {
                return null;
            }
        } catch (UnsupportedFlavorException | IOException ufe) {
            ufe.printStackTrace();
            return null;
        }
        return alist;
    }

    @Override
    public boolean importData(TransferSupport transfer) {
        JList target = (JList) transfer.getComponent();

        int index = 0;
        if (transfer.isDrop()) {
            JList.DropLocation drop = (JList.DropLocation) transfer.getDropLocation();
            index = drop.getIndex();
            if (drop.isInsert()) {
                --index;
            }
        } else {
            index = target.getSelectedIndex();
            if (index < 0) {
                index = target.getModel().getSize() - 1;
            }
        }
        ArrayList<?> alist = getTransferData(transfer.getTransferable());
        if (alist == null) {
            return false;
        }

        if (contentClass != null) {
            for (Object o : alist) {
                if (!contentClass.isInstance(o)) {
                    return false;
                }
            }
        }

        DefaultListModel listModel = (DefaultListModel) target.getModel();
        int max = listModel.getSize();

        if (index < -1) {
            index = -1;
        }
        index++;
        if (index > max) {
            index = max;
        }

        addIndex = index;
        addCount = alist.size();
        for (int i = 0; i < alist.size(); i++) {
            listModel.add(index++, alist.get(i));
        }
        return true;
    }

    @Override
    protected void exportDone(JComponent c, Transferable data, int action) {
        if ((action == MOVE) && (indices != null)) {
            DefaultListModel model = (DefaultListModel) ((JList) c).getModel();

            //If we are moving items around in the same list, we
            //need to adjust the indices accordingly since those
            //after the insertion point have moved.
            if (addCount > 0) {
                for (int i = 0; i < indices.length; i++) {
                    if (indices[i] >= addIndex
                            && indices[i] + addCount < model.getSize()) {
                        indices[i] += addCount;
                    }
                }
            }
            for (int i = indices.length - 1; i >= 0; i--) {
                model.remove(indices[i]);
            }
        }
        indices = null;
        addIndex = -1;
        addCount = 0;
    }

    private boolean hasLocalArrayListFlavor(DataFlavor[] flavors) {
        if (localArrayListFlavor == null) {
            return false;
        }

        for (DataFlavor flavor : flavors) {
            if (flavor.equals(localArrayListFlavor)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasSerialArrayListFlavor(DataFlavor[] flavors) {
        if (serialArrayListFlavor == null) {
            return false;
        }

        for (DataFlavor flavor : flavors) {
            if (flavor.equals(serialArrayListFlavor)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canImport(JComponent c, DataFlavor[] flavors) {
        if (hasLocalArrayListFlavor(flavors)) {
            return true;
        }
        if (hasSerialArrayListFlavor(flavors)) {
            return true;
        }
        return false;
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        if (c instanceof JList) {
            JList source = (JList) c;
            indices = source.getSelectedIndices();
            List values = source.getSelectedValuesList();
            if (values.isEmpty()) {
                return null;
            }
            ArrayList alist = values instanceof ArrayList ? (ArrayList) values : new ArrayList(values);
            return new ListTransferable(alist);
        }
        return null;
    }

    @Override
    public int getSourceActions(JComponent c) {
        return COPY_OR_MOVE;
    }

    public class ListTransferable implements Transferable {

        ArrayList data;

        public ListTransferable(ArrayList alist) {
            data = alist;
        }

        @Override
        public Object getTransferData(DataFlavor flavor)
                throws UnsupportedFlavorException {
            if (!isDataFlavorSupported(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            return data;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{localArrayListFlavor,
                serialArrayListFlavor};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            if (localArrayListFlavor.equals(flavor)) {
                return true;
            }
            if (serialArrayListFlavor.equals(flavor)) {
                return true;
            }
            return false;
        }
    }
}
