package x.mvmn.util.amqpgui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import x.mvmn.util.amqpgui.util.StackTraceUtil;
import x.mvmn.util.amqpgui.util.SwingUtil;

public class AmqpGui implements WindowListener {

	final JFrame mainWindow = new JFrame("MVMn AMQP GUI");
	final JTabbedPane tabPane = new JTabbedPane();
	final JButton btnCreateClient = new JButton("Add client");

	final List<Connection> connections = Collections.synchronizedList(new ArrayList<Connection>());

	public AmqpGui() {
		mainWindow.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		mainWindow.addWindowListener(this);
		mainWindow.getContentPane().setLayout(new BorderLayout());
		mainWindow.add(tabPane, BorderLayout.CENTER);

		mainWindow.getContentPane().add(btnCreateClient, BorderLayout.NORTH);

		btnCreateClient.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new NewClientDialog(mainWindow, new NewClientDialog.NewClientDialogCallback() {
					public void onSuccess(final String serverHost, final String serverPort, final String virtualHost, final String username,
							final char[] password) throws Exception {
						new Thread() {
							public void run() {
								try {
									createNewClientTab(serverHost, serverPort, virtualHost, username, password);
								} catch (final Exception e) {
									e.printStackTrace();
									SwingUtilities.invokeLater(new Runnable() {
										public void run() {
											ErrorMessageDialog emd = new ErrorMessageDialog(mainWindow);
											emd.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
											emd.show("Error connecting to server", e.getClass().getName() + " " + e.getMessage(), StackTraceUtil.toString(e));
										}
									});
								}
							}
						}.start();
					}
				}).setVisible(true);
			}
		});

		mainWindow.setMinimumSize(new Dimension(800, 600));
		mainWindow.pack();
		SwingUtil.moveToScreenCenter(mainWindow);
		mainWindow.setVisible(true);
	}

	public static interface CloseCallback {
		public void close(AmqpClientGui amqpClientGui);
	}

	public void createNewClientTab(String serverHost, String serverPort, String virtualHost, String username, char[] password) throws Exception {
		ConnectionFactory connectionFactory = new ConnectionFactory();
		connectionFactory.setHost(serverHost);
		connectionFactory.setPort(Integer.parseInt(serverPort.trim()));
		connectionFactory.setVirtualHost(virtualHost);
		connectionFactory.setUsername(username);
		connectionFactory.setPassword(new String(password));
		Connection connection = connectionFactory.newConnection();

		connections.add(connection);
		final int index = connections.size() - 1;
		tabPane.addTab(serverHost + ":" + serverPort + virtualHost, new AmqpClientGui(connection, new CloseCallback() {
			public void close(final AmqpClientGui amqpClientGui) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						tabPane.remove(amqpClientGui);
						connections.remove(index);
					}
				});
			}
		}));
		tabPane.setSelectedIndex(tabPane.getTabCount() - 1);
	}

	public void doCleanup() {
		for (Connection connection : connections) {
			try {
				if (connection.isOpen()) {
					connection.close(60000);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String args[]) {
		new AmqpGui();
	}

	public void windowClosing(WindowEvent e) {
		mainWindow.setVisible(false);
		this.doCleanup();
	}

	public void windowOpened(WindowEvent e) {
	}

	public void windowClosed(WindowEvent e) {
	}

	public void windowIconified(WindowEvent e) {
	}

	public void windowDeiconified(WindowEvent e) {
	}

	public void windowActivated(WindowEvent e) {
	}

	public void windowDeactivated(WindowEvent e) {
	}
}
