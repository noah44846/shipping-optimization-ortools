package ch.hefr.isc.shipping_optimization.model.utils;

public class Weight {
    private final double weight;
    private final boolean isKg;

    private Weight(double weight, boolean isKg) {
        this.isKg = isKg;
        this.weight = weight;
    }

    public static Weight ofKiloGrams(double kg) {
        return new Weight(kg, true);
    }

    public static Weight ofGrams(double g) {
        return new Weight(g, false);
    }

    public double kiloGrams() {
        return isKg ? weight : weight / 1000;
    }

    public double grams() {
        return isKg ? weight * 1000 : weight;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!(obj instanceof Weight other)) return false;
        return weight == other.weight && isKg == other.isKg;
    }

    @Override
    public int hashCode() {
        return Double.hashCode(weight) * 31 + Boolean.hashCode(isKg);
    }
}
