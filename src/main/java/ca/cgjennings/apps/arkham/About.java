package ca.cgjennings.apps.arkham;

import ca.cgjennings.apps.arkham.plugins.engine.SEScriptEngineFactory;
import ca.cgjennings.i18n.IntegerPluralizer;
import ca.cgjennings.platform.DesktopIntegration;
import ca.cgjennings.text.SETemplateProcessor;
import ca.cgjennings.ui.BlankIcon;
import ca.cgjennings.ui.EditorPane;
import ca.cgjennings.ui.StyleUtilities;
import ca.cgjennings.ui.anim.AnimationUtilities;
import ca.cgjennings.ui.dnd.ScrapBook;
import ca.cgjennings.ui.theme.Theme;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import javax.lang.model.SourceVersion;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JScrollBar;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import resources.Language;
import static resources.Language.string;
import resources.ResourceKit;

/**
 * Displays information about the application to the user. Also rumoured to
 * contain Easter Eggs.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
final class About extends javax.swing.JDialog {

    About() {
        super(StrangeEons.getWindow(), true);

        initComponents();
        getRootPane().setDefaultButton(okBtn);

        try {
            final String aboutDoc = createAboutText();
            aboutText.getDocument().putProperty("IgnoreCharsetDirective", Boolean.TRUE);
            aboutText.setText(aboutDoc);
        } catch (Exception e) {
            StrangeEons.log.log(Level.SEVERE, "failed to compose about text", e);
        }
        aboutText.select(0, 0);

        getRootPane().setBorder(UIManager.getBorder(Theme.MESSAGE_BORDER_DIALOG));
        scrollPane.getVerticalScrollBar().setBorder(BorderFactory.createEmptyBorder());

        aboutText.addHyperlinkListener((HyperlinkEvent e) -> {
            if (e.getEventType() != HyperlinkEvent.EventType.ACTIVATED) {
                return;
            }
            URL url = e.getURL();
            if (url != null) {
                try {
                    DesktopIntegration.browse(url.toURI(), About.this);
                } catch (Exception ex) {
                    StrangeEons.log.log(Level.WARNING, "exception opening link " + url, ex);
                }
            }
        });

        MouseAdapter scrollStopper = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                stopAutoscrolling();
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                stopAutoscrolling();
            }
        };
        scrollPane.getVerticalScrollBar().addMouseListener(scrollStopper);
        scrollPane.addMouseListener(scrollStopper);
        aboutText.addMouseListener(scrollStopper);

        autoscrollTimer = new Timer(2_000, new ActionListener() {
            private int y = 0;
            private final JScrollBar bar = scrollPane.getVerticalScrollBar();

            @Override
            public void actionPerformed(ActionEvent e) {
                if (y < bar.getMaximum()) {
                    bar.setValue(++y);
                } else {
                    stopAutoscrolling();
                }
            }
        });
        autoscrollTimer.setDelay(1_000 / 20);
        autoscrollTimer.start();

        setLocationRelativeTo(getParent());
        StyleUtilities.setWindowOpacity(this, START_ALPHA);

        buildTable();

        final Icon secretIcon = new BlankIcon(18, 18);
        secretBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // *** SOMETHING SECRET ***
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                AnimationUtilities.animateIconTransition(secretBtn, ResourceKit.getIcon("mt127"));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                AnimationUtilities.animateIconTransition(secretBtn, secretIcon);
            }
        });
    }

    private void buildTable() {
        StringBuilder html = new StringBuilder(256);
        StringBuilder plain = new StringBuilder(256);
        html.append("<html><table border=0 cellspacing=0 cellpadding=0>");

        buildRow(html, plain, "Strange Eons", StrangeEons.getVersionString());
        buildRow(html, plain, "Java runtime", System.getProperty("java.version"));
        Object jdk = null;
        try {
            JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
            if (javac != null) {
                Set<SourceVersion> versions = javac.getSourceVersions();
                SourceVersion max = null;
                for (SourceVersion v : versions) {
                    if (max == null || max.ordinal() < v.ordinal()) {
                        max = v;
                    }
                }
                if (max != null) {
                    jdk = "JDK " + max.ordinal();
                }
            }
        } catch (Throwable t) {
        }
        buildRow(html, plain, "Compiler engine", jdk);
        buildRow(html, plain, "Script engine", SEScriptEngineFactory.getVersion());
        buildRow(html, plain, "User folder", StrangeEons.getUserStorageFile(null));
        html.append("</table>");

        final String plainText = plain.toString();
        systemInfo.setTipText(html.toString());
        systemInfo.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                ScrapBook.setText(plainText);
            }
        });
    }

    private static void buildRow(StringBuilder h, StringBuilder p, String lhs, Object rhs) {
        if (rhs == null) {
            rhs = "n/a";
        }
        h.append("<tr><td><b>").append(ResourceKit.makeStringHTMLSafe(lhs))
                .append("&nbsp; &nbsp;</b><td>").append(ResourceKit.makeStringHTMLSafe(String.valueOf(rhs)))
                .append("</tr>");
        p.append(lhs).append(": ").append(rhs).append('\n');
    }

    private static final float FADE_TIME = .5f;
    private static final float FINAL_ALPHA = 1f;
    private static final float START_ALPHA = 1f / 255f;

    @Override
    public void dispose() {
        stopAutoscrolling();
        super.dispose();
    }

    private Timer autoscrollTimer;

    private String createAboutText() {
        SETemplateProcessor tp = new SETemplateProcessor();
        tp.set("version", StrangeEons.getVersionString());
        tp.set("bg", String.format(Locale.ROOT, "#%06X", aboutText.getBackground().getRGB() & 0xffffff));
        tp.set("fg", String.format(Locale.ROOT, "#%06X", aboutText.getForeground().getRGB() & 0xffffff));
        tp.set("inv", UIManager.getBoolean("useDarkTheme") ? "-inv" : "");

        // updated Feb 02 2024
        final int ANONYMOUS_COUNT = 13;
        tp.set("supporter-list",
                "Audrey Latimer (Arkham Horror Queen Anne), "
                + "Andrew Carpena, "
                + "Gilles Chiniara, "
                + "The Plaid Mentat (Daniel Crumly), "
                + "Jesse Cruz, "
                + "Mike Dawson, "
                + "Juvenihilist (Patrik Ekström), "
                + "Nathalie Emond, "
                + "Grudunza (Eric Endres), "
                + "Simone Grandini, "
                + "Lenore-the-grateful-twitterer (Ian Hansen), "
                + "Jim Kiefer, "
                + "Jonas Philippe Kocher, "
                + "Sean Marshall, "
                + "Bryce McKay, "
                + "Olivier Mercier, "
                + "Bennett Oppel, "
                + "Charles Robb, "
                + "Paul Sacco, "
                + "Sergio Garcia Sanchez, "
                + "Isaac Juan Tomas, "
                + "Tobias Walter, "
                + "Mateusz Wasilewski, "
        );
        tp.set("anon-count",
                String.format(IntegerPluralizer.create().pluralize(Language.getInterface(), ANONYMOUS_COUNT, "ab-supporters-anon"), ANONYMOUS_COUNT)
        );

        String[] langs = new String[]{
            "bg", "fr", "cs", "de", "it", "pl", "ru", "es"
        };
        for (String lang : langs) {
            Locale loc = new Locale(lang);
            tp.set(lang, loc.getDisplayLanguage());
        }

        return tp.processFromResource("text/interface/about.template");
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        cardPanel = new javax.swing.JPanel();
        scrollPane = new javax.swing.JScrollPane();
        aboutText = new EditorPane();
        secretPane = new javax.swing.JPanel();
        okBtn = new javax.swing.JButton();
        secretBtn = new javax.swing.JButton();
        titleLabel = new ca.cgjennings.ui.TitleLabel();
        homePageBtn = new ca.cgjennings.ui.JLinkLabel();
        systemInfo = new ca.cgjennings.ui.JTip();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(string("ab-title")); // NOI18N
        setUndecorated(true);
        setResizable(false);
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentShown(java.awt.event.ComponentEvent evt) {
                formComponentShown(evt);
            }
        });
        getContentPane().setLayout(new java.awt.GridBagLayout());

        cardPanel.setLayout(new java.awt.CardLayout());

        scrollPane.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, java.awt.Color.lightGray));
        scrollPane.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                scrollPaneMouseWheelMoved(evt);
            }
        });
        scrollPane.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                aboutTextMousePressed(evt);
            }
        });

        aboutText.setEditable(false);
        aboutText.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 4, 4, 4));
        aboutText.setContentType("text/html"); // NOI18N
        aboutText.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                aboutTextMousePressed(evt);
            }
        });
        scrollPane.setViewportView(aboutText);

        cardPanel.add(scrollPane, "about");

        secretPane.setLayout(new java.awt.BorderLayout());
        cardPanel.add(secretPane, "info");

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(cardPanel, gridBagConstraints);

        okBtn.setFont(okBtn.getFont().deriveFont(okBtn.getFont().getSize()-1f));
        okBtn.setText(string("ab-close")); // NOI18N
        okBtn.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 8, 4, 8));
        okBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(3, 4, 3, 4);
        getContentPane().add(okBtn, gridBagConstraints);

        secretBtn.setFont(secretBtn.getFont().deriveFont(secretBtn.getFont().getSize()-1f));
        secretBtn.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 8, 1, 8));
        secretBtn.setContentAreaFilled(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 1, 4);
        getContentPane().add(secretBtn, gridBagConstraints);

        titleLabel.setBackground(UIManager.getColor(Theme.PROJECT_HEADER_BACKGROUND)
        );
        titleLabel.setForeground(UIManager.getColor(Theme.PROJECT_HEADER_FOREGROUND)
        );
        titleLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        titleLabel.setText(string("ab-title")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        getContentPane().add(titleLabel, gridBagConstraints);

        homePageBtn.setText(string("app-home")); // NOI18N
        homePageBtn.setURI(  homePage );
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 1, 4);
        getContentPane().add(homePageBtn, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(1, 12, 1, 8);
        getContentPane().add(systemInfo, gridBagConstraints);

        setSize(new java.awt.Dimension(561, 389));
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents
    private void okBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okBtnActionPerformed
        AnimationUtilities.animateOpacityTransition(this, FINAL_ALPHA, 0f, FADE_TIME, true);
    }//GEN-LAST:event_okBtnActionPerformed

    private java.net.URI homePage;

    {
        try {
            homePage = new java.net.URI("http://cgjennings.ca/eons/");
        } catch (URISyntaxException ex) {
            StrangeEons.log.log(Level.SEVERE, null, ex);
        }
    }

    private void aboutTextMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_aboutTextMousePressed
        stopAutoscrolling();
    }//GEN-LAST:event_aboutTextMousePressed

	private void scrollPaneMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_scrollPaneMouseWheelMoved
            stopAutoscrolling();
	}//GEN-LAST:event_scrollPaneMouseWheelMoved

	private void formComponentShown( java.awt.event.ComponentEvent evt ) {//GEN-FIRST:event_formComponentShown
            EventQueue.invokeLater(() -> {
                AnimationUtilities.animateOpacityTransition(About.this, START_ALPHA, FINAL_ALPHA, FADE_TIME, false);
            });
	}//GEN-LAST:event_formComponentShown

    private void stopAutoscrolling() {
        if (autoscrollTimer != null) {
            autoscrollTimer.stop();
            autoscrollTimer = null;
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    javax.swing.JEditorPane aboutText;
    private javax.swing.JPanel cardPanel;
    private ca.cgjennings.ui.JLinkLabel homePageBtn;
    private javax.swing.JButton okBtn;
    private javax.swing.JScrollPane scrollPane;
    private javax.swing.JButton secretBtn;
    private javax.swing.JPanel secretPane;
    private ca.cgjennings.ui.JTip systemInfo;
    private ca.cgjennings.ui.TitleLabel titleLabel;
    // End of variables declaration//GEN-END:variables
}
