package person.kinman.cogame.client.cli.agent;

import person.kinman.cogame.ai.cli.LlmTurnAdvisor;
import person.kinman.cogame.core.action.GameAction;
import person.kinman.cogame.core.model.GameState;
import person.kinman.cogame.core.model.PlayerState;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 外部大模型 Agent：通过子进程管道调起真实大模型（如 Gemini、Codex），基于候选集安全决策
 */
public class LlmAgent implements PlayerAgent {

    private static final Pattern CHOICE_PATTERN = Pattern.compile("\\\"choice\\\"\\s*:\\s*(-?\\d+)");
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);

    private final String name;
    private final List<String> commandPrefix;
    private final Duration timeout;
    private final List<String> history = new ArrayList<>();
    private int turnCounter = 0;

    public LlmAgent(String name, List<String> commandPrefix) {
        this(name, commandPrefix, DEFAULT_TIMEOUT);
    }

    public LlmAgent(String name, List<String> commandPrefix, Duration timeout) {
        this.name = Objects.requireNonNull(name, "name");
        this.commandPrefix = List.copyOf(commandPrefix);
        this.timeout = (timeout != null) ? timeout : DEFAULT_TIMEOUT;
    }

    public static LlmAgent gemini() {
        return new LlmAgent(
                "Gemini-Flash",
                List.of("agy", "--model", "gemini-3.8-flash-high", "--effort", "high", "--prompt")
        );
    }

    public static LlmAgent codex() {
        return new LlmAgent(
                "Codex-GPT5",
                List.of("codex", "--ask-for-approval", "never", "exec", "-c",
                        "model_reasoning_effort=\"high\"", "--ignore-rules", "--ephemeral", "--sandbox",
                        "danger-full-access", "--skip-git-repo-check", "-C", "/tmp")
        );
    }

    public static LlmAgent custom(String name, String command) {
        return new LlmAgent(name, List.of("bash", "-c", command));
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<GameAction> act(GameState state, int myPlayerId) throws Exception {
        turnCounter++;
        List<LlmTurnAdvisor.Candidate> candidates = LlmTurnAdvisor.enumerateCandidates(state);
        if (candidates.isEmpty()) {
            return List.of(GameAction.lock());
        }

        String matchId = "match-" + System.currentTimeMillis();
        String prompt = LlmTurnAdvisor.buildPrompt(matchId, turnCounter, state, name, candidates, history);

        int choice = -1;
        try {
            choice = invokeModel(prompt);
        } catch (Exception e) {
            System.err.printf("⚠️ [%s] 大模型调用异常 (%s)，启动智能降级保底策略！\n", name, e.getMessage());
        }

        if (choice < 0 || choice >= candidates.size()) {
            // 降级策略：选择首个非自杀候选
            choice = selectFallbackChoice(candidates);
        }

        LlmTurnAdvisor.Candidate selected = candidates.get(choice);
        PlayerState me = state.getPlayer(myPlayerId);
        List<GameAction> actions = LlmTurnAdvisor.buildCandidateActions(me, selected);

        history.add(String.format("T%d P%d path=%s lock=%s",
                turnCounter, myPlayerId, LlmTurnAdvisor.pathText(selected.path()), selected.lockDirection()));

        return actions;
    }

    private int selectFallbackChoice(List<LlmTurnAdvisor.Candidate> candidates) {
        for (int i = 0; i < candidates.size(); i++) {
            if (candidates.get(i).outcome() != LlmTurnAdvisor.Outcome.LOSS) {
                return i;
            }
        }
        return 0;
    }

    private int invokeModel(String prompt) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>(commandPrefix);
        cmd.add(prompt);

        Process process = new ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .start();
        process.getOutputStream().close();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Thread reader = new Thread(() -> {
            try {
                process.getInputStream().transferTo(output);
            } catch (IOException ignored) {}
        });
        reader.start();

        boolean finished = process.waitFor(timeout.toSeconds(), TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            reader.join(2000);
            throw new IOException("调用大模型超时 (" + timeout.toSeconds() + "s)");
        }

        reader.join(2000);
        String response = output.toString(StandardCharsets.UTF_8);
        return parseChoice(response);
    }

    private int parseChoice(String response) throws IOException {
        Matcher matcher = CHOICE_PATTERN.matcher(response);
        Integer lastChoice = null;
        while (matcher.find()) {
            lastChoice = Integer.parseInt(matcher.group(1));
        }
        if (lastChoice == null) {
            throw new IOException("模型输出未找到 JSON choice: " + response.substring(Math.max(0, response.length() - 200)));
        }
        return lastChoice;
    }
}
