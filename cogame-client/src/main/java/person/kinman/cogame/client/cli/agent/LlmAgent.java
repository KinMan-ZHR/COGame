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

    private static final Pattern CHOICE_PATTERN = Pattern.compile("(?i)\\\"?choice\\\"?\\s*:\\s*(-?\\d+)");
    private static final Pattern SESSION_ID_PATTERN = Pattern.compile("(?i)session\\s+id:\\s*([0-9a-f-]{36})");
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(120);

    private final String name;
    private final List<String> commandPrefix;
    private final Duration timeout;
    private final boolean isCodex;
    private final String codexBin;
    private final List<String> history = new ArrayList<>();
    private int turnCounter = 0;
    private String sessionId = null;

    public LlmAgent(String name, List<String> commandPrefix) {
        this(name, commandPrefix, DEFAULT_TIMEOUT, false, null);
    }

    public LlmAgent(String name, List<String> commandPrefix, Duration timeout) {
        this(name, commandPrefix, timeout, false, null);
    }

    public LlmAgent(String name, List<String> commandPrefix, Duration timeout, boolean isCodex, String codexBin) {
        this.name = Objects.requireNonNull(name, "name");
        this.commandPrefix = List.copyOf(commandPrefix);
        this.timeout = (timeout != null) ? timeout : DEFAULT_TIMEOUT;
        this.isCodex = isCodex;
        this.codexBin = codexBin;
    }

    public static LlmAgent gemini() {
        return new LlmAgent(
                "Gemini-Flash",
                List.of("agy", "--model", "gemini-3.8-flash-high", "--effort", "high", "--prompt")
        );
    }

    public static LlmAgent codex() {
        String codexBin = java.nio.file.Files.isExecutable(java.nio.file.Path.of("/home/kinman/.local/bin/codex"))
                ? "/home/kinman/.local/bin/codex" : "codex";
        return new LlmAgent(
                "Codex-GPT5",
                List.of(codexBin, "--ask-for-approval", "never", "exec", "-c",
                        "model_reasoning_effort=\"low\"", "--ignore-rules", "--sandbox",
                        "danger-full-access", "--skip-git-repo-check", "-C", "/tmp"),
                DEFAULT_TIMEOUT,
                true,
                codexBin
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
        List<String> cmd = new ArrayList<>();
        if (isCodex && sessionId != null) {
            // 同一局对战中：保持同一会话持续对话！
            System.out.printf("🔄 [Codex] 恢复同一会话 (%s) 继续第 %d 回合思考...\n", sessionId, turnCounter);
            cmd.add(codexBin != null ? codexBin : "codex");
            cmd.add("--ask-for-approval");
            cmd.add("never");
            cmd.add("exec");
            cmd.add("resume");
            cmd.add("-c");
            cmd.add("model_reasoning_effort=\"low\"");
            cmd.add("--dangerously-bypass-approvals-and-sandbox");
            cmd.add(sessionId);
            cmd.add("【第 " + turnCounter + " 回合】\n" + prompt);
        } else {
            // 新对局首回合：开启新对话
            cmd.addAll(commandPrefix);
            cmd.add(prompt);
        }

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

        // 若是新会话首回合，捕获 session id 供后序回合复用
        if (isCodex && sessionId == null) {
            Matcher sMatcher = SESSION_ID_PATTERN.matcher(response);
            if (sMatcher.find()) {
                this.sessionId = sMatcher.group(1);
                System.out.printf("🔗 [Codex] 已锁定对局会话句柄: %s (本局全程保持单会话记忆)\n", sessionId);
            }
        }

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

    @Override
    public void onGameOver(GameState state, int myPlayerId) {
        if (this.sessionId != null) {
            System.out.printf("🏁 [Codex] 对局结束，释放对局会话 %s (下局将开辟新会话)\n", sessionId);
            this.sessionId = null;
        }
        this.turnCounter = 0;
        this.history.clear();
    }

    @Override
    public void close() {
        this.sessionId = null;
    }
}
