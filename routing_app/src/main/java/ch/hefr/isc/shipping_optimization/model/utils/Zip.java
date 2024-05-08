package ch.hefr.isc.shipping_optimization.model.utils;

public class Zip {

    private final String zip;

    private Zip(String zip) {
        this.zip = zip;
    }

    public static boolean isInvalid(String zip) {
        if (zip == null) return true;
        if (zip.length() != 4) return true;
        if (!zip.matches("^[1-9]\\d{3}$")) return true;
        return false;
    }

    public static Zip of(String zip) throws IllegalArgumentException {
        if (zip == null) throw new IllegalArgumentException("Zip cannot be null");
        if (zip.length() != 4)
            throw new IllegalArgumentException("Zip has to be 4 digits long");
        if (!zip.matches("^[1-9]\\d{3}$"))
            throw new IllegalArgumentException("Zip has to be a number");
        return new Zip(zip);
    }

    @Override
    public String toString() {
        return zip;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!(obj instanceof Zip other)) return false;
        return zip.equals(other.zip);
    }

    @Override
    public int hashCode() {
        return zip.hashCode();
    }
}
