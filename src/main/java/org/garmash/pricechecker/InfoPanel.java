package org.garmash.pricechecker;

import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.event.KeyListener;
import java.util.Properties;

/**
 * User: linx
 * Date: 28.09.2009
 * Time: 22:44:41
 */
public class InfoPanel {
    private static final Logger log = Logger.getLogger(InfoPanel.class);
    private Properties props;
    private JLabel lbl;
    private JFrame frame;

    public void showMessage(final String message) {
        lbl.setText(message);
    }

    public void addKeyListener(KeyListener keyListener) {
        frame.addKeyListener(keyListener);
    }

    public InfoPanel(Properties props) {
        this.props = props;
        init();
    }

    private void init() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gs = ge.getDefaultScreenDevice();
        if (gs.isFullScreenSupported()) {
            // Full-screen mode is supported
        }
        else {
            // Full-screen mode will be simulated
        }
        frame = new JFrame(gs.getDefaultConfiguration());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Container contentPane = frame.getContentPane();
        lbl = new JLabel(props.getProperty("welcome"), JLabel.CENTER);
        contentPane.add(lbl, BorderLayout.CENTER);
        gs.setFullScreenWindow(frame);
        frame.setCursor(frame.getToolkit().createCustomCursor(
                new BufferedImage(3, 3, BufferedImage.TYPE_INT_ARGB),
                new Point(0, 0), "null"));
        frame.validate();
        frame.setVisible(true);
    }
}
