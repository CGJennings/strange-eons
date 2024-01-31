package ca.cgjennings.apps.arkham.plugins.debugging;

import java.awt.EventQueue;
import java.awt.Font;
import java.net.InetAddress;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import javax.swing.JButton;
import javax.swing.JLabel;

/**
 * Dialog that scans for local debug servers, allowing one to be selected for
 * connection.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.2
 */
public final class DiscoveryDialog extends javax.swing.JDialog {

    /**
     * Creates new form DiscoveryDialog
     */
    public DiscoveryDialog(java.awt.Frame parent) {
        super(parent, true);
        initComponents();
        validate();
        setLocationRelativeTo(parent);

        Thread searcher = new Thread(() -> {
            try {
                DiscoveryService ds = new DiscoveryService(InetAddress.getLoopbackAddress(), InetAddress.getLocalHost());
                ds.setDiscoveryConsumer(info -> {
                    EventQueue.invokeLater(() -> {
                        try {
                            startNextResult();
                            append(info.pid + '.' + info.hash);
                            append(info.address.getHostName() + ", port " + info.port);
                            append("build " + info.buildNumber + " (" + info.version + ")");
                            append(info.testBundle);
                            endResult(info);
                        } catch (Exception ex) {
                            System.err.println(ex);
                        }
                    });
                });
                ds.setProgressListener((obj, ratio) -> {
                    EventQueue.invokeLater(() -> {
                        progressBar.setIndeterminate(false);
                        progressBar.setValue(Math.round(1000f * ratio));
                    });
                    return false;
                });
                ds.search();
                EventQueue.invokeLater(() -> progressBar.setVisible(false));
            } catch (Exception tex) {
                System.err.println(tex);
            }
        }, "Discovery service");
        searcher.setDaemon(true);
        searcher.start();
    }

    private void startNextResult() {
        if (resultPanel.getComponentCount() > 0) {
            resultPanel.add(new JLabel(" "));
        }
        JLabel head = new JLabel(string("scan-server-head") + ' ' + (++numFound));
        head.setFont(head.getFont().deriveFont(Font.BOLD));
        resultPanel.add(head);
    }

    private void append(String result) {
        JLabel line = new JLabel(result);
        resultPanel.add(line);
    }

    private void endResult(DiscoveryService.ServerInfo info) {
        JButton connect = new JButton(string("connect-btn"));
        connect.setFont(connect.getFont().deriveFont(connect.getFont().getSize2D() - 2f));
        connect.addActionListener(li -> {
            selectedServer = info;
            DiscoveryDialog.this.dispose();
        });
        resultPanel.add(connect);
        resultPanel.validate();
    }

    private DiscoveryService.ServerInfo selectedServer;
    private int numFound = 0;

    public DiscoveryService.ServerInfo getSelectedServer() {
        return selectedServer;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        closeBtn = new javax.swing.JButton();
        resultScroller = new javax.swing.JScrollPane();
        resultPanel = new javax.swing.JPanel();
        progressBar = new javax.swing.JProgressBar();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(string("scan-title")); // NOI18N

        closeBtn.setText(string("close")); // NOI18N
        closeBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeBtnActionPerformed(evt);
            }
        });

        resultPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 6, 6, 6));
        resultPanel.setLayout(new javax.swing.BoxLayout(resultPanel, javax.swing.BoxLayout.PAGE_AXIS));
        resultScroller.setViewportView(resultPanel);

        progressBar.setMaximum(1000);
        progressBar.setIndeterminate(true);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, 294, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 20, Short.MAX_VALUE)
                        .addComponent(closeBtn))
                    .addComponent(resultScroller))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(resultScroller, javax.swing.GroupLayout.DEFAULT_SIZE, 244, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(closeBtn))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void closeBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeBtnActionPerformed
        dispose();
    }//GEN-LAST:event_closeBtnActionPerformed

    private static String string(String key) {
        try {
            return ResourceBundle.getBundle("resources/text/interface/debugger").getString(key);
        } catch (MissingResourceException mre) {
            return "MISSING: " + key;
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton closeBtn;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JPanel resultPanel;
    private javax.swing.JScrollPane resultScroller;
    // End of variables declaration//GEN-END:variables
}
