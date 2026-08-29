import com.ckgame.game.ScenarioLoader;
import com.ckgame.ui.GameAPI;

public class SmokeScenario {
    public static void main(String[] args) {
        for (ScenarioLoader.ScenarioInfo s : ScenarioLoader.listScenarios()) {
            System.out.println("scenario: " + s.id + " / " + s.name + " / " + s.description);
        }
        GameAPI api = new GameAPI("1066");
        System.out.println("api scenario=" + api.simulation().scenarioId);
        System.out.println("SMOKE OK");
    }
}
