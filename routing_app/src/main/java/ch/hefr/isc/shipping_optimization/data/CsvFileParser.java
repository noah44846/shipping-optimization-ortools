package ch.hefr.isc.shipping_optimization.data;

import ch.hefr.isc.shipping_optimization.model.Order;
import ch.hefr.isc.shipping_optimization.model.TimeWindow;
import ch.hefr.isc.shipping_optimization.model.utils.Distance;
import ch.hefr.isc.shipping_optimization.model.utils.Pair;
import ch.hefr.isc.shipping_optimization.model.utils.Weight;
import ch.hefr.isc.shipping_optimization.model.utils.Zip;
import ch.hefr.isc.shipping_optimization.routing.DataModel;
import ch.hefr.isc.shipping_optimization.routing.RoutingConfig;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;

/**
 * A parser for the csv files.
 */
public class CsvFileParser {

    /**
     * Parse the data model from the csv files.
     *
     * @param csvConfig     the csv file config
     * @param routingConfig the routing config
     * @return the data model
     * @throws IOException if an error occurs while reading the files
     */
    public static DataModel parseDataModel(CsvFileConfig csvConfig, RoutingConfig routingConfig) throws IOException {
        Map<Zip, Pair<Distance, Duration>> locations = parseLocations(csvConfig.zipDistancesPath());

        DistanceMatrix distanceMatrix = parseDistanceMatrix(csvConfig.distanceMatrixPath(), locations);

        Map<String, Float> orderWeights = orderIdFloatMap(csvConfig.orderWeightsPath(), ";");
        Map<String, Float> unloadingSites = orderIdFloatMap(csvConfig.unloadingSitesPath(), ",");
        List<Order> orders = parseOrders(csvConfig.ordersPath(), orderWeights, unloadingSites);

        return new CsvFileDataModel(orders, distanceMatrix, routingConfig);
    }

    /**
     * Parse the locations from the locations file.
     *
     * @param path the path to the locations file
     * @return the map of zip code to location
     * @throws IOException if an error occurs while reading the file
     */
    private static Map<Zip, Pair<Distance, Duration>> parseLocations(String path) throws IOException {
        List<String> lines = Files.readAllLines(Path.of(path));
        lines.remove(0);

        Map<Zip, Pair<Distance, Duration>> locations = new HashMap<>();
        for (String line : lines) {
            String[] data = line.split(";");
            // the duration is a float representing hours it needs to be converted to seconds to
            // preserve precision
            long seconds = (long) (Float.parseFloat(data[2]) * 3600);
            Distance distance = Distance.ofKiloMeters(Double.parseDouble(data[1]));
            locations.put(Zip.of(data[0]), new Pair<>(distance, Duration.ofSeconds(seconds)));
        }
        return locations;
    }

    /**
     * Parse the distance matrix from the distance matrix file.
     *
     * @param path      the path to the distance matrix file
     * @param locations the map of zip code to location
     * @return the distance matrix
     * @throws IOException if an error occurs while reading the file
     */
    private static DistanceMatrix parseDistanceMatrix(String path, Map<Zip, Pair<Distance, Duration>> locations)
            throws IOException {
        List<String> lines = Files.readAllLines(Path.of(path));
        if (lines.size() != 1)
            throw new IOException("Distance matrix file should contain only one line");

        JSONObject data = new JSONObject(lines.get(0));
        DistanceMatrix distanceMatrix = new DistanceMatrix();
        for (String from : data.keySet()) {
            JSONObject fromData = data.getJSONObject(from);

            // ignore invalid zip codes
            if (Zip.isInvalid(from)) continue;
            Zip fromZip = Zip.of(from);

            // set distance to self to the inter zip distance and duration
            if (!locations.containsKey(fromZip)) continue;
            Pair<Distance, Duration> location = locations.get(fromZip);
            distanceMatrix.setZipData(fromZip, fromZip, location.first(), location.second());

            for (String to : fromData.keySet()) {
                JSONObject toData = fromData.getJSONObject(to);

                if (Zip.isInvalid(to)) continue;
                Zip toZip = Zip.of(to);

                // ignore not needed zip codes
                if (!locations.containsKey(fromZip)) continue;

                // duration is a float representing minutes it needs to be converted to seconds
                // to preserve precision
                long seconds = (long) (toData.getDouble("duration") * 60);
                Distance distance = Distance.ofKiloMeters(toData.getDouble("length"));
                distanceMatrix.setZipData(fromZip, toZip, distance, Duration.ofSeconds(seconds));
            }
        }
        return distanceMatrix;
    }

    /**
     * Parse the orders from the orders file.
     *
     * @param ordersPath     the path to the orders file
     * @param orderWeights   the map of order id to order weight
     * @param unloadingSites the map of order id to unloading site
     * @return the list of orders
     * @throws IOException if an error occurs while reading the file
     */
    private static List<Order> parseOrders(String ordersPath, Map<String, Float> orderWeights,
                                           Map<String, Float> unloadingSites) throws IOException {
        List<String> lines = Files.readAllLines(Path.of(ordersPath));
        lines.remove(0);

        List<Order> orders = new ArrayList<>();
        for (String line : lines) {
            String[] data = line.split(",");

            TimeWindow timeWindow = new TimeWindow(LocalTime.parse(data[3]), LocalTime.parse(data[4]));

            if (Zip.isInvalid(data[2]))
                throw new IOException("Invalid zip code " + data[2] + " in orders file");

            if (!orderWeights.containsKey(data[0]))
                throw new IOException("Order " + data[0] + " not found in order weights file");

            if (!unloadingSites.containsKey(data[5]))
                throw new IOException("Order " + data[0] + " not found in unloading sites file");

            orders.add(new Order(data[0], Zip.of(data[2]), Integer.parseInt(data[1]), timeWindow,
                    Weight.ofKiloGrams(orderWeights.get(data[0])), unloadingSites.get(data[5])));
        }

        return orders;
    }

    /**
     * Parse a file containing a list of order ids and some float value separated by a separator.
     *
     * @param path      the path to the file
     * @param separator the separator between the order id and the weight
     * @return the map of order id to float value
     * @throws IOException if an error occurs while reading the file
     */
    private static Map<String, Float> orderIdFloatMap(String path, String separator) throws IOException {
        List<String> lines = Files.readAllLines(Path.of(path));
        lines.remove(0);

        Map<String, Float> zipFloatMap = new HashMap<>();
        for (String line : lines) {
            String[] data = line.split(separator);
            zipFloatMap.put(data[0], Float.parseFloat(data[1]));
        }
        return zipFloatMap;
    }
}
