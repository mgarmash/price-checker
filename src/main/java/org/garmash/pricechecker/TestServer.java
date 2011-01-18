package org.garmash.pricechecker;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;
import java.util.Random;
import java.text.DecimalFormat;

/**
 * User: linx
 * Date: 29.09.2009
 * Time: 14:07:21
 */
public class TestServer {
    public static void main(String args[]) throws IOException {
        while (true) {
            Socket skt = null;
            ServerSocket srvr = null;
            PrintWriter out = null;
            BufferedReader in = null;
            try {
                srvr = new ServerSocket(1234);
                skt = srvr.accept();
                System.out.print("Client connected!\n");

                out = new PrintWriter(skt.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(skt.getInputStream()));

                String barcode = in.readLine();
                System.out.println("barcode = " + barcode);
                out.println("Name @ " + barcode);
                Random r = new Random();
                DecimalFormat df = new DecimalFormat("####.##");
                out.println(df.format(r.nextInt(100000) * 0.01));
                System.out.print("Client disconnected!\n");
                skt.close();
                srvr.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                in.close();
                out.close();
                skt.close();
                srvr.close();
            }
        }
    }
}
