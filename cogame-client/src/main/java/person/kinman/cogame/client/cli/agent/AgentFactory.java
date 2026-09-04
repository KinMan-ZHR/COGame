package person.kinman.cogame.client.cli.agent;

import person.kinman.cogame.ai.AntigravityStrategy;
import person.kinman.cogame.ai.CodexStrategy;
import person.kinman.cogame.ai.HeuristicAi;

import java.util.Locale;

/**
 * 统一 Agent 工厂：将任意配置字符串解析为对应的参赛实体
 */
public class AgentFactory {

    public static PlayerAgent create(String spec) {
        if (spec == null || spec.isBlank()) {
            return new HeuristicAgent("Antigravity AI", new AntigravityStrategy());
        }

        String lower = spec.trim().toLowerCase(Locale.ROOT);

        if (lower.equals("human") || lower.startsWith("human:")) {
            String name = spec.contains(":") ? spec.substring(spec.indexOf(':') + 1).trim() : "Human";
            return new TerminalHumanAgent(name);
        }

        if (lower.equals("antigravity") || lower.equals("ai:antigravity") || lower.equals("moheng") || lower.equals("ai:moheng")) {
            return new HeuristicAgent("莫衡 (控盘大师)", new AntigravityStrategy());
        }

        if (lower.equals("codex") || lower.equals("ai:codex") || lower.equals("jingci") || lower.equals("ai:jingci")) {
            return new HeuristicAgent("荆刺 (破局猎手)", new CodexStrategy());
        }

        if (lower.equals("classic") || lower.equals("ai:classic") || lower.equals("guard") || lower.equals("xuanyue") || lower.equals("ai:xuanyue")) {
            return new HeuristicAgent("玄岳 (铁壁守卫)", new HeuristicAi(3));
        }

        if (lower.equals("llm:gemini")) {
            return LlmAgent.gemini();
        }

        if (lower.equals("llm:codex")) {
            return LlmAgent.codex();
        }

        if (lower.startsWith("llm:")) {
            // 格式: llm:模型名:命令
            String[] parts = spec.split(":", 3);
            if (parts.length >= 3) {
                return LlmAgent.custom(parts[1], parts[2]);
            }
        }

        if (lower.startsWith("cmd:")) {
            // 格式: cmd:外部命令 或 cmd:Bot名:外部命令
            String rest = spec.substring(4).trim();
            if (rest.contains(":")) {
                int idx = rest.indexOf(':');
                String name = rest.substring(0, idx).trim();
                String cmd = rest.substring(idx + 1).trim();
                return new ExternalCmdAgent(name, cmd);
            } else {
                return new ExternalCmdAgent("ExternalBot", rest);
            }
        }

        // 默认回退
        return new HeuristicAgent(spec, new AntigravityStrategy());
    }
}
