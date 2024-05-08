package ch.hefr.isc.shipping_optimization.model;

import ch.hefr.isc.shipping_optimization.model.utils.Distance;
import ch.hefr.isc.shipping_optimization.model.utils.Weight;
import ch.hefr.isc.shipping_optimization.model.utils.Zip;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A solution to the routing problem represented by dataModel.
 *
 * @param routes        the routes for each vehicle
 * @param droppedOrders the routeNodes that were dropped
 */
public record RoutingSolution(List<Route> routes, Set<Order> droppedOrders) {
    public RoutingSolution(List<Route> routes, Set<Order> droppedOrders) {
        this.routes = Collections.unmodifiableList(routes);
        this.droppedOrders = Collections.unmodifiableSet(droppedOrders);
    }

    public double totalCostWithoutDirectDelivery() {
        double routeHubCosts = routes.stream()
                .flatMap(route -> route.routeNodes().stream())
                .filter(Predicate.not(RouteNode::isDepot))
                .mapToDouble(node -> node.order().totalHubDeliveryCost())
                .sum();
        double droppedHubCosts = droppedOrders.stream()
                .mapToDouble(Order::totalHubDeliveryCost)
                .sum();
        return routeHubCosts + droppedHubCosts;
    }

    /**
     * Get total monetary cost of the solution.
     *
     * @return the total monetary cost
     */
    public double totalMonetaryCost() {
        long routeCosts = routes.stream()
                .mapToLong(Route::monetaryCost)
                .sum();
        double droppedHubCosts = droppedOrders.stream()
                .mapToDouble(Order::totalHubDeliveryCost)
                .sum();
        return routeCosts + droppedHubCosts;
    }

    /**
     * Get the total distance of the solution.
     *
     * @return the total distance
     */
    public LocalTime lastRouteEndTime() {
        return routes.stream()
                .map(Route::endTime)
                .max(LocalTime::compareTo)
                .orElse(LocalTime.of(0, 0));
    }

    /**
     * Get the total distance of the solution.
     *
     * @return the total distance
     */
    public Distance totalDistance() {
        return routes.stream()
                .map(Route::totalDistance)
                .reduce(
                        Distance.ofKiloMeters(0),
                        (a, b) -> Distance.ofKiloMeters(a.kiloMeters() + b.kiloMeters())
                );
    }

    /**
     * Get the number of direct delivered routeNodes.
     *
     * @return the number of direct delivered routeNodes
     */
    public int numberOfDirectDeliveredOrders() {
        return routes.stream()
                .filter(route -> route.numberOfStops() > 2)
                .mapToInt(route -> route.numberOfStops() - 2)
                .sum();
    }

    /**
     * Build a string representation of the solution.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        int numUnusedVehicles = 0;
        for (int i = 0; i < routes.size(); ++i) {
            builder.append(String.format("Vehicle %d: ", i));

            if (routes.get(i).numberOfStops() == 2) {
                numUnusedVehicles++;
                builder.append("not used");
            }
            else builder.append(routes.get(i));
            builder.append("\n");
        }

        builder.append(String.format("Total monetary cost: %.2f%n", totalMonetaryCost()))
                .append(String.format("No direct delivery cost: %.2f%n", totalCostWithoutDirectDelivery()))
                .append(String.format("Profit: %.2f%n", totalCostWithoutDirectDelivery() - totalMonetaryCost()))
                .append(String.format("Last route end time: %s%n", lastRouteEndTime()))
                .append(String.format("Total distance: %.2fkm%n", totalDistance().kiloMeters()))
                .append(String.format("%d / %d (#routeNodes over total)%n", numberOfDirectDeliveredOrders(),
                        numberOfDirectDeliveredOrders() + droppedOrders.size()))
                .append(String.format("%d / %d vans used", routes.size() - numUnusedVehicles, routes.size()));

        return builder.toString();
    }

    /**
     * A route for a given vehicle.
     *
     * @param routeNodes        the zip codes of the route
     * @param departureTimes    the departure times of the route
     * @param startTime         the start time of the route
     * @param endTime           the end time of the route
     * @param monetaryCost      the total hub delivery cost of the route
     * @param totalDistance     the total distance of the route
     * @param depotZip          the depot zip code
     */
    public record Route(List<RouteNode> routeNodes, List<LocalTime> departureTimes, LocalTime startTime,
                        LocalTime endTime, long monetaryCost, Distance totalDistance, Zip depotZip) {
        public Route(List<RouteNode> routeNodes, List<LocalTime> departureTimes, LocalTime startTime, LocalTime endTime, long monetaryCost,
                     Distance totalDistance, Zip depotZip) {
            this.routeNodes = Collections.unmodifiableList(routeNodes);
            this.departureTimes = Collections.unmodifiableList(departureTimes);
            this.startTime = startTime;
            this.endTime = endTime;
            this.monetaryCost = monetaryCost;
            this.totalDistance = totalDistance;
            this.depotZip = depotZip;
        }

        public Duration totalDuration() {
            return Duration.between(startTime, endTime);
        }

        /**
         * Get the length of the route.
         *
         * @return the length
         */
        public int numberOfStops() {
            return routeNodes.size();
        }

        /**
         * Get the total number of boxes of the route.
         *
         * @return the total number of boxes
         */
        public int totalNumberBoxes() {
            return routeNodes.stream()
                    .filter(Predicate.not(RouteNode::isDepot))
                    .mapToInt(node -> node.order().numberBoxes())
                    .sum();
        }

        /**
         * Get the total weight of the route.
         *
         * @return the total weight
         */
        public Weight totalWeight() {
            return Weight.ofKiloGrams(routeNodes.stream()
                    .filter(Predicate.not(RouteNode::isDepot))
                    .mapToDouble(node -> node.order().weight().kiloGrams())
                    .sum());
        }

        /**
         * Build a string representation of the route.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            for (RouteNode node : routeNodes)
                builder.append(node.zip()).append(" -> ");

            builder.delete(builder.length() - 4, builder.length())
                    .append(String.format(" %s - %s", startTime, endTime))
                    .append(String.format(", duration: (%d:%02d:%02d)", totalDuration().toHoursPart(),
                            totalDuration().toMinutesPart(), totalDuration().toSecondsPart()))
                    .append(String.format(", total weight: %.2fKg", totalWeight().kiloGrams()))
                    .append(String.format(", distance: %.2fkm", totalDistance.kiloMeters()))
                    .append(String.format(", monetary cost: %d", monetaryCost));

            return builder.toString();
        }
    }
}
