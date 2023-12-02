/*
 * Copyright The mod_cluster Project Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.modcluster.demo.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.EnumSet;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jboss.modcluster.demo.client.RequestDriver.ClientStatus;
import org.jboss.modcluster.demo.client.load.ServerLoadParam;
import org.jboss.modcluster.demo.client.load.ServerLoadServlets;
import org.jfree.chart.ChartPanel;

/**
 * Client application for demonstrating load balancing with mod_cluster.
 *
 * @author Brian Stansberry
 */
public class ModClusterDemo {
    private static final String DEFAULT_HOST_NAME = System.getProperty("mod_cluster.proxy.host", "localhost");
    private static final String DEFAULT_PROXY_PORT = System.getProperty("mod_cluster.proxy.port", "8000");
    private static final String DEFAULT_CONTEXT_PATH = "load-demo";
    private static final String DEFAULT_SESSION_TIMEOUT = "20";
    private static final int DEFAULT_NUM_THREADS = 80;
    private static final int DEFAULT_SESSION_LIFE = 120;
    private static final int DEFAULT_SLEEP_TIME = 100;
    private static final int DEFAULT_STARTUP_TIME = 120;

    private final RequestDriver requestDriver;
    private final ChartManager chartManager;
    private final Timer timer;
    private TimerTask currentTask;
    private JFrame frame;
    private JTextField proxyHostNameField;
    private JTextField proxyPortField;
    private JTextField contextPathField;
    private JCheckBox destroySessionField;
    private JTextField numThreadsField;
    private JTextField sessionLifeField;
    private JLabel sessionTimeoutLabel;
    private JTextField sessionTimeoutField;
    private JTextField sleepTimeField;
    private JTextField startupTimeField;
    private JTextField targetHostNameField;
    private JTextField targetPortField;
    private JLabel totalClientsLabel;
    private JLabel liveClientsLabel;
    private JLabel failedClientsLabel;
    private JLabel totalClientsLabelReq;
    private JLabel liveClientsLabelReq;
    private JLabel failedClientsLabelReq;
    private JLabel totalClientsLabelSess;
    private JLabel liveClientsLabelSess;
    private JLabel failedClientsLabelSess;

    private ServerLoadServlets selectedLoadServlet;
    private JLabel targetServletParamLabel1;
    private JTextField targetServletParamField1;
    private JLabel targetServletParamLabel2;
    private JTextField targetServletParamField2;

    /**
     * Launch the application
     *
     * @param args
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    ModClusterDemo window = new ModClusterDemo();
                    window.frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Create the application
     */
    public ModClusterDemo() {
        this.requestDriver = new RequestDriver();
        this.chartManager = new ChartManager(this.requestDriver.getRequestCounts(), this.requestDriver.getSessionCounts());
        this.timer = new Timer("ModClusterDemoTimer", false);

        // Set up GUI
        createContents();
    }

