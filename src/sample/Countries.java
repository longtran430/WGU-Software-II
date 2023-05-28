package sample;

/**
 * Contains country fields and methods
 *
 * @author Long Tran
 */
public class Countries {
    private int id;
    private String name;

    public Countries (int id, String name){
        this.id = id;
        this.name = name;
    }

    /**
     * The method to get country ID
     * @return The country ID
     */
    public int getId() { return id; }

    /**
     * The method to get country name
     * @return The country name
     */
    public String getName() { return name; }
}
