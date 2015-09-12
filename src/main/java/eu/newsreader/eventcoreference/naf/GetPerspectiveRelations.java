package eu.newsreader.eventcoreference.naf;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.tdb.TDBFactory;
import eu.kyotoproject.kaf.KafEvent;
import eu.kyotoproject.kaf.KafParticipant;
import eu.kyotoproject.kaf.KafSaxParser;
import eu.newsreader.eventcoreference.objects.NafMention;
import eu.newsreader.eventcoreference.objects.PerspectiveObject;
import eu.newsreader.eventcoreference.objects.SemActor;
import eu.newsreader.eventcoreference.objects.SemObject;
import eu.newsreader.eventcoreference.output.JenaSerialization;
import eu.newsreader.eventcoreference.util.FrameTypes;
import eu.newsreader.eventcoreference.util.RoleLabels;
import eu.newsreader.eventcoreference.util.Util;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;

/**
 * Created by piek on 11/03/15.
 */
public class GetPerspectiveRelations {


        static public void main (String[] args) {
            String comFrameFile = "/Code/vu/newsreader/EventCoreference/newsreader-vm/vua-eventcoreference_v2_2014/resources/communication.txt";
            String contextualFrameFile = "/Code/vu/newsreader/EventCoreference/newsreader-vm/vua-eventcoreference_v2_2014/resources/contextual.txt";
            String grammaticalFrameFile = "/Code/vu/newsreader/EventCoreference/newsreader-vm/vua-eventcoreference_v2_2014/resources/grammatical.txt";
            Vector<String> communicationVector = Util.ReadFileToStringVector(comFrameFile);
            Vector<String> grammaticalVector = Util.ReadFileToStringVector(grammaticalFrameFile);
            Vector<String> contextualVector = Util.ReadFileToStringVector(contextualFrameFile);

            String project = "test";
            String nafFilePath = "";
            //nafFilePath = args[0];
            nafFilePath = "/Users/piek/Desktop/NWR/NWR-DATA/LN_cars_NAF-2003_1001-2000/2003/1/1";
            KafSaxParser kafSaxParser = new KafSaxParser();
            ArrayList<File> files = Util.makeRecursiveFileList(new File(nafFilePath));
            for (int i = 0; i < files.size(); i++) {
                File file = files.get(i);
                kafSaxParser.parseFile(file);
                String baseUri = kafSaxParser.getKafMetaData().getUrl() + GetSemFromNaf.ID_SEPARATOR;
                ArrayList<SemObject> semActors = new ArrayList<SemObject>();
                String entityUri = ResourcesUri.nwrdata+project+"/entities/";
                if (!baseUri.toLowerCase().startsWith("http")) {
                    baseUri = ResourcesUri.nwrdata + project + "/" + kafSaxParser.getKafMetaData().getUrl() + GetSemFromNaf.ID_SEPARATOR;
                }
                GetSemFromNaf.processNafFileForEntityCoreferenceSets(entityUri, baseUri, kafSaxParser, semActors);
                GetSemFromNaf.processSrlForRemainingFramenetRoles(project, kafSaxParser, semActors);

                ArrayList<PerspectiveObject> perspectives = getPerspective(baseUri, kafSaxParser, semActors, contextualVector, communicationVector, grammaticalVector);
                perspectives = selectSourceEntityToPerspectives(kafSaxParser, perspectives, semActors);
                for (int j = 0; j < perspectives.size(); j++) {
                    PerspectiveObject perspectiveObject = perspectives.get(j);
                  //  System.out.println("perspectiveObject.toString() = " + perspectiveObject.toString());
                }
            }
        }

