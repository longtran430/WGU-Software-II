package helper;
import sample.*;
import java.sql.*;
import java.time.*;

/**
 * Contains queries for manipulating information in database.
 *
 * @author Long Tran
 */
public abstract class Query {
    /**
     * The method for adding an appointment to the database.
     * @param appointment the appointment to be added
     * @return the number of rows affected
     * @throws SQLException if SQL query is invalid
     */
    public static int addAppointment (Appointment appointment) throws SQLException {
        String query = "INSERT INTO APPOINTMENTS " +
                "(Title, Description, Location, Type, Start, End, Create_Date, Created_By, Last_Update, Last_Updated_By, Customer_ID, User_ID, Contact_ID)" +
                " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
        PreparedStatement ps = JDBC.connection.prepareStatement(query);
        //ps.setInt(1, appointment.getAppointmentID());
        ps.setString(1, appointment.getTitle());
        ps.setString(2, appointment.getDescription());
        ps.setString(3, appointment.getLocation());
        ps.setString(4, appointment.getType());
        ps.setTimestamp(5, Timestamp.valueOf(appointment.getStart()));
        ps.setTimestamp(6, Timestamp.valueOf(appointment.getEnd()));
        ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
        ps.setString(8, "application");
        ps.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
        ps.setString(10, "application");
        ps.setInt(11, appointment.getCustomerID());
        ps.setInt(12, appointment.getUserID());
        ps.setInt(13, Main.contact(appointment.getContact()));
        int rowsUpdated = ps.executeUpdate();
        return rowsUpdated;
    }

    /**
     * The method for updating an appointment in the database.
     * @param appointment the appointment information to be saved
     * @return the number of rows affected
     * @throws SQLException if SQL query is invalid
     */
    public static int updateAppointment (Appointment appointment) throws SQLException {
        String query = "UPDATE APPOINTMENTS " +
                "SET Title = ?, Description = ?, Location = ?, Type = ?, Start = ?, End = ?, Customer_ID = ?, User_ID = ?, Contact_ID = ?" +
                " WHERE Appointment_ID = ?";
        PreparedStatement ps = JDBC.connection.prepareStatement(query);
        //ps.setInt(1, appointment.getAppointmentID());
        ps.setString(1, appointment.getTitle());
        ps.setString(2, appointment.getDescription());
        ps.setString(3, appointment.getLocation());
        ps.setString(4, appointment.getType());
        //Parameter index out of range (5 > number of parameters, which is 4).
        //2022-04-15 12:00 PM
        ps.setTimestamp(5, Timestamp.valueOf(appointment.getStart())); //2022-04-15 12:00:00.0
        //ps.setTimestamp(5, Timestamp.valueOf("2022-04-15 12:00:00.0")); // database 14:00:00
        ps.setTimestamp(6, Timestamp.valueOf(appointment.getEnd()));
        ps.setInt(7, appointment.getCustomerID());
        ps.setInt(8, appointment.getUserID());
        ps.setInt(9, Main.contact(appointment.getContact()));
        ps.setInt(10, appointment.getAppointmentID());
        int rowsUpdated = ps.executeUpdate();
        return rowsUpdated;
    }

    /**
     * The method for deleting an appointment from the database.
     * @param appointmentID the ID of the appointment to be deleted
     * @return the number of rows affected
     * @throws SQLException if SQL query is invalid
     */
    public static int deleteAppointment (int appointmentID) throws SQLException {
        String query = "DELETE FROM appointments WHERE Appointment_ID = ?";
        PreparedStatement ps = JDBC.connection.prepareStatement(query);
        ps.setInt(1, appointmentID);
        int rowsUpdated = ps.executeUpdate();
        return rowsUpdated;
    }

    /**
     * The method for adding a customer to the database.
     * @param customer the customer to be added
     * @return the number of rows affected
     * @throws SQLException if SQL query is invalid
     */
    public static int addCustomer (Customer customer) throws SQLException {
        String query = "INSERT INTO CUSTOMERS " +
                "(Customer_Name, Address, Postal_Code, Phone, Create_Date, Created_By, Last_Update, Last_Updated_By, Division_ID)" +
                " VALUES (?,?,?,?,?,?,?,?,?)";
        //Cannot add or update a child row: a foreign key constraint fails (`client_schedule`.`customers`, CONSTRAINT `fk_division_id` FOREIGN KEY (`Division_ID`)
        // REFERENCES `first_level_divisions` (`Division_ID`) ON DELETE CASCADE ON UPDATE CASCADE)
        PreparedStatement ps = JDBC.connection.prepareStatement(query);
        ps.setString(1, customer.getCustomerName());
        ps.setString(2, customer.getAddress());
        ps.setString(3, customer.getPostalCode());
        ps.setString(4, customer.getPhone());
        ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
        ps.setString(6, "application");
        ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
        ps.setString(8, "application");
        ps.setInt(9, customer.getDivisionID());
        int rowsUpdated = ps.executeUpdate();
        return rowsUpdated;
    }

    /**
     * The method for updating customer information in the database.
     * @param customer the customer information to be saved
     * @return the number of rows affected
     * @throws SQLException if SQL query is invalid
     */
    public static int updateCustomer (Customer customer) throws SQLException {
        String query = "UPDATE CUSTOMERS " +
                "SET Customer_Name = ?, Address = ?, Postal_Code = ?, Phone = ?, Division_ID = ?" +
                " WHERE Customer_ID = ?";
        PreparedStatement ps = JDBC.connection.prepareStatement(query);
        ps.setString(1, customer.getCustomerName());
        ps.setString(2, customer.getAddress());
        ps.setString(3, customer.getPostalCode());
        ps.setString(4, customer.getPhone());
        ps.setInt(5, customer.getDivisionID());
        ps.setInt(6, customer.getCustomerID());
        int rowsUpdated = ps.executeUpdate();
        return rowsUpdated;
    }

    /**
     * The method for deleting customer from the database.
     * @param customerID the ID of the customer to be deleted
     * @return the number of rows affected
     * @throws SQLException if SQL query is invalid
     */
    public static int deleteCustomer (int customerID) throws SQLException {
        String query = "DELETE FROM CUSTOMERS WHERE Customer_ID = ?";
        PreparedStatement ps = JDBC.connection.prepareStatement(query);
        ps.setInt(1, customerID);
        int rowsUpdated = ps.executeUpdate();
        return rowsUpdated;
    }
}