    /**
     * Initialize the contents of the frame
     */
    private void createContents() {
        frame = new JFrame();
        frame.setBounds(100, 100, 675, 422);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setTitle("Load Balancing Demonstration");
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                ModClusterDemo.this.stop();
                ModClusterDemo.this.timer.cancel();
            }
        });

        final JTabbedPane tabbedPane = new JTabbedPane();
        frame.getContentPane().add(tabbedPane, BorderLayout.CENTER);

        tabbedPane.addTab("Client Control", null, createClientControlPanel(), null);

        final JPanel loadPanel = createServerLoadControlPanel();
        tabbedPane.addTab("Server Load Control", null, loadPanel, null);

        tabbedPane.addTab("Request Balancing", null, createRequestBalancingPanel(), null);

        tabbedPane.addTab("Session Balancing", null, createSessionBalancingPanel(), null);

        // Load the target host/proxy fields from the client control panel
        // the first time their panel gets focus
        tabbedPane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if (loadPanel.equals(tabbedPane.getSelectedComponent())) {
                    String text = targetHostNameField.getText();
                    if (text == null || text.length() == 0)
                        targetHostNameField.setText(proxyHostNameField.getText());
                    text = targetPortField.getText();
                    if (text == null || text.length() == 0)
                        targetPortField.setText(proxyPortField.getText());
                }
            }
        });
    }

    private JPanel createClientControlPanel() {
        final JPanel controlPanel = new JPanel();
        GridBagLayout gridBagLayout = new GridBagLayout();
        gridBagLayout.columnWidths = new int[] { 7, 0, 7, 0, 7, 0, 7, 7, 0, 0, 7 };
        gridBagLayout.rowHeights = new int[] { 0, 7, 0, 7, 0, 7, 0, 7, 0, 7, 0 };
        controlPanel.setLayout(gridBagLayout);

        JLabel label = new JLabel();
        label.setText("Proxy Hostname:");
        label.setToolTipText("Hostname clients should request");
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weighty = 1;
        gridBagConstraints.anchor = GridBagConstraints.SOUTH;
        controlPanel.add(label, gridBagConstraints);

        proxyHostNameField = new JTextField();
        proxyHostNameField.setText(DEFAULT_HOST_NAME);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridx = 3;
        gridBagConstraints.weightx = 1;
        gridBagConstraints.weighty = 1;
        gridBagConstraints.anchor = GridBagConstraints.SOUTH;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        controlPanel.add(proxyHostNameField, gridBagConstraints);

        label = new JLabel();
        label.setText("Proxy Port:");
        label.setToolTipText("Port clients should request");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridx = 6;
        gridBagConstraints.weighty = 1;
        gridBagConstraints.anchor = GridBagConstraints.SOUTH;
        controlPanel.add(label, gridBagConstraints);

        proxyPortField = new JTextField();
        proxyPortField.setText(DEFAULT_PROXY_PORT);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridx = 9;
        gridBagConstraints.weightx = 1;
        gridBagConstraints.weighty = 1;
        gridBagConstraints.anchor = GridBagConstraints.SOUTH;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        controlPanel.add(proxyPortField, gridBagConstraints);

        label = new JLabel();
        label.setText("Context Path:");
        label.setToolTipText("Context path of the demo war");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridx = 1;
        controlPanel.add(label, gridBagConstraints);

        contextPathField = new JTextField();
        contextPathField.setText(DEFAULT_CONTEXT_PATH);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridx = 3;
        gridBagConstraints.weightx = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        controlPanel.add(contextPathField, gridBagConstraints);

        label = new JLabel();
        label.setText("Session Life (s):");
        label.setToolTipText("Number of seconds client should use session before invalidating or abandoning it");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridx = 6;
        controlPanel.add(label, gridBagConstraints);

        sessionLifeField = new JTextField();
        sessionLifeField.setText(String.valueOf(DEFAULT_SESSION_LIFE));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridx = 9;
        gridBagConstraints.weightx = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        controlPanel.add(sessionLifeField, gridBagConstraints);

        label = new JLabel();
        label.setText("Invalidate:");
        label.setToolTipText("Check if session should be invalidated at end of life; uncheck to abandon session");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridx = 1;
        controlPanel.add(label, gridBagConstraints);

        destroySessionField = new JCheckBox();
        destroySessionField.setSelected(true);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridx = 3;
        gridBagConstraints.weightx = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        controlPanel.add(destroySessionField, gridBagConstraints);

        sessionTimeoutLabel = new JLabel();
        sessionTimeoutLabel.setText("Session Timeout (s):");
        sessionTimeoutLabel.setToolTipText("Session maxInactiveInterval if abandoned, in seconds");
        sessionTimeoutLabel.setEnabled(destroySessionField.isSelected());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridx = 6;
        controlPanel.add(sessionTimeoutLabel, gridBagConstraints);

        sessionTimeoutField = new JTextField();
        sessionTimeoutField.setText(String.valueOf(DEFAULT_SESSION_TIMEOUT));
        sessionTimeoutField.setEnabled(destroySessionField.isSelected());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridx = 9;
        gridBagConstraints.weightx = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        controlPanel.add(sessionTimeoutField, gridBagConstraints);

        label = new JLabel();
        label.setText("Num Threads:");
        label.setToolTipText("Number of client threads to launch; max number of concurrent requests");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridx = 1;
        controlPanel.add(label, gridBagConstraints);

        numThreadsField = new JTextField();
        numThreadsField.setText(String.valueOf(DEFAULT_NUM_THREADS));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridx = 3;
        gridBagConstraints.weightx = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        controlPanel.add(numThreadsField, gridBagConstraints);

        label = new JLabel();
        label.setText("Sleep Time (ms):");
        label.setToolTipText("Number of ms each client should sleep between requests");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridx = 6;
        controlPanel.add(label, gridBagConstraints);

        sleepTimeField = new JTextField();
        sleepTimeField.setText(String.valueOf(DEFAULT_SLEEP_TIME));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridx = 9;
        gridBagConstraints.weightx = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        controlPanel.add(sleepTimeField, gridBagConstraints);

        label = new JLabel();
        label.setText("Startup Time (s):");
        label.setToolTipText("Number of seconds over which client threads should be launched");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridx = 1;
        controlPanel.add(label, gridBagConstraints);

        startupTimeField = new JTextField();
        startupTimeField.setText(String.valueOf(DEFAULT_STARTUP_TIME));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridx = 3;
        gridBagConstraints.weightx = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        controlPanel.add(startupTimeField, gridBagConstraints);

        JButton startButton = new JButton();
        startButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                start();
            }
        });
        startButton.setText("Start");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weighty = 3;
        gridBagConstraints.weightx = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = GridBagConstraints.EAST;
        controlPanel.add(startButton, gridBagConstraints);

        JButton stopButton = new JButton();
        stopButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                stop();
            }
        });
        stopButton.setText("Stop");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridx = 6;
        gridBagConstraints.weighty = 2;
        gridBagConstraints.weightx = 1;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        controlPanel.add(stopButton, gridBagConstraints);

        JPanel statusPanel = new JPanel();
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 11;
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weighty = 2;
        gridBagConstraints.weightx = 1;
        gridBagConstraints.gridwidth = 9;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        controlPanel.add(statusPanel, gridBagConstraints);

        gridBagLayout = new GridBagLayout();
        gridBagLayout.columnWidths = new int[] { 0, 0, 7 };
        gridBagLayout.rowHeights = new int[] { 0, 7, 0, 0 };
        statusPanel.setLayout(gridBagLayout);

        label = new JLabel();
        label.setText("Client Status");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.weightx = 1;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        statusPanel.add(label, gridBagConstraints);

        label = new JLabel();
        label.setText("Total clients:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridx = 1;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        statusPanel.add(label, gridBagConstraints);

        totalClientsLabel = new JLabel();
        totalClientsLabel.setText(String.valueOf("0"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridx = 3;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        statusPanel.add(totalClientsLabel, gridBagConstraints);

        label = new JLabel();
        label.setText("Live clients:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridx = 1;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        statusPanel.add(label, gridBagConstraints);

        liveClientsLabel = new JLabel();
        liveClientsLabel.setText(String.valueOf("0"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridx = 3;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        statusPanel.add(liveClientsLabel, gridBagConstraints);

        label = new JLabel();
        label.setText("Failed clients:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridx = 1;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        statusPanel.add(label, gridBagConstraints);

        failedClientsLabel = new JLabel();
        failedClientsLabel.setText(String.valueOf("0"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridx = 3;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        statusPanel.add(failedClientsLabel, gridBagConstraints);

        return controlPanel;
    }

    private JPanel createServerLoadControlPanel() {
        final JPanel loadPanel = new JPanel();
        GridBagLayout gridBagLayout = new GridBagLayout();
        gridBagLayout.columnWidths = new int[] { 7, 0, 7, 0, 7, 0, 7, 7, 0, 0, 7 };
        gridBagLayout.rowHeights = new int[] { 0, 7, 0, 7, 0, 7, 0, 7, 0, 7, 0 };
        loadPanel.setLayout(gridBagLayout);

        JLabel label = new JLabel();
        label.setText("Target Hostname:");
        label.setToolTipText("Hostname clients should request");
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weighty = 1;
        gridBagConstraints.anchor = GridBagConstraints.SOUTH;
        loadPanel.add(label, gridBagConstraints);

        targetHostNameField = new JTextField();
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridx = 3;
        gridBagConstraints.weightx = 1;
        gridBagConstraints.weighty = 1;
        gridBagConstraints.anchor = GridBagConstraints.SOUTH;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        loadPanel.add(targetHostNameField, gridBagConstraints);

        label = new JLabel();
        label.setText("Target Port:");
        label.setToolTipText("Port clients should request");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridx = 6;
        gridBagConstraints.weighty = 1;
        gridBagConstraints.anchor = GridBagConstraints.SOUTH;
        loadPanel.add(label, gridBagConstraints);

        targetPortField = new JTextField();
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridx = 9;
        gridBagConstraints.weightx = 1;
        gridBagConstraints.weighty = 1;
        gridBagConstraints.anchor = GridBagConstraints.SOUTH;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        loadPanel.add(targetPortField, gridBagConstraints);

        label = new JLabel();
        label.setText("Load Creation Action:");
        label.setToolTipText("Action to invoke on target server to simulate server load");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridx = 1;
        loadPanel.add(label, gridBagConstraints);

        EnumSet<ServerLoadServlets> es = EnumSet.allOf(ServerLoadServlets.class);
        Vector<ServerLoadServlets> v = new Vector<ServerLoadServlets>(es);
        final JComboBox<ServerLoadServlets> targetLoadServletCombo = new JComboBox<ServerLoadServlets>(v);
        targetLoadServletCombo.setRenderer(new ServerLoadServletCellRenderer());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridx = 3;
        gridBagConstraints.weightx = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        loadPanel.add(targetLoadServletCombo, gridBagConstraints);

        targetLoadServletCombo.setSelectedItem(ServerLoadServlets.CONNECTOR_THREAD_USAGE);
        selectedLoadServlet = ServerLoadServlets.CONNECTOR_THREAD_USAGE;

        ServerLoadParam param = ServerLoadServlets.CONNECTOR_THREAD_USAGE.getParams().get(0);

        targetServletParamLabel1 = new JLabel();
        targetServletParamLabel1.setText(param.getLabel() + ":");
        targetServletParamLabel1.setToolTipText(param.getDescription());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridx = 1;
        loadPanel.add(targetServletParamLabel1, gridBagConstraints);

        targetServletParamField1 = new JTextField();
        targetServletParamField1.setText(param.getValue());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridx = 3;
        gridBagConstraints.weightx = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        loadPanel.add(targetServletParamField1, gridBagConstraints);

        param = ServerLoadServlets.CONNECTOR_THREAD_USAGE.getParams().get(1);

        targetServletParamLabel2 = new JLabel();
        targetServletParamLabel2.setText(param.getLabel() + ":");
        targetServletParamLabel2.setToolTipText(param.getDescription());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridx = 1;
        loadPanel.add(targetServletParamLabel2, gridBagConstraints);

        targetServletParamField2 = new JTextField();
        targetServletParamField2.setText(param.getValue());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridx = 3;
        gridBagConstraints.weightx = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        loadPanel.add(targetServletParamField2, gridBagConstraints);

        targetLoadServletCombo.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (ItemEvent.SELECTED == e.getStateChange()) {
                    selectedLoadServlet = (ServerLoadServlets) e.getItem();
                    List<ServerLoadParam> params = selectedLoadServlet.getParams();
                    if (params.size() > 0) {
                        targetServletParamLabel1.setText(params.get(0).getLabel() + ":");
                        targetServletParamLabel1.setToolTipText(params.get(0).getDescription());
                        targetServletParamField1.setVisible(true);
                        targetServletParamField1.setText(params.get(0).getValue());
                    } else {
                        targetServletParamLabel1.setText(" ");
                        targetServletParamField1.setVisible(false);
                    }
                    if (params.size() > 1) {
                        targetServletParamLabel2.setText(params.get(1).getLabel() + ":");
                        targetServletParamLabel2.setToolTipText(params.get(1).getDescription());
                        targetServletParamField2.setVisible(true);
                        targetServletParamField2.setText(params.get(1).getValue());
                    } else {
                        targetServletParamLabel2.setText(" ");
                        targetServletParamField2.setVisible(false);
                    }
                }
            }
        });

        JButton createLoadButton = new JButton();
        createLoadButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                createLoad();
            }
        });
        createLoadButton.setText("Create Load");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weighty = 3;
        gridBagConstraints.weightx = 1;
        gridBagConstraints.gridwidth = 9;
        gridBagConstraints.anchor = GridBagConstraints.NORTH;
        loadPanel.add(createLoadButton, gridBagConstraints);

        return loadPanel;
    }

    private JPanel createRequestBalancingPanel() {
        final JPanel requestBalancingPanel = new JPanel();
        GridBagLayout gridBagLayout = new GridBagLayout();
        gridBagLayout.columnWidths = new int[] { 0 };
        gridBagLayout.rowHeights = new int[] { 0, 0 };
        requestBalancingPanel.setLayout(gridBagLayout);

        final JPanel requestChart = new ChartPanel(this.chartManager.getRequestBalancingChart(), true);
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridx = 0;
        gridBagConstraints.weightx = 1;
        gridBagConstraints.weighty = 1;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        requestBalancingPanel.add(requestChart, gridBagConstraints);

        JPanel clientStatusPanel = new JPanel();
        gridBagLayout = new GridBagLayout();
        gridBagLayout.columnWidths = new int[] { 7, 0, 7, 0, 7, 0, 7, 0, 7, 0, 7, 0, 7 };
        gridBagLayout.rowHeights = new int[] { 7, 0 };
        clientStatusPanel.setLayout(gridBagLayout);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridx = 0;
        gridBagConstraints.weightx = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        requestBalancingPanel.add(clientStatusPanel, gridBagConstraints);

        JLabel label = new JLabel();
        label.setText("Total clients:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridx = 1;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        clientStatusPanel.add(label, gridBagConstraints);

        totalClientsLabelReq = new JLabel();
        totalClientsLabelReq.setText(String.valueOf("0"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridx = 3;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        clientStatusPanel.add(totalClientsLabelReq, gridBagConstraints);

        label = new JLabel();
        label.setText("Live clients:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridx = 5;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        clientStatusPanel.add(label, gridBagConstraints);

        liveClientsLabelReq = new JLabel();
        liveClientsLabelReq.setText(String.valueOf("0"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridx = 7;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        clientStatusPanel.add(liveClientsLabelReq, gridBagConstraints);

        label = new JLabel();
        label.setText("Failed clients:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridx = 9;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        clientStatusPanel.add(label, gridBagConstraints);

        failedClientsLabelReq = new JLabel();
        failedClientsLabelReq.setText(String.valueOf("0"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridx = 11;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        clientStatusPanel.add(failedClientsLabelReq, gridBagConstraints);

        return requestBalancingPanel;
    }

    private JPanel createSessionBalancingPanel() {
        final JPanel sessionBalancingPanel = new JPanel();
        GridBagLayout gridBagLayout = new GridBagLayout();
        gridBagLayout.columnWidths = new int[] { 0 };
        gridBagLayout.rowHeights = new int[] { 0, 0 };
        sessionBalancingPanel.setLayout(gridBagLayout);

        JPanel sessionBalancingChart = new ChartPanel(this.chartManager.getSessionBalancingChart(), true);

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridx = 0;
        gridBagConstraints.weightx = 1;
        gridBagConstraints.weighty = 1;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        sessionBalancingPanel.add(sessionBalancingChart, gridBagConstraints);

        JPanel clientStatusPanel = new JPanel();
        gridBagLayout = new GridBagLayout();
        gridBagLayout.columnWidths = new int[] { 7, 0, 7, 0, 7, 0, 7, 0, 7, 0, 7, 0, 7 };
        gridBagLayout.rowHeights = new int[] { 7, 0 };
        clientStatusPanel.setLayout(gridBagLayout);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridx = 0;
        gridBagConstraints.weightx = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        sessionBalancingPanel.add(clientStatusPanel, gridBagConstraints);

        JLabel label = new JLabel();
        label.setText("Total clients:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridx = 1;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        clientStatusPanel.add(label, gridBagConstraints);

        totalClientsLabelSess = new JLabel();
        totalClientsLabelSess.setText(String.valueOf("0"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridx = 3;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        clientStatusPanel.add(totalClientsLabelSess, gridBagConstraints);

        label = new JLabel();
        label.setText("Live clients:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridx = 5;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        clientStatusPanel.add(label, gridBagConstraints);

        liveClientsLabelSess = new JLabel();
        liveClientsLabelSess.setText(String.valueOf("0"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridx = 7;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        clientStatusPanel.add(liveClientsLabelSess, gridBagConstraints);

        label = new JLabel();
        label.setText("Failed clients:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridx = 9;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        clientStatusPanel.add(label, gridBagConstraints);

        failedClientsLabelSess = new JLabel();
        failedClientsLabelSess.setText(String.valueOf("0"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridx = 11;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        clientStatusPanel.add(failedClientsLabelSess, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridx = 0;
        gridBagConstraints.weightx = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        sessionBalancingPanel.add(clientStatusPanel, gridBagConstraints);
        return sessionBalancingPanel;
    }

    private void start() {
        String sessionTimeoutText = sessionTimeoutField.getText();
        int sessionTimeout = -1;
        if (sessionTimeoutText != null && sessionTimeoutText.trim().length() > 0) {
            try {
                sessionTimeout = Integer.parseInt(sessionTimeoutText);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        boolean invalidate = destroySessionField.isSelected();

        String tmp = createBaseURL(proxyHostNameField.getText(), proxyPortField.getText()) + "record";
        URL requestURL, destroyURL;
        try {
            if (invalidate) {
                requestURL = new URL(tmp);
                destroyURL = new URL(tmp + "?destroy=true");
            } else {
                String timeoutParam = (sessionTimeout > 0) ? "?timeout=" + String.valueOf(sessionTimeout) : "";
                requestURL = new URL(tmp + timeoutParam);
                destroyURL = requestURL;
            }
        } catch (MalformedURLException e) {
            e.printStackTrace(System.err);
            return;
        }

        int num_threads = DEFAULT_NUM_THREADS;
        String numT = numThreadsField.getText();
        if (numT != null && numT.trim().length() > 0) {
            num_threads = Integer.parseInt(numT);
        }

        int session_life = DEFAULT_SESSION_LIFE;
        String sessL = sessionLifeField.getText();
        if (sessL != null && sessL.trim().length() > 0) {
            session_life = Integer.parseInt(sessL);
        }

        int sleep_time = DEFAULT_SLEEP_TIME;
        String sleepT = sleepTimeField.getText();
        if (sleepT != null && sleepT.trim().length() > 0) {
            sleep_time = Integer.parseInt(sleepT);
        }

        int startup_time = DEFAULT_STARTUP_TIME;
        String startT = startupTimeField.getText();
        if (startT != null && startT.trim().length() > 0) {
            startup_time = Integer.parseInt(startT);
        }

        // Start the components
        this.currentTask = new GUIUpdateTimerTask();
        this.chartManager.start();
        this.timer.schedule(this.currentTask, 2000, 2000);
        this.requestDriver.start(requestURL, destroyURL, num_threads, session_life, sleep_time, startup_time);
    }

    private void stop() {
        if (this.currentTask != null) {
            this.currentTask.cancel();
        }

        this.requestDriver.stop();

        // Update the client status panel
        updateStatusPanel();
    }

    private void createLoad() {
        String tmp = createBaseURL(targetHostNameField.getText(), targetPortField.getText())
                + selectedLoadServlet.getServletPath();
        List<ServerLoadParam> params = selectedLoadServlet.getParams();
        if (params.size() > 0) {
            String val = targetServletParamField1.getText();
            params.get(0).setValue(val);
            tmp += "?" + params.get(0).getName() + "=" + val;
        }
        if (params.size() > 1) {
            String val = targetServletParamField2.getText();
            params.get(1).setValue(val);
            tmp += "&" + params.get(1).getName() + "=" + val;
        }

        final URL requestURL;
        try {
            requestURL = new URL(tmp);
        } catch (MalformedURLException e) {
            e.printStackTrace(System.err);
            return;
        }

        // Send the request in another thread
        Runnable r = new Runnable() {
            private final byte[] buffer = new byte[1024];

            public void run() {
                System.out.println("Sending load generation request " + requestURL);
                InputStream input = null;
                HttpURLConnection conn = null;
                try {
                    conn = (HttpURLConnection) requestURL.openConnection(); // not yet connected
                    input = conn.getInputStream(); // NOW it is connected
                    while (input.read(buffer) > 0) {
                    }
                    input.close(); // discard data
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        ExecutorService exec = Executors.newSingleThreadExecutor();
        exec.execute(r);
        exec.shutdown();
    }

    private String createBaseURL(String hostText, String portText) {
        if (portText == null || portText.trim().length() == 0)
            portText = "80";
        portText = portText.trim();

        String contextPath = contextPathField.getText();
        if (contextPath == null)
            contextPath = "";
        contextPath = contextPath.trim();
        if (contextPath.length() > 0 && '/' == contextPath.charAt(0))
            contextPath = contextPath.length() == 1 ? "" : contextPath.substring(1);
        if (contextPath.length() > 0 && '/' == contextPath.charAt(contextPath.length() - 1))
            contextPath = contextPath.length() == 1 ? "" : contextPath.substring(0, contextPath.length() - 1);

        return "http://" + hostText + ":" + portText + "/" + contextPath + "/";
    }

    private void updateStatusPanel() {
        ClientStatus status = requestDriver.getClientStatus();
        totalClientsLabel.setText(String.valueOf(status.clientCount));
        liveClientsLabel.setText(String.valueOf(status.liveClientCount));
        totalClientsLabelReq.setText(String.valueOf(status.clientCount));
        liveClientsLabelReq.setText(String.valueOf(status.liveClientCount));
        totalClientsLabelSess.setText(String.valueOf(status.clientCount));
        liveClientsLabelSess.setText(String.valueOf(status.liveClientCount));
        int failedCount = status.clientCount - status.successfulClientCount;
        failedClientsLabel.setText(String.valueOf(failedCount));
        failedClientsLabel.setForeground(failedCount == 0 ? Color.BLACK : Color.RED);
        failedClientsLabelReq.setText(String.valueOf(failedCount));
        failedClientsLabelReq.setForeground(failedCount == 0 ? Color.BLACK : Color.RED);
        failedClientsLabelSess.setText(String.valueOf(failedCount));
        failedClientsLabelSess.setForeground(failedCount == 0 ? Color.BLACK : Color.RED);
    }

    private class GUIUpdateTimerTask extends TimerTask {
        @Override
        public void run() {
            // Update the chart
            chartManager.updateStats();

            // Update the client status panel
            updateStatusPanel();
        }

    }

    private class ServerLoadServletCellRenderer extends JLabel implements ListCellRenderer<ServerLoadServlets> {
        private static final long serialVersionUID = -8010662328204072428L;

        @Override
        public Component getListCellRendererComponent(JList<? extends ServerLoadServlets> list, ServerLoadServlets value, int index, boolean isSelected, boolean cellHasFocus) {
            this.setText(value.toString());
            this.setToolTipText(((ServerLoadServlets) value).getDescription());
            return this;
        }
    }

}