    /**
     *     public void addToJenaDataSet (Dataset ds, Model provenanceModel,
     HashMap<String, SourceMeta> sourceMetaHashMap) {

     addToJenaDataSet(ds, provenanceModel);
     Resource provenanceResource = provenanceModel.createResource(this.id);

     for (int i = 0; i < nafMentions.size(); i++) {
     NafMention nafMention = nafMentions.get(i);

     //http://www.newsreader-project.eu/data/cars/2003/10/10/49RC-C8V0-01D6-W1FX.xml
     //http://www.newsreader-project.eu/data/cars/2003/01/01/47KF-XY70-010F-G3GG.xml
     //System.out.println("nafMention.getBaseUriWithoutId() = " + nafMention.getBaseUriWithoutId());
     if (sourceMetaHashMap.containsKey(nafMention.getBaseUriWithoutId())) {
     //System.out.println("nafMention.getBaseUriWithoutId() = " + nafMention.getBaseUriWithoutId());

     SourceMeta sourceMeta = sourceMetaHashMap.get(nafMention.getBaseUriWithoutId());
     Property property = provenanceModel.createProperty(ResourcesUri.prov+"wasAttributedTo");
     if (!sourceMeta.getAuthor().isEmpty()) {
     Resource targetResource = provenanceModel.createResource(ResourcesUri.nwrauthor+sourceMeta.getAuthor());
     provenanceResource.addProperty(property, targetResource);
     }
     if (!sourceMeta.getOwner().isEmpty()) {
     Resource targetResource = provenanceModel.createResource(ResourcesUri.nwrsourceowner+sourceMeta.getOwner());
     provenanceResource.addProperty(property, targetResource);
     }
     }
     else {
     //System.out.println("No meta nafMention.getBaseUriWithoutId() = " + nafMention.getBaseUriWithoutId());
     //System.out.println("sourceMetaHashMap = " + sourceMetaHashMap.size());
     }
     }
     }
     */

    /**
     * @TODO get perspective objects needs to be adapted to get attributedTo relations to authors, owner, magazine
     * @param kafSaxParser
     * @param project
     * @param semActors
     * @param contextualVector
     * @param communicationVector
     * @param grammaticalVector
     * @return
     */
        static public ArrayList<PerspectiveObject> getPerspective (KafSaxParser kafSaxParser, String project,
                                                                   ArrayList<SemObject> semActors,
                                    Vector<String> contextualVector, 
                                    Vector<String> communicationVector,
                                    Vector<String> grammaticalVector) {
            String baseUri = kafSaxParser.getKafMetaData().getUrl() + GetSemFromNaf.ID_SEPARATOR;
            if (!baseUri.toLowerCase().startsWith("http")) {
                baseUri = ResourcesUri.nwrdata + project + "/" + kafSaxParser.getKafMetaData().getUrl() + GetSemFromNaf.ID_SEPARATOR;
            }
            ArrayList<PerspectiveObject> perspectiveObjects = getPerspective(baseUri,kafSaxParser, semActors, contextualVector, communicationVector, grammaticalVector);
            perspectiveObjects = selectSourceEntityToPerspectives(kafSaxParser, perspectiveObjects, semActors);
            return perspectiveObjects;
        }

       static public void getPerspective(KafSaxParser kafSaxParser, String project, ArrayList<PerspectiveObject> perspectives,
                                                                   ArrayList<SemObject> semActors,
                                    Vector<String> contextualVector,
                                    Vector<String> communicationVector,
                                    Vector<String> grammaticalVector) {
            String baseUri = kafSaxParser.getKafMetaData().getUrl() + GetSemFromNaf.ID_SEPARATOR;
            if (!baseUri.toLowerCase().startsWith("http")) {
                baseUri = ResourcesUri.nwrdata + project + "/" + kafSaxParser.getKafMetaData().getUrl() + GetSemFromNaf.ID_SEPARATOR;
            }
            ArrayList<PerspectiveObject> perspectiveObjects = getPerspective(baseUri,kafSaxParser, semActors, contextualVector, communicationVector, grammaticalVector);
            perspectives.addAll(perspectiveObjects);
        }

