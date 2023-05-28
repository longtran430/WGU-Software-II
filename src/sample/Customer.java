package sample;

/**
 * Contains customer fields and methods
 *
 * @author Long Tran
 */
public class Customer {
    private int customerID;
    private String customerName;
    private String address;
    private String postalCode;
    private String phone;
    private int divisionID;
    private String division;
    private String country;

    public Customer() {
        customerID = 0;
        customerName = null;
        address = null;
        postalCode = null;
        phone = null;
        division = null;
        country = null;
    }
    public Customer(int customerID, String customerName, String address, String postalCode, String phone, int divisionID, String division, String country) {
        this.customerID = customerID;
        this.customerName = customerName;
        this.address = address;
        this.postalCode = postalCode;
        this.phone = phone;
        this.divisionID = divisionID;
        this.division = division;
        this.country = country;
    }

    /**
     * The method to get the customer's ID
     * @return the customer DI
     */
    public int getCustomerID() { return customerID; }

    /**
     * The method to get the customer's name
     * @return the customer name
     */
    public String getCustomerName() { return customerName; }

    /**
     * The method to get the customer's address
     * @return the customer address
     */
    public String getAddress() { return address; }

    /**
     * The method to get the customer's postal code
     * @return the customer's postal code
     */
    public String getPostalCode() { return postalCode; }

    /**
     * The method to get the customer's phone number
     * @return the customer's phone number
     */
    public String getPhone() { return phone; }

    /**
     * The method to get the customer's first-level divisoin ID
     * @return the customer's first-level division ID
     */
    public int getDivisionID() { return divisionID; }

    /**
     * The method to get the customer's first-level division
     * @return the customer's first-level division
     */
    public String getDivision() { return division; }

    /**
     * The method to get the customer's country
     * @return the customer's country
     */
    public String getCountry() { return country; }

    /**
     * The method to set the customer's ID
     * @param customerID the customer ID
     */
    public void setCustomerID(int customerID) { this.customerID = customerID; }

    /**
     * The method to set the customer's name
     * @param customerName the customer name
     */
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    /**
     * The method to get the customer's address
     * @param address the customer address
     */
    public void setAddress(String address) { this.address = address; }

    /**
     * The method to set the customer's postal code
     * @param postalCode the customer's postal code
     */
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    /**
     * The method to set the customer's phone number
     * @param phone the customer's phone number
     */
    public void setPhone(String phone) { this.phone = phone; }

    /**
     * The method to set the customer's first-level division ID
     * @param divisionID the customer's first-level division ID
     */
    public void setDivisionID(int divisionID) { this.divisionID = divisionID; }

    /**
     * The method to set the customer's first-level division
     * @param division the customer's first-level division
     */
    public void setDivision(String division) { this.division = division; }

    /**
     * The method to set the customer's country
     * @param country the customer's country
     */
    public void setCountry(String country) { this.country = country; }
}

