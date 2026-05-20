package mx.mauricio.lorawan.web;

import static spark.Spark.before;
import static spark.Spark.post;

import mx.mauricio.lorawan.simulator.SimulationRunner;
import mx.mauricio.lorawan.simulator.dto.SimulationRequest;
import mx.mauricio.lorawan.simulator.dto.SimulationResult;

public class SimulationController {

    public void registerRoutes() {
        before("/*", (request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
            response.header("Access-Control-Allow-Headers", "Content-Type,Authorization");
        });

        post("/run", (request, response) -> {
            response.type("application/json");

            SimulationRequest simulationRequest =
                    JsonUtil.fromJson(request.body(), SimulationRequest.class);

            SimulationRunner runner = new SimulationRunner();
            SimulationResult result = runner.run(simulationRequest);

            return JsonUtil.toJson(result);
        });
    }
}