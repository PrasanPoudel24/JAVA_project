package com.rentalapp.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/RegisterServlet")
public class RegisterServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    // Database connection info
    private static final String URL = "jdbc:mysql://localhost:3306/rental_project";
    private static final String USER = "root";     
    private static final String PASSWORD = "";     

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        // Get form data
        String name = request.getParameter("name");
        String email = request.getParameter("email");
        String phone = request.getParameter("phone");
        String password = request.getParameter("password");
        String role = request.getParameter("role");
        String province = request.getParameter("province");
        String district = request.getParameter("district");
        String municipal = request.getParameter("municipal");
        int ward_num = Integer.parseInt(request.getParameter("ward_num"));

        Connection conn = null;
        PreparedStatement ps = null;

        try {
            // Load MySQL JDBC Driver
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Establish connection
            conn = DriverManager.getConnection(URL, USER, PASSWORD);

            // Insert query
            String sql = "INSERT INTO users (name, email, phone, password, role, province, district, municipal, ward_num) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            ps = conn.prepareStatement(sql);

            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, phone);
            ps.setString(4, password);
            ps.setString(5, role);
            ps.setString(6, province);
            ps.setString(7, district);
            ps.setString(8, municipal);
            ps.setInt(9, ward_num);

            int result = ps.executeUpdate();

            if (result > 0) {
                out.println("<script>alert('Registration successful!'); window.location='login.html';</script>");
            } else {
                out.println("<script>alert('Registration failed. Try again.'); window.location='register.html';</script>");
            }

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            out.println("<h3>Error: MySQL Driver not found!</h3>");
        } catch (SQLException e) {
            e.printStackTrace();
            out.println("<h3>Error: Database issue occurred!</h3>");
        } finally {
            try {
                if (ps != null) ps.close();
                if (conn != null) conn.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }
}





