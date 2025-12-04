package com.rentalapp.servlets;

import java.io.*;
import java.sql.*;
import jakarta.servlet.*;
import jakarta.servlet.annotation.*;
import jakarta.servlet.http.*;

@WebServlet("/requests")
public class RequestsServlet extends HttpServlet {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/rental_project";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        System.out.println("\n=== RequestsServlet (GET) ===");
        
        // Check session
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            out.write("{\"error\":\"Please login first\"}");
            return;
        }
        
        int userId = (int) session.getAttribute("userId");
        String userRole = (String) session.getAttribute("role");
        String userName = (String) session.getAttribute("name");
        
        System.out.println("User - ID: " + userId + ", Name: " + userName + ", Role: " + userRole);
        
        String action = request.getParameter("action");
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            
            if ("count".equals(action)) {
                // Get request counts
                getRequestCounts(userId, userRole, conn, out);
            } else {
                // Get requests list (default)
                getRequestsList(userId, userRole, conn, out);
            }
            
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            out.write("{\"error\":\"Server error: " + e.getMessage() + "\"}");
        }
    }
    
    private void getRequestsList(int userId, String userRole, Connection conn, PrintWriter out) 
            throws SQLException {
        
        StringBuilder json = new StringBuilder("[");
        String sql;
        PreparedStatement ps;
        
        if ("owner".equals(userRole)) {
            // Owner: see requests received for their properties
            sql = "SELECT rr.id, rr.post_id, rr.renter_id, rr.owner_id, rr.message, " +
                  "rr.status, rr.created_at, " +
                  "p.title AS post_title, p.category, p.price, p.location, " +
                  "u.name AS renter_name, u.email AS renter_email, u.phone AS renter_phone " +
                  "FROM rent_requests rr " +
                  "JOIN posts p ON rr.post_id = p.id " +
                  "JOIN users u ON rr.renter_id = u.id " +
                  "WHERE rr.owner_id = ? " +
                  "ORDER BY rr.created_at DESC";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);
        } else {
            // Renter: see requests they sent
            sql = "SELECT rr.id, rr.post_id, rr.renter_id, rr.owner_id, rr.message, " +
                  "rr.status, rr.created_at, " +
                  "p.title AS post_title, p.category, p.price, p.location, " +
                  "u.name AS owner_name, u.email AS owner_email, u.phone AS owner_phone " +
                  "FROM rent_requests rr " +
                  "JOIN posts p ON rr.post_id = p.id " +
                  "JOIN users u ON rr.owner_id = u.id " +
                  "WHERE rr.renter_id = ? " +
                  "ORDER BY rr.created_at DESC";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);
        }
        
        ResultSet rs = ps.executeQuery();
        boolean first = true;
        int count = 0;
        
        while (rs.next()) {
            count++;
            if (!first) json.append(",");
            first = false;
            
            json.append("{")
                .append("\"id\":").append(rs.getInt("id")).append(",")
                .append("\"post_id\":").append(rs.getInt("post_id")).append(",")
                .append("\"renter_id\":").append(rs.getInt("renter_id")).append(",")
                .append("\"owner_id\":").append(rs.getInt("owner_id")).append(",")
                .append("\"message\":\"").append(escapeJson(rs.getString("message"))).append("\",")
                .append("\"status\":\"").append(rs.getString("status")).append("\",")
                .append("\"created_at\":\"").append(rs.getTimestamp("created_at")).append("\",")
                .append("\"post_title\":\"").append(escapeJson(rs.getString("post_title"))).append("\",")
                .append("\"category\":\"").append(escapeJson(rs.getString("category"))).append("\",")
                .append("\"price\":").append(rs.getDouble("price")).append(",")
                .append("\"location\":\"").append(escapeJson(rs.getString("location"))).append("\",");
            
            if ("owner".equals(userRole)) {
                json.append("\"renter_name\":\"").append(escapeJson(rs.getString("renter_name"))).append("\",")
                    .append("\"renter_email\":\"").append(escapeJson(rs.getString("renter_email"))).append("\",")
                    .append("\"renter_phone\":\"").append(escapeJson(rs.getString("renter_phone"))).append("\"");
            } else {
                json.append("\"owner_name\":\"").append(escapeJson(rs.getString("owner_name"))).append("\",")
                    .append("\"owner_email\":\"").append(escapeJson(rs.getString("owner_email"))).append("\",")
                    .append("\"owner_phone\":\"").append(escapeJson(rs.getString("owner_phone"))).append("\"");
            }
            
            json.append("}");
        }
        
        json.append("]");
        
        System.out.println("Found " + count + " requests for user ID " + userId);
        out.write(json.toString());
    }
    
    private void getRequestCounts(int userId, String userRole, Connection conn, PrintWriter out) 
            throws SQLException {
        
        StringBuilder json = new StringBuilder("{");
        String whereClause = "owner".equals(userRole) ? "WHERE owner_id = ?" : "WHERE renter_id = ?";
        
        // Total count
        String sqlTotal = "SELECT COUNT(*) as total FROM rent_requests " + whereClause;
        PreparedStatement psTotal = conn.prepareStatement(sqlTotal);
        psTotal.setInt(1, userId);
        ResultSet rsTotal = psTotal.executeQuery();
        int total = 0;
        if (rsTotal.next()) total = rsTotal.getInt("total");
        
        // Pending count
        String sqlPending = "SELECT COUNT(*) as pending FROM rent_requests " + whereClause + " AND status = 'pending'";
        PreparedStatement psPending = conn.prepareStatement(sqlPending);
        psPending.setInt(1, userId);
        ResultSet rsPending = psPending.executeQuery();
        int pending = 0;
        if (rsPending.next()) pending = rsPending.getInt("pending");
        
        // Approved count
        String sqlApproved = "SELECT COUNT(*) as approved FROM rent_requests " + whereClause + " AND status = 'approved'";
        PreparedStatement psApproved = conn.prepareStatement(sqlApproved);
        psApproved.setInt(1, userId);
        ResultSet rsApproved = psApproved.executeQuery();
        int approved = 0;
        if (rsApproved.next()) approved = rsApproved.getInt("approved");
        
        // Rejected count
        String sqlRejected = "SELECT COUNT(*) as rejected FROM rent_requests " + whereClause + " AND status = 'rejected'";
        PreparedStatement psRejected = conn.prepareStatement(sqlRejected);
        psRejected.setInt(1, userId);
        ResultSet rsRejected = psRejected.executeQuery();
        int rejected = 0;
        if (rsRejected.next()) rejected = rsRejected.getInt("rejected");
        
        json.append("\"total\":").append(total).append(",")
            .append("\"pending\":").append(pending).append(",")
            .append("\"approved\":").append(approved).append(",")
            .append("\"rejected\":").append(rejected)
            .append("}");
        
        System.out.println("Counts for user " + userId + ": " + json.toString());
        out.write(json.toString());
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        System.out.println("\n=== RequestsServlet (POST - Update Status) ===");
        
        // Check session
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            out.write("Please login first");
            return;
        }
        
        int userId = (int) session.getAttribute("userId");
        String userRole = (String) session.getAttribute("role");
        
        // Only owners can update request status
        if (!"owner".equals(userRole)) {
            out.write("Only property owners can update request status");
            return;
        }
        
        String action = request.getParameter("action");
        String requestIdStr = request.getParameter("requestId");
        String status = request.getParameter("status");
        
        System.out.println("Update request - Action: " + action + ", Request ID: " + requestIdStr + ", Status: " + status);
        
        if (!"update".equals(action) || requestIdStr == null || status == null) {
            out.write("Invalid parameters");
            return;
        }
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            // Verify the request belongs to this owner
            String sqlVerify = "SELECT id FROM rent_requests WHERE id = ? AND owner_id = ?";
            PreparedStatement psVerify = conn.prepareStatement(sqlVerify);
            psVerify.setInt(1, Integer.parseInt(requestIdStr));
            psVerify.setInt(2, userId);
            ResultSet rsVerify = psVerify.executeQuery();
            
            if (!rsVerify.next()) {
                out.write("Request not found or unauthorized");
                return;
            }
            
            // Update the status
            String sqlUpdate = "UPDATE rent_requests SET status = ? WHERE id = ?";
            PreparedStatement psUpdate = conn.prepareStatement(sqlUpdate);
            psUpdate.setString(1, status);
            psUpdate.setInt(2, Integer.parseInt(requestIdStr));
            
            int rows = psUpdate.executeUpdate();
            
            if (rows > 0) {
                String actionText = "approved".equals(status) ? "accepted" : "rejected".equals(status) ? "rejected" : "updated";
                System.out.println("Request " + actionText + " successfully!");
                out.write("Request " + actionText + " successfully!");
            } else {
                out.write("Failed to update request");
            }
            
        } catch (Exception e) {
            System.out.println("Error updating request: " + e.getMessage());
            out.write("Error: " + e.getMessage());
        }
    }
    
    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}