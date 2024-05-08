package ch.hefr.isc.shipping_optimization.data;

import ch.hefr.isc.shipping_optimization.model.utils.Distance;
import ch.hefr.isc.shipping_optimization.model.utils.Pair;
import ch.hefr.isc.shipping_optimization.model.utils.Zip;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class DistanceMatrix {

    // TODO : use a better default value -> compute form routeNodes and van capacity and hourly cost
    private static final Pair<Distance, Duration> UNREACHABLE_ZIP_DATA = Pair.of(Distance.ofKiloMeters(1e5),
            Duration.ofSeconds(1_000_000));
    private final Map<Zip, Map<Zip, Pair<Distance, Duration>>> adjacencyMap = new HashMap<>();

    /**
     * Set the distance and duration between two zip codes.
     *
     * @param from     the origin zip code
     * @param to       the destination zip code
     * @param distance the distance between the two zip codes
     * @param duration the duration between the two zip codes
     */
    public void setZipData(Zip from, Zip to, Distance distance, Duration duration) {
        adjacencyMap
                .computeIfAbsent(from, val -> new HashMap<>())
                .put(to, Pair.of(distance, duration));
    }

    public Distance distance(Zip from, Zip to) {
        return zipData(from, to).first();
    }

    public Duration duration(Zip from, Zip to) {
        return zipData(from, to).second();
    }

    private Pair<Distance, Duration> zipData(Zip from, Zip to) {
        Map<Zip, Pair<Distance, Duration>> fromMap = adjacencyMap.get(from);
        if (fromMap == null) return UNREACHABLE_ZIP_DATA;
        Pair<Distance, Duration> zipData = fromMap.get(to);
        if (zipData == null) return UNREACHABLE_ZIP_DATA;
        return zipData;
    }
}
