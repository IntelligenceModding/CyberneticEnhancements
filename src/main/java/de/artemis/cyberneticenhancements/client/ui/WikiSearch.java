package de.artemis.cyberneticenhancements.client.ui;

import java.text.Normalizer;
import java.util.*;

/** Cached text fields, all-word matching and deterministic relevance ranking. */
public final class WikiSearch {
    private static final Set<String> QUERY_FILLER = Set.of("a", "an", "the", "how", "do", "does", "i", "my", "to", "of", "for", "and", "is", "can", "what", "where", "with", "in", "on", "at", "which", "much");
    private WikiSearch() {}

    public record Document(String id, String title, String category, String summary, List<String> passages,
                           List<String> titleWords, List<String> categoryWords, List<String> summaryWords, List<String> bodyWords,
                           List<List<String>> passageWords) {
        public Document(String id, String title, String category, String summary, List<String> passages) {
            this(id, title, category, summary, List.copyOf(passages), words(title), words(category), words(summary),
                    words(String.join(" ", passages)).stream().distinct().toList(), passages.stream().map(WikiSearch::words).toList());
        }
    }
    public record Hit(String id, int score, String context) {}

    public static List<Hit> search(Collection<Document> documents, String query) {
        var terms = words(query).stream().filter(term -> !QUERY_FILLER.contains(term)).distinct().toList();
        if (terms.isEmpty()) return List.of();
        List<Hit> hits = new ArrayList<>();
        for (var doc : documents) {
            int score = 0;
            boolean matches = true;
            for (String term : terms) {
                int best = Math.max(Math.max(match(term, doc.titleWords(), 120), match(term, doc.categoryWords(), 55)),
                        Math.max(match(term, doc.summaryWords(), 35), match(term, doc.bodyWords(), 15)));
                if (best == 0) { matches = false; break; }
                score += best;
            }
            if (!matches) continue;
            if (doc.titleWords().equals(terms)) score += 1000;
            else if (String.join(" ", doc.titleWords()).contains(String.join(" ", terms))) score += 300;
            hits.add(new Hit(doc.id(), score, context(doc, terms)));
        }
        var titles = new HashMap<String, String>();
        documents.forEach(doc -> titles.put(doc.id(), doc.title()));
        hits.sort(Comparator.comparingInt(Hit::score).reversed()
                .thenComparing(hit -> titles.get(hit.id()), String.CASE_INSENSITIVE_ORDER).thenComparing(Hit::id));
        return List.copyOf(hits);
    }

    private static String context(Document doc, List<String> terms) {
        String best = doc.summary();
        int bestScore = 0;
        for (int index = 0; index < doc.passages().size(); index++) {
            String passage = doc.passages().get(index);
            var tokens = doc.passageWords().get(index);
            int score = terms.stream().mapToInt(term -> match(term, tokens, 10)).sum();
            if (score > bestScore) { bestScore = score; best = passage; }
        }
        return doc.category().isBlank() ? best : best + " · " + doc.category();
    }

    private static int match(String term, List<String> words, int weight) {
        int best = 0;
        for (String word : words) {
            if (word.equals(term)) return weight;
            if (term.length() >= 2 && word.startsWith(term)) best = Math.max(best, weight * 3 / 4);
            else if (term.length() >= 5 && word.length() >= 5 && oneTypo(term, word)) best = Math.max(best, Math.max(1, weight / 4));
        }
        return best;
    }

    static List<String> words(String input) {
        String normalized = Normalizer.normalize(input.replaceAll("§.", "").replace("€$", " eurodollars ")
                .replace('_', ' ').replaceAll("([a-z])([A-Z])", "$1 $2")
                .replaceAll("([\\p{L}])([0-9])|([0-9])([\\p{L}])", "$1$3 $2$4"), Normalizer.Form.NFKD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
        return Arrays.stream(normalized.split("[^\\p{L}\\p{N}]+"))
                .filter(word -> !word.isEmpty()).map(WikiSearch::canonical).toList();
    }

    private static String canonical(String word) {
        return switch (word) {
            case "contracts", "contract", "quests" -> "quest";
            case "npcs" -> "npc";
            case "blueprint", "blueprints", "schematics" -> "schematic";
            case "eurodollar", "eurodollars", "eddies", "cash", "money" -> "money";
            case "experience" -> "xp";
            case "cyberpsychosis" -> "psychosis";
            case "headroom" -> "capacity";
            case "crafting", "crafts", "crafted" -> "craft";
            case "fabricator", "fabrication", "fabricate", "fabricating" -> "fabricat";
            case "recycler", "recycling", "recycle", "recycled" -> "recycl";
            case "upgrading", "upgrades", "upgrade", "upgraded" -> "upgrad";
            case "repairing", "repairs", "repaired" -> "repair";
            case "supplies" -> "supply";
            default -> word.length() > 4 && word.endsWith("s") && !word.endsWith("ss") && !word.endsWith("sis")
                    ? word.substring(0, word.length() - 1) : word;
        };
    }

    private static boolean oneTypo(String left, String right) {
        if (Math.abs(left.length() - right.length()) > 1) return false;
        if (left.length() == right.length()) {
            int first = -1, second = -1;
            for (int i = 0; i < left.length(); i++) if (left.charAt(i) != right.charAt(i)) {
                if (first == -1) first = i;
                else if (second == -1) second = i;
                else return false;
            }
            return second == -1 || second == first + 1 && left.charAt(first) == right.charAt(second) && left.charAt(second) == right.charAt(first);
        }
        String shorter = left.length() < right.length() ? left : right;
        String longer = left.length() < right.length() ? right : left;
        int i = 0, j = 0, skipped = 0;
        while (i < shorter.length() && j < longer.length()) {
            if (shorter.charAt(i) == longer.charAt(j)) { i++; j++; }
            else { if (++skipped > 1) return false; j++; }
        }
        return true;
    }
}
