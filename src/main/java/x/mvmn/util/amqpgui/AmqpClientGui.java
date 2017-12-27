package x.mvmn.util.amqpgui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import x.mvmn.util.amqpgui.AmqpGui.CloseCallback;
import x.mvmn.util.amqpgui.util.StackTraceUtil;
import x.mvmn.util.amqpgui.util.SwingUtil;

public class AmqpClientGui extends JPanel {

	private static final long serialVersionUID = -1040358216365429952L;

	protected final Connection client;

	protected JTextArea txaMainLog = new JTextArea();
	protected JButton btnClearLog = new JButton("Clear log");
	protected JButton btnClearMessages = new JButton("Clear messages");

	protected JButton btnConsume = new JButton("Consume");
	protected JButton btnClose = new JButton("Close client");

	protected final JTable tblReceivedMessages;
	protected final DefaultTableModel tableModel;

	protected JTextField txExchange = new JTextField("amq.topic");
	protected JTextField txPublishTopic = new JTextField("test.topic");
	protected JTextArea txPublishText = new JTextArea("Hello");
	protected final JButton btnPublish = new JButton("Publish");
	protected final JButton btnPublishMultiple = new JButton("Publish multiple");

	protected JTextField tfQueueName = new JTextField("queue");
	protected DefaultConsumer consumer;

