package edu.cmu.cs.lti.oracles.jointsynsem;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import edu.cmu.cs.lti.oracles.datastructs.Conll;
import edu.cmu.cs.lti.oracles.datastructs.ConllFileIO;
import edu.cmu.cs.lti.oracles.datastructs.SemHead;
import edu.cmu.cs.lti.oracles.datastructs.SynSemAnalysis;

public class OracleRunner {

    @Parameter(names = "-inp", description = "conll file in 2008/09 format")
    public static String input = "proj.en.conll2009.dev.conll";

    @Parameter(names = "-print", description = "print parser states and transition types", arity = 1)
    public static boolean printStates = true;

    @Parameter(names = "-conll08", description = "print all lemmas", arity = 1)
    public static boolean conll08 = false;

    public static boolean debugModeOn = false;

    public static void debug(int ex, State finalState, SynSemAnalysis analysis, Conll conll) {
        System.out.println("Sent. No." + ex + " length = " + conll.size
                + "\n_______________");

        System.out.println("Syn Gold\n----");
        System.out.println(analysis.getDepTree().toString());
        System.out.println("Syn Predicted\n----");
        for (int child : finalState.labeledArcs.keySet()) {
            System.out.println(finalState.labeledArcs.get(child).toString() + " "
                    + child);
        }
        System.out.println("Sem Gold\n----");
        System.out.println(analysis.getSemTree().toString());
        System.out.println("Sem Predicted\n----");
        for (int child : finalState.labeledSemArcs.keySet()) {
            for (SemHead sh : finalState.labeledSemArcs.get(child)) {
                System.out.println(sh.toString() + " " + child);
            }
        }
    }

    public static void main(String[] args) {
        new JCommander(new OracleRunner(), args);

        ConllFileIO reader = new ConllFileIO();
        ImmutableList<Conll> conlls = reader.readConllFile(input, conll08);

        List<SynSemAnalysis> goldAnalyses = Lists.newArrayList();

        int ex = 0;
        int numMistakes = 0;
        int numSelfArcs = 0;
        int numTotalArcs = 0;

        for (Conll conll : conlls) {
            if (ex >= 0) {
                SynSemAnalysis analysis = new SynSemAnalysis(conll);
                goldAnalyses.add(analysis);

                numSelfArcs += analysis.selfArcs;
                numTotalArcs += analysis.semArcs;

                State initialState = new State(analysis);
                Oracle oracle = new Oracle(initialState);
                State finalState = oracle.getAllTransitions(analysis);

                boolean result = OracleHelper.checkIfCorrect(finalState, analysis);
                if (result == false) {
                    ++numMistakes;
                }
                if (debugModeOn) {
                    debug(ex, finalState, analysis, conll);
                }
            }
            ++ex;
        }
        NumberFormat formatter = new DecimalFormat("#0.00");

        System.err.println("Num mistakes = " + numMistakes + " = "
                + formatter.format(numMistakes * 100.0 / conlls.size()) + "%");
        System.err.println("% self arcs = " + formatter.format(numSelfArcs * 100.0 / numTotalArcs));
    }
}
