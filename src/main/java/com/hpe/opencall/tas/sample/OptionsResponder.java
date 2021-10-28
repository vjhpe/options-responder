package com.hpe.opencall.tas.sample;

import java.util.Properties;

import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.ListeningPoint;
import javax.sip.PeerUnavailableException;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipFactory;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.SipStack;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.header.AllowHeader;
import javax.sip.header.CSeqHeader;
import javax.sip.header.HeaderFactory;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OptionsResponder implements SipListener {

	private static Logger _log = LoggerFactory.getLogger(OptionsResponder.class);

	private static String LISTENING_POINT_ADDRESS = "LISTENING_POINT_ADDRESS";
	private static String LISTENING_POINT_PORT = "LISTENING_POINT_PORT";
	private static String LISTENING_POINT_TRANSPORT = "LISTENING_POINT_TRANSPORT";

	private static String DEFAULT_LISTENING_POINT_ADDRESS = "0.0.0.0";
	private static String DEFAULT_LISTENING_POINT_PORT = "5060";
	private static String DEFAULT_LISTENING_POINT_TRANSPORT = "UDP";

	private final String address;
	private final int port;
	private final String transport;

	private SipStack stack = null;

	public OptionsResponder(String address, int port, String transport) {
		this.address = address;
		this.port = port;
		this.transport = transport;
	}

	void start() throws Exception {

		createStack();

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			System.out.println("->shutdownHook()");
			try {
				stack.stop();
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.out.println("<-shutdownHook()");
		}));

	}

	void createStack() throws Exception {

		javax.sip.SipFactory.getInstance().setPathName("gov.nist");
		SipFactory sipFactory = javax.sip.SipFactory.getInstance();
		sipFactory.createMessageFactory();
		sipFactory.createHeaderFactory();
		sipFactory.createAddressFactory();

		Properties props = new Properties();
		props.setProperty("javax.sip.STACK_NAME", "options-responder");

		stack = sipFactory.createSipStack(props);
		_log.info("SIP Stack created - " + stack);
		ListeningPoint listeningPoint = stack.createListeningPoint(address, port, transport);
		SipProvider provider = stack.createSipProvider(listeningPoint);
		_log.info("SIP provider created - " + provider);
		provider.addSipListener(this);
	}

	public static void main(String[] args) {

		String listeningPointAddress = System.getenv(LISTENING_POINT_ADDRESS);
		if (null == listeningPointAddress) {
			listeningPointAddress = DEFAULT_LISTENING_POINT_ADDRESS;
		}
		String listeningPointPortAsStr = System.getenv(LISTENING_POINT_PORT);
		if (null == listeningPointPortAsStr) {
			listeningPointPortAsStr = DEFAULT_LISTENING_POINT_PORT;
		}
		int listeningPointPort;
		try {
			listeningPointPort = Integer.parseUnsignedInt(listeningPointPortAsStr);
		} catch (NumberFormatException e) {
			System.err.println("Failed to parse port = " + e.getMessage());
			return;
		}
		String listeningPointTransport = System.getenv(LISTENING_POINT_TRANSPORT);
		if (null == listeningPointTransport) {
			listeningPointTransport = DEFAULT_LISTENING_POINT_TRANSPORT;
		}

		OptionsResponder optionsResponder = new OptionsResponder(listeningPointAddress, listeningPointPort,
				listeningPointTransport);
		try {
			optionsResponder.createStack();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

	}

	@Override
	public void processDialogTerminated(DialogTerminatedEvent arg0) {
		_log.info("Received dialog terminated event - " + arg0.toString());
	}

	@Override
	public void processIOException(IOExceptionEvent arg0) {
		_log.info("Received I/O error event - " + arg0.toString());
	}

	@Override
	public void processRequest(RequestEvent ev) {

		javax.sip.message.Request req = ev.getRequest();
		String method = req.getMethod();
		if (_log.isDebugEnabled())
			_log.debug("Recieved incoming message:\n" + req);
		ServerTransaction tx = ev.getServerTransaction();

		try {
			if (tx == null) {
				if (_log.isDebugEnabled())
					_log.debug("Transaction is null, creating new server transaction...");
				tx = ((javax.sip.SipProvider) ev.getSource()).getNewServerTransaction(ev.getRequest());
				if (_log.isDebugEnabled())
					_log.debug("New server transaction created");
			} else {
				if (_log.isDebugEnabled())
					_log.debug("Server transaction exists");
			}
		} catch (Exception ex) {
			_log.error("cannot create/find txn for incoming request: " + ex);
			return;
		}

		MessageFactory messageFactory;
		HeaderFactory headerFactory;
		try {
			messageFactory = SipFactory.getInstance().createMessageFactory();
			headerFactory = SipFactory.getInstance().createHeaderFactory();
		} catch (PeerUnavailableException e) {
			_log.error("Failed to get SIP message factory - " + e.getMessage(), e);
			return;
		}

		if (method.equalsIgnoreCase("OPTIONS")) {
			// Respond with 200 OK.
			try {
				Response okResponse = messageFactory.createResponse(Response.OK, req);
				AllowHeader allowHeader = headerFactory.createAllowHeader(Request.OPTIONS);
				okResponse.addHeader(allowHeader);
				tx.sendResponse(okResponse);
				_log.info("Responded incoming OPTIONS with 200.");
			} catch (Exception ex) {
				_log.error("Failed to send reject response - " + ex.getMessage(), ex);
			}

		} else {
			if (method.equalsIgnoreCase(Request.ACK)) {
				// NOP.
				return;
			}
			// Reject all other methods.
			try {
				Response rejectResponse = messageFactory.createResponse(Response.METHOD_NOT_ALLOWED, req);
				AllowHeader allowHeader = headerFactory.createAllowHeader(Request.OPTIONS);
				rejectResponse.addHeader(allowHeader);
				tx.sendResponse(rejectResponse);
			} catch (Exception ex) {
				_log.error("Failed to send reject response - " + ex.getMessage(), ex);
			}
		}

	}

	@Override
	public void processResponse(ResponseEvent ev) {
		Response resp = ev.getResponse();
		int code = resp.getStatusCode();
		CSeqHeader cseq = (CSeqHeader) resp.getHeader(CSeqHeader.NAME);
		if (_log.isDebugEnabled())
			_log.debug("Response Code = " + code + ", method = " + cseq.getMethod());
		if (_log.isDebugEnabled())
			_log.debug("Reason = " + resp.getReasonPhrase());

		return;
	}

	@Override
	public void processTimeout(TimeoutEvent arg0) {
		_log.info("Received timeout event - " + arg0.toString());
		System.err.println("Received timeout event - " + arg0.toString());

	}

	@Override
	public void processTransactionTerminated(TransactionTerminatedEvent arg0) {
		_log.info("Received transaction terminated event - " + arg0.toString());
	}
}
