package client;

import java.awt.*;

import javax.swing.*;

import server.Server;

import java.awt.event.*;
import java.io.*;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;

import javax.sound.sampled.*;

public class Client extends JFrame implements ActionListener {

	public static void main(String[] args) throws LineUnavailableException, IOException {
		Scanner scanner = new Scanner(System.in);
		System.out.println("�������˿ںţ�");
		int port = scanner.nextInt();
		scanner.close();
		Client client = new Client(port);
		client.openServer();
		client.listen();
	}

	private static final long serialVersionUID = 1L;
	private JPanel container;
	private JPanel jp1, jp2, jp3;
	private JLabel jl1 = null;
	private JButton callBtn, anserBtn, hangUpBtn;
	private TextField socketInput;

	private Socket socket = null;
	private DatagramSocket localSocket;
	private int tcpport;
	private int udpPort;
	private int counterpartPort;
	private Server server = null;
	private boolean isbusy;
	private ClientSendVoice clientSendVoice = null;
	private ClientPlay clientPlay = null;

	/*
	 * ���캯�� port��ʾ��ǰ�û������ڽ��������server�˿ں�; ���캯����ͬʱ�������û�����
	 */
	public Client(int port) throws LineUnavailableException, IOException {
		this.tcpport = port;
		this.udpPort = port + 1;
		this.isbusy = false;
		localSocket = new DatagramSocket(udpPort);

		// �����ʼ��
		
		jp1 = new JPanel();
		jp2 = new JPanel();
		jp3 = new JPanel();
		container = new JPanel();
		socketInput = new TextField("�˿ں�",5);
		container.setSize(500, 400);
		container.add(jp1, BorderLayout.NORTH);
		container.add(jp2, BorderLayout.CENTER);
		container.add(jp3, BorderLayout.SOUTH);

		// ��������
		Font myFont = new Font("������κ", Font.BOLD, 30);
		jl1 = new JLabel("�벦��");
		jl1.setFont(myFont);
		jp1.setLayout(null);
		jl1.setLocation(20,20);
		socketInput.setBounds(150,120,300,300);
		jp1.add(jl1);
		jp1.add(socketInput);

		callBtn = new JButton("����");
		callBtn.addActionListener(this);
		callBtn.setActionCommand("callBtn");

		anserBtn = new JButton("����");
		anserBtn.addActionListener(this);
		anserBtn.setActionCommand("anserBtn");

		hangUpBtn = new JButton("�Ҷ�");
		hangUpBtn.addActionListener(this);
		hangUpBtn.setActionCommand("hangUpBtn");

		
		jp3.setLayout(null);
		jp3.setLayout(new GridLayout(1, 3, 10, 10));
		jp3.add(callBtn);
		jp3.add(anserBtn);
		jp3.add(hangUpBtn);
		// ���ð�ť������
		callBtn.setEnabled(true);
		anserBtn.setEnabled(false);
		hangUpBtn.setEnabled(false);
		// ���ô��ڵ�����

		this.setTitle("ͨ��");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setLocationRelativeTo(null);
		this.setVisible(true);
	}

	/*
	 * ��Ӧ�������¼�
	 */
	public void actionPerformed(ActionEvent e) {

		if (e.getActionCommand().equals("callBtn")) {
			dial();
			callBtn.setEnabled(false);
			anserBtn.setEnabled(false);
			hangUpBtn.setEnabled(true);
			jl1.setText("���ں���......");

		} else if (e.getActionCommand().equals("anserBtn")) {
			if (AnserCall()) {
				callBtn.setEnabled(false);
				anserBtn.setEnabled(false);
				hangUpBtn.setEnabled(true);
				jl1.setText("ͨ����...");
			}
		} else if (e.getActionCommand().equals("hangUpBtn")) {
			isbusy = false;
			sendMessage("hang");
			HangUp();
		}
	}

	/*
	 * 
	 */
	public boolean openServer() {
		try {
			server = new Server(tcpport);
			new Thread(server).start();
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	/*
	 * �����Ƿ������� ����������Socket��NULL
	 */

	public void listen() {
		while (true) {
			if (HasPhoneCall())
				break;
		}
	}

	public Boolean HasPhoneCall() {
		if (!server.isConnected()) {
			System.out.print("");
			server.isConnected();
			return false;
		}
		isbusy = true;
		callBtn.setEnabled(false);
		anserBtn.setEnabled(true);
		hangUpBtn.setEnabled(true);
		jl1.setText("����" + server.getCounterpartAddess() + "�ĵ绰");
		this.socket = server.getSocket();
		return true;
	}

	/*
	 * �����绰 �¿��߳̽������������Լ��������� �رմ����߳�
	 * 
	 */
	public boolean AnserCall() {
		if (!HasPhoneCall())
			return false;
		sendMessage("talk");
		communicate();
		WaitRespond();
		callBtn.setEnabled(false);
		anserBtn.setEnabled(false);
		hangUpBtn.setEnabled(true);
		return true;
	}

	/*
	 * �����������Լ���������
	 */
	public boolean communicate() {
		try {
			counterpartPort = 3334;
			clientSendVoice = new ClientSendVoice(localSocket, server.getCounterpartAddess(), counterpartPort);
			new Thread(clientSendVoice).start();
			clientPlay = new ClientPlay(localSocket);
			new Thread(clientPlay).start();
			System.out.println("call2");
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	/*
	 * �绰���У�����
	 */
	public boolean dial() {
		if (isbusy)
			return false;
		isbusy = true;
		while (socket == null) {
			try {
				if (socket != null) {
					socket = new Socket("localhost", 3333);
					sendMessage("call");
				}
			} catch (Exception e) {
				socket = null;
			}
		}
		// sendMessage(String.valueOf(localSocket.getLocalPort()));
		WaitRespond();
		return true;
	}

	public void WaitRespond() {
		new Thread(() -> {
			while (isbusy) {
				try {
					byte[] responsebyte = new byte[4];
					DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
					dataInputStream.read(responsebyte);
					String response = new String(responsebyte);
					System.out.println(response);
					if (response.equals("talk")) {
						callBtn.setEnabled(false);
						anserBtn.setEnabled(false);
						hangUpBtn.setEnabled(true);
						jl1.setText("ͨ����...");
						communicate();
					}
					if (response.equals("hang")) {
						sendMessage("hang");
						HangUp();
						System.out.println("receive hang");
						isbusy = false;
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}).start();
	}

	public void sendMessage(String message) {
		new Thread(() -> {
			DataOutputStream dataOutputStream;
			try {
				dataOutputStream = new DataOutputStream(socket.getOutputStream());
				dataOutputStream.write(message.getBytes());
				System.out.println(tcpport + ":" + message);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}).start();
	}

	public void HangUp() {
		new Thread(() -> {
			jl1.setText("�ѹҶ�");
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			callBtn.setEnabled(true);
			anserBtn.setEnabled(false);
			hangUpBtn.setEnabled(false);
			jl1.setText("�벦��");
		}).start();
		if (clientSendVoice != null)
			clientPlay.setHangUp(true);
		if (clientSendVoice != null)
			clientSendVoice.setHangUp(true);
		System.out.println("hang up");
//		try {
//			socket.close();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}

}
