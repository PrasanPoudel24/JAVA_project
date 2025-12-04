package com.rentalapp.servlets;

import java.io.*;
import java.sql.*;
import jakarta.servlet.*;
import jakarta.servlet.annotation.*;
import jakarta.servlet.http.*;

@WebServlet("/getSavedPosts")
public class GetSavedPostsServlet extends HttpServlet {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/rental_project";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("[]");
            return;
        }
        int userId = (int) session.getAttribute("userId");

        // Query saved posts for this user
        String sql =
            "SELECT p.id, p.title, p.price, p.location, p.category, " +
            "       (SELECT image_url FROM images WHERE post_id = p.id LIMIT 1) AS image_url, " +
            "       u.name AS owner_name " +
            "FROM saved_posts sp " +
            "JOIN posts p ON sp.post_id = p.id " +
            "JOIN users u ON p.owner_id = u.id " +
            "WHERE sp.user_id = ? " +
            "ORDER BY sp.id DESC";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            // driver not found
            e.printStackTrace();
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {

                StringBuilder json = new StringBuilder();
                json.append("[");

                boolean first = true;
                while (rs.next()) {
                    if (!first) json.append(",");
                    first = false;

                    String image = rs.getString("image_url");
                    if (image == null || image.trim().isEmpty()) {
                        image = "images/default.jpg"; // fallback; frontend expects '/RentalApp/uploads/${post.image}'
                    }

                    // priceType – your DB doesn't have a field so default to "/month"
                    String priceType = "/month";

                    json.append("{")
                        .append("\"id\":").append(rs.getInt("id")).append(",")
                        .append("\"title\":\"").append(escapeJson(rs.getString("title"))).append("\",")
                        .append("\"price\":").append(rs.getDouble("price")).append(",")
                        .append("\"priceType\":\"").append(escapeJson(priceType)).append("\",")
                        .append("\"location\":\"").append(escapeJson(rs.getString("location"))).append("\",")
                        .append("\"category\":\"").append(escapeJson(rs.getString("category"))).append("\",")
                        .append("\"ownerName\":\"").append(escapeJson(rs.getString("owner_name"))).append("\",")
                        .append("\"image\":\"").append(escapeJson(image)).append("\"")
                        .append("}");
                }

                json.append("]");
                out.print(json.toString());
            }
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("[]");
        }
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");
    }
}