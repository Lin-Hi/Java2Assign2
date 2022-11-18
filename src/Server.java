import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * An assignment for Sustech Java2.
 *
 * @author : Lin Yuhang
 * @SID: 12010903
 */
public class Server {
  private static ServerSocket serverSocket;
  private static Socket waitingServer;
  private static final int PORT = 8999;
  private static String waitingAccount;
  private static int waitingWinNum;
  private static int waitingLoseNum;
  private static int waitingTieNum;


  private static boolean needWaiting;

  public static void initialize() throws IOException {
    needWaiting = true;
    waitingServer = null;
    serverSocket = new ServerSocket(PORT);
  }


  public static void main(String[] args) throws IOException {
    initialize();
    System.out.println("Waiting for clients to connect...");
    while (true) {
      Socket server = serverSocket.accept();
      System.out.println("Connected: " + server.getPort());
      if (needWaiting || waitingServer == null) {
        waitingServer = server;
        needWaiting = false;
        Scanner in = new Scanner(server.getInputStream());
        waitingAccount = in.next();
        waitingWinNum = in.nextInt();
        waitingLoseNum = in.nextInt();
        waitingTieNum = in.nextInt();
        PrintWriter out = new PrintWriter(server.getOutputStream());
        out.println("WAIT");
        out.flush();
      } else {
        try {
          PrintWriter waitingOut = new PrintWriter(waitingServer.getOutputStream(), true);
          Scanner waitingIn = new Scanner(waitingServer.getInputStream());
          waitingOut.println("EXIST");
          String ans = waitingIn.next();
          Service service = new Service(waitingServer, server);
          Thread t = new Thread(service);
          t.start();
          waitingServer = null;
          needWaiting = true;
        } catch (NoSuchElementException e) {
          System.out.println("Waiting client has closed, need to wait for a new one");
          Client.writeData(waitingAccount, waitingWinNum, waitingLoseNum, waitingTieNum);
          waitingServer = server;
          needWaiting = false;
          PrintWriter out = new PrintWriter(server.getOutputStream());
          out.println("WAIT");
          out.flush();
        }
      }
    }
  }
}