        static public ArrayList<PerspectiveObject> getPerspective (String baseUri,
                                                                   KafSaxParser kafSaxParser,
                                                                   ArrayList<SemObject> semActors,
                                    Vector<String> contextualVector,
                                    Vector<String> communicationVector,
                                    Vector<String> grammaticalVector) {
            ArrayList<PerspectiveObject> perspectiveObjectArrayList = new ArrayList<PerspectiveObject>();
            for (int i = 0; i < kafSaxParser.getKafEventArrayList().size(); i++) {
                KafEvent kafEvent = kafSaxParser.getKafEventArrayList().get(i);
                kafEvent.setTokenStrings(kafSaxParser);
                String eventType = FrameTypes.getEventTypeString(kafEvent.getExternalReferences(), contextualVector, communicationVector, grammaticalVector);
                if (!eventType.isEmpty()) {
                    if (eventType.equalsIgnoreCase(FrameTypes.SOURCE)) {
                        KafParticipant sourceParticipant = new KafParticipant();
                        KafParticipant targetParticipant = new KafParticipant();
                        /// next we get the A0 and message roles

                        for (int k = 0; k < kafEvent.getParticipants().size(); k++) {
                            KafParticipant kafParticipant = kafEvent.getParticipants().get(k);
                            if (RoleLabels.hasSourceTarget(kafParticipant, communicationVector)) {
                                targetParticipant = kafParticipant;
                            }
                            else if (RoleLabels.isPRIMEPARTICIPANT(kafParticipant.getRole())) {
                                sourceParticipant = kafParticipant;
                            }
                        }
                      //  if (sourceParticipant!=null && targetParticipant !=null) {
                          //  System.out.println("targetParticipant.toString() = " + targetParticipant.toString());

                            sourceParticipant.setTokenStrings(kafSaxParser);
                            targetParticipant.setTokenStrings(kafSaxParser);
                            PerspectiveObject perspectiveObject = new PerspectiveObject();
                            perspectiveObject.setDocumentUri(baseUri);
                            perspectiveObject.setPredicateId(kafEvent.getId());
                            perspectiveObject.setEventString(kafEvent.getTokenString());
                            perspectiveObject.setPredicateConcepts(kafEvent.getExternalReferences());
                            perspectiveObject.setPredicateSpanIds(kafEvent.getSpanIds());
                            perspectiveObject.setSource(sourceParticipant);
                            perspectiveObject.setTarget(targetParticipant);
                            perspectiveObject.setNafMention(baseUri, kafSaxParser, kafEvent.getSpanIds());
                            for (int j = 0; j < kafSaxParser.getKafEventArrayList().size(); j++) {
                                if (j!=i) {
                                    KafEvent event = kafSaxParser.getKafEventArrayList().get(j);
                                    if (!Collections.disjoint(targetParticipant.getSpanIds(), event.getSpanIds())) {
                                        /// this event is embedded inside the target
                                        NafMention nafMention = Util.getNafMentionForTermIdArrayList(baseUri, kafSaxParser, event.getSpanIds());
                                        nafMention.addFactuality(kafSaxParser);
                                        perspectiveObject.addTargetEventMention(nafMention);
                                    }
                                }
                            }
                        for (int j = 0; j < perspectiveObject.getTargetEventMentions().size(); j++) {
                            NafMention nafMention = perspectiveObject.getTargetEventMentions().get(j);
                           // System.out.println("nafMention.getFactuality().size() = " + nafMention.getFactuality().size());
                        }
                            perspectiveObjectArrayList.add(perspectiveObject);
                      //}
                    }
                }
            }
            return perspectiveObjectArrayList;
        }

    /**
     * Filters perspectives to select those that match with a given actor
     * @param kafSaxParser
     * @param perspectives
     * @param actors
     * @return
     */
        static public ArrayList<PerspectiveObject> selectSourceEntityToPerspectives (KafSaxParser kafSaxParser, ArrayList<PerspectiveObject> perspectives, ArrayList<SemObject> actors) {
            ArrayList<PerspectiveObject> sourcePerspectives = new ArrayList<PerspectiveObject>();
            for (int i = 0; i < perspectives.size(); i++) {
                PerspectiveObject perspectiveObject = perspectives.get(i);
                for (int j = 0; j < actors.size(); j++) {
                    SemObject semActor = actors.get(j);
                    if (Util.matchAllSpansOfAnObjectMentionOrTheRoleHead(kafSaxParser, perspectiveObject.getSource(), semActor)) {
                      //  System.out.println("semObject.getURI() = " + semActor.getURI());
                        perspectiveObject.setSourceEntity((SemActor)semActor);
                        sourcePerspectives.add(perspectiveObject);
                    }
                }
/*
                SemObject semObject = Util.getBestMatchingObject(kafSaxParser, perspectiveObject.getSource(), actors);
                if (semObject!=null) {
                    System.out.println("semObject.getURI() = " + semObject.getURI());
                    perspectiveObject.setSourceEntity((SemActor)semObject);
                    sourcePerspectives.add(perspectiveObject);
                }
*/
            }
            return sourcePerspectives;
        }



