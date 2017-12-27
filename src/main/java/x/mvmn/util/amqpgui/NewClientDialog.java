package x.mvmn.util.amqpgui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import x.mvmn.util.amqpgui.util.StackTraceUtil;
import x.mvmn.util.amqpgui.util.SwingUtil;

public class NewClientDialog extends JDialog {
	private static final long serialVersionUID = 9217842265255294094L;

	protected JTextField tfHost = new JTextField("127.0.0.1");
	protected JTextField tfPort = new JTextField("5672");
	protected JTextField tfUsername = new JTextField("guest");
	protected JPasswordField tfPassword = new JPasswordField("guest");
	protected JTextField tfVirtualHost = new JTextField("/");

	protected JButton btnCreate = new JButton("Create client");
	protected JButton btnCancel = new JButton("Cancel");

	public static interface NewClientDialogCallback {
		public void onSuccess(String serverHost, String serverPort, String virtualHost, String username, char[] password) throws Exception;
	}

	public NewClientDialog(JFrame parentFrame, final NewClientDialogCallback callback) {
		super(parentFrame, true);
		this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		JPanel mainPanel = new JPanel(new GridLayout(5, 1));
		this.getContentPane().setLayout(new BorderLayout());
		this.getContentPane().add(mainPanel, BorderLayout.CENTER);

		tfHost.setBorder(BorderFactory.createTitledBorder("Server Host"));
		mainPanel.add(tfHost);
		tfPort.setBorder(BorderFactory.createTitledBorder("Server Port"));
		mainPanel.add(tfPort);
		tfVirtualHost.setBorder(BorderFactory.createTitledBorder("Virtual Host"));
		mainPanel.add(tfVirtualHost);
		tfUsername.setBorder(BorderFactory.createTitledBorder("Username"));
		mainPanel.add(tfUsername);
		tfPassword.setBorder(BorderFactory.createTitledBorder("Password"));
		mainPanel.add(tfPassword);

		JPanel buttonsPanel = new JPanel(new GridLayout(1, 2));

		buttonsPanel.add(btnCancel);
		buttonsPanel.add(btnCreate);

		this.getContentPane().add(buttonsPanel, BorderLayout.SOUTH);

		btnCreate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actEvent) {
				try {
					callback.onSuccess(tfHost.getText(), tfPort.getText(), tfVirtualHost.getText(), tfUsername.getText(), tfPassword.getPassword());
				} catch (Exception ex) {
					ex.printStackTrace();
					JOptionPane.showMessageDialog(NewClientDialog.this, StackTraceUtil.toString(ex),
							"Error occurred: " + ex.getClass().getName() + " " + ex.getMessage(), JOptionPane.ERROR_MESSAGE);
				} finally {
					NewClientDialog.this.setVisible(false);
					NewClientDialog.this.dispose();
				}
			}
		});

		btnCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				NewClientDialog.this.setVisible(false);
				NewClientDialog.this.dispose();
			}
		});

		this.setMinimumSize(new Dimension(600, 100));
		this.pack();
		SwingUtil.moveToScreenCenter(this);
	}
}
