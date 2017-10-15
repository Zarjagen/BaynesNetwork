package inference;

import java.io.IOException;
import java.util.Map;
import java.util.Random;

import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

import core.*;
import parser.XMLBIFParser;

public class LikelihoodWeighting implements Inferencer {

	/**
	 * A custom class that uses the concept of java Entry, which has the
	 * Assignment as the key all the time, and the weight for this Assignment as
	 * the values all the time.
	 */
	protected class WeightedEntry implements Map.Entry<Assignment, Double> {
		private Assignment e;
		private Double w;

		public WeightedEntry(Assignment e, double w) {
			this.e = e;
			this.w = w;
		}

		@Override
		public Assignment getKey() {
			return e;
		}

		@Override
		public Double getValue() {
			return w;
		}

		@Override
		public Double setValue(Double value) {
			double oldW = w;
			this.w = value;
			return oldW;
		}

	}

	protected int n = 0;

	/**
	 * The constructor of the class, it creates an instance of the class and
	 * takes a parameter n to determine the size of the samples to generate
	 */
	public LikelihoodWeighting(int n) {
		this.n = n;
	}

	/**
	 * Override method, directly pointed to customized method askWeight, which
	 * takes one additional parameter n
	 */
	@Override
	public Distribution ask(BayesianNetwork bn, RandomVariable X, Assignment e) {
		return askWeight(bn, X, e, n);
	}

	/**
	 * The basic idea is the same as Rejection Sampling except that the samples
	 * are not completely random, and the sampling method returns both the
	 * assignment and the weight, where weight is added to the value of the
	 * Hashmap with domain value of X as the key.
	 */
	protected Distribution askWeight(BayesianNetwork bn, RandomVariable X, Assignment e, int n) {
		Distribution W = init(X);

		for (int i = 0; i < n; i++) {
			WeightedEntry ws = WeightedSample(bn, e);
			Object key = ws.getKey().get(X);
			double value = ws.getValue();
			value += W.get(key);
			W.put(key, value);
		}

		W.normalize();
		return W;
	}

	/**
	 * This method helps initialize a Distribution with all values inside domain
	 * of X so that the hash map inside the Distribution has 0 as value for
	 * every domain values of X as key.
	 */
	public Distribution init(RandomVariable X) {
		Distribution N = new Distribution(X);
		for (Object key : X.getDomain()) {
			N.put(key, 0);
		}
		return N;
	}

	/**
	 * Initialize a weight number to 1, and then go through the Random Variables
	 * inside Bayesian Network topologically, every time if the Random Variable
	 * is an evidence, times the probability of this evidence with weighted
	 * number, else did a normal random Sampling.
	 */
	protected WeightedEntry WeightedSample(BayesianNetwork bn, Assignment e) {
		double w = 1;
		Assignment x = new Assignment();

		for (RandomVariable i : bn.getVariableListTopologicallySorted()) {
			if (e.containsKey(i)) {
				x.put(i, e.get(i));
				w *= bn.getProb(i, x);
			} else {
				Random rand = new Random();
				double randProb = rand.nextDouble();
				double probSum = 0;
				for (Object value : i.getDomain()) {
					x.put(i, value);
					probSum += bn.getProb(i, x);
					if (probSum >= randProb) {
						break;
					}
				}
			}
		}

		WeightedEntry ws = new WeightedEntry(x, w);
		return ws;
	}

	/**
	 * A test case to test Likelihood Weighting
	 */
	public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException {
		LikelihoodWeighting lw = new LikelihoodWeighting(10000);
		XMLBIFParser parser = new XMLBIFParser();
		BayesianNetwork bn = parser.readNetworkFromFile("dog-problem.xml");
		Assignment e = new Assignment();
		e.put(bn.getVariableByName("hear-bark"), "true");
		e.put(bn.getVariableByName("bowel-problem"), "false");
		System.out.println(lw.ask(bn, bn.getVariableByName("light-on"), e));
	}

}
