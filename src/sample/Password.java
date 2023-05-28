package sample;

/**
 * Contains password fields and methods
 *
 * @author Long Tran
 */
public class Password {
    private String password;

    /**
     * The method to set password
     * @param password the password
     */
    public Password (String password) {
        this.password = password;
    }

    /**
     * The method to get password
     * @return the password
     */
    public String getPassword() { return password; }
}

