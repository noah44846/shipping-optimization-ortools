package ch.hefr.isc.shipping_optimization.model;

import ch.hefr.isc.shipping_optimization.model.utils.Weight;
import ch.hefr.isc.shipping_optimization.model.utils.Zip;

public record Order(String id, Zip zip, int numberBoxes, TimeWindow timeWindow, Weight weight,
                    float hubDeliveryCostPerBox) {
    public float totalHubDeliveryCost() {
        return numberBoxes * hubDeliveryCostPerBox;
    }
}
