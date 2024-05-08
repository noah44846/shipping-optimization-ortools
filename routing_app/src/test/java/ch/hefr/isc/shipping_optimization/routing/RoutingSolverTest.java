package ch.hefr.isc.shipping_optimization.routing;

import ch.hefr.isc.shipping_optimization.data.*;
import ch.hefr.isc.shipping_optimization.model.Order;
import ch.hefr.isc.shipping_optimization.model.RouteNode;
import ch.hefr.isc.shipping_optimization.model.RoutingSolution;
import ch.hefr.isc.shipping_optimization.model.TimeWindow;
import ch.hefr.isc.shipping_optimization.model.utils.Distance;
import ch.hefr.isc.shipping_optimization.model.utils.Weight;
import ch.hefr.isc.shipping_optimization.model.utils.Zip;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RoutingSolverTest {

    private static void simpleSolutionTests(DataModel dataModel, RoutingSolution solution) {
        // TODO: add test on empty routes -> is distance and time 0?
        Assertions.assertEquals(dataModel.numberRouteNodes() - 1,
                solution.numberOfDirectDeliveredOrders() + solution.droppedOrders().size());
        Assertions.assertTrue(solution.totalCostWithoutDirectDelivery() > solution.totalMonetaryCost());

        for (RoutingSolution.Route route : solution.routes()) {
            checkRoutesMakeProfit(dataModel, route);
            checkVolumeAndWeight(dataModel, route);
            checkTimeWindows(route);
        }
    }

    private static void checkRoutesMakeProfit(DataModel dataModel, RoutingSolution.Route route) {
        long expectedDurationMinutes = 0;
        long totalHubDeliveryCents = 0;
        for (int i = 0; i < route.numberOfStops() - 1; ++i) {
            if (route.numberOfStops() < 2) break;
            RouteNode from = route.routeNodes().get(i);
            RouteNode to = route.routeNodes().get(i + 1);

            expectedDurationMinutes += Math.round(dataModel.duration(from, to).toSeconds() / 60.0);
            if (!to.isDepot()) {
                totalHubDeliveryCents += Math.round(to.order().totalHubDeliveryCost() * 100);
                expectedDurationMinutes += Math.round(dataModel.config().deliveryExecutionTime().toSeconds() / 60.0);
            }
        }
        Duration expectedDuration = Duration.ofMinutes(expectedDurationMinutes);
        Assertions.assertEquals(expectedDuration, route.totalDuration());

        long centsPerSecond = Math.round(dataModel.config().hourlyVehicleCost() * 100 / 3600);
        long expectedCost = expectedDuration.toSeconds() * centsPerSecond;
        Assertions.assertTrue(expectedCost <= totalHubDeliveryCents);
    }

    private static void checkVolumeAndWeight(DataModel dataModel, RoutingSolution.Route route) {
        int totalBoxes = 0;
        long totalWeightGrams = 0;
        for (int i = 0; i < route.numberOfStops(); ++i) {
            RouteNode node = route.routeNodes().get(i);
            if (node.isDepot()) continue;
            totalBoxes += node.order().numberBoxes();
            totalWeightGrams += Math.round(node.order().weight().grams());
        }
        Assertions.assertTrue(totalBoxes <= dataModel.config().maxBoxesPerVehicle());
        Assertions.assertTrue(totalWeightGrams <= dataModel.config().maxWeightPerVehicle().grams());
    }

    private static void checkTimeWindows(RoutingSolution.Route route) {
        for (int i = 0; i < route.numberOfStops(); ++i) {
            RouteNode node = route.routeNodes().get(i);
            if (node.isDepot()) continue;
            LocalTime departureTime = route.departureTimes().get(i);
            Assertions.assertTrue(node.order().timeWindow().contains(departureTime));
        }
    }

    /**
     * Test that the solution is correct for a simple model.
     */
    @Test
    public void testSimpleModelSolve() {
        final RoutingConfig config = RoutingConfig.builder()
                .setNumberVehicles(2)
                .setDepotZip(Zip.of("1000"))
                .setDeliveryExecutionTime(Duration.ofMinutes(10))
                .setHourlyVehicleCost(10)
                .setMaxBoxesPerVehicle(10)
                .setMaxWeightPerVehicle(Weight.ofKiloGrams(10))
                .build();
        final int NUM_ORDERS = 7;
        final float HUB_DELIVERY_COST = 10;

        List<Zip> zips = IntStream.range(1, NUM_ORDERS + 2)
                .mapToObj(i -> Zip.of(String.format("%d000", i)))
                .toList();

        Distance oneKm = Distance.ofKiloMeters(1);
        Weight oneKg = Weight.ofKiloGrams(1);
        TimeWindow timeWindow = new TimeWindow(LocalTime.of(8, 0), LocalTime.of(18, 0));
        List<Order> orders = IntStream.range(0, NUM_ORDERS - 2)
                .mapToObj(i -> new Order(String.valueOf(i), zips.get(i + 1), 1, timeWindow, oneKg,
                        config.hourlyVehicleCost()))
                .collect(Collectors.toCollection(ArrayList::new));
        orders.add(new Order(String.valueOf(NUM_ORDERS - 2), zips.get(NUM_ORDERS - 1), 1, timeWindow,
                Weight.ofKiloGrams(11), HUB_DELIVERY_COST));
        orders.add(new Order(String.valueOf(NUM_ORDERS - 1), zips.get(NUM_ORDERS), 11, timeWindow, oneKg,
                HUB_DELIVERY_COST));

        DistanceMatrix distanceMatrix = new DistanceMatrix();
        // only set certain distances
        distanceMatrix.setZipData(zips.get(0), zips.get(1), oneKm, Duration.ofMinutes(15));
        distanceMatrix.setZipData(zips.get(0), zips.get(2), oneKm, Duration.ofMinutes(20));
        distanceMatrix.setZipData(zips.get(1), zips.get(3), oneKm, Duration.ofMinutes(20));
        distanceMatrix.setZipData(zips.get(2), zips.get(4), oneKm, Duration.ofMinutes(25));
        distanceMatrix.setZipData(zips.get(3), zips.get(0), oneKm, Duration.ofMinutes(25));
        distanceMatrix.setZipData(zips.get(4), zips.get(0), oneKm, Duration.ofMinutes(30));
        // add a way to reach 8000 and 9000
//        distanceMatrix.setZipData(zips.get(3), zips.get(6), oneKm, Duration.ofMinutes(1));
//        distanceMatrix.setZipData(zips.get(4), zips.get(7), oneKm, Duration.ofMinutes(1));
//        distanceMatrix.setZipData(zips.get(6), zips.get(0), oneKm, Duration.ofMinutes(1));
//        distanceMatrix.setZipData(zips.get(7), zips.get(0), oneKm, Duration.ofMinutes(1));
        // make the zip 7000 too far for it to be worth it to deliver directly
        distanceMatrix.setZipData(zips.get(0), zips.get(5), oneKm, Duration.ofHours(1));
        distanceMatrix.setZipData(zips.get(5), zips.get(3), oneKm, Duration.ofHours(1));

        DataModel dataModel = new CsvFileDataModel(orders, distanceMatrix, config);

        RoutingSolver solver = new RoutingSolver(dataModel);
        solver.solve();
        Assertions.assertTrue(solver.wasRun() && solver.hasSolution());
        RoutingSolution solution = solver.solution();

        simpleSolutionTests(dataModel, solution);

        RouteNode depotNode = RouteNode.depotNode(Zip.of("1000"));
        Set<List<RouteNode>> expectedRoutes = new HashSet<>(List.of(
                new ArrayList<>(List.of(
                        depotNode, RouteNode.orderNode(orders.get(0)), RouteNode.orderNode(orders.get(2)), depotNode)),
                new ArrayList<>(List.of(
                        depotNode, RouteNode.orderNode(orders.get(1)), RouteNode.orderNode(orders.get(3)), depotNode))
        ));

        // check that the routes are correct
        Set<List<RouteNode>> expectedRoutesCopy = new HashSet<>(expectedRoutes);
        Assertions.assertEquals(dataModel.config().numberVehicles(), solution.routes().size());
        for (RoutingSolution.Route route : solution.routes()) {
            // transform into ArrayList so that in can be compared with the expected routes
            List<RouteNode> actualRoute = new ArrayList<>(route.routeNodes());
            // returns a boolean if the element was removed (/ if it was in the set)
            Assertions.assertTrue(expectedRoutesCopy.remove(actualRoute));
        }

        // check the profit calculations
        double expectedCostWithoutDirectDelivery = orders.stream()
                .mapToDouble(Order::totalHubDeliveryCost)
                .sum();
        Assertions.assertEquals(expectedCostWithoutDirectDelivery, solution.totalCostWithoutDirectDelivery());

        Duration expectedRoutesDuration = Duration.ZERO;
        for (List<RouteNode> route : expectedRoutes) {
            for (int i = 0; i < route.size() - 1; ++i) {
                expectedRoutesDuration = expectedRoutesDuration
                        .plus(distanceMatrix.duration(route.get(i).zip(), route.get(i + 1).zip()));
                if (i != route.size() - 2)
                    expectedRoutesDuration = expectedRoutesDuration.plus(dataModel.config().deliveryExecutionTime());
            }
        }
        long expectedRoutesCost = Math.round(expectedRoutesDuration.toMinutes() * dataModel.config().hourlyVehicleCost() / 60);
        double expectedDroppedOrdersCost = orders.get(4).totalHubDeliveryCost() + orders.get(5).totalHubDeliveryCost()
                + orders.get(6).totalHubDeliveryCost();
        double expectedTotalCost = expectedRoutesCost + expectedDroppedOrdersCost;
        Assertions.assertEquals(expectedTotalCost, solution.totalMonetaryCost());
        Assertions.assertEquals(expectedCostWithoutDirectDelivery, solution.totalCostWithoutDirectDelivery());
    }

    /**
     * Test that the solution is reasonable for a reduced dataset.
     */
    @Test
    public void testSolveSmallDataset() {
        DataModel dataModel = null;
        try {
            dataModel = CsvFileParser.parseDataModel(CsvFileConfig.DEFAULT_CONFIG, RoutingConfig.builder().build());
        } catch (IOException e) {
            Assertions.fail(e);
        }

        RoutingSolver solver = new RoutingSolver(dataModel);
        solver.solve();
        Assertions.assertTrue(solver.wasRun() && solver.hasSolution());
        RoutingSolution solution = solver.solution();

        simpleSolutionTests(dataModel, solution);
    }
}
