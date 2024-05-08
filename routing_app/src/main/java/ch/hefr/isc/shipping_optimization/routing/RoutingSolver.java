package ch.hefr.isc.shipping_optimization.routing;

import ch.hefr.isc.shipping_optimization.model.Order;
import ch.hefr.isc.shipping_optimization.model.RouteNode;
import ch.hefr.isc.shipping_optimization.model.RoutingSolution;
import ch.hefr.isc.shipping_optimization.model.utils.Distance;
import com.google.ortools.Loader;
import com.google.ortools.constraintsolver.*;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.LongBinaryOperator;
import java.util.logging.Logger;

public class RoutingSolver {

    private static final Logger logger = Logger.getLogger(RoutingSolver.class.getName());
    private final DataModel data;
    private RoutingModel routing;
    private RoutingIndexManager manager;
    private Assignment solution;

    public RoutingSolver(DataModel data) {
        this.data = data;
    }

    public boolean wasRun() {
        return routing != null;
    }

    public boolean hasSolution() {
        return solution != null;
    }

    // TODO : clean up the magic numbers
    public void solve() {
        logger.info("routing init");
        Loader.loadNativeLibraries();

        // Create Routing Index Manager
        this.manager = new RoutingIndexManager(data.numberRouteNodes(), data.config().numberVehicles(), 0);

        // Create Routing Model
        this.routing = new RoutingModel(manager);

        addDistanceConstraint();

        LongBinaryOperator timeCallback = addTimeWindowConstraint();

        addCapacityConstraint();

        addHubCostDeliveryCostConstraint();

        addMonetaryConstraint(timeCallback);

        // Setting first solution heuristic.
        RoutingSearchParameters searchParameters = main.defaultRoutingSearchParameters()
                .toBuilder()
                .setFirstSolutionStrategy(FirstSolutionStrategy.Value.PATH_CHEAPEST_ARC)
                .setLocalSearchMetaheuristic(LocalSearchMetaheuristic.Value.GUIDED_LOCAL_SEARCH)
                .setTimeLimit(com.google.protobuf.Duration.newBuilder()
                        .setSeconds(data.config().solverTimeLimit().getSeconds()).build())
                .build();

        // Solve the problem.
        logger.info("routing solver start");
        this.solution = routing.solveWithParameters(searchParameters);
        logger.info("routing solver end");
    }

    private LongBinaryOperator addTimeWindowConstraint() {
        // Add Time dimension.
        LongBinaryOperator timeCallback = (long fromIndex, long toIndex) -> {
            RouteNode from = data.routeNodeAt(manager.indexToNode(fromIndex));
            RouteNode to = data.routeNodeAt(manager.indexToNode(toIndex));

            Duration duration = data.duration(from, to);

            if (!to.isDepot())
                duration = duration.plus(data.config().deliveryExecutionTime());

            return Math.round(duration.toSeconds() / 60.0);
        };
        final int timeCallbackIndex = routing.registerTransitCallback(timeCallback);

        routing.addDimension(timeCallbackIndex, 0,  // no slack
                24 * 60,                            // no max time per vehicle (set to 24 hours)
                false,                              // start cumul to zero
                "Time");

        RoutingDimension timeDimension = routing.getMutableDimension("Time");
        // Add time window constraints for each location except depot.
        for (int i = 1; i < data.numberRouteNodes(); ++i) {
            long index = manager.nodeToIndex(i);
            Order order = data.routeNodeAt(i).order();
            timeDimension.cumulVar(index).setRange(order.timeWindow().startAsMinutes(),
                    order.timeWindow().endAsMinutes());
        }

//        // Instantiate route start and end times to produce feasible times.
//        for (int i = 0; i < data.config().numberVehicles(); ++i) {
//            routing.addVariableMinimizedByFinalizer(timeDimension.cumulVar(routing.start(i)));
//            routing.addVariableMinimizedByFinalizer(timeDimension.cumulVar(routing.end(i)));
//        }
        return timeCallback;
    }

    private void addDistanceConstraint() {
        final int distanceCallbackIndex = routing.registerTransitCallback((long fromIndex, long toIndex) -> {
            Distance distance = data.distance(
                    data.routeNodeAt(manager.indexToNode(fromIndex)),
                    data.routeNodeAt(manager.indexToNode(toIndex)));
            return Math.round(distance.meters());
        });
        routing.addDimension(distanceCallbackIndex, 0,  // no slack
                1_000_000,                              // max distance per vehicle is not very important for now
                true,                                   // start cumul to zero
                "Distance");
    }

