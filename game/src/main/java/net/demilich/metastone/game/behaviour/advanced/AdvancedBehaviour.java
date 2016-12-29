package net.demilich.metastone.game.behaviour.advanced;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.actions.GameAction;
import net.demilich.metastone.game.behaviour.Behaviour;
import net.demilich.metastone.game.behaviour.IStateEvaluate;
import net.demilich.metastone.game.cards.Card;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AdvancedBehaviour performs Monte-Carlo traversal of the action DAG for each valid action.
 * State evaluation function needs to be provided. Many optimizations have been implemented
 * such as: visited state hashing, depth limited search, A* search strategy
 *
 * @author  Lukasz Grad
 */
public class AdvancedBehaviour<T extends Comparable<T>> extends Behaviour {

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

	private final IStateEvaluate<T> evaluator;
	private final int maxDepth;
	private final int budget;

	public AdvancedBehaviour(IStateEvaluate<T> evaluator, int maxDepth, int budget) {
		this.evaluator = evaluator;
		this.maxDepth = maxDepth;
		this.budget = budget;
	}

	@Override
	public String getName() {
		return "Advanced AI";
	}

	@Override
	public List<Card> mulligan(GameContext context, Player player, List<Card> cards) {
		return cards
			.stream()
			.filter(card -> card.getBaseManaCost() > 3)
			.collect(Collectors.toList());
	}

	@Override
	public GameAction requestAction(GameContext context, Player player, List<GameAction> validActions) {
		if (validActions.size() == 1)
			return validActions.get(0);

		HashSet<GameContext> knownStates = new HashSet<>();
		GameAction bestAction = validActions.get(0);
		T bestValue = evaluator.evaluate(context, player.getId());
		for (GameAction action : validActions) {
			T value = traverse(context, player.getId(), action, knownStates);
			if (value.compareTo(bestValue) > 0) {
				bestValue = value;
				bestAction = action;
			}
		}
		return bestAction;
	}

	private T traverse(GameContext context,
	                   int playerId,
	                   GameAction action,
	                   HashSet<GameContext> knownStates) {
		GameContext contextClone = context.clone();
		contextClone.getLogic().performGameAction(playerId, action);
		T bestValue = evaluator.evaluate(contextClone, playerId);

		// Start from the root node with depth 0
		PriorityQueue<ContextNode<T>> states = new PriorityQueue<>();
		states.add(new ContextNode<>(contextClone, 0, bestValue));
		int steps = 0;
		T value;
		while(!states.isEmpty() && steps < budget) {
			ContextNode<T> node = states.poll(); // Remove best node
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
						value = bestTraversal(node, playerId);
					}
				}
				if (value.compareTo(bestValue) > 0)
					bestValue = value;
				steps++;
			}
		}
		return bestValue;
	}

	private T bestTraversal(ContextNode<T> node, int playerId) {
		if (isFinished(node.getContext(), playerId))
			return node.getValue();
		T value = bestTraversal(bestNode(spanAll(node, playerId)), playerId);
		return (node.getValue().compareTo(value) > 0) ? node.getValue(): value;
	}

	private ContextNode<T> bestNode(List<ContextNode<T>> nodes) {
		ContextNode<T> best = nodes.get(0);
		T bestValue = best.getValue();
		for (ContextNode<T> node : nodes) {
			if (node.getValue().compareTo(bestValue) > 0) {
				bestValue = node.getValue();
				best = node;
			}
		}
		return best;
	}

	private List<ContextNode<T>> spanAll(ContextNode<T> node, int playerId) {
		List<ContextNode<T>> nodes = new ArrayList<>();
		for (GameAction a : node.getContext().getValidActions()) {
			GameContext actionContext = node.getContext().clone();
			actionContext.getLogic().performGameAction(playerId, a);
			nodes.add(new ContextNode<>(
					actionContext,
					node.getDepth() + 1,
					evaluator.evaluate(actionContext, playerId))
			);
		}
		return nodes;
	}

	private boolean isFinished(GameContext context, int playerId) {
		return (context.getActivePlayerId() != playerId || context.gameDecided());
	}
}