	protected DefaultConsumer createConsumer(final Channel channel) throws IOException {
		return new DefaultConsumer(client.createChannel()) {
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties, byte[] body) throws IOException {
				onMessageArrived(consumerTag, envelope, properties, body);
				channel.basicAck(envelope.getDeliveryTag(), false);
			}
		};
	}

	public AmqpClientGui(final Connection client, final CloseCallback closeCallback) {
		super(new BorderLayout());
		this.client = client;

		txaMainLog.setEditable(false);
		btnClearLog.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				txaMainLog.setText("");
			}
		});
		JTabbedPane topPanel = new JTabbedPane();
		this.add(topPanel, BorderLayout.NORTH);

		JTabbedPane centerTabs = new JTabbedPane();
		JPanel logPanel = new JPanel(new BorderLayout());
		logPanel.add(new JScrollPane(txaMainLog), BorderLayout.CENTER);
		logPanel.add(btnClearLog, BorderLayout.SOUTH);
		centerTabs.add("Log", logPanel);

		tableModel = new DefaultTableModel(new Object[] { "Topic", "Tag", "Body", "Properties" }, 0);
		tblReceivedMessages = new JTable(tableModel);

		JPanel tablePanel = new JPanel(new BorderLayout());
		tablePanel.add(new JScrollPane(tblReceivedMessages), BorderLayout.CENTER);
		tablePanel.add(btnClearMessages, BorderLayout.SOUTH);
		centerTabs.add("Received messages", tablePanel);

		btnClearMessages.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				tableModel.setRowCount(0);
			}
		});

		this.add(centerTabs, BorderLayout.CENTER);
		this.add(btnClose, BorderLayout.SOUTH);

		btnClose.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SwingUtil.performSafely(new SwingUtil.UnsafeOperation() {
					public void run() throws Exception {
						closeCallback.close(AmqpClientGui.this);
						try {
							if (client.isOpen()) {
								client.close(60000);
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
			}
		});

		JPanel pnlConsume = new JPanel(new BorderLayout());
		topPanel.addTab("Consume", pnlConsume);

		JPanel pnlConnectCenter = new JPanel(new GridLayout(1, 2));
		pnlConsume.add(pnlConnectCenter, BorderLayout.CENTER);
		tfQueueName.setBorder(BorderFactory.createTitledBorder("Queue name"));
		pnlConnectCenter.add(tfQueueName);

		JPanel pnlConnectSouth = new JPanel(new GridLayout(1, 2));
		pnlConsume.add(pnlConnectSouth, BorderLayout.SOUTH);

		pnlConnectSouth.add(btnConsume);

		btnConsume.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				final String queueName = tfQueueName.getText();
				SwingUtil.performSafely(new SwingUtil.UnsafeOperation() {
					public void run() throws Exception {
						logOnSwingEdt("Statring consume on queue " + queueName + "...");

						Channel channel = client.createChannel();
						logOnSwingEdt("Consume started with consumer tag: " + channel.basicConsume(queueName, createConsumer(channel)));
					}
				});
			}
		});

		JPanel pnlPublish = new JPanel(new BorderLayout());

		topPanel.addTab("Publish", pnlPublish);

		JPanel pnlPublishTop = new JPanel(new BorderLayout());
		pnlPublish.add(pnlPublishTop, BorderLayout.NORTH);
		pnlPublishTop.add(txPublishTopic, BorderLayout.CENTER);
		pnlPublish.add(new JScrollPane(txPublishText), BorderLayout.CENTER);
		JPanel pnlSouth = new JPanel(new GridLayout(1, 2));
		pnlSouth.add(btnPublish);
		pnlSouth.add(btnPublishMultiple);
		pnlPublish.add(pnlSouth, BorderLayout.SOUTH);
		btnPublish.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				btnPublish.setEnabled(false);
				SwingUtil.performSafely(new SwingUtil.UnsafeOperation() {
					public void run() throws Exception {
						final String exchange = txExchange.getText().trim();
						final String topic = txPublishTopic.getText().trim();
						byte[] payload = txPublishText.getText().getBytes("UTF-8");
						logOnSwingEdt("Publishing...");
						try {
							Channel channel = client.createChannel();
							channel.basicPublish(exchange, topic, null, payload);
							logOnSwingEdt("Message published successfully.");
						} catch (Throwable e) {
							logOnSwingEdt("Message publish failed: " + StackTraceUtil.toString(e));
						} finally {
							SwingUtilities.invokeLater(new Runnable() {
								public void run() {
									btnPublish.setEnabled(true);
								}
							});
						}
					}
				});
			}
		});
		btnPublishMultiple.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String input = JOptionPane.showInputDialog("Enter number of publishes", "10");
				int count = 0;
				if (input != null && !input.trim().isEmpty()) {
					try {
						count = Integer.parseInt(input.trim());
					} catch (NumberFormatException nfe) {
						// Ignore
					}
				}
				if (count > 0) {
					final int finalCount = count;
					SwingUtil.performSafely(new SwingUtil.UnsafeOperation() {
						public void run() throws Exception {
							final String exchange = txExchange.getText().trim();
							final String topic = txPublishTopic.getText().trim();
							String payload = txPublishText.getText();
							logOnSwingEdt("Publishing multiple...");
							try {
								Channel channel = client.createChannel();
								for (int i = 0; i < finalCount; i++) {
									String topicWithCount = topic.replace("$counter", String.valueOf(i));
									String payloadWithCount = payload.replace("$counter", String.valueOf(i));
									logOnSwingEdt("Publishing message " + (i + 1) + " of " + finalCount + "...");
									channel.basicPublish(exchange, topicWithCount, null, payloadWithCount.getBytes("UTF-8"));
									logOnSwingEdt("Message published successfully.");
								}
							} catch (Throwable e) {
								logOnSwingEdt("Message publish failed: " + StackTraceUtil.toString(e));
							} finally {
								SwingUtilities.invokeLater(new Runnable() {
									public void run() {
										btnPublish.setEnabled(true);
									}
								});
							}
						}
					});
				}
			}
		});
	}

	protected void onMessageArrived(String consumerTag, Envelope envelope, BasicProperties properties, byte[] body) {
		try {
			String payloadStr = new String(body, "UTF-8");
			AmqpClientGui.this.logOnSwingEdt(
					"[Global] Message arrived - '" + envelope.getRoutingKey() + "' (Tag " + envelope.getDeliveryTag() + "):\n" + payloadStr + "\n----");
			StringBuilder propsStr = new StringBuilder();
			properties.appendPropertyDebugStringTo(propsStr);
			tableModel.addRow(new Object[] { envelope.getRoutingKey(), String.valueOf(envelope.getDeliveryTag()), payloadStr, propsStr.toString() });
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	protected void logOnSwingEdt(final String text) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				txaMainLog.append(text + "\n");
			}
		});
	}
}
