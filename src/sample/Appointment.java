package sample;

/**
 * Contains appointment fields and methods
 *
 * @author Long Tran
 */
public class Appointment {
    private int appointmentID;
    private String title;
    private String description;
    private String location;
    private String contact;
    private String type;
    //private Timestamp start;
    private String start;
    //private Timestamp end;
    private String end;
    private int customerID;
    private int userID;

    public Appointment() {
        appointmentID = 0;
        title = null;
        description = null;
        location = null;
        contact = null;
        type = null;
        start = null;
        end = null;
        customerID = 0;
        userID = 0;
    }
    public Appointment (int appointmentID, String title, String description, String location, String contact, String type, String start, String end, int customerID, int userID){
        this.appointmentID = appointmentID;
        this.title = title;
        this.description = description;
        this.location = location;
        this.contact = contact;
        this.type = type;
        this.start = start;
        this.end = end;
        this.customerID = customerID;
        this.userID = userID;
    }

    /**
     * The method to get the appointment ID
     * @return The appointment ID
     */
    public int getAppointmentID() {
        return appointmentID;
    }

    /**
     * The method to get the appointment title
     * @return The appointment title
     */
    public String getTitle() {
        return title;
    }

    /**
     * The method to get the appointment description
     * @return The appointment description
     */
    public String getDescription() {
        return description;
    }

    /**
     * The method to get the appointment location
     * @return The appointment location
     */
    public String getLocation() {
        return location;
    }

    /**
     * The method to get the appointment contact's name
     * @return The appointment contact's name
     */
    public String getContact() {
        return contact;
    }

    /**
     * The method to get the appointment type
     * @return the appointment type
     */
    public String getType() {
        return type;
    }

    /**
     * The method to get appointment start date and time
     * @return the appointment start date and time
     */
    public String getStart() { return start; }

    /**
     * The method to get appointment end date and time
     * @return the appointment end date and time
     */
    public String getEnd() { return end; }

    /**
     * The method to get the customer's ID
     * @return the customer's ID
     */
    public int getCustomerID() { return customerID; }

    /**
     * The method to get the user's ID
     * @return the user's ID
     */
    public int getUserID() { return userID; }

    /**
     * The method to set the appointment's ID
     * @param appointmentID the appointment ID
     */
    public void setAppointmentID(int appointmentID) { this.appointmentID = appointmentID; }

    /**
     * The method to set the appointment's title
     * @param title the appointment title
     */
    public void setTitle(String title) { this.title = title; }

    /**
     * The method to set the appointment's description
     * @param description the appointment description
     */
    public void setDescription(String description) { this.description = description; }

    /**
     * The method to set the appointment's location
     * @param location the appointmen location
     */
    public void setLocation(String location) { this.location = location; }

    /**
     * The method to set the appointment's contact name
     * @param contact the appointment's contact name
     */
    public void setContact(String contact) { this.contact = contact; }

    /**
     * The method to set the type of appointment
     * @param type the type of appointment
     */
    public void setType(String type) { this.type = type; }

    /**
     * The method to set the appointment's starting date and time
     * @param start the appointment starting date and time
     */
    public void setStart(String start) { this.start = start; }

    /**
     * The method to set the appointment's ending date and time
     * @param end the appointment ending date and time
     */
    public void setEnd(String end) { this.end = end; }

    /**
     * The method to set the customer's ID
     * @param customerID the customer ID
     */
    public void setCustomerID(int customerID) { this.customerID = customerID; }

    /**
     * The method to set the user's ID
     * @param userID the user ID
     */
    public void setUserID(int userID) { this.userID = userID; }
}
