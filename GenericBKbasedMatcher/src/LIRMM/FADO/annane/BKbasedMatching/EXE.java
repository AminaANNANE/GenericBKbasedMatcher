package LIRMM.FADO.annane.BKbasedMatching;

import java.io.File;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Session;


public class EXE {

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		
		//This is an example about how to run our framework
		//The matcher variable in Parameters Class represents the direct matcher that we use for anchoring
		//matcher may be an instance of any direct matcher that implements a static function align(URL source, URL target) function
		//The align function should return the URL of the generated alignment between source and target
		
		// create the necessary folders
		Fichier.returnFolder("ProcessFolders");
		Fichier.returnFolder(Parameters.BkAlignmentsFolderPath);
		Fichier.returnFolder(Parameters.BkFolderPath);
		Fichier.returnFolder(Parameters.derivationResultFolderPath);
		Fichier.returnFolder(Parameters.ResultFolderPath);
		Fichier.returnFolder(Parameters.directAlignmentFolderPath);
		
		
		String referenceAlignment="C:\\Users\\aannane\\Desktop\\MyDocuments\\JWS\\bridges\\Nouveau dossier\\Anatomy\\reference.rdf";
		String sourcePath="C:\\Users\\aannane\\Desktop\\MyDocuments\\JWS\\bridges\\Nouveau dossier\\Anatomy\\MA.owl";
		String targetPath="C:\\Users\\aannane\\Desktop\\MyDocuments\\JWS\\bridges\\Nouveau dossier\\Anatomy\\NCI.owl";
		
		//Parameters
		Parameters.derivationStrategy=Parameters.derivationStrategies.neo4j;
		
		Parameters.BKselectionInternalExploration=true;
		
		File sourceOntologyFile=new File(sourcePath);//source ontology
		File targetOntologyFile=new File(targetPath);//target ontology
		Parameters.sourceOntology=sourceOntologyFile.toURI().toURL();
		Parameters.targetOntology=targetOntologyFile.toURI().toURL();
		
		/**
		 * Initializing driver and session variables is necessary only if you want to use Neo4j for the derivation process.
		 */
		Parameters.driver = GraphDatabase.driver( "bolt://localhost:7687", AuthTokens.basic("neo4j", "aminaamina")  );
		Parameters.session=Parameters.driver.session();
		Parameters.derivationMaxPathLength=4;
		
		/**
		 * These parameters are necessary if we want to derive mappings with other relations than equivalence
		 */
		Parameters.BKselectionInternalExploration=true;
		Parameters.BKselectionExplorationLength=1;
		Parameters.BKselectionExplorationRelations= new ArrayList<Relation>();
		Relation r=new Relation("http://www.w3.org/2000/01/rdf-schema#subClassOf","<", "subClassOf");
		Parameters.BKselectionExplorationRelations.add(r);
		
		Parameters.mappingSelectionThreshold=0.0;
		
		Parameters.logMapRepair=false;
		
		//match the input ontologies if they exist
		if(Parameters.matcher!=null && sourceOntologyFile.exists()&&targetOntologyFile.exists())
		{
			Matching matcher=new Matching(sourceOntologyFile.toURI().toURL(), targetOntologyFile.toURI().toURL());
			URL res=matcher.BkBasedMatching();//res will contain the URL of the generated alignment
			System.out.println("the generated alignment is here: "+res);
			matcher.ComputeFScore(res, referenceAlignment);
		}
		else
			System.out.println("One of the ontologies to align does not exist!");
	}

}
