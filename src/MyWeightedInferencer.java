import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import core.Assignment;
import core.BayesianNetwork;
import core.Distribution;
import core.RandomVariable;
import inference.LikelihoodWeighting;
import parser.XMLBIFParser;

public class MyWeightedInferencer {
	public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException {
		XMLBIFParser parser = new XMLBIFParser();
		int sampleNumber = Integer.parseInt(args[0]);
		BayesianNetwork bn = parser.readNetworkFromFile(args[1]);
		RandomVariable X = bn.getVariableByName(args[2]);
		Assignment e = new Assignment();
		for (int i = 3; i < args.length; i += 2) {
			if (i + 1 > args.length) {
				throw new RuntimeException("Wrong argument numbers!");
			}
			e.put(bn.getVariableByName(args[i]), args[i + 1]);
		}

		LikelihoodWeighting lw = new LikelihoodWeighting(sampleNumber);
		Distribution Q = lw.ask(bn, X, e);

		String fileName = process(args[1]);
		System.out.println("--------------- File: " + fileName + " ---------------");
		System.out.println("Query Variable X: " + X);
		System.out.println("Evidence e: " + e);
		System.out.println("P(" + X + "|" + e + ") = " + Q);
		System.out.println("---------- End of the Liklihood Weighting! ----------");
	}

	public static String process(String url) {
		String fileName = url;
		if (fileName.contains("/")) {
			fileName = fileName.substring(url.lastIndexOf('/'));
		}
		fileName = fileName.substring(0, fileName.lastIndexOf('.'));
		return fileName;
	}
}
