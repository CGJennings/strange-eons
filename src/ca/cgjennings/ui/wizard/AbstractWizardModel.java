package ca.cgjennings.ui.wizard;

import java.util.LinkedList;
import javax.swing.JComponent;

/**
 * An abstract base class for wizard models. This base class provides all the
 * functionality required by a wizard model except for
 * {@link WizardModel#getPageOrder()}. Because the implementations of the other
 * methods must call <code>getPageOrder()</code>, subclasses that do not
 * represent the page order internally as an array should consider caching the
 * <code>getPageOrder()</code> for performance reasons.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public abstract class AbstractWizardModel implements WizardModel {

    protected boolean blocked;
    protected int curPage = 0;
    protected LinkedList<WizardListener> listeners = new LinkedList<>();

    @Override
    public void aboutToHide(int index, JComponent page) {
        fireHidingPage(index, page);
    }

    @Override
    public void aboutToShow(int index, JComponent page) {
        fireShowingPage(index, page);
    }

    @Override
    public void addWizardListener(WizardListener li) {
        if (li == null) {
            throw new NullPointerException("li");
        }
        for (WizardListener e : listeners) {
            if (e == li) {
                return;
            }
        }
        listeners.add(li);
    }

    @Override
    public int backward() {
        checkPages();
        if (canGoBackward()) {
            if (isProgressBlocked()) {
                setProgressBlocked(false);
            }
            setCurrentPage(curPage - 1);
            return curPage;
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    public boolean canFinish() {
        checkPages();
        return !isProgressBlocked();
    }

    @Override
    public boolean canGoBackward() {
        checkPages();
        return curPage > 0;
    }

    @Override
    public boolean canGoForward() {
        checkPages();
        return (!isProgressBlocked()) && (curPage < getPageCount() - 1);
    }

    protected void checkPages() {
        if (getPageOrder() == null) {
            throw new IllegalStateException("no pages have been set");
        }
    }

    @Override
    public Object finish() {
        if (!canFinish()) {
            throw new IllegalStateException();
        }
        fireFinished();
        return null;
    }

    protected void fireBlockStateChanged() {
        WizardEvent e = new WizardEvent(this, getCurrentPage(), getPageOrder()[getCurrentPage()]);
        for (WizardListener li : listeners) {
            li.wizardBlockStateChanged(e);
        }
    }

    protected void fireFinished() {
        WizardEvent e = new WizardEvent(this, getCurrentPage(), getPageOrder()[getCurrentPage()]);
        for (WizardListener li : listeners) {
            li.wizardFinished(e);
        }
    }

    protected void fireHidingPage(int oldPage, JComponent page) {
        WizardEvent e = new WizardEvent(this, oldPage, page);
        for (WizardListener li : listeners) {
            li.wizardHidingPage(e);
        }
    }

    protected void firePageChanged(int newPage, JComponent page) {
        WizardEvent e = new WizardEvent(this, newPage, page);
        for (WizardListener li : listeners) {
            li.wizardPageChanged(e);
        }
    }

    protected void firePageOrderChanged() {
        WizardEvent e = new WizardEvent(this);
        for (WizardListener li : listeners) {
            li.wizardPageOrderChanged(e);
        }
    }

    protected void fireReset() {
        WizardEvent e = new WizardEvent(this);
        for (WizardListener li : listeners) {
            li.wizardReset(e);
        }
    }

    protected void fireShowingPage(int newPage, JComponent page) {
        WizardEvent e = new WizardEvent(this, newPage, page);
        for (WizardListener li : listeners) {
            li.wizardShowingPage(e);
        }
    }

    @Override
    public int forward() {
        checkPages();
        if (canGoForward()) {
            setCurrentPage(curPage + 1);
            return curPage;
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    public int getCurrentPage() {
        checkPages();
        return curPage;
    }

    @Override
    public int getPageCount() {
        checkPages();
        return getPageOrder().length;
    }

    @Override
    public boolean isProgressBlocked() {
        return blocked;
    }

    @Override
    public void removeWizardListener(WizardListener li) {
        if (li == null) {
            throw new NullPointerException("li");
        }
        listeners.remove(li);
    }

    @Override
    public void reset() {
        checkPages();
        setCurrentPage(0);
        fireReset();
    }

    @Override
    public void setCurrentPage(int index) {
        checkPages();
        if (index < 0 || index >= getPageCount()) {
            throw new IndexOutOfBoundsException("index=" + index);
        }
        if (index != curPage) {
            aboutToHide(curPage, getPageOrder()[curPage]);
            if (isProgressBlocked()) {
                setProgressBlocked(false);
            }
            aboutToShow(index, getPageOrder()[index]);
            curPage = index;
            firePageChanged(curPage, getPageOrder()[curPage]);
        }
    }

    public JComponent getCurrentPageComponent() {
        return getPageOrder()[getCurrentPage()];
    }

    @Override
    public void setProgressBlocked(boolean block) {
        if (block != blocked) {
            blocked = block;
            fireBlockStateChanged();
        }
    }

}
