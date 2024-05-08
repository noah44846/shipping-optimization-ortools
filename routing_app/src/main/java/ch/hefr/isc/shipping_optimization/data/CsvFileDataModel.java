package ch.hefr.isc.shipping_optimization.data;

import ch.hefr.isc.shipping_optimization.model.Order;
import ch.hefr.isc.shipping_optimization.model.RouteNode;
import ch.hefr.isc.shipping_optimization.model.utils.Distance;
import ch.hefr.isc.shipping_optimization.routing.DataModel;
import ch.hefr.isc.shipping_optimization.routing.RoutingConfig;

import java.time.Duration;
import java.util.List;

public class CsvFileDataModel implements DataModel {
    private final List<Order> orders;
    private final DistanceMatrix distanceMatrix;
    private final RoutingConfig config;

    public CsvFileDataModel(List<Order> orders, DistanceMatrix distanceMatrix, RoutingConfig config) {
        this.orders = orders;
        this.distanceMatrix = distanceMatrix;
        this.config = config;
    }

    @Override
    public RoutingConfig config() {
        return config;
    }

    @Override
    public int numberRouteNodes() {
        return orders.size() + 1;
    }

    @Override
    public RouteNode routeNodeAt(int index) {
        if (index == 0) return RouteNode.depotNode(config.depotZip());
        return RouteNode.orderNode(orders.get(index - 1));
    }

    @Override
    public Distance distance(RouteNode from, RouteNode to) {
        if (from.isDepot() && to.isDepot())
            return Distance.ofKiloMeters(0);
        return distanceMatrix.distance(from.zip(), to.zip());
    }

    @Override
    public Duration duration(RouteNode from, RouteNode to) {
        if (from.isDepot() && to.isDepot())
            return Duration.ofSeconds(0);
        return distanceMatrix.duration(from.zip(), to.zip());
    }
}
