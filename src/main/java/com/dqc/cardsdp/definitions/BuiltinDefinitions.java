package com.dqc.cardsdp.definitions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class BuiltinDefinitions {
    private static final List<String> RANKS = List.of("A", "2", "3", "4", "5", "6", "7", "8", "9", "X", "J", "Q", "K");

    private static final SuitSpec FLOWER = new SuitSpec("flower", true, "GoodTimesWithScar", "ZombieCleo", "Rendog");
    private static final SuitSpec PICK = new SuitSpec("pick", false, "BdoubleO100", "GeminiTay", "Grian");
    private static final SuitSpec REDSTONE = new SuitSpec("redstone", true, "MumboJumbo", "PearlescentMoon", "EthosLab");
    private static final SuitSpec SWORD = new SuitSpec("sword", false, "SmallishBeans", "FalseSymmetry", "Technoblade");

    private BuiltinDefinitions() {
    }

    public static CardsDefinitions create() {
        Map<String, Integer> deckColors = Map.ofEntries(
            Map.entry("black", 1381656),
            Map.entry("blue", 2962303),
            Map.entry("brown", 6438693),
            Map.entry("cyan", 1078645),
            Map.entry("gray", 3488573),
            Map.entry("green", 4611344),
            Map.entry("light_blue", 2852515),
            Map.entry("light_gray", 7697777),
            Map.entry("lime", 6329623),
            Map.entry("magenta", 9779853),
            Map.entry("orange", 12214293),
            Map.entry("pink", 11954303),
            Map.entry("purple", 6694282),
            Map.entry("red", 8659484),
            Map.entry("white", 15132390),
            Map.entry("yellow", 12493357)
        );

        Map<String, Integer> tableColors = Map.ofEntries(
            Map.entry("black", 1908001),
            Map.entry("blue", 3949738),
            Map.entry("brown", 8606770),
            Map.entry("cyan", 1481884),
            Map.entry("gray", 4673362),
            Map.entry("green", 6192150),
            Map.entry("light_blue", 3847130),
            Map.entry("light_gray", 10329495),
            Map.entry("lime", 8439583),
            Map.entry("magenta", 13061821),
            Map.entry("orange", 16351261),
            Map.entry("pink", 15961002),
            Map.entry("purple", 8991416),
            Map.entry("red", 11546150),
            Map.entry("white", 16383998),
            Map.entry("yellow", 16701501)
        );

        Map<String, Integer> jokerColors = deckColors;

        Map<String, List<CardDefinition>> decksByColor = deckColors.entrySet().stream()
            .collect(java.util.stream.Collectors.toUnmodifiableMap(
                Map.Entry::getKey,
                entry -> buildDeck(entry.getValue())
            ));

        List<String> redJokerOwners = List.of(
            "OpenBagTwo", "ThePoyoPal", "MrVildy", "Nonik_", "Golem64",
            "fwics", "Lniz", "BlobGames", "OnyxJS", "Nehmne",
            "GunsAndChips", "bbb651", "Pepe20129", "DysphoricPeach", "davie53",
            "Hexasann", "Veganwater45", "FernandoDaniel", "Conure512", "puppywader",
            "Peril137", "victhor003", "FakeZircon", "GoldenRobot_II", "LordFlame",
            "oga449", "ped3strian", "bpendragon", "Zerahu", "Ratman0813",
            "G4B8O", "LightWingUltra", "Queen_1405", "zeppelans", "fuffypandauwu",
            "StealthStalker", "machasins", "HAMMMY7", "AylaMao117", "SlimyRedstone",
            "RubberCrowy", "lilypadSquared", "Jollto", "NotBoringName", "_1024",
            "Mister_Scheu", "TimeBender25", "Vertigofy", "Aicesnow", "Caloob_",
            "Popa_42", "Jeringlyst", "ATOMICtheCrow", "3njooo", "Melancholy__",
            "Paniawesome", "100percentme", "UnableToFindUser", "LukeKr", "OrbBoi",
            "TheOneTheory", "bronylike", "Zeyro_p", "Flamesilk", "Gezinski",
            "ADAM_4644", "omerkb", "Deaths_Shad0w", "FaultierLotus54", "AddyTheNomad",
            "Golden_Wither", "Y2Kun", "Simonomi", "DanielRH", "Joelydsac",
            "Eikichirou", "Coalava"
        );

        List<String> blueJokerOwners = List.of(
            "cubfan135", "Docm77", "hypnotizd", "iJevin", "impulseSV",
            "joehillssays", "Keralis", "Skizzleman", "TangoTek", "VintageBeef",
            "Welsknight", "xBCrafted", "xisumavoid", "Zedaph"
        );

        return new CardsDefinitions(
            deckColors,
            tableColors,
            jokerColors,
            decksByColor,
            redJokerOwners,
            blueJokerOwners
        );
    }

    private static List<CardDefinition> buildDeck(int deckColor) {
        List<CardDefinition> cards = new ArrayList<>(52);
        addSuit(cards, FLOWER, deckColor);
        addSuit(cards, PICK, deckColor);
        addSuit(cards, REDSTONE, deckColor);
        addSuit(cards, SWORD, deckColor);
        return List.copyOf(cards);
    }

    private static void addSuit(List<CardDefinition> out, SuitSpec suit, int deckColor) {
        for (String rank : RANKS) {
            String owner = switch (rank) {
                case "J" -> suit.jackOwner;
                case "Q" -> suit.queenOwner;
                case "K" -> suit.kingOwner;
                default -> null;
            };
            out.add(new CardDefinition(suit.id, rank, deckColor, suit.redTone, owner));
        }
    }

    private record SuitSpec(String id, boolean redTone, String jackOwner, String queenOwner, String kingOwner) {
    }
}
