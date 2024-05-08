package ch.hefr.isc.shipping_optimization;

import ch.hefr.isc.shipping_optimization.data.CsvFileConfig;
import ch.hefr.isc.shipping_optimization.data.CsvFileParser;
import ch.hefr.isc.shipping_optimization.data.RoutingSolutionToJson;
import ch.hefr.isc.shipping_optimization.model.RoutingSolution;
import ch.hefr.isc.shipping_optimization.routing.DataModel;
import ch.hefr.isc.shipping_optimization.routing.RoutingConfig;
import ch.hefr.isc.shipping_optimization.routing.RoutingSolver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Routing app entry point
 */
public class App {

    public static final RoutingConfig DEFAULT_CONFIG_FULL = RoutingConfig.builder()
            .setSolverTimeLimit(Duration.ofMinutes(5))
            .build();
    // public static final RoutingConfig DEFAULT_CONFIG = RoutingConfig.builder().setSolverTimeLimit(Duration.ofMinutes(5)).build();
    public static final RoutingConfig DEFAULT_CONFIG = RoutingConfig.builder().build();

    public static void main(String[] args) throws IOException {
        DataModel dataModel = CsvFileParser.parseDataModel(CsvFileConfig.DEFAULT_CONFIG_FULL, DEFAULT_CONFIG_FULL);
        // DataModel dataModel = CsvFileParser.parseDataModel(CsvFileConfig.DEFAULT_CONFIG, DEFAULT_CONFIG);
        RoutingSolver solver = new RoutingSolver(dataModel);
        solver.solve();
        RoutingSolution solution = solver.solution();
        System.out.println(solution);
        // Files.writeString(Path.of("solution.json"), RoutingSolutionToJson.parseSolution(solution).toString(4));
    }
}
