package person.kinman.cogame.ai.cli;

import person.kinman.cogame.core.model.GameState;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * COGame external-model arena. Every turn is selected by a real model invocation and validated by the referee.
 */
public final class LlmBattleCli {
    private static final Pattern CHOICE_PATTERN = Pattern.compile("\\\"choice\\\"\\s*:\\s*(-?\\d+)");
    private static final Duration MODEL_TIMEOUT = Duration.ofMinutes(10);
    private static final int MAX_MODEL_ATTEMPTS = 2;
    private static final int MAX_TURNS = 200;

    private LlmBattleCli() {
    }

    private interface ModelPlayer {
        String name();

        int choose(String prompt) throws IOException, InterruptedException;
    }

    private static final class CommandModelPlayer implements ModelPlayer {
        private final String name;
        private final List<String> commandPrefix;

        private CommandModelPlayer(String name, List<String> commandPrefix) {
            this.name = Objects.requireNonNull(name, "name");
            this.commandPrefix = List.copyOf(commandPrefix);
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public int choose(String prompt) throws IOException, InterruptedException {
            IOException lastFailure = null;
            for (int attempt = 1; attempt <= MAX_MODEL_ATTEMPTS; attempt++) {
                try {
                    String attemptPrompt = attempt == 1
                            ? prompt
                            : prompt + "\n上次响应不可用。这是唯一一次重试；请只返回 {\"choice\":合法整数}。";
                    return chooseOnce(attemptPrompt);
                } catch (IOException failure) {
                    lastFailure = failure;
                    if (attempt == MAX_MODEL_ATTEMPTS || !isRetryable(failure)) {
                        throw failure;
                    }
                    System.out.printf(
                            "RETRY|model=%s|attempt=%d|reason=%s%n",
                            name, attempt + 1, sanitize(failure.getMessage()));
                }
            }
            throw Objects.requireNonNull(lastFailure, "lastFailure");
        }

        private int chooseOnce(String prompt) throws IOException, InterruptedException {
            List<String> command = new ArrayList<>(commandPrefix);
            command.add(prompt);
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            process.getOutputStream().close();

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Thread outputReader = new Thread(() -> transferOutput(process, output),
                    "cogame-" + name + "-output-reader");
            outputReader.start();
            boolean finished = process.waitFor(MODEL_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                outputReader.join(TimeUnit.SECONDS.toMillis(5));
                throw new IOException(name + " response timed out after " + MODEL_TIMEOUT.toSeconds() + " seconds");
            }

            outputReader.join(TimeUnit.SECONDS.toMillis(5));
            String response = output.toString(StandardCharsets.UTF_8);
            if (process.exitValue() != 0) {
                throw new IOException(name + " exited with code " + process.exitValue() + ": " + abbreviate(response));
            }
            return parseChoice(response);
        }

        private static void transferOutput(Process process, ByteArrayOutputStream output) {
            try {
                process.getInputStream().transferTo(output);
            } catch (IOException ignored) {
                // The caller reports process failures and timeouts with the output captured so far.
            }
        }
    }

    public static void main(String[] args) {
        int size = 6;
        String p1Type = "gemini";
        String p2Type = "codex";
        String matchId = "llm-match-" + System.currentTimeMillis();

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--size", "-s" -> size = Integer.parseInt(requireValue(args, ++i, "--size"));
                case "--p1" -> p1Type = requireValue(args, ++i, "--p1").toLowerCase(Locale.ROOT);
                case "--p2" -> p2Type = requireValue(args, ++i, "--p2").toLowerCase(Locale.ROOT);
                case "--match-id" -> matchId = requireValue(args, ++i, "--match-id");
                case "--help", "-h" -> {
                    printHelp();
                    return;
                }
                default -> throw new IllegalArgumentException("Unknown argument: " + args[i]);
            }
        }

