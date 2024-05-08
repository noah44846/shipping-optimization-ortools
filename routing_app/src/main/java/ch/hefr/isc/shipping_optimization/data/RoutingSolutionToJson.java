package ch.hefr.isc.shipping_optimization.data;

import ch.hefr.isc.shipping_optimization.model.Order;
import ch.hefr.isc.shipping_optimization.model.RouteNode;
import ch.hefr.isc.shipping_optimization.model.RoutingSolution;
import org.json.JSONArray;
import org.json.JSONObject;

public class RoutingSolutionToJson {

    public static JSONObject parseSolution(RoutingSolution solution) {
        JSONObject json = new JSONObject();
        json.put("totalDistance", solution.totalDistance().kiloMeters());
        json.put("lastRouteEndTime", solution.lastRouteEndTime());
        json.put("totalMonetaryCost", solution.totalMonetaryCost());
        json.put("totalCostWithoutDirectDelivery", solution.totalCostWithoutDirectDelivery());

        JSONArray routes = new JSONArray();
        for (int i = 0; i < solution.routes().size(); i++) {
            RoutingSolution.Route route = solution.routes().get(i);
            JSONObject routeJson = new JSONObject();
            routeJson.put("vehicleId", i);
            routeJson.put("startTime", route.startTime());
            routeJson.put("endTime", route.endTime());
            routeJson.put("monetaryCost", route.monetaryCost());
            routeJson.put("totalDuration", String.format("%d:%02d:%02d", route.totalDuration().toHoursPart(),
                    route.totalDuration().toMinutesPart(), route.totalDuration().toSecondsPart()));
            routeJson.put("totalDistance", route.totalDistance().kiloMeters());
            routeJson.put("totalWeightKg", route.totalWeight().kiloGrams());
            routeJson.put("totalNumberBoxes", route.totalNumberBoxes());

            JSONArray routeNodes = new JSONArray();
            for (int j = 0; j < route.routeNodes().size(); j++) {
                RouteNode routeNode = route.routeNodes().get(j);
                JSONObject routeNodeJson = new JSONObject();
                if (routeNode.isDepot()) {
                    routeNodeJson.put("type", "depot");
                    routeNodeJson.put("zip", routeNode.zip());
                    routeNodeJson.put("departureTime", route.departureTimes().get(j));
                } else {
                    routeNodeJson = parseOrder(routeNode.order());
                    routeNodeJson.put("type", "order");
                    routeNodeJson.put("departureTime", route.departureTimes().get(j));
                }
                routeNodes.put(routeNodeJson);
            }
            routeJson.put("routeNodes", routeNodes);

            routes.put(routeJson);
        }
        json.put("routes", routes);

        JSONArray droppedOrders = new JSONArray();
        for (Order order : solution.droppedOrders()) {
            droppedOrders.put(parseOrder(order));
        }
        json.put("droppedOrders", droppedOrders);

        return json;
    }

    private static JSONObject parseOrder(Order order) {
        JSONObject res = new JSONObject();
        res.put("id", order.id());
        res.put("zip", order.zip().toString());
        res.put("weightKg", order.weight().kiloGrams());
        res.put("hubDeliveryCost", order.totalHubDeliveryCost());
        res.put("directDeliveryCost", order.numberBoxes());
        res.put("timeWindowStart", order.timeWindow().start());
        res.put("timeWindowEnd", order.timeWindow().end());
        return res;
    }
}
