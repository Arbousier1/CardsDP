import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class DatapackParseBench {
    private static volatile int blackhole;

    public static void main(String[] args) throws Exception {
        Path root = Path.of("data", "dqc.cards");
        List<Path> recipeDeck = list(root.resolve("recipe").resolve("deck"));
        List<Path> recipeTable = list(root.resolve("recipe").resolve("table"));
        List<Path> recipeJoker = list(root.resolve("recipe").resolve("joker"));
        Path redLoot = root.resolve("loot_table").resolve("joker_red.json");
        Path blueLoot = root.resolve("loot_table").resolve("joker_blue.json");

        int warmup = 100;
        int iterations = 1000;

        for (int i = 0; i < warmup; i++) {
            blackhole ^= parseEquivalentLoad(recipeDeck, recipeTable, recipeJoker, redLoot, blueLoot);
        }

        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            blackhole ^= parseEquivalentLoad(recipeDeck, recipeTable, recipeJoker, redLoot, blueLoot);
        }
        long elapsed = System.nanoTime() - start;
        double ms = elapsed / 1_000_000.0;
        double avgMs = ms / iterations;
        double loadsPerSec = iterations / (elapsed / 1_000_000_000.0);

        System.out.println("=== Datapack load-path benchmark ===");
        System.out.printf("iterations=%d, total=%.2f ms, avg=%.4f ms/load, throughput=%.2f loads/s%n",
            iterations, ms, avgMs, loadsPerSec);
        System.out.println("blackhole=" + blackhole);
    }

    private static int parseEquivalentLoad(
        List<Path> deckRecipes,
        List<Path> tableRecipes,
        List<Path> jokerRecipes,
        Path redLoot,
        Path blueLoot
    ) throws IOException {
        int sum = 0;
        for (Path p : deckRecipes) {
            JsonObject root = readJson(p);
            JsonObject components = root.getAsJsonObject("result").getAsJsonObject("components");
            sum += firstColor(components);
            JsonArray cards = components.getAsJsonArray("minecraft:bundle_contents");
            sum += cards == null ? 0 : cards.size();
        }
        for (Path p : tableRecipes) {
            JsonObject root = readJson(p);
            JsonObject components = root.getAsJsonObject("result").getAsJsonObject("components");
            sum += firstColor(components);
        }
        for (Path p : jokerRecipes) {
            JsonObject root = readJson(p);
            JsonObject components = root.getAsJsonObject("result").getAsJsonObject("components");
            sum += firstColor(components);
        }
        sum += parseLootOwners(redLoot);
        sum += parseLootOwners(blueLoot);
        return sum;
    }

    private static int parseLootOwners(Path lootPath) throws IOException {
        JsonObject root = readJson(lootPath);
        JsonArray pools = root.getAsJsonArray("pools");
        int owners = 0;
        for (JsonElement poolElement : pools) {
            JsonObject pool = poolElement.getAsJsonObject();
            JsonArray entries = pool.getAsJsonArray("entries");
            for (JsonElement entryElement : entries) {
                JsonObject entry = entryElement.getAsJsonObject();
                JsonArray functions = entry.getAsJsonArray("functions");
                if (functions == null || functions.size() == 0) {
                    continue;
                }
                JsonObject fn = functions.get(0).getAsJsonObject();
                JsonObject components = fn.getAsJsonObject("components");
                if (components == null) {
                    continue;
                }
                JsonArray lore = components.getAsJsonArray("lore");
                if (lore != null && lore.size() > 0) {
                    owners++;
                }
            }
        }
        return owners;
    }

    private static int firstColor(JsonObject components) {
        JsonObject cmd = components.getAsJsonObject("minecraft:custom_model_data");
        JsonArray colors = cmd.getAsJsonArray("colors");
        return colors.get(0).getAsInt();
    }

    private static JsonObject readJson(Path path) throws IOException {
        String raw = Files.readString(path, StandardCharsets.UTF_8);
        return JsonParser.parseString(raw).getAsJsonObject();
    }

    private static List<Path> list(Path dir) throws IOException {
        List<Path> paths = new ArrayList<>();
        try (var stream = Files.list(dir)) {
            stream.filter(p -> p.getFileName().toString().endsWith(".json"))
                .sorted()
                .forEach(paths::add);
        }
        return paths;
    }
}
