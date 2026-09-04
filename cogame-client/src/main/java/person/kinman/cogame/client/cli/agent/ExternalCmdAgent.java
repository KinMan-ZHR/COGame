package person.kinman.cogame.client.cli.agent;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import person.kinman.cogame.ai.cli.LlmTurnAdvisor;
import person.kinman.cogame.core.action.GameAction;
import person.kinman.cogame.core.model.GameState;
import person.kinman.cogame.core.model.PlayerState;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 外部自定义程序 Agent：支持任何语言 (Python/C++/Rust/Node/Shell) 编写的外部程序通过标准输入输出 JSON 对战
 */
public class ExternalCmdAgent implements PlayerAgent {

    private final String name;
    private final String shellCommand;
    private final Duration timeout;
    private final Gson gson = new Gson();
    private int turnCounter = 0;

    public ExternalCmdAgent(String name, String shellCommand) {
        this(name, shellCommand, Duration.ofSeconds(15));
    }

    public ExternalCmdAgent(String name, String shellCommand, Duration timeout) {
        this.name = Objects.requireNonNull(name, "name");
        this.shellCommand = Objects.requireNonNull(shellCommand, "shellCommand");
        this.timeout = (timeout != null) ? timeout : Duration.ofSeconds(15);
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

        PlayerState me = state.getPlayer(myPlayerId);
        PlayerState opp = state.getPlayer(myPlayerId == 1 ? 2 : 1);

        // 构建机器可读的标准 JSON 局面载荷
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("turn", turnCounter);
        payload.put("size", state.getRows());
        payload.put("myPlayerId", myPlayerId);
        payload.put("myEnergy", me.getEnergy());
        payload.put("maxEnergy", state.getMaxEnergy());
        payload.put("myPos", List.of(me.getR(), me.getC()));
        payload.put("myFacing", me.getDirection().name());
        payload.put("oppPos", List.of(opp.getR(), opp.getC()));
        payload.put("oppEnergy", opp.getEnergy());

        List<Map<String, Object>> candidateList = new ArrayList<>();
        for (LlmTurnAdvisor.Candidate c : candidates) {
            Map<String, Object> cm = new LinkedHashMap<>();
            cm.put("id", c.id());
            cm.put("steps", c.path().size());
            cm.put("path", c.path().stream().map(Enum::name).toList());
            cm.put("lock", c.lockDirection().name());
            cm.put("target", List.of(c.row(), c.col()));
            cm.put("outcome", c.outcome().name());
            cm.put("myTerritory", c.myTerritory());
            cm.put("oppTerritory", c.opponentTerritory());
            candidateList.add(cm);
        }
        payload.put("candidates", candidateList);

        String inputJson = gson.toJson(payload);

        int choice = -1;
        try {
            choice = executeCommand(inputJson);
        } catch (Exception e) {
            System.err.printf("⚠️ [ExternalBot:%s] 执行异常: %s，启动非自杀降级保底！\n", name, e.getMessage());
        }

        if (choice < 0 || choice >= candidates.size()) {
            choice = selectSafeFallback(candidates);
        }

        LlmTurnAdvisor.Candidate selected = candidates.get(choice);
        return LlmTurnAdvisor.buildCandidateActions(me, selected);
    }

    private int selectSafeFallback(List<LlmTurnAdvisor.Candidate> candidates) {
        for (int i = 0; i < candidates.size(); i++) {
            if (candidates.get(i).outcome() != LlmTurnAdvisor.Outcome.LOSS) {
                return i;
            }
        }
        return 0;
    }

    private int executeCommand(String inputJson) throws IOException, InterruptedException {
        Process process = new ProcessBuilder("bash", "-c", shellCommand)
                .redirectErrorStream(false)
                .start();

        process.getOutputStream().write(inputJson.getBytes(StandardCharsets.UTF_8));
        process.getOutputStream().flush();
        process.getOutputStream().close();

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        Thread reader = new Thread(() -> {
            try {
                process.getInputStream().transferTo(outStream);
            } catch (IOException ignored) {}
        });
        reader.start();

        boolean ok = process.waitFor(timeout.toSeconds(), TimeUnit.SECONDS);
        if (!ok) {
            process.destroyForcibly();
            reader.join(1000);
            throw new IOException("外部进程执行超时 (" + timeout.toSeconds() + "s)");
        }

        reader.join(1000);
        String response = outStream.toString(StandardCharsets.UTF_8).trim();

        JsonObject obj = gson.fromJson(response, JsonObject.class);
        if (obj != null && obj.has("choice")) {
            return obj.get("choice").getAsInt();
        }
        throw new IOException("未在输出中找到 'choice' 字段: " + response);
    }
}
