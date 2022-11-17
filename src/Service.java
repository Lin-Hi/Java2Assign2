import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * An assignment for Sustech Java2.
 *
 * @author : Lin Yuhang
 * @SID: 12010903
 */
public class Service implements Runnable {
  Socket firstHand;
  Socket secondHand;
  Scanner firstIn;
  Scanner secondIn;
  PrintWriter firstOut;
  PrintWriter secondOut;
  int chessCount;

  int[][] chessBoard;

  // int[][]

  public Service(Socket firstHand, Socket secondHand) throws IOException {
    this.firstHand = firstHand;
    this.secondHand = secondHand;
    firstIn = new Scanner(firstHand.getInputStream());
    secondIn = new Scanner(secondHand.getInputStream());
    firstOut = new PrintWriter(firstHand.getOutputStream(), true);
    secondOut = new PrintWriter(secondHand.getOutputStream(), true);
    chessBoard = new int[3][3];
    chessCount = 0;
  }

  public int checkBoard() {
    if (checkWin(1)) {
      return 1;
    }
    if (checkWin(-1)) {
      return -1;
    }
    return 0;
  }

  public boolean checkWin(int i) {
    return (chessBoard[0][0] == i && chessBoard[0][1] == i && chessBoard[0][2] == i)
        || (chessBoard[1][0] == i && chessBoard[1][1] == i && chessBoard[1][2] == i)
        || (chessBoard[2][0] == i && chessBoard[2][1] == i && chessBoard[2][2] == i)
        || (chessBoard[0][0] == i && chessBoard[1][0] == i && chessBoard[2][0] == i)
        || (chessBoard[0][1] == i && chessBoard[1][1] == i && chessBoard[2][1] == i)
        || (chessBoard[0][2] == i && chessBoard[1][2] == i && chessBoard[2][2] == i)
        || (chessBoard[0][0] == i && chessBoard[1][1] == i && chessBoard[2][2] == i)
        || (chessBoard[0][2] == i && chessBoard[1][1] == i && chessBoard[2][0] == i);
  }

  @Override
  public void run() {
    firstOut.println("COLOR 1");
    secondOut.println("COLOR -1");
    while (true) {
      firstOut.println("GO");
      String firstMsg = "";
      try {
        firstMsg = firstIn.next();
      } catch (NoSuchElementException e) {
        System.out.println("Player 1 is out");
        secondOut.println("EXIT");
      }
      if (!firstMsg.equals("MOVE")) {
        System.out.println("Invalid message in run()");
        return;
      }
      int x = firstIn.nextInt();
      int y = firstIn.nextInt();
      chessCount++;
      secondOut.printf("OPPO %d %d\n", x, y);
      chessBoard[x][y] = 1;
      int curBoard = checkBoard();
      if (curBoard == 1) {
        firstOut.println("WIN");
        secondOut.println("LOSE");
        return;
      } else if (chessCount == 9) {
        firstOut.println("TIE");
        secondOut.println("TIE");
        return;
      }

      secondOut.println("GO");
      String secondMsg = "";
      try {
        secondMsg = secondIn.next();
      } catch (NoSuchElementException e) {
        firstOut.println("EXIT");
      }
      if (!secondMsg.equals("MOVE")) {
        System.out.println("Player 1 quit");
        return;
      }
      x = secondIn.nextInt();
      y = secondIn.nextInt();
      chessCount++;
      firstOut.printf("OPPO %d %d\n", x, y);
      chessBoard[x][y] = -1;
      curBoard = checkBoard();
      if (curBoard == -1) {
        firstOut.println("LOSE");
        secondOut.println("WIN");
        return;
      }

    }
  }
}