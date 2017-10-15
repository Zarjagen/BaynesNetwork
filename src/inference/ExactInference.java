package inference;

import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import core.*;
import parser.*;

public class ExactInference implements Inferencer {

	/**
	 * The constructor of the class, it doesn't do anything but create an
	 * instance of the class
	 */
	public ExactInference() {
	}

	/**
	 * Override method on Inferencer, the ask method will return a Distribution
	 * for a Random Variable X, representing all values for X and the
	 * corresponding probability for each specific value.
	 */
	@Override
	public Distribution ask(BayesianNetwork bn, RandomVariable X, Assignment e) {
		Distribution Q = new Distribution(X);
		for (Object value : X.getDomain()) {
			Assignment eX = e.copy();
			eX.put(X, value);
			Q.put(value, Enumerate(bn, 0, eX));
		}
		Q.normalize();
		return Q;
	}

	/**
	 * Enumerate method takes a Bayesian Network and a specific Assignment e,
	 * which is the observed Random Variables. It uses the concept of Full Joint
	 * Distribution to calculate the result of P(X|e), so it lists all the
	 * Random Variables in the Baysian Network, and do summation on all
	 * unobserved evidence, which is every Random Variables instead of the ones
	 * inside Assignment e.
	 */
	public double Enumerate(BayesianNetwork bn, int index, Assignment e) {
		List<RandomVariable> vars = bn.getVariableListTopologicallySorted();
		// index here indicates where to start, if the start index is equal to
		// the size of the Random Variable inside Bayesian Network, we will just
		// return 1
		if (vars.size() == index) {
			return 1.0;
		}
		// if it is not the terminal state as above, find the Random Variable at
		// that index
		RandomVariable Y = vars.get(index);
		// if the Random Variable is actually an evidence variable, we can
		// directly times it with the following Random Variable because the
		// assignment is fixed for evidence
		if (e.get(Y) != null) {
			return bn.getProb(Y, e) * Enumerate(bn, index + 1, e);
		}
		// if the Random Variable Y is unobserved, then assign a value for Y,
		// and calculate as Y is observed. The difference is that we continue to
		// try to assign values for Y until all values inside domain of Y has
		// already been assigned.
		else {
			double sum = 0;
			for (Object value : Y.getDomain()) {
				Assignment eY = e.copy();
				eY.put(Y, value);
				sum += bn.getProb(Y, eY) * Enumerate(bn, index + 1, eY);
			}
			return sum;
		}
	}

	/**
	 * A test case to test Exact Inference
	 */
	public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException {
		XMLBIFParser parser = new XMLBIFParser();
		BayesianNetwork bn = parser.readNetworkFromFile("dog-problem.xml");
		ExactInference ei = new ExactInference();
		Assignment e = new Assignment();
		e.put(bn.getVariableByName("hear-bark"), "true");
		e.put(bn.getVariableByName("bowel-problem"), "false");
		System.out.println(ei.ask(bn, bn.getVariableByName("light-on"), e));
	}

}
