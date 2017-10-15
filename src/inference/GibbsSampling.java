package inference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import core.*;
import parser.XMLBIFParser;

public class GibbsSampling implements Inferencer {

	protected int n = 0;

	/**
	 * The constructor of the class, it creates an instance of the class and
	 * takes a parameter n to determine the size of the samples to generate
	 */
	public GibbsSampling(int n) {
		this.n = n;
	}

	/**
	 * Override method, directly pointed to customized method GibbsAsk, which
	 * takes one additional parameter n
	 */
	@Override
	public Distribution ask(BayesianNetwork bn, RandomVariable X, Assignment e) {
		int n = 1000;
		return GibbsAsk(bn, X, e, n);
	}

	/**
	 * Assignment for all Random Variables are initiated and updated each time
	 * according to the probability calculated from Markov Blanket.
	 */
	public Distribution GibbsAsk(BayesianNetwork bn, RandomVariable X, Assignment e, int n) {
		Distribution N = init(X);
		List<RandomVariable> Z = nonEvidence(bn, e);
		Assignment x = initialState(Z, bn, e);

		for (int j = 0; j < n; j++) {
			for (RandomVariable i : Z) {
				givenMarkovBlanket(bn, i, x);
				Object key = x.get(X);
				double value = N.get(key);
				N.put(key, value + 1);
			}
		}

		N.normalize();
		return N;
	}

	/**
	 * Change one of the Random Variable (that is, "i") value inside Assignment
	 * x by sampling this Random Variable with its Markov Blanket
	 */
	public void givenMarkovBlanket(BayesianNetwork bn, RandomVariable i, Assignment x) {
		Distribution N = init(i);
		Assignment temp = x.copy();

		for (Object obj : i.getDomain()) {
			temp.put(i, obj);
			double prob = bn.getProb(i, temp);
			for (RandomVariable child : bn.getChildren(i)) {
				prob *= bn.getProb(child, temp);
			}
			N.put(obj, prob);
		}
		N.normalize();

		Random rand = new Random();
		double randProb = rand.nextDouble();
		double probSum = 0;
		for (Object obj : i.getDomain()) {
			x.put(i, obj);
			probSum += N.get(obj);
			if (probSum >= randProb) {
				return;
			}
		}
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
	 * Random Sampling on the given Random Variables
	 */
	protected Assignment initialState(List<RandomVariable> Z, BayesianNetwork bn, Assignment e) {
		Assignment x = e.copy();
		for (RandomVariable i : Z) {
			Random rand = new Random();
			int randInt = rand.nextInt(i.getDomain().size());
			x.put(i, i.getDomain().get(randInt));
		}
		return x;
	}

	/**
	 * Find all the Non-Evidence Random Variables inside a Bayesian Network
	 * given all evidences.
	 */
	protected List<RandomVariable> nonEvidence(BayesianNetwork bn, Assignment e) {
		List<RandomVariable> Z = new ArrayList<RandomVariable>();
		for (RandomVariable i : bn.getVariableList()) {
			if (!e.containsKey(i)) {
				Z.add(i);
			}
		}
		return Z;
	}

	/**
	 * A test case to test Gibbs Sampling
	 */
	public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException {
		GibbsSampling gs = new GibbsSampling(10000);
		XMLBIFParser parser = new XMLBIFParser();
		BayesianNetwork bn = parser.readNetworkFromFile("dog-problem.xml");
		Assignment e = new Assignment();
		e.put(bn.getVariableByName("hear-bark"), "true");
		e.put(bn.getVariableByName("bowel-problem"), "false");
		System.out.println(gs.ask(bn, bn.getVariableByName("light-on"), e));
	}

}
