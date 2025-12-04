package com.rentalapp.servlets;

import java.io.*;
import java.sql.*;
import java.util.regex.*;
import jakarta.servlet.*;
import jakarta.servlet.annotation.*;
import jakarta.servlet.http.*;

@WebServlet("/removeSavedPost")
public class RemoveSavedPostServlet extends HttpServlet {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/rental_project";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";

    private static final Pattern POST_ID_PATTERN = Pattern.compile("\"postId\"\\s*:\\s*(\\d+)");

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/plain;charset=UTF-8");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("You must be logged in");
            return;
        }
        int userId = (int) session.getAttribute("userId");

        // read whole body
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = request.getReader()) {
            String l;
            while ((l = br.readLine()) != null) sb.append(l);
        }

        String body = sb.toString();
        if (body == null || body.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("Invalid request body");
            return;
        }

        // extract postId using regex
        Matcher m = POST_ID_PATTERN.matcher(body);
        int postId;
        if (m.find()) {
            try {
                postId = Integer.parseInt(m.group(1));
            } catch (NumberFormatException ex) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("Invalid postId");
                return;
            }
        } else {
            // fallback: try to parse any digits (not recommended but safe)
            String digits = body.replaceAll("\\D+", "");
            if (digits.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("postId not found");
                return;
            }
            postId = Integer.parseInt(digits);
        }

        // delete from saved_posts for this user
        String sql = "DELETE FROM saved_posts WHERE user_id = ? AND post_id = ?";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            // driver missing
            e.printStackTrace();
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.setInt(2, postId);

            int rows = ps.executeUpdate();
            if (rows > 0) {
                out.print("Deleted successfully");
            } else {
                out.print("Nothing deleted");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("DB error: " + e.getMessage());
        }
    }
}