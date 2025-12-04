package com.rentalapp.servlets;

import java.io.*;
import java.sql.*;
import java.util.*;
import jakarta.servlet.*;
import jakarta.servlet.annotation.*;
import jakarta.servlet.http.*;

@WebServlet("/createPostServlet")
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024 * 2,  // 2MB
    maxFileSize = 1024 * 1024 * 10,       // 10MB per file
    maxRequestSize = 1024 * 1024 * 50     // 50MB total
)
public class CreatePostServlet extends HttpServlet {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/rental_project";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();

        int ownerId = 1; // TODO: replace with session-based user id later

        String title = request.getParameter("title");
        String category = request.getParameter("category");
        String price = request.getParameter("price");
        String location = request.getParameter("location");
        String description = request.getParameter("description");

        // Folder for saving images (inside Tomcat project folder)
        String uploadPath = getServletContext().getRealPath("") + File.separator + "uploads";
        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) {
            uploadDir.mkdir();
        }

        Connection conn = null;
        PreparedStatement psPost = null;
        PreparedStatement psImage = null;
        ResultSet rs = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);

            // 1️⃣ Insert post into `posts` table
            String sqlPost = "INSERT INTO posts (owner_id, title, category, price, location, description) VALUES (?, ?, ?, ?, ?, ?)";
            psPost = conn.prepareStatement(sqlPost, Statement.RETURN_GENERATED_KEYS);
            psPost.setInt(1, ownerId);
            psPost.setString(2, title);
            psPost.setString(3, category);
            psPost.setString(4, price);
            psPost.setString(5, location);
            psPost.setString(6, description);
            psPost.executeUpdate();

            // 2️⃣ Get generated post ID
            rs = psPost.getGeneratedKeys();
            int postId = 0;
            if (rs.next()) {
                postId = rs.getInt(1);
            }

            // 3️⃣ Prepare image insert query
            String sqlImage = "INSERT INTO images (post_id, image_url) VALUES (?, ?)";
            psImage = conn.prepareStatement(sqlImage);

            // 4️⃣ Handle uploaded files
            Collection<Part> fileParts = request.getParts();
            for (Part part : fileParts) {
                if (part.getName().equals("postImage") && part.getSize() > 0) {
                    String fileName = System.currentTimeMillis() + "_" + part.getSubmittedFileName();
                    String filePath = uploadPath + File.separator + fileName;

                    // Save file to folder
                    part.write(filePath);

                    // Save path to database
                    psImage.setInt(1, postId);
                    psImage.setString(2, "uploads/" + fileName);
                    psImage.executeUpdate(); // ✅ Make sure this line is called
                }
            }

            out.println("<h3 style='color:green;'>Post and images uploaded successfully!</h3>");

        } catch (Exception e) {
            e.printStackTrace(out);
            out.println("<h3 style='color:red;'>Error: " + e.getMessage() + "</h3>");
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception ignored) {}
            try { if (psPost != null) psPost.close(); } catch (Exception ignored) {}
            try { if (psImage != null) psImage.close(); } catch (Exception ignored) {}
            try { if (conn != null) conn.close(); } catch (Exception ignored) {}
        }
    }
}
