import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.URL;
import java.util.NoSuchElementException;
import java.util.ResourceBundle;
import java.util.Scanner;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

/**
 * An assignment for Sustech Java2.
 *
 * @author : Lin Yuhang
 * @SID: 12010903
 */
public class Client extends Application implements Initializable {
  private static final int EMPTY = 0;
  private static final int BOUND = 90;
  private static final int OFFSET = 15;

  private static final int PORT = 8999;

  private static final int CIRCLE = 1;
  private static final int LINE = -1;

  private static String accountNumber;
  private static int winNum;
  private static int loseNum;
  private static int tieNum;

  private static Socket client;
  private static PrintWriter out;
  private static Scanner in;
  private static int myColor;
  private static int oppoColor;
  private static boolean canGo;
  private static boolean clicked;
  private static boolean isWaiting;
  private static boolean hasExited;
  private static int x;
  private static int y;
  @FXML
  private Pane baseSquare;
  @FXML
  private Rectangle gamePanel;


  private static final int[][] chessBoard = new int[3][3];
  private static final boolean[][] flag = new boolean[3][3];


  private boolean refreshBoard(int x, int y, int color) {
    if (chessBoard[x][y] == EMPTY) {
      chessBoard[x][y] = color;
      drawChess();
      return true;
    }
    return false;
  }

  private void drawChess() {
    for (int i = 0; i < chessBoard.length; i++) {
      for (int j = 0; j < chessBoard[0].length; j++) {
        if (flag[i][j]) {
          // This square has been drawing, ignore.
          continue;
        }
        if (chessBoard[i][j] == CIRCLE) {
          drawCircle(i, j);
        } else if (chessBoard[i][j] == LINE) {
          drawLine(i, j);
        } else if (chessBoard[i][j] == EMPTY) {
          // do nothing
        } else {
          System.err.println("Invalid value!");
        }
      }
    }
  }

  private void drawCircle(int i, int j) {
    Circle circle = new Circle();
    baseSquare.getChildren().add(circle);
    circle.setCenterX(i * BOUND + BOUND / 2.0 + OFFSET);
    circle.setCenterY(j * BOUND + BOUND / 2.0 + OFFSET);
    circle.setRadius(BOUND / 2.0 - OFFSET / 2.0);
    circle.setStroke(Color.RED);
    circle.setFill(Color.TRANSPARENT);
    flag[i][j] = true;
  }

  private void drawLine(int i, int j) {
    Line lineA = new Line();
    Line lineB = new Line();
    baseSquare.getChildren().add(lineA);
    baseSquare.getChildren().add(lineB);
    lineA.setStartX(i * BOUND + OFFSET * 1.5);
    lineA.setStartY(j * BOUND + OFFSET * 1.5);
    lineA.setEndX((i + 1) * BOUND + OFFSET * 0.5);
    lineA.setEndY((j + 1) * BOUND + OFFSET * 0.5);
    lineA.setStroke(Color.BLUE);

    lineB.setStartX((i + 1) * BOUND + OFFSET * 0.5);
    lineB.setStartY(j * BOUND + OFFSET * 1.5);
    lineB.setEndX(i * BOUND + OFFSET * 1.5);
    lineB.setEndY((j + 1) * BOUND + OFFSET * 0.5);
    lineB.setStroke(Color.BLUE);
    flag[i][j] = true;
  }