   /* static public ArrayList<PerspectiveObject> getDefaultPerspective (String baseUri,
                                                               KafSaxParser kafSaxParser,
                                                               Vector<String> contextualVector,
                                                               Vector<String> communicationVector,
                                                               Vector<String> grammaticalVector) {
        ArrayList<PerspectiveObject> perspectiveObjectArrayList = new ArrayList<PerspectiveObject>();
        for (int i = 0; i < kafSaxParser.getKafEventArrayList().size(); i++) {
            KafEvent kafEvent = kafSaxParser.getKafEventArrayList().get(i);
            kafEvent.setTokenStrings(kafSaxParser);
            String eventType = FrameTypes.getEventTypeString(kafEvent.getExternalReferences(), contextualVector, communicationVector, grammaticalVector);
            if (!eventType.isEmpty()) {
                if (!eventType.equalsIgnoreCase("source")) {
                    KafParticipant sourceParticipant = new KafParticipant();
*//*
                    KafParticipant targetParticipant = new KafParticipant();
                    /// next we get the A0 and message roles

                    for (int k = 0; k < kafEvent.getParticipants().size(); k++) {
                        KafParticipant kafParticipant = kafEvent.getParticipants().get(k);
                        if (RoleLabels.hasSourceTarget(kafParticipant, communicationVector)) {
                            targetParticipant = kafParticipant;
                        }
                        else if (RoleLabels.isPRIMEPARTICIPANT(kafParticipant.getRole())) {
                            sourceParticipant = kafParticipant;
                        }
                    }
                    //  if (sourceParticipant!=null && targetParticipant !=null) {
                    //  System.out.println("targetParticipant.toString() = " + targetParticipant.toString());

                    sourceParticipant.setTokenStrings(kafSaxParser);
                    targetParticipant.setTokenStrings(kafSaxParser);
*//*
                    PerspectiveObject perspectiveObject = new PerspectiveObject();
                    perspectiveObject.setDocumentUri(baseUri);
                    perspectiveObject.setPredicateId(kafEvent.getId());
                    perspectiveObject.setEventString(kafEvent.getTokenString());
                    perspectiveObject.setPredicateConcepts(kafEvent.getExternalReferences());
                    perspectiveObject.setPredicateSpanIds(kafEvent.getSpanIds());
                    perspectiveObject.setSource(sourceParticipant);
                    perspectiveObject.setTarget(targetParticipant);
                    perspectiveObject.setNafMention(baseUri, kafSaxParser, kafEvent.getSpanIds());

                    NafMention nafMention = Util.getNafMentionForTermIdArrayList(baseUri, kafSaxParser, kafEvent.getSpanIds());
                    nafMention.addFactuality(kafSaxParser);
                    perspectiveObject.addTargetEventMention(nafMention);

                    perspectiveObjectArrayList.add(perspectiveObject);
                    //}
                }
            }
        }
        return perspectiveObjectArrayList;
    }
*/

    public static void perspectiveRelationsToTrig (String pathToTrigFile, ArrayList<PerspectiveObject> perspectiveObjects) {
        try {
            OutputStream fos = new FileOutputStream(pathToTrigFile);
            Dataset ds = TDBFactory.createDataset();
            Model defaultModel = ds.getDefaultModel();
            ResourcesUri.prefixModel(defaultModel);
          //  Model provenanceModel = ds.getNamedModel("http://www.newsreader-project.eu/perspective");
            ResourcesUri.prefixModelGaf(defaultModel);
            JenaSerialization.addJenaPerspectiveObjects(ds, perspectiveObjects);
            RDFDataMgr.write(fos, ds, RDFFormat.TRIG_PRETTY);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public static void perspectiveRelationsToTrigStream (OutputStream fos, ArrayList<PerspectiveObject> perspectiveObjects) {

                Dataset ds = TDBFactory.createDataset();
                Model defaultModel = ds.getDefaultModel();
                ResourcesUri.prefixModel(defaultModel);
              //  Model provenanceModel = ds.getNamedModel("http://www.newsreader-project.eu/perspective");
                ResourcesUri.prefixModelGaf(defaultModel);
                JenaSerialization.addJenaPerspectiveObjects(ds, perspectiveObjects);
                RDFDataMgr.write(fos, ds, RDFFormat.TRIG_PRETTY);
    }


}
