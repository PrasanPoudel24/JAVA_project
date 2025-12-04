package com.rentalapp.servlets;

import java.io.*;
import java.sql.*;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/postdetail") // match your fetch URL
public class PostDetailsServlet extends HttpServlet {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/rental_project";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String postIdStr = request.getParameter("id");
        if (postIdStr == null || postIdStr.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\":\"Post ID is missing\"}");
            return;
        }

        int postId;
        try {
            postId = Integer.parseInt(postIdStr);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\":\"Invalid Post ID\"}");
            return;
        }

        Connection conn = null;
        PreparedStatement psPost = null;
        PreparedStatement psImage = null;
        PreparedStatement psBooking = null;
        ResultSet rsPost = null;
        ResultSet rsImage = null;
        ResultSet rsBooking = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);

            // 1️⃣ Fetch post + owner details
            String sqlPost = "SELECT p.id, p.title, p.category, p.price, p.location, p.description, p.created_at, " +
                             "u.id AS ownerId, u.phone AS ownerPhone " +
                             "FROM posts p JOIN users u ON p.owner_id = u.id WHERE p.id = ?";
            psPost = conn.prepareStatement(sqlPost);
            psPost.setInt(1, postId);
            rsPost = psPost.executeQuery();

            if (!rsPost.next()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("{\"error\":\"Post not found\"}");
                return;
            }

            int ownerId = rsPost.getInt("ownerId");
            String ownerPhone = rsPost.getString("ownerPhone");
            String title = rsPost.getString("title");
            String category = rsPost.getString("category");
            double price = rsPost.getDouble("price");
            String location = rsPost.getString("location");
            String description = rsPost.getString("description");
            Timestamp createdAt = rsPost.getTimestamp("created_at");

            // 2️⃣ Fetch main image (first image)
            String sqlImage = "SELECT image_url FROM images WHERE post_id = ? LIMIT 1";
            psImage = conn.prepareStatement(sqlImage);
            psImage.setInt(1, postId);
            rsImage = psImage.executeQuery();
            String image = rsImage.next() ? rsImage.getString("image_url") : "";

            // 3️⃣ Check if the post is already booked (dummy logic: you can implement your booking table)
            boolean isBooked = false;
            /*
            String sqlBooking = "SELECT COUNT(*) FROM bookings WHERE post_id = ? AND status='approved'";
            psBooking = conn.prepareStatement(sqlBooking);
            psBooking.setInt(1, postId);
            rsBooking = psBooking.executeQuery();
            if(rsBooking.next()) {
                isBooked = rsBooking.getInt(1) > 0;
            }
            */

            // 4️⃣ Build JSON manually
            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"id\":").append(postId).append(",");
            json.append("\"title\":\"").append(escapeJson(title)).append("\",");
            json.append("\"category\":\"").append(escapeJson(category)).append("\",");
            json.append("\"price\":").append(price).append(",");
            json.append("\"location\":\"").append(escapeJson(location)).append("\",");
            json.append("\"description\":\"").append(escapeJson(description)).append("\",");
            json.append("\"postedDate\":\"").append(createdAt.toString()).append("\",");
            json.append("\"ownerPhone\":\"").append(escapeJson(ownerPhone)).append("\",");
            json.append("\"ownerId\":").append(ownerId).append(",");
            json.append("\"isBooked\":").append(isBooked).append(",");
            json.append("\"image\":\"").append(escapeJson(image)).append("\"");
            json.append("}");

            response.getWriter().write(json.toString());

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        } finally {
            try { if (rsPost != null) rsPost.close(); } catch (Exception ignored) {}
            try { if (rsImage != null) rsImage.close(); } catch (Exception ignored) {}
            try { if (psPost != null) psPost.close(); } catch (Exception ignored) {}
            try { if (psImage != null) psImage.close(); } catch (Exception ignored) {}
            try { if (conn != null) conn.close(); } catch (Exception ignored) {}
        }
    }

    // Escape JSON special characters
    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