  @Override
  public void start(Stage primaryStage) {
    try {
      FXMLLoader fxmlLoader = new FXMLLoader();

      fxmlLoader.setLocation(getClass().getClassLoader().getResource("mainUI.fxml"));
      Pane root = fxmlLoader.load();
      primaryStage.setTitle("Tic Tac Toe");
      primaryStage.setScene(new Scene(root));
      primaryStage.setResizable(false);
      primaryStage.setOnCloseRequest(e -> {
        try {
          System.out.println("Client closed");
          if (!hasExited) {
            writeData(accountNumber, winNum, loseNum, tieNum);
          }
          hasExited = true;
          System.exit(0);
        } catch (IOException ex) {
          throw new RuntimeException(ex);
        }
      });
      primaryStage.show();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public void initialize(URL location, ResourceBundle resources) {
    gamePanel.setOnMouseClicked(event -> {
      int x = (int) (event.getX() / BOUND);
      int y = (int) (event.getY() / BOUND);
      if (!isWaiting && canGo) {
        Client.x = x;
        Client.y = y;
        clicked = true;
      }
    });
    x = -1;
    y = -1;
    isWaiting = false;
    canGo = true;
    clicked = false;
    hasExited = false;
    try {
      client = new Socket("localhost", PORT);
      out = new PrintWriter(client.getOutputStream(), true);
      in = new Scanner(client.getInputStream());
      out.printf("%s %d %d %d\n", accountNumber, winNum, loseNum, tieNum);
      Communicate communicate = new Communicate();
      Thread t = new Thread(communicate);
      t.start();
    } catch (ConnectException e) {
      System.out.println("Server has not started, client will close automatically");
      try {
        writeData(accountNumber, winNum, loseNum, tieNum);
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
      hasExited = true;
      System.exit(0);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  class Communicate implements Runnable {
    @Override
    public void run() {
      try {
        while (!hasExited) {
          String s = in.next();
          handle(s);
        }
      } catch (NoSuchElementException e) {
        try {
          if (!hasExited) {
            System.out.println("Server has closed");
            writeData(accountNumber, winNum, loseNum, tieNum);
            out.println("CLOSE");
            client.close();
            hasExited = true;
            System.exit(0);
          }
        } catch (IOException ex) {
          throw new RuntimeException(ex);
        }
      } catch (InterruptedException | IOException e) {
        throw new RuntimeException(e);
      }
    }

    public void handle(String s) throws InterruptedException, IOException {
      switch (s) {
        case "EXIST":
          out.println("ON");
          break;
        case "EXIT":
          System.out.println("Your opponent disconnect, you win");
          hasExited = true;
          writeData(accountNumber, winNum, loseNum, tieNum);
          return;
        case "WIN":
          System.out.println("You win.");
          winNum++;
          hasExited = true;
          writeData(accountNumber, winNum, loseNum, tieNum);
          return;
        case "LOSE":
          System.out.println("You lose.");
          loseNum++;
          hasExited = true;
          writeData(accountNumber, winNum, loseNum, tieNum);
          return;
        case "TIE":
          System.out.println("Tie.");
          tieNum++;
          hasExited = true;
          writeData(accountNumber, winNum, loseNum, tieNum);
          return;
        case "GO":
          canGo = true;
          clicked = false;
          while (!clicked) {
            Thread.sleep(100);
          }
          Platform.runLater(() -> refreshBoard(x, y, myColor));
          canGo = false;
          out.printf("MOVE %d %d\n", x, y);
          break;
        case "OPPO":
          int x = in.nextInt();
          int y = in.nextInt();
          Platform.runLater(() -> refreshBoard(x, y, oppoColor));
          canGo = true;
          break;
        case "COLOR":
          System.out.println("Game start");
          myColor = in.nextInt();
          oppoColor = -myColor;
          canGo = myColor == 1;
          clicked = false;
          isWaiting = false;
          break;
        case "WAIT":
          System.out.println("Waiting for another player...");
          isWaiting = true;
          canGo = false;
          break;
        default:
      }
    }
  }

  public static void login() throws IOException {
    Scanner in = new Scanner(System.in);
    String accountNumberInput;
    System.out.print("Please enter your account number: ");
    accountNumberInput = in.next();
    int i = checkAccount(accountNumberInput);
    while (i == 3 || i == 4) {
      System.out.print(i == 3 ?
          "This account has login, please input another account: " :
          "Account number can only consists of digital numbers, please input legal number: ");
      accountNumberInput = in.next();
      i = checkAccount(accountNumberInput);
    }
    System.out.println("Login successfully");
  }

  // 1: login in successfully
  // 2: new account
  // 3: already login, need login again
  // 4: illegal account number
  public synchronized static int checkAccount(String accountNumberInput) throws IOException {
    for (int i = 0; i < accountNumberInput.length(); i++) {
      if (!Character.isDigit(accountNumberInput.charAt(i))) {
        return 4;
      }
    }
    File file = new File("src\\account.txt");
    if (!file.exists()) {
      file.createNewFile();
    }
    BufferedReader reader = new BufferedReader(new FileReader(file));
    String line;
    while ((line = reader.readLine()) != null) {
      String[] arr = line.split("\\|");
      if (arr[0].equals(accountNumberInput)) {
        accountNumber = arr[0];
        winNum = Integer.parseInt(arr[1]);
        loseNum = Integer.parseInt(arr[2]);
        tieNum = Integer.parseInt(arr[3]);
        updateLoginAccount(accountNumber);
        return 1;
      } else if (arr[0].charAt(0) == '*' && arr[0].substring(1).equals(accountNumberInput)) {
        return 3;
      }
    }
    System.out.println("User doesn't exist, a new account is created.");
    accountNumber = accountNumberInput;
    winNum = 0;
    loseNum = 0;
    tieNum = 0;
    updateLoginAccount(accountNumber);
    return 2;
  }

  public synchronized static void updateLoginAccount(String accountNumber) throws IOException {
    BufferedReader reader = new BufferedReader(new FileReader("src\\account.txt"));
    String line;
    StringBuilder data = new StringBuilder();
    while ((line = reader.readLine()) != null) {
      if (!line.split("\\|")[0].equals(accountNumber)) {
        data.append(line).append("\n");
      }
    }
    reader.close();
    data.append(String.format("*%s|%d|%d|%d\n", accountNumber, winNum, loseNum, tieNum));
    BufferedWriter writer = new BufferedWriter(new FileWriter("src\\account.txt"));
    writer.write(data.toString());
    writer.flush();
    writer.close();
  }

  public synchronized static void writeData(String accountNumber, int winNum, int loseNum, int tieNum) throws IOException {
    BufferedReader reader = new BufferedReader(new FileReader("src\\account.txt"));
    String line;
    StringBuilder data = new StringBuilder();
    while ((line = reader.readLine()) != null) {
      if (!line.split("\\|")[0].equals("*" + accountNumber)) {
        data.append(line).append("\n");
      }
    }
    reader.close();
    data.append(String.format("%s|%d|%d|%d\n", accountNumber, winNum, loseNum, tieNum));
    BufferedWriter writer = new BufferedWriter(new FileWriter("src\\account.txt"));
    writer.write(data.toString());
    writer.flush();
    writer.close();
  }

  public static void main(String[] args) throws IOException {
    login();
    launch(args);
    writeData(accountNumber, winNum, loseNum, tieNum);
    client.close();
  }
}
