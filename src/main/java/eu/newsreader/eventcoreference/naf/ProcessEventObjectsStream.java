

/**
 * Created by filipilievski on 9/12/15.
 */

    package eu.newsreader.eventcoreference.naf;

    import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.vocabulary.RDF;
import org.apache.commons.codec.binary.Base64;
import org.apache.jena.atlas.web.auth.HttpAuthenticator;
import org.apache.jena.atlas.web.auth.SimpleAuthenticator;
import org.apache.jena.riot.RDFDataMgr;

import java.util.ArrayList;
import java.util.Iterator;

    /**
     * Created with IntelliJ IDEA.
     * User: Filip
     * Date: 09/12/15
     * Time: 3:00 PM
     * To change this template use File | Settings | File Templates.
     */
    public class ProcessEventObjectsStream {



    /*
        @TODO
        1. proper reference to the ontologies (even if not there yet)
        7. parametrize the module to get high-precision or high-recall TriG
        8. entities that are not part of events are not in the output
     */


        static final String USAGE = "This program processes NAF files and stores binary objects for events with all related data in different object files based on the event type and the date\n" +
                "The program has the following arguments:\n" +
                "--naf-folder               <path>   <Folder with the NAF files to be processed. Reads NAF files recursively>\n" +
                "--extension                <string> <File extension to select the NAF files .>\n" +
                "--project                  <string> <The name of the project for creating URIs>\n" +
                "--non-entities                      <If used, additional FrameNet roles and non-entity phrases are included>\n" +
                "--contextual-frames        <path>   <Path to a file with the FrameNet frames considered contextual>\n" +
                "--source-frames            <path>   <Path to a file with the FrameNet frames considered source>\n" +
                "--grammatical-frames       <path>   <Path to a file with the FrameNet frames considered grammatical>\n" +
                "--source-data              <path>   <Path to LexisNexis meta data on owners and authors to enrich the provenance>\n" +
                "--verbose                           <Representation of mentions is extended with token ids, terms ids and sentence number\n" +
                "--ili-uri                           <If used, the ILI-identifiers are used to represents events. This is necessary for cross-lingual extraction>\n" +
                "--contextual-match-type    <path>   <Indicates what is used to match events across resources. Default value is \"ILILEMMA\". Values:\"LEMMA\", \"ILI\", \"ILILEMMA\">\n" +
                "--contextual-lcs                    <Use lowest-common-subsumers. Default value is ON.>\n" +
                "--contextual-needed-roles  <path>   <String with PropBank roles for which there must be a match, e.g. \"a1,a2,a3,a4\">\n" +
                "--contextual-other-roles   <path>   <String with PropBank roles for which there should be a match (match only if role exists), e.g. \"a1,a2,a3,a4\">\n" +
                "--source-match-type        <path>   <Indicates what is used to match events across resources. Default value is \"ILILEMMA\". Values:\"LEMMA\", \"ILI\", \"ILILEMMA\">\n" +
                "--source-lcs                        <Use lowest-common-subsumers. Default value is OFF.>\n" +
                "--source-needed-roles      <path>   <String with PropBank roles for which there must be a match, e.g. \"a1,a2,a3,a4\">\n" +
                "--source-other-roles       <path>   <String with PropBank roles for which there should be a match (match only if role exists), e.g. \"a1,a2,a3,a4\">\n" +
                "--grammatical-match-type   <path>   <Indicates what is used to match events across resources. Default value is \"LEMMA\". Values:\"LEMMA\", \"ILI\", \"ILILEMMA\">\n" +
                "--grammatical-lcs                    <Use lowest-common-subsumers. Default value is OFF.>\n" +
                "--grammatical-needed-roles <path>   <String with PropBank roles for which there must be a match, e.g. \"a1,a2,a3,a4\">\n" +
                "--grammatical-other-roles  <path>   <String with PropBank roles for which there should be a match (match only if role exists), e.g. \"a1,a2,a3,a4\">\n" +
                "--future-match-type        <path>   <Indicates what is used to match events across resources. Default value is \"LEMMA\". Values:\"LEMMA\", \"ILI\", \"ILILEMMA\">\n" +
                "--future-lcs                        <Use lowest-common-subsumers. Default value is OFF.>\n" +
                "--stream-chunk-size        <int>    <The number of NAF files which will be processed in a single stream bulk>\n" +
                "--recent-span              <int>    <Amount of past days which are still considered recent and are treated differently>\n";

        static public String filename = "";
        static public String projectName = "cars";

        static public String CONTEXTUALMATCHTYPE = "ILILEMMA";
        static public boolean CONTEXTUALLCS = true;

        static public String SOURCEMATCHTYPE = "ILILEMMA";
        static public boolean SOURCELCS = false;

        static public String GRAMMATICALMATCHTYPE = "LEMMA";
        static public boolean GRAMMATICALLCS = false;

        static public String FUTUREMATCHTYPE = "LEMMA";
        static public boolean FUTURELCS = false;

        static public ArrayList<String> contextualNeededRoles = new ArrayList<String>();
        static public ArrayList<String> sourceNeededRoles = new ArrayList<String>();
        static public ArrayList<String> grammaticalNeededRoles = new ArrayList<String>();

        static boolean DEBUG = false;

        static public int recentDays = 0;

        static public String done = "";

        final static String serviceEndpoint = "https://knowledgestore2.fbk.eu/nwr/cars3/sparql";
        public static String user = "nwr_partner";
        public static String pass = "ks=2014!";
        public static String authStr = user + ":" + pass;
        public static byte[] authEncoded = Base64.encodeBase64(authStr.getBytes());

        public static final String NL = System.getProperty("line.separator");

        static public void main(String[] args) {

            // 1. CLUSTER

            if (args.length == 0) {
                System.out.println(USAGE);
                System.out.println("NOW RUNNING WITH DEFAULT SETTINGS");
                //  return;
            }

            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if (arg.equals("--filename") && args.length > (i + 1)) {
                    filename = args[i + 1];
                } else if (arg.equals("--project") && args.length > (i + 1)) {
                    projectName = args[i + 1];
                } else if (arg.equals("--contextual-match-type") && args.length > (i + 1)) {
                    CONTEXTUALMATCHTYPE = args[i + 1];
                } else if (arg.equals("--contextual-lcs")) {
                    CONTEXTUALLCS = true;
                } else if (arg.equals("--contextual-roles") && args.length > (i + 1)) {
                    String[] fields = args[i + 1].split(",");
                    for (int j = 0; j < fields.length; j++) {
                        String field = fields[j].trim();
                        contextualNeededRoles.add(field);
                    }
                } else if (arg.equals("--source-match-type") && args.length > (i + 1)) {
                    SOURCEMATCHTYPE = args[i + 1];
                } else if (arg.equals("--source-lcs")) {
                    SOURCELCS = true;
                } else if (arg.equals("--source-roles") && args.length > (i + 1)) {
                    String[] fields = args[i + 1].split(",");
                    for (int j = 0; j < fields.length; j++) {
                        String field = fields[j].trim();
                        sourceNeededRoles.add(field);
                    }
                } else if (arg.equals("--grammatical-match-type") && args.length > (i + 1)) {
                    GRAMMATICALMATCHTYPE = args[i + 1];
                } else if (arg.equals("--grammatical-lcs")) {
                    GRAMMATICALLCS = true;
                } else if (arg.equals("--grammatical-roles") && args.length > (i + 1)) {
                    String[] fields = args[i + 1].split(",");
                    for (int j = 0; j < fields.length; j++) {
                        String field = fields[j].trim();
                        grammaticalNeededRoles.add(field);
                    }
                } else if (arg.equals("--future-match-type") && args.length > (i + 1)) {
                    FUTUREMATCHTYPE = args[i + 1];
                } else if (arg.equals("--future-lcs")) {
                    FUTURELCS = true;
                } else if (arg.equals("--recent-span")) {
                    recentDays = Integer.parseInt(args[i + 1]);
                }
            }



            Dataset ds = TDBFactory.createDataset();
            RDFDataMgr.read(ds, filename);
            Model m = ds.getNamedModel("http://www.newsreader-project.eu/instances");

            DatasetGraph g = ds.asDatasetGraph();

            //System.out.println(m.size());

            Node lemmaNode = NodeFactory.createURI("http://www.w3.org/2000/01/rdf-schema#label");
            Node timeNode = NodeFactory.createURI("http://semanticweb.cs.vu.nl/2009/11/sem/hasTime");
            Node typeNode = NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
            Node instantNode = NodeFactory.createURI("http://www.w3.org/TR/owl-time#Instant");
            Node intervalNode = NodeFactory.createURI("http://www.w3.org/TR/owl-time#Interval");
            Node specificTimeNode = NodeFactory.createURI("http://www.w3.org/TR/owl-time#inDateTime");


            final String prolog1 = "PREFIX sem: <http://semanticweb.cs.vu.nl/2009/11/sem/>";
            final String prolog2 = "PREFIX rdf: <" + RDF.getURI() + ">";
            final String prolog3 = "PREFIX nwr: <http://www.newsreader-project.eu/> ";

            final String allEventsQuery = "SELECT distinct ?ev WHERE { ?ev a <http://semanticweb.cs.vu.nl/2009/11/sem/Event> }";

            final Query query = QueryFactory.create(allEventsQuery);

            // Create a single execution of this query, apply to a model
            // which is wrapped up as a Dataset
            final QueryExecution qexec = QueryExecutionFactory.create(query, m);
            // Or QueryExecutionFactory.create(queryString, model) ;


            try {

                // Assumption: it’s a SELECT query.
                final ResultSet rs = qexec.execSelect();
                // The order of results is undefined.
                for (; rs.hasNext(); ) {
                    final QuerySolution rb = rs.nextSolution();
                    // Get title - variable names do not include the ’?’
                    final RDFNode eventId = rb.get("ev");
                    final Resource z = rb.getResource("ev");

                    // Now get the details for each event

//                ?ev rdf:type ?type . FILTER (regex(STR(?type), "^http://www.newsreader-project.eu/ontologies/.*Event"))

                    String eventDetailsQuery = prolog1 + NL + prolog2 + NL + prolog3 + NL + "SELECT ?type WHERE { <" + eventId + "> rdf:type ?type }";

                    Query evQuery = QueryFactory.create(eventDetailsQuery);

                    String MATCHTYPE = "";
                    // Process the event now ;)
                    String sparqlQuery = "SELECT distinct ?ev WHERE { ?ev a <http://semanticweb.cs.vu.nl/2009/11/sem/Event> , ";
                    ArrayList<String> neededRoles = new ArrayList<String>();
                    String nwrtype = "";
                    ArrayList<String> myILIs = new ArrayList<String>();
                    ArrayList<String> myFrames = new ArrayList<String>();

                    // Create a single execution of this query, apply to a model
                    // which is wrapped up as a Dataset
                    QueryExecution evQexec = QueryExecutionFactory.create(evQuery, m);

                    try {
                        // Assumption: it’s a SELECT query.
                        ResultSet evrs = evQexec.execSelect();
                        // The order of results is undefined.
                        for (; evrs.hasNext(); ) {
                            QuerySolution evrb = evrs.nextSolution();
                            // Get title - variable names do not include the ’?’
                            String rdfType = evrb.get("type").toString();

                            if (rdfType.equals("http://www.newsreader-project.eu/ontologies/grammaticalEvent")) {
                                nwrtype = rdfType;
                                MATCHTYPE = GRAMMATICALMATCHTYPE;
                                neededRoles = grammaticalNeededRoles;
                            } else if (rdfType.equals("http://www.newsreader-project.eu/ontologies/sourceEvent")) {
                                nwrtype = rdfType;
                                MATCHTYPE = SOURCEMATCHTYPE;
                                neededRoles = sourceNeededRoles;
                            } else if (rdfType.equals("http://www.newsreader-project.eu/ontologies/contextualEvent")) {
                                nwrtype = rdfType;
                                MATCHTYPE = CONTEXTUALMATCHTYPE;
                                neededRoles = contextualNeededRoles;
                            } else if (rdfType.contains("http://www.newsreader-project.eu/ontologies/framenet/")) {
                                myFrames.add(rdfType);
                            } else if (!rdfType.equals("http://semanticweb.cs.vu.nl/2009/11/sem/Event")) { // wordnet ILIs
                                myILIs.add(rdfType);
                            }

                        }
                    } finally {
                        evQexec.close();
                    }


                    if (!myFrames.isEmpty()) {
                        for (int i = 0; i < myFrames.size(); i++) {
                            String et = myFrames.get(i);
                            sparqlQuery += "<" + et + "> ";
                            if (i < myFrames.size() - 1)
                                sparqlQuery += ", ";
                            else
                                sparqlQuery += ". ";
                        }
                    } else {
                        sparqlQuery += "<" + nwrtype + "> . ";
                    }

                    Node eventNode = NodeFactory.createURI(eventId.toString());

                    if (!neededRoles.isEmpty()) {
                        boolean skip = false;
                        for (int i = 0; i < neededRoles.size(); i++) {
                            String role = neededRoles.get(i);
                            Node roleNode = NodeFactory.createURI("http://www.newsreader-project.eu/ontologies/propbank/" + role.toUpperCase());

                            ArrayList<Node> allActors = new ArrayList<Node>();
                            for (Iterator<Quad> iter = g.find(null, eventNode, roleNode, null); iter.hasNext(); ) {
                                Quad q = iter.next();
                                allActors.add(q.asTriple().getObject());
                            }
                            if (allActors.isEmpty()) {
                                break;
                            } else if (allActors.size() == 1) {
                                sparqlQuery += "?ev <http://www.newsreader-project.eu/ontologies/propbank/" + role.toUpperCase() + "> <" + allActors.get(0) + "> . ";
                            } else {
                                String rolevar = "?" + role;

                                String filter = " { ?ev <http://www.newsreader-project.eu/ontologies/propbank/" + role.toUpperCase() + "> " + rolevar + " . FILTER ( " + rolevar + " IN (";
                                for (int j = 0; j < allActors.size(); j++) {
                                    filter += allActors.get(j) + ", ";
                                }
                                sparqlQuery += filter.substring(0, filter.length() - 2) + ") ) . ";
                            }
                        }
                        if (skip)
                            continue;
                    }
                    // Roles done!

                    // Match Type now:

                    if (MATCHTYPE.equals("ILILEMMA") && myILIs.size() > 0) {
                        if (myILIs.size() == 1) {
                            sparqlQuery += "?ev a <" + myILIs.get(0) + "> . ";
                        } else {
                            // TODO: The following few lines should be uncommented once the WN ILIs in this format exist in cars3.
                            /*
                            String iliFilter = "?ev a ?ili . FILTER ( ?ili IN (";
                            for (int i = 0; i < myILIs.size(); i++) {
                                iliFilter += "<" + myILIs.get(i) + "> , ";
                            }
                            sparqlQuery += iliFilter.substring(0, iliFilter.length() - 2) + ") ) . ";
                            */
                        }

                    } else {

                        ArrayList<Node> allLemmas = new ArrayList<Node>();
                        for (Iterator<Quad> iter = g.find(null, eventNode, lemmaNode, null); iter.hasNext(); ) {
                            Quad q = iter.next();
                            allLemmas.add(q.asTriple().getObject());
                        }
                        if (!allLemmas.isEmpty()) {
                            if (allLemmas.size() == 1)
                                sparqlQuery += "?ev <http://www.w3.org/2000/01/rdf-schema#label> " + allLemmas.get(0) + " . ";
                            else {
                                String lemmaFilter = " ?ev <http://www.w3.org/2000/01/rdf-schema#label> ?lbl . FILTER ( ?lbl IN (";
                                for (int i = 0; i < allLemmas.size(); i++) {
                                    lemmaFilter += allLemmas.get(i) + ", ";
                                }
                                sparqlQuery += lemmaFilter.substring(0, lemmaFilter.length() - 2) + ") ) . ";
                            }
                        }
                    }


                    ArrayList<Node> allTimes = new ArrayList<Node>();
                    for (Iterator<Quad> iter = g.find(null, eventNode, timeNode, null); iter.hasNext(); ) {
                        Quad q = iter.next();
                        allTimes.add(q.asTriple().getObject());
                    }

                    if (allTimes.size() == 1) {
                        Node tmx = allTimes.get(0);
                        if (g.contains(null, tmx, typeNode, instantNode)) { // One Instant
                            sparqlQuery += "?ev <http://semanticweb.cs.vu.nl/2009/11/sem/hasTime> ?t . ?t a <http://www.w3.org/TR/owl-time#Instant> . ";
                            for (Iterator<Quad> iter = g.find(null, tmx, specificTimeNode, null); iter.hasNext(); ) {
                                Quad q = iter.next();
                                sparqlQuery += "?t <http://www.w3.org/TR/owl-time#inDateTime> <" + q.asTriple().getObject() + "> . ";
                            }
                        } else { // One Interval

                            String intervalQuery = "SELECT ?begin ?end WHERE { <" + tmx + ">  <http://www.w3.org/TR/owl-time#hasBeginning> ?begin ; <http://www.w3.org/TR/owl-time#hasEnd> ?end . }";

                            Query inQuery = QueryFactory.create(intervalQuery);

                            // Create a single execution of this query, apply to a model
                            // which is wrapped up as a Dataset
                            QueryExecution inQexec = QueryExecutionFactory.create(inQuery, m);

                            try {
                                // Assumption: it’s a SELECT query.
                                ResultSet inrs = inQexec.execSelect();
                                // The order of results is undefined.
                                for (; inrs.hasNext(); ) {
                                    QuerySolution evrb = inrs.nextSolution();
                                    // Get title - variable names do not include the ’?’
                                    String begin = evrb.get("begin").toString();
                                    String end = evrb.get("end").toString();

                                    String unionQuery = "{ ?ev <http://semanticweb.cs.vu.nl/2009/11/sem/hasTime> ?t . ?t a <http://www.w3.org/TR/owl-time#Interval> . ?t <http://www.w3.org/TR/owl-time#hasBeginning> <" + begin + "> ; <http://www.w3.org/TR/owl-time#hasEnd> <" + end + "> . } ";
                                    unionQuery += "UNION ";
                                    unionQuery += "{ ?ev <http://semanticweb.cs.vu.nl/2009/11/sem/hasEarliestBeginTimeStamp> ?t1 . ?t1 a <http://www.w3.org/TR/owl-time#Instant> . ?t1 <http://www.w3.org/TR/owl-time#inDateTime> <" + begin + "> . ?ev <http://semanticweb.cs.vu.nl/2009/11/sem/hasEarliestEndTimeStamp> ?t2 . ?t2 a <http://www.w3.org/TR/owl-time#Instant> . ?t2 <http://www.w3.org/TR/owl-time#inDateTime> <" + end + "> . } ";
                                    sparqlQuery += unionQuery;
                                }
                            } finally {
                                inQexec.close();
                            }
                        }
                    } else if (allTimes.size() == 0) {

                        String multiPointQuery = "SELECT ?begin ?end WHERE { { <" + eventId + ">  <http://semanticweb.cs.vu.nl/2009/11/sem/hasEarliestBeginTimeStamp> ?t1 . ?t1 <http://www.w3.org/TR/owl-time#inDateTime> ?begin . OPTIONAL { <" + eventId + ">  <http://semanticweb.cs.vu.nl/2009/11/sem/hasEarliestEndTimeStamp> ?t2 . ?t2 <http://www.w3.org/TR/owl-time#inDateTime> ?end . } } UNION ";
                        multiPointQuery += "{ <" + eventId + ">  <http://semanticweb.cs.vu.nl/2009/11/sem/hasEarliestEndTimeStamp> ?t2 . ?t2 <http://www.w3.org/TR/owl-time#inDateTime> ?end . OPTIONAL { <" + eventId + ">  <http://semanticweb.cs.vu.nl/2009/11/sem/hasEarliestBeginTimeStamp> ?t1 . ?t1 <http://www.w3.org/TR/owl-time#inDateTime> ?begin . } } }";

                        Query mpQuery = QueryFactory.create(multiPointQuery);

                        // Create a single execution of this query, apply to a model
                        // which is wrapped up as a Dataset
                        QueryExecution mpQexec = QueryExecutionFactory.create(mpQuery, m);

                        try {
                            // Assumption: it’s a SELECT query.
                            ResultSet mprs = mpQexec.execSelect();
                            // The order of results is undefined.
                            for (; mprs.hasNext(); ) {
                                QuerySolution mprb = mprs.nextSolution();
                                // Get title - variable names do not include the ’?’
                                String begin = mprb.get("begin").toString();
                                String end = mprb.get("end").toString();
                                if (!begin.isEmpty() && !end.isEmpty()) {
                                    String unionQuery = "{ ?ev <http://semanticweb.cs.vu.nl/2009/11/sem/hasTime> ?t . ?t a <http://www.w3.org/TR/owl-time#Interval> . ?t <http://www.w3.org/TR/owl-time#hasBeginning> <" + begin + "> ; <http://www.w3.org/TR/owl-time#hasEnd> <" + end + "> . } ";
                                    unionQuery += "UNION ";
                                    unionQuery += "{ ?ev <http://semanticweb.cs.vu.nl/2009/11/sem/hasEarliestBeginTimeStamp> ?t1 . ?t1 a <http://www.w3.org/TR/owl-time#Instant> . ?t1 <http://www.w3.org/TR/owl-time#inDateTime> <" + begin + "> . ?ev <http://semanticweb.cs.vu.nl/2009/11/sem/hasEarliestEndTimeStamp> ?t2 . ?t2 a <http://www.w3.org/TR/owl-time#Instant> . ?t2 <http://www.w3.org/TR/owl-time#inDateTime> <" + end + "> . } ";
                                    sparqlQuery += unionQuery;
                                } else if (!begin.isEmpty()) {
                                    sparqlQuery += "{ ?ev <http://semanticweb.cs.vu.nl/2009/11/sem/hasEarliestBeginTimeStamp> ?t1 . ?t1 a <http://www.w3.org/TR/owl-time#Instant> . ?t1 <http://www.w3.org/TR/owl-time#inDateTime> <" + begin + "> . } ";
                                } else {
                                    sparqlQuery += "{ ?ev <http://semanticweb.cs.vu.nl/2009/11/sem/hasEarliestEndTimeStamp> ?t2 . ?t2 a <http://www.w3.org/TR/owl-time#Instant> . ?t2 <http://www.w3.org/TR/owl-time#inDateTime> <" + end + "> . } ";
                                }
                            }
                        } finally {
                            mpQexec.close();
                        }

                    } else {
                        continue;
                    }


                    sparqlQuery += "}";


                    /////////////////////////////////////////////////////////////////////////////////////////////////////
                    /////////////////////////////////////////////////////////////////////////////////////////////////////
                    /////////////////////////////////////////////////////////////////////////////////////////////////////
                    /////////////////////////////////////////////////////////////////////////////////////////////////////
                    /////////////////////////////////////////////////////////////////////////////////////////////////////
                    /////////////////////////////////////////////////////////////////////////////////////////////////////
                    /////////////////////////////////// MATCHING SPARQL /////////////////////////////////////////////////
                    /////////////////////////////////// MATCHING SPARQL /////////////////////////////////////////////////
                    /////////////////////////////////// MATCHING SPARQL /////////////////////////////////////////////////
                    /////////////////////////////////////////////////////////////////////////////////////////////////////
                    /////////////////////////////////////////////////////////////////////////////////////////////////////
                    /////////////////////////////////////////////////////////////////////////////////////////////////////
                    /////////////////////////////////////////////////////////////////////////////////////////////////////
                    /////////////////////////////////////////////////////////////////////////////////////////////////////
                    /////////////////////////////////////////////////////////////////////////////////////////////////////


                    HttpAuthenticator authenticator = new SimpleAuthenticator(user, pass.toCharArray());
                    QueryExecution x = QueryExecutionFactory.sparqlService(serviceEndpoint, sparqlQuery, authenticator);
                    ResultSet resultset = x.execSelect();
                    while (resultset.hasNext()) {
                        System.out.println("<" + eventId + "> <http://www.w3.org/2002/07/owl#sameAs> <" + resultset.nextSolution().get("ev") + "> . \n");
                    }

                }

            } finally {
                // QueryExecution objects should be closed to free any system
                // resources
                qexec.close();
            }


        }
    }
