/*
 * The MIT License
 *
 * Copyright 2014 Davide Mottin <mottin@disi.unitn.eu>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package eu.unitn.disi.db.gref.commands;

import de.parmol.graph.Graph;
import de.parmol.graph.GraphFactory;
import de.parmol.parsers.GraphParser;
import edu.psu.chemxseer.structure.factory.MyFactory;
import eu.unitn.disi.db.command.Command;
import eu.unitn.disi.db.command.CommandInput;
import eu.unitn.disi.db.command.ParametersNumber;
import eu.unitn.disi.db.command.exceptions.ExecutionException;
import eu.unitn.disi.db.gref.utils.Utils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Convert queries and reformulations into graphviz files
 * @author Davide Mottin <mottin@disi.unitn.eu>
 */
public class DrawResults extends Command {
    private String graphFile; 
    private String nodeFile; 
    private String edgeFile; 
    private String parserClass; 
    private String outputFolder; 
    private String separator;
    private String prefix;
    private boolean directed; 
    
    private static final String GSEP = "<EOG>";
    private static final String QSEP = "<EOQ>";
    private static final String EDGE_FORMAT = "\t\t\"%s\" %s \"%s\" [label=\"%s\"]"; 
    private static final String NODE_FORMAT = "\t\t\"%s\" [label=\"%s\"]";
    private static final String SUBGRAPH_TEMPLATE = 
            "	subgraph cluster_%s {\n" +
            "		label = \"%s\";\n" +
            "		/* Node definition */\n" +
            "%s\n" +
            "		/* Edge definition */\n" +
            "%s\n" +
            "	}";
    private static final String GRAPH_TEMPLATE = 
        "%s %s {\n" +
        "	/*rankdir = BT*/\n" +
        "	size=\"8,5\"\n" +
        "	fontname = \"Arial\"\n" +
        "	margin=0.0002\n" +
        "\n" +
        "	edge [\n" +
        "		fontname = \"Arial\"\n" +
        "	]\n" +
        "\n" +
        "	node [\n" +
        "		shape = rectangle, \n" +
        "		fontname = \"Arial\"\n" +
        "		fontsize = 15,\n" +
        "		width = 1.15,\n" +
        "		height = 0.58,\n" +
        "		style = \"rounded,filled\",\n" +
        "		fillcolor = white\n" +
        "	];\n" +
        "	/* Query and reformualtions */ " + 
        "       %s" +
        "}"; 
    
    
    @Override
    protected void execute() throws ExecutionException {
        Map<String,String> nodeMap, edgeMap; 
        String graphString, nodeLabel, edgeLabel, graphName, graphType, fileName; 
        String[] results; 
        String[] graphs, nodes, edges, dotGraphs; 
        Graph graph;
        Path out; 
        GraphParser parser; 
        GraphFactory factory; 
        String edgeType; 
        String graphId;
        int count = 0; 
        
        try {
            MyFactory.getDFSCoder();//The usual insane pre-load
            parser = (GraphParser) Class.forName(parserClass).newInstance(); 
            factory = GraphFactory.getFactory(parser.getDesiredGraphFactoryProperties());
            nodeMap = readNameFile(nodeFile, separator);
            edgeMap = readNameFile(edgeFile, separator);
            graphString = Utils.readFileToString(graphFile);
            
            results = graphString.split(QSEP);
            out = Paths.get(outputFolder);
            graphType = directed? "digraph" : "graph";
            try {
                Files.createDirectory(out);
            } catch (FileAlreadyExistsException ex) {
                warn("Output folder already exists, it might overwrite some file");
            }
            
            for (String result : results) {
                graphs = result.split(GSEP);
                count++;
                
                if (graphs.length < 2) {
                    error("Graph file must contain at least two graphs. We ignore the results");
                } else {
                    dotGraphs = new String[graphs.length];
                    edgeType = directed? "->" : "--";

                    for (int i = 0; i < graphs.length; i++) {
                        graph = parser.parse(graphs[i].trim(), factory);
                        nodes = new String[graph.getNodeCount()];
                        for (int ni = 0; ni < graph.getNodeCount(); ni++) {
                            nodeLabel = graph.getNodeLabel(ni) + "";
                            nodes[ni] = String.format(NODE_FORMAT, "" + i + ni, nodeMap.get(nodeLabel) != null? nodeMap.get(nodeLabel) : nodeLabel);
                        }
                        edges = new String[graph.getEdgeCount()];
                        for (int ei = 0; ei < graph.getEdgeCount(); ei++) {
                            edgeLabel = graph.getEdgeLabel(ei) + "";
                            edges[ei] = String.format(EDGE_FORMAT,
                                    "" + i + graph.getNodeA(ei),
                                    edgeType,
                                    "" + i + graph.getNodeB(ei), 
                                    edgeMap.get(edgeLabel) != null ? edgeMap.get(edgeLabel) : edgeLabel);
                        }
                        graphId = (i == 0? "q" : i + "");
                        graphName = (i == 0? "Q" : "R" + i);
                        dotGraphs[i] = createSubgraph(graphId, graphName, nodes, edges);
                    }
                    fileName = prefix + "_" + count + ".dot";
                    writeGraph(out.toString() + File.separator + fileName, graphType, dotGraphs);
                }
            }
        } catch (FileNotFoundException ex) { 
            throw new ExecutionException(ex);
        } catch (IOException ex) { 
            throw new ExecutionException(ex);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | ParseException ex) {
            Logger.getLogger(DrawResults.class.getName()).log(Level.SEVERE, null, ex);
        } 
    }