        if (size != 6 && size != 12) {
            throw new IllegalArgumentException("Only board sizes 6 and 12 are supported");
        }
        runGame(size, matchId, createPlayer(p1Type), createPlayer(p2Type));
    }

    private static void runGame(int size, String matchId, ModelPlayer p1, ModelPlayer p2) {
        GameState state = new GameState(size);
        state.getP1().setName(p1.name());
        state.getP2().setName(p2.name());
        Instant startedAt = Instant.now();
        System.out.printf("START|match=%s|size=%d|p1=%s|p2=%s%n", matchId, size, p1.name(), p2.name());

        int turns = 0;
        Integer forcedWinnerId = null;
        String failure = null;
        List<String> history = new ArrayList<>();
        while (!state.isOver() && turns < MAX_TURNS) {
            turns++;
            int playerId = state.getCurrentTurn();
            ModelPlayer current = playerId == 1 ? p1 : p2;
            List<LlmTurnAdvisor.Candidate> candidates = LlmTurnAdvisor.enumerateCandidates(state);
            if (candidates.isEmpty()) {
                forcedWinnerId = opponentOf(playerId);
                failure = current.name() + " has no legal turn";
                break;
            }

            int choice;
            try {
                String prompt = LlmTurnAdvisor.buildPrompt(
                        matchId, turns, state, current.name(), candidates, history);
                choice = current.choose(prompt);
            } catch (IOException | InterruptedException exception) {
                if (exception instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                forcedWinnerId = opponentOf(playerId);
                failure = exception.getMessage();
                break;
            }

            if (choice < 0 || choice >= candidates.size()) {
                forcedWinnerId = opponentOf(playerId);
                failure = current.name() + " returned illegal choice " + choice
                        + ", expected [0," + (candidates.size() - 1) + "]";
                break;
            }

            LlmTurnAdvisor.Candidate selected = candidates.get(choice);
            if (!LlmTurnAdvisor.executeCandidate(state, playerId, selected)) {
                forcedWinnerId = opponentOf(playerId);
                failure = current.name() + " selected a candidate rejected by the referee";
                break;
            }
            String path = LlmTurnAdvisor.pathText(selected.path());
            System.out.printf(
                    "TURN|match=%s|turn=%d|model=%s|player=%d|choice=%d|path=%s|lock=%s|at=%d,%d|outcome=%s%n",
                    matchId, turns, current.name(), playerId, selected.id(), path,
                    selected.lockDirection(), selected.row(), selected.col(), selected.outcome());
            history.add(String.format(
                    "T%d P%d path=%s at=%d,%d lock=%s",
                    turns, playerId, path, selected.row(), selected.col(), selected.lockDirection()));
        }

        int winnerId = resolveWinnerId(state, forcedWinnerId);
        String winner = winnerId == 1 ? p1.name() : winnerId == 2 ? p2.name() : "DRAW";
        long seconds = Duration.between(startedAt, Instant.now()).toSeconds();
        System.out.printf(
                "RESULT|match=%s|size=%d|p1=%s|p2=%s|winner=%s|winnerPlayer=P%d|territory=%d:%d|turns=%d|seconds=%d|failure=%s%n",
                matchId, size, p1.name(), p2.name(), winner, winnerId,
                state.getP1Territory(), state.getP2Territory(), turns, seconds,
                failure == null ? "none" : sanitize(failure));
    }

    private static ModelPlayer createPlayer(String type) {
        return switch (type) {
            case "gemini" -> new CommandModelPlayer(
                    "Gemini-3.8-Flash-High",
                    List.of("agy", "--model", "gemini-3.8-flash-high", "--effort", "high", "--prompt"));
            case "codex" -> new CommandModelPlayer(
                    "Codex-GPT-5.6-Sol-High",
                    List.of("codex", "--ask-for-approval", "never", "exec", "-c",
                            "model_reasoning_effort=\"high\"", "--ignore-rules", "--ephemeral", "--sandbox",
                            "read-only", "--skip-git-repo-check", "-C", "/tmp"));
            default -> throw new IllegalArgumentException("Unsupported external model: " + type);
        };
    }

    static int parseChoice(String response) throws IOException {
        Matcher matcher = CHOICE_PATTERN.matcher(Objects.requireNonNull(response, "response"));
        Integer lastChoice = null;
        while (matcher.find()) {
            lastChoice = Integer.parseInt(matcher.group(1));
        }
        if (lastChoice == null) {
            throw new IOException("Model returned no JSON choice: " + abbreviate(response));
        }
        return lastChoice;
    }

    static boolean isRetryable(IOException failure) {
        String message = Objects.requireNonNullElse(failure.getMessage(), "").toLowerCase(Locale.ROOT);
        return !message.contains("timed out");
    }

    private static int opponentOf(int playerId) {
        return playerId == 1 ? 2 : 1;
    }

    private static int resolveWinnerId(GameState state, Integer forcedWinnerId) {
        return forcedWinnerId == null ? state.getWinner() : forcedWinnerId;
    }

    private static String requireValue(String[] args, int index, String option) {
        if (index >= args.length) {
            throw new IllegalArgumentException("Missing value for " + option);
        }
        return args[index];
    }

    private static String abbreviate(CharSequence value) {
        String text = value.toString().replace('\n', ' ').trim();
        return text.length() <= 500 ? text : text.substring(text.length() - 500);
    }

    private static String sanitize(String value) {
        return Objects.requireNonNullElse(value, "unknown").replace('|', '/').replace('\n', ' ');
    }

    private static void printHelp() {
        System.out.println(
                "Usage: llm-battle-cli --size <6|12> --p1 <gemini|codex> --p2 <gemini|codex> --match-id <id>");
    }
}
