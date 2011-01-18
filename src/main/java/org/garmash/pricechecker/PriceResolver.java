package org.garmash.pricechecker;

import org.apache.log4j.Logger;

import java.util.*;
import java.util.regex.Pattern;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.io.*;

import gnu.io.*;

/**
 * User: linx
 * Date: 28.09.2009
 * Time: 18:37:11
 */
public class PriceResolver {
    private static final Logger log = Logger.getLogger(PriceResolver.class);
    private Properties props;
    private InfoPanel infoPanel;
    private StringBuffer buffer = new StringBuffer();

    private Timer controlTimer = new Timer();
    private TimerTask resetTask = new ResetBufferTask();
    private TimerTask idleTask = new IdleTask();
    private Socket tcpSocket;


    private void pushSymbol(Character symbol) {
        if (symbol.equals('\n')) {
            triggerResetTimer();
        }
        else if (Character.isDigit(symbol)) {
            buffer.append(symbol);
        }
    }

    private final class ResetBufferTask extends TimerTask {
        public void run() {
            String barcode = buffer.toString();
            log.info("Checking barcode string");
            if (props.getProperty("trim-leading-zeroes").equals("true") && barcode.length() > 0) {
                char[] chars = barcode.toCharArray();
                int i = 0;
                while (chars[i] == '0')
                    i++;
                if (i > 0) {
                    barcode = barcode.substring(i, barcode.length());
                }
            }
            if (checkBarcode(barcode)) {
                log.debug("Barcode string correct");
                infoPanel.showMessage(props.getProperty("barcode").replace("$barcode", barcode));
                try {
                    Thread.sleep(Integer.valueOf(props.getProperty("barcode-show-delay")));
                }
                catch (InterruptedException e) {
                }
                log.info("Getting data...");
                infoPanel.showMessage(props.getProperty("getting-data"));
                HashMap<String, String> data = getNamePriceData(barcode, Integer.valueOf(props.getProperty("retry")));
                if (data != null)
                    if (data.get("price").equals("-1") && data.get("name").isEmpty()) {
                        log.warn("Price not found for barcode \"" + barcode + "\"");
                        infoPanel.showMessage(props.getProperty("price-not-found"));
                    }
                    else
                        infoPanel.showMessage(props.getProperty("price").replace("$name", data.get("name")).replace("$price", data.get("price")));
                else
                    infoPanel.showMessage(props.getProperty("check-error"));
            }
            else {
                log.warn("Barcode string incorrect: \"" + barcode + "\"");
                infoPanel.showMessage(props.getProperty("incorrect-barcode"));
            }
            buffer = new StringBuffer();
            triggerIdleTimer();
        }
    }


    private HashMap<String, String> getNamePriceData(String barcode, int retry) {
        if (retry <= 0)
            return null;
        try {
            if (tcpSocket == null) {
                log.debug("Connecting to " + props.getProperty("host") + " at port " + props.getProperty("port") + " try " + (Integer.valueOf(props.getProperty("retry")) - retry + 1));
                tcpSocket = new Socket(props.getProperty("host"), Integer.valueOf(props.getProperty("port")));
                tcpSocket.setKeepAlive(true);
                tcpSocket.setSoTimeout(1000);
                log.debug("Successfully connected to " + props.getProperty("host") + " at port " + props.getProperty("port"));
            }
        }
        catch (SocketException e) {
            log.error(e);
            tcpSocket = null;
            return getNamePriceData(barcode, retry - 1);
        }
        catch (UnknownHostException e) {
            log.error(e);
            tcpSocket = null;
            return getNamePriceData(barcode, retry - 1);
        }
        catch (IOException e) {
            log.error(e);
            tcpSocket = null;
            return getNamePriceData(barcode, retry - 1);
        }
        try {
            OutputStream out = tcpSocket.getOutputStream();
            byte pre[] = {0x20, 0x20, 0x20, 0x20, 0x20, 0x09};
            byte post[] = {0x0D, 0x0A, 0x00};
            byte[] bytes = new byte[pre.length + barcode.length() + post.length];
            for (int i = 0; i < bytes.length; i++) {
                    if (i >= 0 && i <= 5)
                        bytes[i] = pre[i];
                    if (i > 5 && i <= 5 + barcode.length())
                        bytes[i] = barcode.getBytes()[i - 5 - 1];
                    if (i > 5 + barcode.length())
                        bytes[i] = post[i - 5  - barcode.length() - 1];
            }
            log.debug("Sending \"" + new String(pre) + barcode + new String(post)+ "\"");
            out.write(bytes);
            out.flush();

            int sym = -1;
            bytes = new byte[1024];
            int len = 0;
            try {
                while ((sym = tcpSocket.getInputStream().read()) != -1) {
                    bytes[len++] = (byte) sym;
                }
            }
            catch (SocketException e) {
                if (e.getMessage().equals("Connection reset"))
                    log.debug("Normal connection reset");
            }
            String line = new String(bytes, props.getProperty("server-encoding"));
            log.debug("Received line\"" + line + "\"");
            Pattern ptr = Pattern.compile(props.getProperty("error-detection-regex"));
            HashMap<String, String> data = new HashMap<String, String>();
            if (ptr.matcher(line).matches()) {
                data.put("price", "-1");
                data.put("name", "");
            }
            else {
                line = line.trim();
                int pos = line.indexOf(" ", line.indexOf(" ") + 1);
                data.put("price", line.substring(0, pos).trim());
                data.put("name", line.substring(pos, line.length()).trim());
            }
            tcpSocket.close();
            tcpSocket = null;
            return data;
        }
        catch (IOException e) {
            log.error(e);
            tcpSocket = null;
            return getNamePriceData(barcode, retry - 1);
        }
    }

