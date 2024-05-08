package ch.hefr.isc.shipping_optimization.routing;

import ch.hefr.isc.shipping_optimization.model.TimeWindow;
import ch.hefr.isc.shipping_optimization.model.utils.Weight;
import ch.hefr.isc.shipping_optimization.model.utils.Zip;

import java.time.Duration;

public record RoutingConfig (int numberVehicles, Zip depotZip, Duration deliveryExecutionTime, float hourlyVehicleCost,
                             int maxBoxesPerVehicle, Weight maxWeightPerVehicle, Duration solverTimeLimit) {
    private static final int DEFAULT_VEHICLE_NUMBER = 30;
    private static final Zip DEFAULT_DEPOT_ZIP = Zip.of("3000");
    private static final Duration DEFAULT_DELIVERY_EXECUTION_TIME = Duration.ofMinutes(10);
    private static final float DEFAULT_HOURLY_VEHICLE_COST = 200;
    private static final int DEFAULT_MAX_BOXES_PER_VEHICLE = 64;
    private static final Weight DEFAULT_MAX_WEIGHT_PER_VEHICLE = Weight.ofKiloGrams(1000);
    private static final Duration DEFAULT_SOLVER_TIME_LIMIT = Duration.ofSeconds(30);

    public static RoutingConfigBuilder builder() {
        return new RoutingConfigBuilder();
    }

    public static class RoutingConfigBuilder {
        private int numberVehicles = DEFAULT_VEHICLE_NUMBER;
        private Zip depotZip = DEFAULT_DEPOT_ZIP;
        private Duration deliveryExecutionTime = DEFAULT_DELIVERY_EXECUTION_TIME;
        private float hourlyVehicleCost = DEFAULT_HOURLY_VEHICLE_COST;
        private int maxBoxesPerVehicle = DEFAULT_MAX_BOXES_PER_VEHICLE;
        private Weight maxWeightPerVehicle = DEFAULT_MAX_WEIGHT_PER_VEHICLE;
        private Duration solverTimeLimit = DEFAULT_SOLVER_TIME_LIMIT;

        private RoutingConfigBuilder() {}

        public RoutingConfig build() {
            return new RoutingConfig(numberVehicles, depotZip, deliveryExecutionTime, hourlyVehicleCost,
                    maxBoxesPerVehicle, maxWeightPerVehicle, solverTimeLimit);
        }

        public RoutingConfigBuilder setNumberVehicles(int numberVehicles) {
            this.numberVehicles = numberVehicles;
            return this;
        }

        public RoutingConfigBuilder setDepotZip(Zip depotZip) {
            this.depotZip = depotZip;
            return this;
        }

        public RoutingConfigBuilder setDeliveryExecutionTime(Duration deliveryExecutionTime) {
            this.deliveryExecutionTime = deliveryExecutionTime;
            return this;
        }

        public RoutingConfigBuilder setHourlyVehicleCost(float hourlyVehicleCost) {
            this.hourlyVehicleCost = hourlyVehicleCost;
            return this;
        }

        public RoutingConfigBuilder setMaxBoxesPerVehicle(int maxBoxesPerVehicle) {
            this.maxBoxesPerVehicle = maxBoxesPerVehicle;
            return this;
        }

        public RoutingConfigBuilder setMaxWeightPerVehicle(Weight maxWeightPerVehicle) {
            this.maxWeightPerVehicle = maxWeightPerVehicle;
            return this;
        }

        public RoutingConfigBuilder setSolverTimeLimit(Duration solverTimeLimit) {
            this.solverTimeLimit = solverTimeLimit;
            return this;
        }
    }
}
