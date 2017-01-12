package net.demilich.metastone.game.behaviour.advanced;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.actions.EndTurnAction;
import net.demilich.metastone.game.actions.GameAction;
import net.demilich.metastone.game.behaviour.Behaviour;
import net.demilich.metastone.game.behaviour.IStateEvaluate;
import net.demilich.metastone.game.cards.Card;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AdvancedBehaviour performs Monte-Carlo traversal of a single turn action DAG.
 * State evaluation function needs to be provided. Many optimizations have been implemented
 * such as: visited state hashing, depth limited search, A* search strategy
 *
 * @author  Lukasz Grad
 */
public class AdvancedBehaviour extends Behaviour {

	/*
	 * Class representing a node during DAG traversal
	 */
	private class ContextNode<T1 extends Comparable<T1>> implements Comparable {
		private final GameContext context;
		private final Integer depth;
		private final T1 value;

		public ContextNode(GameContext context, int depth, T1 value) {
			this.context = context;
			this.depth = depth;
			this.value = value;
		}

		public GameContext getContext() {
			return context;
		}

		public int getDepth() {
			return depth;
		}

		public T1 getValue() {
			return value;
		}

		@SuppressWarnings("unchecked")
		@Override
		public int compareTo(Object o) {
			// TODO Throw exception if cannot cast
			ContextNode<T1> other = (ContextNode<T1>) o;
			int compareDepth = depth.compareTo(other.getDepth());
			// Prefer shallower nodes
			if (compareDepth != 0)
				return compareDepth;
			// Nodes with the same depth, prefer higher value
			return -1 * value.compareTo(other.getValue());
		}
	}

	private final IStateEvaluate<Double> evaluator;
	private final int maxDepth;
	private final int budget;
	private final int bestDepth;
	private String name;

	public AdvancedBehaviour(IStateEvaluate<Double> evaluator, int maxDepth, int bestDepth, int budget) {
		this.evaluator = evaluator;
		this.maxDepth = maxDepth;
		this.bestDepth = bestDepth;
		this.budget = budget;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return "Advanced AI " + name;
	}

	@Override
	public List<Card> mulligan(GameContext context, Player player, List<Card> cards) {
		return cards
			.stream()
			.filter(card -> card.getBaseManaCost() > 3)
			.collect(Collectors.toList());
	}

	@Override
	public void onGameOver(GameContext context, int playerId, int winningPlayerId) {
		// TODO
	}

	@Override
	public GameAction requestAction(GameContext context, Player player, List<GameAction> validActions) {
		if (validActions.size() == 1)
			return validActions.get(0);

		HashSet<GameContext> knownStates = new HashSet<>();
		GameAction bestAction = new EndTurnAction();
		Double bestValue = evaluate(context, player.getId());
		for (GameAction action : validActions) {
			Double value = traverse(context, player.getId(), action, knownStates);
			if (value.compareTo(bestValue) > 0) {
				bestValue = value;
				bestAction = action;
			}
		}
		System.out.println("Best action score: " + bestValue);
		return bestAction;
	}

	private Double traverse(GameContext context,
	                   int playerId,
	                   GameAction action,
	                   HashSet<GameContext> knownStates) {
		GameContext contextClone = context.clone();
		contextClone.getLogic().performGameAction(playerId, action);
		Double bestValue = evaluator.evaluate(contextClone, playerId);
		// Start from the root node with depth 0
		PriorityQueue<ContextNode<Double>> states = new PriorityQueue<>();
		states.add(new ContextNode<>(contextClone, 0, bestValue));
		int steps = 0;
		Double value;
		while(!states.isEmpty() && steps < budget) {
			ContextNode<Double> node = states.poll(); // Remove best node
			// Immediately return if already seen
			if (!knownStates.contains(node.getContext())) {
				// Previously unseen state
				knownStates.add(node.getContext());
				value = node.getValue();
				if (!isFinished(node.getContext(), playerId)) {
					// Player turn not finished
					if (node.getDepth() <= maxDepth) {
						states.addAll(spanAll(node, playerId));
					} else {
						// Maximum depth exceeded, perform best child traversal
						value = bestTraversal(node, playerId, 1);
					}
				}
				if (value.compareTo(bestValue) > 0)
					bestValue = value;
				steps++;
			}
		}
		return bestValue;
	}

	private Double bestTraversal(ContextNode<Double> node, int playerId, int currentDepth) {
		if (isFinished(node.getContext(), playerId) || currentDepth == bestDepth)
			return node.getValue();
		Double value = bestTraversal(bestNode(spanAll(node, playerId)), playerId, currentDepth + 1);
		return (node.getValue().compareTo(value) > 0) ? node.getValue(): value;
	}

	private ContextNode<Double> bestNode(List<ContextNode<Double>> nodes) {
		ContextNode<Double> best = nodes.get(0);
		Double bestValue = best.getValue();
		for (ContextNode<Double> node : nodes) {
			if (node.getValue().compareTo(bestValue) > 0) {
				bestValue = node.getValue();
				best = node;
			}
		}
		return best;
	}

	private List<ContextNode<Double>> spanAll(ContextNode<Double> node, int playerId) {
		List<ContextNode<Double>> nodes = new ArrayList<>();
		for (GameAction a : node.getContext().getValidActions()) {
			GameContext actionContext = node.getContext().clone();
			actionContext.getLogic().performGameAction(playerId, a);
			nodes.add(new ContextNode<>(
					actionContext,
					node.getDepth() + 1,
					evaluate(actionContext, playerId))
			);
		}
		return nodes;
	}

	private boolean isFinished(GameContext context, int playerId) {
		return (context.getActivePlayerId() != playerId || context.gameDecided());
	}

	private Double evaluate(GameContext context, int playerId) {
		if (context.getPlayer(playerId).isDestroyed())
			return Double.NEGATIVE_INFINITY;
		if (context.getOpponent(context.getPlayer(playerId)).isDestroyed())
			return Double.POSITIVE_INFINITY;
		return evaluator.evaluate(context, playerId);
	}
}