    @Override
    protected String commandDescription() {
        return "Convert a result file from Reformulate command into a set of graphs";
    }
    
    private Map<String,String> readNameFile(String nameFile, String separator) throws IOException, 
            ExecutionException 
    {
        Map<String,String> nameMap = new HashMap<>();
        String line = null; 
        String[] splittedLine; 
        int lineNo = 0; 
        try (BufferedReader nameReader = new BufferedReader(new FileReader(nameFile))) 
        {
            while ((line = nameReader.readLine()) != null) {
                lineNo++;
                line = line.trim();
                if (!"".equals(line)) {
                    splittedLine = line.split(separator);
                    if (splittedLine.length != 2) {
                        throw new ExecutionException("File %s has a problem at line %d", nameFile, lineNo);
                    }
                    if (nameMap.containsKey(splittedLine[0])) {
                        warn("Inserting duplicate key=%s", splittedLine[0]);
                    }
                    nameMap.put(splittedLine[0], splittedLine[1]);
                }
            }
        } catch (FileNotFoundException ex) {
            warn("Edge or node name file not found");
        }
        return nameMap;
    } 
    
    
    private void writeGraph(String fileName, String graphType, String[] graphs) throws IOException {
        try(BufferedWriter out = new BufferedWriter(new FileWriter(fileName))) {
            out.append(String.format(GRAPH_TEMPLATE, graphType, "g", Utils.join("\n\n", graphs)));
        }
    }
    
    private String createSubgraph(String id, String name, String[] nodes, String[] edges) {
        return String.format(SUBGRAPH_TEMPLATE, id, name, Utils.join("\n", nodes), Utils.join("\n", edges));  
    }

    
    
    @CommandInput(
        consoleFormat = "-g",
        defaultValue = "",
        mandatory = true,
        description = "file containing all graphs",
        parameters = ParametersNumber.TWO)
    public void setGraphFile(String graphFile) {
        this.graphFile = graphFile;
    }

    @CommandInput(
        consoleFormat = "-n",
        defaultValue = "",
        mandatory = false,
        description = "file containing the mapping between node labels and actual labels",
        parameters = ParametersNumber.TWO)
    public void setNodeFile(String nodeFile) {
        this.nodeFile = nodeFile;
    }

    @CommandInput(
        consoleFormat = "-e",
        defaultValue = "",
        mandatory = false,
        description = "file containing the mapping between edge labels and actual labels",
        parameters = ParametersNumber.TWO)
    public void setEdgeFile(String edgeFile) {
        this.edgeFile = edgeFile;
    }

    @CommandInput(
        consoleFormat = "-parser",
        defaultValue = "de.parmol.parsers.LineGraphParser",
        mandatory = false,
        description = "file containing the mapping between edge labels and actual labels",
        parameters = ParametersNumber.TWO)
    public void setParserClass(String parserClass) {
        this.parserClass = parserClass;
    }
    
    @CommandInput(
        consoleFormat = "-out",
        defaultValue = "OutputData",
        mandatory = false,
        description = "name of the folder to store the output",
        parameters = ParametersNumber.TWO)
    public void setOutputFolder(String outputFolder) {
        this.outputFolder = outputFolder;
    }

    @CommandInput(
        consoleFormat = "-sep",
        defaultValue = "\t",
        mandatory = false,
        description = "separator for the mapping files",
        parameters = ParametersNumber.TWO)
    public void setSeparator(String separator) {
        this.separator = separator;
    }

    @CommandInput(
        consoleFormat = "-d",
        defaultValue = "false",
        mandatory = false,
        description = "specify if the graph is directed or undirected",
        parameters = ParametersNumber.ONE)
    public void setDirected(boolean directed) {
        this.directed = directed;
    }
    
    @CommandInput(
        consoleFormat = "-p",
        defaultValue = "results",
        mandatory = false,
        description = "prefix for output files",
        parameters = ParametersNumber.TWO)
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }    
}
