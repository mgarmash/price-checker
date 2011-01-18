package org.garmash.pricechecker;

import org.apache.log4j.Logger;

import java.util.Properties;
import java.io.*;

/**
 * User: linx
 * Date: 28.09.2009
 * Time: 17:16:47
 */
public class Starter {
    static{
        String value = System.getProperty("log4j.configuration");
        if (value == null || value.isEmpty())
            System.setProperty("log4j.configuration", "file:log4j.properties");
    }
    private static final Logger log = Logger.getLogger(Starter.class);
    public static void main(String[] args) {

        Properties props = new Properties();
        try {
            log.info("Loading properties from price-checker.xml");
            props.loadFromXML(new FileInputStream("price-checker.xml"));
        }
        catch (IOException e) {
            log.error(e);
            System.exit(1);
        }
        InfoPanel ip = new InfoPanel(props);
        new PriceResolver(props, ip);
    }
}
