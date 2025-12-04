package com.rentalapp.servlets;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.WebServlet;
import java.io.*;
import java.sql.*;

@WebServlet("/savePost")
public class SavePostServlet extends HttpServlet {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/rental_project";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        response.setContentType("text/plain");
        PrintWriter out = response.getWriter();

        BufferedReader reader = request.getReader();
        String body = reader.readLine();

        int postId = Integer.parseInt(body.replaceAll("\\D+", ""));

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.write("You must be logged in to save posts.");
            return;
        }

        int userId = (int) session.getAttribute("userId");

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {

            String query = "INSERT INTO saved_posts (user_id, post_id) VALUES (?, ?)";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, userId);
            ps.setInt(2, postId);

            ps.executeUpdate();
            out.write("Post saved successfully!");

        } catch (SQLIntegrityConstraintViolationException e) {
            out.write("Post already saved.");
        } catch (SQLException e) {
            e.printStackTrace();
            out.write("Error saving post.");
        }
    }
}
