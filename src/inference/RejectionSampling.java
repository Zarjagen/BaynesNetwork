package inference;

import java.io.IOException;
import java.util.Random;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import core.*;
import parser.XMLBIFParser;

public class RejectionSampling implements Inferencer {

	protected int n = 0;

	/**
	 * The constructor of the class, it creates an instance of the class and
	 * takes a parameter n to determine the size of the samples to generate
	 */
	public RejectionSampling(int n) {
		this.n = n;
	}

	/**
	 * Override method, directly pointed to customized method askRejection,
	 * which takes one additional parameter n
	 */
	@Override
	public Distribution ask(BayesianNetwork bn, RandomVariable X, Assignment e) {
		return askRejection(bn, X, e, n);
	}

	/**
	 * Implementing the main idea of Rejection Sampling, which tests whether a
	 * random sample is consistent with the evidence first, and if it is, add
	 * one to that value of Query variable, and normalize after tried n times.
	 */
	public Distribution askRejection(BayesianNetwork bn, RandomVariable X, Assignment e, int n) {
		Distribution N = init(X);
		for (int i = 0; i < n; i++) {
			Assignment x = PriorSample(bn);
			if (consistent(x, e)) {
				Object key = x.get(X);
				double value = N.get(key);
				N.put(key, value + 1);
			}
		}
		N.normalize();
		return N;
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
	 * Test whether two Assignments are consistent, the second parameter is the
	 * observed Assignment, so the second parameter always has smaller size.
	 * Therefore, we go through the Random Variable in e and tests whether the
	 * domain value for that Random Variable in e are the same as that in x.
	 */
	private boolean consistent(Assignment x, Assignment e) {
		for (RandomVariable key : e.keySet()) {
			if (!x.get(key).equals(e.get(key))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Create a random sample according to the probability of each domain values
	 * inside the Random Variable, since find the probability for one Random
	 * Variable from CPT in Bayesian Network requires to know it's parents'
	 * domain values, we go through the Random Variable list topologically.
	 */
	public Assignment PriorSample(BayesianNetwork bn) {
		Assignment x = new Assignment();
		for (RandomVariable i : bn.getVariableListTopologicallySorted()) {
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
		return x;
	}

	/**
	 * A test case to test Rejection Sampling
	 */
	public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException {
		RejectionSampling rs = new RejectionSampling(10000);
		XMLBIFParser parser = new XMLBIFParser();
		BayesianNetwork bn = parser.readNetworkFromFile("dog-problem.xml");
		Assignment e = new Assignment();
		e.put(bn.getVariableByName("hear-bark"), "true");
		e.put(bn.getVariableByName("bowel-problem"), "false");
		System.out.println(rs.ask(bn, bn.getVariableByName("light-on"), e));
	}
}
