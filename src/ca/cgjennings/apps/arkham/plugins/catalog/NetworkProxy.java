package ca.cgjennings.apps.arkham.plugins.catalog;

import ca.cgjennings.apps.arkham.AbstractGameComponentEditor;
import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.platform.AgnosticDialog;
import ca.cgjennings.platform.PlatformSupport;
import ca.cgjennings.ui.FilteredDocument;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JComponent;
import static resources.Language.string;
import resources.RawSettings;

/**
 * Applies and configures network proxy settings. The proxy engine is
 * initialized by calling {@link #install()}. To display a dialog that allows
 * the user to manage proxy settings, call {@link #showProxySettingsDialog}
 * (after the application is running).
 *
 * <p>
 * <b>Note:</b> Proxy passwords are stored using a simple encryption mechanism
 * that will prevent casual snooping, but this should not be relied upon. If
 * proxy password theft is a concern, ensure that other security measures (such
 * as appropriate file permissions) are in place.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
public class NetworkProxy extends javax.swing.JDialog implements AgnosticDialog {

    private NetworkProxy() {
        super(StrangeEons.getWindow(), true);
        initComponents();
        PlatformSupport.makeAgnosticDialog(this, okBtn, cancelBtn);
        AbstractGameComponentEditor.localizeComboBoxLabels(typeCombo, null);
        typeCombo.setSelectedIndex(getProxyType().ordinal());
        hostField.setText(getServer());
        portField.setDocument(FilteredDocument.createDigitDocument());
        portField.setText("" + getPort());
        userField.setText(getUser());
        passField.setText(getPassword());
    }

    /**
     * Closes the configuration dialog as if Cancel was pressed.
     *
     * @param evt
     */
    @Override
    public void handleCancelAction(ActionEvent evt) {
        dispose();
    }

    /**
     * Closes the configuration dialog as if OK was pressed.
     *
     * @param evt
     */
    @Override
    @SuppressWarnings("deprecation")
    public void handleOKAction(ActionEvent evt) {
        setProxyType(ProxyType.values()[typeCombo.getSelectedIndex()]);
        setServer(hostField.getText());
        try {
            setPort(Integer.valueOf(portField.getText()));
        } catch (NumberFormatException e) {
        }
        setUser(userField.getText());
        setPassword(passField.getText());
        dispose();
    }

    private static boolean installed;

    /**
     * This method is called during application startup to install the proxy
     * handling mechanism.
     */
    public static synchronized void install() {
        // Sanity check
        if (installed) {
            throw new IllegalStateException("NetworkProxy.install() already called during startup");
        }

        System.setProperty("java.net.useSystemProxies", "true");
        final ProxySelector dps = ProxySelector.getDefault();
        ProxySelector.setDefault(new ProxySelector() {
            @Override
            @SuppressWarnings("fallthrough")
            public List<Proxy> select(URI uri) {
                switch (getProxyType()) {
                    case PROXY:
                    case SECURE_PROXY:
                        if (isProxied(uri)) {
                            return Collections.singletonList(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(getServer(), getPort())));
                        }
                    // fallthrough
                    case SYSTEM:
                        if (dps != null) {
                            return dps.select(uri);
                        }
                    // fallthrough
                    case DIRECT:
                        return NO_PROXY_LIST;
                    default:
                        throw new AssertionError("Unknown proxy type: " + getProxyType());
                }
            }

            @Override
            public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
                if (uri == null || sa == null || ioe == null) {
                    throw new NullPointerException("null argument passed to handler");
                }
                if (isProxied(uri) && getProxyType().ordinal() > ProxyType.SYSTEM.ordinal()) {
                    if (failures.incrementAndGet() == 5) {
                        System.err.println("Warning: custom proxy settings failed too many times, falling back on default settings");
                        setProxyType(ProxyType.SYSTEM);
                    }
                } else {
                    if (dps != null) {
                        dps.connectFailed(uri, sa, ioe);
                    }
                }
            }

            /**
             * Returns {@code true} if the URI should be passed to the
             * proxy server for possible modification. The current rule, which
             * might be modified in response to future issues, is to proxy only
             * http requests where the host name is not localhost.
             *
             * @param uri the URI to consider
             * @return {@code true} if the proxy server should be consulted
             */
            private boolean isProxied(URI uri) {
                // proxy http URLs where the host is NOT localhost
                // (this only checks for "localhost" not the local IP address,
                // but presumably the proxy would do the right thing in this case)
                final String scheme = uri.getScheme();
                if ("http".equalsIgnoreCase(scheme)) { // null safe order
                    final String host = uri.getHost();
                    if (!"localhost".equalsIgnoreCase(host)) { // null safe order
                        return true;
                    }
                }
                return false;
            }
            private final AtomicInteger failures = new AtomicInteger();
        });

        Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                ProxyType t = getProxyType();
                if (t == ProxyType.SECURE_PROXY) {
                    return new PasswordAuthentication(getUser(), getPassword().toCharArray());
                }
                return null;
            }
        });

        installed = true;
    }

    /**
     * Shared list for returning a result to be accessed via a direct
     * connection.
     */
    private static final List<Proxy> NO_PROXY_LIST = Collections.singletonList(Proxy.NO_PROXY);

    /**
     * Shows a dialog that allows the user to configure proxy settings.
     *
     * @param parent the parent component of the dialog (may be
     * {@code null})
     */
    public static void showProxySettingsDialog(JComponent parent) {
        // Sanity check
        if (!EventQueue.isDispatchThread()) {
            throw new IllegalStateException("NetworkProxy.showProxySettingsDialog must be called from the EDT");
        }

        NetworkProxy d = new NetworkProxy();
        d.setLocationRelativeTo(parent);
        d.setVisible(true);
    }

    public static enum ProxyType {
        DIRECT, SYSTEM, PROXY, SECURE_PROXY
    }

    public static void setProxyType(ProxyType type) {
        RawSettings.setUserSetting(TYPE, type.name());
    }

    public static ProxyType getProxyType() {
        String t = RawSettings.getUserSetting(TYPE);
        if (t != null) {
            try {
                return ProxyType.valueOf(t);
            } catch (IllegalArgumentException e) {
            }
        }
        return ProxyType.SYSTEM;
    }

    public static void setServer(String s) {
        if (s == null) {
            s = "";
        }
        RawSettings.setUserSetting(SERVER, s);
    }

    public static String getServer() {
        String s = RawSettings.getUserSetting(SERVER);
        if (s == null) {
            s = "";
        }
        return s;
    }

    public static void setPort(int port) {
        if (port < 0 || port > 65_535) {
            port = 8_080;
        }
        RawSettings.setUserSetting(PORT, String.valueOf(port));
    }

    public static int getPort() {
        String s = RawSettings.getUserSetting(PORT);
        if (s == null) {
            s = "8080";
        }
        try {
            return Math.max(0, Math.min(65_535, Integer.valueOf(s)));
        } catch (NumberFormatException e) {
        }
        return 80;
    }

    public static void setUser(String s) {
        if (s == null) {
            s = "";
        }
        RawSettings.setUserSetting(USER, s);
    }

    public static String getUser() {
        String s = RawSettings.getUserSetting(USER);
        if (s == null) {
            s = "";
        }
        return s;
    }

    public static void setPassword(String s) {
        if (s == null) {
            s = "";
        }
        s = RawSettings.obfuscate(s);
        RawSettings.setUserSetting(PASS, s);
    }

    public static String getPassword() {
        String s = RawSettings.getUserSetting(PASS);
        if (s == null) {
            return "";
        }
        s = RawSettings.unobfuscate(s);
        return s;
    }

    private static final String TYPE = "proxy-type";
    private static final String SERVER = "proxy-server";
    private static final String PORT = "proxy-port";
    private static final String USER = "proxy-user";
    private static final String PASS = "proxy-pass";

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        proxyPanel = new javax.swing.JPanel();
        hostLabel = new javax.swing.JLabel();
        hostField = new javax.swing.JTextField();
        portLabel = new javax.swing.JLabel();
        portField = new javax.swing.JTextField();
        userLabel = new javax.swing.JLabel();
        passLabel = new javax.swing.JLabel();
        passField = new javax.swing.JPasswordField();
        userField = new javax.swing.JTextField();
        jPanel2 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        typeCombo = new javax.swing.JComboBox();
        cancelBtn = new javax.swing.JButton();
        okBtn = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(string( "proxy-title" )); // NOI18N

        proxyPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(string( "proxy-server" ))); // NOI18N

        hostLabel.setText(string( "proxy-host" )); // NOI18N

        hostField.setColumns(25);

        portLabel.setText(string( "proxy-port" )); // NOI18N

        portField.setColumns(5);
        portField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        portField.setText("80");

        userLabel.setText(string( "proxy-user" )); // NOI18N

        passLabel.setText(string( "proxy-pass" )); // NOI18N

        passField.setColumns(25);
        passField.setEchoChar( '\u25cf' );

        userField.setColumns(25);

        javax.swing.GroupLayout proxyPanelLayout = new javax.swing.GroupLayout(proxyPanel);
        proxyPanel.setLayout(proxyPanelLayout);
        proxyPanelLayout.setHorizontalGroup(
            proxyPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(proxyPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(proxyPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(passLabel)
                    .addComponent(userLabel)
                    .addComponent(hostLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(proxyPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(userField, javax.swing.GroupLayout.DEFAULT_SIZE, 158, Short.MAX_VALUE)
                    .addComponent(passField, javax.swing.GroupLayout.DEFAULT_SIZE, 158, Short.MAX_VALUE)
                    .addComponent(hostField, javax.swing.GroupLayout.DEFAULT_SIZE, 158, Short.MAX_VALUE))
                .addGap(10, 10, 10)
                .addComponent(portLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(portField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        proxyPanelLayout.setVerticalGroup(
            proxyPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(proxyPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(proxyPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(hostLabel)
                    .addComponent(hostField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(portField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(portLabel))
                .addGap(18, 18, 18)
                .addGroup(proxyPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(userLabel)
                    .addComponent(userField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(proxyPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(passLabel)
                    .addComponent(passField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(string( "proxy-settings" ))); // NOI18N

        jLabel2.setText(string( "proxy-l-info2" )); // NOI18N

        jLabel1.setFont(jLabel1.getFont().deriveFont(jLabel1.getFont().getSize()-1f));
        jLabel1.setText(string( "proxy-l-info" )); // NOI18N

        typeCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "proxy-cb-type0", "proxy-cb-type1", "proxy-cb-type2", "proxy-cb-type3" }));
        typeCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                typeComboActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(typeCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabel2)
                    .addComponent(jLabel1))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(typeCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel1)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        cancelBtn.setText(string( "cancel" )); // NOI18N

        okBtn.setText(string( "proxy-ok" )); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(proxyPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(okBtn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelBtn)))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {cancelBtn, okBtn});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(proxyPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 19, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cancelBtn)
                    .addComponent(okBtn))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

	private void typeComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_typeComboActionPerformed
            int i = typeCombo.getSelectedIndex();
            if (i < 0) {
                return; // init: no selection yet
            }
            proxyPanel.setEnabled(i >= 2);
            hostLabel.setEnabled(i >= 2);
            hostField.setEnabled(i >= 2);
            portLabel.setEnabled(i >= 2);
            portField.setEnabled(i >= 2);

            userLabel.setEnabled(i >= 3);
            userField.setEnabled(i >= 3);
            passLabel.setEnabled(i >= 3);
            passField.setEnabled(i >= 3);
	}//GEN-LAST:event_typeComboActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelBtn;
    private javax.swing.JTextField hostField;
    private javax.swing.JLabel hostLabel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JButton okBtn;
    private javax.swing.JPasswordField passField;
    private javax.swing.JLabel passLabel;
    private javax.swing.JTextField portField;
    private javax.swing.JLabel portLabel;
    private javax.swing.JPanel proxyPanel;
    private javax.swing.JComboBox typeCombo;
    private javax.swing.JTextField userField;
    private javax.swing.JLabel userLabel;
    // End of variables declaration//GEN-END:variables
}