    private final class IdleTask extends TimerTask {
        public void run() {
            infoPanel.showMessage(props.getProperty("welcome"));
        }
    }

    private boolean checkBarcode(String barcode) {
        Pattern ptr = Pattern.compile(props.getProperty("barcode-regex"));
        return ptr.matcher(barcode).matches();
    }

    private void triggerResetTimer() {
        idleTask.cancel();
        resetTask.cancel();
        resetTask = new ResetBufferTask();
        controlTimer.schedule(resetTask, Integer.valueOf(props.getProperty("input-delay")));
    }

    private void triggerIdleTimer() {
        resetTask.cancel();
        idleTask.cancel();
        idleTask = new IdleTask();
        controlTimer.schedule(idleTask, Integer.valueOf(props.getProperty("idle-delay")));
    }


    public PriceResolver(final Properties props, final InfoPanel infoPanel) {
        this.props = props;
        this.infoPanel = infoPanel;
        infoPanel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                pushSymbol(e.getKeyChar());
            }
        });

        try {
            CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(props.getProperty("serial-port"));
            if (portIdentifier.isCurrentlyOwned()) {
                log.error("Port is currently in use");
            }
            else {
                CommPort commPort = portIdentifier.open(this.getClass().getName(), 2000);

                if (commPort instanceof SerialPort) {
                    SerialPort serialPort = (SerialPort) commPort;
                    serialPort.setSerialPortParams(Integer.parseInt(props.getProperty("serial-speed")), Integer.parseInt(props.getProperty("serial-data-bits")), Integer.parseInt(props.getProperty("serial-stop-bits")), Integer.parseInt(props.getProperty("serial-parity")));

                    InputStream in = serialPort.getInputStream();
                    serialPort.addEventListener(new SerialReader(in));
                    serialPort.notifyOnDataAvailable(true);

                }
                else {
                    log.error("Only serial ports are handled");
                }
            }
        }
        catch (NoSuchPortException e) {
            log.error(e);
        }
        catch (UnsupportedCommOperationException e) {
            log.error(e);
        }
        catch (IOException e) {
            log.error(e);
        }
        catch (PortInUseException e) {
            log.error(e);
        }
        catch (TooManyListenersException e) {
            log.error(e);
        }
    }

    private class SerialReader implements SerialPortEventListener {
        private InputStream in;

        public SerialReader(InputStream in) {
            this.in = in;
        }

        public void serialEvent(SerialPortEvent arg0) {
            try {
                while (in.available() > 0) {
                    pushSymbol((char) in.read());
                }
            }
            catch (IOException e) {
                log.error(e);
            }
            finally {
                pushSymbol('\n');
            }
        }
    }
}