package ch.hefr.isc.shipping_optimization.data;

/**
 * Configuration for the data files.
 */
public record CsvFileConfig(String zipDistancesPath, String ordersPath, String orderWeightsPath,
                            String unloadingSitesPath, String distanceMatrixPath) {
    private static final String DEFAULT_DATA_PATH = "../data/";
    private static final String DEFAULT_DISTANCE_MATRIX_JSON = DEFAULT_DATA_PATH +
            "distance_matrix_switzerland.json";
    private static final String DEFAULT_ORDER_DATA_PATH = DEFAULT_DATA_PATH + "order_data/";
    private static final String DEFAULT_LOCATION_DATA_CSV = DEFAULT_ORDER_DATA_PATH +
            "distances_clear_NPA.csv";
    private static final String DEFAULT_ORDER_DATA_CSV = DEFAULT_ORDER_DATA_PATH +
            "Orders_clear_NPA.csv";
    private static final String DEFAULT_ORDER_DATA_SMALL_CSV = DEFAULT_ORDER_DATA_PATH +
            "Orders_clear_NPA_small.csv";
    private static final String DEFAULT_ORDER_WEIGHTS_CSV = DEFAULT_ORDER_DATA_PATH +
            "OrderWeights.csv";
    private static final String DEFAULT_UNLOADING_SITES = DEFAULT_ORDER_DATA_PATH +
            "UnloadingSites.csv";

    public static final CsvFileConfig DEFAULT_CONFIG_FULL = CsvFileConfig.builder().build();

    public static final CsvFileConfig DEFAULT_CONFIG = CsvFileConfig.builder()
            .setOrdersPath(DEFAULT_ORDER_DATA_SMALL_CSV)
            .build();

    public static DataFileConfigBuilder builder() {
        return new DataFileConfigBuilder();
    }

    public static class DataFileConfigBuilder {
        private String zipDistancesPath = DEFAULT_LOCATION_DATA_CSV;
        private String ordersPath = DEFAULT_ORDER_DATA_CSV;
        private String orderWeightsPath = DEFAULT_ORDER_WEIGHTS_CSV;
        private String unloadingSitesPath = DEFAULT_UNLOADING_SITES;
        private String distanceMatrixPath = DEFAULT_DISTANCE_MATRIX_JSON;

        private DataFileConfigBuilder() {}

        public CsvFileConfig build() {
            return new CsvFileConfig(zipDistancesPath, ordersPath, orderWeightsPath, unloadingSitesPath,
                    distanceMatrixPath);
        }

        public DataFileConfigBuilder setZipDistancesPath(String zipDistancesPath) {
            this.zipDistancesPath = zipDistancesPath;
            return this;
        }

        public DataFileConfigBuilder setOrdersPath(String ordersPath) {
            this.ordersPath = ordersPath;
            return this;
        }

        public DataFileConfigBuilder setOrderWeightsPath(String orderWeightsPath) {
            this.orderWeightsPath = orderWeightsPath;
            return this;
        }

        public DataFileConfigBuilder setUnloadingSitesPath(String unloadingSitesPath) {
            this.unloadingSitesPath = unloadingSitesPath;
            return this;
        }

        public DataFileConfigBuilder setDistanceMatrixPath(String distanceMatrixPath) {
            this.distanceMatrixPath = distanceMatrixPath;
            return this;
        }
    }
}
