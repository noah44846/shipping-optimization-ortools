package ch.hefr.isc.shipping_optimization.model.utils;

public class Distance {

    private final double distance;
    private final boolean isKm;

    private Distance(double distance, boolean isKm) {
        this.isKm = isKm;
        this.distance = distance;
    }

    public static Distance ofKiloMeters(double km) {
        return new Distance(km, true);
    }

    public static Distance ofMeters(double m) {
        return new Distance(m, false);
    }

    public double kiloMeters() {
        return isKm ? distance : distance / 1000;
    }

    public double meters() {
        return isKm ? distance * 1000 : distance;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!(obj instanceof Distance other)) return false;
        return distance == other.distance && isKm == other.isKm;
    }

    @Override
    public int hashCode() {
        return Double.hashCode(distance) * 31 + Boolean.hashCode(isKm);
    }
}
