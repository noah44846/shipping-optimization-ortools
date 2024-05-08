package ch.hefr.isc.shipping_optimization.routing;

import ch.hefr.isc.shipping_optimization.model.RouteNode;
import ch.hefr.isc.shipping_optimization.model.utils.Distance;

import java.time.Duration;

public interface DataModel {
    RoutingConfig config();

    int numberRouteNodes();

    RouteNode routeNodeAt(int index);

    Distance distance(RouteNode from, RouteNode to);

    Duration duration(RouteNode from, RouteNode to);
}
