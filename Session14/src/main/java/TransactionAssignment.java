package main.java;
import java.sql.*;
import java.math.BigDecimal;
import java.util.Scanner;

public class TransactionAssignment {
    private static final String URL = "jdbc:mysql://localhost:3306/BankDB";
    private static final String USER = "root";
    private static final String PASS = "123456";
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.print("Nhập ID người gửi: ");
        String senderId = sc.nextLine();
        System.out.print("Nhập ID người nhận: ");
        String receiverId = sc.nextLine();
        System.out.print("Nhập số tiền chuyển: ");
        double amount = sc.nextDouble();
        transferMoney(senderId, receiverId, amount);
    }
    public static void transferMoney(String fromId, String toId, double amount) {
        Connection conn = null;

        try {
            conn = DriverManager.getConnection(URL, USER, PASS);
            conn.setAutoCommit(false);

            String checkSql = "SELECT Balance FROM Accounts WHERE AccountId = ?";
            try (PreparedStatement psCheck = conn.prepareStatement(checkSql)) {
                psCheck.setString(1, fromId);
                ResultSet rs = psCheck.executeQuery();

                if (rs.next()) {
                    double currentBalance = rs.getDouble("Balance");
                    if (currentBalance < amount) {
                        System.err.println("Lỗi: Tài khoản không đủ số dư!");
                        return;
                    }
                } else {
                    System.err.println("Lỗi: Tài khoản gửi không tồn tại!");
                    return;
                }
            }

            try (CallableStatement cstmt = conn.prepareCall("{call sp_UpdateBalance(?, ?)}")) {

                cstmt.setString(1, fromId);
                cstmt.setDouble(2, -amount);
                cstmt.execute();

                cstmt.setString(1, toId);
                cstmt.setDouble(2, amount);
                cstmt.execute();

                conn.commit();
                System.out.println("--- Giao dịch thành công! ---");
            }

            showFinalResults(conn, fromId, toId);

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                    System.err.println("Giao dịch thất bại. Đã Rollback dữ liệu!");
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private static void showFinalResults(Connection conn, String id1, String id2) throws SQLException {
        String query = "SELECT * FROM Accounts WHERE AccountId IN (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, id1);
            ps.setString(2, id2);
            ResultSet rs = ps.executeQuery();

            System.out.println("\nBẢNG ĐỐI SOÁT TÀI KHOẢN:");
            System.out.println("-------------------------------------------");
            System.out.printf("%-10s | %-20s | %-10s\n", "ID", "Họ Tên", "Số dư");
            while (rs.next()) {
                System.out.printf("%-10s | %-20s | %-10.2f\n",
                        rs.getString("AccountId"),
                        rs.getString("FullName"),
                        rs.getDouble("Balance"));
            }
            System.out.println("-------------------------------------------");
        }
    }
}