    private void addCapacityConstraint() {
        // Add boxes dimension.
        final int boxesCallbackIndex = routing.registerUnaryTransitCallback((long index) -> {
            RouteNode node = data.routeNodeAt(manager.indexToNode(index));
            if (node.isDepot()) return 0;
            return node.order().numberBoxes();
        });
        routing.addDimension(boxesCallbackIndex, 0, // no slack
                data.config().maxBoxesPerVehicle(), // maximum boxes for all vehicles
                true,                               // start cumul to zero
                "Boxes");

        // Add weight dimension.
        final int weightCallbackIndex = routing.registerUnaryTransitCallback((long index) -> {
            RouteNode node = data.routeNodeAt(manager.indexToNode(index));
            if (node.isDepot()) return 0;
            return Math.round(node.order().weight().grams());
        });
        routing.addDimension(weightCallbackIndex, 0,                        // no slack
                Math.round(data.config().maxWeightPerVehicle().grams()),    // maximum weight for all vehicles
                true,                                                       // start cumul to zero
                "Weight");
    }

    private void addHubCostDeliveryCostConstraint() {
        // add hub delivery cost disjunction
        for (int i = 1; i < data.numberRouteNodes(); ++i) {
            // convert hub delivery cost to time in minutes because the arc cost evaluator is in minutes
            long hubDeliveryCost = Math.round(data.routeNodeAt(i).order().totalHubDeliveryCost());
            routing.addDisjunction(new long[]{manager.nodeToIndex(i)}, hubDeliveryCost);
        }
    }

    private void addMonetaryConstraint(LongBinaryOperator timeCallback) {
        // Use the monetary cost as arc cost evaluator for all vehicles.
        final int monetaryCallbackIndex = routing.registerTransitCallback((long fromIndex, long toIndex) -> {
            long durationInMinutes = timeCallback.applyAsLong(fromIndex, toIndex);
            return Math.round(data.config().hourlyVehicleCost() * durationInMinutes / 60);
        });
        routing.setArcCostEvaluatorOfAllVehicles(monetaryCallbackIndex);

        // Add monetary cost dimension.
        routing.addDimension(monetaryCallbackIndex, 0,              // no slack
                Math.round(8 * data.config().hourlyVehicleCost()),  // represents the travel cost for 8 hours
                true,                                               // start cumul to zero
                "MonetaryCost");
    }

    public RoutingSolution solution() throws IllegalStateException {
        if (!wasRun()) throw new IllegalStateException("Routing wasn't executed.");
        if (!hasSolution()) throw new IllegalStateException("No solution found.");

        RoutingDimension timeDimension = routing.getMutableDimension("Time");
        RoutingDimension distanceDimension = routing.getMutableDimension("Distance");

        List<RoutingSolution.Route> routes = new ArrayList<>();
        for (int i = 0; i < data.config().numberVehicles(); ++i) {
            List<RouteNode> route = new ArrayList<>();
            List<LocalTime> departureTimes = new ArrayList<>();
            long totalMonetaryCost = 0;
            long index = routing.start(i);
            while (!routing.isEnd(index)) {
                route.add(data.routeNodeAt(manager.indexToNode(index)));
                departureTimes.add(LocalTime.ofSecondOfDay(solution.value(timeDimension.cumulVar(index)) * 60));
                long previousIndex = index;
                index = solution.value(routing.nextVar(index));
                totalMonetaryCost += routing.getArcCostForVehicle(previousIndex, index, i);
            }
            route.add(data.routeNodeAt(manager.indexToNode(index)));
            departureTimes.add(LocalTime.ofSecondOfDay(solution.value(timeDimension.cumulVar(index)) * 60));
            long monetaryCost = totalMonetaryCost;

            LocalTime startTime = LocalTime.ofSecondOfDay(solution.value(timeDimension.cumulVar(routing.start(i))) * 60);
            LocalTime endTime = LocalTime.ofSecondOfDay(solution.value(timeDimension.cumulVar(index)) * 60);

            Distance totalDistance = Distance.ofMeters(solution.value(distanceDimension.cumulVar(index)));
            routes.add(new RoutingSolution.Route(route, departureTimes, startTime, endTime, monetaryCost,
                    totalDistance, data.config().depotZip()));
        }

        Set<Order> droppedOrders = new HashSet<>();
        for (int i = 1; i < routing.nodes(); ++i) {
            RouteNode routeNode = data.routeNodeAt(manager.indexToNode(i));
            if (routeNode.isDepot()) continue;
            if (solution.value(routing.activeVar(i)) == 0) droppedOrders.add(routeNode.order());
        }

        return new RoutingSolution(routes, droppedOrders);
    }
}
