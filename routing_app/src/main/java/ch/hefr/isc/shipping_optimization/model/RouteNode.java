package ch.hefr.isc.shipping_optimization.model;

import ch.hefr.isc.shipping_optimization.model.utils.Zip;

public class RouteNode {

    private final Order order;
    private final Zip depotZip;

    private RouteNode(Order order, Zip depotZip) {
        this.order = order;
        this.depotZip = depotZip;
    }

    public static RouteNode depotNode(Zip depotZip) {
        return new RouteNode(null, depotZip);
    }

    public static RouteNode orderNode(Order order) {
        return new RouteNode(order, null);
    }

    public boolean isDepot() {
        return order == null;
    }

    public Order order() {
        return order;
    }

    public Zip zip() {
        if (isDepot())
            return depotZip;
        return order.zip();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!(obj instanceof RouteNode other)) return false;
        if (isDepot() && other.isDepot())
            return depotZip.equals(other.depotZip);
        if (!isDepot() && !other.isDepot())
            return order.equals(other.order);
        return false;
    }

    @Override
    public int hashCode() {
        if (isDepot())
            return depotZip.hashCode();
        return order.hashCode();
    }
}
