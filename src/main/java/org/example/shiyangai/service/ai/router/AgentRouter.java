// AgentRouter.java
package org.example.shiyangai.service.ai.router;

import org.example.shiyangai.service.ai.agent.Agent;
import org.example.shiyangai.service.ai.context.ConversationContext;

import java.util.List;

public interface AgentRouter {
    List<Agent> route(ConversationContext ctx);
}