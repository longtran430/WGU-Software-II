package sample;

import helper.JDBC;
import helper.Query;

import java.io.*;
import java.sql.*;
import java.time.chrono.ChronoLocalDate;
import java.util.*;

import helper.Query;
import javafx.application.Application;
import javafx.collections.*;
import javafx.fxml.FXMLLoader;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.text.*;

import java.time.*;
import java.time.format.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;


/**
 * An appointment management system that supports adding, updating and deleting customers and appointments.
 *
 * @author Long Tran
 */
public class Main extends Application {

    /**
     * The functional interface that defines the convertTime method.
     */
    interface timeConversion {
        String convertTime(String time, String zone);
    }

    /**
     * The method that converts time from UTC to local time. The lambda expression implements the convertTime method defined by the timeConversion interface.
     */
    timeConversion convertToLocal = (dbTimeString, zone) -> {
        //8:00 to 22:00 EST business hours
        //13:00 to 3:00 UTC
        //set dbTimeString to UTC
        String dbTimeStringUTC = dbTimeString + "+0000";

        //convert dbTimestring to time format
        LocalDateTime oldLDT = LocalDateTime.parse(dbTimeStringUTC, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.sZ"));

        ZonedDateTime oldZDT = ZonedDateTime.of(oldLDT, ZoneId.of(zone)); //2020-05-28T12:00Z[UTC]
        ZonedDateTime localZDY = oldZDT.withZoneSameInstant(ZoneId.systemDefault()); //2020-05-28T08:00-04:00[America/New_York]
        //return local time
        return localZDY.format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a")); //2020-05-28 06:00 AM
    };

    /**
     * The method that converts time from local time to a target time zone. The lambda expression implements the convertTime method defined by the timeConversion interface.
     */
    static timeConversion localToZone = (localTimeString, zone) -> {
        //convert local time string to localdatetime
        LocalDateTime localLDT = LocalDateTime.parse(localTimeString, DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a")); //2020-05-28T08:00

        //convert to TimeZone
        ZonedDateTime localZDT = ZonedDateTime.of(localLDT, ZoneId.systemDefault());
        ZonedDateTime newZDT = localZDT.withZoneSameInstant(ZoneId.of(zone)); //2020-05-28T12:00Z[UTC]
        //format to date time
        return newZDT.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")); //2020-05-28 12:00:00
    };

    /**
     * The method that converts time from local to database time (UTC). The lambda expression implements the convertTime method defined by the timeConversion interface.
     */
    timeConversion formatForDatabase = (localTimeString, utcString) -> {
        LocalDateTime localLDT = LocalDateTime.parse(localTimeString, DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a")); //2020-05-28T08:00
        ZonedDateTime localZDT = ZonedDateTime.of(localLDT, ZoneId.systemDefault());
        String newLDTString = localZDT.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        Timestamp ts = Timestamp.valueOf(newLDTString);
        return String.valueOf(ts); //2020-05-28 12:00:00
    };

    /**
     * Method for converting string to LocalDateTime format.
     * @param dtString the date and time in String format
     * @return the date and time in LocalDateTime format
     */
    private static LocalDateTime convertFormat(String dtString, String format) {
        LocalDateTime ldt = LocalDateTime.now();
        try {
            ldt =  LocalDateTime.parse(dtString, DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a"));
        } catch (Exception e) {
            try {
                ldt = LocalDateTime.parse(dtString, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (Exception ex) {
                ldt = LocalDateTime.parse(dtString, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.s"));
            }
        }
        return ldt;
    }

    /**
     * Method for changing date/time format.
     * @param dtString the date and time in database format
     * @return the date and time in AM/PM format
     */
    private String convertFormat(String dtString) {
        LocalDateTime ldt;
        String s = "";
        try {
            ldt =  LocalDateTime.parse(dtString, DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a"));
        } catch (Exception e) {
            try {
                ldt = LocalDateTime.parse(dtString, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (Exception ex) {
                ldt = LocalDateTime.parse(dtString, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.s"));
            }
        }
        s = ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a"));
        return s;
    }

    /**
     * The font used for bolding labels.
     */
    private static final Font boldFont = Font.font("Times New Roman", FontWeight.BOLD, 12);

    /**
     * The text object used to alert the user about an upcoming appointment.
     */
    private static Text appointmentAlert = new Text();
    /**
     * The text object used to inform the user of a required and completed action.
     */
    private static Text message = new Text();
    /**
     * The text object used to help the user input a valid appointment date and time.
     */
    private static Text dateTimeNote = new Text("Use yyyy-mm-dd hh:mm a format (ex. 2020-01-01 01:00 PM). \nBusiness hours are 8:00 A.M. to 10:00 P.M. EST Monday through Sunday.");
    /**
     * The text object used to remind user to input valid appointment date and time.
     */
    private static Text dateTimeCheck = new Text("");
    /**
     * The strings for keeping track of recent activity in report screen.
     */
    private static String activityString1 = "", activityString2 = "", activityString3 = "", activityString4 = "", activityString5 = "";

    /**
     * The string array for the Contacts ComboBox.
     */
    private String contacts[] = {"Anika Costa", "Daniel Garcia","Li Lee"};
    /**
     * The ComboBox for selecting a contact.
     */
    private ComboBox contactBox = new ComboBox(FXCollections.observableArrayList(contacts));

    /**
     * The method that replaces contact ID with matching contact name.
     * @param contactID The ID of the contact
     * @return the contact name
     * @throws SQLException if SQL query is invalid
     */
    public static String contact(int contactID) throws SQLException {
        String contact = "";

        String query = "SELECT * FROM contacts";
        PreparedStatement ps = JDBC.connection.prepareStatement(query);
        ResultSet resultset = null;
        try {
            resultset = ps.executeQuery("SELECT * FROM contacts");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        boolean continueQuery = true;
        while(true){
            try {
                if (!resultset.next()) {
                    continueQuery = false;
                    break;
                }
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
            //for each customer
            if (continueQuery){
                //go through countries table
                try {
                    //get country if countryID matches
                    if (contactID == resultset.getInt("Contact_ID")){
                        contact = resultset.getString("Contact_Name");
                    }
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
        }
        return contact;
    }

    /**
     * The method that replaces contact name with matching contact ID.
     * @param contactName The name of the contact
     * @return the contact ID
     * @throws SQLException if SQL query
     */
    public static int contact(String contactName) throws SQLException {
        String contact = "";
        int contactID = 0;

        String query = "SELECT * FROM contacts";
        PreparedStatement ps = JDBC.connection.prepareStatement(query);
        ResultSet resultset = null;
        try {
            resultset = ps.executeQuery("SELECT * FROM contacts");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        boolean continueQuery = true;
        while(true){
            try {
                if (!resultset.next()) {
                    continueQuery = false;
                    break;
                }
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
            //for each customer
            if (continueQuery){
                //go through countries table
                try {
                    //get contact ID if contact name matches
                    if (contactName.equals(resultset.getString("Contact_Name"))){
                        contactID = resultset.getInt("Contact_ID");
                    }
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
        }
        return contactID;
    }

    /**
     * The String array for the month ComboBox.
     */
    private String monthList[] = {"January","February","March","April","May","June","July","August","September","October","November","December"};
    /**
     * The ComboBox for selecting a month filter.
     */
    private ComboBox monthBox = new ComboBox(FXCollections.observableArrayList(monthList));

    /**
     * The String array for the country ComboBox.
     */
    private String countries[] =  {"U.S.", "Canada", "U.K."};
    /**
     * The ComboBox for selecting a country.
     */
    private ComboBox countryBox = new ComboBox(FXCollections.observableArrayList(countries));
    /**
     * The String array for the US States ComboBox.
     */
    private String USStates[] = {"Alabama","Alaska","Arizona","Arkansas","California","Colorado","Connecticut","Delaware","Florida","Georgia","Hawaii","Idaho","Illinois","Indiana","Iowa","Kansas","Kentucky","Louisiana","Maine","Maryland","Massachusetts","Michigan","Minnesota","Mississippi","Missouri","Montana","Nebraska","Nevada","New Hampshire","New Jersey","New Mexico","New York","North Carolina","North Dakota","Ohio","Oklahoma","Oregon","Pennsylvania","Rhode Island","South Carolina","South Dakota","Tennessee","Texas","Utah","Vermont","Virginia","Washington","West Virginia","Wisconsin","Wyoming"};
    /**
     * The String array for the Canada provinces ComboBox.
     */
    private String canadaProvinces[] = {"Alberta","British Columbia","Manitoba","New Brunswick","Newfoundland and Labrador","Northwest Territories","Nova Scotia","Nunavut","Ontario","Prince Edward Island","Quebec","Saskatchewan","Yukon"};
    /**
     * The String array for the UK divisions ComboBox.
     */
    private String UKDivisions[] = {"England","Northern Ireland","Scotland","Wales"};
    /**
     * The ComboBox for selecting a first-level division.
     */
    private ComboBox divisionBox = new ComboBox();

    /**
     * The method for logging recent activity into the report screen.
     * @param activity The most recent action performed on the appointment screen
     */
    public void activityReport(String activity) {
        LocalDate ld = LocalDate.now();
        DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("hh:mm:ss a");
        String lt = LocalTime.now().format(formatter1);
        activityString5 = activityString4;
        activityString4 = activityString3;
        activityString3 = activityString2;
        activityString2 = activityString1;
        activityString1 = activity + " on " + ld + " at " + lt + "\n";
    }

    /**
     * The string that that alerts user of upcoming appointments.
     */
    private static String upcomingString = "There are no upcoming appointments. ";

    /**
     * The method for checking for upcoming appointments. Lambda expression adds functionality to "OK" button to close alert window.
     */
    public static void upcomingAppt() {
        //checks if there is an upcoming appointment in 15 minutes
        //set dbTimeString to UTC
        Stage upcomingStage = new Stage();
        upcomingString = "There are no upcoming appointments. ";
        boolean upcoming = false;
        for (Appointment a:appointmentList) {
            String apptTimeString = String.valueOf(a.getStart());
            int apptID = a.getAppointmentID();
            //convert dbTimestring to time format
            String pattern = "yyyy-MM-dd hh:mm a";
            DateTimeFormatter offsetFormatter = DateTimeFormatter.ofPattern(pattern);

            LocalDateTime apptDateTime = null;
            try {
                apptDateTime = LocalDateTime.parse(apptTimeString, offsetFormatter);
                LocalDateTime now = LocalDateTime.now();
                if (apptDateTime.isAfter(now) && apptDateTime.isBefore(now.plusMinutes(16))) {
                    upcomingString = "Appointment ID: " + apptID + " on " + apptTimeString.substring(0,10) + " at " +
                            apptTimeString.substring(11) + " \nwill be starting within 15 minutes.";
                    upcoming = true;
                }
            } catch (Exception e) {
                dateTimeCheck.setText("Please use proper format. ");
            }
        }
        appointmentAlert.setText(upcomingString);
        Text alertText = new Text(upcomingString);
        alertWindow(null);
        Button okButton = new Button("OK");
        okButton.setPrefSize(70, 20);
        okButton.setOnAction(event -> {
            upcomingStage.close();
        });
    }

    /**
     * The stage for the alert message window.
     */
    private static Stage alertStage = new Stage();

    /**
     * The method for alerting user of an error. Lambda expression adds functionality to "OK" button to close alert window.
     * @param alertString the alert message
     */
    public static void alertWindow(String alertString) {
        alertStage.close();
        Text upcomingText = new Text(upcomingString);
        Text alertText = new Text(alertString);
        Button okButton = new Button("OK");
        okButton.setPrefSize(70, 20);
        okButton.setOnAction(event -> {
            alertStage.close();
        });
        GridPane grid = new GridPane();
        grid.add(upcomingText, 1, 1, 5, 1);
        grid.add(alertText, 1, 2, 5, 1);
        grid.add(okButton, 5, 4, 1, 1);
        okButton.setAlignment(Pos.CENTER);
        grid.setHgap(20);
        grid.setVgap(20);
        Scene scene = new Scene(grid, 280, 200);
        alertStage.setScene(scene);
        alertStage.show();
    }

    /**
     * The method for converting division ID to division and country name.
     * @param divisionID The ID of the customer's division
     * @return The first-level division and country matching the Division ID
     * @throws SQLException if SQL query is invalid
     */
    public String[] divisionCountry(int divisionID) throws SQLException {
        String divisionCountry[] = {"division", "country"};

        String query = "SELECT * FROM first_level_divisions";
        PreparedStatement ps = JDBC.connection.prepareStatement(query);
        ResultSet resultset = null;
        try {
            resultset = ps.executeQuery("SELECT * FROM first_level_divisions");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        boolean continueQuery = true;
        while(true){
            try {
                if (!resultset.next()) {
                    continueQuery = false;
                    break;
                }
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
            //for each customer
            if (continueQuery){
                String queryComb = null;
                String division;
                int countryID;
                String country;
                //go through Division table
                try {
                    //get countryID and division if divisionID matches
                    if (divisionID == resultset.getInt("Division_ID")){
                        division = resultset.getString("Division");
                        divisionCountry[0] = division;
                        countryID = resultset.getInt("Country_ID");
                        country = country(countryID);
                        divisionCountry[1] = country;
                    }

                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
        }
        return divisionCountry;
    }

    /**
     * The method for converting division to division ID.
     * @param divisionName The name of the customer's division
     * @return The first-level division ID
     * @throws SQLException if SQL query is invalid
     */
    public int divisionNameID(String divisionName) throws SQLException {
        int divisionID = 1;

        String query = "SELECT * FROM first_level_divisions";
        PreparedStatement ps = JDBC.connection.prepareStatement(query);
        ResultSet resultset = null;
        try {
            resultset = ps.executeQuery("SELECT * FROM first_level_divisions");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        boolean continueQuery = true;
        while(true){
            try {
                if (!resultset.next()) {
                    continueQuery = false;
                    break;
                }
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
            //for each customer
            if (continueQuery){
                String queryComb = null;
                //go through Division table
                try {
                    if (divisionName.equals(resultset.getString("Division"))){
                        divisionID = resultset.getInt("Division_ID");
                    }

                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
        }
        return divisionID;
    }

    /**
     * The method for converting country ID to country name.
     * @param countryID the ID of the country
     * @return the country name
     * @throws SQLException if SQL query is invalid
     */
    public String country(int countryID) throws SQLException {
        String country = "country";

        String query = "SELECT * FROM countries";
        PreparedStatement ps = JDBC.connection.prepareStatement(query);
        ResultSet resultset = null;
        try {
            resultset = ps.executeQuery("SELECT * FROM countries");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        boolean continueQuery = true;
        while(true){
            try {
                if (!resultset.next()) {
                    continueQuery = false;
                    break;
                }
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
            //for each customer
            if (continueQuery){
                //go through countries table
                try {
                    //get country if countryID matches
                    if (countryID == resultset.getInt("Country_ID")){
                        country = resultset.getString("Country");
                    }
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
        }
        return country;
    }

    /**
     * The auto-generated appointment ID.
     */
    private static int appointmentID = 1;

    /**
     * The list of all appointments.
     */
    private static ObservableList<Appointment> appointmentList = FXCollections.observableArrayList();
    /**
     * The filtered list of appointments.
     */
    private static ObservableList<Appointment> filteredList = FXCollections.observableArrayList();
    /**
     * The table to view appointment list.
     */
    private static TableView<Appointment> appointmentTable = new TableView();

    /**
     * The method to build customer appointments table.
     */
    public void appointmentTable() {
        String columnString11 = "Appointment ID",
                columnString12 = "Title",
                columnString13 = "Description",
                columnString14 = "Location",
                columnString15 = "Contact",
                columnString16 = "Type",
                columnString17 = "Start Date and Time",
                columnString18 = "End Date and Time",
                columnString19 = "Customer ID",
                columnString20 = "User ID";

        TableColumn<Appointment, String> column11 = new TableColumn<>(columnString11);
        column11.setCellValueFactory(new PropertyValueFactory<>("appointmentID"));
        TableColumn<Appointment, String> column12 = new TableColumn<>(columnString12);
        column12.setCellValueFactory(new PropertyValueFactory<>("title"));
        TableColumn<Appointment, String> column13 = new TableColumn<>(columnString13);
        column13.setCellValueFactory(new PropertyValueFactory<>("description"));
        TableColumn<Appointment, String> column14 = new TableColumn<>(columnString14);
        column14.setCellValueFactory(new PropertyValueFactory<>("location"));
        TableColumn<Appointment, String> column15 = new TableColumn<>(columnString15);
        column15.setCellValueFactory(new PropertyValueFactory<>("contact"));
        TableColumn<Appointment, String> column16 = new TableColumn<>(columnString16);
        column16.setCellValueFactory(new PropertyValueFactory<>("type"));
        TableColumn<Appointment, String> column17 = new TableColumn<>(columnString17);
        column17.setCellValueFactory(new PropertyValueFactory<>("start"));
        TableColumn<Appointment, String> column18 = new TableColumn<>(columnString18);
        column18.setCellValueFactory(new PropertyValueFactory<>("end"));
        TableColumn<Appointment, String> column19 = new TableColumn<>(columnString19);
        column19.setCellValueFactory(new PropertyValueFactory<>("customerID"));
        TableColumn<Appointment, String> column20 = new TableColumn<>(columnString20);
        column20.setCellValueFactory(new PropertyValueFactory<>("userID"));

        appointmentTable.getColumns().add(column11);
        column11.setPrefWidth(100);
        appointmentTable.getColumns().add(column12);
        appointmentTable.getColumns().add(column13);
        appointmentTable.getColumns().add(column14);
        appointmentTable.getColumns().add(column15);
        appointmentTable.getColumns().add(column16);
        appointmentTable.getColumns().add(column17);
        column17.setPrefWidth(130);
        appointmentTable.getColumns().add(column18);
        column18.setPrefWidth(130);
        appointmentTable.getColumns().add(column19);
        appointmentTable.getColumns().add(column20);
        appointmentTable.setMaxHeight(150);
        appointmentTable.setMinWidth(900);
    }

    /**
     * The method to insert customer appointments into table from database.
     * @throws SQLException if SQL query is invalid
     */
    public void appointmentList() throws SQLException {
        //adds appointments from database to table
        appointmentList.clear();
        String query2 = "SELECT * FROM customers";
        PreparedStatement ps2 = JDBC.connection.prepareStatement(query2);
        ResultSet resultset2 = null;
        try {
            resultset2 = ps2.executeQuery("SELECT * FROM appointments");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        boolean continueQuery2 = true;
        while(true){
            try {
                if (!resultset2.next()) {
                    continueQuery2 = false;
                    break;
                }
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
            if (continueQuery2){
                Appointment appointment = new Appointment();
                try {
                    appointment.setAppointmentID(resultset2.getInt("Appointment_ID"));
                    //autogenerated ID for new appointments
                    appointmentID = appointment.getAppointmentID() + 1;
                    appointment.setTitle(resultset2.getString("Title"));
                    appointment.setDescription(resultset2.getString("Description"));
                    appointment.setLocation(resultset2.getString("Location"));
                    appointment.setContact(contact(resultset2.getInt("Contact_ID")));
                    appointment.setType(resultset2.getString("Type"));
                    appointment.setStart(resultset2.getTimestamp("Start").toString());
                    appointment.setEnd(resultset2.getTimestamp("End").toString());
                    appointment.setCustomerID(resultset2.getInt("Customer_ID"));
                    appointment.setUserID(resultset2.getInt("User_ID"));
                    appointmentList.add(appointment);
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
        }
        //timezone conversions
        for (Appointment a:appointmentList){
            String start = convertFormat(a.getStart());
            String end = convertFormat(a.getEnd());
            a.setStart(start);
            a.setEnd(end);
        }
        appointmentTable.setItems(appointmentList);
    }

    /**
     * The method for creating reports. Lambda expression used on closeButton to close window on button press action.
     */
    public void report() {
        Stage reportStage = new Stage();
        reportStage.setTitle("Appointment Reports");
        ObservableList<Appointment> scheduleList1 = FXCollections.observableArrayList();
        TableView<Appointment> scheduleTable1 = new TableView();
        ObservableList<Appointment> scheduleList2 = FXCollections.observableArrayList();
        TableView<Appointment> scheduleTable2 = new TableView();
        ObservableList<Appointment> scheduleList3 = FXCollections.observableArrayList();
        TableView<Appointment> scheduleTable3 = new TableView();

        String columnString1 = "Appointment ID",
                columnString2 = "Title",
                columnString3 = "Type",
                columnString4 = "Description",
                columnString5 = "Start Date and Time",
                columnString6 = "End Date and Time",
                columnString7 = "Customer ID";
        TableColumn<Appointment, String> column11 = new TableColumn<>(columnString1);
        column11.setCellValueFactory(new PropertyValueFactory<>("appointmentID"));
        TableColumn<Appointment, String> column12 = new TableColumn<>(columnString2);
        column12.setCellValueFactory(new PropertyValueFactory<>("title"));
        TableColumn<Appointment, String> column13 = new TableColumn<>(columnString3);
        column13.setCellValueFactory(new PropertyValueFactory<>("type"));
        TableColumn<Appointment, String> column14 = new TableColumn<>(columnString4);
        column14.setCellValueFactory(new PropertyValueFactory<>("description"));
        TableColumn<Appointment, String> column15 = new TableColumn<>(columnString5);
        column15.setCellValueFactory(new PropertyValueFactory<>("start"));
        TableColumn<Appointment, String> column16 = new TableColumn<>(columnString6);
        column16.setCellValueFactory(new PropertyValueFactory<>("end"));
        TableColumn<Appointment, String> column17 = new TableColumn<>(columnString7);
        column17.setCellValueFactory(new PropertyValueFactory<>("customerID"));

        TableColumn<Appointment, String> column21 = new TableColumn<>(columnString1);
        column21.setCellValueFactory(new PropertyValueFactory<>("appointmentID"));
        TableColumn<Appointment, String> column22 = new TableColumn<>(columnString2);
        column22.setCellValueFactory(new PropertyValueFactory<>("title"));
        TableColumn<Appointment, String> column23 = new TableColumn<>(columnString3);
        column23.setCellValueFactory(new PropertyValueFactory<>("type"));
        TableColumn<Appointment, String> column24 = new TableColumn<>(columnString4);
        column24.setCellValueFactory(new PropertyValueFactory<>("description"));
        TableColumn<Appointment, String> column25 = new TableColumn<>(columnString5);
        column25.setCellValueFactory(new PropertyValueFactory<>("start"));
        TableColumn<Appointment, String> column26 = new TableColumn<>(columnString6);
        column26.setCellValueFactory(new PropertyValueFactory<>("end"));
        TableColumn<Appointment, String> column27 = new TableColumn<>(columnString7);
        column27.setCellValueFactory(new PropertyValueFactory<>("customerID"));

        TableColumn<Appointment, String> column31 = new TableColumn<>(columnString1);
        column31.setCellValueFactory(new PropertyValueFactory<>("appointmentID"));
        TableColumn<Appointment, String> column32 = new TableColumn<>(columnString2);
        column32.setCellValueFactory(new PropertyValueFactory<>("title"));
        TableColumn<Appointment, String> column33 = new TableColumn<>(columnString3);
        column33.setCellValueFactory(new PropertyValueFactory<>("type"));
        TableColumn<Appointment, String> column34 = new TableColumn<>(columnString4);
        column34.setCellValueFactory(new PropertyValueFactory<>("description"));
        TableColumn<Appointment, String> column35 = new TableColumn<>(columnString5);
        column35.setCellValueFactory(new PropertyValueFactory<>("start"));
        TableColumn<Appointment, String> column36 = new TableColumn<>(columnString6);
        column36.setCellValueFactory(new PropertyValueFactory<>("end"));
        TableColumn<Appointment, String> column37 = new TableColumn<>(columnString7);
        column37.setCellValueFactory(new PropertyValueFactory<>("customerID"));

        scheduleTable1.getColumns().add(column11);
        column11.setPrefWidth(100);
        scheduleTable1.getColumns().add(column12);
        column12.setPrefWidth(120);
        scheduleTable1.getColumns().add(column14);
        scheduleTable1.getColumns().add(column13);
        scheduleTable1.getColumns().add(column15);
        column15.setPrefWidth(130);
        scheduleTable1.getColumns().add(column16);
        column16.setPrefWidth(130);
        scheduleTable1.getColumns().add(column17);
        scheduleTable1.setMaxHeight(100);
        scheduleTable1.setMinWidth(800);

        scheduleTable2.getColumns().add(column21);
        column11.setPrefWidth(100);
        scheduleTable2.getColumns().add(column22);
        column12.setPrefWidth(120);
        scheduleTable2.getColumns().add(column24);
        scheduleTable2.getColumns().add(column23);
        scheduleTable2.getColumns().add(column25);
        column15.setPrefWidth(130);
        scheduleTable2.getColumns().add(column26);
        column16.setPrefWidth(130);
        scheduleTable2.getColumns().add(column27);
        scheduleTable2.setMaxHeight(100);
        scheduleTable2.setMinWidth(800);

        scheduleTable3.getColumns().add(column31);
        column11.setPrefWidth(100);
        scheduleTable3.getColumns().add(column32);
        column11.setPrefWidth(120);
        scheduleTable3.getColumns().add(column34);
        scheduleTable3.getColumns().add(column33);
        scheduleTable3.getColumns().add(column35);
        column15.setPrefWidth(130);
        scheduleTable3.getColumns().add(column36);
        column16.setPrefWidth(130);
        scheduleTable3.getColumns().add(column37);
        scheduleTable3.setMaxHeight(100);
        scheduleTable3.setMinWidth(800);

        //report number of appointments by type and month
        int planningSessionCount = 0;
        int debriefingCount = 0;
        int otherCount = 0;
        int[] monthCount = {0,0,0,0,0,0,0,0,0,0,0,0};
        DateTimeFormatter dt = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a");
        for (Appointment a: appointmentList) {
            if (a.getType().equalsIgnoreCase("planning session")) {
                planningSessionCount++;
            } else if (a.getType().equalsIgnoreCase("de-briefing")) {
                debriefingCount++;
            } else {
                otherCount++;
            }
            LocalDate ld = LocalDate.parse((CharSequence) a.getStart(), dt);
            monthCount[ld.getMonthValue()-1]++;
        }
        Text typeReportTitle = new Text("Number of Appointments by Type: ");
        String planningSessions = "Planning Sessions: ", debriefings = "De-Briefings: ", otherAppointments = "Other Appointments: ";
        Text monthReportTitle = new Text("Number of Appointments by Month: " );
        Text activityTitle = new Text("Recent Activity: ");
        Text scheduleReportTitle1 = new Text("Anika Costa's Schedule");
        Text scheduleReportTitle2 = new Text("Daniel Garcia's Schedule");
        Text scheduleReportTitle3 = new Text("Li Lee's Schedule");
        String closeButtonString = "Close";
        activityTitle.setFont(boldFont);
        typeReportTitle.setFont(boldFont);
        monthReportTitle.setFont(boldFont);
        scheduleReportTitle1.setFont(boldFont);
        scheduleReportTitle2.setFont(boldFont);
        scheduleReportTitle3.setFont(boldFont);
        Text typeReport = new Text(planningSessions + planningSessionCount + "\n" + debriefings + debriefingCount + "\n" + otherAppointments + otherCount);
        String monthList1 = (""), monthList2 = (""), monthList3 = ("");
        int count = 0;
        for (int i = 0; i < 12; i++) {
            if (monthCount[i] > 0) {
                if (count <= 4) {
                    monthList1 = monthList1 + String.format(" %1$-12s: %2$2d \n", monthList[i], monthCount[i]);
                } else if (count > 4 && count <= 8) {
                    monthList2 = monthList2 + String.format(" %1$-12s: %2$2d \n", monthList[i], monthCount[i]);
                } else if (count > 8) {
                    monthList3 = monthList3 + String.format(" %1$-12s: %2$2d \n", monthList[i], monthCount[i]);
                }
                count++;
            }
        }

        Text monthReport1 = new Text(monthList1);
        Text monthReport2 = new Text(monthList2);
        Text monthReport3 = new Text(monthList3);
        //2022-03-30 10:40 PM
        Text activity = new Text(activityString5 + activityString4 + activityString3 + activityString2 + activityString1);

        for (Appointment a:appointmentList) {
            Appointment appointment = new Appointment(a.getAppointmentID(), a.getTitle(), a.getDescription(),
                    null, null, a.getType() , a.getStart() , a.getEnd() , a.getCustomerID(), 0);
            try {
                if (a.getContact().equals("Anika Costa")) {
                    scheduleList1.add(appointment);
                } else if (a.getContact().equals("Daniel Garcia")) {
                    scheduleList2.add(appointment);
                } else if (a.getContact().equals("Li Lee")) {
                    scheduleList3.add(appointment);
                } else if (a.getContact().equals(null)){
                }
            } catch (Exception e) {

            }
        }

        scheduleTable1.setItems(scheduleList1);
        scheduleTable2.setItems(scheduleList2);
        scheduleTable3.setItems(scheduleList3);

        Button closeButton = new Button(closeButtonString);
        closeButton.setOnAction(event -> {
            reportStage.close();
        });

        GridPane grid = new GridPane();
        grid.add(typeReportTitle, 1, 1, 1, 1);
        grid.add(typeReport, 1, 2, 1, 1);
        grid.add(monthReportTitle, 3, 1, 1, 1);
        grid.add(monthReport1, 3, 2, 1, 1);
        grid.add(monthReport2, 4, 2, 1, 1);
        grid.add(monthReport3, 5, 2, 1, 1);
        grid.add(activityTitle, 6, 1, 1, 1);
        grid.add(activity, 6, 2, 2, 2);
        grid.add(scheduleReportTitle1, 1, 3, 1, 1);
        grid.add(scheduleTable1, 1, 4, 6, 1);
        grid.add(scheduleReportTitle2, 1, 5, 1, 1);
        grid.add(scheduleTable2, 1, 6, 6, 1);
        grid.add(scheduleReportTitle3, 1, 7, 1, 1);
        grid.add(scheduleTable3, 1, 8, 6, 1);
        grid.add(closeButton, 5, 9, 1, 1);
        grid.setHgap(20);
        grid.setVgap(20);
        Scene scene = new Scene(grid, 900, 650);
        reportStage.setScene(scene);
        reportStage.show();
    }

    /**
     * The auto-generated customer ID.
     */
    private static int customerID = 1;
    /**
     * The list of all customers.
     */
    private static ObservableList<Customer> customerList = FXCollections.observableArrayList();
    /**
     * The table for viewing customer data.
     */
    private static TableView<Customer> customerTable = new TableView();

    /**
     * The method for building the customer table.
     * @throws SQLException if SQL query is invalid
     */
    public void customerTable() throws SQLException {
        String columnString1 = "Customer ID",
                columnString2 = "First Name",
                columnString3 = "Address",
                columnString4 = "Postal Code",
                columnString5 = "Phone Number",
                columnString6 = "Division",
                columnString7 = "Country";

        TableColumn<Customer, Integer> column1 = new TableColumn<>(columnString1);
        column1.setCellValueFactory(new PropertyValueFactory<>("customerID"));
        TableColumn<Customer, String> column2 = new TableColumn<>(columnString2);
        column2.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        TableColumn<Customer, String> column3 = new TableColumn<>(columnString3);
        column3.setCellValueFactory(new PropertyValueFactory<>("address"));
        TableColumn<Customer, String> column4 = new TableColumn<>(columnString4);
        column4.setCellValueFactory(new PropertyValueFactory<>("postalCode"));
        TableColumn<Customer, String> column5 = new TableColumn<>(columnString5);
        column5.setCellValueFactory(new PropertyValueFactory<>("phone"));
        TableColumn<Customer, String> column6 = new TableColumn<>(columnString6);
        column6.setCellValueFactory(new PropertyValueFactory<>("division"));
        TableColumn<Customer, String> column7 = new TableColumn<>(columnString7);
        column7.setCellValueFactory(new PropertyValueFactory<>("country"));

        customerTable.getColumns().add(column1);
        customerTable.getColumns().add(column2);
        customerTable.getColumns().add(column3);
        customerTable.getColumns().add(column4);
        customerTable.getColumns().add(column5);
        customerTable.getColumns().add(column6);
        customerTable.getColumns().add(column7);
        customerTable.setMaxHeight(150);
        customerTable.setMinWidth(710);

    }

    /**
     * The method for extracting customer information from database.
     * @throws SQLException if SQL query is invalid
     */
    public void customerList() throws SQLException{
        customerList.clear();

        //adds customers from database to table
        String query = "SELECT * FROM customers";
        PreparedStatement ps = JDBC.connection.prepareStatement(query);
        ResultSet resultset = null;
        try {
            resultset = ps.executeQuery("SELECT * FROM customers");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        boolean continueQuery = true;
        while(true){
            try {
                if (!resultset.next()) {
                    continueQuery = false;
                    break;
                }
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
            //for each customer
            if (continueQuery){
                String usernameQuery = null;
                String passwordQuery = null;
                String queryComb = null;
                Customer customer = new Customer();
                int divisionID;
                String division;
                String country;
                String[] divisionCountry;
                try {
                    customer.setCustomerID(resultset.getInt("Customer_ID"));
                    customer.setCustomerName(resultset.getString("Customer_Name"));
                    customer.setAddress(resultset.getString("Address"));
                    customer.setPostalCode(resultset.getString("Postal_Code"));
                    customer.setPhone(resultset.getString("Phone"));
                    //go through Division and Country tables
                    customer.setDivisionID(resultset.getInt("Division_ID"));
                    divisionCountry = divisionCountry(customer.getDivisionID());
                    customer.setDivision(divisionCountry[0]);
                    customer.setCountry(divisionCountry[1]);
                    if (customer.getCountry().equals("US")||customer.getCountry().equals("U.S")) {
                        customer.setCountry("U.S.");
                    } else if (customer.getCountry().equals("UK")||customer.getCountry().equals("U.K")) {
                        customer.setCountry("U.K.");
                    }
                    customerList.add(customer);
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
        }
        customerTable.setItems(customerList);
    }

    /**
     * The method for the login screen. Lambda expressions are used on loginButton to log into appointmentScreen with username and password and exitButton to close the window.
     * @throws SQLException if SQL query is invalid
     */
    public void loginScreen() throws SQLException {
        Stage loginStage = new Stage();
        ZoneId zone = ZoneId.systemDefault();
        String zoneString = zone.getId();
        Text localeText = new Text(String.valueOf(ZoneId.systemDefault()));

        loginStage.setTitle("Appointment Log-In");
        Label loginTitle = new Label("Please log in for appointments.");
        loginTitle.setAlignment(Pos.CENTER);
        Text loginError = new Text();
        loginError.setFill(Color.RED);

        Label usernameLabel = new Label("Username:");
        TextField usernameField = new TextField();

        //check username
        //queries need to have "select *"
        AtomicBoolean validUsername = new AtomicBoolean(false);
        String query = "SELECT * FROM customers";
        PreparedStatement ps = JDBC.connection.prepareStatement(query);
        //check username/password combination
        Label passwordLabel = new Label("Password:");
        TextField passwordField = new TextField();
        String password = passwordField.getText();

        Label locationLabel = new Label("Location: ");
        Button loginButton = new Button("Log In");
        //login button
        loginButton.setOnAction(event -> {
            ResultSet resultset = null;
            try {
                resultset = ps.executeQuery("SELECT * FROM users");
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
            String username = usernameField.getText();
            String loginComb = usernameField.getText() + " " + passwordField.getText();
            //defaults to false each login button press
            validUsername.set(false);
            //goes through table rows
            while (true){
                //while loop start
                boolean continueQuery = true;
                //breaks out of loop when last row is reached
                try {
                    if (!resultset.next()) {
                        continueQuery = false;
                        break;
                    }
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }

                if (continueQuery){
                    //while loop continue
                    String usernameQuery = null;
                    String passwordQuery = null;
                    String queryComb = null;
                    try {
                        usernameQuery = resultset.getString("User_Name");
                        passwordQuery = resultset.getString("Password");
                        queryComb = usernameQuery + " " + passwordQuery;
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                    }
                    //validates username/password combination
                    if (loginComb.equals(queryComb)){
                        validUsername.set(true);
                    }
                    //while loop end
                }
            } //end while loop
            //display message after username/password validation
            if (validUsername.compareAndSet(true, true)) {
                loginError.setText("");
                try {
                    appointmentScreen();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
                try {
                    //login activity
                    File loginActivity = new File("login_activity.txt");
                    if (!loginActivity.exists()){
                        loginActivity.createNewFile();
                    }
                    FileWriter fw = new FileWriter(loginActivity,true);
                    BufferedWriter bw = new BufferedWriter(fw);
                    LocalDate ld = LocalDate.now();
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm:ss a");
                    String lt = LocalTime.now().format(formatter);
                    bw.write("Log-in attempt with username \"" + username + "\" successful on " + ld + " at " + lt + "\n");
                    bw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                loginStage.close();
            } else if (validUsername.compareAndSet(false, false)){
                if (Locale.getDefault().getCountry().equals("FR") || Locale.getDefault().equals("fr_FR")){
                    loginError.setText("Mot de passe et identifiant \"" + username + "\" incorrects. Veuillez réessayer.");
                }
                else {
                    loginError.setText("Incorrect username \"" + username + "\" and password combination. Please try again.");
                }
                //login activity to log txt
                try {
                    File loginActivity = new File("login_activity.txt");
                    if (!loginActivity.exists()){
                        loginActivity.createNewFile();
                    }
                    FileWriter fw = new FileWriter(loginActivity,true);
                    BufferedWriter bw = new BufferedWriter(fw);LocalDate ld = LocalDate.now();
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm:ss a");
                    String lt = LocalTime.now().format(formatter);
                    bw.write("Log-in attempt with username \"" + username + "\" failed on " + ld + " at " + lt + "\n");
                    System.out.println("Login failed.");
                    bw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        });

        Button exitButton = new Button("Exit");
        exitButton.setOnAction(event -> {
            loginStage.close();
        });
        //if French
        if (Locale.getDefault().getCountry().equals("FR") || Locale.getDefault().equals("fr_FR")){
            loginStage.setTitle("Connexion au rendez-vous");
            loginTitle.setText("Veuillez vous connecter pour les rendez-vous.");
            usernameLabel.setText("Nom d'utilisateur: ");
            passwordLabel.setText("Mot de passe: ");
            locationLabel.setText("Emplacement: ");
            loginButton.setText("Connexion");
            exitButton.setText("Sortir");
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.add(loginTitle, 4, 0, 6, 1);
        grid.add(loginError, 2, 1, 5, 1);
        grid.add(usernameLabel, 0, 2, 3, 1);
        grid.add(usernameField, 3, 2, 2, 1);
        grid.add(passwordLabel, 0, 3, 3, 1);
        grid.add(passwordField, 3, 3, 2, 1);
        grid.add(locationLabel, 0, 4, 3, 1);
        grid.add(localeText, 4, 4, 3, 1);
        grid.add(loginButton, 6, 5,3, 1);
        grid.add(exitButton, 9, 5, 3, 1);

        Scene scene = new Scene(grid, 500, 300);
        loginStage.setScene(scene);
        loginStage.show();
    }

    /**
     * The method for the appointment screen. Lambda expressions are used for addAppointmentButton, updateAppointmentButton, deleteAppointmentButton, addCustomerButton, updateCustomerButton, deleteCustomerButton, reportButton, and logoutButton buttons to open their respective windows. Lambda expressions are also used allRadio, monthRadio and weekRadio to make combo boxes appear to filter appointment list by month and week.
     * @throws SQLException if SQL query is invalid
     */
    public void appointmentScreen() throws SQLException {
        Stage appointmentStage = new Stage();
        Label apptTableLabel = new Label("Appointments");
        apptTableLabel.setFont(boldFont);
        Label custTableLabel = new Label("Customers");
        custTableLabel.setFont(boldFont);
        appointmentList();
        customerList();

        //business hours
        String localStart = convertToLocal.convertTime("2020-01-01 08:00:00.0", "EST5EDT");
        LocalDateTime localStartLDT = LocalDateTime.parse(localStart, DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a"));
        LocalTime localStartTime = localStartLDT.toLocalTime();
        String localStartTimeA = localStartTime.format(DateTimeFormatter.ofPattern("h:mm a"));

        String localEnd = convertToLocal.convertTime("2020-01-01 22:00:00.0", "EST5EDT");
        LocalDateTime localEndLDT = LocalDateTime.parse(localEnd, DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a"));
        LocalTime localEndTime = localEndLDT.toLocalTime();
        String localEndTimeA = localEndTime.format(DateTimeFormatter.ofPattern("h:mm a"));

        dateTimeNote.setText("Use yyyy-MM-dd hh:mm a format (ex. 2020-01-01 01:00 PM). \nBusiness hours are " +
                localStartTimeA + " to " + localEndTimeA + " local time Monday through Sunday.");

        ToggleGroup filterToggle = new ToggleGroup();
        RadioButton allRadio = new RadioButton("Show All");
        allRadio.setToggleGroup(filterToggle);
        allRadio.setSelected(true);
        RadioButton monthRadio = new RadioButton("Sort by Month");
        monthRadio.setToggleGroup(filterToggle);

        RadioButton weekRadio = new RadioButton("Sort by Current Week");
        weekRadio.setToggleGroup(filterToggle);
        String May2022List[] = {"May 2-6","May 9-13","May 16-20","May 23-27","May 30-June 3"};
        String June2022List[] = {"June 6-10","June 13-17","June 20-24","June 27-July 1"};
        String July2022List[] = {"July 4-8","July 11-15","July 18-22","July 25-29"};
        String Aug2022List[] = {"August 1-5","August 8-12","August 15-19","August 22-26","August 29-September 2"};
        String Sept2022List[] = {"September 5-9","September 12-16","September 19-23","September 26-30"};
        String Oct2022List[] = {"October 3-7","October 10-14","October 17-21","October 24-28","October 31-November 4"};
        String Nov2022List[] = {"November 7-11","November 14-18","November 21-25","November 28-December 2"};
        String Dec2022List[] = {"December 5-9","December 12-16","December 19-23","December 26-30"};
        String Jan2023List[] = {"January 2-6","January 9-13","January 16-20","January 23-27","January 30-February 3"};
        String Feb2023List[] = {"February 6-10","February 13-17","February 20-24","February 27-March 3"};
        String Mar2023List[] = {"March 6-10","March 13-17","March 20-24","March 27-31"};
        String Apr2023List[] = {"April 3-7","April 10-14","April 17-21","April 24-28"};
        ComboBox weekBox = new ComboBox();

        appointmentStage.setTitle("Appointments");
        filteredList.setAll(appointmentList);

        Button addAppointmentButton = new Button("Add Appointment");
        addAppointmentButton.setOnAction(event -> {
            addUpdateAppt(false);
            appointmentStage.close();
        });
        Button updateAppointmentButton = new Button("Update Appointment");
        updateAppointmentButton.setOnAction(ev -> {
            try {
                int i = appointmentTable.getSelectionModel().getSelectedIndex();
                addUpdateAppt(true);
                appointmentStage.close();
            } catch (Exception ex) {
                String m = "Please select an appointment to update.";
                message.setText(m);
                alertWindow(m);
            }
        });
        Button deleteAppointmentButton = new Button("Delete Appointment");
        deleteAppointmentButton.setOnAction(ev -> {
            try {
                int i = appointmentTable.getSelectionModel().getSelectedIndex();
                int j = appointmentTable.getSelectionModel().getSelectedItem().getAppointmentID();
                deleteAppointment();
            } catch (Exception ex) {
                String m = "Please select an appointment to cancel.";
                message.setText(m);
                alertWindow(m);
            }
        });
        Button reportButton = new Button("View Reports");
        reportButton.setOnAction(event -> {
            report();
        });
        Button addCustomerButton = new Button("Add Customer");
        addCustomerButton.setOnAction(event -> {
            addUpdateCustomer(false);
            appointmentStage.close();
        });
        Button updateCustomerButton = new Button("Update Customer");
        updateCustomerButton.setOnAction(event -> {
            try {
                int i = customerTable.getSelectionModel().getSelectedIndex();
                addUpdateCustomer(true);
                appointmentStage.close();
            } catch (Exception e) {
                String m = "Please select a customer to update.";
                message.setText(m);
                alertWindow(m);
            }
        });
        Button deleteCustomerButton = new Button("Delete Customer");
        deleteCustomerButton.setOnAction(ev -> {
            boolean hasAppointments = false;
            try {
                int c = customerTable.getSelectionModel().getSelectedItem().getCustomerID();
                for (Appointment a: appointmentList){
                    if (a.getCustomerID() == c) {
                        message.setText("Customer currently has appointments.");
                        hasAppointments = true;
                    }
                }
                if (!hasAppointments){
                    deleteCustomer();
                } else {
                    alertWindow("Customer currently has appointments.");
                }
            } catch (Exception e) {
                String m = "Please select a customer to delete.";
                message.setText(m);
                alertWindow(m);
            }
        });

        Button logOutButton = new Button("Log Out");
        logOutButton.setOnAction(event -> {
            try {
                loginScreen();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
            appointmentStage.close();
        });

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setPadding(new Insets(10, 10, 10, 10));

        Button appointmentRefresh = new Button("Refresh");
        appointmentRefresh.setOnAction(e -> {
            try {
                appointmentList();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

        grid.add(appointmentAlert, 1, 1, 5, 1);
        grid.add(message, 1, 2, 2, 1);

        DateTimeFormatter dt = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a");

        grid.add(allRadio, 1, 3, 1, 1);
        AtomicBoolean hasMonthBox = new AtomicBoolean(false);
        allRadio.setOnAction(e -> {
            grid.getChildren().remove(monthBox);
            grid.getChildren().remove(weekBox);
            hasMonthBox.set(false);
            filteredList.setAll(appointmentList);
        });
        grid.add(monthRadio, 2, 3, 1, 1);
        monthRadio.setOnAction(e -> {
            grid.getChildren().remove(weekBox);
            grid.getChildren().remove(monthBox);
            grid.add(monthBox, 4, 3, 1, 1);
            for (Appointment a: appointmentList) {
                LocalDate ld = LocalDate.parse(a.getStart(), dt);
                filteredList.remove(a);
                if (ld.getMonthValue() == monthBox.getSelectionModel().getSelectedIndex()+1) {
                    filteredList.add(a);
                }
            }
        });

        grid.add(weekRadio, 3, 3, 1, 1);
        weekRadio.setOnAction(e -> {
            grid.getChildren().remove(monthBox);
            weekBox.setPromptText("Select Week");

            //get day a week from now
            LocalDateTime now = LocalDateTime.now();
            int dayOfMonth = LocalDateTime.now().getDayOfMonth();
            int day7 = dayOfMonth + 7;
            filteredList.removeAll(filteredList);

            for (Appointment a: appointmentList) {
                LocalDate apptDate = LocalDate.parse((CharSequence) a.getStart(), dt);
                if (apptDate.isAfter(ChronoLocalDate.from(now)) && apptDate.isBefore(ChronoLocalDate.from(now.plusDays(7)))) {
                    filteredList.add(a);
                } else {
                    filteredList.remove(a);
                }
            }
            //appointmentScreen
            appointmentTable.setItems(filteredList);
        });
        monthBox.setPromptText("Select Month");
        weekBox.setPromptText("Select Week");

        monthBox.setOnAction(e -> {
            filteredList.removeAll(filteredList);
            if(monthBox.getValue().equals("May")) {
                weekBox.setItems(FXCollections.observableArrayList(May2022List));
            } else if(monthBox.getValue().equals("June")) {
                weekBox.setItems(FXCollections.observableArrayList(June2022List));
            } else if(monthBox.getValue().equals("July")) {
                weekBox.setItems(FXCollections.observableArrayList(July2022List));
            } else if(monthBox.getValue().equals("August")) {
                weekBox.setItems(FXCollections.observableArrayList(Aug2022List));
            } else if(monthBox.getValue().equals("September")) {
                weekBox.setItems(FXCollections.observableArrayList(Sept2022List));
            } else if(monthBox.getValue().equals("October")) {
                weekBox.setItems(FXCollections.observableArrayList(Oct2022List));
            } else if(monthBox.getValue().equals("November")) {
                weekBox.setItems(FXCollections.observableArrayList(Nov2022List));
            } else if(monthBox.getValue().equals("December")) {
                weekBox.setItems(FXCollections.observableArrayList(Dec2022List));
            } else if(monthBox.getValue().equals("January")) {
                weekBox.setItems(FXCollections.observableArrayList(Jan2023List));
            } else if(monthBox.getValue().equals("February")) {
                weekBox.setItems(FXCollections.observableArrayList(Feb2023List));
            } else if(monthBox.getValue().equals("March")) {
                weekBox.setItems(FXCollections.observableArrayList(Mar2023List));
            } else if(monthBox.getValue().equals("April")) {
                weekBox.setItems(FXCollections.observableArrayList(Apr2023List));
            }

            for (Appointment a: appointmentList) {
                LocalDate ld = LocalDate.parse(a.getStart(), dt);
                filteredList.remove(a);
                if (ld.getMonthValue() == monthBox.getSelectionModel().getSelectedIndex()+1) {
                    filteredList.add(a);
                }
            }
            //appointmentScreen
            appointmentTable.setItems(filteredList);
        });

        RowConstraints row1 = new RowConstraints(10);
        RowConstraints row2 = new RowConstraints(10);
        RowConstraints row3 = new RowConstraints(10);
        RowConstraints row4 = new RowConstraints(30);
        grid.getRowConstraints().addAll(row1, row2, row3, row4);
        grid.add(apptTableLabel, 1, 4, 6, 1);

        grid.add(appointmentTable, 1, 5, 6, 1);
        grid.add(addAppointmentButton, 1, 6, 1, 1);
        grid.add(updateAppointmentButton, 2, 6, 1,1);
        grid.add(deleteAppointmentButton, 3, 6, 1, 1);
        grid.add(reportButton, 4, 6, 1, 1);
        grid.add(custTableLabel, 1, 7, 6, 1);

        grid.add(customerTable, 1, 8, 4, 1);
        grid.add(addCustomerButton, 1, 9, 1, 1);
        grid.add(updateCustomerButton, 2, 9, 1, 1);
        grid.add(deleteCustomerButton, 3, 9, 1, 1);
        grid.add(logOutButton, 4, 9, 1, 1);

        Scene scene = new Scene(grid,1000, 650);
        appointmentStage.setScene(scene);
        appointmentStage.show();
        upcomingAppt();
    }

    /**
     * The method for validating appointment times.
     * @param appointment the appointment information being saved
     * @return the alert message if appointment times are invalid
     */
    public static String timeValidation(Appointment appointment) {
        Appointment saveAppointment = new Appointment();
        String message = "";

        //checks if start and end times are within business hours
        LocalTime openTime = LocalTime.of(8, 0, 0);
        LocalTime closeTime = LocalTime.of(22, 0, 0);
        //check if start and end fields are blank
        try {
            //problem
            LocalDateTime startTest = LocalDateTime.parse(appointment.getStart(), DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a"));
            LocalDateTime endTest = LocalDateTime.parse(appointment.getEnd(), DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a"));
            //check if end time happens before start time
            //convert times to EST
            String startESTString = localToZone.convertTime(appointment.getStart(), "EST5EDT");
            LocalTime startEST = LocalTime.parse(startESTString, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String endESTString = localToZone.convertTime(appointment.getEnd(), "EST5EDT");
            LocalTime endEST = LocalTime.parse(endESTString, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            //2022-03-15 10:40 PM
            //check times against business hours 2022-03-15 11:40 PM

            if (startEST.isBefore(openTime) ||
                    startEST.isAfter(closeTime) ||
                    endEST.isBefore(openTime) ||
                    endEST.isAfter(closeTime)) {
                message = "Please enter times within business hours.";
            }
            if (startTest.isAfter(endTest)) {
                message = "Start time must be before end time.";
            }
            //check conflicts
            for (Appointment list : appointmentList) {
                LocalDateTime apptStart = convertFormat(appointment.getStart(), null);
                LocalDateTime apptEnd = convertFormat(appointment.getEnd(), null);
                LocalDateTime listStart = convertFormat(list.getStart(), null).minusMinutes(1);
                LocalDateTime listEnd = convertFormat(list.getEnd(), null).plusMinutes(1);
                int listID = list.getAppointmentID();
                String m = "User's or contact's appointment times conflict with other appointments.";
                if (appointment.getCustomerID() == list.getCustomerID() || appointment.getContact().equals(list.getContact())) {
                    if (apptStart.isAfter(listStart) && apptStart.isBefore(listEnd) && listID != appointment.getAppointmentID()) {
                        //if appointment start time happens during another appointment
                        message = m;
                    } else if (apptStart.isBefore(listStart) && apptEnd.isAfter(listEnd) && listID != appointment.getAppointmentID()) {
                        //if appointment start and end times wrap around another appointment
                        message = m;
                    } else if (apptEnd.isAfter(listStart) && apptEnd.isBefore(listEnd) && listID != appointment.getAppointmentID()) {
                        //if appointment end time happens during another appointment
                        message = m;
                    }
                }

            }
        } catch (Exception e) {
            message = "Please enter a start and end time.";
        }
        return message;
    }

    /**
     * The method for validating a customer ID listed for an appointment in appointment form.
     * @param customerID the customer ID attached to the appointment
     * @return whether the customer ID is valid or not
     */
    public static boolean customerIDValidation(int customerID){
        boolean valid = false;
        for (Customer customer: customerList) {
            if (customerID == customer.getCustomerID()) {
                valid = true;
            }
        }
        return valid;
    }

    /**
     * The method for validating a user ID listed for an appointment in appointment form.
     * @param userID the user ID attached to an appointment
     * @return whether the user ID is valid
     * @throws SQLException if SQL query is invalid
     */
    public static boolean userIDValidation(int userID)  throws SQLException{

        boolean valid = false;
        String query = "SELECT * FROM users";
        PreparedStatement ps = JDBC.connection.prepareStatement(query);
        ResultSet resultset = null;
        try {
            resultset = ps.executeQuery("SELECT * FROM users");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        boolean continueQuery = true;
        while(true){
            try {
                if (!resultset.next()) {
                    continueQuery = false;
                    break;
                }
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
            //for each user
            if (continueQuery){
                try {
                    if (userID == resultset.getInt("User_ID")) {
                        valid = true;
                    }
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
        }
        return valid;
    }

    /**
     * The method for adding or updating appointments. Lambda expressions are used to add functionality to saveButton to save appointments and cancelButton to close the window without saving appointment.
     * @param update whether to use the method for adding or updating an appointment
     */
    public void addUpdateAppt(boolean update) {
        Stage addUpdateApptStage = new Stage();
        addUpdateApptStage.setTitle("Add Appointment");
        dateTimeCheck.setText("");
        String appointmentLabelString = "Appointment ID: ",
                titleLabelString = "Title: ",
                descriptionLabelString = "Description: ",
                locationLabelString = "Location: ",
                contactLabelString = "Contact: ",
                typeLabelString = "Type: ",
                startLabelString = "Start Date and Time: ",
                endLabelString = "End Date and Time: ",
                customerLabelString = "Customer ID: ",
                userLabelString = "User ID: ",
                saveButtonLabelString = "Update Appointment",
                selectContactString = "Please select a contact.",
                checkTimesValid = "Please check that times are valid.",
                checkTimesBusinessHours = "Please enter times within business hours.",
                cancelButtonString = "Cancel";

        Label title = new Label("");
        Label appointmentIDLabel = new Label(appointmentLabelString);
        TextField appointmentIDField = new TextField("Auto-generated");
        appointmentIDField.setDisable(true);
        Label titleLabel = new Label(titleLabelString);
        TextField titleField = new TextField();
        Label descriptionLabel = new Label(descriptionLabelString);
        TextField descriptionField = new TextField();
        Label locationLabel = new Label(locationLabelString);
        TextField locationField = new TextField();
        Label contactLabel = new Label(contactLabelString);
        contactBox.getSelectionModel().selectFirst();

        Label typeLabel = new Label(typeLabelString);
        TextField typeField = new TextField();
        Label startLabel = new Label(startLabelString);
        TextField startField = new TextField("2020-01-02 01:00 PM");
        Label endLabel = new Label(endLabelString);
        TextField endField = new TextField("2020-01-02 02:00 PM");
        Label customerIDLabel = new Label(customerLabelString);
        TextField customerIDField = new TextField("1");
        Label userIDLabel = new Label(userLabelString);
        TextField userIDField = new TextField("1");

        AtomicReference<String> atomicValidIDString = new AtomicReference<>("");
        Text IDAlert = new Text();

        AtomicReference<String> m = new AtomicReference<>(), ar = new AtomicReference<>();
        m.set("New appointment has been added.");
        ar.set("Added appointment");
        int testID = 0;
        if (update) {
            addUpdateApptStage.setTitle("Update Appointment");
            Appointment selectedAppointment = appointmentTable.getSelectionModel().getSelectedItem();
            appointmentIDField.setText(String.valueOf(selectedAppointment.getAppointmentID()));
            titleField.setText(selectedAppointment.getTitle());
            descriptionField.setText(selectedAppointment.getDescription());
            locationField.setText(selectedAppointment.getLocation());
            contactBox.getSelectionModel().select(selectedAppointment.getContact());
            typeField.setText(selectedAppointment.getType());
            startField.setText(selectedAppointment.getStart());
            endField.setText(selectedAppointment.getEnd());
            customerIDField.setText(String.valueOf(selectedAppointment.getCustomerID()));
            userIDField.setText(String.valueOf(selectedAppointment.getUserID()));
            m.set("Appointment has been updated.");
            ar.set("Updated appointment");
            testID = Integer.parseInt(appointmentIDField.getText());
        }
        final int finaltestID = testID;
        Button saveButton = new Button(saveButtonLabelString);
        saveButton.setOnAction(actionEvent -> {
            dateTimeCheck.setText("");
            int saveID = finaltestID;
            boolean noError = true;
            Appointment saveAppointment = new Appointment();
            saveAppointment.setAppointmentID(finaltestID);
            saveAppointment.setTitle(titleField.getText());
            saveAppointment.setDescription(descriptionField.getText());
            saveAppointment.setLocation(locationField.getText());
            saveAppointment.setContact((String) contactBox.getValue());
            saveAppointment.setType(typeField.getText());

            saveAppointment.setStart(startField.getText());
            saveAppointment.setEnd(endField.getText());
            saveAppointment.setCustomerID(Integer.parseInt(customerIDField.getText()));
            saveAppointment.setUserID(Integer.parseInt(userIDField.getText()));

            boolean validIDs = true;
            String validIDString = "";
            if (!customerIDValidation (saveAppointment.getCustomerID())) {
                atomicValidIDString.set("Please enter valid Customer ID & User ID.");
                validIDString = "Please enter valid Customer ID & User ID.";
                validIDs = false;
            }
            try {
                if (!userIDValidation(saveAppointment.getUserID())){
                    atomicValidIDString.set("Please enter valid Customer ID & User ID.");
                    validIDString = "Please enter valid Customer ID & User ID.";
                    validIDs = false;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            IDAlert.setText(validIDString);

            int contactID = 0;
            try {
                contactID = contact((String) contactBox.getValue());
            } catch (SQLException e) {
                e.printStackTrace();
            }

            String timeAlert = timeValidation(saveAppointment);
            dateTimeCheck.setText(timeAlert);

            //saves contact if no errors
            if (timeAlert.equals("") && validIDs) {
                String aStart = formatForDatabase.convertTime(startField.getText(), "UTC"); //2022-04-15 12:00 PM
                saveAppointment.setStart(aStart);
                String aEnd = formatForDatabase.convertTime(endField.getText(), "UTC");
                saveAppointment.setEnd(aEnd);

                try {
                    if(update) {
                        Query.updateAppointment(saveAppointment);
                    } else if (!update) {
                        Query.addAppointment(saveAppointment);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                message.setText(m.get());
                activityReport(ar.get());
                addUpdateApptStage.close();
                try {
                    appointmentScreen();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                alertWindow(m.get());
            }
            //end of saveButton
        });
        Button cancelButton = new Button("Cancel");
        cancelButton.setOnAction(actionEvent -> {
            addUpdateApptStage.close();
            try {
                appointmentScreen();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.add(title, 1, 1, 1, 1);
        grid.add(appointmentIDLabel, 1, 2, 1, 1);
        grid.add(appointmentIDField, 2, 2, 1, 1);
        grid.add(titleLabel, 3, 2, 1, 1);
        grid.add(titleField, 4, 2, 1, 1);
        grid.add(descriptionLabel, 1, 3, 1, 1);
        grid.add(descriptionField, 2, 3, 1, 1);
        grid.add(locationLabel, 3, 3, 1, 1);
        grid.add(locationField, 4, 3, 1, 1);
        grid.add(contactLabel, 1, 4, 1, 1);
        grid.add(contactBox, 2, 4, 1, 1);
        grid.add(typeLabel, 3, 4, 1, 1);
        grid.add(typeField, 4, 4, 1, 1);
        grid.add(startLabel, 1, 5, 1, 1);
        grid.add(startField, 2, 5, 1, 1);
        grid.add(dateTimeNote, 3, 5, 3, 1);
        grid.add(dateTimeCheck, 3, 6, 3, 1);
        grid.add(endLabel, 1, 6, 1, 1);
        grid.add(endField, 2, 6, 1, 1);
        grid.add(customerIDLabel, 1, 7, 1, 1);
        grid.add(customerIDField, 2, 7, 1, 1);
        grid.add(userIDLabel, 3, 7, 1, 1);
        grid.add(userIDField, 4, 7, 1, 1);
        grid.add(saveButton, 1, 8, 1, 1);
        grid.add(cancelButton, 2, 8, 1, 1);
        grid.add(IDAlert, 3, 8, 2, 1);
        Scene scene = new Scene (grid,800, 500);
        addUpdateApptStage.setScene(scene);
        addUpdateApptStage.show();
    }

    /**
     * The method for deleting appointments. Lambda expressions are used to add functionality to deleteButton to confirm deleting appointments and cancelButton to close the window without deleting.
     */
    public void deleteAppointment() {
        Stage deleteStage = new Stage();
        Text deleteText = new Text();
        deleteStage.setTitle("Delete Appointment");
        deleteText.setText("Are you sure you want delete this appointment?");

        Button deleteButton = new Button("Delete");
        deleteButton.setOnAction(event -> {
            Appointment appointment = appointmentTable.getSelectionModel().getSelectedItem();
            String m = "A(n) " + appointment.getType() + " appointment \nwith ID: " + appointment.getAppointmentID() + " has been deleted.";
            message.setText(m);
            upcomingAppt();
            alertWindow(m);
            activityReport("Deleted appointment");
            try {
                Query.deleteAppointment(appointment.getAppointmentID());
                appointmentList();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            deleteStage.close();
        });
        Button cancelDeletionButton = new Button("Cancel");
        cancelDeletionButton.setOnAction(event -> {
            deleteStage.close();
        });

        GridPane grid = new GridPane();
        grid.add(deleteText, 1, 1, 2, 1);
        grid.add(deleteButton,1, 2, 1, 1);
        grid.add(cancelDeletionButton, 2, 2, 1, 1);
        grid.setHgap(20);
        grid.setVgap(20);
        Scene scene = new Scene(grid, 400, 150);
        deleteStage.setScene(scene);
        deleteStage.show();
    }

    /**
     * The method for adding or updating customers. Lambda expressions are used to add functionality to saveButton to save customer information and cancelButton to close the window without saving customer. Lambda expression is also used to add functionality to a selection in countryBox to determine first-level division list based on selected country.
     * @param update whether to use the method for adding or updating a customer
     */
    public void addUpdateCustomer(boolean update) {
        Stage addUpdateCustStage = new Stage();
        addUpdateCustStage.setTitle("Add New Customer");

        Label title = new Label("");
        Label customerIDLabel = new Label("Customer ID: ");
        TextField customerIDField = new TextField(String.valueOf(customerID));
        customerIDField.setText("Auto-generated");
        customerID++;
        customerIDField.setDisable(true);
        Label customerNameLabel = new Label("Customer Name: ");
        TextField customerNameField = new TextField();
        Label addressLabel = new Label("Address: ");
        TextField addressField = new TextField();
        Label postalLabel = new Label("Postal Code: ");
        TextField postalField = new TextField();
        Label countryLabel = new Label("Country: ");
        Label divisionLabel = new Label("State, province or division: ");
        Label phoneLabel = new Label("Phone Number: ");
        TextField phoneField = new TextField();

        AtomicReference<String> m = new AtomicReference<>(), ar = new AtomicReference<>();
        m.set("New customer has been added.");
        ar.set("Added customer");
        divisionBox.setItems(FXCollections.observableArrayList(USStates));
        countryBox.setOnAction(e -> {
            if (countryBox.getValue().equals("U.S.")){
                divisionBox.setItems(FXCollections.observableArrayList(USStates));
            } else if (countryBox.getValue().equals("Canada")){
                divisionBox.setItems(FXCollections.observableArrayList(canadaProvinces));
            } else if (countryBox.getValue().equals("U.K.")){
                divisionBox.setItems(FXCollections.observableArrayList(UKDivisions));
            }
            divisionBox.getSelectionModel().selectFirst();
        });
        countryBox.getSelectionModel().selectFirst();
        divisionBox.getSelectionModel().selectFirst();
        if (update) {
            addUpdateCustStage.setTitle("Update Customer");
            Customer selectedCustomer = customerTable.getSelectionModel().getSelectedItem();
            customerIDField.setText(String.valueOf(selectedCustomer.getCustomerID()));
            customerNameField.setText(selectedCustomer.getCustomerName());
            addressField.setText(selectedCustomer.getAddress());
            postalField.setText(selectedCustomer.getPostalCode());
            phoneField.setText(selectedCustomer.getPhone());
            countryBox.getSelectionModel().select(selectedCustomer.getCountry());
            divisionBox.getSelectionModel().select(selectedCustomer.getDivision());
            m.set("Customer had been updated.");
            ar.set("Updated customer");
        }

        Button saveButton = new Button("Save");
        saveButton.setOnAction(actionEvent -> {
            Customer customer = new Customer();
            customer.setCustomerName(customerNameField.getText());
            customer.setAddress(addressField.getText());
            customer.setPostalCode(postalField.getText());
            customer.setCountry((String)countryBox.getValue());
            customer.setDivision((String)divisionBox.getValue());
            customer.setPhone(phoneField.getText());
            int divisionID = 1;
            try {
                divisionID = divisionNameID((String)divisionBox.getValue());
                customer.setDivisionID(divisionID);
                if (update) {
                    customer.setCustomerID(Integer.parseInt(customerIDField.getText()));
                    Query.updateCustomer(customer);
                } else if (!update) {
                    Query.addCustomer(customer);
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
            message.setText(String.valueOf(m));
            activityReport(String.valueOf(ar));

            addUpdateCustStage.close();
            try {
                appointmentScreen();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
            alertWindow(String.valueOf(m));
        });
        Button cancelButton = new Button("Cancel");
        cancelButton.setOnAction(actionEvent -> {
            addUpdateCustStage.close();
            try {
                appointmentScreen();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        });
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.add(title, 1, 1, 1, 1);
        grid.add(customerIDLabel, 1, 2, 1, 1);
        grid.add(customerIDField, 2, 2, 1, 1);
        grid.add(customerNameLabel, 3, 2, 1, 1);
        grid.add(customerNameField, 4, 2, 1, 1);
        grid.add(addressLabel, 1, 3, 1, 1);
        grid.add(addressField, 2, 3, 1, 1);
        grid.add(postalLabel, 3, 3, 1, 1);
        grid.add(postalField, 4, 3, 1, 1);
        grid.add(countryLabel, 1, 4, 1, 1);
        grid.add(countryBox, 2, 4, 1, 1);
        grid.add(divisionLabel, 3, 4, 1, 1);
        grid.add(divisionBox,4, 4, 1, 1);
        divisionBox.setMinWidth(200);
        grid.add(phoneLabel, 1, 5, 1, 1);
        grid.add(phoneField, 2, 5, 1, 1);
        grid.add(saveButton, 1, 7, 1, 1);
        grid.add(cancelButton, 2, 7, 1, 1);
        Scene scene = new Scene (grid,800, 500);
        addUpdateCustStage.setScene(scene);
        addUpdateCustStage.show();
    }

    /**
     * The method for deleting customers. Lambda expressions are used to add functionality to deleteButton to confirm deleting customer and cancelButton to close window without deleting.
     */
    public void deleteCustomer() {
        Stage deleteStage = new Stage();
        Text deleteText = new Text();
        deleteStage.setTitle("Delete Customer");
        deleteText.setText("Are you sure you want delete this customer?");

        Button deleteButton = new Button("Delete");
        deleteButton.setOnAction(event -> {
            Customer customer = customerTable.getSelectionModel().getSelectedItem();
            String m = "Customer with ID: " + customer.getCustomerID() + " has been deleted.";
            message.setText(m);
            alertWindow(m);
            activityReport("Deleted customer");
            try {
                Query.deleteCustomer(customer.getCustomerID());
                customerList();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            deleteStage.close();
        });
        Button cancelDeletionButton = new Button("Cancel");
        cancelDeletionButton.setOnAction(event -> {
            deleteStage.close();
        });

        GridPane grid = new GridPane();
        grid.add(deleteText, 1, 1, 2, 1);
        grid.add(deleteButton,1, 2, 1, 1);
        grid.add(cancelDeletionButton, 2, 2, 1, 1);
        grid.setHgap(20);
        grid.setVgap(20);
        Scene scene = new Scene(grid, 400, 150);
        deleteStage.setScene(scene);
        deleteStage.show();
    }

    /**
     * The start method.
     * @param primaryStage the main window
     * @throws Exception for any exception
     */
    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));
        String sql = "SELECT * FROM users";
        PreparedStatement ps = JDBC.connection.prepareStatement(sql);
        ResultSet resultset = ps.executeQuery("SELECT * FROM users");

        customerTable();
        appointmentTable();
        loginScreen();
    }

    /**
     * The main method.
     * @param args command-line arguments
     * @throws SQLException if SQL squery is invalid
     */
    public static void main(String[] args) throws SQLException {
        JDBC.openConnection();
        launch(args);
        JDBC.closeConnection();
    }
}
